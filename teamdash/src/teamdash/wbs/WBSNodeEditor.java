
package teamdash.wbs;

import java.awt.*;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.*;
import java.util.EventObject;
import javax.swing.table.TableCellEditor;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import java.awt.Component;
import javax.swing.CellEditor;
import java.awt.event.MouseEvent;
import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import teamdash.*;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.awt.event.KeyEvent;


public class WBSNodeEditor extends AbstractCellEditor
    implements TableCellEditor
{

    private JTable table;
    private JMenu menu;
    private WBSModel wbsModel;
    private Map iconMap;

    private EditorComponent editorComponent;
    private Action demoteAction, promoteAction;

    private WBSNode editingNode = null;
    private int rowNumber = -1;
    private EventObject editingRestartEvent = null;


    public WBSNodeEditor(JTable table, WBSModel wbsModel, Map iconMap) {
        this.table = table;
        this.wbsModel = wbsModel;
        this.iconMap = iconMap;

        menu = buildIconMenu(iconMap);

        this.editorComponent = new EditorComponent();
    }

    private JMenu buildIconMenu(Map iconMap) {
        JMenu result = new JMenu();
        Iterator i = iconMap.keySet().iterator();
        while (i.hasNext()) {
            String type = (String) i.next();
            if (type != null)
                result.add(new NodeIconMenuAction(type));
        }
        return result;
    }


    // implementation of javax.swing.table.TableCellEditor interface

    /**
     *
     */
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column)
    {
        System.out.println("get table cell editor component");
        rowNumber = row;
        editingNode = (WBSNode) value;
        editorComponent.updateInfo();
        editorComponent.setText(editingNode.getName());
        editorComponent.maybeRestoreEditorState(editingRestartEvent);
        editingRestartEvent = null;

        return editorComponent;
    }

    // implementation of javax.swing.CellEditor interface

    public Object getCellEditorValue() {
        System.out.println("get cell editor value");
        return editingNode;
    }

    /**
     *
     * @param param1 <description>
     * @return <description>
     */
    public boolean isCellEditable(EventObject e) {
        System.out.println("isCellEditable("+e+")");

        if (e == null) return true;

        if (e instanceof EditorComponent.RestartEditingEvent) {
            editingRestartEvent = e;
            return true;
        }

        if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent)e;
            if (me.getID() == MouseEvent.MOUSE_DRAGGED || me.isShiftDown())
                return false;
        }

        int clickLocation = getClickedItem(e);
        switch (clickLocation) {
        case CLICKED_NONE:
            return false;

        case CLICKED_EXPANDER:
            expandCollapseNode(clickedNode);
            return false;

        case CLICKED_ICON:
        case CLICKED_TEXT:
            return true;
        }

        return false;
    }

    private void expandCollapseNode(WBSNode node) {
        node.setExpanded(!node.isExpanded());
        wbsModel.recalcRows();
    }

    /**
     *
     * @param param1 <description>
     * @return <description>
     */
    public boolean shouldSelectCell(EventObject e)
    {
        System.out.println("shouldSelectCell called");
        if (e instanceof EditorComponent.RestartEditingEvent)
            return true;

        switch (getClickedItem(e)) {
        case CLICKED_NONE: return false;
        case CLICKED_EXPANDER: return false;
        }
        if (e instanceof MouseEvent && editingNode != null) {
            MouseEvent me = (MouseEvent)e;
            if (me.getID() == MouseEvent.MOUSE_DRAGGED) {
                if (menu.isPopupMenuVisible())
                    return false;

                System.out.println("I'm calling stopCellEditing");
                stopCellEditing();
            }
        }
        System.out.println("shouldSelectCell returning true");
        return true;
    }

    /** Tell the editor to stop editing and accept any partially edited
     * value as the value of the editor.
     *
     * The editor returns false if editing was not stopped, useful for
     * editors which validate and can not accept invalid entries.  */
    public boolean stopCellEditing() {
        System.out.println("stopCellEditing called");

        if (editingNode != null) {
            if (menu.isPopupMenuVisible())
                return false;

            String oldName = editingNode.getName();
            String newName = editorComponent.getText();
            if (newName == null || newName.trim().length() == 0) return false;
            newName = newName.trim();

            if (!newName.equals(oldName)) {
                editingNode.setName(newName);
                wbsModel.fireTableRowsUpdated(rowNumber,
                                              wbsModel.getRowCount()-1);
            }
            editingNode = null;
            rowNumber = -1;
        }

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
        System.out.println("cancelCellEditing called");
        stopCellEditing();
    }

    WBSNode getEditingNode() { return editingNode; }


    public EventObject getRestartEditingEvent() {
        return editorComponent.getRestartEditingEvent();
    }

    private static final int CLICKED_NONE = 0;
    private static final int CLICKED_WHITESPACE = 1;
    private static final int CLICKED_EXPANDER = 2;
    private static final int CLICKED_ICON = 3;
    private static final int CLICKED_TEXT = 4;

    private int getClickedItem(EventObject e) {
        if (!(e instanceof MouseEvent)) {
            clickedNode = null;
            return CLICKED_NONE;
        }

        MouseEvent me = (MouseEvent) e;
        Point p = me.getPoint();

        int columnNumber = table.columnAtPoint(p);
        int rowNumber = table.rowAtPoint(p);
        Rectangle r = table.getCellRect(rowNumber, columnNumber, true);

        int ourXPos = p.x - r.x;
        int outYPos = p.y - r.y;

        clickedNode = wbsModel.getNodeForRow(rowNumber);
        if (clickedNode == null) return CLICKED_NONE;

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
    private WBSNode clickedNode = null;


    private static final int HORIZ_SPACING =
        WBSNodeRenderer.ICON_HORIZ_SPACING;


    /** Component facilitating the editing of a WBS node.
     */
    private class EditorComponent extends Container {

        JMenuBar menuBar;
        //DropDownMenu menu;
        JTextField textField;
        JComponent iconListener;

        int indentationLevel = 0;
        boolean isExpanded, isLeaf;
        Icon nodeIcon;
        String iconError;
        Color backgroundColor = null;

        /**
         * Constructs an EditorComponent object.
         */
        public EditorComponent() {
            setLayout(null);
            setBackground(Color.white);

            textField = new JTextField(new WBSNodeNameDocument(), "", 9999);
            textField.setNextFocusableComponent(table);
            textField.setBorder(BorderFactory.createEmptyBorder());
            this.add(textField);

            menuBar = new JMenuBar();
            menuBar.add(menu);
            this.add(menuBar);

            iconListener = new IconListener();
            this.add(iconListener);

            if (table instanceof WBSJTable)
                ((WBSJTable) table).installActions(textField);
        }
        private Color getBackgroundColor() {
            if (backgroundColor == null) {
                Color selColor = table.getSelectionBackground();
                if (selColor != null)
                    backgroundColor = IconFactory.mixColors
                        (selColor, Color.white, 0.5f);
                textField.setBackground(backgroundColor);
            }
            return backgroundColor;
        }


        /**
         * Overrides <code>Container.paint</code> to paint the node's
         * icon and use the selection color for the background.
         */
        public void paint(Graphics g) {
            g.setColor(getBackgroundColor());
            g.fillRect(0, 0, getWidth(), getHeight());

            // call super.paint to paint the text field.
            super.paint(g);

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

        private void paintIcon(Graphics g, Icon i, Dimension size, int x) {
            int yLoc = (size.height - i.getIconHeight() + 1) / 2;
            if (yLoc < 0) yLoc = 0;
            i.paintIcon(this, g, x, yLoc);
        }

        public void updateInfo() {
            if (editingNode == null) return;

            indentationLevel = editingNode.getIndentLevel();
            isExpanded = editingNode.isExpanded();
            isLeaf = wbsModel.isLeaf(editingNode);
            nodeIcon = getIconForType(editingNode.getType());
            iconError = (String) editingNode.getAttribute
                (WBSModelValidator.NODE_TYPE_ERROR_ATTR_NAME);
            iconListener.setToolTipText(iconError);
            if (iconError != null)
                nodeIcon = IconFactory.getModifiedIcon
                    (nodeIcon, IconFactory.ERROR_ICON);
        }

        public void setText(String text) { textField.setText(text); }
        public String getText()          { return textField.getText(); }

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
            return new Dimension(1000, 16);
        }

        public void maybeRestoreEditorState(EventObject e) {
            if (e instanceof RestartEditingEvent)
                ((RestartEditingEvent) e).restoreState(this);
        }


        private class IconListener extends JComponent implements MouseListener
        {
            public IconListener() { addMouseListener(this); }
            public void paint(Graphics g) { }

            // implementation of java.awt.event.MouseListener interface
            boolean pressHidPopup = false;

            public void mouseClicked(MouseEvent e) {
                System.out.println("mouseClicked");
                if (!pressHidPopup) {
                    System.out.println("menu.doClick");
                    menu.doClick(0);
                }
            }
            public void mousePressed(MouseEvent e) {
                System.out.println("mousePressed");
                //menuBar.grabFocus();
                menu.dispatchEvent(e);
                System.out.println("dispatched mouse event");
                if (menu.isPopupMenuVisible())
                    pressHidPopup = false;
                else
                    pressHidPopup = true;
            }
            public void mouseReleased(MouseEvent param1) { }
            public void mouseEntered(MouseEvent param1) { }
            public void mouseExited(MouseEvent param1) { }
        }

        class RestartEditingEvent extends EventObject implements Runnable {
            private WBSNode savedStateFromNode;
            private int caretPos, selectionStart, selectionEnd, caretPixel;
            public RestartEditingEvent() {
                super(EditorComponent.this);
                savedStateFromNode = editingNode;
                if (savedStateFromNode == null) {
                    caretPos = selectionStart = selectionEnd = caretPixel = -1;
                } else {
                    caretPos = textField.getCaretPosition();
                    selectionStart = textField.getSelectionStart();
                    selectionEnd = textField.getSelectionEnd();
                    try {
                        Rectangle r = textField.modelToView(caretPos);
                        caretPixel = r.x + textField.getX();
                    } catch (Exception e) {
                        e.printStackTrace(); caretPixel = -1; }
                }
            }
            public void restoreState(EditorComponent c) {
                if (c != getSource()) return;

                if (savedStateFromNode != null) {
                    if (savedStateFromNode == editingNode) {
                        if (selectionStart == selectionEnd)
                            textField.setCaretPosition(caretPos);
                        else {
                            textField.setCaretPosition
                                (caretPos == selectionStart
                                 ? selectionEnd : selectionStart);
                            textField.moveCaretPosition(caretPos);
                        }
                    } else if (caretPixel != -1) {
                        //try for the goal column
                        doLayout();
                        int x = caretPixel - textField.getX();
                        Point p = new Point(x, textField.getHeight()/2);
                        int pos = textField.viewToModel(p);
                        textField.setCaretPosition(pos);
                    }
                }
                SwingUtilities.invokeLater(this);
            }
            public void run() { textField.grabFocus(); }
        }
        public EventObject getRestartEditingEvent() {
            return new RestartEditingEvent();
        }

        public void requestFocus() { textField.requestFocus(); }
    }

    private class NodeIconMenuAction extends AbstractAction {
        String type;
        public NodeIconMenuAction(String type) {
            super(type, getIconForType(type));
            this.type = type;
        }
        public void actionPerformed(ActionEvent e) {
            if (editingNode != null) {
                editingNode.setType(type);
                wbsModel.fireTableRowsUpdated(rowNumber,
                                              wbsModel.getRowCount()-1);
                editorComponent.updateInfo();
                editorComponent.invalidate();
                editorComponent.repaint();
                //wbsModel.fireTableRowsUpdated(rowNumber, rowNumber);
            } else {
                System.out.println("in NodeIconMenuAction.actionPerformed: editingNode = null!");
            }
        }
    }

    private Icon getIconForType(String type) {
        Icon result = (Icon) iconMap.get(type);
        if (result == null)
            result = (Icon) iconMap.get(null);
        return result;
    }

    private class DropDownMenu extends JMenu {
        public void dispatchMouseEvent(MouseEvent e) {
            processMouseEvent(e);
        }
    }

    private class WBSNodeNameDocument extends PlainDocument {

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

}
