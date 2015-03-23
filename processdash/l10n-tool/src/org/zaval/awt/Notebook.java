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

import java.awt.*;
import java.util.*;

public class Notebook
extends Panel
implements LayoutManager
{
    public static final int TOP = 0;
    public static final int BOTTOM = 1;
    public static final int ROUNDED = 0;
    public static final int SQUARE = 1;

    private final int IMAGE_SIZE = 16;

    private int TF_LEFT = 0;   // old 9
    private int TF_RIGHT = 0; // old -9
    private int TF_TOP = 23;  // old 30
    private int TF_BOTTOM = 0;    // old -9

    private int TF_BTN_HEIGHT = 20;

    protected int curIndex = -1;

    private Font fReg;
    private Font fSel;
    private Color caption = Color.black;

    private int iTabsPosition = TOP;
    private int iTabsStyle = SQUARE;//BOTTOM; //TOP; //ROUNDED;

    private int firstVisibleTab = 0;
    private Button dbLeft;
    private Button dbRight;

    private Polygon nullPoly;
    private int lastWidth = -1;

    private Vector pages = null;
    private Panel oobj = null;
    private CardLayout card = null;
    private ImageResolver imgres;
    private boolean mark = false;

    public Notebook()
    {
       this(TOP, ROUNDED);
    }

    public Notebook(boolean bTabsOnTop)
    {
       this(bTabsOnTop ? TOP : BOTTOM, bTabsOnTop ? ROUNDED : SQUARE);
    }

    public void setImageResolver(ImageResolver r) {
      imgres = r;
    }

    public void setFont(Font norm, Font sel, Color c)
    {
       fReg = norm;
       if(sel==null)
          sel = new Font(norm.getName(),
                         norm.getStyle() | Font.BOLD,
                         norm.getSize());
       fSel = sel;
       caption = c;
    }

    public Notebook(int tabsPostion, int tabsStyle)
    {
       pages = new Vector();
       setTabsInfo(tabsPostion,tabsStyle);

       fReg = new Font("Helvetica", Font.PLAIN, 12);
       fSel = new Font("Helvetica", Font.BOLD, 12);
       setLayout(this);

       dbLeft = new Button("<<");
       dbRight = new Button(">>");
       add(dbLeft);
       add(dbRight);
       oobj = new Panel();
       oobj.setLayout(card = new CardLayout());
       add(oobj);

       nullPoly = new Polygon();
       nullPoly.addPoint(0, 0);
       nullPoly.addPoint(1, 1);
       nullPoly.addPoint(0, 0);

       iTabsStyle = TOP;
    }

    public void setTabsInfo(int tabsPosition, int tabsStyle)
    {
       iTabsPosition = tabsPosition;
       if (iTabsPosition == TOP)
           iTabsStyle = ROUNDED;
       else
           iTabsStyle = tabsStyle;

       if (iTabsStyle == ROUNDED)
           TF_BTN_HEIGHT = 20;
       else
           TF_BTN_HEIGHT = 17;
    }

    public int getTabIndex() {
       return curIndex;
    }

    public void addPanel(String caption, Component comp)
    {
       int pos = pages.size();
       NotebookPage np = new NotebookPage();
       np.setImageResolver(imgres);

       np.comp  = comp;
       np.label = caption;
       np.hidden= false;
       np.poly  = null;
       np.color = Color.gray;
       np.name  = Integer.toString(pos);

       if(curIndex<0) curIndex = pos;
       pages.addElement(np);

       oobj.add(np.name,np.comp);
       repaint();
    }

    public void removePanel(int pos)
    {
       NotebookPage np = (NotebookPage)pages.elementAt(pos);
       pages.removeElementAt(pos);
       oobj.remove(np.comp);
       showPanel(curIndex);
    }

    public boolean isHidden(int pos)
    {
       NotebookPage np = (NotebookPage)pages.elementAt(pos);
       return np.hidden;
    }

    public Component getPanel(int pos)
    {
       NotebookPage np = (NotebookPage)pages.elementAt(pos);
       return np.comp;
    }

    public void showPanel(int pos)
    {
       NotebookPage np = (NotebookPage)pages.elementAt(pos);
       card.show(oobj,np.name);
       curIndex = pos;
       repaint();
    }

    public void setColor(int pos, Color color)
    {
       NotebookPage np = (NotebookPage)pages.elementAt(pos);
       np.color = color;
       repaint();
    }

    public void layoutContainer(Container obj)
    {
        Rectangle r = obj.bounds();
        int width = r.width - TF_LEFT + TF_RIGHT;
        if (width < 0) return;

        int height = r.height - TF_TOP + TF_BOTTOM;
        if (height < 0) return;

        int col = TF_LEFT;
        int row = 0;

        if (iTabsPosition == TOP) row = TF_TOP;
        else row = TF_TOP - TF_BTN_HEIGHT;

        oobj.move(col + 1, row + 1);

        //oobj.move(0,0);
        oobj.resize(width-3, height-3);

        if (iTabsPosition == TOP){

            dbLeft.move(r.width-33+TF_RIGHT, TF_TOP - 16);
            dbRight.move(r.width-16+TF_RIGHT, TF_TOP - 16);
            dbLeft.resize(16, 15);
            dbRight.resize(16, 15);
        }
        else{
            dbLeft.move(r.width-33+TF_RIGHT, r.height + TF_BOTTOM - TF_BTN_HEIGHT);
            dbRight.move(r.width-16+TF_RIGHT, r.height + TF_BOTTOM - TF_BTN_HEIGHT);
            dbLeft.resize(16, 15);
            dbRight.resize(16, 15);
        }
        repaint();
    }

    public Point location(int xx, int yy)
    {
       return new Point(0,0);
    }

    public void addLayoutComponent(String name, Component comp)
    {
    }

    public void removeLayoutComponent(Component comp)
    {
    }

    public Dimension preferredLayoutSize(Container parent)
    {
        return getSize(parent,true);
    }

    public Dimension minimumLayoutSize(Container parent)
    {
        return getSize(parent,false);
    }

    private Dimension getSize(Container obj, boolean max)
    {
        int width = TF_LEFT - TF_RIGHT;
        int height = TF_TOP - TF_BOTTOM;
        int col = TF_LEFT;
        int row = 0;

        if (iTabsPosition == TOP) row = TF_TOP;
        else row = TF_TOP - TF_BTN_HEIGHT;

        Dimension d = max?oobj.preferredSize() : oobj.minimumSize();
        return new Dimension (d.width + width, d.height + height);
    }

    public void setVisible(int pos, boolean vis)
    {
       NotebookPage np = (NotebookPage)pages.elementAt(pos);
       np.hidden = !vis;
       layout();
       repaint();
    }

    public void paint(Graphics g)
    {
        Rectangle r = bounds();

        // paint the box
        int width = r.width - TF_LEFT + TF_RIGHT;
        if (width < 0) width = 0;
        int height = r.height - TF_TOP + TF_BOTTOM;
        if (height < 0) height = 0;

        if (r.width > lastWidth) firstVisibleTab = 0;
        lastWidth = r.width;

        int col = TF_LEFT;
        int row;

        Color c = g.getColor();
        g.setColor(getBackground());
        g.fillRect(0, 0, r.width, r.height);

        if (iTabsPosition == TOP) row = TF_TOP;
        else row = TF_TOP - TF_BTN_HEIGHT;

        // draw border
        g.setColor(Color.white);
        g.drawLine(col, row, (col + width - 1), row);
        g.drawLine(col, row, col, (row + height - 1));

        g.setColor(Color.gray);
        g.drawLine((col + 2), (row + height - 2), (col + width - 2), (row + height - 2));
        g.drawLine((col + width - 2), (row + 2), (col + width - 2), (row + height - 2));

        g.setColor(Color.black);
        g.drawLine((col + 1), (row + height - 1), (col + width - 1), (row + height - 1));
        g.drawLine((col + width - 1), (row + 1), (col + width - 1), (row + height - 1));

        // paint the tabs, and record areas
        int x1;
        int x2 = TF_LEFT + 8;
        int y1;
        int y2;
        int x3 = 0;
        int x4 = TF_LEFT;

        int sze = pages.size();
        String sLabel;

        Font f = g.getFont();
        FontMetrics fm = getFontMetrics(fReg);
        FontMetrics fms = getFontMetrics(fSel);
        int labelWidth = 0;
        Polygon p;

        int w;
        NotebookPage[] npages = new NotebookPage[sze];
        for(w=0;w<sze;++w){
           npages[w]=(NotebookPage)pages.elementAt(w);
           npages[w].poly = nullPoly;
        }

        // make sure there is a polygon for each tab
        if (firstVisibleTab > 0) x4 += 2;
        int xStep=1;

        for (w=firstVisibleTab; w < sze; w++)
        {
            int fheight = fm.getHeight() - fms.getDescent();
            if (w == curIndex) fheight = fms.getHeight() - fms.getDescent();

            p = new Polygon();
            if(npages[w].hidden){
                y1 = TF_TOP - TF_BTN_HEIGHT;
                y2 = TF_TOP - 1;
                x1=x4-1;
                x2=x1+1;
                x3=x1;
                x4=x2;
                p.addPoint(x1,y1);
                p.addPoint(x1,y2);
                p.addPoint(x2,y2);
                p.addPoint(x2,y1);
                npages[w].poly = p;
    //          xStep++;
                continue;
            }
            try
            {
                sLabel = npages[w].label;
                if (w == curIndex)
                    labelWidth = fms.stringWidth(sLabel);
                else
                    labelWidth = fm.stringWidth(sLabel);

                if (npages[w].img != null) labelWidth += IMAGE_SIZE;

                if (iTabsPosition == TOP)
                {
                    y1 = TF_TOP - TF_BTN_HEIGHT;
                    y2 = TF_TOP - 1;
                }
                else
                {
                    y1 = r.height + TF_BOTTOM + 1;
                    y2 = r.height + TF_BOTTOM - TF_BTN_HEIGHT;
                }

                if (iTabsStyle == ROUNDED)
                {
                    x1 = x4 + 2;
                    x2 = x1 + labelWidth + 13;
                }
                else
                {
                    x1 = x2 - 7;
                    x2 = x1 + labelWidth + 28;
                }

                // check to see if this tab would draw too far
                if ( (x2 + 36 - TF_RIGHT) > r.width )
                    break;

                // draw the outside edge of the tab
                if (iTabsPosition == TOP)
                {
                    // if current tab, it extends further
                    if (w == curIndex)
                    {
                        y1 -= 3;
                        x1 -= 2;
                    }
                    g.setColor(Color.white);
                    g.drawLine(x1+2, y1, x2, y1);

                    // draw the border between tabs if not covered by the current one
                    g.drawLine(x1, y1+2, x1, y2);
                    x3 = x1;

                    g.drawLine(x1+1, y1+1, x1+1, y1+1);

                    g.setColor(Color.gray);
                    g.drawLine(x2, y1, x2, y2);
                    g.setColor(Color.black);
                    g.drawLine(x2+1, y1+2, x2+1, y2);
                    x4 = x2;
                }
                else
                {
                    if (iTabsStyle == SQUARE)
                    {
                        g.setColor(Color.gray);
                        g.drawLine(x1+9, y1, x2-9, y1);

                        g.setColor(Color.black);
                        // left \ slanted line
                        if (w == 0 || w == curIndex)
                        {
                            g.drawLine(x1, y2, x1+9, y1);
                            p.addPoint(x1, y2);
                        }
                        else
                        {
                            g.drawLine(x1+4, y1-9, x1+9, y1);
                            p.addPoint(x1+9, y2);
                            p.addPoint(x1+4, y1-9);
                        }
                        p.addPoint(x1+9, y1);
                        p.addPoint(x2-9, y1);

                        if ((w+xStep) == curIndex)
                        {
                            g.drawLine(x2-5, y1-9, x2-9, y1);
                            p.addPoint(x2-5, y1);
                            p.addPoint(x2-9, y2);
                        }
                        else
                        {
                            g.drawLine(x2, y2, x2-9, y1);
                            p.addPoint(x2, y2);
                        }

                        if (w == 1 || w == curIndex) p.addPoint(x1, y2);
                        else p.addPoint(x1+9, y2);
                    }
                    else
                    {
                        // if current tab, it extends further
                        if (w == curIndex)
                        {
                            y1 += 3;
                            x1 -= 2;
                        }
                        g.setColor(Color.white);
                        if (curIndex == (w + xStep))
                             g.drawLine(x1+2, y1, x2-2, y1);
                        else g.drawLine(x1+2, y1, x2, y1);

                        // draw the border between tabs if not covered by the current one
                        if (curIndex != (w - xStep))
                        {
                            g.drawLine(x1, y1-2, x1, y2);
                            x3 = x1;
                        }
                        else x3 = x1 + 1;

                        g.drawLine(x1+1, y1-1, x1+1, y1-1);

                        if (curIndex != (w + xStep))
                        {
                            g.setColor(Color.gray);
                            g.drawLine(x2, y1, x2, y2);
                            g.setColor(Color.black);
                            g.drawLine(x2+1, y1-2, x2+1, y2);
                            x4 = x2;
                        }
                        else x4 = x2 - 1;
                    }
                }

                // draw the inside edge of the tab
                if (w == curIndex)
                {
                    if (iTabsPosition == TOP) ++y2;
                    else --y2;
                    g.setColor(getBackground());
                    g.drawLine(x1+1, y2, x2, y2);
                    if (iTabsPosition == BOTTOM)
                        g.drawLine(x1+1, y2-1, x2, y2-1);

                    g.setFont(fSel);
                }
                else g.setFont(fReg);

                // if (iTabsPosition == TOP)
                if (iTabsStyle == ROUNDED)
                {
                    p.addPoint(x3-1, y2+1);
                    p.addPoint(x4+1, y2+1);
                    p.addPoint(x4+1, y1-1);
                    p.addPoint(x3+2, y1-1);
                    p.addPoint(x3-1, y1+2);
                    p.addPoint(x3-1, y2+1);
                }
                npages[w].poly = p;

                g.setColor(npages[w].color);
                Polygon p2 = justPolygon( p, iTabsPosition == TOP);
                g.fillPolygon(p2);

             // Boolean bool = (Boolean) vEnabled.elementAt(w);
             // if (bool.booleanValue())
                    g.setColor(caption);
             // else
             //     g.setColor(Color.gray);

                int dx = (npages[w].img==null)?0:IMAGE_SIZE;

                int xx = x1 + 8 + dx;
                int yy = y1 + 15;

                if (iTabsStyle == TOP)
                {
                }
                else
                if (iTabsStyle == ROUNDED)
                {
                  yy = y1-6;
                }
                else
                {
                  xx = x1+14+dx;
                  yy = y1-4;
                }

                int imgy = yy - fheight/2;


                if (npages[w].img != null)
                {
                  int imgH = npages[w].img.getHeight(this);
                  int imgW = npages[w].img.getWidth (this);
                  imgy = imgy - imgH/2 + 1;
                  g.drawImage (npages[w].img, xx - IMAGE_SIZE - 2, imgy, imgW, imgH, this);
                }

                g.drawString (sLabel, xx, yy);
            }
            catch (ArrayIndexOutOfBoundsException e) {}
            xStep=1;
        }

        // do I need to show arrows because there are too many tabs???
        if ( (firstVisibleTab > 0) || (w < sze) )
        {
            dbLeft.show();
            dbRight.show();
            if (firstVisibleTab > 0) dbLeft.enable();
            else dbLeft.disable();

            if (w < sze) dbRight.enable();
            else dbRight.disable();
        }
        else
        {
            dbLeft.hide();
            dbRight.hide();
        }
        g.setFont(f);
        g.setColor(c);

        if (mark && curIndex>=0) drawMark(g, npages[curIndex].poly);

        npages=null;
    }

    private void drawMark(Graphics g, Polygon r)
    {
       g.setColor(Color.black);
       Rectangle rr = r.getBoundingBox();
       rr.x += 3;
       rr.y += 3;
       rr.height -= 5;
       rr.width  -= 6;

       drawRect(g, rr.x, rr.y, rr.width, rr.height);
    }

    private Polygon justPolygon(Polygon p, boolean isTop)
    {
       Polygon p2=new Polygon();
       Rectangle r=p.getBoundingBox();
       int x,y,i;
       for(i=0;i<p.npoints;++i){
          x=p.xpoints[i];
          if(p.xpoints[i]>=r.x+r.width-1) x=p.xpoints[i]-1;
          if(p.xpoints[i]==r.x) x=p.xpoints[i]+2;

          y=p.ypoints[i];
          if(!isTop && p.ypoints[i]>=r.y+r.height-1) y=p.ypoints[i]-1;
          if(isTop  && p.ypoints[i]==r.y) y=p.ypoints[i]+2;
          p2.addPoint(x,y);
       }
       return p2;
    }

    public boolean keyDown(Event e, int key)
    {
      switch (key)
      {
        case Event.LEFT :
             {
               int x = curIndex ;
               if (x> 0 && mark) x--;
               else return false;
               for (; x>=0; x--)
               try {
                 NotebookPage np = (NotebookPage)pages.elementAt(x);
                 if(isHidden(x)) continue;
                 sendActionEvent(x, 0);
                 requestFocus();
                 break;
               }
               catch (ArrayIndexOutOfBoundsException ex) {}
             } break;
        case Event.RIGHT:
             {
               int size = pages.size(), x = curIndex;
               if (x < (size-1) && mark) x++;
               else return false;
               for (; x<size; x++)
               try {
                 NotebookPage np = (NotebookPage)pages.elementAt(x);
                 if(isHidden(x)) continue;
                 sendActionEvent(x, 0);
                 requestFocus();
                 break;
               }
               catch (ArrayIndexOutOfBoundsException ex) {}
             } break;
      }
      return super.keyDown(e, key);
    }

    boolean isHandle = true;

    public void setHandleEvent(boolean b) {
      isHandle = b;
    }

    public boolean handleEvent(Event evt)
    {
       switch (evt.id){
       case Event.MOUSE_MOVE:
       case Event.MOUSE_DOWN:
       case Event.MOUSE_ENTER:
       case Event.MOUSE_EXIT:
       case Event.MOUSE_DRAG: return true;
       case Event.MOUSE_UP:  if (!isHandle) return true;
           int sizeR = pages.size();
           NotebookPage np = null;
           for (int x = 0; x < sizeR; x++)
           try{
              np = (NotebookPage)pages.elementAt(x);
              if ( (np.poly != nullPoly) && np.poly.inside(evt.x, evt.y) )
              {
                  if(isHidden(x)) continue;
                  if (x == curIndex)
                  {
                    requestFocus();
                    return true;
                  }
                  sendActionEvent(x, 1);
                  break;
              }
           }
           catch (ArrayIndexOutOfBoundsException e) {}
           break;

         case Event.ACTION_EVENT:
           if (evt.target == dbLeft){
               if (--firstVisibleTab < 0) firstVisibleTab = 0;
               else repaint();
               return true;
           }
           else if (evt.target == dbRight){
              int sze = pages.size();
              if (++firstVisibleTab == sze) firstVisibleTab--;
              else repaint();
              return true;
           }
           break;
       }

       return super.handleEvent(evt);
    }

    public boolean lostFocus(Event e, Object o)
    {
      if (!(e.target instanceof Notebook)) return super.lostFocus(e, o);
      mark = false;
      repaint();
      return super.lostFocus(e, o);
    }

    public void setImage(int pos, String image)
    {
       NotebookPage np = (NotebookPage)pages.elementAt(pos);
       np.setImage(image);
       repaint();
    }

    private void sendActionEvent(int page, int mod)
    {
      Event e = new Event(this,
                          System.currentTimeMillis(),
                          Event.ACTION_EVENT,
                          page,0,0,mod,Integer.toString(page));
      mark = false;
      getParent().postEvent(e);
      repaint();
    }

    public void requestFocus()
    {
      super.requestFocus();
      mark = true;
      repaint();
    }

    public boolean mouseMove (Event e, int x, int y)
    {
      return true;
    }

    public boolean mouseExit (Event e, int x, int y)
    {
      return true;
    }

    public boolean mouseEnter (Event e, int x, int y)
    {
      return true;
    }

    public int countPages() {
      return pages.size();
    }

    public static void drawRect(Graphics gr, int x, int y, int w, int h)
    {
      drawVLine(gr, y, y+h, x);
      drawVLine(gr, y, y+h, x+w);
      drawHLine(gr, x, x+w, y);
      drawHLine(gr, x, x+w, y+h);
    }

    public static void drawHLine(Graphics gr, int x1, int x2, int y1)
    {
      int dx    = x2 - x1;
      int count = dx/2 + dx%2;
      for (int i=0; i<count; i++)
      {
        gr.drawLine (x1, y1, x1, y1);
        x1+=2;
      }
      gr.drawLine (x2, y1, x2, y1);
    }

    public static void drawVLine(Graphics gr, int y1, int y2, int x1)
    {
      int dy    = y2 - y1;
      int count = dy/2 + dy%2;;
      for (int i=0; i<count; i++)
      {
        gr.drawLine (x1, y1, x1, y1);
        y1+=2;
      }
      gr.drawLine (x1, y2, x1, y2);
    }
}
