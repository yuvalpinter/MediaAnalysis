package media_analysis;

import org.jsoup.nodes.Element;

public enum Outlet {
    
    YNET, ISRAEL_HAYOM, HAARETZ, MAARIV, NRG, MAKO, WALLA;

    public String domain() {
        return "www." + dirName().replace("_", "") + ".co.il/";
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