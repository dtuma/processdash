// Copyright (C) 2009 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.ui.web.psp;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.ui.lib.chart.DrawingSupplierFactory;
import net.sourceforge.processdash.ui.lib.chart.PhaseChartColorer;
import net.sourceforge.processdash.ui.web.CGIChartBase;
import net.sourceforge.processdash.util.ComparableValue;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.SegmentedTimeline;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;

public class TimeLogPhaseWaterfallChart extends CGIChartBase {

    private static final Resources resources = Resources
            .getDashBundle("PspForEng.TimeLogPhaseChart");



    @Override
    protected void writeContents() throws IOException {
        if (isHtmlMode()) {
            out.write("<p>");
            super.writeContents();
            out.write("</p>\n");
        } else {
            super.writeContents();
        }
    }

    @Override
    protected void buildData() {
        // this method in the superclass builds a ResultSet object.  We
        // do not use ResultSets as the basis for our dataset, so we override
        // this method to do nothing.
    }

    @Override
    public JFreeChart createChart() {
        ProcessUtil process = new ProcessUtil(getDataContext());
        List<String> phases = getPhaseList(process);

        List timeLogEntries = getTimeLogEntries();
        GapSkipTracker gaps = createGapTracker();
        IntervalXYDataset dataset = createDataset(process, phases,
            timeLogEntries, gaps);

        return createChart(dataset, process, phases, gaps);
    }


    private List<String> getPhaseList(ProcessUtil process) {
        List phases = process.getProcessListPlain("Phase_List");
        phases = process.filterPhaseList(phases);
        Collections.reverse(phases);
        return phases;
    }


    private List getTimeLogEntries() {
        String path = getPrefix();
        TimeLog tl = getDashboardContext().getTimeLog();
        List result;
        try {
            result = Collections.list(tl.filter(path, null, null));
            Collections.sort(result);
        } catch (IOException e) {
            e.printStackTrace();
            result = Collections.EMPTY_LIST;
        }
        return result;
    }


    private GapSkipTracker createGapTracker() {
        int gapWidth;
        try {
            double gapSize = Double.parseDouble(getParameter("GapSize"));
            String units = getParameter("GapUnits");
            if (units != null && units.startsWith("M"))
                gapWidth = (int) (gapSize * MINUTE_MILLIS);
            else
                gapWidth = (int) (gapSize * HOUR_MILLIS);
        } catch (Exception e) {
            gapWidth = HOUR_MILLIS;
        }
        return new GapSkipTracker(gapWidth);
    }
    private static final int MINUTE_MILLIS = 60 * 1000;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;


    private IntervalXYDataset createDataset(ProcessUtil process,
            List<String> phases, List timeLogEntries, GapSkipTracker gaps) {
        XYIntervalSeriesCollection result = new XYIntervalSeriesCollection();

        Map<String, PhaseSeries> series = makeSeries(phases);
        ExceptionSeries exceptions = new ExceptionSeries();

        addTimeLogEntries(process, series, timeLogEntries, gaps, exceptions);

        for (PhaseSeries ps : series.values())
            result.addSeries(ps);
        result.addSeries(exceptions);

        return result;
    }


    private Map<String, PhaseSeries> makeSeries(List<String> phases) {
        Map<String, PhaseSeries> result = new LinkedHashMap();
        for (int i = 0; i < phases.size(); i++) {
            String phase = phases.get(i);
            PhaseSeries series = new PhaseSeries(phase, i);
            result.put(phase, series);
        }
        return result;
    }


    private void addTimeLogEntries(ProcessUtil process,
            Map<String, PhaseSeries> phases, List timeLogEntries,
            GapSkipTracker gaps, ExceptionSeries exceptions) {
        for (Iterator i = timeLogEntries.iterator(); i.hasNext();) {
            TimeLogEntry tle = (TimeLogEntry) i.next();
            addTimeLogEntry(process, phases, tle, gaps, exceptions);
        }
    }


    private void addTimeLogEntry(ProcessUtil process,
            Map<String, PhaseSeries> phases, TimeLogEntry tle,
            GapSkipTracker gaps, ExceptionSeries exceptions) {
        String entryPath = tle.getPath();
        String phase = process.getEffectivePhase(entryPath, true);
        PhaseSeries series = phases.get(phase);
        if (series != null) {
            long start = tle.getStartTime().getTime();
            long deltaMins = tle.getElapsedTime() + tle.getInterruptTime();
            long end = start + deltaMins * MINUTE_MILLIS;
            series.add(start, end);
            gaps.addTimeLogEntry(start, end);
            exceptions.maybeAddException(start, end, phase);
        }
    }



    private JFreeChart createChart(IntervalXYDataset dataset,
            ProcessUtil process, List<String> phases, GapSkipTracker gaps) {
        JFreeChart result = ChartFactory.createXYBarChart(null, null, true,
            null, dataset, PlotOrientation.VERTICAL, false, true, false);

        XYPlot xyplot = (XYPlot) result.getPlot();
        DateAxis dateAxis = new DateAxis(null);
        dateAxis.setTickMarksVisible(false);
        dateAxis.setTickLabelsVisible(false);
        dateAxis.setLowerMargin(0.01);
        dateAxis.setUpperMargin(0.01);
        setupGaps(gaps, dateAxis, xyplot);
        xyplot.setDomainAxis(dateAxis);

        String[] phaseNameList = new String[phases.size()];
        for (int i = 0;  i < phaseNameList.length;  i++)
            phaseNameList[i] = Translator.translate(phases.get(i));
        SymbolAxis symbolaxis = new SymbolAxis(null, phaseNameList);
        symbolaxis.setGridBandsVisible(false);
        xyplot.setRangeAxis(symbolaxis);

        final XYBarRenderer renderer = (XYBarRenderer) xyplot.getRenderer();
        renderer.setUseYInterval(true);
        renderer.setDrawBarOutline(true);
        renderer.setBaseOutlinePaint(Color.black);

        new PhaseChartColorer(process, phases) {
            public void setItemColor(Object key, int pos, Color c) {
                renderer.setSeriesPaint(pos, c);
            }
        }.run();

        int exceptionSeries = dataset.getSeriesCount()-1;
        renderer.setSeriesPaint(exceptionSeries, EXCEPTION_PAINT);
        renderer.setSeriesFillPaint(exceptionSeries, EXCEPTION_PAINT);
        renderer.setSeriesOutlinePaint(exceptionSeries, EXCEPTION_PAINT);

        renderer.setBaseToolTipGenerator(new TooltipGenerator());

        xyplot.setBackgroundPaint(Color.white);
        xyplot.setDomainGridlinesVisible(false);
        xyplot.setRangeGridlinePaint(Color.lightGray);
        xyplot.setDrawingSupplier(DRAWING_SUPPLIER_FACTORY
                .newDrawingSupplier());

        result.setAntiAlias(false);
        result.setTextAntiAlias(true);

        parameters.put("title", resources.getString("Title"));

        return result;
    }

    private void setupGaps(GapSkipTracker gapTracker, DateAxis dateAxis,
            XYPlot plot) {
        if (gapTracker.gaps.isEmpty())
            return;

        SegmentedTimeline timeline = new SegmentedTimeline(1000, 100, 0);
        timeline.setStartTime(gapTracker.leftEnd);

        for (Span gap : gapTracker.gaps) {
            timeline.addException(gap.start + GAP_SPACING, gap.end-1000);

            long annotationX = gap.start + GAP_SPACING / 2;
            plot.addAnnotation(new XYLineAnnotation(annotationX,
                    VERT_LINE_MIN_Y, annotationX, VERT_LINE_MAX_Y, GAP_STROKE,
                    Color.darkGray));

            double boxW = GAP_SPACING * 0.4;
            XYBoxAnnotation box = new XYBoxAnnotation(annotationX - boxW,
                    VERT_LINE_MIN_Y, annotationX + boxW, VERT_LINE_MAX_Y, null,
                    null, null);
            String toolTip = resources.format("No_Activity_FMT", gap.getStart(),
                gap.getEnd());
            box.setToolTipText(toolTip);
            plot.addAnnotation(box);
        }

        dateAxis.setTimeline(timeline);
    }
    private static final long GAP_SPACING = 2 * MINUTE_MILLIS;


    private class PhaseSeries extends XYIntervalSeries {

        private int index;

        public PhaseSeries(String phaseName, int index) {
            super(new ComparableValue(phaseName, index));
            this.index = index;
        }

        public void add(long start, long end) {
            add(start, start, end, index, (double) index - 0.4D,
                (double) index + 0.4D);
        }
    }

    private class ExceptionSeries extends XYIntervalSeries {

        private List<Span> activeEntries;

        public ExceptionSeries() {
            super(new ComparableValue("Problems", EXCEPTION_SERIES_ORDINAL));
            this.activeEntries = new LinkedList<Span>();
        }

        public void maybeAddException(long start, long end, String phaseName) {
            for (Iterator i = activeEntries.iterator(); i.hasNext();) {
                Span e = (Span) i.next();
                if (e.end <= start + ALLOWED_OVERLAP) {
                    i.remove();
                } else {
                    long exceptionEnd = Math.min(end, e.end);
                    super.add(start, start, exceptionEnd, 0, VERT_LINE_MIN_Y,
                        VERT_LINE_MAX_Y);
                }
            }
            activeEntries.add(new Span(start, end));
        }

    }
    private static final int EXCEPTION_SERIES_ORDINAL = -1;
    private static final long ALLOWED_OVERLAP = MINUTE_MILLIS;


    private class GapSkipTracker {

        private int collapseWidth;

        private long leftEnd, rightEnd;

        private List<Span> gaps;

        public GapSkipTracker(int collapseWidth) {
            if (collapseWidth < GAP_SPACING)
                this.collapseWidth = Integer.MAX_VALUE;
            else
                this.collapseWidth = collapseWidth;
            this.leftEnd = this.rightEnd = 0;
            this.gaps = new ArrayList<Span>();
        }

        public void addTimeLogEntry(long start, long end) {
            if (leftEnd == 0) {
                leftEnd = start;
                rightEnd = end;
                return;
            }

            long thisGap = start - rightEnd;
            if (thisGap > collapseWidth) {
                gaps.add(new Span(rightEnd, start));
            }

            rightEnd = Math.max(rightEnd, end);
        }

    }

    private class Span {
        long start, end;

        public Span(long start, long end) {
            this.start = start;
            this.end = end;
        }
        public Date getStart() { return new Date(start); }
        public Date getEnd() { return new Date(end); }
    }

    private class TooltipGenerator implements XYToolTipGenerator {
        public String generateToolTip(XYDataset xy, int series, int item) {
            ComparableValue key = (ComparableValue) xy.getSeriesKey(series);
            if (key.getOrdinal() == EXCEPTION_SERIES_ORDINAL)
                return resources.getString("Overlap_Message");

            IntervalXYDataset dataset = (IntervalXYDataset) xy;
            String phaseName = Translator.translate(key.toString());
            long start = dataset.getStartX(series, item).longValue();
            long end = dataset.getEndX(series, item).longValue();
            return resources.format("Time_Log_Entry_FMT", phaseName,
                new Date(start), new Date(end));
        }
    }


    private static final DrawingSupplierFactory DRAWING_SUPPLIER_FACTORY =
        new DrawingSupplierFactory()
            .setPaintSequence(DrawingSupplierFactory.PALETTE1)
            .setFillPaintSequence(DrawingSupplierFactory.PALETTE1);

    private static final Color EXCEPTION_PAINT = new Color(1f, 0.5f, 0.5f);

    private static final Stroke GAP_STROKE = new BasicStroke(1,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f,
            new float[] { 10f, 5f }, 0f);

    private static final double VERT_LINE_MIN_Y = -1;
    private static final double VERT_LINE_MAX_Y = 99;

}
