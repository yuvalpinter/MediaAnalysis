package media_analysis.features;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import media_analysis.Outlet;

import com.sun.org.apache.xerces.internal.impl.io.UTF8Reader;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffSaver;

public class NewsOutletPredictorFeatureExtractor {

    private static final String ISRABLOG_FREQ_FILE_LOCATION = "data/israblog-freqs.txt";
    private static final String WORDLISTS_FREQ_FILE_LOCATION = "data/wordlists-freqs-2012.txt";
    private static final String HEB_LETTERS = "אבגדהוזחטיכךלמםנןסעפףצץקרשת";
    private static final List<Integer> HEB_MAX_ACC_LETTER_INDICES = Arrays.asList(1, 2, 5, 6, 10, 11, 12, 13, 14, 15,
                    16, 17, 18, 19, 20, 22, 26, 27);
    private static final Pattern punctPattern = Pattern.compile("^[\\p{Punct}\\|–]+$");

    private static final String[] topFiftyFebLemmata = {"את", "נתניהו", "נשל", "0", "לא", "על", "ישראל", "בית", "עם",
                    "ניגד", "שלג", "מבקר", "בחירה", "ממשלה", "איחר", "איראן", "נאם", "ליכוד", "הרצוג", "דאעש",
                    "ירושלים", "היה", "קונגרס", "משטרה", "מדינה", "דרום", "כול", "ראש", "דיור", "לפיד", "דוח", "פיגוע",
                    "ניצב", "רה\"מ", "קשה", "טרור", "ציון", "סערה", "יש", "דו\"ח", "אין", "ארה\"ב", "הרוג", "תאונה",
                    "ליבן", "חשד", "בנט", "ועדה", "מעון", "ירדן"};

    private static final String[] topFiftyJanLemmata = {"0", "את", "על", "נשל", "נתניהו", "ישראל", "לא", "פריז",
                    "טרור", "פיגוע", "בית", "ליכוד", "צה\"ל", "שלג", "דיווח", "ניגד", "ירושלים", "סוריה", "איחר",
                    "מחבל", "צפן", "ישן", "חיזבאללה", "משטרה", "עם", "הרוג", "רשימה", "צרפת", "בנה", "נהרג", "היה",
                    "חי", "סערה", "איראן", "גולן", "עבודה", "רכב", "שידור", "יהודים", "מקום", "פריימריז", "כול", "ראש",
                    "נפגע", "ערובה", "בחירה", "צבא", "בכיר", "ירה", "הר"};

    private Map<String, Integer> israBlogFreqTable;
    private Map<String, Integer> wordlistsFreqTable;

    public NewsOutletPredictorFeatureExtractor() throws IOException {
        israBlogFreqTable = readFreqTable(ISRABLOG_FREQ_FILE_LOCATION);
        wordlistsFreqTable = readFreqTable(WORDLISTS_FREQ_FILE_LOCATION);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: FeatureExtractor <input> <output>");
            return;
        }

        NewsOutletPredictorFeatureExtractor extractor = new NewsOutletPredictorFeatureExtractor();

        boolean setIds = false;
        boolean runOnceOnAllOutlets = true;
        boolean runOnAllPairs = false;
        boolean runMaxAccFeats = true;

        List<String> allOutlets = outletList();
        String inArg = args[0];
        String outBaseArg = args[1];
        if (runOnceOnAllOutlets) {
            extractor.extractFeatures(inArg, outBaseArg + (setIds ? "" : "-no-ids"), setIds, runMaxAccFeats, allOutlets);
        }

        Outlet[] outletVals = Outlet.values();
        if (runOnAllPairs) {
            for (int i = 0; i < outletVals.length; i++) {
                Outlet oi = outletVals[i];
                for (int j = i + 1; j < outletVals.length; j++) {
                    List<String> outlets = new ArrayList<>(2);
                    Outlet oj = outletVals[j];
                    outlets.add(oi.name());
                    outlets.add(oj.name());
                    extractor.extractFeatures(inArg, outBaseArg + "-" + oi.code() + "-" + oj.code(), false,
                                    runMaxAccFeats, outlets);
                }
            }
        }

    }

    public void extractFeatures(String inFileLocation, String outFileLocation, boolean setIds, boolean runMaxAccFeats,
                    List<String> outlets) throws FileNotFoundException, IOException {

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
        Attribute minIsrablogLemmaFreqAttr = runMaxAccFeats ? null : new Attribute("min-isbl-lemma-freq");
        Attribute minWordlistWordFreqAttr = runMaxAccFeats ? null : new Attribute("min-wlst-word-freq");
        Attribute medIsrablogLemmaFreqAttr = new Attribute("med-isbl-lemma-freq");
        Attribute medWordlistWordFreqAttr = new Attribute("med-wlst-word-freq");
        Attribute epochCountAttr = runMaxAccFeats ? null : new Attribute("epochs");
        Map<Character, Attribute> affixLetterAttrs = new HashMap<>();
        int l = 1;
        for (char a : HEB_LETTERS.toCharArray()) {
            if (!runMaxAccFeats || HEB_MAX_ACC_LETTER_INDICES.contains(l)) {
                affixLetterAttrs.put(a, new Attribute("affix-" + l));
            }
            l++;
        }
        Attribute[] freqJanLemmataAttrs = runMaxAccFeats ? null : new Attribute[50];
        if (freqJanLemmataAttrs != null) {
            for (int i = 0; i < 50; i++) {
                freqJanLemmataAttrs[i] = new Attribute("freq-jan-lemma-" + i);
            }
        }
        Attribute totalAffixLettersAttr = runMaxAccFeats ? null : new Attribute("total-affix-letters");
        Attribute affixLettersPerWordAttr = runMaxAccFeats ? null : new Attribute("affix-letters-per-word");
        Attribute affixLettersPerCharAttr = runMaxAccFeats ? null : new Attribute("affix-letters-per-char");
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
        if (!runMaxAccFeats) {
            attrs.add(minIsrablogLemmaFreqAttr);
            attrs.add(minWordlistWordFreqAttr);
        }
        attrs.add(medIsrablogLemmaFreqAttr);
        attrs.add(medWordlistWordFreqAttr);
        if (!runMaxAccFeats) {
            attrs.add(epochCountAttr);
        }
        attrs.addAll(affixLetterAttrs.values());
        if (freqJanLemmataAttrs != null) {
            for (Attribute fla : freqJanLemmataAttrs) {
                attrs.add(fla);
            }
        }
        if (!runMaxAccFeats) {
            attrs.add(totalAffixLettersAttr);
            attrs.add(affixLettersPerWordAttr);
            attrs.add(affixLettersPerCharAttr);
        }
        attrs.add(outletAttr);
        Instances instances = new Instances("Instances", attrs, 100);

        BufferedReader in = new BufferedReader(new UTF8Reader(new FileInputStream(new File(inFileLocation))));
        String line = in.readLine();
        int written = 0, badLineInputs = 0, wordLemmaAlignmentFails = 0;
        while (line != null) {
            // time-changed \t outlet \t epochs \t raw \t lemmatized
            String[] columns = line.split("\\t");
            if (columns.length != 5) {
                badLineInputs++;
                line = in.readLine();
                continue;
            }

            String outlet = columns[1];
            if (!outlets.contains(outlet)) {
                line = in.readLine();
                continue;
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
            if (!runMaxAccFeats) {
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
                Integer f = wordlistsFreqTable.get(w);
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
            if (!runMaxAccFeats) {
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
                Integer f = israBlogFreqTable.get(lem);
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
                    if (topFiftyJanLemmata[fjl].equals(lem)) {
                        Attribute attr = freqJanLemmataAttrs[fjl];
                        inst.setValue(attr, inst.value(attr) + 1);
                    }
                }
            }
            inst.setValue(avgIsrablogLemmaFreqAttr, numOfLemmata == 0 ? 0.0 : totalIsrablogLemmaLogFreq / numOfLemmata);
            Arrays.sort(lemLogFreqs);
            if (!runMaxAccFeats) {
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
            if (!runMaxAccFeats) {
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

        // String output = args[1] + "_ih-ha" + ".arff";
        String output = outFileLocation + ".arff";
        if (instances != null) {
            ArffSaver saver = new ArffSaver();
            saver.setInstances(instances);
            saver.setFile(new File(output));
            saver.writeBatch();
        }

        System.out.println("Finished! Wrote " + written + " vectors with " + badLineInputs + " bad inputs and "
                        + wordLemmaAlignmentFails + " alignment failures.");
    }

    private static int noPunctLength(String iText) {
        return iText.length() - countPuncts(iText);
    }

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
