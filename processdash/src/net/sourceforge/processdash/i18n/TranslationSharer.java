// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2004 Software Process Dashboard Initiative
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.i18n;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.util.ClientHttpRequest;


public class TranslationSharer implements Runnable {


    private static final String SHARE_ENABLED_SETTING = "translations.share";
    private static final String AUTHOR_SETTING = "translations.share.author";
    private static final String EMAIL_SETTING = "translations.share.email";

    private static final String POST_URL =
        "http://processdash.sourceforge.net/cgi-bin/shareTranslation";
    private static final String EMAIL_FIELD_NAME = "email";
    private static final String AUTHOR_FIELD_NAME = "author";
    private static final String FILE_FIELD_NAME = "file";

    private static final Resources resources =
        Resources.getDashBundle("ProcessDashboard.Translation.Sharing");

    private String filename;

    public void maybeShareTranslations(String filename) {
        if (shouldShareTranslations()) {
            this.filename = filename;
            Thread t = new Thread(this);
            t.start();
        }
    }

    public void run() {
        try {
            postTranslations(filename);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void postTranslations(String filename) throws Exception {
        ClientHttpRequest request = new ClientHttpRequest(POST_URL);

        String email = Settings.getVal(EMAIL_SETTING);
        if (email != null)
            request.setParameter(EMAIL_FIELD_NAME, email);

        String author = Settings.getVal(AUTHOR_SETTING);
        if (author != null)
            request.setParameter(AUTHOR_FIELD_NAME, author);

        request.setParameter(FILE_FIELD_NAME, new File(filename));
        InputStream response = request.post();
        int success = response.read();
        response.close();
    }


    private boolean shouldShareTranslations() {
        String sharingEnabled = Settings.getVal(SHARE_ENABLED_SETTING);
        if (sharingEnabled == null)
            sharingEnabled = promptToShareTranslations();
        return Boolean.valueOf(sharingEnabled).booleanValue();
    }


    private String promptToShareTranslations() {
        // ask the user if they are willing to share their translations
        int userResponse = JOptionPane.showConfirmDialog
            (null, resources.getStrings("Enable.Prompt"),
             resources.getString("Enable.Title"), JOptionPane.YES_NO_OPTION);

        if (userResponse != JOptionPane.YES_OPTION) {
            // InternalSettings.set(SHARE_ENABLED_SETTING, "false");
            return "false";
        }

        InternalSettings.set(SHARE_ENABLED_SETTING, "true");
        promptForAuthorInfo();
        return "true";
    }


    private void promptForAuthorInfo() {
        JLabel authorLabel = new JLabel
            (resources.getString("Author.Name_Prompt"));
        JTextField author = new JTextField(20);

        JLabel emailLabel = new JLabel
            (resources.getString("Author.Email_Prompt"));
        JTextField email = new JTextField(30);

        JRadioButton yesOption = new JRadioButton
            (resources.getString("Author.Yes_Option"), true);
        JRadioButton noOption = new JRadioButton
            (resources.getString("Author.No_Option"));
        ButtonGroup group = new ButtonGroup();
        group.add(yesOption);
        group.add(noOption);

        ContactOptionListener l = new ContactOptionListener
            (yesOption,
             new JComponent[] { authorLabel, author, emailLabel, email });
        yesOption.addActionListener(l);
        noOption.addActionListener(l);

        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(yesOption);
        buttonBox.add(Box.createHorizontalStrut(10));
        buttonBox.add(noOption);
        buttonBox.add(Box.createHorizontalGlue());

        Object[] message = new Object[] {
                resources.getStrings("Author.Prompt"),
                buttonBox,
                inputBox(authorLabel, author),
                inputBox(emailLabel, email)
         };
        JOptionPane.showMessageDialog
            (null, message, resources.getString("Author.Title"),
             JOptionPane.OK_OPTION);

        if (yesOption.isSelected()) {
            InternalSettings.set(AUTHOR_SETTING, author.getText());
            InternalSettings.set(EMAIL_SETTING, email.getText());
        }
    }


    private JComponent inputBox(JLabel label, JComponent inputField) {
        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalStrut(20));
        result.add(label);
        result.add(Box.createHorizontalStrut(5));
        result.add(inputField);
        result.add(Box.createHorizontalGlue());
        return result;
    }


    private class ContactOptionListener implements ActionListener {

        private JRadioButton yesOption;
        private JComponent[] affectedComponents;

        public ContactOptionListener(JRadioButton yesOption,
                JComponent[] affectedComponents) {
            this.yesOption = yesOption;
            this.affectedComponents = affectedComponents;
        }
        public void actionPerformed(ActionEvent e) {
            boolean enable = yesOption.isSelected();
            for (int i = 0; i < affectedComponents.length; i++)
                affectedComponents[i].setEnabled(enable);
        }

    }

}
