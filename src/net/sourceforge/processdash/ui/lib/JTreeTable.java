/*
 * @(#)JTreeTable.java  1.2 98/10/27
 *
 * Copyright 1997, 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistribution of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * Redistribution in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

package net.sourceforge.processdash.ui.lib;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;

import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;

import java.awt.event.MouseEvent;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This example shows how to create a simple JTreeTable component,
 * by using a JTree as a renderer (and editor) for the cells in a
 * particular column in the JTable.
 *
 * @version 1.2 10/27/98
 *
 * @author Philip Milne
 * @author Scott Violet
 */
public class JTreeTable extends JTable {
    /** A subclass of JTree. */
    protected TreeTableCellRenderer tree;
    protected TreeTableModelAdapter adapter;
    protected Map trees = new HashMap();
    protected Map adapters = new HashMap();


    protected ListToTreeSelectionModelWrapper selectionWrapper;

    public JTreeTable(TreeTableModel treeTableModel) {
        super();

        // Create the tree. It will be used as a renderer and editor.
        tree = new TreeTableCellRenderer(treeTableModel);
        trees.put(treeTableModel, tree);

        // Install a tableModel representing the visible rows in the tree.
        super.setModel
            (adapter = new TreeTableModelAdapter(treeTableModel, tree));
        adapters.put(treeTableModel, adapter);

        // Force the JTable and JTree to share their row selection models.
        selectionWrapper = new ListToTreeSelectionModelWrapper();
        tree.setSelectionModel(selectionWrapper);
        setSelectionModel(selectionWrapper.getListSelectionModel());

        // Install the tree editor renderer and editor.
        setDefaultRenderer(TreeTableModel.class, tree);
        setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());

        // No grid.
        setShowGrid(false);

        // No intercell spacing
        setIntercellSpacing(new Dimension(0, 0));

        // And update the height of the trees row to match that of
        // the table.
        if (tree.getRowHeight() < 1) {
            // Metal looks better like this.
            setRowHeight(18);
        } else if (getRowHeight() > 1) {
            setRowHeight(getRowHeight());
        }
    }

    public void setTreeTableModel(TreeTableModel treeTableModel) {
        // remove the selection model from the outgoing tree, so it doesn't
        // become shared by multiple trees
        tree.setSelectionModel(null);

        if (trees.containsKey(treeTableModel)) {
            // reuse the previously created tree object, and its associated
            // adapter;  this will allow the tree nodes to have the same
            // expanded/collapsed properties as they had when the user
            // last saw that tree.
            tree = (TreeTableCellRenderer) trees.get(treeTableModel);
            adapter = (TreeTableModelAdapter) adapters.get(treeTableModel);

        } else {
            // Create the tree. It will be used as a renderer and editor.
            tree = new TreeTableCellRenderer(treeTableModel);
            trees.put(treeTableModel, tree);

            // Install a tableModel representing the visible rows in the tree.
            adapter = new TreeTableModelAdapter(treeTableModel, tree);
            adapters.put(treeTableModel, adapter);

            // Set the row height to be the same as the table
            if (getRowHeight() > 1)
                tree.setRowHeight(getRowHeight());
        }

        // Force the JTable and JTree to share their row selection models.
        tree.setSelectionModel(selectionWrapper);

        // install the data model for the table.
        super.setModel(adapter);

        // Install the tree editor renderer and editor.
        setDefaultRenderer(TreeTableModel.class, tree);
    }


    public void dispose() {
        Iterator i = adapters.values().iterator();
        while (i.hasNext()) {
            TreeTableModelAdapter adapter = (TreeTableModelAdapter) i.next();
            adapter.dispose();
        }
    }

    /**
     * Overridden to message super and forward the method to the tree.
     * Since the tree is not actually in the component hieachy it will
     * never receive this unless we forward it in this manner.
     */
    public void updateUI() {
        super.updateUI();
        if(tree != null) {
            tree.updateUI();
        }
        // Use the tree's default foreground and background colors in the
        // table.
        LookAndFeel.installColorsAndFont(this, "Tree.background",
                                         "Tree.foreground", "Tree.font");
    }

    /* Workaround for BasicTableUI anomaly. Make sure the UI never tries to
     * paint the editor. The UI currently uses different techniques to
     * paint the renderers and editors and overriding setBounds() below
     * is not the right thing to do for an editor. Returning -1 for the
     * editing row in this case, ensures the editor is never painted.
     */
    public int getEditingRow() {
        if (editingColumn != -1
                && getColumnClass(editingColumn) == TreeTableModel.class)
            return -1;
        else
            return editingRow;
    }

    /**
     * Overridden to pass the new rowHeight to the tree.
     */
    public void setRowHeight(int rowHeight) {
        super.setRowHeight(rowHeight);
        if (tree != null && tree.getRowHeight() != rowHeight) {
            tree.setRowHeight(getRowHeight());
        }
    }

    /**
     * Returns the tree that is being shared between the model.
     */
    public JTree getTree() {
        return tree;
    }

    public void setPreferredTreeSizeFollowsRowSize(boolean p) {
        tree.calculatePreferredSizeByRow = p;
    }

    /**
     * Method to convert between table rows and tree rows - meant for
     * overriding in a subclass.
     */
    protected int convertTableRowToTreeRow(int tableRow) {
        return tableRow;
    }

    /**
     * A TreeCellRenderer that displays a JTree.
     */
    public class TreeTableCellRenderer extends JTree implements
                 TableCellRenderer {
        /** Last table/tree row asked to renderer. */
        protected int visibleRow;
        /** True if we should report a preferred size based upon the size of
         *  the visible row */
        protected boolean calculatePreferredSizeByRow;

        public TreeTableCellRenderer(TreeModel model) {
            super(model);
            calculatePreferredSizeByRow = true;
        }

        /**
         * updateUI is overridden to set the colors of the Tree's renderer
         * to match that of the table.
         */
        public void updateUI() {
            super.updateUI();
            // Make the tree's cell renderer use the table's cell selection
            // colors.
            TreeCellRenderer tcr = getCellRenderer();
            if (tcr instanceof DefaultTreeCellRenderer) {
                DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer)tcr);
                // For 1.1 uncomment this, 1.2 has a bug that will cause an
                // exception to be thrown if the border selection color is
                // null.
                // dtcr.setBorderSelectionColor(null);
                dtcr.setTextSelectionColor(UIManager.getColor
                                           ("Table.selectionForeground"));
                dtcr.setBackgroundSelectionColor(UIManager.getColor
                                                ("Table.selectionBackground"));
            }
        }

        /**
         * Sets the row height of the tree, and forwards the row height to
         * the table.
         */
        public void setRowHeight(int rowHeight) {
            if (rowHeight > 0) {
                super.setRowHeight(rowHeight);
                if (JTreeTable.this != null &&
                    JTreeTable.this.getRowHeight() != rowHeight) {
                    JTreeTable.this.setRowHeight(getRowHeight());
                }
            }
        }

        /**
         * This is overridden to set the height to match that of the JTable.
         */
        public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, 0, w, getRowHeight()*getRowCount());
        }

        /**
         * Sublcassed to translate the graphics such that the last visible
         * row will be drawn at 0,0.
         */
        public void paint(Graphics g) {
            g.translate(0, -visibleRow * getRowHeight());
            super.paint(g);
        }

        /**
         * TreeCellRenderer method. Overridden to update the visible row.
         */
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column) {
            if(isSelected)
                setBackground(table.getSelectionBackground());
            else
                setBackground(table.getBackground());

            visibleRow = convertTableRowToTreeRow(row);
            return this;
        }

        /**
         * Subclassed to fetch the tooltip for the visible row.
         */
        public String getToolTipText(MouseEvent event) {
            event.translatePoint(0, visibleRow * getRowHeight());
            return super.getToolTipText(event);
        }

        public Dimension getPreferredSize() {
            if (!calculatePreferredSizeByRow)
                return super.getPreferredSize();

            Rectangle visibleRowBounds = getRowBounds(visibleRow);
            double rightEdge = visibleRowBounds.getWidth() + visibleRowBounds.getX();

            // We have to reduce the preferred height in order for the
            // datatips tooltips to show up correctly.

            return new Dimension((int)rightEdge,
                                 (int)visibleRowBounds.getHeight()-1);
        }
    }


    /**
     * TreeTableCellEditor implementation. Component returned is the
     * JTree.
     */
    public class TreeTableCellEditor extends AbstractCellEditor implements
                 TableCellEditor {
        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int r, int c) {
            return tree;
        }

        /**
         * Overridden to return false, and if the event is a mouse event
         * it is forwarded to the tree.<p>
         * The behavior for this is debatable, and should really be offered
         * as a property. By returning false, all keyboard actions are
         * implemented in terms of the table. By returning true, the
         * tree would get a chance to do something with the keyboard
         * events. For the most part this is ok. But for certain keys,
         * such as left/right, the tree will expand/collapse where as
         * the table focus should really move to a different column. Page
         * up/down should also be implemented in terms of the table.
         * By returning false this also has the added benefit that clicking
         * outside of the bounds of the tree node, but still in the tree
         * column will select the row, whereas if this returned true
         * that wouldn't be the case.
         * <p>By returning false we are also enforcing the policy that
         * the tree will never be editable (at least by a key sequence).
         */
        public boolean isCellEditable(EventObject e) {
            if (e instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) e;
                if ((me.getModifiersEx() & ANY_MODIFIER_KEY_DOWN) != 0)
                    return false;
                for (int counter = getColumnCount() - 1; counter >= 0;
                     counter--) {
                    if (getColumnClass(counter) == TreeTableModel.class) {
                        MouseEvent newME = new MouseEvent(tree, me.getID(),
                                   me.getWhen(), me.getModifiers(),
                                   me.getX() - getCellRect(0, counter, true).x,
                                   me.getY(), me.getClickCount(),
                                   me.isPopupTrigger());
                        tree.dispatchEvent(newME);

                        // The Mac look-and-feel doesn't expand/collapse
                        // until a mouse released event is received.  But
                        // mouse release events don't get routed through this
                        // method, so we have to simulate one instead.
                        if (MacGUIUtils.isMacOSX()
                                && me.getID() == MouseEvent.MOUSE_PRESSED) {
                            newME = new MouseEvent(tree,
                                    MouseEvent.MOUSE_RELEASED,
                                    me.getWhen(), me.getModifiers(),
                                    me.getX() - getCellRect(0, counter, true).x,
                                    me.getY(), me.getClickCount(),
                                    me.isPopupTrigger());
                            tree.dispatchEvent(newME);
                        }

                        break;
                    }
                }
            }
            return false;
        }

        public Object getCellEditorValue() { return null; }
    }
    private static final int ANY_MODIFIER_KEY_DOWN = MouseEvent.CTRL_DOWN_MASK
            | MouseEvent.SHIFT_DOWN_MASK | MouseEvent.META_DOWN_MASK
            | MouseEvent.ALT_DOWN_MASK | MouseEvent.ALT_GRAPH_DOWN_MASK;


    /**
     * ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel
     * to listen for changes in the ListSelectionModel it maintains. Once
     * a change in the ListSelectionModel happens, the paths are updated
     * in the DefaultTreeSelectionModel.
     */
    class ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel {
        /** Set to true when we are updating the ListSelectionModel. */
        protected boolean         updatingListSelectionModel;

        public ListToTreeSelectionModelWrapper() {
            super();
            getListSelectionModel().addListSelectionListener
                                    (createListSelectionListener());
        }

        /**
         * Returns the list selection model. ListToTreeSelectionModelWrapper
         * listens for changes to this model and updates the selected paths
         * accordingly.
         */
        ListSelectionModel getListSelectionModel() {
            return listSelectionModel;
        }

        /**
         * This is overridden to set <code>updatingListSelectionModel</code>
         * and message super. This is the only place DefaultTreeSelectionModel
         * alters the ListSelectionModel.
         */
        public void resetRowSelection() {
            if(!updatingListSelectionModel) {
                updatingListSelectionModel = true;
                try {
                    super.resetRowSelection();
                }
                finally {
                    updatingListSelectionModel = false;
                }
            }
            // Notice how we don't message super if
            // updatingListSelectionModel is true. If
            // updatingListSelectionModel is true, it implies the
            // ListSelectionModel has already been updated and the
            // paths are the only thing that needs to be updated.
        }

        /**
         * Creates and returns an instance of ListSelectionHandler.
         */
        protected ListSelectionListener createListSelectionListener() {
            return new ListSelectionHandler();
        }

        /**
         * If <code>updatingListSelectionModel</code> is false, this will
         * reset the selected paths from the selected rows in the list
         * selection model.
         */
        protected void updateSelectedPathsFromSelectedRows() {
            if(!updatingListSelectionModel) {
                updatingListSelectionModel = true;
                try {
                    // This is way expensive, ListSelectionModel needs an
                    // enumerator for iterating.
                    int        min = listSelectionModel.getMinSelectionIndex();
                    int        max = listSelectionModel.getMaxSelectionIndex();

                    clearSelection();
                    if(min != -1 && max != -1) {
                        for(int counter = min; counter <= max; counter++) {
                            if(listSelectionModel.isSelectedIndex(counter)) {
                                TreePath     selPath = tree.getPathForRow
                                                            (counter);

                                if(selPath != null) {
                                    addSelectionPath(selPath);
                                }
                            }
                        }
                    }
                }
                finally {
                    updatingListSelectionModel = false;
                }
            }
        }

        /**
         * Class responsible for calling updateSelectedPathsFromSelectedRows
         * when the selection of the list changse.
         */
        class ListSelectionHandler implements ListSelectionListener {
            public void valueChanged(ListSelectionEvent e) {
                updateSelectedPathsFromSelectedRows();
            }
        }
    }

}
