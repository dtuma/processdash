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

import org.zaval.awt.peer.*;

import java.awt.*;

public class SymTree
extends Panel
implements ScrollArea, ScrollObject
{
    public static final int SEL_CHANGED = 1006; //selection changed event

    private TreeNode selectedNode;      // highlighted node

    private Scrollbar   sbV;        // vertical scrollbar
    private Scrollbar   sbH;        // vertical scrollbar

    private Color bgHighlightColor = Color.gray;    // selection bg color
    private Color fgHighlightColor = Color.white;   // selection fg color
    private int   viewHeight       = 300;
    private int   viewWidth        = 300; // pixel size of tree display

    private final int cellSize    = 16;   // size of node image
    private int clickSize   = 8;    // size of mouse toggle (plus or minus)
    private int textInset   = 6;    // left margin for text
    private int textBaseLine= 3;    // position of font baseline from bottom of cell
    private FontMetrics fm; // current font metrics
    private ScrollController sm;

    protected Image    im1;    // offscreen image
    protected Graphics g1 = null;  // offscreen graphics context
    protected boolean  noChoice = false;

    private int        posx = 0, posy = 0;
    private Dimension  scrollInsets = new Dimension (10,0);
    private LevelTree  ltree=null;
    private ScrollLayout sl = new ScrollLayout();
    private boolean validating = true;

    private static final int DELETE = 127;
    private static final int INSERT = 1025;

    public SymTree()
    {
      super.setLayout(sl);

      add("East",   sbV = new Scrollbar(Scrollbar.VERTICAL  ));
      add("South",  sbH = new Scrollbar(Scrollbar.HORIZONTAL));
      add("Stubb",  new StubbComponent());

      sbV.hide();
      sbH.hide();
      sbV.setBackground(Color.lightGray);
      sbH.setBackground(Color.lightGray);
      sm = new ScrollController(this, this);
      ltree = new LevelTree();
    }

    public SymTree(TreeNode head)    {
       this();
       selectedNode = ltree.getRootNode();
    }

    public void setBackground(Color c)  {
        super.setBackground(c);
        invalidate();
    }

    public void setForeground(Color c)  {
        super.setForeground(c);
        invalidate();
    }

    // Insert a new node relative to a node in the tree.
    // position = CHILD inserts the new node as a child of the node
    // position = NEXT inserts the new node as the next sibling
    // position = PREVIOUS inserts the new node as the previous sibling
    public void insert(TreeNode newNode, TreeNode relativeNode, int position) {
      ltree.insert(newNode, relativeNode, position);
    }

    public TreeNode getRootNode()        { return ltree.getRootNode();   }
    public int getCount()                { return ltree.getCount();      }
    public boolean exists(TreeNode node) { return ltree.exists(node);  }
    public boolean exists(String s)      { return ltree.exists(s);  }
    private void resetVector()           { ltree.resetVector(); }

// ========================================================================
// This functions will be added on caf
// ========================================================================

    public TreeNode getNode(String name) { return ltree.getNode(name); }

// ========================================================================

    public boolean insertChild(String name, String addname)
    {
      boolean b = insertChild(name, addname, null, null);
      validate2();
      return b;
    }

// ========================================================================

    public boolean  insertChild(String name, String addname, String im1, String im2)
    {
      boolean b = ltree.insertChild(name, addname, im1, im2);
      validate2();
      return b;
    }

// ========================================================================

    public boolean  insertNext(String name, String addname)
    {
      boolean b = insertNext(name, addname, null, null);
      validate2();
      return b;
    }

// ========================================================================

    public boolean  insertNext(String name, String addname, String im1, String im2)
    {
      boolean b = ltree.insertNext(name, addname, im1, im2);
      validate2();
      return b;
    }

// ========================================================================

    public boolean  insertRoot(String addname)
    {
      boolean b = insertRoot(addname, null, null);
      validate2();
      return b;
    }

// ========================================================================

    public boolean  insertRoot(String addname, String im1, String im2)
    {
      boolean b = ltree.insertRoot(addname, im1, im2);
      validate2();
      return b;
    }

// ========================================================================

    public boolean  setImages(String name, String img1, String img2) {
      return   setImageOpen (name, img1) &&
               setImageClose(name, img2);
    }

// ========================================================================

    public boolean  setImageOpen(String name, String img) {
     return ltree.setImageOpen(name, img);
    }

// ========================================================================

    public boolean setImageClose(String name, String img) {
     return ltree.setImageClose(name, img);
    }

// ========================================================================

    public boolean changeText(String name, String newname) {
      return ltree.changeText(name, newname);
    }

// ========================================================================

    public boolean selectNode (String name)
    {
      if (getNode(name) == null) return false;
      TreeNode f=getNode(name);
      if (isHidden(name)) return false;
      selectedNode = f;
      return true;
    }

    public boolean selectNode (TreeNode tn)  {
       selectedNode = tn;
       return true;
    }

    public boolean selectNodeAndOpen (String name)
    {
       if(!selectNode(name)) return false;
       TreeNode x = selectedNode.parent;
       while(x!=null){
          x.expand();
          x=x.parent;
       }
       validate2();
       return true;
    }

    public void  setNamesDelim(String delim)
    {
      ltree.setNamesDelim(delim);
    }

    public TreeNode findNode(String name, String d) {
      return ltree.findNode(name, d);
    }


// ========================================================================
// end add
// ========================================================================

    // add new node to level 0
    public void append(TreeNode newNode) {
      ltree.append(newNode);
      if (getRootNode()==null) selectedNode = getRootNode();
    }

    public void remove(String s) {
      remove(getNode(s));
    }

    public void removeSelected()
    {
      if (selectedNode != null) {
        remove(selectedNode);
      }
    }

    public void remove(TreeNode node)
    {
        int viewCount = getViewCount();
        if (node == selectedNode)
        {
            int index = getIndex(selectedNode);

            if (index == -1) index = ltree.e.indexOf(selectedNode);

            if (index > viewCount-1)
                index = viewCount-1;

            if (index>0)
            {
                changeSelection((TreeNode)ltree.v.elementAt(index-1),index-1);
            }
            else
            if (viewCount>0)
            {
                try{
                   changeSelection((TreeNode)ltree.v.elementAt(1),1);
                }
                catch(Exception e){
                   changeSelection((TreeNode)ltree.v.elementAt(0),0);
                }
            }
        }
        ltree.remove(node);
    }

    // -----------------------------------------
    // --------- event related methods ---------
    // -----------------------------------------

    protected boolean checkScrolls()
    {
        if (!isVisible()) return false;

        int       viewCount = getViewCount(); //getViewCount2();
        Dimension d         = getSASize(); //size();
        boolean   b         = false;

        int hh = sm.getMaxHorScroll();
        int hv = sm.getMaxVerScroll();

        int       w = d.width  - (hv>0?ScrollController.SCROLL_SIZE:0);
        int       h = d.height - (hh>0?ScrollController.SCROLL_SIZE:0);

        if (hv>0)
        {
           int hl = viewCount - h/cellSize + ((h%cellSize>0)?1:0);
           hl += scrollInsets.height;
           if (hl <= 1) hl = 2;
           int v = sbV.getValue();
           sbV.setValues(v, 0, 0, hl);
           if (!sbV.isVisible()) b = true;
           sbV.show();
        }
        else
        {
            if (sbV.isVisible())
            {
              sbV.hide();
              sbV.setValue(0);
              posy = 0;
              b = true;
            }
        }

        if (sm.getMaxHorScroll()>0)
        {
              int h1 = getMaxWidth() - w + scrollInsets.width;
              int v = sbH.getValue();

              if (h1 <= 1) h1 = 2;
              sbH.setValues(v, 0, 0, h1);
              if (!sbH.isVisible()) b = true;
              sbH.show();
        }
        else
        {
            if (sbH.isVisible())
            {
              sbH.setValue(0);
              sbH.hide();
              posx = 0;
              b = true;
            }
        }
        return b;
    }

    public void reshape(int x, int y, int w, int h) {
      super.reshape(x,y,w,h);
      validate2();
    }

    public boolean handleEvent(Event event)
    {
        if (scroll(event)) return true;

        if (event.key > 0 && selectedNode != null)
        {
          if ((event.key == 45) && (selectedNode.isExpanded()))
          {
            toggleEvent(selectedNode, 0);
            return true;
          }

          if ((event.key == 43) && isExpandable(selectedNode) && (!selectedNode.isExpanded()))
          {
            toggleEvent(selectedNode,1);
            return true;
          }
        }
        return(super.handleEvent(event));
    }

    public String getCaption(String name)
    {
       TreeNode t=getNode(name);
       if(t==null) return null;
       return t.getCaption();
    }

    int  mouse_down = 0;
    long time = 0;

    public boolean mouseUp(Event event, int x, int y)
    {
      if (time == 0) time = System.currentTimeMillis();
      long tt = System.currentTimeMillis();
      long dt = tt - time;
      time = tt;
      if (event.clickCount == 0 && dt < 300) event.clickCount = 2;

      if (event.modifiers == 4) return super.mouseUp(event, x, y);
      requestFocus();

      mouse_down = 1;
      boolean[] flags=new boolean[1];
      boolean b = changeSelection(event, x, y, true, flags);
      if (!flags[0]) time -= 400;

      if (flags[0] && event.clickCount == 2)
      {
         Event e = new Event(event.target, 9999, event.arg);
         getParent().postEvent(e);
      }
      return super.mouseUp(event,x,y);
    }

    public boolean keyDown(Event event, int key)
    {
        requestFocus();
        if (selectedNode == null) return super.keyDown(event, key);
        int index     = getIndex(selectedNode);
        int viewCount = getViewCount();
        TreeNode f    = null;
        mouse_down = 0;
        switch (key)
        {
            case 10:
                sendActionEvent(event);
                break;
            case Event.UP:
                if (index > 0)
                {
                    index--;
                    changeSelection((TreeNode)ltree.v.elementAt(index), index);
                    sendActionEvent(event);
                } break;
            case Event.DOWN:
                if (index < viewCount-1)
                {
                    index++;
                    changeSelection((TreeNode)ltree.v.elementAt(index), index);
                    sendActionEvent(event);
                } break;
            case Event.RIGHT:
                {
                    if (!selectedNode.isExpanded())
                    {
                      toggleEvent(selectedNode,1);
                    }
                    else
                    if (selectedNode.child != null)
                    {
                      f = selectedNode.child;
                      while (f!=null && f.hidden) f = f.sibling;
                      if (f != null)
                      {
                        changeSelection(f, index);
                        sendActionEvent(event);
                      }
                    }
                } break;
           case Event.LEFT:
                {
                    if (selectedNode.isExpanded())
                      toggleEvent(selectedNode,0);
                    else
                    if (selectedNode.parent != null && selectedNode.parent != getRootNode() && !selectedNode.parent.hidden)
                    {
                      changeSelection(selectedNode.parent, index);
                      sendActionEvent(event);
                    }
                } break;
           case Event.PGUP:
                {
                  scrollPages(-1);
                  sendActionEvent(event);
                } break;
           case Event.PGDN:
                {
                  scrollPages(1);
                  sendActionEvent(event);
                } break;
           case Event.HOME:
                {
                  f = (TreeNode)ltree.v.elementAt(0);
                  changeSelection(f, 0);
                  sendActionEvent(event);
                } break;
           case Event.END:
                {
                  f = (TreeNode)ltree.v.elementAt(ltree.v.size()-1);
                  changeSelection(f, ltree.v.size()-1);
                  sendActionEvent(event);
                } break;
           case INSERT:
                {
                   Event e = new Event(event.target, 9999, event.arg);
                   getParent().postEvent(e);
                } break;
           case DELETE:
                {
                   Event e = new Event(event.target, 9991, event.arg);
                   getParent().postEvent(e);
                } break;
        }
        return super.keyDown(event, key); //false;
    }

    private void sendActionEvent(Event event)
    {
        int id = event.id;
        Object arg = event.arg;
        event.id = Event.ACTION_EVENT;
        event.arg = new String(selectedNode.getText());
        postEvent(event);
        event.id = id;
        event.arg = arg;
        repaint();
    }

    public TreeNode getSelectedNode()  {
        return selectedNode;
    }

    public String getSelectedText()
    {
        if (selectedNode==null) return null;
        return selectedNode.getText();
    }

    public TreeNode getNode(int x, int y)
    {
      int index = ((Math.abs(posy) + y)/cellSize);
      for(int i=0;i<=index && i<ltree.v.size();++i)
      {
         TreeNode tmpNode = (TreeNode)ltree.v.elementAt(i);
         if(tmpNode.getHide()) ++index;
      }
      if(index>=ltree.v.size()) return null;
      return (TreeNode)ltree.v.elementAt(index);
    }

    public boolean changeSelection(Event evt, int x, int y,
                                   boolean isToggle, boolean[] flags)
    {
        requestFocus();

        Dimension d = size();
        flags[0]=false;

        int viewCount = d.height/cellSize;
        int index = ((Math.abs(posy) + y)/cellSize);
        noChoice = false;

        int index2 = y/cellSize;
        if (index2 > viewCount-1)
        {
          noChoice = true;
          return false;     //clicked below the last node
        }

        for(int i=0;i<=index && i<ltree.v.size();++i)
        {
           TreeNode tmpNode = (TreeNode)ltree.v.elementAt(i);
           if(tmpNode.getHide()) ++index;
        }

        if(index>=ltree.v.size()) return false;

        TreeNode oldNode = selectedNode;
        TreeNode newNode = (TreeNode)ltree.v.elementAt(index);
        int newDepth = newNode.getDepth();

      // check click in place plus/minus

        if (isExpandable(newNode))
        {
          Rectangle rec = new Rectangle(
          posx + cellSize*(newDepth-1) + cellSize/4,
          posy + getIndex(newNode)*cellSize + clickSize/2,clickSize, clickSize);

          if ((rec.inside(x,y))&&(isToggle))
          {
            toggleEvent(newNode, newNode.isExpanded()?1:0);
            return false;
          }
        }

      // check max right position

        String text=(newNode.caption==null)?newNode.text:newNode.caption;
        int x1 = posx + ((newDepth-1) * (cellSize)) + cellSize + textInset;
        int x2 = x1  + fm.stringWidth(text) + 4;
        FontMetrics fm = g1.getFontMetrics();
        if (newNode.getImage() != null)
        {
           x2 = x2 + fm.getHeight();
//           x1 = x1 - fm.getHeight();
        }

        if (newNode.getIndicator() != null)
           x2 = x2 + fm.getHeight();

        if ((x > x2)||(x < x1))
        {
          noChoice = true;
          return false;
        }

        if(newNode==oldNode) flags[0]=true;
        changeSelection(newNode, index);

        // check for toggle box click
        // todo: make it a bit bigger
        Rectangle toggleBox = new Rectangle(
           posx + cellSize*newDepth + cellSize/4,
           posy + index*cellSize + clickSize/2,
           clickSize,
           clickSize);

        if ((evt.modifiers != 4)&&(isToggle)&&(newNode == oldNode))
        {
           toggleEvent(newNode, newNode.isExpanded()?0:1);
           return false;
        }

        if (newNode.getImage()    != null) toggleBox.x -= fm.getHeight();
        if (newNode.getIndicator()!= null) toggleBox.x -= fm.getHeight();

        if (!toggleBox.inside(x,y))
           sendActionEvent(evt);

        return false;
    }

    private void validate2()
    {
      if (validating) {
	      resetVector();
	      if (checkScrolls()) invalidate();
	      validate();
	      repaint();
      }
    }
    public void setValidating(boolean v) {
    	if (validating != v) {
    		validating = v;
    		if (v) validate2();
    	}
    }

    private int getIndex(TreeNode node)  {
       return ltree.v.indexOf(node);
    }

    private void changeSelection(TreeNode node, int index)
    {
        if (selectedNode == null)  {
          if (node != null) selectedNode = node;
          else return;
        }

        TreeNode oldNode = selectedNode;
        selectedNode = node;

        int y = index*cellSize;
        drawNodeText(oldNode, y, true);
        drawNodeText(node,    y, true);

        checkSelection(index);
    }

    protected void checkSelection(int index)
    {
       if (!sbV.isVisible() || index < 0) return ;
       int y = index*cellSize;

       if (posy != 0 && y < Math.abs(posy))
       {
           int maxIndex = Math.abs(posy)/cellSize;
           int decIndex = maxIndex - index;
           int dy       = decIndex*cellSize;
           vscroll(-decIndex);
           return;
       }

       y +=(posy + cellSize);
       int k = viewHeight;
       if (sbH.isVisible()) k -= ScrollController.SCROLL_SIZE;
       if (y > k)
       {
           int dy       = (y - k);
           int incIndex = dy/cellSize;
           if (dy % cellSize > 0) incIndex++;
           vscroll(incIndex);
           return;
       }
    }

    public void paint (Graphics g)
    {
        redraw();
        if(im1!=null) g.drawImage(im1, 0, 0, this);

        g.setColor(Color.gray);
        Dimension d = size();
        g.drawRect(0,0, d.width-1, d.height-1);
    }

    public void redraw()
    {
       resetVector();
       drawTree();
    }

    public void drawTree()
    {
        Dimension d = size();
        if ((d.width != viewWidth) || (d.height != viewHeight) || g1==null)
        {
            if(d.width*d.height<=0) return;

            im1 = createImage(d.width, d.height);
            if (g1 != null)
            {
              g1.dispose();
              g1 = null;
            }
            g1 = im1.getGraphics();
            viewWidth  = d.width;
            viewHeight = d.height;
        }

        Font f = getFont();  // unix version might not provide a default font

        //Make certain there is a font
        if (f == null)
        {
            f = new Font("TimesRoman", Font.PLAIN, 14);
            g1.setFont(f);
            setFont(f);
        }

        //Make certain the graphics object has a font (Mac doesn't seem to)
        if (f != null)
          if (g1.getFont() == null)  g1.setFont(f);

        int ww = viewWidth  - ((sbV.isVisible())?ScrollController.SCROLL_SIZE:0);
        int hh = viewHeight - ((sbH.isVisible())?ScrollController.SCROLL_SIZE:0);

        fm = g1.getFontMetrics();
        g1.setColor(getBackground());
        g1.fillRect(0,0,viewWidth,viewHeight);  // clear image

        int lastOne   = ltree.v.size();
        int skipCount = 0;
        int viewCount = getViewCount();//getViewCount2();
        for (int i=0; i<lastOne; i++)
        {
           TreeNode node=(TreeNode)ltree.v.elementAt(i);

           int x = posx + cellSize*(node.depth - 1);
           int y = posy + (i-skipCount)*cellSize;

            // draw lines
            g1.setColor(getForeground());

            // draw vertical sibling line
            TreeNode sb = getSibling(node);
            if (sb != null)
            {
                int k = getIndex(sb) - getIndex(node);
                //if (k > lastOne) k = lastOne;
                drawDotLine(x + cellSize/2, y + cellSize/2,
                            x + cellSize/2, y + cellSize/2 +  k*cellSize);
            }

            // draw vertical child lines
            if (node.isExpanded())
            {
                int xx = x + cellSize + cellSize/2;
                drawDotLine(xx, y + cellSize -2,
                            xx, y + cellSize + cellSize/2);
            }

            // draw node horizontal line
            g1.setColor(getForeground());
            int xxx = x + cellSize/2;
            drawDotLine(xxx, y + cellSize/2,
                        xxx + cellSize/2 + 10, y + cellSize/2);

            // draw toggle box
            if (isExpandable(node))
            {
                    //int xx = cellSize*(node.depth) + cellSize/4;
                    int xx = x + clickSize/2;

                    g1.setColor(getBackground());
                    g1.fillRect(xx, y + clickSize/2, clickSize, clickSize );
                    g1.setColor(getForeground());
                    g1.drawRect(xx, y + clickSize/2, clickSize, clickSize );
                    // cross hair
                    g1.drawLine(xx + 2,             y + cellSize/2,
                                xx + clickSize - 2, y + cellSize/2);

                    if (!(isExpanded(node)))
                    {
                        g1.drawLine(xx + clickSize/2, y + clickSize/2 +2,
                                    xx + clickSize/2, y + clickSize/2 + clickSize -2);
                    }
            }

            // draw node image
            Image nodeImage = isExpanded(node)?node.getExpandedImage():node.getCollapsedImage();
            if (nodeImage != null)
            {
              g1.drawImage(nodeImage, x + cellSize, y, this);
            }

            // draw node indicator
            Image nodeInd = node.getIndicator();
            if (nodeInd != null)
            {
              int dx = ((nodeImage==null)?cellSize:2*cellSize) + 2;
              int dy = (cellSize - nodeInd.getHeight(this))/2 + 1;

              g1.drawImage(nodeInd, x + dx, y + dy, this);
            }

            // draw node text
            if (node.text != null)
               drawNodeText(node, y, node==selectedNode);
        }
    }

    private TreeNode getSibling(TreeNode node) {
      TreeNode tn = node.sibling;
      while (tn!=null && tn.hidden) tn = tn.sibling;
      return tn;
    }

    private int getMaxWidth()
    {
      int  max = 0;
      for (int i=0; i<ltree.v.size(); i++)
      {
         TreeNode node=(TreeNode)ltree.v.elementAt(i);
         if (node.getHide()) continue;
         String text = node.caption==null?node.text:node.caption;
         int depth=node.depth;

         int stringWidth = ((depth-1) * cellSize) + cellSize + textInset - 1 + ((fm==null)?0:fm.stringWidth(text));
         if (node.getImage()     != null && fm != null) stringWidth += fm.getHeight();
         if (node.getIndicator() != null && fm != null) stringWidth += fm.getHeight();
         if (stringWidth > max) max = stringWidth;
      }
      return max;
    }

    private void drawNodeText(TreeNode node, int yPosition, boolean eraseBackground)
    {
        String text = node.caption==null?node.text:node.caption;
        Color fg = null;
        Color bg = null;
        int   textOffset = getTextPos(node);

        if (node==selectedNode)
        {
          fg=fgHighlightColor;
          bg=bgHighlightColor;
        }
        else
        {
          fg = getForeground();
          bg = getBackground();
        }

        int stringWidth = textOffset - 1 + fm.stringWidth(text) - posx;
        if (eraseBackground)
        {
            g1.setColor(bg);
            if (styleMark != RECT_MARK)
            {
              g1.fillRect(textOffset-1, yPosition+1, fm.stringWidth(text)+4, cellSize-1);
              g1.setColor(fg);
            }
            else
            {
              g1.setColor(Color.black);
              g1.drawRect(textOffset-1, yPosition+1, fm.stringWidth(text)+4, cellSize-2);
            }
        }
        else
        {
           g1.setColor(fg);
        }

        g1.drawString(text, textOffset, yPosition + cellSize - textBaseLine);
    }

    private void drawDotLine(int x0, int y0, int x1, int y1)
    {
       if (y0==y1)
       {
         for (int i = x0; i<x1; i+=2)
           g1.drawLine(i,y0, i, y1);
       }
       else
       {
         for (int i = y0; i<y1; i+=2)
           g1.drawLine(x0, i, x1, i);
       }
    }

    public void setTreeStructure(String s[])
    {
        ltree.setTreeStructure(s);
        selectedNode = null;
        invalidate();
    }

    public String [] getTreeStructure()
    {
      return ltree.getTreeStructure();
    }

    public Dimension preferredSize()
    {
      FontMetrics fm = getFontMetrics(getFont());
      return new Dimension(175, Math.min(ltree.v.size() * fm.getHeight(), 500));
    }

    public Dimension minimumSize()
    {
      return preferredSize();
    }

    public void setLayout(LayoutManager lm){
    }

    public void setResolver(ImageResolver imgres) {
       ltree.setResolver(imgres);
    }

    public void show(String name)  {
       TreeNode t=getNode(name);
       if(t!=null) t.setHide(false);
    }

    public void hide(String name)
    {
       TreeNode t=getNode(name);
       if(t!=null) t.setHide(true);
       else   return;

       if (t == selectedNode && fm !=null)
       {
         correctSelect(t.parent);
         return;
       }

       if (!isExpandable(t.parent))
       {
         t.parent.collapse();
         if (fm !=null)
           correctSelect(t.parent);
       }

       validate2();
    }

    private void correctSelect(TreeNode n)
    {
      resetVector();
      if (selectedNode != null && ltree.v.indexOf(selectedNode) < 0)
      {
        changeSelection(n, ltree.v.indexOf(n));
        Event event  = new Event(this, 0, null);
        sendActionEvent(event);
      }
    }

    public void setCaption(String name, String caption)
    {
       TreeNode t=getNode(name);
       if(t==null) return;
       t.setCaption(caption);
       validate2();
    }

    private boolean isExpandable(TreeNode node)
    {
        int jjk;

        if(!node.isExpandable()) return false;
        if(node.child==null) return false;
        node=node.child;

        if(!node.getHide()) return true;
        while(node.sibling!=null){
           node=node.sibling;
           if(!node.getHide()) return true;
        }
        return false;
    }

    public void openNode(String name)
    {
       TreeNode t=getNode(name);
       if(t==null || t.isExpanded()) return;

       if(t.isExpandable() && !t.isExpanded())
         t.toggle();

       validate2();
    }

    public void closeNode(String name)
    {
       TreeNode t=getNode(name);
       if(t==null) return;
       if(t.isExpandable() && t.isExpanded())
       {
         t.toggle();
         correctSelect(t);
       }

       validate2();
    }

    public boolean isHidden(String name)
    {
       TreeNode t=getNode(name);
       if(t==null) return false;
       while(t.parent!=null)
          if(t.getHide()) return true;
          else t = t.parent;
       return false;
    }

    public void expandAll(String path) {
      ltree.expandAll(path);
      validate2();
    }
    public void collapseAll(String path) {
      ltree.collapseAll(path);
	  validate2();
    }

 // ========================================================================

    public TreeNode getNode2(String name) {
      return ltree.getNode2(name);
    }

// ========================================================================

   protected int  getNumChild (TreeNode parent) {
     return ltree.getNumChild(parent);
   }

// ========================================================================

   protected TreeNode  getChild (TreeNode parent, String nameChild){
     return ltree.getChild(parent, nameChild);
   }

// ========================================================================

   public TreeNode[]  enumChild (String name){
     return ltree.enumChild(name);
   }

// ========================================================================

   public TreeNode[]  enumChild (TreeNode tn) {
     return ltree.enumChild(tn);
   }

// ========================================================================

   public static int NONE_FRAME = 0;
   public static int LINE_FRAME = 1;
   private int styleFrame = 1;

   public void styleFrame(int style) {
     styleFrame = style;
     repaint();
   }

   public static int RECT_MARK     = 0;
   public static int FILLRECT_MARK = 1;
   private int styleMark = 1;

   public void styleMark(int style) {
     styleMark = style;
     repaint();
   }

   public void insertNode(String top, String news, String before) {
      ltree.insertNode(top,news,before);
      repaint();
   }

   private boolean isExpanded(TreeNode node)
   {
      if(!node.isExpanded()) return false;
      TreeNode c = node.child;
      while(c!=null){
         if(!c.getHide()) return true;
         c = c.sibling;
      }
      return false;
   }

   public int getViewCount() {
     return ltree.getViewCount();
   }

   public Dimension getSASize() {
     return sl.getAreaSize(this);
   }

   public Scrollbar getVBar() {
     return sbV;
   }

   public Scrollbar getHBar() {
     return sbH;
   }

   public Point getSOLocation  () {
     return null;
   }

   public void setSOLocation(int x, int y) {
   }

   public Dimension  getSOSize()
   {
     int       v = getViewCount();//getViewCount2();
     Dimension r = new Dimension(getMaxWidth(), 0);
     r.height = v * cellSize;
     return r;
   }

   public Component  getScrollComponent() {
     return null;
   }

   public Rectangle getVisibleArea (Container c) {
      Rectangle r = new Rectangle();
      return r;
   }

   public boolean gotFocus(Event e, Object o)
   {
      bgHighlightColor = Color.blue;
      repaint();
      return true;
      //return super.gotFocus(e, o);
   }

   public void setIndicator(String name, String image)
   {
     TreeNode t=getNode(name);
     if(t==null) return;
     t.setIndicator(image);
   }

   public boolean lostFocus(Event e, Object o)
   {
      bgHighlightColor = Color.gray;
      repaint();
      return super.lostFocus(e, o);
   }

   private void toggleEvent(TreeNode n, int i)
   {
     if (!isExpandable(n)) return;
     getParent().postEvent(new Event(this,0,8888, 0, 0, i, 0, n.getText()));
     n.toggle();
     correctSelect(n);
     validate2();
   }

   protected boolean scrollPages(int pages)
   {
     int index  = getIndex(selectedNode);
     int height = viewHeight - ((sbH.isVisible())?ScrollController.SCROLL_SIZE:0);
     int lines  = height/cellSize;
     if (pages < 0) lines = -lines;

     int pg  = lines + index;
     int max = ltree.v.size();
     if (pg >= max) pg = max-1;
     if (pg < 0)    pg = 0;
     changeSelection((TreeNode)ltree.v.elementAt(pg), pg);
     repaint();
     return vscroll(lines);
   }

   protected boolean vscroll(int lines)
   {
     sbV.setValue(sbV.getValue() + lines);
     int id = (lines<0)?Event.SCROLL_LINE_DOWN:Event.SCROLL_LINE_UP;
     Event e = new Event(sbV, id, null);
     return scroll(e);
   }

   protected boolean scroll(Event e)
   {
      boolean b = false;
      if (e.target == sbV)
      {
          if (sbV.isVisible()) {
            posy = (-sbV.getValue()*cellSize);
            b = true;
          }
      }

      if (e.target == sbH)
      {
         if (sbH.isVisible()) {
           posx = -sbH.getValue();
           b = true;
         }
      }

      if (b) validate2();
      return b;
   }

   protected int getTextPos(TreeNode node)
   {
     int depth=node.depth;
     int textOffset = ((depth-1) * (cellSize)) + cellSize + textInset + posx;
     if (node.getImage() != null) textOffset = textOffset + fm.getHeight();
     if (node.getIndicator() != null) textOffset += cellSize;
     return textOffset;
   }
}




