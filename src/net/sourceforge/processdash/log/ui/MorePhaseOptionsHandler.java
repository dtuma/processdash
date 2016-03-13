// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.defects.DefectPhase;
import net.sourceforge.processdash.log.defects.DefectPhaseList;
import net.sourceforge.processdash.process.WorkflowInfo.Phase;
import net.sourceforge.processdash.process.WorkflowInfo.Workflow;
import net.sourceforge.processdash.ui.lib.JOptionPaneClickHandler;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.util.StringUtils;

public class MorePhaseOptionsHandler implements ActionListener {

    private DefectPhaseList workflowPhases, processPhases;

    private boolean isInjected;

    private DefectPhase lastSelectedPhase;

    private JTree phaseOptionsTree;

    public static final DefectPhase MORE_OPTIONS = new DefectPhase(".");

    private static final Resources resources = DefectDialog.resources;


    public MorePhaseOptionsHandler(JComboBox cb,
            DefectPhaseList workflowPhases, DefectPhaseList processPhases,
            boolean isInjected) {
        this.workflowPhases = workflowPhases;
        this.processPhases = processPhases;
        this.isInjected = isInjected;
        cb.addItem(MORE_OPTIONS);
        cb.addActionListener(this);
        lastSelectedPhase = (DefectPhase) cb.getSelectedItem();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JComboBox cb = (JComboBox) e.getSource();
        DefectPhase selectedItem = (DefectPhase) cb.getSelectedItem();
        if (selectedItem == MORE_OPTIONS) {
            cb.hidePopup();
            DefectPhase p = showMorePhasesDialog(cb);
            if (p != null)
                DefectDialog.phaseComboSelect(cb, lastSelectedPhase = p);
            else
                cb.setSelectedItem(lastSelectedPhase);
        } else {
            lastSelectedPhase = selectedItem;
        }
    }

    private DefectPhase showMorePhasesDialog(JComboBox cb) {
        if (phaseOptionsTree == null)
            phaseOptionsTree = buildTree();

        // display a dialog to the user prompting for a phase selection
        String title = resources.getString("More_Options.Window_Title");
        String prompt = resources.getString("More_Options."
                + (isInjected ? "Injected" : "Removed") + "_Prompt");
        Object[] message = new Object[] { prompt,
                new JScrollPane(phaseOptionsTree),
                new JOptionPaneTweaker.GrabFocus(phaseOptionsTree) };
        setSelectedPhase();
        int userChoice = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(cb), message, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
        if (userChoice != JOptionPane.OK_OPTION)
            return null;

        // Return the phase object selected by the user
        TreePath selPath = phaseOptionsTree.getSelectionPath();
        if (selPath == null)
            return null;
        DefaultMutableTreeNode selNode = (DefaultMutableTreeNode) selPath
                .getLastPathComponent();
        Object selItem = selNode.getUserObject();
        if (selItem instanceof DefectPhase)
            return (DefectPhase) selItem;
        else
            return null;
    }

    private void setSelectedPhase() {
        phaseOptionsTree.clearSelection();
        for (int row = 0; row < phaseOptionsTree.getRowCount(); row++)
            phaseOptionsTree.collapseRow(row);
        phaseOptionsTree.scrollRowToVisible(0);

        DefaultMutableTreeNode selNode = getNodeForSelectedPhase();
        if (selNode != null) {
            TreePath path = new TreePath(selNode.getPath());
            phaseOptionsTree.setSelectionPath(path);
            phaseOptionsTree.scrollPathToVisible(path);
        }
    }

    private DefaultMutableTreeNode getNodeForSelectedPhase() {
        if (lastSelectedPhase == null)
            return null;
        String phaseID = lastSelectedPhase.getTerminalPhaseID();

        TreeNode root = (TreeNode) phaseOptionsTree.getModel().getRoot();
        Enumeration<TreeNode> workflows = root.children();
        while (workflows.hasMoreElements()) {
            TreeNode oneWorkflow = workflows.nextElement();
            Enumeration<DefaultMutableTreeNode> phases = oneWorkflow.children();
            while (phases.hasMoreElements()) {
                DefaultMutableTreeNode phaseNode = phases.nextElement();
                DefectPhase onePhase = (DefectPhase) phaseNode.getUserObject();
                if (phaseID == null) {
                    if (onePhase.phaseID == null
                            && lastSelectedPhase.legacyPhase
                                    .equals(onePhase.legacyPhase))
                        return phaseNode;
                } else if (phaseID.equals(onePhase.phaseID)) {
                    return phaseNode;
                }
            }
        }

        return null;
    }

    private JTree buildTree() {
        // build a tree model to contain the phase options.
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();

        // Add all known workflows to the model.
        for (Workflow workflow : workflowPhases.workflowInfo.getWorkflows()) {
            DefaultMutableTreeNode workflowNode = new DefaultMutableTreeNode(
                    workflow.getWorkflowName());
            root.add(workflowNode);
            for (Phase onePhase : workflow.getPhases()) {
                DefectPhase dp = new DefectPhase(onePhase);
                workflowNode.add(new DefaultMutableTreeNode(dp));
            }
        }

        // Add MCF phases to the model.
        if (processPhases != null && !processPhases.isEmpty()) {
            String processName = processPhases.get(0).processName;
            if (!StringUtils.hasValue(processName))
                processName = resources.getString("More_Options.Process_Phases");
            DefaultMutableTreeNode mcfNode = new DefaultMutableTreeNode(
                    processName);
            root.add(mcfNode);
            for (DefectPhase onePhase : processPhases)
                mcfNode.add(new DefaultMutableTreeNode(onePhase));
        }

        // Create a JTree for this model.
        JTree result = new JTree(root);
        result.setRootVisible(false);
        result.setShowsRootHandles(true);
        result.setToggleClickCount(4);
        result.getSelectionModel().setSelectionMode(
            TreeSelectionModel.SINGLE_TREE_SELECTION);
        result.setVisibleRowCount(10);
        new JOptionPaneClickHandler().install(result);
        return result;
    }

}