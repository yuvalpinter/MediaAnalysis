package media_analysis.features;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import media_analysis.Outlet;

import com.sun.org.apache.xerces.internal.impl.io.UTF8Reader;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffSaver;

public class FeatureExtractor {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: FeatureExtractor <input> <output>\n"
                            + "output file recommended extension is .arff");
            return;
        }

        Attribute idAttr = new Attribute("ID", (List<String>) null);
        Attribute outletAttr = new Attribute("class", outletList());
        ArrayList<Attribute> attrs = new ArrayList<>();
        attrs.add(idAttr);
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

            Instance inst = new SparseInstance(0);
            inst.setValue(idAttr, columns[0] + ":" + columns[1]);

            // class: outlet
            inst.setValue(outletAttr, columns[1]);

            // time-based duplication
            // num of chars
            // num of punct
            // words
            // lemmata
            // affix letters

            instances.add(inst);
            written++;

            line = in.readLine();
        }
        in.close();

        String output = args[1];
        if (instances != null) {
            ArffSaver saver = new ArffSaver();
            saver.setInstances(instances);
            saver.setFile(new File(output));
            saver.writeBatch();
        }

        System.out.println("Finished! Wrote " + written + " vectors with " + badLineInputs + " bad inputs.");
    }

    private static List<String> outletList() {
        List<String> classes = new ArrayList<>();
        for (Outlet val : Outlet.values()) {
            classes.add(val.name());
        }
        return classes;
    }

}
