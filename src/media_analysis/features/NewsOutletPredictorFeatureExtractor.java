package media_analysis.features;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import media_analysis.Configuration;
import media_analysis.Outlet;
import media_analysis.utils.Consts;
import media_analysis.utils.ProcessedData;

import com.sun.org.apache.xerces.internal.impl.io.UTF8Reader;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffSaver;

public class NewsOutletPredictorFeatureExtractor extends Consts {

    private static final String HEB_LETTERS = "אבגדהוזחטיכךלמםנןסעפףצץקרשת";
    private static final List<Integer> HEB_MAX_ACC_LETTER_INDICES = Arrays.asList(1, 2, 5, 6, 10, 11, 12, 13, 14, 15,
                    16, 17, 18, 19, 20, 22, 26, 27);
    private static final Pattern punctPattern = Pattern.compile("^[\\p{Punct}\\|–]+$");

    private static final DateFormat sourceDataFormat = new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH);
    private static final DateFormat confDataFormat = new SimpleDateFormat(Configuration.DATE_PATTERN, Locale.ENGLISH);

    private Map<String, Integer> lemmaFreqTable;
    private Map<String, Integer> wordFreqTable;

    private Date startDate = new Date(0);
    private Date endDate = new Date(Long.MAX_VALUE);

    private boolean optimizeFeatures = false;
    private boolean setIds = false;

    public NewsOutletPredictorFeatureExtractor(Configuration conf) throws IOException {
        lemmaFreqTable = readFreqTable(conf.get(Configuration.LEMMA_FREQ_FILE_KEY));
        wordFreqTable = readFreqTable(conf.get(Configuration.WORD_FREQ_FILE_KEY));
        optimizeFeatures = conf.getBoolean(Configuration.OPTIMIZE_FEATURES_KEY);
        setIds = conf.getBoolean(Configuration.FEATURES_HAVE_IDS_KEY);
        try {
            startDate = conf.getDate(Configuration.START_DATE_KEY);
            endDate = conf.getDate(Configuration.END_DATE_KEY);
            endDate.setHours(23);
            endDate.setMinutes(59);
        } catch (ParseException e) {
            System.err.println("No date range specified - using all data");
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: FeatureExtractor <conf-file>");
            return;
        }

        Configuration conf = new Configuration(args[0]);
        NewsOutletPredictorFeatureExtractor extractor = new NewsOutletPredictorFeatureExtractor(conf);

        boolean runOnAllPairs = conf.getBoolean(Configuration.WRITE_PAIRED_KEY);

        List<String> outlets = new ArrayList<>();
        String[] confOutlets = conf.getArray(Configuration.OUTLETS_KEY);
        if (confOutlets[0].equals("ALL")) {
            outlets = outletList();
        } else {
            outlets = Arrays.asList(confOutlets);
        }
        String inArg = conf.get(Configuration.INPUT_KEY);
        String outBaseArg = conf.get(Configuration.OUTPUT_KEY);

        extractor.extractFeatures(inArg, outBaseArg, outlets);

        if (runOnAllPairs) {
            for (int i = 0; i < outlets.size(); i++) {
                for (int j = i + 1; j < outlets.size(); j++) {
                    List<String> outletPair = Arrays.asList(outlets.get(i), outlets.get(j));
                    extractor.extractFeatures(inArg, outBaseArg, outletPair);
                }
            }
        }

    }

    @SuppressWarnings("resource")
    public void extractFeatures(String inFileLocation, String outFileBase, List<String> outlets)
                    throws FileNotFoundException, IOException {

        StringBuilder sb = new StringBuilder(outFileBase);
        if (!setIds) {
            sb.append("-no-ids");
        }
        for (String outlet : outlets) {
            sb.append("-" + Outlet.toCode(outlet));
        }
        sb.append("_").append(confDataFormat.format(startDate)).append("-").append(confDataFormat.format(endDate))
                        .append(".arff");
        String outFileLocation = sb.toString();
        System.out.println("Writing features derived from " + inFileLocation + " to " + outFileLocation);

        Attribute idAttr = new Attribute("ID", (List<String>) null);
        Attribute numOfCharsAttr = new Attribute("num-of-chars");
        Attribute numOfWordsAttr = new Attribute("num-of-words");
        Attribute avgWordLengthAttr = new Attribute("avg-word-length");
        Attribute minWordLengthAttr = new Attribute("min-word-length");
        Attribute medWordLengthAttr = new Attribute("med-word-length");
        Attribute maxWordLengthAttr = new Attribute("max-word-length");
        Attribute numOfPunctsAttr = new Attribute("num-of-puncts");
        Attribute avgIsrablogLemmaFreqAttr = new Attribute("avg-isbl-lemma-freq");
        Attribute avgWordlistWordFreqAttr = new Attribute("avg-wlst-word-freq");
        Attribute maxIsrablogLemmaFreqAttr = new Attribute("max-isbl-lemma-freq");
        Attribute maxWordlistWordFreqAttr = new Attribute("max-wlst-word-freq");
        Attribute minIsrablogLemmaFreqAttr = optimizeFeatures ? null : new Attribute("min-isbl-lemma-freq");
        Attribute minWordlistWordFreqAttr = optimizeFeatures ? null : new Attribute("min-wlst-word-freq");
        Attribute medIsrablogLemmaFreqAttr = new Attribute("med-isbl-lemma-freq");
        Attribute medWordlistWordFreqAttr = new Attribute("med-wlst-word-freq");
        Attribute epochCountAttr = optimizeFeatures ? null : new Attribute("epochs");
        Map<Character, Attribute> affixLetterAttrs = new HashMap<>();
        int l = 1;
        for (char a : HEB_LETTERS.toCharArray()) {
            if (!optimizeFeatures || HEB_MAX_ACC_LETTER_INDICES.contains(l)) {
                affixLetterAttrs.put(a, new Attribute("affix-" + l));
            }
            l++;
        }
        Attribute[] freqJanLemmataAttrs = optimizeFeatures ? null : new Attribute[50];
        if (freqJanLemmataAttrs != null) {
            for (int i = 0; i < 50; i++) {
                freqJanLemmataAttrs[i] = new Attribute("freq-jan-lemma-" + i);
            }
        }
        Attribute totalAffixLettersAttr = optimizeFeatures ? null : new Attribute("total-affix-letters");
        Attribute affixLettersPerWordAttr = optimizeFeatures ? null : new Attribute("affix-letters-per-word");
        Attribute affixLettersPerCharAttr = optimizeFeatures ? null : new Attribute("affix-letters-per-char");
        Attribute outletAttr = new Attribute("class", outlets);
        ArrayList<Attribute> attrs = new ArrayList<>();
        if (setIds) {
            attrs.add(idAttr);
        }
        attrs.add(numOfCharsAttr);
        attrs.add(numOfWordsAttr);
        attrs.add(avgWordLengthAttr);
        attrs.add(minWordLengthAttr);
        attrs.add(medWordLengthAttr);
        attrs.add(maxWordLengthAttr);
        attrs.add(numOfPunctsAttr);
        attrs.add(avgIsrablogLemmaFreqAttr);
        attrs.add(avgWordlistWordFreqAttr);
        attrs.add(maxIsrablogLemmaFreqAttr);
        attrs.add(maxWordlistWordFreqAttr);
        if (!optimizeFeatures) {
            attrs.add(minIsrablogLemmaFreqAttr);
            attrs.add(minWordlistWordFreqAttr);
        }
        attrs.add(medIsrablogLemmaFreqAttr);
        attrs.add(medWordlistWordFreqAttr);
        if (!optimizeFeatures) {
            attrs.add(epochCountAttr);
        }
        attrs.addAll(affixLetterAttrs.values());
        if (freqJanLemmataAttrs != null) {
            for (Attribute fla : freqJanLemmataAttrs) {
                attrs.add(fla);
            }
        }
        if (!optimizeFeatures) {
            attrs.add(totalAffixLettersAttr);
            attrs.add(affixLettersPerWordAttr);
            attrs.add(affixLettersPerCharAttr);
        }
        attrs.add(outletAttr);
        Instances instances = new Instances("Instances", attrs, 100);

        BufferedReader in = new BufferedReader(new UTF8Reader(new FileInputStream(new File(inFileLocation))));
        String line = in.readLine();
        int written = 0, badLineInputs = 0, wordLemmaAlignmentFails = 0;
        Set<String> OutletsWithLastRead = new HashSet<>();
        while (line != null) {
            // time-changed \t outlet \t epochs \t raw \t lemmatized
            String[] columns = line.split("\\t");
            if (columns.length != 5) {
                badLineInputs++;
                line = in.readLine();
                continue;
            }

            // outlet filtering
            String outlet = columns[1];
            if (!outlets.contains(outlet)) {
                line = in.readLine();
                continue;
            }

            // date filtering - but keep one extra for each because it contains data from last day
            Date instanceTime = null;
            try {
                instanceTime = sourceDataFormat.parse(columns[0]);
            } catch (ParseException e) {
                System.out.println("Bad date: " + line);
                badLineInputs++;
                line = in.readLine();
                continue;
            }
            if (instanceTime.before(startDate)) {
                line = in.readLine();
                continue;
            }
            if (instanceTime.after(endDate)) {
                if (OutletsWithLastRead.contains(outlet)) {
                    line = in.readLine();
                    continue;
                }
                OutletsWithLastRead.add(outlet);
            }

            // TODO time-based duplication

            Instance inst = new SparseInstance(0);
            if (setIds) {
                inst.setValue(idAttr, columns[0] + ":" + outlet);
            }

            String rawTitle = columns[3];
            String lemTitle = columns[4];

            // num of chars
            int numOfChars = rawTitle.length();
            inst.setValue(numOfCharsAttr, numOfChars);

            // num of punct
            inst.setValue(numOfPunctsAttr, countPuncts(rawTitle));

            // num of epochs
            if (!optimizeFeatures) {
                inst.setValue(epochCountAttr, Integer.parseInt(columns[2]));
            }

            String[] rawWords = rawTitle.split("\\s+");
            String[] rawLem = lemTitle.split("\\s+");

            // num of words
            int numOfWords = rawWords.length;
            int numOfLemmata = rawLem.length;
            inst.setValue(numOfWordsAttr, numOfWords);

            // word freq, length stats
            double totalWordlistWordLogFreq = 0.0;
            double[] wordLogFreqs = new double[numOfWords];
            int totalWordChars = 0;
            int[] wordLengths = new int[numOfWords];
            int i = 0;
            for (String w : rawWords) {
                int len = noPunctLength(w);
                totalWordChars += len;
                wordLengths[i] = len;
                Integer f = wordFreqTable.get(w);
                if (f == null || f < 5) {
                    f = 3;
                }
                double logFreq = Math.log(f);
                wordLogFreqs[i] = logFreq;
                totalWordlistWordLogFreq += logFreq;
                i++;
            }
            inst.setValue(avgWordlistWordFreqAttr, numOfWords == 0 ? 0.0 : totalWordlistWordLogFreq / numOfWords);
            Arrays.sort(wordLogFreqs);
            if (!optimizeFeatures) {
                inst.setValue(minWordlistWordFreqAttr, numOfWords == 0 ? 0.0 : wordLogFreqs[0]);
            }
            inst.setValue(medWordlistWordFreqAttr, numOfWords == 0 ? 0.0 : wordLogFreqs[numOfWords / 2]);
            inst.setValue(maxWordlistWordFreqAttr, numOfWords == 0 ? 0.0 : wordLogFreqs[numOfWords - 1]);

            inst.setValue(avgWordLengthAttr, ((double) totalWordChars) / numOfWords);
            Arrays.sort(wordLengths);
            inst.setValue(minWordLengthAttr, wordLengths[0]);
            inst.setValue(medWordLengthAttr, wordLengths[numOfWords / 2]);
            inst.setValue(maxWordLengthAttr, wordLengths[numOfWords - 1]);

            // lemma freq + freq lemmata
            double totalIsrablogLemmaLogFreq = 0.0;
            double[] lemLogFreqs = new double[numOfLemmata];
            i = 0;
            for (String lem : rawLem) {
                // lemma freq
                Integer f = lemmaFreqTable.get(lem);
                if (f == null || f < 10) {
                    f = 5;
                }
                double logFreq = Math.log(f);
                lemLogFreqs[i] = logFreq;
                totalIsrablogLemmaLogFreq += logFreq;
                i++;

                // freq lemmata
                if (freqJanLemmataAttrs == null) {
                    continue;
                }
                for (int fjl = 0; fjl < 50; fjl++) {
                    if (ProcessedData.topFiftyJanLemmata[fjl].equals(lem)) {
                        Attribute attr = freqJanLemmataAttrs[fjl];
                        inst.setValue(attr, inst.value(attr) + 1);
                    }
                }
            }
            inst.setValue(avgIsrablogLemmaFreqAttr, numOfLemmata == 0 ? 0.0 : totalIsrablogLemmaLogFreq / numOfLemmata);
            Arrays.sort(lemLogFreqs);
            if (!optimizeFeatures) {
                inst.setValue(minIsrablogLemmaFreqAttr, numOfLemmata == 0 ? 0.0 : lemLogFreqs[0]);
            }
            inst.setValue(medIsrablogLemmaFreqAttr, numOfLemmata == 0 ? 0.0 : lemLogFreqs[numOfLemmata / 2]);
            inst.setValue(maxIsrablogLemmaFreqAttr, numOfLemmata == 0 ? 0.0 : lemLogFreqs[numOfLemmata - 1]);

            // affix letters
            int totalAffixLetters = 0;
            try {
                int shift = 0;
                for (int j = 0; j < numOfWords; j++) {
                    String raw = rawWords[j];
                    if (isPunct(raw) || raw.trim().isEmpty()) {
                        shift++;
                        continue;
                    }
                    String lem = rawLem[j - shift];
                    for (char c : raw.toCharArray()) {
                        String cs = "" + c;
                        if (!lem.contains(cs) && HEB_LETTERS.contains(cs)) {
                            totalAffixLetters++;
                            Attribute val = affixLetterAttrs.get(c);
                            if (val != null) {
                                inst.setValue(val, inst.value(val) + 1);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Failed alignment: " + rawTitle + "\t" + lemTitle);
                wordLemmaAlignmentFails++;
            }
            if (!optimizeFeatures) {
                inst.setValue(totalAffixLettersAttr, totalAffixLetters);
                inst.setValue(affixLettersPerWordAttr, ((double) totalAffixLetters) / numOfWords);
                inst.setValue(affixLettersPerCharAttr, ((double) totalAffixLetters) / numOfChars);
            }

            // class: outlet
            inst.setValue(outletAttr, outlet);

            instances.add(inst);
            written++;

            line = in.readLine();
        }
        in.close();

        if (instances != null) {
            ArffSaver saver = new ArffSaver();
            saver.setInstances(instances);
            saver.setFile(new File(outFileLocation));
            saver.writeBatch();
        }

        System.out.println("Finished! Wrote " + written + " vectors with " + badLineInputs + " bad inputs and "
                        + wordLemmaAlignmentFails + " alignment failures.");
    }

    private static int noPunctLength(String iText) {
        return iText.length() - countPuncts(iText);
    }

    @SuppressWarnings("resource")
    private static Map<String, Integer> readFreqTable(String iFileLocation) throws FileNotFoundException, IOException {
        Map<String, Integer> freqTable = new HashMap<>();
        BufferedReader freqFile = new BufferedReader(new UTF8Reader(new FileInputStream(new File(iFileLocation))));
        String freqLine = freqFile.readLine();
        while (freqLine != null) {
            String[] wordFreq = freqLine.split("\\t");
            if (wordFreq.length != 2) {
                continue;
            }
            freqTable.put(wordFreq[0], Integer.parseInt(wordFreq[1]));
            freqLine = freqFile.readLine();
        }
        freqFile.close();
        return freqTable;
    }

    private static int countPuncts(String iText) {
        int punctCount = 0;
        for (char c : iText.toCharArray()) {
            if (isPunct(Character.toString(c)))
                punctCount++;
        }
        return punctCount;
    }

    private static boolean isPunct(String s) {
        Matcher m = punctPattern.matcher(s);
        return m.matches();
    }

    private static List<String> outletList() {
        List<String> classes = new ArrayList<>();
        for (Outlet val : Outlet.values()) {
            classes.add(val.name());
        }
        return classes;
    }

}
