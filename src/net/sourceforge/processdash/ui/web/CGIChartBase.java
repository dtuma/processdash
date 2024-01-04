// Copyright (C) 2001-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.imagemap.ImageMapUtilities;
import org.jfree.chart.imagemap.StandardToolTipTagFragmentGenerator;
import org.jfree.chart.imagemap.StandardURLTagFragmentGenerator;
import org.jfree.chart.imagemap.ToolTipTagFragmentGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleInsets;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;




public abstract class CGIChartBase extends net.sourceforge.processdash.ui.web.TinyCGIBase {

    protected ResultSet data = null;
    protected boolean chromeless = false;

    private static Color INVISIBLE = new Color(1f, 1f, 1f, 0f);

    private static final String HTML_PARAM = "html";

    private static final Resources resources = Resources
            .getDashBundle("Analysis");

    protected boolean isHtmlMode() {
        return parameters.containsKey(HTML_PARAM);
    }

    /** Write the CGI header. */
    @Override
    protected void writeHeader() {
        if (isHtmlMode())
            writeHtmlHeader();
        else
            writeImageHeader();
    }

    protected void writeHtmlHeader() {
        super.writeHeader();
    }

    protected void writeImageHeader() {
        out.print("Content-type: image/png\r\n\r\n");
        out.flush();
    }

    /** create the data upon which this chart is based. */
    protected void buildData() throws IOException {
        retrieveParamsFromServlet("dqf");
        massageParameters();
        String prefix = getPrefix();
        data = ResultSet.get(getDataRepository(), parameters, prefix,
                             getPSPProperties());

        if (parameters.get("transpose") != null)
            data = data.transpose();

        if (parameters.containsKey("chartCols")) {
            String[] param = getParameter("chartCols").split(",");
            int[] cols = new int[param.length];
            for (int i = cols.length; i-- > 0;)
                cols[i] = Integer.parseInt(param[i]);
            data = data.pluckColumns(cols);
        }
    }

    /** Generate CGI chart output. */
    @Override
    protected void writeContents() throws IOException {
        buildData();            // get the data for display

        chromeless = (parameters.get("chromeless") != null);
        JFreeChart chart = createChart();

        int width = getIntSetting("width");
        int height = getIntSetting("height");
        Color initGradColor  = getColorSetting("initGradColor");
        Color finalGradColor = getColorSetting("finalGradColor");
        chart.setBackgroundPaint(new GradientPaint
            (0, 0, initGradColor, width, height, finalGradColor));
        if (parameters.get("hideOutline") != null)
            chart.getPlot().setOutlinePaint(INVISIBLE);

        String title = getSetting("title");
        if (chromeless || title == null || title.length() == 0)
            chart.setTitle((TextTitle) null);
        else {
            chart.setTitle(Translator.translate(title));
            String titleFontSize = getSetting("titleFontSize");
            if (titleFontSize != null) try {
                float fontSize = Float.parseFloat(titleFontSize);
                TextTitle t = chart.getTitle();
                t.setFont(t.getFont().deriveFont(fontSize));
            } catch (Exception tfe) {}
        }

        if (chromeless || parameters.get("hideLegend") != null)
            chart.removeLegend();
        else {
            LegendTitle l = chart.getLegend();
            String legendFontSize = getSetting("legendFontSize");
            if (l != null && legendFontSize != null) try {
                float fontSize = Float.parseFloat(legendFontSize);
                l.setItemFont(l.getItemFont().deriveFont(fontSize));
            } catch (Exception lfe) {}
        }

        if (chart.getPlot().getNoDataMessage() == null)
            chart.getPlot().setNoDataMessage(resources.getString("No_Data_Message"));

        Axis xAxis = getHorizontalAxis(chart);
        if (xAxis != null) {
            if (parameters.get("hideTickLabels") != null||
                parameters.get("hideXTickLabels") != null) {
                xAxis.setTickLabelsVisible(false);
            } else if (parameters.get("tickLabelFontSize") != null ||
                       parameters.get("xTickLabelFontSize") != null) {
                String tfs = getParameter("xTickLabelFontSize");
                if (tfs == null) tfs = getParameter("tickLabelFontSize");
                float fontSize = Float.parseFloat(tfs);
                xAxis.setTickLabelFont
                    (xAxis.getTickLabelFont().deriveFont(fontSize));
            }
        }

        Axis yAxis = getVerticalAxis(chart);
        if (yAxis != null) {
            if (parameters.get("hideTickLabels") != null||
                parameters.get("hideYTickLabels") != null) {
                yAxis.setTickLabelsVisible(false);
            } else if (parameters.get("tickLabelFontSize") != null ||
                       parameters.get("yTickLabelFontSize") != null) {
                String tfs = getParameter("yTickLabelFontSize");
                if (tfs == null) tfs = getParameter("tickLabelFontSize");
                float fontSize = Float.parseFloat(tfs);
                yAxis.setTickLabelFont
                    (yAxis.getTickLabelFont().deriveFont(fontSize));
            }
        }

        String axisFontSize = getSetting("axisLabelFontSize");
        if (axisFontSize != null) try {
            float fontSize = Float.parseFloat(axisFontSize);
            if (xAxis != null)
                xAxis.setLabelFont(xAxis.getLabelFont().deriveFont(fontSize));
            if (yAxis != null)
                yAxis.setLabelFont(yAxis.getLabelFont().deriveFont(fontSize));
        } catch (Exception afs) {}

        ChartRenderingInfo info =
                (isHtmlMode() ? new ChartRenderingInfo() : null);
        BufferedImage img = new BufferedImage
            (width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        if ("auto".equals(getSetting("titleFontSize")))
            maybeAdjustTitleFontSize(chart, g2, width);
        chart.draw(g2, new Rectangle2D.Double(0, 0, width, height), info);
        g2.dispose();

        String outputFormat = getSetting("outputFormat");
        OutputStream imgOut;
        if (isHtmlMode()) {
            imgOut = PngCache.getOutputStream();
        } else {
            imgOut = outStream;
        }
        ImageIO.write(img, outputFormat, imgOut);
        imgOut.flush();
        imgOut.close();
        if (isHtmlMode())
            writeImageHtml(width, height, imgOut.hashCode(), info);
    }

    private void writeImageHtml(int width, int height, int imgID,
            ChartRenderingInfo info) throws IOException {
        String tooltip = getParameter("tooltip");
        if (!StringUtils.hasValue(tooltip))
            tooltip = resources.getHTML("More_Detail_Here_Instruction");

        String href = getParameter("href");
        if (StringUtils.hasValue(href)) {
            // create a copy of the entity collection, and place an entity for
            // the entire chart at the beginning of the list.  This will
            // make it appear last in the image map (which is important,
            // because browsers process the image map areas in the order that
            // they appear; having the entire chart area listed first would
            // obscure all of the other image map areas).
            EntityCollection entities = new StandardEntityCollection();
            entities.add(new ChartEntity(info.getChartArea(), tooltip, href));
            if (info.getEntityCollection() != null)
                entities.addAll(info.getEntityCollection());

            // Next: most of our chart entities will not have URLs. URL values
            // don't inherit in the imagemap, so if we want the entire image
            // to have a single URL, we need to assign that URL to every
            // area in the chart.
            for (Iterator i = entities.iterator(); i.hasNext();) {
                ChartEntity ce = (ChartEntity) i.next();
                // check for no-op chart entity - these won't appear in the
                // image map anyway, so they don't need to be adjusted
                if (ce.getToolTipText() == null && ce.getURLText() == null)
                    continue;
                // for other entities, add a tooltip and a URL as needed.
                if (!StringUtils.hasValue(ce.getToolTipText()))
                    ce.setToolTipText(tooltip);
                if (!StringUtils.hasValue(ce.getURLText()))
                    ce.setURLText(href);
            }

            // install our modified version of the entity collection into
            // the chart info object, so it will be used when generating
            // the image map later.
            info.setEntityCollection(entities);
        }

        // write the image tag
        out.write("<img width=\"" + width + "\" height=\"" + height +
            "\" src=\"/reports/pngCache?id=" + imgID +
            "\" usemap=\"#IMG" + imgID + '"');

        // imagemaps have hyperlink borders by default, even if we don't
        // have a URL we're pointing to.  Turn that border off.
        if (!StringUtils.hasValue(href) || parameters.containsKey("noBorder"))
            out.write(" border=\"0\"");

        // Our client might want to add attributes to the image tag. Look
        // through the query parameters we received for arbitrary attributes
        // starting with the prefix "img", and copy them into the tag.
        for (Iterator i = parameters.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String attrName = (String) e.getKey();
            if (attrName.startsWith("img") && !attrName.endsWith("_ALL")) {
                out.write(" " + attrName.substring(3) + "=\"");
                out.write(HTMLUtils.escapeEntities(e.getValue().toString()));
                out.write('"');
            }
        }

        out.write(">");

        // finally, write the image map.  Note that we have to strip line
        // break characters from the resulting HTML, otherwise firefox seems
        // to decide that the <map> tag actually takes up space on the page
        String imageMapHtml = ImageMapUtilities.getImageMap("IMG" + imgID,
                info, getToolTipGenerator(tooltip),
                new StandardURLTagFragmentGenerator());
        for (int i = 0;  i < imageMapHtml.length();  i++) {
            char c = imageMapHtml.charAt(i);
            if (c != '\r' && c != '\n')
                out.write(c);
        }
        out.flush();
    }

    protected ToolTipTagFragmentGenerator getToolTipGenerator(String defaultTooltip) {
        String tooltipType = getSetting("tooltips");
        if ("overlib".equalsIgnoreCase(tooltipType))
            return new CustomOverlibFragmentGenerator(defaultTooltip);
        else if ("plain".equalsIgnoreCase(tooltipType))
            return new StandardToolTipTagFragmentGenerator();
        else
            return null;
    }

    private static class CustomOverlibFragmentGenerator implements
            ToolTipTagFragmentGenerator {

        private String defaultTooltip;

        public CustomOverlibFragmentGenerator(String tooltip) {
            this.defaultTooltip = tooltip;
        }

        public String generateToolTipFragment(String toolTipText) {
            // In the dashboard, many tool tip generators are dual purpose
            // (generating tooltips both for rich client and HTML views). To
            // produce styled tips in the rich client, generators must wrap the
            // tool tip text in an <html> tag. If we see this pattern when
            // producing an HTML report, strip the <html> and <body> tags.
            if (toolTipText.startsWith("<html>"))
                toolTipText = toolTipText.substring(6);
            if (toolTipText.endsWith("</html>"))
                toolTipText = toolTipText.substring(0, toolTipText.length() - 7);
            if (toolTipText.startsWith("<body>"))
                toolTipText = toolTipText.substring(6);
            if (toolTipText.endsWith("</body>"))
                toolTipText = toolTipText.substring(0, toolTipText.length() - 7);

            // generate HTML attributes to display the tip via overLib.
            StringBuilder result = new StringBuilder();
            result.append(" onMouseOver=\"return overlib('");
            result.append(ImageMapUtilities.htmlEscape(ImageMapUtilities
                    .javascriptEscape(toolTipText)));
            if (toolTipText.equals(defaultTooltip))
                result.append("', DELAY, 1000, FOLLOWMOUSE, FGCOLOR, '#FFFFCC");
            result.append("');\" onMouseOut=\"return nd();\"");
            return result.toString();
        }

    }

    protected void maybeAdjustTitleFontSize(JFreeChart chart,
                                        Graphics2D g, int width) {
        // if the chart has no title, do nothing.
        if (chart.getTitle() == null) return;
        String title = chart.getTitle().getText();
        if (title == null || title.length() == 0) return;

        // compute the width needed to draw the title.
        Font f = chart.getTitle().getFont();
        Font sf = getFontSizeToFit(g, f, title, (int)(width * 0.85));
        if (f != sf)
            chart.getTitle().setFont(sf);
    }

    protected Font getFontSizeToFit(Graphics2D g, Font baseFont,
                                    String text, int width) {
        FontMetrics m = g.getFontMetrics(baseFont);
        int currentWidth = m.stringWidth(text);
        Font result = baseFont;

        int maxIter = 100;
        while (currentWidth > width && maxIter-- > 0) {
            result = result.deriveFont(result.getSize2D() * 0.95f);
            m = g.getFontMetrics(result);
            currentWidth = m.stringWidth(text);
        }

        return result;
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
    protected static float ALPHA = 1.0f;
    protected boolean get3DSetting() {
        String setting = getSetting("3d");

        if ("false".equalsIgnoreCase(setting))
            return false;

        if ("true".equalsIgnoreCase(setting)) {
            ALPHA = 1.0f;
            return true;
        }

        if (setting != null) try {
            ALPHA = Float.parseFloat(setting);
            return true;
        } catch (Exception e) { }

        ALPHA = 1.0f;
        return true;
    }


    protected void setupCategoryChart(JFreeChart chart) {

        if (!(chart.getPlot() instanceof CategoryPlot)) return;
        CategoryPlot cp = chart.getCategoryPlot();

        CategoryAxis catAxis = cp.getDomainAxis();
        ValueAxis otherAxis  = cp.getRangeAxis();

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

            catAxis.setLabel(catAxisLabel);
            otherAxis.setLabel(otherAxisLabel);
            if ("vertical".equalsIgnoreCase(catLabels))
                catAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
            else if ("none".equalsIgnoreCase(catLabels))
                catAxis.setTickLabelsVisible(false);
        }

        if (data.numCols() == 1 && getParameter("noSkipLegend") == null) {
            chart.removeLegend();
            chart.getPlot().setInsets(new RectangleInsets(5,2,2,5));
        }
    }

    /** Optionally massage the parameters before the ResultSet is generated */
    public void massageParameters() {}

    /** Create some specific type of chart */
    public abstract JFreeChart createChart();

    protected Axis getHorizontalAxis(JFreeChart chart) {
        return getAxis(chart, PlotOrientation.HORIZONTAL);
    }

    protected Axis getVerticalAxis(JFreeChart chart) {
        return getAxis(chart, PlotOrientation.VERTICAL);
    }

    protected Axis getAxis(JFreeChart chart, PlotOrientation dir) {
        try {
            CategoryPlot p = chart.getCategoryPlot();
            if (dir.equals(p.getOrientation()))
                return p.getRangeAxis();
            else
                return p.getDomainAxis();
        } catch (Exception e) {
            return null;
        }
    }
}
