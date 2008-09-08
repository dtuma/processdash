package teamdash.templates.setup;

import net.sourceforge.processdash.util.HTMLUtils;

class MigrationException extends Exception {

    private StringBuffer url;

    MigrationException() {
        this.url = new StringBuffer("migrateError.shtm");
    }

    MigrationException(String query) {
        this();
        add(query);
    }

    public MigrationException(Throwable t) {
        this("generalError");
        initCause(t);
    }

    public MigrationException add(String query) {
        HTMLUtils.appendQuery(url, query);
        return this;
    }

    public MigrationException add(String name, String value) {
        HTMLUtils.appendQuery(name, value);
        return this;
    }

    public String getURL() {
        return url.toString();
    }

}
