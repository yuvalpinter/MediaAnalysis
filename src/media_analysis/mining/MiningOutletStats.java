package media_analysis.mining;

public class MiningOutletStats {
    
    public int headlinesNotFound = 0;
    public int emptyHeadlines = 0;
    
    public static String header() {
        return "headlines not found" + "\t" + "empty headlines";
    }
    
    @Override
    public String toString() {
        return headlinesNotFound + "\t" + emptyHeadlines;
    };

}
