package teamdash.process;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.EventHandler;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ItemListEditor extends JPanel {

    private ItemListTableModel model;
    private JTable table;
    JButton insertButton, deleteButton, upButton, downButton;


    public ItemListEditor(ItemListTableModel model, String title) {
        super(new BorderLayout());
        this.model = model;
        this.table = model.createJTable();

        // listen to changes in the row selection, and enable/disable
        // buttons accordingly
        table.getSelectionModel().addListSelectionListener
            (new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        enableButtons(); }});

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createTitledBorder
                     (BorderFactory.createLoweredBevelBorder(), title));
        add(sp, BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
    }

    private Component buildButtons() {
        Box buttons = Box.createHorizontalBox();
        buttons.add(Box.createHorizontalGlue());
        buttons.add(insertButton = new JButton("Insert"));
        insertButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "insert"));

        buttons.add(Box.createHorizontalGlue());
        buttons.add(deleteButton = new JButton("Delete"));
        deleteButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "delete"));

        buttons.add(Box.createHorizontalGlue());
        buttons.add(upButton = new JButton("Move Up"));
        upButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "moveUp"));

        buttons.add(Box.createHorizontalGlue());
        buttons.add(downButton = new JButton("Move Down"));
        downButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "moveDown"));
        buttons.add(Box.createHorizontalGlue());

        enableButtons();

        Box result = Box.createVerticalBox();
        result.add(Box.createVerticalStrut(2));
        result.add(buttons);
        result.add(Box.createVerticalStrut(2));
        return result;
    }

    private int getSelectedRow() {
        return table.getSelectionModel().getMinSelectionIndex();
    }
    private void selectRow(int row) {
        table.getSelectionModel().setSelectionInterval(row, row);
    }

    private void enableButtons() {
        int row = getSelectedRow();
        boolean editable = ((row >= 0) && (model.isCellEditable(row, 0)));
        deleteButton.setEnabled(editable && row >= 0);
        upButton    .setEnabled(editable && row > 0);
        downButton  .setEnabled(editable &&
                              row >= 0 && row+1 < model.getRealRowCount());
    }

    public void insert() {
        int row = getSelectedRow();
        if (row < 0) row = 0;
        model.insertItem(row);
        selectRow(row);
    }
    public void delete() {
        int row = getSelectedRow();
        if (row >= 0) model.deleteItem(row);
    }
    public void moveUp()   {
        int row = getSelectedRow();
        model.moveItemUp(row--);
        selectRow(row);
    }
    public void moveDown() {
        int row = getSelectedRow()+1;
        model.moveItemUp(row);
        selectRow(row);
    }
}
