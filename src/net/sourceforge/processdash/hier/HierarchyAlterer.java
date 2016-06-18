// Copyright (C) 2002-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Timer;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.data.repository.DataNameFilter;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.ui.HierarchyEditor;
import net.sourceforge.processdash.log.time.ModifiableTimeLog;
import net.sourceforge.processdash.log.time.PathRenamer;
import net.sourceforge.processdash.log.ui.DefectLogEditor;
import net.sourceforge.processdash.util.ComparableValue;
import net.sourceforge.processdash.util.ThreadThrottler;

/** General purpose class for making <b>simple</b> changes to the hierarchy.
 */
public class HierarchyAlterer implements ItemListener {

    private ProcessDashboard dashboard;
    private DashHierarchy origHierarchy;
    private HierarchyEventDispatcher eventDispatcher;

    private static final Logger logger = Logger
            .getLogger(HierarchyAlterer.class.getName());

    public HierarchyAlterer(ProcessDashboard dashboard) {
        this.dashboard = dashboard;
        this.eventDispatcher = new HierarchyEventDispatcher();
    }

    public class HierarchyAlterationException extends Exception {
        public HierarchyAlterationException(String reason) { super(reason); }
    }

    private void beginChanges() throws HierarchyAlterationException {
        if (dashboard.isHierarchyEditorOpen())
            throw new HierarchyAlterationException
                ("The hierarchy editor is currently open.");
        if (Settings.isReadOnly() && !Settings.isFollowMode())
            throw new HierarchyAlterationException
                ("The dashboard is running in read-only mode.");

        eventDispatcher.beginChanges();
        dashboard.getData().startInconsistency();

        DashHierarchy hier = dashboard.getHierarchy();
        hier.addItemListener(this);
        origHierarchy = new DashHierarchy(hier.dataPath);
        origHierarchy.copy(hier);
    }

    private void endChanges() {
        origHierarchy = null;
        dashboard.getHierarchy().removeItemListener(this);
        updateNodesAndLeaves(dashboard.getData(), dashboard.getHierarchy());
        dashboard.getData().finishInconsistency(true);
        eventDispatcher.endChanges();
        ThreadThrottler.tick();
    }


    /** Create a plain hierarchy node at the specified path.
     *
     * Missing parent nodes will be created automatically.
     *
     * WARNING: no checks are performed to prevent nodes from being
     * illegally created. (e.g. adding new phases underneath a
     * structured process)
     * @throws HierarchyAlterationException
     */
    public synchronized void addNode(String path)
            throws HierarchyAlterationException {
        addNode(dashboard, path);
    }

    /** @deprecated */
    public synchronized void addNode(ProcessDashboard dash, String path)
        throws HierarchyAlterationException
    {
        beginChanges();
        try {
            logger.log(Level.FINE, "addNode({0})", path);
            doAddNode(dash.getHierarchy(), path);
        } finally {
            endChanges();
        }
    }

    public static PropertyKey doAddNode(DashHierarchy hier, String path) {
        if (path == null || path.length() == 0)
            return PropertyKey.ROOT;
        StringTokenizer tok = new StringTokenizer(path, "/");
        PropertyKey key = PropertyKey.ROOT;
        while (tok.hasMoreTokens())
            key = maybeMakeNode(hier, key, tok.nextToken());
        return key;
    }

    private static PropertyKey maybeMakeNode(DashHierarchy props,
            PropertyKey parent, String childName) {
        PropertyKey child;
        Prop val = props.pget(parent);
        for (int i = val.getNumChildren();   i-- > 0; ) {
            child = val.getChild(i);
            if (child.name().equals(childName))
                return child;
        }

        props.addChildKey(parent, childName, -1);
        return new PropertyKey (parent, childName);
    }


    /** Instantiate a particular hierarchy template at the specified path.
     *
     * Missing parent nodes will be created automatically.
     *
     * WARNING: hierarchy constraints are ignored by this
     * method. Thus, no checks are performed to prevent templates from
     * being illegally created (e.g. adding a template underneath a
     * structured process).  In addition, template insertion positions
     * will not be respected.
     *
     * WARNING: if something already exists at the given path, it will
     * be clobbered by the copied template!
     */
    public synchronized void addTemplate(String path, String templateID)
        throws HierarchyAlterationException
    {
        DashHierarchy templates = dashboard.getTemplateProperties();
        PropertyKey templateKey = templates.getByID(templateID);
        if (templateKey == null && templateID.startsWith("/"))
            templateKey = templates.findExistingKey(templateID);
        if (templateKey == null)
            throw new HierarchyAlterationException("Could not find any"
                    + " template called '" + templateID
                    + "' to add to path '" + path + "'");

        beginChanges();
        try {
            logger.log(Level.FINE, "addTemplate({0} => {1})", new Object[] {
                    path, templateID });
            PropertyKey newNode = doAddNode(dashboard.getHierarchy(), path);
            dashboard.getHierarchy().copyFrom (templates, templateKey, newNode);
        } finally {
            endChanges();
        }
    }

    /** Changes the template ID of a node in the hierarchy.
     *
     * Currently does not alter anything else about the node.  The caller
     * should make certain that the existing template ID and the new template
     * ID are compatible with each other (same children, same datafile, same
     * defect logging enablement, etc.)
     *
     * @param nodePath the path to an existing node in the hierarchy
     * @param templateID the new template ID to assign to the node
     * @throws HierarchyAlterationException
     */
    public synchronized void setTemplateId(String nodePath, String templateID)
            throws HierarchyAlterationException {
        DashHierarchy templates = dashboard.getTemplateProperties();
        PropertyKey templateKey = templates.getByID(templateID);
        if (templateKey == null)
            throw new HierarchyAlterationException("Could not find any"
                        + " template called '" + templateID
                        + "' to add to path '" + nodePath + "'");
        Prop p = templates.pget(templateKey);
        String constraints = p.getStatus();

        beginChanges();
        try {
            logger.log(Level.FINE, "setTemplateId({0} => {1})", new Object[] {
                    nodePath, templateID});
            doSetTemplateId(dashboard.getHierarchy(), nodePath, templateID,
                    constraints);
        } finally {
            endChanges();
        }
    }

    public static void doSetTemplateId(DashHierarchy hier, String nodePath,
            String templateID, String constraints) {
        PropertyKey node = hier.findExistingKey(nodePath);
        if (node != null) {
            Prop p = hier.pget(node);
            p.setID(templateID);
            p.setStatus(constraints);
        }
    }


    /** Delete the given node, and all its children, from the hierarchy.
     *
     * WARNING: hierarchy constraints are ignored by this method.
     * Thus, no checks are performed to prevent templates from being
     * illegally deleted (e.g. deleting a read only phase underneath a
     * structured process).  However, this <b>will</b> refuse to
     * delete the root node of the hierarchy.
     */
    public synchronized void deleteNode(String path)
            throws HierarchyAlterationException {
        if (path == null || path.length() < 2)
            throw new HierarchyAlterationException("Illegal to delete node");

        beginChanges();
        try {
            logger.log(Level.FINE, "deleteNode({0})", path);
            doDeleteNode(dashboard.getHierarchy(), path);
        } finally {
            endChanges();
        }
    }

    public static void doDeleteNode(DashHierarchy props, String path) {
        PropertyKey node = props.findExistingKey(path);
        if (node == null) return;

        PropertyKey parent = node.getParent();
        if (parent == null) return;

        Prop val = props.pget(parent);
        for (int i = val.getNumChildren();   i-- > 0; )
            if (val.getChild(i) == node) {
                props.removeChildKey(parent, i);
                break;
            }
    }


    /** Move and/or rename a node within the hierarchy.
     *
     * Missing parent nodes at newPath will be created automatically.
     *
     * WARNING: hierarchy constraints are ignored by this
     * method. Thus, no checks are performed to prevent templates from
     * being illegally moved/renamed (e.g. adding a template underneath a
     * structured process, or renaming a phase in a structured process).
     * In addition, template insertion positions will not be respected.
     *
     * WARNING: if something already exists at the given path, it will
     * be clobbered by the moved node!
     */
    public synchronized void renameNode(String oldPath, String newPath)
        throws HierarchyAlterationException
    {
        if (oldPath == null || oldPath.length() < 2
                || newPath == null || newPath.length() < 2)
            throw new HierarchyAlterationException("Illegal to rename node");

        beginChanges();
        try {
            logger.log(Level.FINE, "renameNode({0} => {1})", new Object[] {
                    oldPath, newPath});
            doRenameNode(dashboard.getHierarchy(), oldPath, newPath);
        } finally {
            endChanges();
        }
    }

    public static void doRenameNode(DashHierarchy hier, String oldPath,
            String newPath) {
        // find the node to move, abort if it doesn't exist.
        PropertyKey oldNode = hier.findExistingKey(oldPath);
        if (oldNode == null) return;

        // add the new node to the hierarchy.
        PropertyKey newNode = doAddNode(hier, newPath);
        // If the node hasn't changed parents (we are just renaming it in
        // place), position the new node immediately before the existing one.
        PropertyKey parent = newNode.getParent();
        Prop parentProp = hier.pget(parent);
        int oldPos = parentProp.getChildPos(oldNode);
        int newPos = parentProp.getChildPos(newNode);
        if (oldPos != -1 && newPos != -1) {
            parentProp.removeChild(newPos);
            parentProp.addChild(newNode, oldPos);
        }
        // move data and children from the old location to the new
        hier.move(oldNode, newNode);
        // delete the old node
        doDeleteNode(hier, oldPath);
    }


    /** Rearrange the children of the given parent, so they appear in an
     * order most closely matching the given list.
     *
     * If the parent has children whose names do not appear in the given list,
     * their order (relative to their recognized siblings) will be disturbed
     * as little as possible.
     *
     * @param parentPath the path to the parent node whose children should
     *     be reordered.  If no such parent exists, nothing will be done.
     * @param childNames a list of child names, in the order they should
     *     appear after the reordering
     * @throws HierarchyAlterationException
     */
    public synchronized boolean reorderChildren(String parentPath,
            List childNames) throws HierarchyAlterationException {
        // run the reorder in "what-if" mode.  If no change is needed, do
        // nothing and return false.
        if (doReorderChildren(dashboard.getHierarchy(), parentPath, childNames,
                true) == false)
            return false;

        beginChanges();
        try {
            logger.log(Level.FINE, "reorderChildren({0})", parentPath);
            return doReorderChildren(dashboard.getHierarchy(), parentPath,
                    childNames, false);
        } finally {
            endChanges();
        }
    }

    public static boolean doReorderChildren(DashHierarchy hier, String parentPath,
            List childNames) {
        return doReorderChildren(hier, parentPath, childNames, false);
    }

    private static boolean doReorderChildren(DashHierarchy hier,
            String parentPath, List childNames, boolean whatIf) {
        PropertyKey parent = hier.findExistingKey(parentPath);
        if (parent == null) return false;
        Prop prop = hier.pget(parent);

        int numChildren = prop.getNumChildren();
        if (numChildren == 0) return false;

        ComparableValue[] children = new ComparableValue[numChildren];
        int lastOrdinal = -1;
        Object selectedChild = null;
        for (int i = 0; i < numChildren; i++) {
            PropertyKey childKey = prop.getChild(i);
            String childName = childKey.name();
            int ordinal = childNames.indexOf(childName);
            if (ordinal == -1)
                // not found? inherit the ordinal of our previous sibling.
                ordinal = lastOrdinal;

            children[i] = new ComparableValue(childKey, ordinal);
            if (i == prop.getSelectedChild())
                selectedChild = children[i];

            lastOrdinal = ordinal;
        }

        Arrays.sort(children);

        boolean madeChange = false;
        for (int i = 0; i < children.length; i++) {
            PropertyKey child = (PropertyKey) children[i].getValue();
            if (prop.getChild(i) != child) {
                if (whatIf == false)
                    prop.setChild(child, i);
                madeChange = true;
            }
            if (children[i] == selectedChild && whatIf == false)
                prop.setSelectedChild(i);
        }
        return madeChange;
    }


    /** Make any alterations necessary to make the current hierarchy
     * match the hierarchy passed in to this method.
     *
     * @param src the new hierarchy whose contents we should copy
     * @throws HierarchyAlterationException
     */
    public void mergeChangesFrom(DashHierarchy src)
            throws HierarchyAlterationException {
        beginChanges();
        try {
            logger.log(Level.FINE, "mergeChanges");
            dashboard.getHierarchy().mergeChangesFrom(src);
        } finally {
            endChanges();
        }
    }



    public void itemStateChanged(ItemEvent e) {
        if (e.getItem() instanceof PendingDataChange)
            processDataChange ((PendingDataChange) e.getItem());
    }

    protected void processDataChange(PendingDataChange p) {
        switch (p.changeType) {
        case PendingDataChange.CREATE: createDataFile(p); break;
        case PendingDataChange.DELETE: deleteDataFile(p); break;
        case PendingDataChange.CHANGE: renameData(p); break;
        }
    }


    /** Create a data file for a newly created node.
     */
    protected void createDataFile(PendingDataChange p) {
        String dataDir = dashboard.getDirectory();

        if (DashHierarchy.EXISTING_DATAFILE.equals(p.srcFile))
            ;
        else if (p.srcFile == null)
            HierarchyEditor.createEmptyFile(dataDir + p.destFile);
        else
            HierarchyEditor.createDataFile(dataDir + p.destFile, p.srcFile,
                    p.extraData);

        if (p.newPrefix != null)
            dashboard.openDatafile (p.newPrefix, p.destFile);
    }


    /** Close a data file for a node that has been deleted.
     */
    protected void deleteDataFile(PendingDataChange p) {
        dashboard.getData().closeDatafile (p.oldPrefix);
    }


    /** Rename some data.
     */
    protected void renameData(PendingDataChange p) {
        DashHierarchy newHierarchy = dashboard.getHierarchy();
        DefectLogEditor.rename(origHierarchy, newHierarchy, p.oldPrefix,
                p.newPrefix, dashboard);
        ModifiableTimeLog timeLog = (ModifiableTimeLog) dashboard.getTimeLog();
        timeLog.addModification(PathRenamer.getRenameModification(p.oldPrefix,
                p.newPrefix));
        dashboard.getData().renameData(p.oldPrefix, p.newPrefix);
    }


    /** Ensure that "leaf" and "node" data elements are properly set.
     */
    public static void updateNodesAndLeaves(DataRepository data,
                                            DashHierarchy useProps) {
        HashSet nodesAndLeaves = new HashSet();
        readNodesAndLeaves(nodesAndLeaves, data);
        setNodesAndLeaves(nodesAndLeaves, data, useProps, PropertyKey.ROOT);
        clearNodesAndLeaves(nodesAndLeaves, data);
        useProps.assignMissingNodeIDs();
    }

    public static final String NODE_TAG = "/node";
    public static final String NODE_TAG2 = NODE_TAG.substring(1);
    public static final String LEAF_TAG = "/leaf";
    public static final String LEAF_TAG2 = LEAF_TAG.substring(1);


    /** Make a list of all the "leaf" and "node" data elements in the
     * repository.
     */
    private static void readNodesAndLeaves(HashSet nodesAndLeaves,
                                           DataRepository data) {
        Iterator dataNames = data.getKeys(null, DATA_NAME_HINT);
        String name;

        while (dataNames.hasNext()) {
            name = (String) dataNames.next();
            if ((name.endsWith(NODE_TAG) || name.endsWith(LEAF_TAG)) &&
                data.getValue(name) instanceof TagData)
                nodesAndLeaves.add(name);
        }
        /* "Pretend" that the data item "/node" is set.  We never want
         * that element to be set; claiming that it IS set will
         * effectively prevent setNodesAndLeaves() from setting it.
         */
        nodesAndLeaves.add(NODE_TAG);
    }

    /** Create appropriate "leaf" and "node" data elements if they are
     * missing.
     */
    private static void setNodesAndLeaves(HashSet nodesAndLeaves,
                                          DataRepository data,
                                          DashHierarchy useProps,
                                          PropertyKey key) {
        String path = key.path();
        String name;

        // set the "node" tag on every node
        name = path + NODE_TAG;
        if (nodesAndLeaves.remove(name) == false)
            data.putValue(name, TagData.getInstance());

        // set or unset the leaf tag, if applicable.
        int numChildren = useProps.getNumChildren(key);
        name = path + LEAF_TAG;
        if (numChildren == 0) {
            if (nodesAndLeaves.remove(name) == false)
                data.putValue(name, TagData.getInstance());
        } else {
            while (numChildren-- > 0)
                                // recursively process all children.
                setNodesAndLeaves(nodesAndLeaves, data, useProps,
                                  useProps.getChildKey(key, numChildren));
        }
    }

    /** Delete "leaf" and "node" data elements for nodes that no
     * longer exist.
     */
    private static void clearNodesAndLeaves(HashSet nodesAndLeaves,
                                            DataRepository data) {
        Iterator i = nodesAndLeaves.iterator();
        while (i.hasNext())
            data.putValue((String) i.next(), null);
    }


    private static class NodeAndLeafDataNameHint implements
            DataNameFilter.PrefixLocal {
        public boolean acceptPrefixLocalName(String prefix, String localName) {
            return localName.endsWith(NODE_TAG2)
                    || localName.endsWith(LEAF_TAG2);
        }
    }
    private static final Object DATA_NAME_HINT = new NodeAndLeafDataNameHint();

    /** Number of milliseconds to delay before delivering a hierarchy changed
     * notification event - nonzero to prevent unnecessary over-notification
     * during a period of heavy hierarchy alteration */
    private static final int EVENT_DISPATCH_DELAY = 300;

    /** Max number of alterations to make before deciding to unequivocally
     * send a hierarchy changed event (regardless of event dispatch delay
     * considerations) */
    private static final int EVENT_COUNT_THRESHHOLD = 30;


    /** This class manages the dispatch of "hierarchy changed" events.  A
     * separate object is needed to avoid deadlock between these synchronized
     * methods and the synchronized methods in the HierarchyAlterer class.
     */
    private class HierarchyEventDispatcher implements ActionListener {
        private boolean changesAreUnderway;
        private boolean eventIsNeeded;
        private int eventDispatchChangeCount;
        private Timer eventDispatchTimer;

        public HierarchyEventDispatcher() {
            changesAreUnderway = eventIsNeeded = false;
            eventDispatchChangeCount = 0;

            eventDispatchTimer = new Timer(EVENT_DISPATCH_DELAY, this);
            eventDispatchTimer.setRepeats(false);
        }

        protected void finalize() throws Throwable {
            eventDispatchTimer.removeActionListener(this);
            super.finalize();
        }

        public synchronized void beginChanges() {
            changesAreUnderway = true;
        }

        public synchronized void endChanges() {
            changesAreUnderway = false;

            if (++eventDispatchChangeCount >= EVENT_COUNT_THRESHHOLD
                    || eventIsNeeded) {
                eventDispatchTimer.stop();
                dispatchHierarchyChangedEvent(true);
            }

            eventDispatchTimer.restart();
        }

        public synchronized void actionPerformed(ActionEvent e) {
            if (changesAreUnderway)
                eventIsNeeded = true;
            else
                dispatchHierarchyChangedEvent(false);
        }

        private void dispatchHierarchyChangedEvent(boolean isAdjusting) {
            eventDispatchChangeCount = 0;
            eventIsNeeded = false;
            dashboard.getHierarchy().fireHierarchyChanged(isAdjusting);
        }
    }
}
