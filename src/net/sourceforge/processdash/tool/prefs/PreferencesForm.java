// Copyright (C) 2009-2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.prefs;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.TranslationLevelPreferenceBox;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.prefs.editor.ManuallyEnteredPreferencesList;
import net.sourceforge.processdash.tool.prefs.editor.PreferencesCheckBox;
import net.sourceforge.processdash.tool.prefs.editor.PreferencesDatasetEncodingconverter;
import net.sourceforge.processdash.tool.prefs.editor.PreferencesFileList;
import net.sourceforge.processdash.tool.prefs.editor.PreferencesPasswordField;
import net.sourceforge.processdash.tool.prefs.editor.PreferencesRadioButtons;
import net.sourceforge.processdash.tool.prefs.editor.PreferencesTextField;
import net.sourceforge.processdash.tool.prefs.editor.SharedFolderPreferencesList;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.binding.BoundForm;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

/**
 * This form is used to modify the user preferences under a specific category.
 */
public class PreferencesForm extends BoundForm {

    /** Various xml tags used in spec files */
    private static final String ID_TAG = "id";
    private static final String REQUIRES_TAG = "requires";
    public static final String SETTING_TAG = "setting";
    public static final String REQUIRES_RESTART = "requiresRestart";
    private static final String HELP_ID = "helpId";

    /** The tags for which special Preferences editors are used */
    private static final String CHECKBOX_TAG = "checkbox";
    private static final String TEXTFIELD_TAG = "textfield";
    private static final String PASSWORD_TAG = "password";
    private static final String RADIOBUTTONS_TAG = "radio";
    private static final String FILELIST_TAG = "file-list";
    private static final String MANUAL_ENTRY_TAG = "manualEntry";
    private static final String SHARED_FOLDERS_TAG = "sharedFolders";
    private static final String I18N_LEVEL_TAG = "translationLevel";
    private static final String DATASET_ENCODING_CONVERTER = "datasetEncodingConverter";

    /** The JPanel containing the GUI */
    private JPanel panel = new JPanel();

    /** Contains all settings for which a modification needs the Dashboard to
     *   be restarted in order to be effective */
    Set<String> requireRestartSettings = new TreeSet<String>();

    private static final Logger logger = Logger.getLogger(PreferencesForm.class.getName());

    public PreferencesForm(PreferencesCategory category) {
        addElementType(CHECKBOX_TAG, PreferencesCheckBox.class);
        addElementType(TEXTFIELD_TAG, PreferencesTextField.class);
        addElementType(PASSWORD_TAG, PreferencesPasswordField.class);
        addElementType(RADIOBUTTONS_TAG, PreferencesRadioButtons.class);
        addElementType(FILELIST_TAG, PreferencesFileList.class);
        addElementType(MANUAL_ENTRY_TAG, ManuallyEnteredPreferencesList.class);
        addElementType(SHARED_FOLDERS_TAG, SharedFolderPreferencesList.class);
        addElementType(I18N_LEVEL_TAG, TranslationLevelPreferenceBox.class);
        addElementType(DATASET_ENCODING_CONVERTER,
            PreferencesDatasetEncodingconverter.class);

        put("os." + InternalSettings.getOSPrefix(), "true");
        if (Settings.isPersonalMode()) put("personalMode", "true");
        if (Settings.isTeamMode()) put("teamMode", "true");
        if (Settings.isHybridMode()) put("hybridMode", "true");
        if (!"en".equals(Locale.getDefault().getLanguage())
                || System.getProperty("user.origLanguage") != null)
            put("i18n", "true");

        selectCategory(category);
        panel.setLayout(new BorderLayout());
        panel.add(BorderLayout.CENTER, getContainer());
    }

    /**
     * Selects the category shown by the PreferencesForm
     */
    private void selectCategory(PreferencesCategory category) {
        if (category != null) {
            SortedSet<PreferencesPane> panes = category.getPanes();

            for (PreferencesPane pane : panes) {
                setResources(pane.getResources());
                loadSpecFileContents(pane.getSpecFile());
            }
        }
    }

    private void loadSpecFileContents(String specFileLocation) {
        try {
            Document spec = getSpecDocument(specFileLocation);

            // Determine which add-on has declared this spec file, and set its
            // classloader as the context class loader for the current thread.
            URL specUrl = TemplateLoader.resolveURL(specFileLocation);
            ClassLoader cl = TemplateLoader.getTemplateClassLoader(specUrl);
            ClassLoader orig = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(cl);
                addFormElements(spec.getDocumentElement());
            } finally {
                Thread.currentThread().setContextClassLoader(orig);
            }

        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
    }

    /**
     * An element should be ignored if the requirements in its "requires" attribute
     *  are not met.
     */
    @Override
    protected boolean shouldIgnoreElement(Element xml) {
        boolean elementIsValid = false;

        if (!super.shouldIgnoreElement(xml)) {
            String requirements = xml.getAttribute(REQUIRES_TAG);

            elementIsValid =
                TemplateLoader.meetsPackageRequirement(requirements);

            if (!elementIsValid) {
                logger.log(Level.INFO, "Could not load preferences widget \"" +
                                        xml.getAttribute(ID_TAG) + "\". " +
                                        "Requirements \"" + requirements + "\" " +
                                        "not met.");
            }
        }

        return !elementIsValid;
    }

    private Document getSpecDocument(String specFileLocation) {
        Document document = null;
        URLConnection conn = TemplateLoader.resolveURLConnection(specFileLocation);

        if (conn != null) {
            try {
                document = XMLUtils.parse(conn.getInputStream());
            }
            catch (SAXException e) { document = null; }
            catch (IOException e) { document = null; }
        }

        if (document == null) {
            throw new IllegalArgumentException("Could not open specFile \"" +
                                               specFileLocation + "\"");
        }

        return document;
    }

    public JPanel getPanel() {
        return panel;
    }

    @Override
    protected void addFormElement(Object element, Element xml) {
        super.addFormElement(element, xml);

        if (Boolean.parseBoolean(xml.getAttribute(REQUIRES_RESTART))) {
            String settingName = xml.getAttribute(SETTING_TAG);

            if (StringUtils.hasValue(settingName))
                requireRestartSettings.add(settingName);
        }
    }

    @Override
    protected void addFormComponent(JComponent component, Element xml) {
        super.addFormComponent(component, xml);

        String helpId = xml.getAttribute(HELP_ID);
        if (StringUtils.hasValue(helpId))
            PCSH.enableHelpKey(component, helpId);
    }

    public Set<String> getRequireRestartSettings() {
        return requireRestartSettings;
    }

}
