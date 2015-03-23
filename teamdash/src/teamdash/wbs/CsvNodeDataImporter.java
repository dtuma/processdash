// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList;
import teamdash.wbs.columns.TaskDependencyColumn;
import teamdash.wbs.columns.TeamMemberTimeColumn;
import teamdash.wbs.columns.TeamTimeColumn;

/** Imports textual data (most likely comma-separated values dumped by
 * Microsoft Project) and converts it to a list of WBSNode objects.
 */
public class CsvNodeDataImporter {

    public static final String CSV_EXTRA_FIELDS_KEY = "csvAttributeMap";


    public class ParseException extends Exception {

        public ParseException(String message) {
            super(message);
        }

    }

    private BufferedReader in;

    private TeamMemberList team;

    private char delimiter;

    private String[] fieldNamesInFile;

    private int[] columnPos;


    public List getNodesFromCsvFile(File f, TeamMemberList teamList)
            throws IOException, ParseException {
        in = new BufferedReader(new FileReader(f));
        team = teamList;

        parseHeaderLine();
        List result = parseData();

        in.close();
        in = null;
        team = null;

        return result;
    }

    private void parseHeaderLine() throws ParseException, IOException {
        String line = in.readLine();
        delimiter = determineDelimiter(line);

        fieldNamesInFile = parseCsvLine(line, delimiter);
        columnPos = new int[FIELD_NAMES_OF_INTEREST.length];
        Arrays.fill(columnPos, -1);
        for (int colID = 0; colID < FIELD_NAMES_OF_INTEREST.length; colID++) {
            String[] lookingForField = FIELD_NAMES_OF_INTEREST[colID];
            for (int pos = 0; pos < fieldNamesInFile.length; pos++) {
                if (nameMatches(fieldNamesInFile[pos], lookingForField)) {
                    columnPos[colID] = pos;
                    break;
                }
            }
        }

        if (columnPos[NAME] == -1)
            throw new ParseException(
                    "No task name column was found.  Please\n"
                            + "export the data again, and ensure that:\n"
                            + " - you enable the 'Export includes headers' option\n"
                            + " - you export the 'Name' field\n"
                            + " - you use a comma, space, or tab as the field delimiter");
    }

    private char determineDelimiter(String line) throws ParseException {
        for (int i = 0; i < POSSIBLE_DELIMITERS.length(); i++) {
            char c = POSSIBLE_DELIMITERS.charAt(i);
            if (line.indexOf(c) != -1)
                return c;
        }
        // if we reach this point, it is possible that the file only contains
        // a single field.  On that assumption, choose an arbitrary delimiter.
        return POSSIBLE_DELIMITERS.charAt(0);
    }

    private boolean nameMatches(String name, String[] names) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equalsIgnoreCase(name)
                    || names[i].replace('_', ' ').equalsIgnoreCase(name))
                return true;
        }
        return false;
    }

    private List parseData() throws IOException {
        List result = new ArrayList();
        WBSNode node = new WBSNode(null, "Imported Items", "Component", 1, true);
        result.add(node);

        String line;
        int idNum = 1;
        while ((line = in.readLine()) != null) {
            String[] fields = parseCsvLine(line, delimiter);
            node = parseNode(fields, idNum++);
            result.add(node);
        }
        tweakNodes(result);

        return result;
    }

    private WBSNode parseNode(String[] fields, int nodeID) {
        WBSNode result = createBasicNode(fields);
        parseIdInfo(result, fields, nodeID);
        parsePredecessorInfo(result, fields);
        parseDurationInfo(result, fields);
        parseResourcesInfo(result, fields);
        storeExtraFields(result, fields);
        return result;
    }

    private WBSNode createBasicNode(String[] fields) {
        String name = fields[columnPos[NAME]];
        int indent = getIntField(fields, INDENT, 1) + 1;
        boolean expand = (indent < 3);
        WBSNode result = new WBSNode(null, name, TYPE, indent, expand);
        return result;
    }

    private void parseIdInfo(WBSNode node, String[] fields, int nodeID) {
        int id = getIntField(fields, ID, nodeID);
        node.setAttribute(MasterWBSUtil.SOURCE_NODE_ID, "csv:" + id);
    }

    private void parsePredecessorInfo(WBSNode node, String[] fields) {
        if (columnPos[PREDECESSORS] != -1) {
            String pred = fields[columnPos[PREDECESSORS]];
            if (pred.length() > 0) {
                String[] predCsvIDs = pred.split(",");
                StringBuffer predDashIDs = new StringBuffer();
                for (int i = 0; i < predCsvIDs.length; i++) {
                    predDashIDs.append(",csv:").append(predCsvIDs[i]);
                    node.setAttribute(TaskDependencyColumn.NAME_ATTR_PREFIX + i,
                            "Dependency not included in CSV import file");
                }
                node.setAttribute(TaskDependencyColumn.ID_LIST_ATTR,
                        predDashIDs.substring(1));
            }
        }
    }

    private void parseDurationInfo(WBSNode node, String[] fields) {
        double duration = parseDurationAsHours(fields, DURATION);
        if (duration > 0) {
            node.setAttribute(TIME_PER_PERSON_ATTR, Double.toString(duration));
            node.setAttribute(DURATION_TEMP, fields[columnPos[DURATION]]);
        }
    }

    private void parseResourcesInfo(WBSNode node, String[] fields) {
        if (columnPos[RESOURCES] != -1) {
            try {
                int num = fields[columnPos[RESOURCES]].split(",").length;
                if (num > 1)
                    node.setAttribute(NUM_PEOPLE_ATTR, new Double(num));
            } catch (Exception e) {
            }
        }

        if (columnPos[INITIALS] != -1) {
            node.setAttribute(INITIALS_TEMP, fields[columnPos[INITIALS]]);
        }
    }

    private void storeExtraFields(WBSNode result, String[] fields) {
        Map extraFields = new LinkedHashMap();
        for (int i = 0; i < fields.length; i++) {
            String fieldName = fieldNamesInFile[i];
            String fieldValue = fields[i];
            extraFields.put(fieldName, fieldValue);
        }
        for (int i = 0; i < columnPos.length; i++) {
            int pos = columnPos[i];
            if (pos != -1) {
                String usedFieldName = fieldNamesInFile[pos];
                extraFields.remove(usedFieldName);
            }
        }
        if (!extraFields.isEmpty())
            result.setAttribute(CSV_EXTRA_FIELDS_KEY, extraFields);
    }

    private void tweakNodes(List nodes) {
        if (nodes == null)
            return;

        if (nodes.size() > 2) {
            // remove top-down estimates from non-leaf nodes
            ListIterator i = nodes.listIterator(nodes.size());
            WBSNode nextNode = (WBSNode) i.previous();
            tweakInitialsData(nextNode);
            while (i.hasPrevious()) {
                WBSNode node = (WBSNode) i.previous();
                int nodeIndent = node.getIndentLevel();
                int nextIndent = nextNode.getIndentLevel();
                if (nodeIndent < nextIndent) {
                    // this node is a parent node.
                    node.setAttribute(TIME_PER_PERSON_ATTR, null);
                    node.setAttribute(NUM_PEOPLE_ATTR, null);
                } else {
                    // this node is a leaf node.
                    tweakInitialsData(node);
                }
                node.setAttribute(INITIALS_TEMP, null);
                node.setAttribute(DURATION_TEMP, null);
                nextNode = node;
            }
        }
    }

    private void tweakInitialsData(WBSNode node) {
        String initials = (String) node.getAttribute(INITIALS_TEMP);
        Object timePerPerson = node.getAttribute(TIME_PER_PERSON_ATTR);
        if (initials == null || timePerPerson == null)
            return;

        String duration = (String) node.getAttribute(DURATION_TEMP);
        String[] intialsList = initials.split(",");
        for (int i = 0; i < intialsList.length; i++) {
            TeamMember m = getTeamMember(intialsList[i]);
            if (m != null) {
                String init = m.getInitials();
                Object time = timePerPerson;

                Double hoursPerWeek = m.getHoursPerWeek();
                if (hoursPerWeek != null
                        && hoursPerWeek.doubleValue() != 0
                        && hoursPerWeek.doubleValue() != 20)
                    time = Double.toString(parseDurationAsCustomHours(duration,
                            hoursPerWeek));

                node.setAttribute(init + INDIV_TIME_SUFFIX, time);
            }
        }
    }

    private TeamMember getTeamMember(String initials) {
        initials = initials.trim();
        if (initials.length() == 0)
            return null;

        for (Iterator i = team.getTeamMembers().iterator(); i.hasNext();) {
            TeamMember m = (TeamMember) i.next();
            if (initials.equalsIgnoreCase(m.getInitials()))
                return m;
        }
        return null;
    }

    private int getIntField(String[] fields, int colID, int defaultValue) {
        int col = columnPos[colID];
        if (col != -1)
            try {
                return Integer.parseInt(fields[col]);
            } catch (Exception e) {
            }
        return defaultValue;
    }

    private double parseDurationAsHours(String[] fields, int colID) {
        int col = columnPos[colID];
        if (col == -1)
            return 0;

        String val = fields[col];
        if (val.length() == 0)
            return 0;

        return parseDurationAsHours(val, DURATION_MULTIPLIERS);
    }

    private double parseDurationAsCustomHours(String val, Double hoursPerWeek) {
        double[] multipliers = (double[]) DURATION_MULTIPLIERS.clone();
        for (int i = 4; i < multipliers.length; i++)
            multipliers[i] *= (hoursPerWeek.doubleValue() / 20.0);
        return parseDurationAsHours(val, multipliers);
    }

    private double parseDurationAsHours(String val, double[] multipliers) {
        Matcher m = DURATION_PATTERN.matcher(val);
        if (!m.matches())
            return 0;

        double result = Double.parseDouble(m.group(1));
        for (int group = 2; group < multipliers.length; group++)
            if (m.group(group) != null)
                result *= multipliers[group];

        return result;
    }

    private static final Pattern DURATION_PATTERN = Pattern
            .compile("([0-9.]+)\\s*"
                    + "(m|mins?|minutes?)?(h|hrs?|hours?)?(d|dys?|days?)?"
                    + "(w|wks?|weeks?)?(mo|mons?|months?)?(y|yrs?|years?)?");

    private static final double[] DURATION_MULTIPLIERS = { 0, 0, // unused
            1.0 / 60, // minutes
            1.0, // hours
            4.0, // days - assumes 20 task-hours per week
            20.0, // weeks
            80.0, // months
            1000.0, // years
    };

    public static String[] parseCsvLine(String line, char delim) {
        List results = new LinkedList();
        StringBuffer field = new StringBuffer();
        boolean inQuotes = false;
        int len = line.length();

        for (int i = 0; i < len; i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (inQuotes && (i + 1 < len) && (line.charAt(i + 1) == '"')) {
                    field.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }

            } else if (ch == delim && !inQuotes) {
                results.add(field.toString());
                field.setLength(0);

            } else {
                field.append(ch);
            }
        }
        results.add(field.toString());

        return (String[]) results.toArray(new String[results.size()]);
    }

    private static final int ID = 0;
    private static final int NAME = 1;
    private static final int INDENT = 2;
    private static final int DURATION = 3;
    private static final int RESOURCES = 4;
    private static final int INITIALS = 5;
    private static final int PREDECESSORS = 6;

    private static final String[][] FIELD_NAMES_OF_INTEREST = {
            { "ID" },
            { "Name", "Task_Name" },
            { "Indent", "Outline_Level" },
            { "Duration" },
            { "Resource_Names", "Resource_Initials", "Resources" },
            { "Resource_Initials", "Initials" },
            { "Predecessors" },
    };

    private static final String POSSIBLE_DELIMITERS = "\t, ";
    private static final String TYPE = WBSNode.UNKNOWN_TYPE;
    private static final String INITIALS_TEMP = "Initials_Working_Data";
    private static final String DURATION_TEMP = "Duration_Working_Data";

    private static final String TIME_PER_PERSON_ATTR = TeamTimeColumn.TPP_ATTR;
    private static final String NUM_PEOPLE_ATTR = TeamTimeColumn.NUM_PEOPLE_ATTR;
    private static final String INDIV_TIME_SUFFIX =
            TeamMemberTimeColumn.ATTR_SUFFIX + " (Top Down)";

}
