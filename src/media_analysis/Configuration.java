package media_analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.sun.org.apache.xerces.internal.impl.io.UTF8Reader;

public class Configuration {

    public static final String INPUT_KEY = "features-input";
    public static final String OUTPUT_KEY = "features-output";

    public static final String OUTLETS_KEY = "features-outlets";
    public static final String WRITE_PAIRED_KEY = "features-write-paired-files";
    public static final String OPTIMIZE_FEATURES_KEY = "optimize-features";
    public static final String FEATURES_HAVE_IDS_KEY = "features-have-ids";
    
    public static final String START_DATE_KEY = "features-start-date";
    public static final String END_DATE_KEY = "features-end-date";
    
    public static final String WORD_FREQ_FILE_KEY = "features-word-freq-file";
    public static final String LEMMA_FREQ_FILE_KEY = "features-lemma-freq-file";
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    private Map<String, String> paramValues = new HashMap<>();
    private static final DateFormat df = new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH);
    
    @SuppressWarnings("resource")
    public Configuration(String fileName) throws IOException {
        BufferedReader confFile = new BufferedReader(new UTF8Reader(new FileInputStream(new File(fileName))));
        String line = confFile.readLine();
        while (line != null) {
            if (line.isEmpty() || line.startsWith("#")) {
                line = confFile.readLine();
                continue;
            }
            String[] kv = line.split(":");
            paramValues.put(kv[0], kv[1]);
            line = confFile.readLine();
        }
        confFile.close();
    }

    public String get(String key) {
        String val = paramValues.get(key);
        if (val == null) {
            return "";
        }
        return val.trim();
    }

    public Date getDate(String key) throws ParseException {
        return df.parse(get(key));
    }

    public boolean getBoolean(String key) {
        return get(key).equalsIgnoreCase("true");
    }

    public String[] getArray(String key) {
        return get(key).split(",");
    }

}
