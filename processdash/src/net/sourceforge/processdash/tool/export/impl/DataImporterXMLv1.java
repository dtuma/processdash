// Copyright (C) 2005-2013 Tuma Solutions, LLC
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

import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.MalformedValueException;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.util.XmlNumberFormatter;

import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class DataImporterXMLv1 implements ArchiveMetricsFileImporter.Handler,
        ArchiveMetricsXmlConstants, DataXmlConstantsv1 {

    public boolean canHandle(String type, String version) {
        return FILE_TYPE_METRICS.equals(type) && "1".equals(version);
    }

    public void handle(ArchiveMetricsFileImporter caller, InputStream in,
            String type, String version) throws Exception {
        Map defns = caller.getDefns();
        if (shouldImportRootValuesOnly(caller))
            defns.put(ROOT_VALUES_ONLY_TAG, "true");

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();

        parser.setInput(in, ENCODING);
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, DATA_ELEM);
        importData(parser, defns, null);
        parser.require(XmlPullParser.END_TAG, null, DATA_ELEM);

        defns.remove(ROOT_VALUES_ONLY_TAG);
    }

    private boolean shouldImportRootValuesOnly(ArchiveMetricsFileImporter caller) {
        Element spec = caller.getImportSpec(FILE_TYPE_METRICS);
        return spec != null
                && spec.getElementsByTagName(ROOT_VALUES_ONLY_TAG).getLength() > 0;

    }

    private static void importData(XmlPullParser parser, Map defns,
            String prefix) throws XmlPullParserException, IOException {
        while (true) {
            int event = parser.next();
            if (event == XmlPullParser.END_TAG)
                return;

            if (event == XmlPullParser.START_TAG) {
                String tagName = parser.getName();
                ElementHandler h = (ElementHandler) HANDLER_MAP.get(tagName);
                if (h == null)
                    throw new IOException("Couldn't understand tag '" + tagName
                            + "'");
                else
                    h.handle(parser, defns, prefix);
            }
        }
    }

    private static String concat(String prefix, String name) {
        if (prefix == null)
            return name;
        else
            return prefix + "/" + name;
    }

    private interface ElementHandler {
        public void handle(XmlPullParser parser, Map defns, String prefix)
                throws XmlPullParserException, IOException;
    }

    private static class NodeHandler implements ElementHandler {
        public void handle(XmlPullParser parser, Map defns, String prefix)
                throws XmlPullParserException, IOException {
            // if we are in "root values only" mode, we have already seen at
            // least one <tag> element, and we have just observed another
            // <node> element, then swap out the destination defns map with
            // a throwaway replacement.  This <node> and all of its children
            // will store their data in this throwaway map, so it won't be
            // retained in the real map we return to our caller.
            if (ROOT_VALUES_DONE.equals(defns.get(ROOT_VALUES_ONLY_TAG)))
                defns = new HashMap();

            String childName = parser.getAttributeValue(null, NAME_ATTR);
            importData(parser, defns, concat(prefix, childName));
        }

    }

    private static abstract class AbstractDataHandler implements ElementHandler {

        public void handle(XmlPullParser parser, Map defns, String prefix)
                throws XmlPullParserException, IOException {
            try {
                String elemName = parser.getAttributeValue(null, NAME_ATTR);
                String dataName = concat(prefix, elemName);
                String valueText = parser.nextText();
                SimpleData value = parse(valueText);
                if (shouldImport(dataName, value))
                    defns.put(dataName, value);
            } catch (MalformedValueException e) {
                e.printStackTrace();
            }
        }

        abstract SimpleData parse(String value) throws MalformedValueException;

    }

    private static class NumberDataHandler extends AbstractDataHandler {
        SimpleData parse(String value) throws MalformedValueException {
            SimpleData result = (SimpleData) NUMBER_MAP.get(value);
            if (result == null) {
                try {
                    Number d = NUMBER_FORMAT.parse(value);
                    if (d != null)
                        result = new DoubleData(d.doubleValue(), false);
                } catch (ParseException e) {
                    throw new MalformedValueException();
                }
            }
            return result;
        }
    }

    private static class StringDataHandler extends AbstractDataHandler {

        @Override
        public void handle(XmlPullParser parser, Map defns, String prefix)
                throws XmlPullParserException, IOException {
            String elemName = parser.getAttributeValue(null, NAME_ATTR);
            if ("Indiv_Initials".equals(elemName))
                maybeAddPseudoIndivRootTag(defns, prefix);

            super.handle(parser, defns, prefix);
        }

        /**
         * PDASH data files from very old dashboard backups (with team projects
         * that were created before v1.9.2) do not have an "Indiv Root Tag".
         * This tag is required by newer team process definitions, so we will
         * manufacture it if is is missing.
         */
        private void maybeAddPseudoIndivRootTag(Map<String, Object> defns,
                String prefix) {
            String processId = null;
            for (String dataName : defns.keySet()) {
                if (dataName.endsWith(" Rollup Eligible")) {
                    int slashPos = dataName.lastIndexOf('/');
                    processId = dataName.substring(slashPos + 1,
                        dataName.length() - 16);
                    if (!processId.equals("PSP"))
                        break;
                }
            }
            if (processId != null) {
                String indivTagName = prefix + "/" + processId
                        + " Indiv Root Tag";
                if (!defns.containsKey(indivTagName))
                    defns.put(indivTagName, TagData.getInstance());
            }
        }

        SimpleData parse(String value) throws MalformedValueException {
            StringData result = StringData.create(value);
            result.setEditable(false);
            return result;
        }
    }

    private static class TagDataHandler extends AbstractDataHandler {
        @Override
        public void handle(XmlPullParser parser, Map defns, String prefix)
                throws XmlPullParserException, IOException {
            // if we are in "root values only" mode, record the fact that
            // we just saw a <tag> data element.
            if (defns.containsKey(ROOT_VALUES_ONLY_TAG))
                defns.put(ROOT_VALUES_ONLY_TAG, ROOT_VALUES_DONE);
            super.handle(parser, defns, prefix);
        }
        SimpleData parse(String value) throws MalformedValueException {
            return TagData.getInstance();
        }
    }

    private static class DateDataHandler extends AbstractDataHandler {
        SimpleData parse(String value) throws MalformedValueException {
            DateData result = DateData.create(value);
            result.setEditable(false);
            return result;
        }
    }

    private static Map HANDLER_MAP = initHandlerMap();

    private static Map initHandlerMap() {
        HashMap result = new HashMap();
        result.put(NODE_ELEM, new NodeHandler());
        result.put(NUMBER_ELEM, new NumberDataHandler());
        result.put(STRING_ELEM, new StringDataHandler());
        result.put(TAG_ELEM, new TagDataHandler());
        result.put(DATE_ELEM, new DateDataHandler());
        return Collections.unmodifiableMap(result);
    }

    private static boolean shouldImport(String dataName, SimpleData value) {
        if (dataName == null || value == null)
            return false;
        if (dataName.indexOf(ExportManager.EXPORT_DATANAME) != -1)
            return false;
        return true;
    }

    private static Map NUMBER_MAP = initNumberMap();

    private static Map initNumberMap() {
        HashMap result = new HashMap();
        for (int i = 0; i < 100; i++) {
            ImmutableDoubleData num = new ImmutableDoubleData(i, false, true);
            result.put(Integer.toString(i), num);
            result.put(Integer.toString(i) + ".0", num);
        }
        return Collections.unmodifiableMap(result);
    }

    private static NumberFormat NUMBER_FORMAT = new XmlNumberFormatter();

    private static final String ROOT_VALUES_ONLY_TAG = "importRootElementsOnly";

    private static final String ROOT_VALUES_DONE = "done";

}
