package teamdash.wbs.columns;

import teamdash.wbs.DataTableModel;
import teamdash.wbs.WBSModel;

public class SizeMeasureColumn //implements DataColumn , TableModelListener
{
    protected DataTableModel dataModel;
    protected WBSModel wbsModel;

    public SizeMeasureColumn(DataTableModel dataModel, String name) {
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        //recalc();
        //wbsModel.addTableModelListener(this);
    }

}
