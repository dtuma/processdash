// Copyright (C) 2005 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.ui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.beans.EventHandler;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.sourceforge.processdash.ui.lib.WrappingText;

public abstract class WizardPanel extends JPanel {

    protected static final int SPACING = 5;

    protected Wizard wizard;

    private String resourcePrefix;

    protected JButton backButton, nextButton, cancelButton;

    public WizardPanel(Wizard wizard, String resourcePrefix) {
        this.wizard = wizard;
        this.resourcePrefix = resourcePrefix + ".";
    }

    protected String getAbsoluteString(String key) {
        return Wizard.resources.getString(key);
    }

    protected String getRelativeString(String key) {
        return Wizard.resources.getString(resourcePrefix + key);
    }

    protected String getString(String key) {
        try {
            return getRelativeString(key);
        } catch (Exception e) {
            return getAbsoluteString(key);
        }
    }

    protected String getOptionalRelativeString(String key) {
        try {
            return getRelativeString(key);
        } catch (Exception e) {
            return null;
        }
    }

    protected Component indentedComponent(int indent, Component c) {
        Box result = Box.createHorizontalBox();
        result.add(horizSpace(indent));
        result.add(c);
        result.add(Box.createHorizontalGlue());
        return result;
    }

    protected Component horizSpace(int mult) {
        return Box.createHorizontalStrut(SPACING * mult);
    }

    protected Component verticalSpace(int mult) {
        return Box.createVerticalStrut(SPACING * mult);
    }

    public void doBack() {
        wizard.goBackward();
    }

    public abstract void doNext();

    public void doCancel() {
        wizard.close();
    }

    protected void buildUserInterface() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel promptLabel = new JLabel(getRelativeString("Prompt"));
        promptLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(indentedComponent(0, promptLabel));
        add(verticalSpace(2));

        String header = getOptionalRelativeString("Header");
        if (header != null) {
            add(indentedComponent(2, new WrappingText(header)));
            add(verticalSpace(2));
        }

        buildMainPanelContents();

        add(verticalSpace(2));

        String footer = getOptionalRelativeString("Footer");
        if (footer != null) {
            add(indentedComponent(2, new WrappingText(footer)));
            add(verticalSpace(2));
        }

        add(createButtonBox());
    }

    protected void buildMainPanelContents() {
        // do nothing
    }

    protected Component createButtonBox() {
        Box buttons = Box.createHorizontalBox();
        buttons.add(Box.createHorizontalGlue());

        String backLabel = getBackButtonLabel();
        if (backLabel != null && wizard.hasPanels()) {
            backButton = new JButton(backLabel);
            backButton.addActionListener((ActionListener) EventHandler.create(
                    ActionListener.class, this, "doBack"));
            buttons.add(backButton);
        }

        String nextLabel = getNextButtonLabel();
        if (nextLabel != null) {
            buttons.add(horizSpace(1));
            nextButton = new JButton(nextLabel);
            nextButton.addActionListener((ActionListener) EventHandler.create(
                    ActionListener.class, this, "doNext"));
            buttons.add(nextButton);
        }

        String cancelLabel = getCancelButtonLabel();
        if (cancelLabel != null) {
            buttons.add(horizSpace(1));
            cancelButton = new JButton(cancelLabel);
            cancelButton.addActionListener((ActionListener) EventHandler
                    .create(ActionListener.class, this, "doCancel"));
            buttons.add(cancelButton);
        }

        Box verticalBox = Box.createVerticalBox();
        addBottomPadding(verticalBox);
        verticalBox.add(verticalSpace(2));
        verticalBox.add(buttons);

        return verticalBox;
    }

    protected void addBottomPadding(Box verticalBox) {
        JLabel spacer = new JLabel(" ");
        Dimension d = new Dimension(10, 999);
        spacer.setPreferredSize(d);
        spacer.setMaximumSize(d);
        verticalBox.add(spacer);
    }

    protected String getCancelButtonLabel() {
        return getAbsoluteString("Cancel_Button");
    }

    protected String getNextButtonLabel() {
        return getAbsoluteString("Next_Button");
    }

    protected String getBackButtonLabel() {
        return getAbsoluteString("Back_Button");
    }
}
