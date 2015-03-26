// Copyright (C) 2011-2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.ui;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.tool.diff.AbstractLanguageFilter;
import net.sourceforge.processdash.tool.diff.engine.AccountingType;
import net.sourceforge.processdash.tool.diff.engine.DiffAdapter;
import net.sourceforge.processdash.tool.diff.engine.DiffEvent;
import net.sourceforge.processdash.tool.diff.engine.DiffResult;
import net.sourceforge.processdash.util.XMLUtils;

public class XmlDiffReportWriter extends DiffAdapter {


    private OutputStream out;

    private XmlSerializer xml;

    private int[] locCounts = new int[AccountingType.values().length];

    public XmlDiffReportWriter() throws IOException {
        this(System.out);
    }

    public XmlDiffReportWriter(String filename) throws IOException {
        this(XMLUtils.hasValue(filename)
                ? new FileOutputStream(filename)
                : System.out);
    }

    public XmlDiffReportWriter(OutputStream out) throws IOException {
        this.out = new BufferedOutputStream(out);
        this.xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(this.out, ENCODING);
    }

    @Override
    public void analysisStarting(DiffEvent e) throws IOException {
        xml.startDocument(ENCODING, null);
        xml.startTag(null, DOC_TAG);
    }

    @Override
    public void fileAnalysisFinished(DiffEvent e) throws IOException {
        DiffResult r = e.getDiffResult();
        if (r == null)
            return;

        xml.startTag(null, FILE_TAG);
        xml.attribute(null, NAME_ATTR, r.getFile().getFilename());
        xml.attribute(null, CHANGE_ATTR, TYPE_ATTR_NAMES[r.getChangeType()
                .ordinal()]);
        xml.attribute(null, TYPE_ATTR, AbstractLanguageFilter.getFilterName(r
                .getLanguageFilter()));
        int[] thisFileCounts = r.getLocCounts();
        if (thisFileCounts != null) {    // binary files have null locCounts
            writeLocCounts(thisFileCounts);
            for (int i = 0; i < thisFileCounts.length; i++) {
                this.locCounts[i] += thisFileCounts[i];
            }
        }
        xml.endTag(null, FILE_TAG);
    }

    @Override
    public void analysisFinished(DiffEvent e) throws IOException {
        xml.startTag(null, TOTAL_TAG);
        writeLocCounts(locCounts);
        xml.endTag(null, TOTAL_TAG);

        xml.endTag(null, DOC_TAG);
        xml.endDocument();
        out.flush();
        out.close();
    }

    private void writeLocCounts(int[] counts) throws IOException {
        for (int i = 0; i < counts.length; i++) {
            String oneCount = Integer.toString(counts[i]);
            xml.attribute(null, TYPE_ATTR_NAMES[i], oneCount);
        }
    }

    private static final String ENCODING = "UTF-8";

    private static final String DOC_TAG = "locCounts";

    private static final String FILE_TAG = "file";

    private static final String TOTAL_TAG = "total";

    private static final String NAME_ATTR = "name";

    private static final String TYPE_ATTR = "type";

    private static final String CHANGE_ATTR = "change";

    private static final String[] TYPE_ATTR_NAMES = new String[AccountingType
            .values().length];
    static {
        for (AccountingType type : AccountingType.values()) {
            TYPE_ATTR_NAMES[type.ordinal()] = type.toString().toLowerCase();
        }
    }

}
