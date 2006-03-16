package teamdash.wbs;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/** Class for managing a custom keystroke/action mapping */
public class ActionMapping {

    KeyStroke keyStroke;

    Object actionKey;

    Action action;

    /** Create a new action mapping */
    public ActionMapping(int keyCode, int modifiers, Object actionKey,
            Action action) {
        this(KeyStroke.getKeyStroke(keyCode, modifiers), actionKey, action);
    }

    /** Create a new action mapping */
    public ActionMapping(KeyStroke key, Object actionKey, Action action) {
        this.keyStroke = key;
        this.actionKey = actionKey;
        this.action = action;
    }

    /** Install this action mapping into a component */
    public void install(JComponent component) {
        install(component, JComponent.WHEN_FOCUSED);
    }

    /** Install this action mapping into a component */
    public void install(JComponent component, int condition) {
        component.getInputMap(condition).put(keyStroke, actionKey);
        component.getActionMap().put(actionKey, action);
    }

    /** override hashcode and equals to defer to our KeyStroke field */
    public int hashCode() {
        return this.keyStroke.hashCode();
    }

    public boolean equals(Object obj) {
        return (obj instanceof ActionMapping && ((ActionMapping) obj).keyStroke
                .equals(this.keyStroke));
    }
}
