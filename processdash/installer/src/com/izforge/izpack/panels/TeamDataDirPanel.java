/*
 *  $Id$
 *  IzPack
 *  Copyright (C) 2001-2003 Julien Ponge
 *  Changes Copyright (C) 2003-2011 David Tuma
 *
 *  File :               TeamDataDirPanel.java
 *  Description :        A panel to select the team data location.
 *  Author's email :     julien@izforge.com
 *  Author's Website :   http://www.izforge.com
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.izforge.izpack.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.izforge.izpack.Pack;
import com.izforge.izpack.adaptator.IXMLElement;
import com.izforge.izpack.adaptator.impl.XMLElementImpl;
import com.izforge.izpack.gui.ButtonFactory;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;
import com.izforge.izpack.pdash.DashboardInstallConstants;
import com.izforge.izpack.pdash.ExternalConfiguration;

/**
 *  The taget directory selection panel.
 *
 * @author     Julien Ponge
 * @created    November 1, 2002
 */
public class TeamDataDirPanel extends IzPanel implements ActionListener,
        DashboardInstallConstants
{
    /**  The default directory. */
    private String defaultDir;

    /**  The info label. */
    private JLabel infoLabel;

    /**  The text field. */
    private JTextField textField;

    /**  The 'browse' button. */
    private JButton browseButton;

    /**  The layout . */
    private GridBagLayout layout;

    /**  The layout constraints. */
    private GridBagConstraints gbConstraints;


    /**
     *  The constructor.
     *
     * @param  parent  The parent window.
     * @param  idata   The installation data.
     */
    public TeamDataDirPanel(InstallerFrame parent, InstallData idata)
    {
        super(parent, idata);

        // We initialize our layout
        layout = new GridBagLayout();
        gbConstraints = new GridBagConstraints();
        setLayout(layout);

        // load the default directory info (if present)
        loadDefaultDir();
        if (defaultDir == null)
            createDefaultDirValue();
        if (defaultDir != null)
            // override the system default
            idata.setVariable(TEAM_DATA_PATH, defaultDir);

        // We create and put the components

        infoLabel = new JLabel
            (parent.langpack.getString("TeamDataDirPanel.info"),
             parent.icons.getImageIcon("home"), JLabel.TRAILING);
        parent.buildConstraints(gbConstraints, 0, 0, 2, 1, 3.0, 0.0);
        gbConstraints.insets = new Insets(5, 5, 5, 5);
        gbConstraints.fill = GridBagConstraints.NONE;
        gbConstraints.anchor = GridBagConstraints.SOUTHWEST;
        layout.addLayoutComponent(infoLabel, gbConstraints);
        add(infoLabel);

        textField = new JTextField(idata.getVariable(TEAM_DATA_PATH), 40);
        textField.addActionListener(this);
        parent.buildConstraints(gbConstraints, 0, 1, 1, 1, 3.0, 0.0);
        gbConstraints.fill = GridBagConstraints.HORIZONTAL;
        gbConstraints.anchor = GridBagConstraints.WEST;
        layout.addLayoutComponent(textField, gbConstraints);
        add(textField);

        browseButton = ButtonFactory.createButton(parent.langpack.getString("TargetPanel.browse"),
            parent.icons.getImageIcon("open"),
            idata.buttonsHColor);
        browseButton.addActionListener(this);
        parent.buildConstraints(gbConstraints, 1, 1, 1, 1, 1.0, 0.0);
        gbConstraints.fill = GridBagConstraints.HORIZONTAL;
        gbConstraints.anchor = GridBagConstraints.EAST;
        layout.addLayoutComponent(browseButton, gbConstraints);
        add(browseButton);

        String message =  parent.langpack.getString("TeamDataDirPanel.message");
        if (message != null) {
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
            parent.buildConstraints(gbConstraints, 0, 2, 2, 1, 3.0, 0.0);
            gbConstraints.fill = GridBagConstraints.HORIZONTAL;
            gbConstraints.anchor = GridBagConstraints.NORTHEAST;
            gbConstraints.insets = new Insets(20, 50, 0, 0);
            layout.addLayoutComponent(messagePanel, gbConstraints);
            add(messagePanel);

            String[] lines = message.trim().split("\\\\n|[\r\n]+");
            for (int i = 0; i < lines.length; i++)
                messagePanel.add(new JLabel(lines[i]));
        }

    }


    /**
     *  Loads up the "dir" resource associated with TeamDataDirPanel. Acceptable dir
     *  resource names: <code>
     *   TeamDataDirPanel.dir.macosx
     *   TeamDataDirPanel.dir.mac
     *   TeamDataDirPanel.dir.windows
     *   TeamDataDirPanel.dir.unix
     *   TeamDataDirPanel.dir.xxx,
     *     where xxx is the lower case version of System.getProperty("os.name"),
     *     with any spaces replace with underscores
     *   TeamDataDirPanel.dir (generic that will be applied if none of above is found)
     *   </code> As with all IzPack resources, each the above ids should be
     *  associated with a separate filename, which is set in the install.xml file
     *  at compile time.
     */
    public void loadDefaultDir()
    {
        // We check to see if user settings exists for the default dir.
        Preferences prefs = Preferences.userRoot().node(USER_VALUES_PREFS_NODE);
        String userTeamDataDir = prefs.get(TEAM_DATA_PATH, null);
        if (userTeamDataDir != null) {
            defaultDir = userTeamDataDir;
            return;
        }

        Properties p = ExternalConfiguration.getConfig();
        String extDefault = p.getProperty("dir.teamconfig.default");
        if (extDefault != null) {
            defaultDir = extDefault;
            return;
        }

        BufferedReader br = null;
        try
        {
            String os = System.getProperty("os.name");
            InputStream in = null;

            if (os.regionMatches(true, 0, "windows", 0, 7))
                in = parent.getResource("TeamDataDirPanel.dir.windows");

            else if (os.regionMatches(true, 0, "mac os x", 0, 8))
                in = parent.getResource("TeamDataDirPanel.dir.macosx");

            else if (os.regionMatches(true, 0, "mac", 0, 3))
                in = parent.getResource("TeamDataDirPanel.dir.mac");

            else
            {
                // first try to look up by specific os name
                os.replace(' ', '_');// avoid spaces in file names
                os = os.toLowerCase();// for consistency among TeamDataDirPanel res files
                in = parent.getResource("TeamDataDirPanel.dir.".concat(os));
                // if not specific os, try getting generic 'unix' resource file
                if (in == null)
                    in = parent.getResource("TeamDataDirPanel.dir.unix");

                // if all those failed, try to look up a generic dir file
                if (in == null)
                    in = parent.getResource("TeamDataDirPanel.dir");

            }

            // if all above tests failed, there is no resource file,
            // so use system default
            if (in == null)
                return;

            // now read the file, once we've identified which one to read
            InputStreamReader isr = new InputStreamReader(in);
            br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                // use the first non-blank line
                if (!line.equals(""))
                    break;
            }
            defaultDir = line;
        }
        catch (Exception e)
        {
            defaultDir = null;// leave unset to take the system default set by Installer class
        }
        finally
        {
            try
            {
                if (br != null)
                    br.close();
            }
            catch (IOException ignored)
            {}
        }
    }

    private static final boolean SHOULD_CREATE_DEFAULT_DIR = false;
    private void createDefaultDirValue() {
        if (SHOULD_CREATE_DEFAULT_DIR) {
            String personalDataDirName = idata.getVariable(DATA_PATH);
            if (personalDataDirName != null) {
                File personalDataDir = new File(personalDataDirName);
                File defaultFile = new File(personalDataDir, "teaminstance");
                defaultDir = defaultFile.getAbsolutePath();
            }
        }
    }


    /**
     *  Indicates wether the panel has been validated or not.
     *
     * @return    Wether the panel has been validated or not.
     */
    public boolean isValidated()
    {
        String dataPath = textField.getText().trim();
        boolean ok = true;

        // We put a warning if the specified target is nameless
        if (dataPath.length() == 0)
        {
            JOptionPane.showMessageDialog(this,
                parent.langpack.getString("TeamDataDirPanel.empty_datadir"),
                parent.langpack.getString("installer.error"),
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Normalize the path, only if it's local
        if (dataPath.startsWith("http")) {
            idata.setVariable(TEAM_DATA_HTTP_FLAG, "true");
        } else {
            File path = new File(dataPath);
            dataPath = path.toString();
            idata.setVariable(TEAM_DATA_HTTP_FLAG, "false");
        }

        idata.setVariable(TEAM_DATA_PATH, dataPath);
        return ok;
    }


    /**
     *  Actions-handling method.
     *
     * @param  e  The event.
     */
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        if (source == textField)
        {
            parent.navigateNext();
        }
        else
        {
            // The user wants to browse its filesystem

            // Prepares the file chooser
            JFileChooser fc = new JFileChooser();
            fc.setCurrentDirectory(new File(textField.getText()));
            fc.setMultiSelectionEnabled(false);
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.addChoosableFileFilter(fc.getAcceptAllFileFilter());

            // Shows it
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                textField.setText(fc.getSelectedFile().getAbsolutePath());

        }
    }

    /**  Called when the panel becomes active.  */
    public void panelActivate() {
        for (int i = 0; i < idata.selectedPacks.size (); i++) {
            Pack p = (Pack)idata.selectedPacks.get (i);
            String packID = p.id;
            if (packID != null && packID.toLowerCase().indexOf("team") != -1)
                // if a team-related package has been selected, return from this
                // method.
                return;
        }

        // if we made it this far, then the user has NOT chosen to install
        // any team-related packages.  Skip this panel entirely.
        parent.skipPanel ();
    }


    /**
     *  Asks to make the XML panel data.
     *
     * @param  panelRoot  The tree to put the data in.
     */
    public void makeXMLData(IXMLElement panelRoot)
    {
        // Data path markup
        IXMLElement ipath = new XMLElementImpl("teamdatapath", panelRoot);
        ipath.setContent(idata.getVariable(TEAM_DATA_PATH));
        panelRoot.addChild(ipath);
    }


    /**
     *  Asks to run in the automated mode.
     *
     * @param  panelRoot  The XML tree to read the data from.
     */
    public void runAutomated(IXMLElement panelRoot)
    {
        // We set the data path
        IXMLElement ipath = panelRoot.getFirstChildNamed("teamdatapath");
        idata.setVariable(TEAM_DATA_PATH, ipath.getContent());
    }
}
