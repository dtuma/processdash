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
import javax.swing.*;
import java.util.*;
import pspdash.data.DateData;

public class HierarchyButton implements ActionListener {

    PSPDashboard parent = null;
    JMenu menu = null;
    HierarchyButton child = null;
    PropertyKey self = null;
    int numChildren = 0;

    private void debug(String msg) {
        // System.out.println(msg);
    }


    HierarchyButton(PSPDashboard dash, PropertyKey useSelf) {
        super();
        parent = dash;
        self = useSelf;
        //if (self != null)
            //debug (self.toString());

        String s;
        PSPProperties props = parent.getProperties ();

        //debug("Getting numChildren:"+self);
        numChildren = props.getNumChildren (self);
        //debug("numChildren:"+numChildren);

        if (numChildren != 0) {

            //debug("Processing children:"+numChildren);

            menu = new JMenu();
            PCSH.enableHelpKey(parent.hierarchy_menubar, "HierarchyMenus");
            //PCSH.enableHelpKey(menu, "HierarchyMenus");

            parent.hierarchy_menubar.add(menu);
            parent.hierarchy_menubar.invalidate();

            int i;
            for (i=0; i<numChildren; i++) {
                //debug("Getting child " + i + " of "+numChildren);
                PropertyKey key = props.getChildKey (self, i);
                //debug(" key:"+key.key()+": = "+props.getChildName (self, i));
                menu.add(new MyMenuItem(key.name()));
            }

            selectChild(props.getChildName(self, props.getSelectedChild(self)));

        } else {
            parent.setCurrentPhase(self);
            parent.validate();
            parent.pack();
        }

    }



    // Destroy the entire hierarchy, but ensure the data for the current phase
    // has been saved.
    protected void terminate() {

        // save any data from the current phase
        parent.setCurrentPhase(self);

        // destroy the hierarchy
        delete();
    }



    protected void delete() {
        if (child != null) {
            child.delete();
            child = null;
            parent.hierarchy_menubar.remove(menu);
            menu = null;
        }
    }

    public boolean setPath(String path) {
        if (path == null || path.length() == 0) return true;

        // remove the / from the beginning, if it is present.
        if (path.charAt(0) == '/')
            path = path.substring(1);

        String childName, rest;
        int slashPos = path.indexOf('/');

        if (slashPos == -1) {
            childName = path;
            rest = null;
        } else {
            childName = path.substring(0, slashPos);
            rest = path.substring(slashPos+1);
        }

        if (selectChild(childName))
            return child.setPath(rest);
        else
            return false;
    }

    public boolean setPhase(String phase) {
        if (phase == null || phase.length() == 0) return true;

        // If this is the terminal HierarchyButton without a menu, we
        // cannot set the phase.  return false.
        if (child == null) return false;

        // If our child has children, unconditionally delegate this task.
        if (child.numChildren != 0)
            return child.setPhase(phase);

        else
            // our child has no children.  We must be the HierarchyButton
            // with the rightmost visible menu.
            return selectChild(phase);
    }


    /** Select the child with the given name.
     *
     * @return true if the selection was successful, false if no child
     * with the given name exists.
     */
    boolean selectChild(String name) {
        //debug("selectChild.name = " + self + name);

        PSPProperties props = parent.getProperties ();
        int i;
        int sel = 0;
        for (i=0; i<numChildren; i++)
            if (name.equals(props.getChildName (self, i))) {
                sel = i;
                break;
            }

        if (i == numChildren) // didn't find the named child?
            return false;

        if ((child != null) && (props.getSelectedChild (self) == sel))
            return true;

        //debug("deleting old child");
        if (child != null) child.delete();

        props.setSelectedChild (self, sel);
        menu.setText(name);
        menu.invalidate();

        //debug("creating new child"+sel);
        child = new HierarchyButton (parent, new PropertyKey (self, name));
        return true;
    }

    public void workPerformed(DateData d) {
          // calculate the name of the Start Date data element for our path.
        String dataName = parent.data.createDataName(self.path(), "Started");

            // if our start date has not already been set, set it to d
        if (parent.data.getValue(dataName) == null)
            parent.data.putValue(dataName, d);

                // pass info down to child, as well.
        if (child != null) child.workPerformed(d);
    }


    private void markCompleted() {
          // mark this phase as completed in the data repository.
        String dataName = parent.data.createDataName(self.path(), "Completed");
        parent.data.putValue(dataName, new DateData());
    }



    public boolean selectNext() {

          // first, try to delegate this task to our child.  If the child is
          // able to perform a selectNext, we're done.
        if ((child != null) && child.selectNext()) return true;

            // if this is the terminal HierarchyButton without a menu, we
            // cannot perform selectNext.  Return false.
        if (child == null) { markCompleted(); return false; }

            // calculate the number position of the next item to be selected.
        PSPProperties props = parent.getProperties ();
        int sel = props.getSelectedChild (self) + 1;

            // if that item is past the end of our list, we cannot perform
            // selectNext. Return false.
        if (sel == numChildren) { markCompleted(); return false; }

            // select the next item on our menu.
        selectChild(props.getChildName (self, sel));

        // Here's a philosphical question concerning the situation When our
        // child "rolls over" - i.e., our child already had his last item
        // selected, and thus wants us to increment our menu instead.  We just
        // incremented our menu with the line above.  Do we now want to force
        // our new child to select its first element?  That would make sense in
        // a mathematical sense.  In practice, however, I think it isn't the
        // right thing to do.  People don't usually follow processes in
        // flowchart order - instead, they may need to bounce around a bit.  For
        // example, if the user just completed a cycle of an SCR/PSP3 process,
        // move them into the next cycle, but don't require them to start that
        // cycle from the beginning if they have already performed some work
        // there.  Similarly, if the user just completed an entire project, move
        // the menu to the next project, but don't start that project over from
        // the beginning if they have already performed work there.
        //
        // So, with the above rationale, the following line is commented out.
        // If someone feels the mathematical behavior would be better, uncomment
        // the next line, as well as the definition of the procedure
        // selectFirst, below.
        //
        // child.selectFirst();

        return true;
    }



//     public void selectFirst() {
//       if (child != null) {
//      selectChild(parent.getProperties().getChildName(self, 0));
//      child.selectFirst();
//       }
//     }



    public void actionPerformed(ActionEvent e) {
        selectChild( ((JMenuItem)e.getSource()).getText() );
    }


    class MyMenuItem extends JMenuItem {
        MyMenuItem(String text) {
            super(text);
            addActionListener(HierarchyButton.this);
        }
    }

}
