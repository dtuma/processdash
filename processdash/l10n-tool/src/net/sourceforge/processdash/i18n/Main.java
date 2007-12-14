// Copyright (C) 2007 Tuma Solutions, LLC
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

public class Main {
    
    // The property that indicates which resource to translate
    // Warning : This constant must be syschronized with the
    // one in net.sourceforge.processdash.i18n.OpenLocalizationToolAction
    public static final String PROPERTY_RESOURCE_TO_TRANSLATE = "translate.resource";
    
    // The property that indicates what URL to navigate to to access the help topic
    // Warning : This constant must be syschronized with the
    // one in net.sourceforge.processdash.i18n.OpenLocalizationToolAction
    public static final String PROPERTY_HELP_URL = "help.url";

    public static void main(String arg[]) {
        String filename = System.getProperty(PROPERTY_RESOURCE_TO_TRANSLATE);
        String helpURL = System.getProperty(PROPERTY_HELP_URL);
        
        try {
            org.zaval.tools.i18n.translator.BundleSet.setDefaultComparator(new TranslationSorter());
            org.zaval.tools.i18n.translator.Main.main(new String[] { filename});
            org.zaval.tools.i18n.translator.Main.setFilter(new TranslationFilter());
            org.zaval.tools.i18n.translator.Main.setSaveListener(new TranslationsSavedListener());
            org.zaval.tools.i18n.translator.Main.setHelpListener(new TranslationHelpListener(helpURL));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
