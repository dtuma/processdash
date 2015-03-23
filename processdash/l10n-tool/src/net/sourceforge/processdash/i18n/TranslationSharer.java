// Copyright (C) 2004-2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.i18n;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.util.Locale;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.zaval.tools.i18n.translator.Translator;
import org.zaval.util.SafeResourceBundle;

public class TranslationSharer implements Runnable {


    private static final String SHARE_ENABLED_SETTING = "translations.share";
    private static final String AUTHOR_SETTING = "translations.share.author";
    private static final String EMAIL_SETTING = "translations.share.email";

    private static final String POST_URL =
        "http://processdash.sourceforge.net/cgi-bin/shareTranslation";
    private static final String EMAIL_FIELD_NAME = "email";
    private static final String AUTHOR_FIELD_NAME = "author";
    private static final String FILE_FIELD_NAME = "file";

    private static final SafeResourceBundle resources = 
        new SafeResourceBundle(Translator.BUNDLE_NAME, Locale.getDefault());

    private String filename;
    
    private Preferences prefs;
    
    public TranslationSharer() {
        prefs = Preferences.userNodeForPackage(this.getClass());
    }

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

        String email = prefs.get(EMAIL_SETTING, null);
        if (email != null)
            request.setParameter(EMAIL_FIELD_NAME, email);

        String author = prefs.get(AUTHOR_SETTING, null);
        if (author != null)
            request.setParameter(AUTHOR_FIELD_NAME, author);
        
        request.setParameter(FILE_FIELD_NAME, new File(filename));
        
        InputStream response = request.post();
        
        int success = response.read();
        response.close();
    }


    private boolean shouldShareTranslations() {
        boolean sharingEnabled = prefs.getBoolean(SHARE_ENABLED_SETTING, false);
        if (!sharingEnabled)
            sharingEnabled = promptToShareTranslations();
        return Boolean.valueOf(sharingEnabled).booleanValue();
    }


    private boolean promptToShareTranslations() {
        // ask the user if they are willing to share their translations
        int userResponse = JOptionPane.showConfirmDialog
            (null, resources.getString("Translation.Sharing.Enable.Prompt"),
             resources.getString("Translation.Sharing.Enable.Title"), JOptionPane.YES_NO_OPTION);

        if (userResponse != JOptionPane.YES_OPTION) {
            // InternalSettings.set(SHARE_ENABLED_SETTING, "false");
            return false;
        }

        prefs.putBoolean(SHARE_ENABLED_SETTING, true);
        
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            System.out.println(resources.getString("Translation.Errors.Cant_Save_Preferences"));
            e.printStackTrace();
        }
        
        promptForAuthorInfo();
        return true;
    }


    private void promptForAuthorInfo() {
        JLabel authorLabel = new JLabel
            (resources.getString("Translation.Sharing.Author.Name_Prompt"));
        JTextField author = new JTextField(20);

        JLabel emailLabel = new JLabel
            (resources.getString("Translation.Sharing.Author.Email_Prompt"));
        JTextField email = new JTextField(30);

        JRadioButton yesOption = new JRadioButton
            (resources.getString("Translation.Sharing.Author.Yes_Option"), true);
        JRadioButton noOption = new JRadioButton
            (resources.getString("Translation.Sharing.Author.No_Option"));
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
                resources.getString("Translation.Sharing.Author.Prompt"),
                buttonBox,
                inputBox(authorLabel, author),
                inputBox(emailLabel, email)
         };
        JOptionPane.showMessageDialog
            (null, message, resources.getString("Translation.Sharing.Author.Title"),
             JOptionPane.OK_OPTION);

        if (yesOption.isSelected()) {
            prefs.put(AUTHOR_SETTING, author.getText());
            prefs.put(EMAIL_SETTING, email.getText());
            try {
                prefs.flush();
            } catch (BackingStoreException e) {
                System.out.println(resources.getString("Translation.Errors.Cant_Save_Preferences"));
                e.printStackTrace();
            }
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