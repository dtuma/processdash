// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package pspdash;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.jrefinery.chart.*;

import pspdash.data.DataRepository;
import pspdash.data.ResultSet;



public abstract class CGIChartBase extends pspdash.TinyCGIBase {

    protected ResultSet data = null;
    protected boolean chromeless = false;

    private static Color INVISIBLE = new Color(1f, 1f, 1f, 0f);

    /** Write the CGI header. */
    protected void writeHeader() {
        out.print("Content-type: image/jpeg\r\n\r\n");
        out.flush();
    }

    /** Write the CGI header. */
    protected void writeHtmlHeader() {
        super.writeHeader();
    }

    /** create the data upon which this chart is based. */
    protected void buildData() {
        massageParameters();
        String prefix = getPrefix();
        data = ResultSet.get(getDataRepository(), parameters, prefix,
                             getPSPProperties());
        if (parameters.get("transpose") != null)
            data = data.transpose();
    }

    /** Generate CGI chart output. */
    protected void writeContents() throws IOException {
        buildData();            // get the data for display

        chromeless = (parameters.get("chromeless") != null);
        JFreeChart chart = createChart();

        int width = getIntSetting("width");
        int height = getIntSetting("height");
        Color initGradColor  = getColorSetting("initGradColor");
        Color finalGradColor = getColorSetting("finalGradColor");
        chart.setChartBackgroundPaint(new GradientPaint
            (0, 0, initGradColor, width, height, finalGradColor));
        if (parameters.get("hideOutline") != null)
            chart.getPlot().setOutlinePaint(INVISIBLE);

        String title = getSetting("title");
        if (chromeless || title == null || title.length() == 0)
            chart.setTitle((Title) null);
        else {
            chart.setTitle(Translator.translate(title));
            String titleFontSize = getSetting("titleFontSize");
            if (titleFontSize != null) try {
                float fontSize = Float.parseFloat(titleFontSize);
                StandardTitle t = (StandardTitle) chart.getTitle();
                t.setTitleFont(t.getTitleFont().deriveFont(fontSize));
            } catch (Exception tfe) {}
        }

        if (chromeless || parameters.get("hideLegend") != null)
            chart.setLegend(null);
        else {
            StandardLegend l = (StandardLegend) chart.getLegend();
            String legendFontSize = getSetting("legendFontSize");
            if (l != null && legendFontSize != null) try {
                float fontSize = Float.parseFloat(legendFontSize);
                l.setSeriesFont(l.getSeriesFont().deriveFont(fontSize));
            } catch (Exception lfe) {}
        }


        Axis xAxis = chart.getPlot().getAxis(Plot.HORIZONTAL_AXIS);
        if (parameters.get("hideTickLabels") != null||
            parameters.get("hideXTickLabels") != null) {
            xAxis.setShowTickLabels(false);
        } else if (parameters.get("tickLabelFontSize") != null ||
                   parameters.get("xTickLabelFontSize") != null) {
            String tfs = getParameter("xTickLabelFontSize");
            if (tfs == null) tfs = getParameter("tickLabelFontSize");
            float fontSize = Float.parseFloat(tfs);
            xAxis.setTickLabelFont
                (xAxis.getTickLabelFont().deriveFont(fontSize));
        }

        Axis yAxis = chart.getPlot().getAxis(Plot.VERTICAL_AXIS);
        if (parameters.get("hideTickLabels") != null||
            parameters.get("hideYTickLabels") != null) {
            yAxis.setShowTickLabels(false);
        } else if (parameters.get("tickLabelFontSize") != null ||
                   parameters.get("yTickLabelFontSize") != null) {
            String tfs = getParameter("yTickLabelFontSize");
            if (tfs == null) tfs = getParameter("tickLabelFontSize");
            float fontSize = Float.parseFloat(tfs);
            yAxis.setTickLabelFont
                (yAxis.getTickLabelFont().deriveFont(fontSize));
        }

        String axisFontSize = getSetting("axisLabelFontSize");
        if (axisFontSize != null) try {
            float fontSize = Float.parseFloat(axisFontSize);
            Axis a = chart.getPlot().getAxis(Plot.HORIZONTAL_AXIS);
            a.setLabelFont(a.getLabelFont().deriveFont(fontSize));
            a = chart.getPlot().getAxis(Plot.VERTICAL_AXIS);
            a.setLabelFont(a.getLabelFont().deriveFont(fontSize));
        } catch (Exception afs) {}


        BufferedImage img = new BufferedImage
            (width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        chart.draw(g2, new Rectangle2D.Double(0, 0, width, height));
        g2.dispose();

        int quality = (parameters.containsKey("EXPORT") ? 85 : 100);
        JpegEncoder jpegEncoder = new JpegEncoder(img, quality, outStream);
        jpegEncoder.Compress();
        outStream.flush();
        outStream.close();
    }
    public static final String SETTING_PREFIX = "chart.";

    protected String getSetting(String name) {
        String result = getParameter(name);
        if (result == null) result = Settings.getVal(SETTING_PREFIX + name);
        return result;
    }
    protected int getIntSetting(String name) {
        try {
            return Integer.parseInt(getParameter(name));
        } catch (Exception e) {}
        try {
            return Integer.parseInt(Settings.getVal(SETTING_PREFIX + name));
        } catch (Exception e) {}
        return -1;
    }
    protected Color getColorSetting(String name) {
        String val = getParameter(name);
        if (val != null) try {
            if (val.startsWith("_"))
                val = "#" + val.substring(1);
            return Color.decode(val);
        } catch (Exception e) {}
        return Color.decode(Settings.getVal(SETTING_PREFIX +name));
    }

    protected void setupCategoryChart(JFreeChart chart) {

        Axis horizAxis = chart.getPlot().getAxis(Plot.HORIZONTAL_AXIS);
        Axis vertAxis  = chart.getPlot().getAxis(Plot.VERTICAL_AXIS);

        if (!chromeless) {
            String catAxisLabel = data.getColName(0);
            if (catAxisLabel == null)
                catAxisLabel = Translator.translate("Project/Task");

            String otherAxisLabel = Translator.translate(getSetting("units"));
            if ((otherAxisLabel == null || otherAxisLabel.length() == 0)
                && data.numCols() == 1)
                otherAxisLabel = data.getColName(1);
            if (otherAxisLabel == null)
                otherAxisLabel = Translator.translate("Value");

            String catLabels = getParameter("categoryLabels");

            if (horizAxis instanceof HorizontalCategoryAxis) {
                horizAxis.setLabel(catAxisLabel);
                vertAxis.setLabel(otherAxisLabel);
                if ("vertical".equalsIgnoreCase(catLabels))
                    ((HorizontalCategoryAxis) horizAxis)
                        .setVerticalCategoryLabels(true);
                else if ("none".equalsIgnoreCase(catLabels))
                    horizAxis.setShowTickLabels(false);
            } else {
                horizAxis.setLabel(otherAxisLabel);
                vertAxis.setLabel(catAxisLabel);
                if ("none".equalsIgnoreCase(catLabels))
                    vertAxis.setShowTickLabels(false);
            }
        }

        if (data.numCols() == 1) {
            chart.setLegend(null);
            chart.getPlot().setInsets(new Insets(5,2,2,5));
        }
    }

    /** Optionally massage the parameters before the ResultSet is generated */
    public void massageParameters() {}

    /** Create some specific type of chart */
    public abstract JFreeChart createChart();
}
