// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.hier;


import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.ui.HierarchyEditor;

/** General purpose class for making <b>simple</b> changes to the hierarchy.
 */
public class HierarchyAlterer implements ItemListener {

    private ProcessDashboard dashboard;

    public HierarchyAlterer(ProcessDashboard dashboard) {
        this.dashboard = dashboard;
    }

    public class HierarchyAlterationException extends Exception {
        public HierarchyAlterationException(String reason) { super(reason); }
    }

    private void beginChanges() throws HierarchyAlterationException {
        if (dashboard.isHierarchyEditorOpen())
            throw new HierarchyAlterationException
                ("The hierarchy editor is currently open.");

        dashboard.getData().startInconsistency();
        dashboard.getHierarchy().addItemListener(this);

        // if we implement "rename node" in the future, we will need
        // to do a lot more here...like making a copy of the
        // properties, ensuring the time log editor isn't open,
        // reading in the time log, etc.
    }

    private void endChanges() {
        dashboard.getHierarchy().removeItemListener(this);
        dashboard.getData().finishInconsistency();
        dashboard.getHierarchy().fireHierarchyChanged();
        updateNodesAndLeaves(dashboard.getData(), dashboard.getHierarchy());
    }



    /** Create a plain hierarchy node at the specified path.
     *
     * Missing parent nodes will be created automatically.
     *
     * WARNING: no checks are performed to prevent nodes from being
     * illegally created. (e.g. adding new phases underneath a
     * structured process)
     */
    public void addNode(ProcessDashboard dash, String path)
        throws HierarchyAlterationException
    {
        beginChanges();
        try {
            doAddNode(dash, path);
        } finally {
            endChanges();
        }
    }

    private PropertyKey doAddNode(ProcessDashboard dash, String path) {
        StringTokenizer tok = new StringTokenizer(path, "/");
        PropertyKey key = PropertyKey.ROOT;
        while (tok.hasMoreTokens())
            key = maybeMakeNode(dash.getHierarchy(), key, tok.nextToken());
        return key;
    }

    private PropertyKey maybeMakeNode(DashHierarchy props,
                                      PropertyKey parent,
                                      String childName) {
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
    public void addTemplate(String path, String templateID)
        throws HierarchyAlterationException
    {
        DashHierarchy templates = dashboard.getTemplateProperties();
        PropertyKey templateKey = templates.getByID(templateID);
        if (templateKey == null)
            throw new HierarchyAlterationException("No such template");

        beginChanges();
        try {
            PropertyKey newNode = doAddNode(dashboard, path);
            dashboard.getHierarchy().copyFrom (templates, templateKey, newNode);
        } finally {
            endChanges();
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
    public void deleteNode(String path) throws HierarchyAlterationException {
        if (path == null || path.length() < 2)
            throw new HierarchyAlterationException("Illegal to delete node");

        beginChanges();
        try {
            doDeleteNode(dashboard, path);
        } finally {
            endChanges();
        }
    }

    private void doDeleteNode(ProcessDashboard dash, String path) {
        DashHierarchy props = dash.getHierarchy();
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

    /* Not yet implemented.
    public void renameNode(ProcessDashboard dash, String oldPath, String newPath)
        throws HierarchyAlterationException
    {
        PSPProperties props = dash.props;
        props.addItemListener(this);

        oldProps = new PSPProperties(props.dataPath);
        oldProps.copy(props);

    }
    */



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

        if (p.srcFile == null)
            HierarchyEditor.createEmptyFile(dataDir + p.destFile);
        else
            HierarchyEditor.createDataFile(dataDir + p.destFile, p.srcFile);

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
        throw new UnsupportedOperationException();
    }

    /** Ensure that "leaf" and "node" data elements are properly set.
     */
    public static void updateNodesAndLeaves(DataRepository data,
                                            DashHierarchy useProps) {
        HashSet nodesAndLeaves = new HashSet();
        readNodesAndLeaves(nodesAndLeaves, data);
        setNodesAndLeaves(nodesAndLeaves, data, useProps, PropertyKey.ROOT);
        clearNodesAndLeaves(nodesAndLeaves, data);
    }

    public static final String NODE_TAG = "/node";
    public static final String LEAF_TAG = "/leaf";


    /** Make a list of all the "leaf" and "node" data elements in the
     * repository.
     */
    private static void readNodesAndLeaves(HashSet nodesAndLeaves,
                                           DataRepository data) {
        Iterator dataNames = data.getKeys();
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


}
