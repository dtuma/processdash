/**
 *     Caption: Zaval Java Resource Editor
 *     $Revision$
 *     $Date$
 *
 *     @author:     Victor Krapivin
 *     @version:    1.3
 *
 * Zaval JRC Editor is a visual editor which allows you to manipulate 
 * localization strings for all Java based software with appropriate 
 * support embedded.
 * 
 * For more info on this product read Zaval Java Resource Editor User's Guide
 * (It comes within this package).
 * The latest product version is always available from the product's homepage:
 * http://www.zaval.org/products/jrc-editor/
 * and from the SourceForge:
 * http://sourceforge.net/projects/zaval0002/
 *
 * Contacts:
 *   Support : support@zaval.org
 *   Change Requests : change-request@zaval.org
 *   Feedback : feedback@zaval.org
 *   Other : info@zaval.org
 * 
 * Copyright (C) 2001-2002  Zaval Creative Engineering Group (http://www.zaval.org)
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * (version 2) as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */
package org.zaval.awt;

import org.zaval.awt.*;
import java.util.*;
import org.zaval.awt.peer.*;

// ========================================================================
//  08/08/97    Added function CAF
// ========================================================================
//
//    public TreeNode getNode      (String name)
//    public boolean  insertChild  (String name, String addname)
//    public boolean  insertChild  (String name, String addname, String im1, String im2)
//    public boolean  insertNext   (String name, String addname)
//    public boolean  insertNext   (String name, String addname, String im1, String im2)
//    public boolean  insertRoot   (String addname)
//    public boolean  insertRoot   (String addname, String im1, String im2)
//    public boolean  setImages    (String name, String img1, String img2)
//    public boolean  setImageOpen (String name, String img)
//    public boolean  setImageClose(String name, String img)
//    public boolean  changeText   (String name, String newname)
//
// ========================================================================
// ========================================================================

public class LevelTree
{
    // constants for insertion
    public static final int CHILD   = 0;
    public static final int NEXT    = CHILD + 1;
    public static final int LAST    = CHILD + 2;


    public  Vector e = new Vector();    // e is vector of existing nodes
    public  Vector v = new Vector();    // v is vector of viewable nodes
    private TreeNode rootNode;          // root node of tree
    private ImageResolver imgres;       // To autosetup

    private int count=0;    // Number of nodes in the tree
    private int viewCount=0;// Number of viewable nodes in the tree
                            // (A node is viewable if all of its
                            // parents are expanded.)


    private String treeStructure[];
    String         delim = ".";

    private String selectedNode;

    // constructors
    public LevelTree()
    {
       count=0;
    }


    public LevelTree(TreeNode head)
    {
        this();
        rootNode = head;
        setResolver(rootNode,imgres);
        count=1;
    }


    public String  getNamesDelim()
    {
     return delim;
    }


    public int getViewCount()
    {
      return viewCount;
    }

    // Insert a new node relative to a node in the tree.
    // position = CHILD inserts the new node as a child of the node
    // position = NEXT inserts the new node as the next sibling
    // position = PREVIOUS inserts the new node as the previous sibling
    public void insert(TreeNode newNode, TreeNode relativeNode, int position)
    {
        if (newNode==null || relativeNode==null)
        {
            return;
        }
        if (exists(relativeNode)==false)
        {
            return;
        }
        switch (position)
        {
            case CHILD:
                addChild(newNode, relativeNode);
                break;

            case NEXT:
                addSibling(newNode, relativeNode);
                break;

            case LAST:
                addSibling(newNode, relativeNode);
                break;

            default:
                // invalid position
                return;
        }
		rootNode.setResolver(imgres);
    }

    public TreeNode getRootNode() {
      return rootNode;
    }

    public int getCount() {
       return count;
    }

    boolean viewable(TreeNode node)
    {
        for (int i=0; i<viewCount; i++)
        {
            if (node == v.elementAt(i))
            {
                return true;
            }
        }

        return false;
    }

    boolean viewable(String s)
    {
        if (s==null)
        {
            return false;
        }

        for (int i=0; i<viewCount; i++)
        {
            TreeNode tn = (TreeNode)v.elementAt(i);

            if (tn.text != null)
            {
                if (s.equals(tn.text))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean exists(TreeNode node)
    {
  //      recount();

        for (int i=0; i<count; i++)
        {
            if (node == e.elementAt(i))
            {
                return true;
            }
        }

        return false;
    }

    public boolean exists(String s)
    {
//        recount();

        if (s==null)
        {
            return false;
        }

        for (int i=0; i<count; i++)
        {
            TreeNode tn = (TreeNode)e.elementAt(i);

            if (tn.text != null)
            {
                if (s.equals(tn.text))
                {
                    return true;
                }
            }
        }

        return false;
    }

// ========================================================================
// This functions will be added on caf
// ========================================================================

    public TreeNode getNode(String name)
    {
      if (name == null) return null;
      for (int i=0; i<count; i++)
      {
        TreeNode tn = (TreeNode)e.elementAt(i);
        if (tn.text != null)
          if (name.equals(tn.text)) return tn;
      }
      return null;
    }

 // ========================================================================

    public boolean insertChild(String name, String addname)
    {
      return insertChild(name, addname, null, null);
    }

 // ========================================================================

    public boolean  insertChild(String name, String addname, String im1, String im2)
    {
      if ((name == null)||(addname==null)) return false;
      TreeNode tn = getNode(name);
      if (tn == null) return false;
      insert(new TreeNode(addname, im1, im2), tn, LevelTree.CHILD);
      return true;
    }

 // ========================================================================

    public boolean  insertNext(String name, String addname)
    {
      return insertNext(name, addname, null, null);
    }

 // ========================================================================

    public boolean  insertNext(String name, String addname, String im1, String im2)
    {
      if ((name == null)||(addname==null)) return false;
      TreeNode tn = getNode(name);
      TreeNode root = getRootNode();
      if ((tn == null)||(root == null)||(tn.text.equals(root.text))) return false;
      insert(new TreeNode(addname, im1, im2), tn, LevelTree.NEXT);
      return true;
    }

// ========================================================================

    public boolean  insertRoot(String addname)
    {
     return insertRoot(addname, null, null);
    }

// ========================================================================

    public boolean  insertRoot(String addname, String im1, String im2)
    {
     if (addname==null) return false;
     if (getRootNode() == null)
      append(new TreeNode("root"));
     return insertChild(rootNode.text, addname, im1, im2);
    }

// ========================================================================

    public boolean  setImages(String name, String img1, String img2)
    {
     return   setImageOpen (name, img1) &&
              setImageClose(name, img2);
    }

// ========================================================================

    public boolean  setImageOpen(String name, String img)
    {
     if ((name == null)||(img == null)) return false;
     TreeNode tn = getNode(name);
     if (tn==null) return false;
     tn.setExpandedImage(img);
     return true;
    }

// ========================================================================

    public boolean setImageClose(String name, String img)
    {
     if ((name == null)||(img == null)) return false;
     TreeNode tn = getNode(name);
     if (tn==null) return false;
     tn.setCollapsedImage(img);
     return true;
    }

// ========================================================================

    public boolean changeText(String name, String newname)
    {
     if ((name == null)||(newname == null))  return false;
     TreeNode tn = getNode(name);
     tn.setText(newname);
     return true;
    }

// ========================================================================

    public void setNamesDelim(String delim)
    {
     this.delim = delim;
    }

// ========================================================================
// end add
// ========================================================================

    // add new node to level 0
    public void append(TreeNode newNode)
    {
        if (rootNode==null)
        {
            rootNode=newNode;
            rootNode.setDepth(0);
            rootNode.setStringProperty("PATH","");
            e.addElement(rootNode);
            count=1;
        }
        else
        {
            addSibling(newNode, rootNode);
        }
		newNode.setResolver(imgres);
    }

    void addChild(TreeNode newNode, TreeNode relativeNode)
    {
        if (relativeNode.child == null)
        {
            relativeNode.child = newNode;
            newNode.parent     = relativeNode;
            newNode.setDepth(relativeNode.getDepth()+1);
            String prop = relativeNode.getStringProperty("PATH");
            if (prop.length()>0) prop += delim;
            newNode.setStringProperty("PATH", prop+ newNode.text);
            e.addElement(newNode);
            count++;
        }
        else
        {
            addSibling(newNode, relativeNode.child);
        }

        ++relativeNode.numberOfChildren;
		newNode.setResolver(imgres);
    }

    protected void addBefore (
        TreeNode whereNode,
        TreeNode newNode,
        TreeNode mark)
    {
        if(whereNode==null || newNode ==null || mark == null) return;

        TreeNode rel = whereNode;
        if(whereNode.child == mark || whereNode.child==null){
            newNode.sibling = whereNode.child;
            whereNode.child = newNode;
        }
        else{
            whereNode = whereNode.child;
            while(whereNode.sibling!=null && whereNode.sibling!=mark)
                whereNode = whereNode.sibling;;
            if(whereNode.sibling!=null) newNode.sibling = whereNode.sibling;
            whereNode.sibling = newNode;
        }
        newNode.setResolver(imgres);
        ++rel.numberOfChildren;
        newNode.parent = rel;
        newNode.setDepth(rel.getDepth()+1);

        String prop = whereNode.getStringProperty("PATH");

        if (prop.length()>0) prop += delim;
        newNode.setStringProperty("PATH", prop + newNode.text);
        e.insertElementAt(newNode,e.indexOf(mark));
        count++;
    }

    void addSibling(TreeNode newNode, TreeNode siblingNode)
    {
        TreeNode tempNode;
        tempNode = siblingNode;

        String s     = siblingNode.getStringProperty("PATH");
        int    index = s.lastIndexOf(delim);
        if (index >= 0)
            newNode.setStringProperty("PATH", s.substring(0, index) + delim + newNode.text);
        else
            newNode.setStringProperty("PATH", newNode.text);
        while (tempNode.sibling != null) tempNode = tempNode.sibling;
        tempNode.sibling = newNode;
        newNode.parent   = tempNode.parent;
        newNode.setDepth(tempNode.getDepth());
        e.addElement(newNode);
        count++;
		newNode.setResolver(imgres);
    }

    public TreeNode remove(String s)
    {
        recount();

        for (int i=0; i<count; i++)
        {
            TreeNode tn = (TreeNode)e.elementAt(i);

            if (tn.text != null)
            {
                if (s.equals(tn.text))
                {
                    remove(tn);
                    return tn;
                }
            }
        }

        return null;
    }


    public void remove(TreeNode node)
    {
        if (!exists(node))
        {
            return;
        }

        // remove node and its decendents
        if (node.parent != null)
        {
            if (node.parent.child == node)
            {
                if (node.sibling != null)
                {
                    node.parent.child = node.sibling;
                }
                else
                {
                    node.parent.child = null;
                    node.parent.collapse();
                }
            }
            else
            {
                TreeNode tn=node.parent.child;

                while (tn.sibling != node)
                {
                    tn = tn.sibling;
                }

                if (node.sibling != null)
                {
                    tn.sibling = node.sibling;
                }
                else
                {
                    tn.sibling = null;
                }
            }
        }
        else
        {
            if (node == rootNode)
            {
                if (node.sibling == null)
                {
                    rootNode=null;
                }
                else
                {
                    rootNode=node.sibling;
                }
            }
            else
            {
                TreeNode tn = rootNode;

                while (tn.sibling != node)
                {
                    tn = tn.sibling;
                }

                if (node.sibling != null)
                {
                    tn.sibling = node.sibling;
                }
                else
                {
                    tn.sibling = null;
                }
            }
        }

        recount();
    }



    private void recount()
    {
        count = 0;
        e = new Vector();

        if (rootNode != null)
        {
            rootNode.depth=0;
            traverse(rootNode);
        }
    }

    private void traverse(TreeNode node)
    {
        count++;
        e.addElement(node);

        if (node.child != null)
        {
            node.child.depth = node.depth+1;
            traverse(node.child);
        }
        if (node.sibling != null)
        {
            node.sibling.depth = node.depth;
            traverse(node.sibling);
        }
    }


    public void resetVector()
    {
        // Traverses tree to put nodes into vector v
        // for internal processing. Depths of nodes are set,
        // and viewCount and viewWidest is set.
        v = new Vector(count);

        if (count < 1)
        {
            viewCount=0;
            return;
        }

        rootNode.depth=0;
        vectorize(rootNode.child);
        viewCount=v.size();
    }

    private void vectorize(TreeNode node)
    {
        if (node==null) return;

        if (!node.hidden)
        {
          v.addElement(node);
          if (node.isExpanded())
          {
              if (node.child != null)
              {
                node.child.depth = node.depth + 1;
                vectorize(node.child);
              }
          }
        }

        if (node.sibling != null)
        {
            node.sibling.depth = node.depth;
            vectorize(node.sibling);
        }
    }

    public void setTreeStructure(String s[])
    {
        rootNode = null;
        treeStructure = s;
        try
        {
            parseTreeStructure();
        }
        catch(InvalidTreeNodeException e)
        {
            System.out.println(e);
        }
    }

    public String []getTreeStructure()
    {
        return (treeStructure);
    }

    private void parseTreeStructure()
        throws InvalidTreeNodeException
    {
        String tempStructure[] = null;
        String entry           = null;
        TreeNode node;

        tempStructure = treeStructure;

       //entry = tempStructure[0];

      // if(findLastPreSpace(entry) > -1)
      // {
      //    throw new InvalidTreeNodeException();
      //  }

     //   node = new TreeNode(entry.trim());
      //  node.setDepth(0);
      //  append(node);

        node = new TreeNode( "root" );
        node.setDepth(0);
        append(node);

        for(int i = 0; i < tempStructure.length; i++)
        {
            TreeNode currentNode;
            int indentLevel;

            entry = tempStructure[i];
            indentLevel = findLastPreSpace(entry)+1;

            if(indentLevel == -1)
            {
                throw new InvalidTreeNodeException();
            }

            currentNode = rootNode;

            for(int j = 1; j < indentLevel; j++)
            {
                TreeNode tempNode;
                int numberOfChildren;

                numberOfChildren = currentNode.numberOfChildren;
                tempNode = null;

                if(numberOfChildren > 0)
                {
                    tempNode = currentNode.child;

                    while(tempNode.sibling != null)
                    {
                        tempNode = tempNode.sibling;
                    }
                }

                if(tempNode != null)
                {
                    currentNode = tempNode;
                }
                else
                {
                    break;
                }
            }

            int diff;

            diff = indentLevel - currentNode.getDepth();

            if(diff > 1)
            {
                throw new InvalidTreeNodeException();
            }

            node = new TreeNode(entry.trim());
            node.setDepth(indentLevel);

            if(diff == 1)
            {
                insert(node, currentNode, CHILD);
            }
            else
            {
                insert(node, currentNode, NEXT);
            }
        }
//        topVisibleNode = rootNode.child;
    }

    private int findLastPreSpace(String s)
    {
        int length;

        length = s.length();

        if(s.charAt(0) != ' ' && s.charAt(0) != '\t')
        {
            return 0;
        }

        for(int i = 1; i < length; i++)
        {
            if(s.charAt(i) != ' ' && s.charAt(i) != '\t')
            {
                return i;
            }
        }

        return -1;
    }

    public void setResolver(ImageResolver imgres)
    {
       this.imgres=imgres;
       TreeNode t=getRootNode();
       setResolver(t,imgres);
    }

    private void setResolver(TreeNode t, ImageResolver imgres)
    {
       if(t!=null) t.setResolver(imgres);
       int i;
       for(i=0;i<e.size();++i){
          TreeNode c=(TreeNode)e.elementAt(i);
          c.setResolver(imgres);
       }
    }

    public void show(String name)
    {
       TreeNode t=getNode(name);
       if(t!=null) t.setHide(false);
    }

    public void hide(String name)
    {
       TreeNode t=getNode(name);
       if(t!=null) t.setHide(true);
    }

    public void setCaption(String name, String caption)
    {
       TreeNode t=getNode(name);
       if(t==null) return;
       t.setCaption(caption);
    }

    public void openNode(String name)
    {
       TreeNode t=getNode(name);
       if(t==null) return;
       if(t.isExpandable() && !t.isExpanded()) t.toggle();
    }

    public void closeNode(String name)
    {
       TreeNode t=getNode(name);
       if(t==null) return;
       if(t.isExpandable() && t.isExpanded()) t.toggle();
    }

    public void toggleNode(String name)
    {
       TreeNode t=getNode(name);
       if(t==null) return;
       if(t.isExpandable()) t.toggle();
    }

    public boolean isHidden(String name)
    {
       TreeNode t=getNode(name);
       if(t==null) return false;
       return t.getHide();
    }

 // ========================================================================

    public void expandAll()
    {
    	expandAll(null);	
    }
	public void expandAll(String path)
    {
      recount();
      if (e == null) return;
      for (int i=0; i<e.size(); i++)
      {
        TreeNode tn = (TreeNode)e.elementAt(i);
		if (path == null || tn.text.startsWith(path))
	        tn.expand();
      }
	  resetVector();
    }

	public void collapseAll() {
		collapseAll(null);
	}
	public void collapseAll(String path)
	{
	  recount();
	  if (e == null) return;
	  for (int i=0; i<e.size(); i++)
	  {
		TreeNode tn = (TreeNode)e.elementAt(i);
		if (path == null || tn.text.startsWith(path))
			tn.collapse();
	  }
	  resetVector();
	}

 // ========================================================================

    public TreeNode findNode(String name, String d)
    {
      recount();

      TreeNode r = getRootNode ();
      if (r == null) return null;

      TreeNode ch = r.child;
      StringTokenizer st = new StringTokenizer(name, d);

      if (!st.hasMoreElements()) return null;

      String n = (String)st.nextElement();
      while (ch != null)
      {
        if (ch.caption!=null && ch.caption.equalsIgnoreCase(n))
        {
          if (!st.hasMoreElements()) return ch;
          else
          {
            n  = (String)st.nextElement();
            ch = ch.child;
          }
        }
        else
          ch = ch.sibling;
      }
      return null;
    }


    public void view(TreeNode r, String s)
    {
      recount();
      if (r == null) return;

      System.out.println (s + r.caption);
      view (r.child, s + " ");
      view (r.sibling, s);
    }


  // ========================================================================


    public TreeNode getNode2(String name)
    {
      recount();
      StringTokenizer st     = new StringTokenizer(name, this.delim);
      TreeNode        parent = null;
      if (st.hasMoreElements())
      {
        parent = getNode(st.nextToken());
      }
      else
        return null;
      while (st.hasMoreElements())
      {
        String   childName = st.nextToken();
        TreeNode child     = getChild    (parent, childName);
        if (child == null) return null;
        parent    = child;
      }
      return parent;
    }

// ========================================================================

   protected int  getNumChild (TreeNode parent)
   {
     recount();
     if (parent == null) return -1;
     TreeNode next = parent.child;
     int count=0;
     while(next!=null)
     {
      count++;
      next=next.sibling;
     }
     return count;
   }

// ========================================================================

   protected TreeNode  getChild (TreeNode parent, String nameChild)
   {
     recount();
     if (parent == null || parent.child==null ||
        nameChild==null)
           return null;
     int  size = getNumChild(parent);
     TreeNode next = parent.child;
     for (int i=0; i<size; i++)
     {
       if (nameChild.equalsIgnoreCase(next.getText())) return next;
       next = next.sibling;
     }
     return null;
   }

// ========================================================================

   public TreeNode[]  enumChild (String name)
   {
     recount();
     StringTokenizer st     = new StringTokenizer(name, this.delim);
     TreeNode        parent = null;
     if (st.hasMoreElements())  parent = getNode(st.nextToken());
     while (st.hasMoreElements())
     {
       String   childName = st.nextToken();
       TreeNode child     = getChild    (parent, childName);
       if (child == null) return null;
       parent    = child;
     }
     return enumChild(parent);
   }

// ========================================================================

   public TreeNode[]  enumChild (TreeNode tn)
   {
     recount();
     if (tn == null || tn.child == null) return null;
     int  size = getNumChild(tn);
     TreeNode tns[] = new TreeNode[size];
     TreeNode next  = tn.child;
     for (int i=0; i<size; i++)
     {
       tns[i] = next;
       next   = next.sibling;
     }
     return tns;
   }

   public void insertNode(String top, String news, String before)
   {
      TreeNode a = top==null?rootNode:getNode(top);
      TreeNode b = new TreeNode(news);
      TreeNode c = getNode(before);
      addBefore(a,b,c);
   }
}
