package media_analysis;

import org.jsoup.nodes.Element;

public enum Outlet {
    
    YNET("yn"), ISRAEL_HAYOM("ih"), HAARETZ("ha"), MAARIV("mv"), NRG("nr"), MAKO("mk"), WALLA("wa");

    public String domain() {
        return "www." + dirName().replace("_", "") + ".co.il/";
    }
    
    private Outlet(String iCode) {
        code = iCode;
    }
    
    private String code;

    public static String toCode(String rep) {
        return valueOf(rep).code;
    }
    
    public String code() {
        return code;
    }

    public String dirName() {
        return this.name().toLowerCase();
    }

    public String indexFile() {
        return (this == YNET ? "home/0,7340,L-8,00" : "index") + ".html";
    }

    public boolean isMainHeadline(Element doc) {
        switch (this) {
            case HAARETZ:
            case MAARIV:
            case NRG:
                return doc.nodeName().contains("h1");
            case YNET:
                return doc.classNames().contains("blkbigheader");
            case ISRAEL_HAYOM:
                return doc.parent() != null && doc.parent().nodeName().equals("a")
                                && doc.classNames().contains("title");
            case MAKO:
                return doc.parent() != null && doc.parent().parent() != null
                                && doc.parent().parent().parent() != null
                                && doc.parent().parent().parent().parent() != null
                                && doc.parent().parent().parent().parent().classNames().contains("mainItem");
            case WALLA:
                return doc.parent() != null && doc.parent().parent() != null
                                && doc.parent().parent().classNames().contains("hp-main-article")
                                && doc.nodeName().contains("h3");
        }

        return false;
    }

    public String charset() {
        if (this == NRG) {
            return "windows-1255";
        }
        return "UTF8";
    }
    
}