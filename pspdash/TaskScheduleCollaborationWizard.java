// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import pspdash.data.DataRepository;
import pspdash.data.SimpleData;
import pspdash.data.StringData;


/**
 * Collaboration is only available for local task lists.
 */
public class TaskScheduleCollaborationWizard {

    private boolean isRollup;
    private JFrame frame;
    private DataRepository data;
    private String taskListName;

    /** Create and display a collaboration wizard.
     */
    public TaskScheduleCollaborationWizard(DataRepository data,
                                           String taskListName) {
        // check input parameters
        if (EVTaskListData.validName(taskListName) &&
            EVTaskListData.exists(data, taskListName))
            isRollup = false;

        else if (EVTaskListRollup.validName(taskListName) &&
                 EVTaskListRollup.exists(data, taskListName))
            // isRollup = true;
            throw new IllegalArgumentException
                ("Rollup schedules not yet supported.");

        else
            throw new IllegalArgumentException
                ("No local task list by that name");

        this.data = data;
        this.taskListName = taskListName;


        // Create the frame and set an appropriate icon
        frame = new JFrame("Task and Schedule Collaboration Wizard");
        frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().createImage
                           (getClass().getResource("icon32.gif")));
        //FIXME - need to create help
        //PCSH.enableHelpKey(frame, "UsingTaskSchedule");

        frame.getContentPane().add(new WelcomeScreen());
        frame.pack();
        //frame.setResizable(false);
        frame.show();
    }

    private String getTaskNameText() {
        return ("<html><h2>" + HTMLUtils.escapeEntities(taskListName) +
                "</h2></html>");
    }

    private void closeWizard() {
        frame.setVisible(false);
        frame.dispose();
    }

    private void setPanel(JPanel panel) {
        frame.setVisible(false);
        frame.getContentPane().removeAll();
        frame.getContentPane().add(panel);
        frame.setVisible(true);
    }

    private void showPublishScreen() {
        setPanel(new PasswordScreen(PUBLISH));
    }

    private void showShareScreen() {
        setPanel(new PasswordScreen(SHARE));
    }

    private void showResultsScreen(int action, String password) {
        setPanel(new ResultsScreen(action, password));
    }

    private void showRollupScreen() {
        //FIXME: not yet implemented
    }

    private static final int PUBLISH = 0;
    private static final int SHARE   = 1;
    private static final int ROLLUP  = 2;
    private static final int CANCEL  = 3;


    // Private class - displays the welcome screen.
    private class WelcomeScreen extends JPanel
        implements ActionListener, MouseListener
    {

        public JLabel taskListName;
        public JLabel prompt;
        public JPanel buttonBox;
        public JButton publishButton;
        public JLabel filler1;
        public JButton shareButton;
        public JLabel filler2;
        public JButton rollupButton;
        public JLabel filler3;
        public JButton cancelButton;
        public JLabel image;
        public JTextArea explanation;

        ImageIcon publishImage, shareImage, rollupImage;


        public JPanel BuildbuttonBox() {
            // FIXME: we need to ensure that all JPanel objects have a
            // background color of #c6c3c6 (198, 195, 198)

            JPanel buttonBox = new JPanel();
            GridBagLayout oLayout = new GridBagLayout();
            buttonBox.setLayout(oLayout);
            GridBagConstraints oConst;

            publishButton = newJButton();
            publishButton.setText("Publish");
            buttonBox.add(publishButton);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =0;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =1;
            oLayout.setConstraints(publishButton, oConst);

            filler1 = new JLabel();
            filler1.setText("");
            buttonBox.add(filler1);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =1;
            oConst.weighty =1.0;
            oConst.fill =3;
            oLayout.setConstraints(filler1, oConst);

            shareButton = newJButton();
            shareButton.setText("Share");
            buttonBox.add(shareButton);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =2;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =1;
            oLayout.setConstraints(shareButton, oConst);

            filler2 = new JLabel();
            filler2.setText("");
            buttonBox.add(filler2);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =3;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =1;
            oLayout.setConstraints(filler2, oConst);

            rollupButton = newJButton();
            rollupButton.setText("Rollup");
            buttonBox.add(rollupButton);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =4;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =1;
            oLayout.setConstraints(rollupButton, oConst);

            filler3 = new JLabel();
            filler3.setText("");
            buttonBox.add(filler3);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =5;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =1;
            oLayout.setConstraints(filler3, oConst);

            cancelButton = newJButton();
            cancelButton.setText("Cancel");
            buttonBox.add(cancelButton);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =6;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =1;
            oLayout.setConstraints(cancelButton, oConst);

            return buttonBox;
        }

        void BuildFrame() {
            Container oPanel = this;
            GridBagLayout oLayout = new GridBagLayout();
            oPanel.setLayout(oLayout);
            GridBagConstraints oConst;

            taskListName = new JLabel();
            taskListName.setText(getTaskNameText());
            oPanel.add(taskListName);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =0;
            oConst.weightx =1.0;
            oConst.gridwidth =2;
            oConst.fill =2;
            oConst.anchor =17;
            oConst.insets.top =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oConst.insets.bottom =10;
            oLayout.setConstraints(taskListName, oConst);

            prompt = new JLabel();
            prompt.setText("What would you like to do?");
            oPanel.add(prompt);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =1;
            oConst.gridwidth =2;
            oConst.anchor =17;
            oConst.insets.left =10;
            oConst.insets.bottom =10;
            oLayout.setConstraints(prompt, oConst);

            buttonBox = BuildbuttonBox();
            oPanel.add(buttonBox);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =2;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.gridheight =2;
            oConst.fill =1;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oConst.insets.bottom =10;
            oLayout.setConstraints(buttonBox, oConst);

            image = new JLabel(publishImage);
            Dimension d = new Dimension(230, 170);
            image.setMinimumSize(d);
            image.setPreferredSize(d);
            image.setMaximumSize(d);
            oPanel.add(image);
            oConst = new GridBagConstraints();
            oConst.gridx =1;
            oConst.gridy =2;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =1;
            oConst.insets.right =10;
            oConst.insets.bottom =10;
            oLayout.setConstraints(image, oConst);

            explanation = new JTextArea(TEXT[PUBLISH], 3, 10);
            explanation.setBackground(null);
            explanation.setLineWrap(true);
            explanation.setWrapStyleWord(true);
            explanation.setEditable(false);
            explanation.setMinimumSize(explanation.getPreferredSize());
            oPanel.add(explanation);
            oConst = new GridBagConstraints();
            oConst.gridx =1;
            oConst.gridy =3;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =1;
            oConst.insets.right =10;
            oConst.insets.bottom =10;
            oLayout.setConstraints(explanation, oConst);

        }


        public WelcomeScreen() {
            publishImage = images[PUBLISH] = new ImageIcon
                (getClass().getResource("/Templates/Images/ev-publish.png"));
            shareImage = images[SHARE] = new ImageIcon
                (getClass().getResource("/Templates/Images/ev-share.png"));
            rollupImage = images[ROLLUP] = new ImageIcon
                (getClass().getResource("/Templates/Images/ev-rollup.png"));
            images[CANCEL] = null;

            BuildFrame();
        }


        private JButton newJButton() {
            JButton result = new JButton();
            result.addMouseListener(this);
            result.addActionListener(this);
            result.setFocusPainted(false);
            return result;
        }

        public void mouseClicked(MouseEvent e) {}
        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {
            Object o = e.getSource();
            if (o == publishButton)     showInfo(PUBLISH);
            else if (o == shareButton)  showInfo(SHARE);
            else if (o == rollupButton) showInfo(ROLLUP);
            else if (o == cancelButton) showInfo(CANCEL);
            else return;

            colorButton(publishButton, o);
            colorButton(shareButton,   o);
            colorButton(rollupButton,  o);
            //colorButton(cancelButton,  o);
        }
        private void colorButton(JButton button, Object target) {
            button.setBackground(button == target ? Color.yellow : null);
        }
        public void mouseExited(MouseEvent e) { }
        private void ignored(MouseEvent e) {
            Object o = e.getSource();
            if (o instanceof JButton)
                ((JButton) o).setBackground(null);
        }
        private void showInfo(int which) {
            image.setIcon(images[which]);
            explanation.setText(TEXT[which]);
        }
        ImageIcon[] images = new ImageIcon[4];
        private final String[] TEXT = new String[] {
            "Publish this schedule so your co-workers, manager, and/or" +
                " customer can see it.",
            "Share this schedule so your co-workers and/or manager can" +
                " add it to a roll-up schedule.",
            "Create a new schedule which rolls up this schedule with"+
                " other schedules.",
            "" };

        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source == publishButton)
                showPublishScreen();
            else if (source == shareButton)
                showShareScreen();
            else if (source == rollupButton)
                showRollupScreen();
            else if (source == cancelButton)
                closeWizard();
        }

    }

    private String getPrefix(int action) {
        return (action == PUBLISH ? "/ev /" : "/evr /") + taskListName;
    }

    private static final String NO_PASSWORD = " none ";

    private class PasswordScreen extends JPanel implements ActionListener {
        public JLabel taskListName;
        public JLabel prompt;
        public JRadioButton reqPasswordOption;
        public JLabel passPrompt;
        public JTextField password;
        public JRadioButton noPasswordOption;
        public JButton okButton;
        public JLabel filler;
        public ButtonGroup buttonGroup;
        public int action;

        private String passwordRecallName() {
            return getPrefix(action) + "/PW_STOR";
        }

        String getPassword() {
            String dataName = passwordRecallName();
            SimpleData val = data.getSimpleValue(dataName);
            if (val == null) return null;
            String str = val.format();
            if (NO_PASSWORD.equals(str)) return NO_PASSWORD;
            return Base64.decode(str);
        }

        void setPassword(String password) {
            // store a weakly encoded version of the password so we
            // can retrieve it later.
            String dataName = passwordRecallName();
            String recallVal = password;
            if (!NO_PASSWORD.equals(recallVal))
                recallVal = Base64.encode(recallVal);
            data.putValue(dataName, StringData.create(recallVal));

            // actually set the password in the TinyWebServer
            String username = (action == PUBLISH ? "guest" : "EV");
            if (NO_PASSWORD.equals(password))
                username = password = null;
            TinyWebServer.setPassword(data, getPrefix(action),
                                      username, password);
        }

        void BuildFrame() {
            Container oPanel = this;
            GridBagLayout oLayout = new GridBagLayout();
            oPanel.setLayout(oLayout);
            GridBagConstraints oConst;
            taskListName = new JLabel();
            taskListName.setText(getTaskNameText());
            oPanel.add(taskListName);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =0;
            oConst.weightx =1.0;
            oConst.gridwidth =2;
            oConst.fill =2;
            oConst.anchor =17;
            oConst.insets.top =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oLayout.setConstraints(taskListName, oConst);

            prompt = new JLabel();
            prompt.setText("Please configure the permissions for "+
                           (action == PUBLISH ? "publishing" : "sharing") +
                           " this schedule: ");
            oPanel.add(prompt);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =1;
            oConst.gridwidth =2;
            oConst.anchor =17;
            oConst.insets.top =10;
            oConst.insets.left =10;
            oLayout.setConstraints(prompt, oConst);

            buttonGroup = new ButtonGroup();
            reqPasswordOption = new JRadioButton();
            buttonGroup.add(reqPasswordOption);
            reqPasswordOption.setText("Require people to enter this password:");
            oPanel.add(reqPasswordOption);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =2;
            oConst.gridwidth =2;
            oConst.anchor =17;
            oConst.insets.top =10;
            oConst.insets.left =20;
            oLayout.setConstraints(reqPasswordOption, oConst);

            passPrompt = new JLabel();
            passPrompt.setText("Password:");
            oPanel.add(passPrompt);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =3;
            oConst.anchor =17;
            oConst.insets.left =50;
            oConst.insets.right =10;
            oLayout.setConstraints(passPrompt, oConst);

            password = new JTextField();
            password.setColumns(10);
            oPanel.add(password);
            oConst = new GridBagConstraints();
            oConst.gridx =1;
            oConst.gridy =3;
            oConst.anchor =17;
            oLayout.setConstraints(password, oConst);

            noPasswordOption = new JRadioButton();
            buttonGroup.add(noPasswordOption);
            noPasswordOption.setText("Don't require a password");
            oPanel.add(noPasswordOption);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =4;
            oConst.gridwidth =2;
            oConst.anchor =17;
            oConst.insets.top =10;
            oConst.insets.left =20;
            oLayout.setConstraints(noPasswordOption, oConst);

            okButton = new JButton();
            okButton.addActionListener(this);
            okButton.setText("OK");
            oPanel.add(okButton);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =5;
            oConst.gridwidth =2;
            oConst.insets.top =20;
            oConst.insets.bottom =10;
            oLayout.setConstraints(okButton, oConst);

            filler = new JLabel();
            filler.setText("");
            oPanel.add(filler);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =6;
            oConst.weighty =1.0;
            oConst.fill =3;
            oLayout.setConstraints(filler, oConst);

            String currentPassword = getPassword();
            if (NO_PASSWORD.equals(currentPassword))
                noPasswordOption.doClick();
            else {
                reqPasswordOption.doClick();
                password.setText(currentPassword);
            }
        }


        public PasswordScreen(int action) {
            this.action = action;
            BuildFrame();
        }

        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source != okButton) return;

            // they clicked the OK button. check to ensure their inputs
            // are valid; process them if so.
            if (noPasswordOption.isSelected()) {
                setPassword(NO_PASSWORD);
                showResultsScreen(action, NO_PASSWORD);
                return;
            }

            String newPassword = password.getText();
            if (newPassword == null || newPassword.trim().length() == 0) {
                JOptionPane.showMessageDialog
                    (frame, PASSWORD_ERROR, "Please Enter Password",
                     JOptionPane.ERROR_MESSAGE);
                return;
            }

            newPassword = newPassword.trim();
            setPassword(newPassword);
            showResultsScreen(action, newPassword);
        }
    }

    private static final String[] PASSWORD_ERROR = {
        "Please enter a password, or select",
        "the \"Don't require a password\" option." };

    private class ResultsScreen extends JPanel {
        public JLabel taskListName;
        public JEditorPane resultsMessage;
        public JButton okButton;
        public JLabel filler;

        void BuildFrame() {
            Container oPanel = this;
            GridBagLayout oLayout = new GridBagLayout();
            oPanel.setLayout(oLayout);
            GridBagConstraints oConst;
            taskListName = new JLabel();
            taskListName.setText("Name of Task List");
            oPanel.add(taskListName);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =0;
            oConst.weightx =1.0;
            oConst.gridwidth =2;
            oConst.fill =2;
            oConst.anchor =17;
            oConst.insets.top =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oLayout.setConstraints(taskListName, oConst);

            resultsMessage = new JEditorPane();
            oPanel.add(resultsMessage);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =1;
            oConst.weightx =1.0;
            oConst.gridwidth =2;
            oConst.fill =2;
            oConst.insets.top =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oLayout.setConstraints(resultsMessage, oConst);

            okButton = new JButton();
            okButton.setText("OK");
            oPanel.add(okButton);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =2;
            oConst.gridwidth =2;
            oConst.insets.top =20;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oConst.insets.bottom =10;
            oLayout.setConstraints(okButton, oConst);

            filler = new JLabel();
            filler.setText("");
            oPanel.add(filler);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =3;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.gridwidth =2;
            oConst.fill =1;
            oLayout.setConstraints(filler, oConst);

        }


        public ResultsScreen(int action, String password) {
            BuildFrame();
        }

    }


}
