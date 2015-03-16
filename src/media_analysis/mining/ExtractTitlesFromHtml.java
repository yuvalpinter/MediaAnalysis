package media_analysis.mining;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

import media_analysis.Outlet;
import media_analysis.utils.Consts;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ExtractTitlesFromHtml extends Consts {

    private static final String BASE_DIR = "C:/Users/yuvalp/Documents/sites/";
    private static final String DEMO_DATE = "Wed02-25-2015_12-15PM";

    private static final DateFormat df = new SimpleDateFormat(DATE_PATTERN);
    private static String[] dateDirs;

    private static void initDateDirs() throws ParseException {
        TreeMap<Date, String> dateDirList = new TreeMap<>();
        File[] dirs = new File(BASE_DIR + Outlet.HAARETZ.dirName()).listFiles();
        for (File d : dirs) {
            String dirName = d.getName();
            dateDirList.put(df.parse(dirName), dirName);
        }
        dateDirs = dateDirList.values().toArray(new String[dateDirList.size()]);
    }

    private static String getPage(Outlet o, String date) {
        return BASE_DIR + o.dirName() + "/" + date + "/" + o.domain() + o.indexFile();
    }

    private static String getDemoPage(Outlet o) {
        return getPage(o, DEMO_DATE);
    }

    private static String getHeadline(Outlet o, String date) throws IOException {
        String demoPage = getPage(o, date);
        Document doc = Jsoup.parse(new File(demoPage), o.charset());
        return findHeadlinesRecursively(doc, o);
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
        initDateDirs();

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

        FileWriter out = new FileWriter(new File("out-headlines"));

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
                }
                if (!h.isEmpty() && !h.equals(currHead)) {
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
