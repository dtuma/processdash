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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.naming.OperationNotSupportedException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.WrappingText;

public abstract class ChoicePanel extends WizardPanel implements ActionListener {

    private String[] choices;

    private String selectedChoice;

    private String lastSelectedChoice;

    private WizardPanel lastSelectedPanel;

    public ChoicePanel(Wizard wizard, String resourcePrefix, String[] choices) {
        super(wizard, resourcePrefix);
        this.choices = choices;
        buildUserInterface();

        nextButton.setEnabled(false);
    }


    protected void buildMainPanelContents() {
        ButtonGroup buttonGroup = new ButtonGroup();

        for (int i = 0; i < choices.length; i++) {
            add(verticalSpace(2));

            String choice = choices[i];
            String title = getRelativeString("Choices." + choice + ".Title");
            String description = getOptionalRelativeString("Choices." + choice
                    + ".Description");
            String helpTopic = getOptionalRelativeString("Choices." + choice
                    + ".HelpTopicID_");

            JRadioButton radioButton = new JRadioButton(title);
            radioButton.setHorizontalAlignment(SwingConstants.LEFT);
            radioButton.setActionCommand(choice);
            radioButton.addActionListener(this);
            buttonGroup.add(radioButton);

            if (helpTopic != null)
                PCSH.enableHelpOnButton(radioButton, helpTopic);

            add(indentedComponent(2, radioButton));
            add(verticalSpace(1));

            if (description != null) {
                add(indentedComponent(6, new WrappingText(description)));
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source instanceof JRadioButton) {
            selectedChoice = e.getActionCommand();
            nextButton.setEnabled(true);
        }
    }

    public void doNext() {
        if (selectedChoice != null) {
            choiceSelected(selectedChoice);
        }
    }

    public void choiceSelected(String choice) {
        if (!choice.equals(lastSelectedChoice) || lastSelectedPanel == null) {
            lastSelectedChoice = choice;
            lastSelectedPanel = getPanelForChoice(choice);
        }
        wizard.goForward(lastSelectedPanel);
    }

    public WizardPanel getPanelForChoice(String choice) {
        throw new UnsupportedOperationException(
                "This method must be implemented by subclass");
    }
}
