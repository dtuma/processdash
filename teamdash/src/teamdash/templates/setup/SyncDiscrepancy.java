package teamdash.templates.setup;

import java.util.Date;
import java.util.Map;

public abstract class SyncDiscrepancy {

    public static final String DISCREPANCIES_DATANAME = "discrepancies_ //list";

    public interface Visitor {
        public void visit(PlanTime p);
        public void visit(EVSchedule s);
        public void visit(NodeType n);
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
