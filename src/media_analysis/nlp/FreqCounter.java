package media_analysis.nlp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import media_analysis.Outlet;

import com.sun.org.apache.xerces.internal.impl.io.UTF8Reader;

public class FreqCounter {

    // TODO sort
    public static void main(String[] args) throws IOException {
        
        int threshold = 5;

        BufferedReader in = new BufferedReader(new UTF8Reader(new FileInputStream(new File(args[0]))));
        String line = in.readLine();
        Map<Outlet, Map<String, Integer>> wordsFreqs = new HashMap<>();
        Map<Outlet, Map<String, Integer>> lemmataFreqs = new HashMap<>();
        Map<String, Integer> allWordFreqs = new HashMap<>();
        Map<String, Integer> allLemmataFreqs = new HashMap<>();
        for (Outlet o : Outlet.values()) {
            wordsFreqs.put(o, new HashMap<String, Integer>());
            lemmataFreqs.put(o, new HashMap<String, Integer>());
        }
        int lines = 0;
        while (line != null) {
            // time-changed \t outlet \t epochs \t raw \t lemmatized
            String[] columns = line.split("\\t");
            if (columns.length != 5) {
                continue;
            }
            Outlet o = Outlet.valueOf(columns[1]);            
            Map<String, Integer> wHisto = wordsFreqs.get(o);
            Map<String, Integer> lHisto = lemmataFreqs.get(o);
            for (String w : columns[3].split(" ")) {
                increment(wHisto, w);
                increment(allWordFreqs, w);
            }
            for (String l : columns[4].split(" ")) {
                increment(lHisto, l);
                increment(allLemmataFreqs, l);
            }
            line = in.readLine();
            if (++lines % 1000 == 0) {
                System.out.println("Analyzed " + lines + " headlines");
            }
        }
        System.out.println("Finished analysis of " + lines + " headlines");
        in.close();
        
        String outBase = args[1] + "-" + threshold;
        BufferedWriter out = new BufferedWriter(new FileWriter(outBase + "-words.txt"));
        for (Outlet o : Outlet.values()) {
            int totalCount = 0;
            out.append(o + ":"); out.newLine();
            for (Entry<String, Integer> kv : sortedEntries(wordsFreqs.get(o))) {
                int ct = kv.getValue();
                totalCount += ct;
                if (ct >= threshold) {
                    out.append(kv.getKey() + "\t" + ct); out.newLine();                    
                }
            }
            out.append("TOTAL\t" + totalCount); out.newLine();
            out.append("--------"); out.newLine();
            out.newLine();
            out.flush();
        }
        out.append("ALL:"); out.newLine();
        int totalTotalCount = 0;
        for (Entry<String, Integer> kv : sortedEntries(allWordFreqs)) {
            int ct = kv.getValue();
            totalTotalCount += ct;
            if (ct >= threshold) {
                out.append(kv.getKey() + "\t" + ct); out.newLine();                    
            }
        }
        out.append("TOTAL\t" + totalTotalCount); out.newLine();
        out.append("--------"); out.newLine();
        
        out.flush();
        out.close();
        
        out = new BufferedWriter(new FileWriter(outBase + "-lemmata.txt"));
        for (Outlet o : Outlet.values()) {
            int totalCount = 0;
            out.append(o + ":"); out.newLine();
            for (Entry<String, Integer> kv : sortedEntries(lemmataFreqs.get(o))) {
                int ct = kv.getValue();
                totalCount += ct;
                if (ct >= threshold) {
                    out.append(kv.getKey() + "\t" + ct); out.newLine();                    
                }
            }
            out.append("TOTAL\t" + totalCount); out.newLine();
            out.append("--------"); out.newLine();
            out.newLine();
            out.flush();
        }
        out.append("ALL:"); out.newLine();
        totalTotalCount = 0;
        for (Entry<String, Integer> kv : sortedEntries(allLemmataFreqs)) {
            int ct = kv.getValue();
            totalTotalCount += ct;
            if (ct >= threshold) {
                out.append(kv.getKey() + "\t" + ct); out.newLine();                    
            }
        }
        out.append("TOTAL\t" + totalTotalCount); out.newLine();
        out.append("--------"); out.newLine();
        
        out.flush();
        out.close();
        System.out.println("Done!");
    }

    public static List<Entry<String, Integer>> sortedEntries(Map<String, Integer> map) {
        Set<Entry<String, Integer>> entries = map.entrySet();
        ArrayList<Entry<String, Integer>> ret = new ArrayList<>(entries.size());
        ret.addAll(entries);
        Collections.sort(ret, new Comparator<Entry<String, Integer>>() {

            @Override
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                return Integer.compare(o2.getValue(), o1.getValue());
            }
            
        });
        return ret;
    }

    public static void increment(Map<String, Integer> wHisto, String w) {
        Integer curr = wHisto.get(w);
        if (curr == null) {
            wHisto.put(w, 1);
        } else {
            wHisto.put(w, curr + 1);
        }
    }

}
