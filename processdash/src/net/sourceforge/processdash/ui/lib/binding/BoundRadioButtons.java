// Copyright (C) 2009-2012 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib.binding;

import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class BoundRadioButtons extends JPanel implements ActionListener {

    /** The tags used to define the various options */
    private static final String OPTION_TAG = "option";
    private static final String OPTION_ID_TAG = "id";
    private static final String OPTION_VALUE_TAG = "value";
    private static final String OPTION_DEFAULT_TAG = "default";

    /** The buttons should be indented by 30 pixels relative to the
     *   grouping title */
    private static final int BUTTONS_LEFT_INDENT = 30;

    private BoundMap map;
    private String propertyName;
    private String resourcesId;
    private JPanel buttonsPannel;
    private ButtonGroup buttonGroup;

    protected BoundRadioButtons() { }

    public BoundRadioButtons(BoundMap map, Element xml) {
        this.map = map;
        String propertyName = xml.getAttribute("id");
        String resourcesId = propertyName;
        Map<String, Option> options = getOptions(resourcesId, xml, map.getResources());

        init(map, propertyName, resourcesId, options);
    }

    protected void init(BoundMap map,
                        String propertyName,
                        String resourcesId,
                        Map<String, Option> options) {
        this.map = map;
        this.propertyName = propertyName;
        this.resourcesId = resourcesId;

        buttonsPannel = getButtonPanel(options);
        buttonsPannel.setBorder(
            BorderFactory.createEmptyBorder(0, BUTTONS_LEFT_INDENT, 0, 0));
        this.add(buttonsPannel);

        Object listener = EventHandler.create(PropertyChangeListener.class,
            this, "updateFromMap");
        map.addPropertyChangeListener(this.propertyName, (PropertyChangeListener) listener);

        updateFromMap();

        // The border has to be created once all components are drawn in order
        //  for its width to be computed correctly
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createBorder();
            }
        });
    }

    private void createBorder() {
        String label = map.getResource(resourcesId + ".Border_Label");

        FontMetrics metrics = this.getFontMetrics(this.getFont());
        int buttonsPannelWidth = buttonsPannel.getWidth();
        int borderLabelWidth = metrics.stringWidth(label);
        int rightPadding = buttonsPannelWidth > borderLabelWidth ?
                            0 : borderLabelWidth - buttonsPannelWidth;

        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createEmptyBorder(0, 0, 0, rightPadding),
            label);
        this.setBorder(border);
    }

    private JPanel getButtonPanel(Map<String, Option> options) {
        JPanel buttonPannel = new JPanel(new GridLayout(0, 1));

        buttonGroup = new ButtonGroup();
        JRadioButton button = null;
        Option option = null;

        for (Map.Entry<String, Option> optionDefinition : options.entrySet()) {
            option = optionDefinition.getValue();

            button = new JRadioButton(option.label);
            button.setActionCommand(option.value);
            button.addActionListener(this);
            button.setSelected(option.isDefaultOption);
            button.putClientProperty(Option.class, option);

            buttonGroup.add(button);
            buttonPannel.add(button);
        }

        if (buttonGroup.getSelection() == null) {
            // Since no button is selected, we select the first one

            Enumeration<AbstractButton> buttons = buttonGroup.getElements();
            if (buttons.hasMoreElements()) {
                buttons.nextElement().setSelected(true);
            }
        }

        return buttonPannel;
    }

    /**
     *  This method iterates through all child elements of the xml definition of this
     *  widget to find defined options. It then return an id-option mapping of those
     *  options.
     */
    protected Map<String, Option> getOptions(String widgetResourcesId,
                                             Element xml,
                                             ResourceBundle resources) {
        Map<String, Option> options = new LinkedHashMap<String, Option>();

        for (Element element : XMLUtils.getChildElements(xml)) {

            if (OPTION_TAG.equals(element.getTagName())) {
                String optionId = element.getAttribute(OPTION_ID_TAG);
                String label =
                    resources.getString(widgetResourcesId + "." + optionId + ".Label");
                String value = element.getAttribute(OPTION_VALUE_TAG);
                boolean isDefault =
                    Boolean.TRUE.toString().equalsIgnoreCase(
                        element.getAttribute(OPTION_DEFAULT_TAG));

                options.put(optionId, new Option(optionId, label, value, isDefault));
            }
        }

        return options;
    }

    public void actionPerformed(ActionEvent e) {
        map.put(this.propertyName, e.getActionCommand());
    }

    public void updateFromMap() {
        Object value = map.get(this.propertyName);

        boolean buttonFound = false;
        if (value != null && value instanceof String) {
            Enumeration<AbstractButton> buttons = buttonGroup.getElements();

            while (buttons.hasMoreElements() && !buttonFound) {
                AbstractButton button = buttons.nextElement();

                if (((String) value).equalsIgnoreCase(button.getActionCommand())) {
                    button.setSelected(true);
                    buttonFound = true;
                }
            }
        }

        if (!buttonFound) {
            Enumeration<AbstractButton> buttons = buttonGroup.getElements();
            while (buttons.hasMoreElements() && !buttonFound) {
                AbstractButton button = buttons.nextElement();
                Option option = (Option) button.getClientProperty(Option.class);
                if (option != null && option.isDefaultOption) {
                    button.setSelected(true);
                    buttonFound = true;
                }
            }
        }
    }

    /**
     * A class representing a possible choice
     */
    protected static class Option {
        protected String id;
        protected String label;
        protected String value;
        protected boolean isDefaultOption;

        public Option(String id, String label, String value, boolean isDefaultOption) {
            this.id = id;
            this.label = label;
            this.value = value;
            this.isDefaultOption = isDefaultOption;
        }
    }

}
