
package teamdash.wbs;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;


public class WorkflowSelectionModel extends DefaultListSelectionModel implements TableModelListener, PropertyChangeListener {

    WBSJTable sourceTable;
    WorkflowWBSModel sourceModel;



    PropertyEditor selectedWorkflowNamesManager;
    Set selectedWorkflowNames;
    private boolean isMakingChange = false;

    public WorkflowSelectionModel(WBSJTable sourceTable, PropertyEditor e) {
        this.sourceTable = sourceTable;
        this.selectedWorkflowNamesManager = e;
        this.selectedWorkflowNamesManager.addPropertyChangeListener(this);

        WorkflowModel sourceTableModel = (WorkflowModel) sourceTable.getModel();
        sourceModel = (WorkflowWBSModel) sourceTableModel.getWBSModel();
        sourceModel.addTableModelListener(this);
    }


    public void tableChanged(TableModelEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                refreshSelectionFromWorkflows();
            }});
    }


    public void propertyChange(PropertyChangeEvent evt) {
        refreshSelectionFromWorkflows();
    }


    private void refreshWorkflowsFromSelection() {
        if (!isMakingChange) {
            int[] rows = sourceTable.getSelectedRows();
            Set newSelectedWorkflowNames = sourceModel.getWorkflowsForRows(rows);

            if (!newSelectedWorkflowNames.equals(selectedWorkflowNames))
                selectedWorkflowNamesManager.setValue(newSelectedWorkflowNames);

            refreshSelectionFromWorkflows();
        }
    }


    private void refreshSelectionFromWorkflows() {
        isMakingChange = true;
        selectedWorkflowNames = (Set) selectedWorkflowNamesManager.getValue();
        List workflowNodes = sourceModel.getNodesForWorkflows(selectedWorkflowNames);
        int[] rows = sourceModel.getRowsForNodes(workflowNodes);
        Arrays.sort(rows);

        int anchor = super.getAnchorSelectionIndex();
        int lead = super.getLeadSelectionIndex();
        if (Arrays.binarySearch(rows, anchor) < 0)
            anchor = -1;
        if (Arrays.binarySearch(rows, lead) < 0)
            lead = -1;

        super.clearSelection();
        for (int i = 0; i < rows.length; i++)
            super.addSelectionInterval(rows[i], rows[i]);

        if (anchor != -1) {
            if (lead != -1)
                super.addSelectionInterval(anchor, lead);
            else
                super.addSelectionInterval(anchor, anchor);
        }
        isMakingChange = false;
    }


    public void addSelectionInterval(int index0, int index1) {
        super.addSelectionInterval(index0, index1);
        refreshWorkflowsFromSelection();
    }
    public void clearSelection() {
        super.clearSelection();
        refreshWorkflowsFromSelection();
    }
    public void removeSelectionInterval(int index0, int index1) {
        super.removeSelectionInterval(index0, index1);
        refreshWorkflowsFromSelection();
    }
    public void setAnchorSelectionIndex(int anchorIndex) {
        super.setAnchorSelectionIndex(anchorIndex);
        refreshWorkflowsFromSelection();
    }
    public void setLeadSelectionIndex(int leadIndex) {
        super.setLeadSelectionIndex(leadIndex);
        refreshWorkflowsFromSelection();
    }
    public void setSelectionInterval(int index0, int index1) {
        super.setSelectionInterval(index0, index1);
        refreshWorkflowsFromSelection();
    }


}
