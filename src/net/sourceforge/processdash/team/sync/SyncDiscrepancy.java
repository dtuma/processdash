// Copyright (C) 2002-2020 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package net.sourceforge.processdash.team.sync;

import java.util.Date;
import java.util.Map;

import net.sourceforge.processdash.hier.HierarchyNote;

public abstract class SyncDiscrepancy {

    public static final String DISCREPANCIES_DATANAME = "discrepancies_ //list";

    public interface Visitor {
        public void visit(PlanTime p);
        public void visit(SizeData s);
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

    public static class SizeData extends SyncDiscrepancy {
        private String path;
        private String wbsId;
        private String metric;
        private String metricID;
        private boolean plan;
        private double value;
        private long timestamp;

        public SizeData(String path, String wbsId, String metric,
                String metricID, boolean plan, double value, long timestamp) {
            this.path = path;
            this.wbsId = wbsId;
            this.metric = metric;
            this.metricID = metricID;
            this.plan = plan;
            this.value = value;
            this.timestamp = timestamp;
        }

        public String getPath() {
            return path;
        }

        public String getWbsId() {
            return wbsId;
        }

        public String getMetric() {
            return metric;
        }

        public final String getMetricID() {
            return metricID;
        }

        public boolean isPlan() {
            return plan;
        }

        public double getValue() {
            return value;
        }

        public long getTimestamp() {
            return timestamp;
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
