// Copyright (C) 2005-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Element;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.MalformedValueException;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.InvalidDatafileFormat;
import net.sourceforge.processdash.ev.ImportedEVManager;
import net.sourceforge.processdash.log.defects.ImportedDefectManager;
import net.sourceforge.processdash.log.time.ImportedTimeLogManager;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.util.EscapeString;
import net.sourceforge.processdash.util.XMLUtils;

public class TextMetricsFileImporter implements Runnable {

    static final String XML_DATA_NAME_SUFFIX = "/XML Task List";

    private File file;

    private DataRepository data;

    private String prefix;

    public TextMetricsFileImporter(DataRepository data, File file, String prefix) {
        this.data = data;
        this.file = file;
        this.prefix = prefix;
    }

    public void run() {
        try {
            doImport();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doImport() throws IOException {
        InputStream inputStream = new FileInputStream(file);
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    inputStream, "UTF-8"));
            Map defns = new HashMap();
            Map<String, String> taskLists = new HashMap();

            String line, name, value;
            int commaPos;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("!") || line.startsWith("<!--"))
                    break;

                commaPos = line.indexOf(',');
                if (commaPos == -1) {
                    in.close();
                    return; // this isn't a valid dump file.
                }

                name = line.substring(1, commaPos);
                name = EscapeString.unescape(name, '\\', ",", "c");
                value = line.substring(commaPos + 1);

                // don't import the data elements which contain export
                // instructions - this would get us into an infinite
                // import/export loop.
                if (name.indexOf(ExportManager.EXPORT_DATANAME) != -1)
                    continue;

                // special handling for earned value schedules
                if (name.endsWith(XML_DATA_NAME_SUFFIX)) {
                    taskLists.put(name, value);
                    continue;
                }

                // To the best of my knowledge, the DataImporter is
                // currently only being used to import individual
                // data, for the purpose of calculating team rollups.
                // Rollups interact with this data in a fairly
                // predictable way; for now, I'll take advantage of
                // this predictable behavior by omitting data elements
                // which I know cannot affect rollups. This will
                // significantly reduce the memory requirements of the
                // team dashboard. In particular, I:
                //
                // (1) won't import "To Date" data, and
                if (name.endsWith(" To Date"))
                    continue;
                //
                // (2) won't import data values that are zero or invalid.
                if (value.equals("0.0") || value.equals("NaN")
                        || value.equals("Infinity"))
                    continue;

                defns.put(name, parseValue(value));
            }

            ImportedDefectManager.closeDefects(prefix);
            ImportedTimeLogManager.getInstance().closeTimeLogs(prefix);
            ImportedEVManager.getInstance().closeTaskLists(prefix);
            while (line != null && !line.startsWith("<!--"))
                line = in.readLine();
            if (line != null) {
                DefectImporterXMLv1 defImp = new DefectImporterXMLv1();
                defImp.importDefectsFromStream(in, prefix);
            }
            for (Entry<String, String> e : taskLists.entrySet()) {
                importEvTaskList(e.getKey(), e.getValue());
            }

            // Protect this data from being viewed via external http requests.
            defns.put("_Password_", ImmutableDoubleData.READ_ONLY_ZERO);

            try {
                data.mountImportedData(prefix, defns);
            } catch (InvalidDatafileFormat idf) {
            }
        } finally {
            inputStream.close();
        }

    }

    private static Object parseValue(String value) {
        SimpleData result;

        // is it a tag?
        if ("TAG".equalsIgnoreCase(value))
            return TagData.getInstance();

        // first, try to interpret the string as a number.
        if ("0.0".equals(value))
            return ImmutableDoubleData.READ_ONLY_ZERO;
        if ("NaN".equals(value))
            return ImmutableDoubleData.READ_ONLY_NAN;
        if (DoubleData.P_INF_STR.equals(value)
                || DoubleData.N_INF_STR.equals(value))
            return ImmutableDoubleData.DIVIDE_BY_ZERO;
        if (value.length() > 0)
            switch (value.charAt(0)) {
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
            case '-': case '+': case '.': case ',':
                try {
                    result = new DoubleData(value);
                    result.setEditable(false);
                    return result;
                } catch (MalformedValueException mfe) {
                }
            }

        // next, try to interpret the string as a date.
        try {
            result = DateData.create(value);
            result.setEditable(false);
            return result;
        } catch (MalformedValueException mfe) {
        }

        // give up and interpret it as a plain string.
        result = StringData.create(StringData.unescapeString(value));
        result.setEditable(false);
        return result;
    }

    private void importEvTaskList(String name, String value) {
        int nameLen = name.length() - XML_DATA_NAME_SUFFIX.length();
        String uniqueKey = prefix + name.substring(0, nameLen);

        try {
            String rawXml = StringData.unescapeString(value);
            Element xml = XMLUtils.parse(rawXml).getDocumentElement();
            ImportedEVManager.getInstance().importTaskList(uniqueKey, xml, null);
        } catch (Exception e) {
            System.err.println("Cannot parse imported XML schedule for key '"
                    + name + "' - discarding");
        }
    }

}
