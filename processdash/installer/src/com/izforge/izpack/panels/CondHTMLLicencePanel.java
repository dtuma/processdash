/*
 *  $Id$
 *  IzPack
 *  Copyright (C) 2001-2003 Julien Ponge
 *  Changes Copyright (C) 2003-2011 David Tuma
 *
 *  File :               CondHTMLLicencePanel.java
 *  Description :        A panel to prompt the user for a licence agreement
 *                       if a certain pack is being installed.
 *  Author's email :     tuma@users.sourceforge.net
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.izforge.izpack.Pack;
import com.izforge.izpack.adaptator.IXMLElement;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;
import com.izforge.izpack.installer.ResourceManager;
import com.izforge.izpack.util.MultiLineLabel;

/**
 *  The IzPack HTML license panel.
 *
 * @author     Julien Ponge
 * @created    November 1, 2002
 */
public class CondHTMLLicencePanel extends IzPanel implements HyperlinkListener, ActionListener
{
    private static int          instanceCount   = 0;
    private        int          instanceNumber  = 0;

    private InstallerFrame      parent;

    /** An internal panel */
    private JPanel subpanel;

    /**  The layout. */
    private GridBagLayout layout;

    /**  The layout constraints. */
    private GridBagConstraints gbConstraints;

    /**  The header for the license */
    private MessageFormat headerFmt;

    /**  The info label. */
    private MultiLineLabel headerArea;

    /**  The text area. */
    private JEditorPane textArea;

    /**  The agreement label. */
    private JLabel agreeLabel;

    /**  The radio buttons. */
    private JRadioButton yesRadio, noRadio;

    /** The list of all packs that require this license agreement */
    private java.util.List forPacks;

    /** The list of selected packs that require this license agreement */
    private java.util.List selectedPacks;
    private String selectedPacksText;


    /**
     *  The constructor.
     *
     * @param  idata   The installation data.
     * @param  parent  Description of the Parameter
     */
    public CondHTMLLicencePanel(InstallerFrame parent, InstallData idata)
    {
        super(parent, idata);

        this.parent = parent;
        instanceNumber = instanceCount++;

        // We initialize our layout
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // We load the header information
        loadHeader();

        // We load the information about which packages require this license
        loadPackList();

        // We put our components
        headerArea = new MultiLineLabel("");
        headerArea.setAlignmentX(LEFT_ALIGNMENT);
        add(headerArea);

        subpanel = new JPanel();
        subpanel.setAlignmentX(LEFT_ALIGNMENT);
        add(subpanel);
        layout = new GridBagLayout();
        gbConstraints = new GridBagConstraints();
        subpanel.setLayout(layout);

        try
        {
            textArea = new JEditorPane();
            textArea.setEditable(false);
            textArea.addHyperlinkListener(this);
            JScrollPane scroller = new JScrollPane(textArea);
            textArea.setPage(loadLicence());
            parent.buildConstraints(gbConstraints, 0, 1, 2, 1, 1.0, 1.0);
            gbConstraints.anchor = GridBagConstraints.CENTER;
            gbConstraints.fill = GridBagConstraints.BOTH;
            layout.addLayoutComponent(scroller, gbConstraints);
            subpanel.add(scroller);
        }
        catch (Exception err)
        {
            err.printStackTrace();
        }

        agreeLabel = new JLabel(parent.langpack.getString("LicencePanel.agree"),
            parent.icons.getImageIcon("help"), JLabel.TRAILING);
        parent.buildConstraints(gbConstraints, 0, 2, 2, 1, 1.0, 0.0);
        gbConstraints.anchor = GridBagConstraints.WEST;
        gbConstraints.fill = GridBagConstraints.NONE;
        layout.addLayoutComponent(agreeLabel, gbConstraints);
        subpanel.add(agreeLabel);

        ButtonGroup group = new ButtonGroup();

        yesRadio = new JRadioButton(parent.langpack.getString("LicencePanel.yes"), false);
        group.add(yesRadio);
        parent.buildConstraints(gbConstraints, 0, 3, 1, 1, 0.5, 0.0);
        gbConstraints.anchor = GridBagConstraints.WEST;
        layout.addLayoutComponent(yesRadio, gbConstraints);
        subpanel.add(yesRadio);
        yesRadio.addActionListener(this);

        noRadio = new JRadioButton(parent.langpack.getString("LicencePanel.no"), false);
        group.add(noRadio);
        parent.buildConstraints(gbConstraints, 1, 3, 1, 1, 0.5, 0.0);
        gbConstraints.anchor = GridBagConstraints.EAST;
        layout.addLayoutComponent(noRadio, gbConstraints);
        subpanel.add(noRadio);
        noRadio.addActionListener(this);
    }


    /**  Loads the header text.  */
    private void loadHeader()
    {
        try
        {
            String resNamePrifix = "CondHTMLLicencePanel"+instanceNumber+".header";
            headerFmt = new MessageFormat
                (parent.langpack.getString(resNamePrifix));
        }
        catch (Exception err)
        {
            headerFmt = null;
        }
    }


    /**
     *  Loads the license text.
     *
     * @return    The license text URL.
     */
    private URL loadLicence()
    {
        String resNamePrifix = "CondHTMLLicencePanel"+instanceNumber+".licence";
        try
        {
            return ResourceManager.getInstance().getURL(resNamePrifix);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return null;
    }


    /**  Loads the list of packages which require this license.  */
    private void loadPackList()
    {
        try {
            forPacks = new ArrayList();

            String resName = "CondHTMLLicencePanel"+instanceNumber+".packList";
            InputStream input = parent.getResource (resName);
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = in.readLine()) != null)
                forPacks.add(line);
        } catch (Exception e) {
        }
    }


    /**  Loads the list of packages which require this license.  */
    private void getSelectedPackages()
    {
        selectedPacks = new LinkedList();
        selectedPacksText = "";

        for (int i = 0; i < idata.selectedPacks.size (); i++)
        {
            Pack p = (Pack)idata.selectedPacks.get (i);
            String packName = getI18NPackName(p);
            String packID = p.id;
            if (forPacks.contains(packID)) {
                selectedPacks.add(p);
                selectedPacksText = selectedPacksText + ", " + packName;
            }
        }

        String headerText = null;
        if (!selectedPacks.isEmpty()) try {
            selectedPacksText = selectedPacksText.substring(2);
            System.out.println("selectedPacksText="+selectedPacksText);
            if (headerFmt == null) {
                headerText = parent.langpack.getString("LicencePanel.info");
            } else {
                Object[] args = new Object[2];
                args[0] = new Integer(selectedPacks.size());
                args[1] = selectedPacksText;
                headerText = headerFmt.format
                    (args, new StringBuffer(), null).toString();
            }
        } catch (Exception e) {
            headerText = parent.langpack.getString("LicencePanel.info");
        }
        headerArea.setText(headerText == null ? "" : headerText);
    }

    private String getI18NPackName(Pack pack)
    {
        // Internationalization code
        String packName = pack.name;
        String key = pack.id;
        if (parent.langpack != null && pack.id != null && !"".equals(pack.id))
        {
            packName = parent.langpack.getString(key);
        }
        if ("".equals(packName) || key == null || key.equals(packName))
        {
            packName = pack.name;
        }
        return (packName);
    }


    /**
     *  Actions-handling method (here it launches the installation).
     *
     * @param  e  The event.
     */
    public void actionPerformed(ActionEvent e)
    {
        if (yesRadio.isSelected() || noRadio.isSelected())
            parent.unlockNextButton();
        else
            parent.lockNextButton();
    }


    /**
     *  Indicates wether the panel has been validated or not.
     *
     * @return    true if the user agrees with the license, false otherwise.
     */
    public boolean isValidated()
    {
        if (yesRadio.isSelected() || selectedPacks.isEmpty())
            return true;

        else if (noRadio.isSelected())
        {
            String title = parent.langpack.getString("LicencePanel.deselect.title");

            Object[] message = new Object[3];
            message[0] = parent.langpack.getString("LicencePanel.deselect.message1")
                .split("\\\\n");
            message[1] = "        " + selectedPacksText;
            message[2] = parent.langpack.getString("LicencePanel.deselect.message2")
                .split("\\\\n");

            int choice = JOptionPane.showConfirmDialog
                (this, message, title, JOptionPane.OK_CANCEL_OPTION);
            if (choice == JOptionPane.OK_OPTION) {
                deselectPacks();
                return true;
            } else
                return false;
        }

        else
            return false;
    }


    /** The user has changed their mind and wants to deselect the
     * packages in question.  Make it so.
     */
    private void deselectPacks() {
        // Remove the packages from the globally selected list.
        idata.selectedPacks.removeAll(selectedPacks);
        // Update our internal state to stay consistent
        selectedPacks.clear();
        selectedPacksText = "";

        // now we need to tell the packs panel to recreate its XML data.
        for (int i = 0;  i < idata.panels.size();   i++) {
            IzPanel panel = (IzPanel) idata.panels.get(i);
            if ("packsPanel".equals(panel.metadata.getPanelid())) {
                IXMLElement panelXML = idata.xmlData.getChildAtIndex(i);
                while (panelXML.hasChildren())
                    panelXML.removeChild(panelXML.getChildAtIndex(0));
                panel.makeXMLData(panelXML);
                break;
            }
        }
    }

    /**
     *  Hyperlink events handler.
     *
     * @param  e  The event.
     */
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        try
        {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                textArea.setPage(e.getURL());
        }
        catch (Exception err)
        {}
    }


    /**  Called when the panel becomes active.  */
    public void panelActivate()
    {
        System.out.println("condlicense panel activate");
        getSelectedPackages();

        if (selectedPacks.isEmpty())
            parent.skipPanel ();

        else if (!yesRadio.isSelected() && !noRadio.isSelected()) {
            parent.lockNextButton();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    yesRadio.requestFocusInWindow();
                }});
        }
    }
}
