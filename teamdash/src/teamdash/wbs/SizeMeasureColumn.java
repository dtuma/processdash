package teamdash.wbs;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class SizeMeasureColumn implements DataColumn, TableModelListener
{
    protected DataTableModel dataModel;
    protected WBSModel wbsModel;

    public SizeMeasureColumn(DataTableModel dataModel, String name) {
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        recalc();
        wbsModel.addTableModelListener(this);
    }

}
