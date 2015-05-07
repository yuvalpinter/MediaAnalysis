package media_analysis.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import media_analysis.utils.Consts;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.hebrew.MorphAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.Version;

public class AddTokenization extends Consts {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        
        if (args.length < 2) {
            System.out.println("Usage: AddTokenization <in-location> <out-location>");
            return;
        }
        
        Analyzer an = new MorphAnalyzer(Version.LUCENE_4_9, "");

        FileWriter out = new FileWriter(new File(args[1]));
        BufferedReader fr = new BufferedReader(new FileReader(args[0]));
        String l = fr.readLine();
        TokenStream ts = null;
        while (l != null) {
            String[] cols = l.split("\\t");
            // time changed \t outlet \t epochs survived \t headline
            if (cols.length != 4) {
                continue;
            }
            ts = an.tokenStream("", new StringReader(cols[3]));
            ts.reset();
            StringBuilder sb = new StringBuilder();
            while (ts.incrementToken()) {
                String term = ts.getAttribute(CharTermAttribute.class).toString();
                int posIncrement = ts.getAttribute(PositionIncrementAttribute.class).getPositionIncrement();
                if (posIncrement > 0) {
                    sb.append(" ").append(term);
                }
            }
            out.append(l + "\t" + sb.toString().trim() + "\n");
            l = fr.readLine();
            ts.close();
        }
        out.flush();
        out.close();
        an.close();
        fr.close();
        System.out.println("Done!");
    }

}
