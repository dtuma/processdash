package teamdash.templates.setup;

import net.sourceforge.processdash.util.HTMLUtils;

public class MoveProjectException extends Exception {

    String page;

    String query;

    MoveProjectException(String page, String query) {
        this.page = page;
        this.query = query;
    }

    MoveProjectException(String query) {
        this(null, query);
    }

    MoveProjectException(Throwable t) {
        this("generalError");
        initCause(t);
    }

    MoveProjectException append(String name, String value) {
        if (value != null)
            query = query + "&" + name + "=" + HTMLUtils.urlEncode(value);
        return this;
    }

}
