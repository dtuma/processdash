// Copyright (C) 2003-2007 Tuma Solutions, LLC
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
import java.util.Locale;

import javax.swing.JOptionPane;

import org.zaval.tools.i18n.translator.Translator;
import org.zaval.util.SafeResourceBundle;


public class TranslationsSavedListener implements ActionListener {
    
    
    private static final SafeResourceBundle resources = 
        new SafeResourceBundle(Translator.BUNDLE_NAME, Locale.getDefault());
    
    TranslationSharer sharer = new TranslationSharer();
    
    
    public void actionPerformed(ActionEvent e) {
        sharer.maybeShareTranslations(e.getActionCommand());
        showSaveMessage();
    }


    private void showSaveMessage() {
        JOptionPane.showMessageDialog
            (null, resources.getString("Translation.Save.Message"),
             resources.getString("Translation.Save.Title"), JOptionPane.PLAIN_MESSAGE);        
    }

}