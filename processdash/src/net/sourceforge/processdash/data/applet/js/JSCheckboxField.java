
package net.sourceforge.processdash.data.applet.js;

import net.sourceforge.processdash.data.repository.Repository;

public class JSCheckboxField extends JSField {


    public JSCheckboxField(
        String id,
        String dataName,
        JSFieldManager mgr,
        Repository data,
        String dataPath) {
        super(id, dataName, mgr, data, dataPath);
    }

    public void fetch() {
        variantValue = i.getBoolean();
    }

    public void parse() {
        variantValue = new Boolean(isChecked(jsValue));
    }

    private boolean isChecked(Object checked) {
        if (checked == null) return false;
        if (checked instanceof Boolean) return ((Boolean) checked).booleanValue();
        if (checked instanceof Number) return ((Number) checked).intValue() > 0;
        String s = checked.toString();
        if (s == null || s.length() == 0) return false;
        return ("1yYtTcC".indexOf(s.charAt(0)) != -1);
    }

}
