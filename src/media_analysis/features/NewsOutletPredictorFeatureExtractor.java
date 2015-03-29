package media_analysis.features;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
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
    private static final Pattern punctPattern = Pattern.compile("^[\\p{Punct}]+$");

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: FeatureExtractor <input> <output>");
            return;
        }

        Map<String, Integer> israBlogFreqTable = readFreqTable(ISRABLOG_FREQ_FILE_LOCATION);
        Map<String, Integer> wordlistsFreqTable = readFreqTable(WORDLISTS_FREQ_FILE_LOCATION);

        Attribute idAttr = new Attribute("ID", (List<String>) null);
        Attribute numOfCharsAttr = new Attribute("num-of-chars");
        Attribute numOfPunctsAttr = new Attribute("num-of-puncts");
        Attribute avgIsrablogLemmaFreq = new Attribute("avg-isbl-lemma-freq");
        Attribute avgWordlistWordFreq = new Attribute("avg-wlst-word-freq");
        Attribute epochCountAttr = new Attribute("epochs");
        Map<Character, Attribute> affixLetterAttrs = new HashMap<>();
        int l = 1;
        for (char a : HEB_LETTERS.toCharArray()) {
            affixLetterAttrs.put(a, new Attribute("affix-" + l));
            l++;
        }
        Attribute outletAttr = new Attribute("class", outletList());
        ArrayList<Attribute> attrs = new ArrayList<>();
        attrs.add(idAttr);
        attrs.add(numOfCharsAttr);
        attrs.add(numOfPunctsAttr);
        attrs.add(avgIsrablogLemmaFreq);
        attrs.add(avgWordlistWordFreq);
        attrs.add(epochCountAttr);
        attrs.addAll(affixLetterAttrs.values());
        attrs.add(outletAttr);
        Instances instances = new Instances("Instances", attrs, 100);

        BufferedReader in = new BufferedReader(new UTF8Reader(new FileInputStream(new File(args[0]))));
        String line = in.readLine();
        int written = 0, badLineInputs = 0;
        while (line != null) {
            // time-changed \t outlet \t epochs \t raw \t lemmatized
            String[] columns = line.split("\\t");
            if (columns.length != 5) {
                badLineInputs++;
                line = in.readLine();
                continue;
            }

            String outlet = columns[1];
            // if (!outlet.startsWith("ISR") && !outlet.startsWith("HA")) {
            // line = in.readLine();
            // continue;
            // }

            // TODO time-based duplication

            Instance inst = new SparseInstance(0);
            inst.setValue(idAttr, columns[0] + ":" + outlet);

            String rawTitle = columns[3];
            String lemTitle = columns[4];

            // num of chars
            inst.setValue(numOfCharsAttr, rawTitle.length());

            // num of punct
            inst.setValue(numOfPunctsAttr, countPuncts(rawTitle));

            // num of epochs
            inst.setValue(epochCountAttr, Integer.parseInt(columns[2]));

            // TODO num of words, word length, overall affixes, israblog avg. freq
            // TODO words
            String[] rawWords = rawTitle.split("\\s+");
            // TODO lemmata
            String[] rawLem = lemTitle.split("\\s+");

            // word freq
            double totalWordlistWordLogFreq = 0.0;
            for (String w : rawWords) {
                Integer f = wordlistsFreqTable.get(w);
                if (f == null || f < 5) {
                    f = 3;
                }
                totalWordlistWordLogFreq += Math.log(f);
            }
            inst.setValue(avgWordlistWordFreq, rawLem.length == 0 ? 0.0 : totalWordlistWordLogFreq / rawWords.length);
            
            // lemma freq
            double totalIsrablogLemmaLogFreq = 0.0;
            for (String lem : rawLem) {
                Integer f = israBlogFreqTable.get(lem);
                if (f == null || f < 10) {
                    f = 5;
                }
                totalIsrablogLemmaLogFreq += Math.log(f);
            }
            inst.setValue(avgIsrablogLemmaFreq, rawLem.length == 0 ? 0.0 : totalIsrablogLemmaLogFreq / rawLem.length);
            
            // affix letters
            if (rawWords.length == rawLem.length) {
                for (int i = 0; i < rawWords.length; i++) {
                    String raw = rawWords[i];
                    String lem = rawLem[i];
                    for (char c : raw.toCharArray()) {
                        String cs = "" + c;
                        if (!lem.contains(cs) && HEB_LETTERS.contains(cs)) {
                            Attribute val = affixLetterAttrs.get(c);
                            inst.setValue(val, inst.value(val) + 1);
                        }
                    }
                }
            }
            
            // class: outlet
            inst.setValue(outletAttr, outlet);

            instances.add(inst);
            written++;

            line = in.readLine();
        }
        in.close();

        // String output = args[1] + "_ih-ha" + ".arff";
        String output = args[1] + ".arff";
        if (instances != null) {
            ArffSaver saver = new ArffSaver();
            saver.setInstances(instances);
            saver.setFile(new File(output));
            saver.writeBatch();
        }

        System.out.println("Finished! Wrote " + written + " vectors with " + badLineInputs + " bad inputs.");
    }

    private static Map<String, Integer> readFreqTable(String iFileLocation) throws FileNotFoundException, IOException {
        Map<String, Integer> freqTable = new HashMap<>();
        BufferedReader freqFile =
                        new BufferedReader(new UTF8Reader(new FileInputStream(new File(iFileLocation))));
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
