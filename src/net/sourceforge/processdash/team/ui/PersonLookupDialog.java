// Copyright (C) 2010-2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HTTPUtils;

public class PersonLookupDialog {

    // this constant must be kept in sync with the corresponding value in the
    // com.tuma_solutions.teamserver.jnlp.controller.LaunchDatasetJnlp class
    private static final String USER_LOOKUP_URL_SETTING =
        "com.tuma_solutions.teamserver.userLookup.URL";

    private static final int MAX_DIALOG_HEIGHT = 300;

    JDialog dialog;

    JScrollPane scrollPane;

    HtmlPane html;

    PersonLookupData person;


    public PersonLookupDialog(Window parent, PersonLookupData person)
            throws IOException {

        this.person = person;

        html = new HtmlPane();

        scrollPane = new JScrollPane(html,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    "cancelLookup");
        scrollPane.getActionMap().put("cancelLookup", new CancelAction());

        dialog = new JDialog(parent, "Team Member Details",
                ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().add(scrollPane);
        dialog.setSize(200, 30);

        // ask the editor pane to make the initial connection to the server
        // synchronously, so we can receive the IOException if that fails.
        html.getDocument().putProperty("load priority", -1);
        String url = getInitialUrl(person);
        html.setPage(url);

        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private String getInitialUrl(PersonLookupData person) {
        StringBuffer buf = new StringBuffer();
        buf.append(getLookupServerUrl());
        if (hasValue(person.getServerIdentityInfo()))
            HTMLUtils.appendQuery(buf, person.getServerIdentityInfo());
        else
            HTMLUtils.appendQuery(buf, "search", person.getName());
        return buf.toString();
    }


    public void onDocumentLoad(HTMLDocument htmlDoc, Element commandElem) {
        // get the string command that the document wants us to perform.
        String command = getInputValue(commandElem);
        if ("cancel".equals(command)) {
            dialog.dispose();
        } else if ("save".equals(command)) {
            saveData(htmlDoc);
            dialog.dispose();
        } else {
            processUserInterfaceChanges(htmlDoc);
        }
    }


    private void saveData(HTMLDocument htmlDoc) {
        // retrieve the display name, as found by the server's person lookup
        // logic.  If the server didn't find a display name (highly unusual),
        // leave whatever display name the user typed previously.
        String display = getInputValue(htmlDoc, "displayName");
        if (hasValue(display))
            person.setName(display.trim());

        // Save the query string that was provided by the server.
        String query = getInputValue(htmlDoc, "queryString");
        person.setServerIdentityInfo(query);
    }

    /** Find an INPUT element in the HTML document, and return its value. */
    private String getInputValue(HTMLDocument doc, String name) {
        Element e = doc.getElement(doc.getDefaultRootElement(),
            HTML.Attribute.NAME, name);
        if (e == null)
            return null;
        else
            return getInputValue(e);
    }

    private String getInputValue(Element e) {
        Object value = e.getAttributes().getAttribute(HTML.Attribute.VALUE);
        return (value == null ? null : value.toString());
    }


    private void processUserInterfaceChanges(HTMLDocument htmlDoc) {
        // if we find a title in the HTML doc, use it as the title of the dialog
        Object docTitle = htmlDoc.getProperty(HTMLDocument.TitleProperty);
        if (docTitle != null)
            dialog.setTitle(String.valueOf(docTitle));

        // enlarge the dialog if necessary to fit the contents.
        Dimension pref = html.getPreferredSize();
        Dimension curr = scrollPane.getViewport().getSize();
        Dimension d = dialog.getSize();

        // First, test to see if the dialog needs to be taller.  If so,
        // enlarge it.
        double yDelta = pref.getHeight() + 5 - curr.getHeight();
        if (yDelta > 0) {
            d.height = (int) Math.min(d.height + yDelta, MAX_DIALOG_HEIGHT);
            dialog.setSize(d);
            dialog.validate();
            // changing the dialog height could have caused a vertical
            // scrollbar to appear or disappear.  Just in case, get the new
            // size of the viewport so the horizontal resizing logic can use it.
            curr = scrollPane.getViewport().getSize();
        }

        // Now test to see if the dialog needs to be wider.  If so, enlarge it.
        double xDelta = pref.getWidth() + 5 - curr.getWidth();
        if (xDelta > 0) {
            d.width = (int) (d.width + xDelta);
            dialog.setSize(d);
            dialog.validate();
        }

        // if the size of the window changed, reposition it so its center
        // stays in the same location as before
        if (yDelta > 0 || xDelta > 0) {
            Point location = dialog.getLocation();
            location.setLocation( //
                Math.max(0, location.x - Math.max(0, xDelta / 2)),
                Math.max(0, location.y - Math.max(0, yDelta / 2)));
            dialog.setLocation(location);
        }

        // scan the HTML document, and arrange for all text fields to perform
        // a "select all" when they receive the focus.  Also, arrange for all
        // buttons to accept Enter as an activation key.
        tweakInputFields(html);

        // if one of the elements on the form has the ID "grabFocus," arrange
        // for it to receive focus immediately.
        Element e = htmlDoc.getElement("grabFocus");
        JComponent c = findComponentForInputElement(e);
        if (c != null)
            new GrabFocus(c);
    }

    private void tweakInputFields(Container c) {
        if (c instanceof JTextField) {
            ((JTextComponent) c).addFocusListener(SELECT_ALL);
        } else if (c instanceof JButton) {
            enableEnterKey((JButton) c);
        } else {
            for (int i = c.getComponentCount(); i-- > 0;) {
                Component child = c.getComponent(i);
                if (child instanceof Container)
                    tweakInputFields((Container) child);
            }
        }
    }

    /** Pressing the space bar activates the focused button.  This method
     * will allow the Enter key to have the same effect. */
    private void enableEnterKey(JButton c) {
        InputMap m = c.getInputMap();
        Object key = m.get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false));
        m.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), key);
        key = m.get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true));
        m.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), key);
    }

    /** Assuming e is an INPUT element in an HTML document, find the JComponent
     * that is acting as its view.  Currently only supports text fields and
     * buttons. */
    private JComponent findComponentForInputElement(Element e) {
        if (e == null)
            return null;
        Object model = e.getAttributes().getAttribute(
            StyleConstants.ModelAttribute);
        if (model == null)
            return null;
        return findComponentWithModel(html, model);
    }

    private JComponent findComponentWithModel(Container c, Object model) {
        if (c instanceof JButton) {
            JButton button = (JButton) c;
            if (button.getModel() == model)
                return button;

        } else if (c instanceof JTextField) {
            JTextField textField = (JTextField) c;
            if (textField.getDocument() == model)
                return textField;

        } else {
            for (int i = c.getComponentCount(); i-- > 0;) {
                Object child = c.getComponent(i);
                if (child instanceof Container) {
                    JComponent result = findComponentWithModel(
                        (Container) child, model);
                    if (result != null)
                        return result;
                }
            }
        }
        return null;
    }

    public static boolean isLookupServerConfigured() {
        return getLookupServerUrl() != null;
    }

    public static boolean isTeamMemberLookupRequired() {
        String url = getLookupServerUrl();
        return HTMLUtils.parseQuery(url).containsKey("pa");
    }

    private static String getLookupServerUrl() {
        String result = System.getProperty(USER_LOOKUP_URL_SETTING);
        if (hasValue(result))
            return result;
        else
            return null;
    }

    private static boolean hasValue(String s) {
        return s != null && s.trim().length() > 0;
    }

    private class HtmlPane extends JEditorPane implements DocumentListener,
            HyperlinkListener, ActionListener {

        Timer documentChangeTimer;

        Document doc = null;

        public HtmlPane() {
            documentChangeTimer = new Timer(50, this);
            documentChangeTimer.setRepeats(false);

            setContentType("text/html");
            setEditable(false);
            addHyperlinkListener(this);
            setBackground(null);
        }

        /** Messaged when the documentChangeTimer fires */
        public void actionPerformed(ActionEvent e) {
            Object doc = getDocument();
            if (doc instanceof HTMLDocument) {
                HTMLDocument html = (HTMLDocument) doc;
                Element commandElem = html.getElement("command");
                if (commandElem != null)
                    onDocumentLoad(html, commandElem);
            }
        }

        @Override
        public void setDocument(Document newDocument) {
            if (this.doc != null)
                this.doc.removeDocumentListener(this);

            this.doc = newDocument;

            if (this.doc != null)
                this.doc.addDocumentListener(this);

            super.setDocument(newDocument);
        }

        public void changedUpdate(DocumentEvent e) {
            documentChangeTimer.restart();
        }

        public void insertUpdate(DocumentEvent e) {
            documentChangeTimer.restart();
        }

        public void removeUpdate(DocumentEvent e) {
            documentChangeTimer.restart();
        }

        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    html.setPage(e.getURL());
                } catch (IOException ioe) {
                    Toolkit.getDefaultToolkit().beep();
                    dialog.dispose();
                    ioe.printStackTrace();
                }
            }
        }

    }


    private static class SelectAll implements FocusListener {

        public void focusGained(FocusEvent e) {
            Component c = e.getComponent();
            if (c instanceof JTextComponent) {
                JTextComponent tc = (JTextComponent) c;
                tc.selectAll();
            }
        }

        public void focusLost(FocusEvent e) {}

    }
    private static final SelectAll SELECT_ALL = new SelectAll();


    private static class GrabFocus implements ActionListener {

        private JComponent c;

        public GrabFocus(JComponent c) {
            this.c = c;

            Timer t = new Timer(50, this);
            t.setRepeats(false);
            t.start();
        }

        public void actionPerformed(ActionEvent e) {
            c.requestFocus();
        }

    }


    private class CancelAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            dialog.dispose();
        }

    }

    public static String lookupNameForUser(String username) {
        String lookupServerUrl = getLookupServerUrl();
        if (username == null || lookupServerUrl == null)
            return null;

        StringBuffer urlStr = new StringBuffer(lookupServerUrl);
        HTMLUtils.appendQuery(urlStr, "search", username);
        HTMLUtils.appendQuery(urlStr, "save", "Save");
        try {
            URL url = new URL(urlStr.toString());
            String doc = HTTPUtils.getResponseAsString(url.openConnection());
            return getInputValue(doc, "displayName");
        } catch (Exception e) {
            return null;
        }
    }

    private static String getInputValue(String doc, String inputName) {
        // find a tag in this document with the given name
        String nameAttr = "name=\"" + inputName + "\"";
        int pos = doc.indexOf(nameAttr);
        if (pos == -1)
            return null;

        // find the beginning and end of the enclosing tag
        int beg = doc.lastIndexOf('<', pos);
        int end = doc.indexOf('>', pos + nameAttr.length());
        if (beg == -1 || end == -1)
            return null;

        // extract the value and return it
        String tag = doc.substring(beg, end);
        Matcher m = VALUE_ATTR_PAT.matcher(tag);
        if (m.find())
            return HTMLUtils.unescapeEntities(m.group(1));
        else
            return null;
    }

    private static final Pattern VALUE_ATTR_PAT = Pattern.compile(
        "\\bvalue=\"([^'\"]+)\"", Pattern.CASE_INSENSITIVE);

}
