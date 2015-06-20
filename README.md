# MediaAnalysis
Extraction and analysis of data from Israeli news sites

Yuval Pinter, 2015 (GPL v3 license)

This project contains code and data used for the analysis of headlines in the following Israeli news sites:
ynet, Israel Hayom, Haaretz, Maariv, nrg, Mako, walla.

Sequentially, we have the following:
- Headline Extraction: given a directory structure such as the one in data/html-dir-sample, create a single line record for each single headline per site, replacing immediate duplicates (lingering headlines) with the number of consecutive timestamps they appeared in. Location: media_analysis.mining.ExtractTitlesFromHtml
    - Compatible with html formats of all sites starting July 1, 2014, except: Maariv (Aug. 27, 2014), Haaretz (Nov. 25, 2014), and Walla (Jan 25, 2015).
- Tokenization addition: given the output of previous step, run Lucene's Hebrew Morphological Analyzer (part of the hspell project - Har’el and Kenigsberg, 2012) and append each line with a tokenized version. Location: media_analysis.nlp.AddTokenization
- DATA: full output of the code so far, under data/tok-cann-all-headlines.txt
- Auxiliary data: WORDLIST's (Hermit Dave) corpus count of Hebrew tokens (frequency >=5), under data/wordlists-freqs-2012.txt
- Auxiliary data: Linzen 2004's corpus count of Hebrew lemmata (frequency >=5), under data/israblog-freqs.txt
- Feature extraction in Weka format from DATA, using auxiliary tables, under media_analysis.features.NewsOutletPredictorFeatureExtractor (full documentation to follow). Demo configuration file in conf/conf.txt
- Weka format feature files under data. Best-performing feature set with Weka's RandomForest on default parameters under data/weka-cann-out-4538.arff, and performance summaries under data/4538-summary.txt

Notes:
- Consult .classpath for external resources to be downloaded and included (jsoup, lucene + hebmorph, weka)
- build.xml can be run to produce a JAR file which, when put in a directory alongside jsoup, can be used to extract titles from an HTML file dir structure via script.

=================
Initial results and release of first dataset and code were announced at the Israeli Seminar on Computational Linguistics (ISCOL) on June 22, 2015 ( http://www.openu.ac.il/ISCOL2015 )

The code was written by Yuval Pinter, blogger at http://dagesh.wordpress.com and Research Engineer at Yahoo Labs: http://labs.yahoo.com/author/yuvalp/

Contact: yuvalpinter@gmail.com

*Yahoo Labs and Yahoo Inc. are not related to this project*

Raw data was supplied by Oren Persico and Shuki Tausig of Ha-ayin Ha-Shvi'it: http://the7eye.org.il .
