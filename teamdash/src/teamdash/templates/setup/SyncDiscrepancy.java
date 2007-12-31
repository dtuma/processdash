package teamdash.templates.setup;

import java.util.Date;
import java.util.Map;

import net.sourceforge.processdash.hier.HierarchyNote;

public abstract class SyncDiscrepancy {

    public static final String DISCREPANCIES_DATANAME = "discrepancies_ //list";

    public interface Visitor {
        public void visit(PlanTime p);
        public void visit(EVSchedule s);
        public void visit(NodeType n);
        public void visit(ItemNote n);
    }

    public static class PlanTime extends SyncDiscrepancy {
        private String path;
        private String wbsId;
        private double plannedHours;

        public PlanTime(String path, String wbsId, double plannedHours) {
            this.path = path;
            this.wbsId = wbsId;
            this.plannedHours = plannedHours;
        }

        public String getPath() {
            return path;
        }

        public String getWbsId() {
            return wbsId;
        }

        public double getPlannedHours() {
            return plannedHours;
        }

        @Override
        public void visit(Visitor v) {
            v.visit(this);
        }

    }

    public static class NodeType extends SyncDiscrepancy {
        private String path;
        private String wbsId;
        private String type;

        public NodeType(String path, String wbsId, String type) {
            this.path = path;
            this.wbsId = wbsId;
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public String getWbsId() {
            return wbsId;
        }

        public String getType() {
            return type;
        }

        @Override
        public void visit(Visitor v) {
            v.visit(this);
        }

    }

    public static class ItemNote extends SyncDiscrepancy {
        private String path;
        private String wbsId;
        private String text;
        private String format;
        private String author;
        private Date timestamp;
        private Date baseTimestamp;

        public ItemNote(String path, String wbsId, HierarchyNote originalNote,
                HierarchyNote modifiedNote) {
            this.path = path;
            this.wbsId = wbsId;
            if (modifiedNote != null) {
                this.text = modifiedNote.getContent();
                this.format = modifiedNote.getFormat();
                this.author = modifiedNote.getAuthor();
                this.timestamp = modifiedNote.getTimestamp();
            }
            if (originalNote != null) {
                this.baseTimestamp = originalNote.getTimestamp();
            }
        }

        public String getPath() {
            return path;
        }

        public String getWbsId() {
            return wbsId;
        }

        public String getText() {
            return text;
        }

        public String getFormat() {
            return format;
        }

        public String getAuthor() {
            return author;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public Date getBaseTimestamp() {
            return baseTimestamp;
        }

        @Override
        public void visit(Visitor v) {
            v.visit(this);
        }

    }

    public static class EVSchedule extends SyncDiscrepancy {
        private Map<Date,Double> exceptions;

        public EVSchedule(Map<Date,Double> exceptions) {
            this.exceptions = exceptions;
        }

        public Map<Date,Double> getExceptions() {
            return exceptions;
        }

        @Override
        public void visit(Visitor v) {
            v.visit(this);
        }

    }

    public abstract void visit(Visitor v);
}
