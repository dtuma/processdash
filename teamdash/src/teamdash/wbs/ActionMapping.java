// Copyright (C) 2002-2013 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

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
    public ActionMapping(Object actionKey, Action action) {
        this((KeyStroke) action.getValue(Action.ACCELERATOR_KEY), actionKey,
                action);
    }

    /** Create a new action mapping */
    public ActionMapping(KeyStroke key, Object actionKey, Action action) {
        if (key == null)
            throw new IllegalArgumentException("KeyStroke required");
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
        install(component, condition, action);
    }

    protected void install(JComponent component, int condition,
            Action actionToInstall) {
        component.getInputMap(condition).put(keyStroke, actionKey);
        component.getActionMap().put(actionKey, actionToInstall);
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
