// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.awt.event.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import javax.swing.tree.TreePath;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pspdash.data.DataRepository;
import pspdash.data.DataComparator;
import pspdash.data.DoubleData;
import pspdash.data.StringData;
import pspdash.data.SimpleData;
import pspdash.data.ListData;

public class EVTaskList extends AbstractTreeTableModel
    implements EVTask.Listener, ActionListener
{

    public static final String MAIN_DATA_PREFIX = "/Task-Schedule/";


    protected String taskListName;
    protected EVSchedule schedule;

    /** timer for triggering recalculations */
    protected Timer recalcTimer = null;

    protected double totalPlanTime;

    protected EVTaskList(String taskListName,
                         String displayName,
                         boolean willNeedChangeNotification) {
        super(null);

        this.taskListName = taskListName;

        if (willNeedChangeNotification) {
            recalcListeners = Collections.synchronizedSet(new HashSet());

            recalcTimer = new Timer(Integer.MAX_VALUE, this);
            recalcTimer.setInitialDelay(1000);
            recalcTimer.setRepeats(false);
        }

        if (displayName != null)
            root = new EVTask(taskListName);
    }

    public EVTaskList(String taskListName,
                      String displayName,
                      String errorMessage) {
        this(taskListName, null, false);
        createErrorRootNode(displayName, errorMessage);
    }


    protected void createErrorRootNode(String displayName,
                                       String errorMessage) {
        if (displayName == null || displayName.length() == 0)
            displayName = "Error";
        root = new EVTask(displayName);
        schedule = new EVSchedule(0.0);
        ((EVTask) root).setTaskError(errorMessage);
        schedule.getMetrics().addError(errorMessage, (EVTask) root);
    }


    public static String[] findTaskLists(DataRepository data) {
        return findTaskLists(data, false, false);
    }
    public static String[] findTaskLists(DataRepository data,
                                         boolean excludeRollups,
                                         boolean includeImports) {
        /*
            let findTaskLists return a full path to imported schedules.
            save that full path in rollup task list names.
            make the name pretty when displaying it to the user.

         */
        TreeSet result = new TreeSet();
        Iterator i = data.getKeys();
        String dataName, taskListName;
        while (i.hasNext()) {
            dataName = (String) i.next();

            // see if this data name defines a regular task list.
            taskListName =
                EVTaskListData.taskListNameFromDataElement(dataName);

            // if that failed, maybe see if this defines a rollup.
            if (taskListName == null && !excludeRollups)
                taskListName =
                    EVTaskListRollup.taskListNameFromDataElement(dataName);

            // if that failed, maybe see if this defines an imported list.
            if (taskListName == null && includeImports)
                taskListName =
                    EVTaskListXML.taskListNameFromDataElement(dataName);

            // if any of the tests succeeded, add the name to the list.
            if (taskListName != null)
                result.add(taskListName);
        }

        String[] ret = new String[result.size()];
        i = result.iterator();
        int j = 0;
        while (i.hasNext())
            ret[j++] = (String) i.next();
        return ret;
    }

    /** Finds and opens an existing task list.
     *  @param taskListName the symbolic name of the task list.
     *  @param data a reference to the data repository.
     *  @param hierarchy a reference to the user's hierarchy.
     *  @param willNeedChangeNotification should the returned task list
     *     support change notifications?
     *  @return the named task list, or null if no list by that name is found.
     */
    public static EVTaskList openExisting(String taskListName,
                                          DataRepository data,
                                          PSPProperties hierarchy,
                                          ObjectCache cache,
                                          boolean willNeedChangeNotification)
    {
        // most common case: open a regular task list
        if (EVTaskListData.validName(taskListName) &&
            EVTaskListData.exists(data, taskListName))
            return new EVTaskListData(taskListName, data, hierarchy,
                                      willNeedChangeNotification);

        // next most common case: open a rollup task list
        if (EVTaskListRollup.validName(taskListName) &&
            EVTaskListRollup.exists(data, taskListName))
            return new EVTaskListRollup(taskListName, data, hierarchy, cache);

        // open an cached imported XML task list.
        if (EVTaskListCached.validName(taskListName) &&
            EVTaskListCached.exists(taskListName, cache))
            return new EVTaskListCached(taskListName, cache);

        // open an imported XML task list
        if (EVTaskListXML.validName(taskListName) &&
            EVTaskListXML.exists(data, taskListName))
            return new EVTaskListXML(taskListName, data);

        // no task list was found.
        return null;
    }

    public static EVTaskList open(String taskListName,
                                  DataRepository data,
                                  PSPProperties hierarchy,
                                  ObjectCache cache,
                                  boolean willNeedChangeNotification)
    {
        EVTaskList result = openExisting
            (taskListName, data, hierarchy, cache, willNeedChangeNotification);

        if (result == null && EVTaskListData.validName(taskListName))
            result = new EVTaskListData
                (taskListName, data, hierarchy, willNeedChangeNotification);

        if (result == null && EVTaskListXML.validName(taskListName))
            result = new EVTaskListXML(taskListName, data);

        if (result == null)
            result = new EVTaskList(taskListName,
                                    getDisplayName(taskListName),
                                    "Invalid Schedule");

        return result;
    }


    public static String getDisplayName(String taskListName) {
        int slashPos = taskListName.lastIndexOf('/');
        if (slashPos == -1)
            return taskListName;
        else
            return taskListName.substring(slashPos+1);
    }

    public static String cleanupName(String taskListDataName) {
        // Strip all initial text up to and including the "main data prefix."
        int pos = taskListDataName.indexOf(MAIN_DATA_PREFIX);
        if (pos != -1)
            taskListDataName = taskListDataName.substring
                (pos + MAIN_DATA_PREFIX.length());

        // Strip all final text following the "/" character.
        pos = taskListDataName.indexOf('/');
        if (pos != -1)
            taskListDataName = taskListDataName.substring(0, pos);

        return taskListDataName;
    }

    public boolean isEmpty() { return ((EVTask) root).isLeaf(); }
    public boolean isEditable() { return false; }
    public String getRootName() { return ((EVTask) root).name; }


    public void save() { save(taskListName); }
    public void save(String newName) {}

    public String getAsXML() {
        StringBuffer result = new StringBuffer();
        result.append("<EVModel>");
        ((EVTask) root).saveToXML(result);
        schedule.saveToXML(result);
        result.append("</EVModel>");
        //System.out.print(result.toString());
        try {
            return new String(result.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) { // can't happen?
            return result.toString();
        }
    }

    public boolean addTask(String path,
                           DataRepository data,
                           PSPProperties hierarchy,
                           ObjectCache cache,
                           boolean willNeedChangeNotification) {
        if (path == null || path.length() == 0 || !isEditable()) return false;

        EVTask newTask = createAndAddTask(path, data, hierarchy, cache,
                                          willNeedChangeNotification);
        if (newTask == null)
            return false;

        // send the appropriate TreeModel event.
        int[] childIndices = new int[] { ((EVTask)root).getNumChildren() - 1 };
        Object[] children = new Object[] { newTask };
        fireTreeNodesInserted
            (this, ((EVTask) root).getPath(), childIndices, children);
        return true;
    }
    public EVTask createAndAddTask(String path,
                                   DataRepository data,
                                   PSPProperties hierarchy,
                                   ObjectCache cache,
                                   boolean willNeedChangeNotification) {
        return null;
    }

    public boolean removeTask(TreePath path) {
        if (!isEditable()) return false;

        // for now, only remove tasks which are children of the root.
        int pathLen = path.getPathCount();
        if (pathLen != 2) return false;

        EVTask parent = (EVTask) path.getPathComponent(pathLen-2);
        EVTask child  = (EVTask) path.getPathComponent(pathLen-1);
        int pos = parent.remove(child);
        finishRemovingTask(pos);
        child.destroy();


        // send the appropriate TreeModel event.
        int[] childIndices = new int[] { pos };
        Object[] children = new Object[] { child };
        fireTreeNodesRemoved
            (this, ((EVTask) parent).getPath(), childIndices, children);
        return true;
    }
    protected void finishRemovingTask(int pos) {}

    public boolean explodeTask(TreePath path) { return false; }

    public boolean moveTaskUp(int pos) {
        if (!isEditable()) return false;

        EVTask r = (EVTask) root;
        if (pos < 1 || pos >= r.getNumChildren()) return false;

        // make the change
        r.moveUp(pos);
        finishMovingTaskUp(pos);

        // send the appropriate TreeModel event.
        int[] childIndices = new int[] { pos-1, pos };
        Object[] children = new Object[]{ r.getChild(pos-1), r.getChild(pos) };
        fireTreeStructureChanged(this, r.getPath(), childIndices, children);
        return true;
    }
    protected void finishMovingTaskUp(int pos) {}


    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    /// Change notification support
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////


    /*
     * Pass along evNodeChanged events to up to one additional EVTask.Listener
     */

    EVTask.Listener evNodeListener = null;
    public void setNodeListener(EVTask.Listener l) { evNodeListener = l; }
    public void evNodeChanged(EVTask node) {
        if (evNodeListener != null) evNodeListener.evNodeChanged(node);
        if (recalcTimer    != null) recalcTimer.restart();
    }


    /** Defines the interface for an object that listens for recalculations
     *  that occur in an EVTaskList.
     *
     * Note: this only allows for notification of recalculation
     * events - not to the structure of the task list.  To be
     * notified of such changes, register as a TreeModelListener.
     */
    public interface RecalcListener {
        /** The task list has been recalculated. */
        public void evRecalculated(EventObject e);
    }
    Set recalcListeners = null;
    public synchronized void addRecalcListener(RecalcListener l) {
        if (recalcListeners != null)
            recalcListeners.add(l);
    }
    public void removeRecalcListener(RecalcListener l) {
        if (recalcListeners != null) {
            recalcListeners.remove(l);
            maybeDispose();
        }
    }
    protected boolean someoneCares() {
        return (recalcListeners != null && !recalcListeners.isEmpty());
    }
    protected void fireEvRecalculated() {
        if (someoneCares()) {
            EventObject e = new EventObject(this);
            Iterator i = recalcListeners.iterator();
            while (i.hasNext())
                ((RecalcListener) i.next()).evRecalculated(e);
        }
    }
    public void actionPerformed(ActionEvent e) {
        if (recalcTimer != null && e.getSource() == recalcTimer &&
            someoneCares())
            recalc();
    }

    private void maybeDispose() {
        if (recalcListeners == null) return;
        if (recalcListeners.isEmpty()) {
        }
    }
    protected void dispose() {
        //System.out.println("disposing!");
        ((EVTask) root).destroy();
    }

    public void finalize() throws Throwable {
        //System.out.println("finalizing EVTaskList " + taskListName);
        super.finalize();
    }


    public void recalc() {
        totalPlanTime = schedule.getMetrics().totalPlan();
        fireEvRecalculated();
    }

    public EVSchedule getSchedule() { return schedule; }



    //////////////////////////////////////////////////////////////////////
    /// TreeTableModel support
    //////////////////////////////////////////////////////////////////////


    /** Names of the columns in the TreeTableModel. */
    protected static String[] colNames = { "Project/Task", "PT", "Time",
        "PV", "CPT", "CPV", "Plan Date", "Date", "%C", "%S", "EV" };
    public static int[] colWidths =      { 175,             50,   50,
         40,   50,    40,    80,          80,     40,   40,   40 };
    public static String[] toolTips = {
        null,
        "Planned Time (hours:minutes)",
        "Actual Time (hours:minutes)",
        "Planned Value",
        "Cumulative Planned Time (hours:minutes)",
        "Cumulative Planned Value",
        "Planned Completion Date",
        "Actual Completion Date",
        "Percent Complete",
        "Percent Spent",
        "Actual Earned Value" };

    public static final int TASK_COLUMN           = 0;
    public static final int PLAN_TIME_COLUMN      = 1;
    public static final int ACT_TIME_COLUMN       = 2;
    public static final int PLAN_VALUE_COLUMN     = 3;
    public static final int PLAN_CUM_TIME_COLUMN  = 4;
    public static final int PLAN_CUM_VALUE_COLUMN = 5;
    public static final int PLAN_DATE_COLUMN      = 6;
    public static final int DATE_COMPLETE_COLUMN  = 7;
    public static final int PCT_COMPLETE_COLUMN   = 8;
    public static final int PCT_SPENT_COLUMN      = 9;
    public static final int VALUE_EARNED_COLUMN   = 10;

    /** Types of the columns in the TreeTableModel. */
    static protected Class[]  colTypes = {
        TreeTableModel.class,   // project/task
        String.class,           // planned time
        String.class,           // actual time
        String.class,           // planned value
        String.class,           // planned cumulative time
        String.class,           // planned cumulative value
        Date.class,             // planned date
        Date.class,             // date
        String.class,           // percent complete
        String.class,           // percent spent
        String.class };         // earned value


    //
    // The TreeModel interface
    //

    /** Returns the number of children of <code>node</code>. */
    public int getChildCount(Object node) {
        return ((EVTask) node).getNumChildren();
    }

    /** Returns the child of <code>node</code> at index <code>i</code>. */
    public Object getChild(Object node, int i) {
        return ((EVTask) node).getChild(i);
    }

    /** Returns true if the passed in object represents a leaf, false
     *  otherwise. */
    public boolean isLeaf(Object node) {
        return ((EVTask) node).isLeaf();
    }

    /** Returns true if the value in column <code>column</code> of object
     *  <code>node</code> is editable. */
    public boolean isCellEditable(Object node, int column) {
        if (node == null) return false;
        switch (column) {
        case TASK_COLUMN:
            // The column with the tree in it should be editable; this
            // causes the JTable to forward mouse and keyboard events
            // in the Tree column to the underlying JTree.
            return true;

        case PLAN_TIME_COLUMN:
            return ((EVTask) node).plannedTimeIsEditable();

        case DATE_COMPLETE_COLUMN:
            return ((EVTask) node).completionDateIsEditable();
        }
        return false;
    }


    //
    //  The TreeTableNode interface.
    //

    /** Returns the number of columns. */
    public int getColumnCount() { return colNames.length; }

    /** Returns the name for a particular column. */
    public String getColumnName(int column) { return colNames[column]; }

    /** Returns the class for the particular column. */
    public Class getColumnClass(int column) { return colTypes[column]; }

    /** Returns the value of the particular column. */
    public Object getValueAt(Object node, int column) {
        if (node == null) return null;
        EVTask n = (EVTask) node;
        switch (column) {
        case TASK_COLUMN:           return n.getName();
        case PLAN_TIME_COLUMN:      return n.getPlanTime();
        case ACT_TIME_COLUMN:       return n.getActualTime();
        case PLAN_VALUE_COLUMN:     return n.getPlanValue(totalPlanTime);
        case PLAN_CUM_TIME_COLUMN:  return n.getCumPlanTime();
        case PLAN_CUM_VALUE_COLUMN: return n.getCumPlanValue(totalPlanTime);
        case PLAN_DATE_COLUMN:      return n.getPlanDate();
        case DATE_COMPLETE_COLUMN:  return n.getActualDate();
        case PCT_COMPLETE_COLUMN:   return n.getPercentComplete();
        case PCT_SPENT_COLUMN:      return n.getPercentSpent();
        case VALUE_EARNED_COLUMN:   return n.getValueEarned(totalPlanTime);
        }
        return null;
    }

    /** Set the value at a particular row/column */
    public void setValueAt(Object aValue, Object node, int column) {
        //System.out.println("setValueAt("+aValue+","+node+","+column+")");
        if (node == null) return;
        EVTask n = (EVTask) node;
        switch (column) {
        case PLAN_TIME_COLUMN:      n.setPlanTime(aValue);   break;
        case DATE_COMPLETE_COLUMN:  n.setActualDate(aValue); break;
        }
    }

    /** If the given cell has an error, return it.  Otherwise return null */
    public String getErrorStringAt(Object node, int column) {
        if (node == null) return null;
        EVTask n = (EVTask) node;
        switch (column) {
        case TASK_COLUMN: return ((EVTask) node).getTaskError();
        case PLAN_TIME_COLUMN: return ((EVTask) node).getPlanTimeError();
        default: return null;
        }
    }

    public TableModel getSimpleTableModel() {
        return new SimpleTableModel();
    }

    private class SimpleTableModel implements TableModel {
        public int getRowCount() { return countRows((EVTask) root); }
        public int getColumnCount() {
            return EVTaskList.this.getColumnCount(); }
        public String getColumnName(int columnIndex) {
            return EVTaskList.this.getColumnName(columnIndex); }
        public Class getColumnClass(int columnIndex) {
            return EVTaskList.this.getColumnClass(columnIndex); }
        public boolean isCellEditable(int row, int columnIndex) {
            return EVTaskList.this.isCellEditable(getRow(row), columnIndex); }
        public Object getValueAt(int row, int columnIndex) {
            return (columnIndex == 0 ?
                    getRow(row).getFullName() :
                    EVTaskList.this.getValueAt(getRow(row), columnIndex)); }
        public void setValueAt(Object aValue, int row, int columnIndex) {
            EVTaskList.this.setValueAt(aValue, getRow(row), columnIndex); }
        public void addTableModelListener(TableModelListener l) { }
        public void removeTableModelListener(TableModelListener l) { }

        private int countRows(EVTask node) {
            int result = 0;
            if (node.isLeaf())
                result = 1;
            else
                for (int i = node.getNumChildren();   i-- > 0; )
                    result += countRows(node.getChild(i));
            return result;
        }

        private EVTask getRow(int row) {
            RowFinder f = new RowFinder((EVTask) root, row);
            return f.getResult();
        }

        private class RowFinder {
            int rowToReturn, rowsSeen = 0;
            EVTask result = null;
            public RowFinder(EVTask node, int rowToReturn) {
                this.rowToReturn = rowToReturn;
                find(node);
            }
            public EVTask getResult() { return result; }
            private void find(EVTask node) {
                if (result != null)
                    return;
                else if (node.isLeaf()) {
                    if (rowToReturn == rowsSeen) {
                        result = node;
                        return;
                    } else
                        rowsSeen++;
                } else {
                    for (int i = 0;  i < node.getNumChildren();  i++)
                        if (result == null)
                            find(node.getChild(i));
                }
            }
        }
    }


}

//  class ignored extends AbstractTreeTableModel
//      implements EVTask.Listener, ActionListener, PSPProperties.Listener
//  {

//      public static final String XML_SAVE_NAME = "XML Saved List";





//      /**
//       * @param taskListName the name of the task list. This will be one of:
//       *   for plain task lists: a simple string, not containing any '/'
//       *      characters (e.g. "My Tasks").
//       *
//       *   for XML task lists: a data name pointing to the data element in
//       *      the repository where the XML can be found. (e.g.
//       *      "/Imported/298243029/Task-Schedule/My Tasks").
//       *
//       *   for URL task lists: an encoded URL indicating where the XML task
//       *      list can be downloaded, and what password to use.  In the
//       *      URL, '/' chars have been replaced with '|'. Also, a trailing
//       *      '|!' and user credential may have been appended. (e.g.
//       *      "http:||host:2468|ev+|My Tasks||reports|ev.class?xml|Basic A1209C")
//       */
//      public EVTaskList(String taskListName,
//                        DataRepository data,
//                        PSPProperties hierarchy,
//                        boolean createRollup,
//                        boolean willNeedChangeNotification) {

//          if (openURL(data, taskListName)) return;
//          if (openXML(data, taskListName)) return;

//          root = new EVTask(taskListName);

//          if (isRollup(data, taskListName) ||
//              (createRollup && !isPlain(data, taskListName))) {
//          } else {
//          }
//      }



//      private boolean openURL(DataRepository data, String taskListName) {
//          if (!taskListName.startsWith("http:||")) return false;
//          String xmlDoc = null, error = null, url = taskListName;
//          String saveName =
//              data.createDataName("/" + taskListName, XML_SAVE_NAME);

//          // Retrieve the password, if it is present.
//          String credential = null;
//          int credentialPos = url.indexOf("|!");
//          if (credentialPos != -1) {
//              credential = url.substring(credentialPos + 2);
//              url = url.substring(0, credentialPos);
//          }

//          /*
//          // Retrieve the friendly name, if it is present.
//          String friendlyName = null;
//          int namePos = url.indexOf("|~");
//          if (namePos != -1) {
//              friendlyName = url.substring(namePos + 2);
//              url = url.substring(0, namePos);
//          }*/

//          // translate the taskListName into a URL.
//          url = url.replace('|', '/');
//          URL u = null;

//          // first try to connect to the given URL and fetch the XML doc.
//          try {
//              u = new URL(url);
//              URLConnection conn = u.openConnection();
//              if (credential != null)
//                  conn.setRequestProperty("Authorization", credential);
//              conn.connect();

//              // check for errors.
//              int status = ((HttpURLConnection) conn).getResponseCode();
//              if (status == 403)          // unauthorized?
//                  error = "You don't have the correct password to connect "+
//                      "to your coworker's schedule.";

//              else if (status == 404)     // no such schedule?
//                  error = "Your coworker doesn't have a schedule by this name. "+
//                      "(They may have renamed the schedule?)";

//              else if (status != 200)     // some other problem?
//                  error = "Couldn't retrieve this schedule from your " +
//                      "coworker's computer.";

//              // retrieve the xml document and use it to create the task list.
//              if (error == null) {
//                  xmlDoc = new String
//                      (TinyWebServer.slurpContents(conn.getInputStream(), true));
//                  if (openXML(xmlDoc, null)) {
//                      data.putValue(saveName, StringData.create(xmlDoc));
//                      return true;
//                  }
//              }

//          } catch (MalformedURLException mue) {
//              error = "The url '" + url + "' is malformed.";
//          } catch (UnknownHostException uhe) {
//              error = "Couldn't find your coworker's computer" +
//                  (u == null ? "." : ", '" + u.getHost() + "'.");
//          } catch (ConnectException ce) {
//              error = "Couldn't connect to your coworker's computer. " +
//                  "(They may not have their dashboard running?)";
//          } catch (IOException ioe) {
//              error = "There was a problem connecting to your " +
//                  "coworker's computer.";
//          }

//          // if that fails, look for a cached value of the XML in the data
//          // repository
//          SimpleData value = data.getSimpleValue(saveName);
//          if (value instanceof StringData) {
//              xmlDoc = value.format();
//              if (openXML(xmlDoc, null)) return true;
//              // fixme - possibly check the effective date of the cached
//              // schedule, and display a warning if it is really old?
//          }

//          // if that fails, create an "invalid task list".
//          return false;
//      }

//  }
