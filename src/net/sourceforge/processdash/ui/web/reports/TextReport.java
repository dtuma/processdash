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

package net.sourceforge.processdash.ui.web.reports;


import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;



public class TextReport extends TinyCGIBase {

    static Resources resources = Resources.getGlobalBundle();

    @Override
    protected void writeHeader() {}

    @Override
    protected void writeContents() throws IOException {
        ResultSet resultSet = getResultSet();

        String format = getParameter(FORMAT_PARAM);
        if (FORMAT_XML.equals(format))
            writeXml(resultSet);
        else
            writeHtml(resultSet); // default
    }

    private ResultSet getResultSet() {
        // get the data
        retrieveParamsFromServlet("dqf");
        if (parameters.get("h0") == null)
            parameters.put("h0", "Project/Task");
        ResultSet tableData = ResultSet.get(getDataRepository(), parameters,
            getPrefix(), getPSPProperties());
        if (parameters.get("transpose") != null)
            tableData = tableData.transpose();

        return tableData;
    }



    /** Write result set data in XML format */
    private void writeXml(ResultSet resultSet) throws IOException {
        out.write("Content-Type: text/xml\r\n\r\n");
        out.flush();

        boolean whitespace = parameters.containsKey("whitespace");
        boolean inlineAttributes = parameters.containsKey("dataAsAttr");

        if (inlineAttributes) {
            for (int col = 1; col <= resultSet.numCols(); col++) {
                String colName = resultSet.getColName(col);
                String attrName = textToSafeAttrName(colName);
                resultSet.setColName(col, attrName);
            }
        }

        XmlSerializer xml = XMLUtils.getXmlSerializer(whitespace);
        xml.setOutput(outStream, ENCODING);
        xml.startDocument(ENCODING, null);

        xml.startTag(null, RESULT_SET_TAG);

        for (int row = 1; row <= resultSet.numRows(); row++) {
            xml.startTag(null, RESULT_ITEM_TAG);
            xml.attribute(null, PATH_ATTR, resultSet.getRowName(row));

            for (int col = 1; col <= resultSet.numCols(); col++) {
                String name = resultSet.getColName(col);
                SimpleData d = resultSet.getData(row, col);
                String value = (d == null ? null : d.format());

                if (inlineAttributes) {
                    if (value != null)
                        xml.attribute(null, name, value);
                } else {
                    xml.startTag(null, RESULT_DATA_TAG);
                    xml.attribute(null, NAME_ATTR, name);
                    if (value != null)
                        xml.attribute(null, VALUE_ATTR, value);
                    xml.endTag(null, RESULT_DATA_TAG);
                }
            }

            xml.endTag(null, RESULT_ITEM_TAG);
        }

        xml.endTag(null, RESULT_SET_TAG);

        xml.endDocument();
    }

    private String textToSafeAttrName(String text) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c))
                result.append(c);
            else
                result.append("_");
        }
        return result.toString();
    }



    /** Write result set data in HTML format */
    private void writeHtml(ResultSet tableData) throws IOException {
        super.writeHeader();

        String title = tr((String) parameters.get("title"));
        String head  = tr((String) parameters.get("headerComment"));
        String foot  = tr((String) parameters.get("footerComment"));

        boolean skipRowHdr = (parameters.get("skipRowHdr") != null);
        boolean skipColHdr = (parameters.get("skipColHdr") != null);
        boolean includable = (parameters.get("includable") != null);

        if (!includable) {
            out.println("<HTML><HEAD>");
            if (title != null) out.println("<TITLE>"+ esc(title) +"</TITLE>");
            out.println(cssLinkHTML());

            out.println("</HEAD><BODY>");
            if (title != null) out.println("<H1>" + esc(title) + "</H1>");
            if (head  != null) out.println("<P>"  + esc(head)  + "</P>");
            out.println(parameters.containsKey("style") ?
                        "<TABLE>" : "<TABLE BORDER>");
        }

        // print the table
        int firstRow = (skipColHdr ? 1 : 0);
        int firstCol = (skipRowHdr ? 1 : 0);
        for (int row=firstRow;  row <= tableData.numRows();  row++) {
            out.println("<TR>");
            for (int col=firstCol;  col <= tableData.numCols();  col++) {
                out.print("<TD" + getColAttributes(col) + ">");
                out.print(esc(tableData.format(row, col)));
                out.println("</TD>");
            }
            out.println("</TR>");
        }

        if (!includable) {
            out.println("</TABLE>");
            if (foot != null) out.println("<P>" + esc(foot) + "</P>");
            out.print("<P class='doNotPrint'><A HREF=\"excel.iqy\"><I>" +
                      resources.getString("Export_to_Excel") +
                      "</I></A></P></BODY></HTML>");
        }
    }

    private String getColAttributes(int col) {
        String cssClass = (String) parameters.get("c" + col);
        if (cssClass == null) cssClass = (String) parameters.get("c");
        if (cssClass == null) return "";
        return " class='" + cssClass + "'";
    }

    protected String tr(String s) {
        return Translator.translate(s);
    }

    protected String esc(String s) {
        return HTMLUtils.escapeEntities(s);
    }

    private static final String FORMAT_PARAM = "format";
    private static final String FORMAT_XML = "xml";

    private static final String ENCODING = "UTF-8";
    private static final String RESULT_SET_TAG = "pdashResultSet";
    private static final String RESULT_ITEM_TAG = "resultItem";
    private static final String PATH_ATTR = "path";
    private static final String RESULT_DATA_TAG = "resultData";
    private static final String NAME_ATTR = "name";
    private static final String VALUE_ATTR = "value";

}
