package media_analysis.mining;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

import media_analysis.Outlet;
import media_analysis.utils.Consts;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Extracts headlines from html files, in chronological order and with duplicate unification.
 * Output format: last date headline appeared \tab outlet name \tab number of consecutive appearances \tab headline text
 * @author yuvalp
 *
 */
public class ExtractTitlesFromHtml extends Consts {

    private static final DateFormat df = new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH);    
    
    private String baseDir;
    private String[] dateDirs;
    
    public ExtractTitlesFromHtml(String iBaseDir) throws ParseException {
        iBaseDir = iBaseDir.replaceAll("\\\\", "/");
        baseDir = iBaseDir.endsWith("/") ? iBaseDir : iBaseDir + "/";
        initDateDirs();
    }

    private void initDateDirs() throws ParseException {
        TreeMap<Date, String> dateDirList = new TreeMap<>();
        File[] dirs = new File(baseDir + Outlet.HAARETZ.dirName()).listFiles();
        for (File d : dirs) {
            String dirName = d.getName();
            dateDirList.put(df.parse(dirName), dirName);
        }
        dateDirs = dateDirList.values().toArray(new String[dateDirList.size()]);
    }

    private String getPage(Outlet o, String date) {
        return baseDir + o.dirName() + "/" + date + "/" + o.domain() + o.indexFile();
    }

    private String getHeadline(Outlet o, String date) throws IOException {
        String demoPage = getPage(o, date);
        Document doc = Jsoup.parse(new File(demoPage), o.charset());
        return findHeadlinesRecursively(doc, o).replaceAll("[0-9]+", "0");
    }

    private static String findHeadlinesRecursively(Element doc, Outlet o) {
        StringBuilder sb = new StringBuilder();
        if (o.isMainHeadline(doc)) {
            sb.append(doc.text());
        }
        for (Element x : doc.children()) {
            String xHeadline = findHeadlinesRecursively(x, o);
            if (xHeadline.length() != 0) {
                sb.append(" " + xHeadline);
            }
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) throws IOException, ParseException {
        if (args.length < 2) {
            System.out.println("Usage: ExtractTitlesFromHtml <in-dir> <out-location>");
            return;
        }
        
        ExtractTitlesFromHtml extractor = new ExtractTitlesFromHtml(args[0]);
        extractor.extract(args[1]);
        
    }

    public void extract(String outLocation) throws IOException {
        
        // initialize counters etc.
        int numOfOutlets = Outlet.values().length;
        MiningOutletStats[] aggregateStats = new MiningOutletStats[numOfOutlets];
        String[] currHeadlines = new String[numOfOutlets];
        int[] headEpoch = new int[numOfOutlets];
        for (int i = 0; i < numOfOutlets; i++) {
            currHeadlines[i] = null;
            headEpoch[i] = 0;
            aggregateStats[i] = new MiningOutletStats();
        }

        OutputStreamWriter out = new OutputStreamWriter(new PrintStream(new File(outLocation), "UTF8"), "UTF8");
        
        String lastDate = "";
        int written = 0;
        for (String d : dateDirs) {
            for (Outlet o : Outlet.values()) {
                int i = o.ordinal();
                MiningOutletStats stats = aggregateStats[i];
                String currHead = currHeadlines[i];

                String h = "";
                try {
                    h = getHeadline(o, d);
                } catch (Exception e) {
                    stats.headlinesNotFound++;
                    continue;
                }
                if (currHead == null) {
                    currHeadlines[i] = h;
                    headEpoch[i] = 1;
                    continue;
                }
                if (h.isEmpty()) {
                    stats.emptyHeadlines++;
                    continue;
                }
                if (!h.equals(currHead)) {
                    out.append(d + "\t" + o + "\t" + headEpoch[i] + "\t" + currHeadlines[i] + "\n");
                    currHeadlines[i] = h;
                    headEpoch[i] = 1;
                } else {
                    headEpoch[i]++;
                }
            }
            written++;
            if (written % 100 == 0) {
                out.flush();
                System.out.println("wrote " + written);
            }
            lastDate = d;
        }
        for (Outlet o : Outlet.values()) {
            int i = o.ordinal();
            out.append(lastDate + "\t" + o + "\t" + headEpoch[i] + "\t" + currHeadlines[i] + "\n");
        }
        out.flush();
        out.close();
        System.out.println("Done! Total = " + written);
        System.out.println();
        System.out.println("outlet\t" + MiningOutletStats.header());
        for (Outlet o : Outlet.values()) {
            int i = o.ordinal();
            System.out.println(o + "\t" + aggregateStats[i]);
        }
    }

}
