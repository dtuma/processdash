// Copyright (C) 2002-2014 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ev.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Stack;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.cache.ObjectCache;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.util.Base64;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


/**
 * Collaboration is only available for local task lists.
 */
public class TaskScheduleCollaborationWizard {

    private boolean isRollup;
    private JFrame frame;
    private DashboardContext dash;
    private DataRepository data;
    private DashHierarchy hierarchy;
    private ObjectCache cache;
    private WebServer webServer;
    private String taskListName;
    private String rollupTaskListName = null;
    private Stack previousPanes = new Stack();

    Resources resources = Resources.getDashBundle("EV.Collab");

    /** Create and display a collaboration wizard.
     */
    public TaskScheduleCollaborationWizard(DashboardContext dash,
                                           String taskListName) {
        // check input parameters
        if (EVTaskListData.validName(taskListName) &&
            EVTaskListData.exists(dash.getData(), taskListName))
            isRollup = false;

        else if (EVTaskListRollup.validName(taskListName) &&
                 EVTaskListRollup.exists(dash.getData(), taskListName))
            isRollup = true;

        else
            throw new IllegalArgumentException
                ("No local task list by that name");

        // Save the parameters into our data structure
        this.dash = dash;
        this.data = dash.getData();
        this.hierarchy = dash.getHierarchy();
        this.cache = dash.getCache();
        this.webServer = dash.getWebServer();
        this.taskListName = taskListName;

        // Create the frame and set an appropriate icon
        frame = new JFrame(resources.getString("Window_Title"));
        DashboardIconFactory.setWindowIcon(frame);

        frame.getContentPane().add(new WelcomeScreen());
        frame.pack();
        //frame.setResizable(false);
        frame.setVisible(true);
    }

    private String getTaskNameText() { return getTaskNameText(PUBLISH); }
    private String getTaskNameText(int action) {
        String name = (action == ROLLUP ? rollupTaskListName : taskListName);
        return ("<html><h2>" + HTMLUtils.escapeEntities(name) +
                "</h2></html>");
    }

    private void closeWizard() {
        frame.setVisible(false);
        frame.dispose();
        previousPanes.clear();
        data = null;
        hierarchy = null;
        cache = null;
        webServer = null;
        dash = null;
    }

    private void setPanel(JPanel panel) {
        setPanel(panel, true);
    }

    private void setPanel(JPanel panel, boolean pushCurrent) {
        frame.setVisible(false);
        if (pushCurrent)
            previousPanes.push(frame.getContentPane().getComponent(0));
        frame.getContentPane().removeAll();
        frame.getContentPane().add(panel);
        frame.setVisible(true);
        PCSH.enableHelpKey(frame, PCSH.getHelpIDString(panel));
    }

    private void backPanel() {
        if (previousPanes.empty()) return;
        setPanel((JPanel) previousPanes.pop(), false);
    }

    private void showPublishScreen() {
        showConnectivityOrPasswordScreen(PUBLISH);
    }

    private void showShareScreen() {
        showConnectivityOrPasswordScreen(SHARE);
    }

    private void showConnectivityOrPasswordScreen(int action) {
        if (isRemoteAccessBlocked())
            setPanel(new ConnectivityScreen(action));
        else
            showPasswordScreen(action);
    }

    private void showPasswordScreen(int action) {
        setPanel(new PasswordScreen(action));
    }

    private void showResultsScreen(int action, String password) {
        setPanel(new ResultsScreen(action, password));
    }

    private void showRollupScreen() {
        setPanel(new RollupNameScreen());
    }

    // We display images in the welcome screen that have a background
    // color (because saving them with alpha transparency tripled the
    // size of the files unnecessarily).  This background color
    // matches the default Windows Java background color, but that
    // default might be different for other platforms, or for users
    // with nonstandard Windows color schemes. Therefore, to keep the
    // images from being displayed with a shadow around them, we
    // manually set the background color of the wizard to match.
    private Color backgroundColor = new Color(198, 195, 198);

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



        public JPanel BuildbuttonBox() {

            JPanel buttonBox = new JPanel();
            buttonBox.setBackground(backgroundColor);
            GridBagLayout oLayout = new GridBagLayout();
            buttonBox.setLayout(oLayout);
            GridBagConstraints oConst;

            publishButton = newJButton();
            publishButton.setText
                (resources.getString("Publish.Button"));
            publishButton.setBackground(null);
            buttonBox.add(publishButton);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =0;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =GridBagConstraints.BOTH;
            oLayout.setConstraints(publishButton, oConst);

            filler1 = new JLabel();
            filler1.setText("");
            buttonBox.add(filler1);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =1;
            oConst.weighty =1.0;
            oConst.fill =GridBagConstraints.VERTICAL;
            oLayout.setConstraints(filler1, oConst);

            shareButton = newJButton();
            shareButton.setText(resources.getString("Share.Button"));
            shareButton.setBackground(null);
            shareButton.setEnabled(!isRollup);
            buttonBox.add(shareButton);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =2;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =GridBagConstraints.BOTH;
            oLayout.setConstraints(shareButton, oConst);

            filler2 = new JLabel();
            filler2.setText("");
            buttonBox.add(filler2);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =3;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =GridBagConstraints.BOTH;
            oLayout.setConstraints(filler2, oConst);

            rollupButton = newJButton();
            rollupButton.setText(resources.getString("Rollup.Button"));
            rollupButton.setBackground(null);
            rollupButton.setEnabled(!isRollup);
            buttonBox.add(rollupButton);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =4;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =GridBagConstraints.BOTH;
            oLayout.setConstraints(rollupButton, oConst);

            filler3 = new JLabel();
            filler3.setText("");
            buttonBox.add(filler3);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =5;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =GridBagConstraints.BOTH;
            oLayout.setConstraints(filler3, oConst);

            cancelButton = newJButton();
            cancelButton.setText(resources.getString("Cancel"));
            cancelButton.setBackground(null);
            buttonBox.add(cancelButton);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =6;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.fill =GridBagConstraints.BOTH;
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
            oConst.fill =GridBagConstraints.HORIZONTAL;
            oConst.anchor =GridBagConstraints.WEST;
            oConst.insets.top =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oConst.insets.bottom =10;
            oLayout.setConstraints(taskListName, oConst);

            prompt = new JLabel();
            prompt.setText(resources.getString
                           ("Welcome_Screen_Prompt"));

            oPanel.add(prompt);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =1;
            oConst.gridwidth =2;
            oConst.anchor =GridBagConstraints.WEST;
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
            oConst.fill =GridBagConstraints.BOTH;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oConst.insets.bottom =10;
            oLayout.setConstraints(buttonBox, oConst);

            image = new JLabel(images[PUBLISH]);
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
            oConst.fill =GridBagConstraints.BOTH;
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
            oConst.fill =GridBagConstraints.BOTH;
            oConst.insets.right =10;
            oConst.insets.bottom =10;
            oLayout.setConstraints(explanation, oConst);

        }


        public WelcomeScreen() {
            images[PUBLISH] = getTemplateImage("ev-publish.png");
            images[SHARE] = getTemplateImage("ev-share.png");
            images[ROLLUP] = getTemplateImage("ev-rollup.png");
            images[CANCEL] = null;

            BuildFrame();
            this.setBackground(backgroundColor);
            PCSH.enableHelpKey(this, "TaskScheduleCollaboration");
        }

        private ImageIcon getTemplateImage(String image) {
            URL imageURL = TemplateLoader.resolveURL("/Images/" + image);
            return new ImageIcon(imageURL);
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
            if (o instanceof JButton && ((JButton) o).isEnabled()) {

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
        }
        private void colorButton(JButton button, Object target) {
            button.setBackground(button == target ? Color.yellow : null);
        }
        public void mouseExited(MouseEvent e) { }
        private void showInfo(int which) {
            image.setIcon(images[which]);
            explanation.setText(TEXT[which]);
        }
        ImageIcon[] images = new ImageIcon[4];
        private final String[] TEXT = new String[] {
            resources.getString("Publish.Description"),
            resources.getString("Share.Description"),
            resources.getString("Rollup.Description"),
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

    private class ConnectivityScreen extends JPanel implements ActionListener {
        public JLabel taskListName;

        public JEditorPane resultsMessage;

        public JButton backButton, nextButton, cancelButton;

        public int action;

        public JPanel BuildbuttonBox() {
            JPanel buttonBox = new JPanel();
            buttonBox.setBackground(backgroundColor);
            FlowLayout oLayout = new FlowLayout(FlowLayout.RIGHT, 0, 0);
            buttonBox.setLayout(oLayout);

            backButton = new JButton(resources.getString("Back_Button"));
            backButton.addActionListener(this);
            buttonBox.add(backButton);

            nextButton = new JButton(resources.getString("Next_Button"));
            nextButton.addActionListener(this);
            buttonBox.add(nextButton);

            JLabel filler = new JLabel("  ");
            buttonBox.add(filler);

            cancelButton = new JButton(resources.getString("Cancel"));
            cancelButton.addActionListener(this);
            buttonBox.add(cancelButton);

            return buttonBox;
        }

        void BuildFrame() {
            Container oPanel = this;
            GridBagLayout oLayout = new GridBagLayout();
            oPanel.setLayout(oLayout);
            GridBagConstraints oConst;
            taskListName = new JLabel();
            taskListName.setText(getTaskNameText(action));
            oPanel.add(taskListName);
            oConst = new GridBagConstraints();
            oConst.gridx = 0;
            oConst.gridy = 0;
            oConst.weightx = 1.0;
            oConst.gridwidth = 2;
            oConst.fill = GridBagConstraints.HORIZONTAL;
            oConst.anchor = GridBagConstraints.WEST;
            oConst.insets.top = 10;
            oConst.insets.left = 10;
            oConst.insets.right = 10;
            oLayout.setConstraints(taskListName, oConst);

            resultsMessage = new JEditorPane();
            resultsMessage.setContentType("text/html");
            resultsMessage.setEditable(false);
            resultsMessage.setBackground(null);
            resultsMessage.setText("<html><head><style>"
                    + "ul { margin-top: 0px; margin-bottom: 0px; }"
                    + "</style></head><body>"
                    + resources.getString("Connectivity.Message_HTML")
                    + "</body></html>");
            resultsMessage.setCaretPosition(0);

            JScrollPane sp = new JScrollPane(resultsMessage);
            sp.setVerticalScrollBarPolicy(
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            oPanel.add(sp);
            oConst = new GridBagConstraints();
            oConst.gridx = 0;
            oConst.gridy = 1;
            oConst.weightx = 1.0;
            oConst.weighty = 1.0;
            oConst.gridwidth = 2;
            oConst.fill = GridBagConstraints.BOTH;
            oConst.insets.top = 10;
            oConst.insets.left = 10;
            oConst.insets.right = 10;
            oLayout.setConstraints(sp, oConst);

            JPanel buttonBox = BuildbuttonBox();
            oPanel.add(buttonBox);
            oConst = new GridBagConstraints();
            oConst.gridx = 0;
            oConst.gridy = 2;
            oConst.gridwidth = 2;
            oConst.anchor = GridBagConstraints.EAST;
            oConst.insets.top = 20;
            oConst.insets.bottom = 10;
            oConst.insets.left = 10;
            oConst.insets.right = 10;
            oLayout.setConstraints(buttonBox, oConst);

        }

        public ConnectivityScreen(int action) {
            this.action = action;
            BuildFrame();
            this.setBackground(backgroundColor);
            PCSH.enableHelpKey(this, "TaskScheduleCollaboration.limitations");
        }

        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source == cancelButton) {
                closeWizard();
            } else if (source == backButton) {
                backPanel();
            } else if (source == nextButton) {
                InternalSettings.set(WebServer.HTTP_ALLOWREMOTE_SETTING,
                        "false");
                showPasswordScreen(action);
            }
        }
    }



    private String getPrefix(int action) {
        return (action == PUBLISH ? "/ev /" : "/evr /") + taskListName;
    }

    private static final String NO_PASSWORD = " none ";

    private class PasswordScreen extends JPanel implements ActionListener {
        public JLabel taskListName;
        public JTextArea prompt;
        public JRadioButton reqPasswordOption;
        public JLabel passPrompt;
        public JTextField password;
        public JRadioButton noPasswordOption;
        public JButton backButton, nextButton, cancelButton;
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
            return new String(Base64.decode(str));
        }

        void setPassword(String password) {
            // store a weakly encoded version of the password so we
            // can retrieve it later.
            String dataName = passwordRecallName();
            String recallVal = password;
            if (!NO_PASSWORD.equals(recallVal))
                recallVal = Base64.encodeBytes(recallVal.getBytes());
            data.putValue(dataName, StringData.create(recallVal));

            /* DISABLED
            // actually set the password in the TinyWebServer
            String username = (action == PUBLISH ? "guest" : "EV");
            if (NO_PASSWORD.equals(password))
                username = password = null;
            WebServer.setPassword(data, getPrefix(action),
                                      username, password); */
        }

        public JPanel BuildbuttonBox() {
            JPanel buttonBox = new JPanel();
            buttonBox.setBackground(backgroundColor);
            FlowLayout oLayout = new FlowLayout(FlowLayout.RIGHT, 0, 0);
            buttonBox.setLayout(oLayout);

            backButton = new JButton
                (resources.getString("Back_Button"));
            backButton.addActionListener(this);
            buttonBox.add(backButton);

            nextButton = new JButton
                (resources.getString("Next_Button"));

            nextButton.addActionListener(this);
            buttonBox.add(nextButton);

            JLabel filler = new JLabel("  ");
            buttonBox.add(filler);

            cancelButton = new JButton(resources.getString("Cancel"));
            cancelButton.addActionListener(this);
            buttonBox.add(cancelButton);

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
            oConst.fill =GridBagConstraints.HORIZONTAL;
            oConst.anchor =GridBagConstraints.WEST;
            oConst.insets.top =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oLayout.setConstraints(taskListName, oConst);

            prompt = new JTextArea("", 2, 10);
            prompt.setText(resources.getString
                           (action == PUBLISH
                            ? "Publish.Permissions_Prompt"
                            : "Share.Permissions_Prompt"));
            prompt.setBackground(null);
            prompt.setLineWrap(true);
            prompt.setWrapStyleWord(true);
            prompt.setEditable(false);
            oPanel.add(prompt);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =1;
            oConst.weightx =1.0;
            oConst.weighty =0.0;
            oConst.fill =GridBagConstraints.BOTH;
            oConst.gridwidth =2;
            oConst.anchor =GridBagConstraints.WEST;
            oConst.insets.top =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oLayout.setConstraints(prompt, oConst);

            buttonGroup = new ButtonGroup();
            reqPasswordOption = new JRadioButton();
            buttonGroup.add(reqPasswordOption);
            reqPasswordOption.setBackground(null);
            reqPasswordOption.setText
                (resources.getString("Password.Require_Option"));
            oPanel.add(reqPasswordOption);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =2;
            oConst.gridwidth =2;
            oConst.anchor =GridBagConstraints.WEST;
            oConst.insets.top =10;
            oConst.insets.left =20;
            oLayout.setConstraints(reqPasswordOption, oConst);

            passPrompt = new JLabel();
            passPrompt.setText(resources.getString("Password.Label"));
            oPanel.add(passPrompt);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =3;
            oConst.anchor =GridBagConstraints.WEST;
            oConst.insets.left =50;
            oConst.insets.right =10;
            oLayout.setConstraints(passPrompt, oConst);

            password = new JTextField();
            password.setColumns(10);
            oPanel.add(password);
            oConst = new GridBagConstraints();
            oConst.gridx =1;
            oConst.gridy =3;
            oConst.anchor =GridBagConstraints.WEST;
            oLayout.setConstraints(password, oConst);

            noPasswordOption = new JRadioButton();
            buttonGroup.add(noPasswordOption);
            noPasswordOption.setBackground(null);
            noPasswordOption.setText
                (resources.getString("Password.Do_Not_Require_Option"));
            oPanel.add(noPasswordOption);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =4;
            oConst.gridwidth =2;
            oConst.anchor =GridBagConstraints.WEST;
            oConst.insets.top =10;
            oConst.insets.left =20;
            oLayout.setConstraints(noPasswordOption, oConst);

            filler = new JLabel();
            filler.setText("");
            oPanel.add(filler);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =5;
            oConst.weighty =1.0;
            oConst.fill =GridBagConstraints.VERTICAL;
            oLayout.setConstraints(filler, oConst);

            JPanel buttonBox = BuildbuttonBox();
            oPanel.add(buttonBox);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =6;
            oConst.gridwidth =2;
            oConst.anchor =GridBagConstraints.EAST;
            oConst.insets.top =20;
            oConst.insets.bottom =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oLayout.setConstraints(buttonBox, oConst);

            prompt.setFont(passPrompt.getFont());

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
            this.setBackground(backgroundColor);
            PCSH.enableHelpKey(this, PASSWORD_HELP_TOPICS[action]);
        }

        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source == cancelButton) {
                closeWizard(); return;
            } else if (source == backButton) {
                backPanel(); return;
            } else if (source != nextButton) return;

            // they clicked the next button. check to ensure their inputs
            // are valid; process them if so.
            if (noPasswordOption.isSelected()) {
                setPassword(NO_PASSWORD);
                showResultsScreen(action, NO_PASSWORD);
                return;
            }

            String newPassword = password.getText();
            if (newPassword == null || newPassword.trim().length() == 0) {
                JOptionPane.showMessageDialog
                    (frame,
                     resources.getStrings("Password.Error_Prompt"),
                     resources.getString("Password.Error_Title"),
                     JOptionPane.ERROR_MESSAGE);
                return;
            }

            newPassword = newPassword.trim();
            setPassword(newPassword);
            showResultsScreen(action, newPassword);
        }
    }

    private static final String[] PASSWORD_HELP_TOPICS = {
        "TaskScheduleCollaboration.publishPassword",
        "TaskScheduleCollaboration.sharePassword",
        "TaskScheduleCollaboration.createRollup" };

    private class RollupNameScreen extends JPanel implements ActionListener {
        public JLabel taskListName;
        public JTextArea prompt;
        public JLabel namePrompt;
        public JTextField rollupName;
        public JButton backButton, nextButton, cancelButton;
        public JLabel filler;

        public JPanel BuildbuttonBox() {
            JPanel buttonBox = new JPanel();
            buttonBox.setBackground(backgroundColor);
            FlowLayout oLayout = new FlowLayout(FlowLayout.RIGHT, 0, 0);
            buttonBox.setLayout(oLayout);

            backButton = new JButton
                (resources.getString("Back_Button"));
            backButton.addActionListener(this);
            buttonBox.add(backButton);

            nextButton = new JButton
                (resources.getString("Next_Button"));
            nextButton.addActionListener(this);
            buttonBox.add(nextButton);

            JLabel filler = new JLabel("  ");
            buttonBox.add(filler);

            cancelButton = new JButton(resources.getString("Cancel"));
            cancelButton.addActionListener(this);
            buttonBox.add(cancelButton);

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
            oConst.fill =GridBagConstraints.HORIZONTAL;
            oConst.anchor =GridBagConstraints.WEST;
            oConst.insets.top =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oLayout.setConstraints(taskListName, oConst);

            prompt = new JTextArea("", 3, 10);
            prompt.setText(resources.getString
                           ("Rollup.Choose_Name_Prompt"));
            prompt.setBackground(null);
            prompt.setLineWrap(true);
            prompt.setWrapStyleWord(true);
            prompt.setEditable(false);
            oPanel.add(prompt);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =1;
            oConst.weightx =1.0;
            oConst.weighty =0.0;
            oConst.fill =GridBagConstraints.BOTH;
            oConst.gridwidth =2;
            oConst.anchor =GridBagConstraints.WEST;
            oConst.insets.top =10;
            oConst.insets.bottom =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oLayout.setConstraints(prompt, oConst);

            namePrompt = new JLabel();
            namePrompt.setText(resources.getString
                               ("Rollup.Name_Label"));
            oPanel.add(namePrompt);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =2;
            oConst.anchor =GridBagConstraints.WEST;
            oConst.insets.left =50;
            oConst.insets.right =10;
            oLayout.setConstraints(namePrompt, oConst);

            rollupName = new JTextField();
            if (rollupTaskListName != null)
                rollupName.setText(rollupTaskListName);
            else
                rollupName.setText
                    (resources.format
                     ("Rollup.Default_Name_FMT",
                        TaskScheduleCollaborationWizard.this.taskListName));
            rollupName.setColumns(15);
            oPanel.add(rollupName);
            oConst = new GridBagConstraints();
            oConst.gridx =1;
            oConst.gridy =2;
            oConst.anchor =GridBagConstraints.WEST;
            oLayout.setConstraints(rollupName, oConst);

            filler = new JLabel();
            filler.setText("");
            oPanel.add(filler);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =3;
            oConst.weighty =1.0;
            oConst.fill =GridBagConstraints.VERTICAL;
            oLayout.setConstraints(filler, oConst);

            JPanel buttonBox = BuildbuttonBox();
            oPanel.add(buttonBox);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =4;
            oConst.gridwidth =2;
            oConst.anchor =GridBagConstraints.EAST;
            oConst.insets.top =20;
            oConst.insets.bottom =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oLayout.setConstraints(buttonBox, oConst);

            prompt.setFont(namePrompt.getFont());
        }


        public RollupNameScreen() {
            BuildFrame();
            this.setBackground(backgroundColor);
            PCSH.enableHelpKey(this, "TaskScheduleCollaboration.createRollup");
        }

        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source == cancelButton) {
                closeWizard(); return;
            } else if (source == backButton) {
                backPanel(); return;
            } else if (source != nextButton) return;

            // they clicked the next button. check to ensure their inputs
            // are valid.
            String newName = rollupName.getText();
            if (newName != null) newName = newName.trim();

            // check the validity of the name they chose.
            String errorMessage =
                TaskScheduleChooser.checkNewTemplateName(newName, data);
            if (errorMessage != null) {
                //errorMessage = StringUtils.findAndReplace
                //    (errorMessage, "template", "schedule");
                JOptionPane.showMessageDialog
                    (frame, errorMessage,
                     resources.getString
                     ("Rollup.Invalid_Schedule_Name_Title"),
                     JOptionPane.ERROR_MESSAGE);
                return;
            }

            // create the new task list, and add the current list to it.
            EVTaskListRollup rollup = new EVTaskListRollup
                (newName, data, hierarchy, cache, false);
            rollup.addTask(TaskScheduleCollaborationWizard.this.taskListName,
                           data, hierarchy, cache, false);
            rollup.save();
            rollup = null;

            // display the results screen.
            rollupTaskListName = newName;
            showResultsScreen(ROLLUP, NO_PASSWORD);
        }
    }

    private class ResultsScreen extends JPanel
        implements ActionListener, HyperlinkListener
    {
        public JLabel taskListName;
        public JEditorPane resultsMessage;
        public JButton backButton, finishButton, cancelButton;
        public int action;
        public String password;

        public JPanel BuildbuttonBox() {
            JPanel buttonBox = new JPanel();
            buttonBox.setBackground(backgroundColor);
            FlowLayout oLayout = new FlowLayout(FlowLayout.RIGHT, 0, 0);
            buttonBox.setLayout(oLayout);

            backButton = new JButton
                (resources.getString("Back_Button"));
            backButton.addActionListener(this);
            buttonBox.add(backButton);

            finishButton = new JButton
                (resources.getString("Finish_Button"));
            finishButton.addActionListener(this);
            buttonBox.add(finishButton);

            JLabel filler = new JLabel("  ");
            buttonBox.add(filler);

            cancelButton = new JButton(resources.getString("Cancel"));
            cancelButton.setEnabled(false);
            cancelButton.addActionListener(this);
            buttonBox.add(cancelButton);

            return buttonBox;
        }

        void BuildFrame() {
            Container oPanel = this;
            GridBagLayout oLayout = new GridBagLayout();
            oPanel.setLayout(oLayout);
            GridBagConstraints oConst;
            taskListName = new JLabel();
            taskListName.setText(getTaskNameText(action));
            oPanel.add(taskListName);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =0;
            oConst.weightx =1.0;
            oConst.gridwidth =2;
            oConst.fill =GridBagConstraints.HORIZONTAL;
            oConst.anchor =GridBagConstraints.WEST;
            oConst.insets.top =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oLayout.setConstraints(taskListName, oConst);

            resultsMessage = new JEditorPane();
            resultsMessage.setContentType("text/html");
            resultsMessage.setEditable(false);
            resultsMessage.setBackground(null);
            resultsMessage.addHyperlinkListener(this);

            JScrollPane sp = new JScrollPane(resultsMessage);
            oPanel.add(sp);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =1;
            oConst.weightx =1.0;
            oConst.weighty =1.0;
            oConst.gridwidth =2;
            oConst.fill =GridBagConstraints.BOTH;
            oConst.insets.top =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oLayout.setConstraints(sp, oConst);

            JPanel buttonBox = BuildbuttonBox();
            oPanel.add(buttonBox);
            oConst = new GridBagConstraints();
            oConst.gridx =0;
            oConst.gridy =2;
            oConst.gridwidth =2;
            oConst.anchor =GridBagConstraints.EAST;
            oConst.insets.top =20;
            oConst.insets.bottom =10;
            oConst.insets.left =10;
            oConst.insets.right =10;
            oLayout.setConstraints(buttonBox, oConst);

        }


        public ResultsScreen(int action, String password) {
            this.action = action;
            this.password = password;
            BuildFrame();
            setText();
            this.setBackground(backgroundColor);
            PCSH.enableHelpKey(this, RESULTS_HELP_TOPICS[action]);
        }

        public void setText() {
            // Get the template for the message
            String messageTemplate = getText(resultURL[action]);

            // Construct the arguments for the message template
            Object[] args = new Object[7];

            // args 0 and 1 hold the name of the task list (html-encoded
            // and URL-encoded, respectively).
            String name = TaskScheduleCollaborationWizard.this.taskListName;
            args[0] = HTMLUtils.escapeEntities(name);
            args[1] = urlEncode(name);

            // args 2 and 3 hold the URL of the task list (html-encoded
            // and URL-encoded, respectively).
            String url = getTaskListURL();
            args[2] = HTMLUtils.escapeEntities(url);
            args[3] = urlEncode(url);

            // arg 4 holds the integer 1 if this item is password-protected.
            args[4] = new Integer(password == NO_PASSWORD ? 0 : 1);

            // args 5 and 6 hold the password (html-encoded and
            // URL-encoded, respectively).
            args[5] = HTMLUtils.escapeEntities(password);
            args[6] = urlEncode(password);

            // construct the message and display it.
            String message = MessageFormat.format(messageTemplate, args);
            resultsMessage.setText(message);
            resultsMessage.setCaretPosition(0);
        }

        public String getTaskListURL() {
            String prefix = getPrefix(action);
            prefix = WebServer.urlEncodePath(prefix);
            String host = webServer.getHostName(true);
            int port = webServer.getPort();
            return "http://" + host + ":" + port + prefix + EV_URL;
        }

        // we'll pass the following arguments to the message format:
        //   0 - the name of the schedule (html-encoded)
        //   1 - the name of the schedule (url-encoded)
        //   2 - the URL of the schedule (html-encoded)
        //   3 - the URL of the schedule (url-encoded)
        //   4 - 0 if no password is required, 1 otherwise
        //   5 - the password for the schedule (html-encoded)
        //   6 - the password for the schedule (url-encoded)
        // if the password is not required, params 5 and 6 will be empty
        // strings.

        /**
         * Launch a browser when you click on a link
         */
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String url = e.getURL().toString();
                if (url.startsWith("http://help/"))
                    PCSH.displayHelpTopic(url.substring(12));
                else
                    Browser.launch(url);
            }
        }

        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source == finishButton || source == cancelButton) {
                if (action == ROLLUP &&
                    rollupTaskListName != null &&
                    source == finishButton)
                    TaskScheduleChooser.open(dash, rollupTaskListName);

                closeWizard(); return;
            } else if (source == backButton) {
                backPanel(); return;
            }
        }
    }

    private static final String[] RESULTS_HELP_TOPICS = {
        "TaskScheduleCollaboration.publishFinish",
        "TaskScheduleCollaboration.shareFinish",
        "TaskScheduleCollaboration.createRollup" };

    private static final String[] resultURL = {
        "/dash/ev-publish-results.htm",
        "/dash/ev-share-results.htm",
        "/dash/ev-rollup-results.htm" };
    private static final String EV_URL =
        "//reports/ev.class";

    private String getText(String resourceName) {
        try {
            return webServer.getRequestAsString(resourceName);
        } catch (IOException ioe) {
            return "";
        }
    }

    private static String urlEncode(String str) {
        str = HTMLUtils.urlEncode(str);
        str = StringUtils.findAndReplace(str, "+", "%20");
        return str;
    }

    private static boolean isRemoteAccessBlocked() {
        String remoteSetting =
            Settings.getVal(WebServer.HTTP_ALLOWREMOTE_SETTING);
        return "never".equals(remoteSetting);
    }

}
