
package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventObject;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellEditor;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;


/** Custom editor for {@link WBSJTable}
 */
public class WBSNodeEditor extends AbstractCellEditor
    implements TableCellEditor
{

    /** The table we are editing nodes for */
    private WBSJTable table;
    /** The wbs model for the given table */
    private WBSModel wbsModel;
    /** A map translating node types to appropriate icons */
    private Map iconMap;

    /** the component we use for editing */
    private EditorComponent editorComponent;
    /** A popup menu to display when the user clicks on a node icon */
    private JMenu iconMenu;

    /** the WBSNode we are currently editing */
    private WBSNode editingNode = null;
    /** the row number of the node we are currently editing */
    private int rowNumber = -1;
    /** An event we can use to restart an interrupted editing session */
    private EventObject editingRestartEvent = null;


    /** Create a new WBSNodeEditor */
    public WBSNodeEditor(WBSJTable table, WBSModel wbsModel,
                         Map iconMap, JMenu iconMenu) {
        this.table = table;
        this.wbsModel = wbsModel;
        this.iconMap = iconMap;

        if (iconMenu == null)
            this.iconMenu = buildDefaultIconMenu(iconMap);
        else
            this.iconMenu = configureMenu(iconMenu, iconMap);

        this.editorComponent = new EditorComponent();
    }



    ///////////////////////////////////////////////////////////////////
    // implementation of javax.swing.table.TableCellEditor interface
    ///////////////////////////////////////////////////////////////////

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column)
    {
        // record the current editing state
        rowNumber = row;
        editingNode = (WBSNode) value;
        UndoList.addCellEditor(table, this);
        // update the contents/appearance of the editor component
        editorComponent.updateInfo();
        editorComponent.setText(editingNode.getName());
        // restart editing where we left off, if possible.
        editorComponent.maybeRestoreEditorState(editingRestartEvent);
        editingRestartEvent = null;

        return editorComponent;
    }

    ///////////////////////////////////////////////////////////////////
    // implementation of javax.swing.CellEditor interface
    ///////////////////////////////////////////////////////////////////


    public Object getCellEditorValue() { return editingNode; }



    /** Determine whether editing should start because of the given event
     */
    public boolean isCellEditable(EventObject e) {

        // When the user presses F2, we are inexplicably passed "null" for
        // the event.  We should return true in this case.
        if (e == null) return true;

        // if the event is an instance of our "RestartEditingEvent", then
        // there is no question that editing should restart.
        if (e instanceof EditorComponent.RestartEditingEvent) {
            editingRestartEvent = e;
            return true;
        }

        // mouse drag, shift-click, and ctrl-click events are used to alter
        // the table selection.  Don't start editing for such events.
        if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent)e;
            if (me.getID() == MouseEvent.MOUSE_DRAGGED ||
                me.isShiftDown() || me.isControlDown())
                return false;
        }

        // determine which part of the WBS node the user clicked. (This will
        // also set the private field "clickedNode" to indicate the wbs node
        // in question.)
        int clickLocation = getClickedItem(e);
        switch (clickLocation) {
        case CLICKED_NONE:
        case CLICKED_WHITESPACE:
            // if this isn't a user click, or if they clicked on whitespace,
            // don't begin editing.
            return false;

        case CLICKED_EXPANDER:
            // if the user clicked on the "expand/collapse" icon, forward
            // their request and don't begin editing.
            clickedNode.setExpanded(!clickedNode.isExpanded());
            wbsModel.recalcRows();
            return false;

        case CLICKED_ICON:
        case CLICKED_TEXT:
            // if the user clicked on the icon or the node name, begin editing.
            return true;
        }

        // for other user events, don't start editing.
        return false;
    }



    /** Determine whether a node should be selected in response to the
     * given user event.
     */
    public boolean shouldSelectCell(EventObject e)
    {
        // if we are restarting an editing session, select the edited cell.
        if (e instanceof EditorComponent.RestartEditingEvent)
            return true;

        // if this isn't a user click, or if they clicked on the
        // "expand/collapse" icon, don't select this cell.
        switch (getClickedItem(e)) {
        case CLICKED_NONE: return false;
        case CLICKED_EXPANDER: return false;
        }

        // if we are currently editing a node, and this is a mouse drag event
        if (e instanceof MouseEvent && editingNode != null) {
            MouseEvent me = (MouseEvent)e;
            if (me.getID() == MouseEvent.MOUSE_DRAGGED) {

                // mouse drags that occur when the icon menu is visible are
                // simply interactions with the popup menu. Ignore them -
                // they belong to the popup menu.
                if (iconMenu.isPopupMenuVisible())
                    return false;

                // other mouse drag events signify table selections.  We
                // should return true, but first we'll call stopCellEditing.
                stopCellEditing();
            }
        }
        return true;
    }



    /** Tell the editor to stop editing and save the current edited value.
     */
    public boolean stopCellEditing() {
        //System.out.println("stopCellEditing");

        if (editingNode != null) {
            if (iconMenu.isPopupMenuVisible())
                return false;

            String oldName = editingNode.getName();
            String newName = editorComponent.getText();
            newName = (newName == null ? "" : newName.trim());

            // if the name of the node has actually changed
            if (!newName.equals(oldName)) {
                // save the new name of the node.
                editingNode.setName(newName);
                // changing this node's name might cause subsequent nodes
                // names to become invalid (which means they would need to
                // be displayed in red).  Fire a table event so the nodes
                // will be repainted.
                wbsModel.fireTableRowsUpdated(rowNumber,
                                              wbsModel.getRowCount()-1);
                UndoList.madeChange(table, "Rename WBS element");
            }
        }

        // We're no longer editing - save that state information.
        editingNode = null;
        rowNumber = -1;

        return super.stopCellEditing();
    }


    /** Message from the JTable to cancel editing and not accept any
     * partially edited value.
     *
     * Because we won't require the user to explicitly start an editing
     * session, the concept of canceling an editing session doesn't
     * make much sense.  Thus, we just redirect to stopCellEditing().
     */
    public void cancelCellEditing() {
        //System.out.println("cancelCellEditing called");
        stopCellEditing();
    }




    ///////////////////////////////////////////////////////////////////
    // misc methods specific to WBSNodeEditor
    ///////////////////////////////////////////////////////////////////


    /** Return an event which can be used to restart editing
     * where we left off */
    public EventObject getRestartEditingEvent() {
        return editorComponent.getRestartEditingEvent();
    }

    /** Update the appearance of the editor, in response to an external change
     * that affects the icon we should display.
     */
    public void updateIconAppearance() {
        editorComponent.updateInfo();  // get the updated icon information
        //editorComponent.invalidate();  // not necessary?
        editorComponent.repaint();     // repaint the editor component.
    }


    // constants returned by the getClickedItem method.
    private static final int CLICKED_NONE = 0;
    private static final int CLICKED_WHITESPACE = 1;
    private static final int CLICKED_EXPANDER = 2;
    private static final int CLICKED_ICON = 3;
    private static final int CLICKED_TEXT = 4;

    /** Check the event; if it is a mouse click, determine what node the user
     * clicked on, and what part of that node they clicked.
     */
    private int getClickedItem(EventObject e) {
        if (!(e instanceof MouseEvent)) {
            clickedNode = null;
            return CLICKED_NONE;
        }

        // determine what row/column they clicked, and get its bounding rect
        MouseEvent me = (MouseEvent) e;
        Point p = me.getPoint();
        int columnNumber = table.columnAtPoint(p);
        int rowNumber = table.rowAtPoint(p);
        Rectangle r = table.getCellRect(rowNumber, columnNumber, true);
        // calculate x and y relative to the table cell origin
        int ourXPos = p.x - r.x;
        int outYPos = p.y - r.y;

        // determine which WBSNode the user clicked on.
        clickedNode = wbsModel.getNodeForRow(rowNumber);
        if (clickedNode == null) return CLICKED_NONE;

        // translate the x position according to the indentation level of
        // clicked node, and determine which part of the node was clicked.
        int xDelta = ourXPos - clickedNode.getIndentLevel() * HORIZ_SPACING;
        if (xDelta > HORIZ_SPACING)
            return CLICKED_TEXT;
        else if (xDelta > 0)
            return CLICKED_ICON;
        else if (xDelta > -HORIZ_SPACING && !wbsModel.isLeaf(clickedNode))
            return CLICKED_EXPANDER;
        else
            return CLICKED_WHITESPACE;
    }
    /** The last node the user clicked on */
    private WBSNode clickedNode = null;

    /** Convenience declaration */
    private static final int HORIZ_SPACING =
        WBSNodeRenderer.ICON_HORIZ_SPACING;


    /** Create a default icon menu if none was provided. */
    private JMenu buildDefaultIconMenu(Map iconMap) {
        JMenu result = new JMenu();
        Iterator i = iconMap.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            String type = (String) e.getKey();
            Icon icon = (Icon) e.getValue();
            if (type != null) {
                JMenuItem menuItem = new JMenuItem(type, icon);
                menuItem.setActionCommand(type);
                menuItem.addActionListener(ICON_MENU_LISTENER);
                result.add(menuItem);
            }
        }
        return result;
    }

    /** Add icons and actions to the items in the given menu. */
    private JMenu configureMenu(JMenu iconMenu, Map iconMap) {
        for (int i = iconMenu.getItemCount();   i-- > 0; ) {
            JMenuItem item = iconMenu.getItem(i);
            if (item instanceof JMenu)
                configureMenu((JMenu) item, iconMap);
            else {
                String nodeType = getNodeTypeForMenuItem(item);
                if (nodeType == null) continue;
                item.setIcon((Icon) iconMap.get(nodeType));
                item.addActionListener(ICON_MENU_LISTENER);
            }
        }
        return iconMenu;
    }

    /** Extract the node type from an item in an icon menu. */
    private String getNodeTypeForMenuItem(JMenuItem menuItem) {
        if (menuItem == null) return null;
        String result = menuItem.getActionCommand();
        if (result == null) result = menuItem.getText();
        if (iconMap.containsKey(result))
            return result;
        else
            return null;
    }




    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    /** Component facilitating the editing of a WBS node.
     */
    private class EditorComponent extends JComponent {

        /** An invisible menu bar that can display and manage the icon menu */
        JMenuBar menuBar;
        /** A text field where the user can edit the node name. */
        JTextField textField;
        /** A component which can and listen for mouse events on the icon */
        JComponent iconListener;

        /** The indentation level of the edited node */
        int indentationLevel = 0;
        /** is the edited node currently expanded? */
        boolean isExpanded;
        /** is the edited node a leaf node */
        boolean isLeaf;
        /** the icon we should display for the node */
        Icon nodeIcon;
        /** The tool tip to display for the icon */
        String iconToolTip;
        /** The border to draw */
        Border border;
        /** Is the node type editable or read only? */
        boolean nodeTypeEditable;



        /** Constructs an EditorComponent object. */
        public EditorComponent() {
            setLayout(null);             // we'll handle our own layout.
            setBackground(Color.white);  // draw a white background.

            border = UIManager.getBorder("Table.focusCellHighlightBorder");

            // create the text field.  use a custom document to prevent
            // the user from entering invalid characters, and use a large
            // column size so the text field will use all the horizontal
            // space available in the table.
            textField = new JTextField(new WBSNodeNameDocument(), "", 9999);
            // don't display a border around the text field.
            textField.setBorder(BorderFactory.createEmptyBorder());
            this.add(textField);

            // create the menu bar containing the icon menu.
            menuBar = new JMenuBar();
            menuBar.add(iconMenu);
            this.add(menuBar);

            // create the icon listener
            iconListener = new IconListener();
            this.add(iconListener);

            // install the table's custom actions on the text field, so
            // they can be triggered even if editing is underway.
            table.installCustomActions(textField);

            // install a focus traversal action on the text field.
            textField.setFocusTraversalKeysEnabled(false);
            textField.getInputMap().put
                (KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.CTRL_MASK),
                 "FocusNext");
            textField.getActionMap().put("FocusNext",
                                         new FocusNextComponentAction());
        }

        /** Update the editor based on the currently edited node. (However,
         * don't change the text in the text field.) */
        public void updateInfo() {
            if (editingNode == null) return;

            indentationLevel = editingNode.getIndentLevel();
            isExpanded       = editingNode.isExpanded();
            isLeaf           = wbsModel.isLeaf(editingNode);
            Object iconObj   = WBSNodeRenderer.getIconForNode
                (table, iconMap, editingNode, wbsModel);
            if (iconObj instanceof ErrorValue) {
                iconToolTip = ((ErrorValue) iconObj).error;
                nodeIcon = (Icon) ((ErrorValue) iconObj).value;
                nodeTypeEditable = true;
            } else {
                iconToolTip = wbsModel.filterNodeType(editingNode);
                nodeIcon = (Icon) iconObj;
                nodeTypeEditable = editingNode.getType().equals(iconToolTip);
            }
            iconListener.setToolTipText(iconToolTip);
            iconMenu.setEnabled(nodeTypeEditable);
        }

        /** set the text to be displayed for the node */
        public void setText(String text) { textField.setText(text);    }
        /** Get the text the user has entered in the text field */
        public String getText()          { return textField.getText(); }

        /** Get an event we can use to restart editing. */
        public EventObject getRestartEditingEvent() {
            return new RestartEditingEvent();
        }

        /** Try to restart an interrupted editing session */
        public void maybeRestoreEditorState(EventObject e) {
            if (e instanceof RestartEditingEvent)
                ((RestartEditingEvent) e).restoreState(this);
        }


        /** Lays out this Container.  */
        public void doLayout() {
            Dimension cSize = getSize();

            int left = indentationLevel * HORIZ_SPACING;
            menuBar.setLocation(left, 0);
            menuBar.setBounds(left, 0, 0, cSize.height);

            iconListener.setLocation(left, 0);
            iconListener.setBounds(left, 0, 16, cSize.height);

            left += HORIZ_SPACING + 4;
            textField.setLocation(left, 0);
            textField.setBounds(left, 0, cSize.width - left, cSize.height);
        }

        /** Returns the preferred size for the Container.  */
        public Dimension getPreferredSize() {
            return new Dimension(3000, 16);
        }

        /** Paint the editor. */
        public void paint(Graphics g) {
            // paint the background.
            g.setColor(this.getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());

            // call super.paint to paint the text field.
            super.paint(g);

            // paint the border
            border.paintBorder(this, g, 0, 0, getWidth(), getHeight());

            Dimension size = getSize();

            // paint the expand/collapse icon
            if (indentationLevel > 0 && isLeaf == false) {
                Icon i = (isExpanded
                          ? WBSNodeRenderer.MINUS_ICON
                          : WBSNodeRenderer.PLUS_ICON);
                paintIcon(g, i, size, (indentationLevel-1)*HORIZ_SPACING + 5);
            }

            // paint the icon for this node.
            paintIcon(g, nodeIcon, size, indentationLevel*HORIZ_SPACING + 1);
        }

        /** paint an icon within this component, vertically centered */
        private void paintIcon(Graphics g, Icon i, Dimension size, int x) {
            int yLoc = (size.height - i.getIconHeight() + 1) / 2;
            if (yLoc < 0) yLoc = 0;
            i.paintIcon(this, g, x, yLoc);
        }


        /** override to redirect focus to the text field */
        public void requestFocus() { textField.requestFocus(); }

        /** override to set the text field's next focusable component, too */
        public void setNextFocusableComponent(Component aComponent) {
            textField.setNextFocusableComponent(aComponent);
            super.setNextFocusableComponent(aComponent);
        }



        /** Invisible component which handles mouse events on the node icon.
         *
         * This catches mouse clicks on the icon, and forwards them to the icon
         * menu.  It also responds to mouseOver events with a tool tip.
         */
        private class IconListener extends JComponent implements MouseListener
        {
            /** did the last mouse press hide the popup menu? */
            boolean pressHidPopup = false;

            public IconListener() { addMouseListener(this); }
            public void paint(Graphics g) { /* invisible */}

            // implementation of java.awt.event.MouseListener interface

            public void mouseClicked(MouseEvent e) {
                // forward mouse clicks to our menu.
                if (!pressHidPopup)
                    iconMenu.doClick(0);
            }
            public void mousePressed(MouseEvent e) {
                iconMenu.dispatchEvent(e);
                pressHidPopup = (iconMenu.isPopupMenuVisible() == false);
            }
            public void mouseReleased(MouseEvent e) { }
            public void mouseEntered(MouseEvent e) { }
            public void mouseExited(MouseEvent e) { }
        }


        /** Event object which can be used to resume an interrupted editing
         * session */
        class RestartEditingEvent extends EventObject implements Runnable {
            /** Which node were we editing when we saved editing state? */
            private WBSNode savedStateFromNode;
            // information about the caret position and text selection
            private int caretPos, selectionStart, selectionEnd, caretPixel;

            public RestartEditingEvent() {
                super(EditorComponent.this);
                savedStateFromNode = editingNode;
                if (savedStateFromNode == null) {
                    // if we aren't editing any node, set nonsense values.
                    caretPos = selectionStart = selectionEnd = caretPixel = -1;
                } else {
                    // save the position of the caret
                    caretPos = textField.getCaretPosition();
                    // save the start and end of the current text selection
                    selectionStart = textField.getSelectionStart();
                    selectionEnd = textField.getSelectionEnd();
                    // translate the caret position into an x pixel value,
                    // measured relative to the entire editor component.
                    try {
                        Rectangle r = textField.modelToView(caretPos);
                        caretPixel = r.x + textField.getX();
                    } catch (Exception e) { caretPixel = -1; }
                }
            }

            /** Restore the saved editing state */
            public void restoreState(EditorComponent c) {
                if (c != getSource()) return;

                if (savedStateFromNode != null) {
                    if (savedStateFromNode == editingNode) {
                        // if we are resuming editing on the same node as
                        // we were editing when we saved state, restore
                        // everything.
                        if (selectionStart == selectionEnd)
                            // no selection? just set caret position.
                            textField.setCaretPosition(caretPos);
                        else {
                            // set both the current selection and the caret pos
                            textField.setCaretPosition
                                (caretPos == selectionStart
                                 ? selectionEnd : selectionStart);
                            textField.moveCaretPosition(caretPos);
                        }

                    } else if (caretPixel != -1) {
                        // we are editing a different WBS node. try to position
                        // the caret the same distance from the left edge of
                        // the table as before.
                        doLayout();
                        int x = caretPixel - textField.getX();
                        Point p = new Point(x, textField.getHeight()/2);
                        int pos = textField.viewToModel(p);
                        textField.setCaretPosition(pos);
                    }
                }
                // grab the focus (must be deferred to avoid race conditions)
                SwingUtilities.invokeLater(this);
            }
            //public void run() { textField.grabFocus(); }
            public void run() { textField.requestFocus(); }
        }
    }



    /** Custom document for WBS node names - prohibits the insertion of tabs,
     * newlines, and slash characters into the name of the node.
     */
    private final class WBSNodeNameDocument extends PlainDocument {

        public void insertString(int offs, String str, AttributeSet a)
            throws BadLocationException
        {
            if (str == null) return;

            str = str.replace('\t', ' ');
            str = str.replace('\n', ' ');
            str = str.replace('/',  ',');
            super.insertString(offs, new String(str), a);
        }
    }


    /** Listener for icon menu events; changes the type of the edited
     * node and updates the icon.
     */
    private final class IconMenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (editingNode == null) return;
            if (!(e.getSource() instanceof JMenuItem)) return;

            // Get the node type from the JMenuItem's label.
            String type = getNodeTypeForMenuItem((JMenuItem) e.getSource());
            // if this menu item doesn't name a node type, do nothing.
            if (type == null) return;

            // if the name of the node matches the old node type, update the
            // name of the task to keep it in sync.
            String oldType = editingNode.getType();
            String nodeName = editorComponent.getText();
            nodeName = (nodeName == null ? "" : nodeName.trim());
            if (nodeName.length() == 0 || oldType.equals(nodeName + " Task")) {
                nodeName = type.substring(0, type.length() - 5);
                editorComponent.setText(nodeName);
                editingNode.setName(nodeName);
            }

            // save the new type of the node.
            editingNode.setType(type);

            // we must update not just the icon, but also the icons
            // below it in the table (since they may now be invalid
            // because of the change).  Although only descendants of
            // this node could be affected, just repaint all the
            // remaining rows (lazy).
            wbsModel.fireTableRowsUpdated(rowNumber,
                                          wbsModel.getRowCount()-1);
            // update the icon.
            updateIconAppearance();

            UndoList.madeChange(table, "Change type of WBS element");
        }
    }
    final IconMenuListener ICON_MENU_LISTENER = new IconMenuListener();


    private final class FocusNextComponentAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            stopCellEditing();
            table.transferFocus();
        }
    }

}
