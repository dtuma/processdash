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

public class Resizer
extends Canvas
{
   private int startx;
   private int oldrg;

   private boolean drag=false;
   private boolean state = false;
   private boolean _enable;

   public Resizer()
   {
      super();
      startx=oldrg=0;
      _enable = isEnabled();
   }

   public void enable()
   {
      enable(true);
   }

   public void disable()
   {
      enable(false);
   }

   public void enable(boolean e)
   {
      if(_enable==e)return;
      super.enable(_enable = e);
      if(!_enable)
      {
         drag=false;
         startx=oldrg=0;
      }
      repaint();
   }

   public void paint(Graphics gr)
   {
      if(_enable)
      {
         Rectangle r=bounds();
         int x=0,y=0;
         int w=r.width-2*x-1, h=r.height-2*y-1;
         gr.setColor(Color.lightGray);
         gr.fillRect(x,y,x+w,y+h);
         gr.setColor(Color.white);
         gr.drawLine(x,y,x+w,y);
         gr.drawLine(x,y,x,y+h);
         gr.setColor(Color.black);
         gr.drawLine(x,y+h,x+w,y+h);
         gr.drawLine(x+w,y,x+w,y+h);
      }
   }

   private void paintLine(Component c, int x)
   {
      for(int i=0;i<bounds().width;i++,x++)
         drawVLineOnComponent(c, bounds().y, bounds().height+bounds().y, x, Color.darkGray);
   }

   private static void drawVLineOnComponent(Component c, int y1, int y2, int x, Color col)
   {
      if (c == null) return;
      Rectangle d = c.bounds();
      if (d.height <= y2) y2 = d.height - 1;

      Component lc = getNextBottomChild(c, y1 + 1, x);
      int yy1, yy2;
      while (lc != null && y1 < y2)
      {
         Rectangle lr = lc.bounds();
         yy1 = y1 - lr.y;
         if (yy1 < 0) yy1 = 0;

         yy2 = y2 - lr.y;
         if (yy2 >= lr.height) yy2 = lr.height-1;

         int xx = x - lr.x;
         if (yy2 <= yy1) break;

         if (lr.y > y1)
         {
            Graphics  g  = c.getGraphics();
            Color clr = g.getColor();
            g.setColor(col);
            g.setXORMode(c.getBackground());
            g.drawLine(x,y1,x,lr.y);
            g.setColor(clr);
            g.setPaintMode();
            g.dispose();
         }

         drawVLineOnComponent(lc, yy1, yy2, xx, col);
         y1 = (lr.y + lr.height);
         lc = getNextBottomChild(c, y1, x);
      }

      if (y1 < y2)
      {
         Graphics g  = c.getGraphics();
         Color clr = g.getColor();
         g.setColor(col);
         g.setXORMode(c.getBackground());
         g.drawLine(x,y1,x,y2);
         g.setColor(clr);
         g.setPaintMode();
         g.dispose();
      }
   }

   private static Component getNextBottomChild(Component parent, int y, int x)
   {
      if (!(parent instanceof Container)) return null;

      Component c = getComponentAtFix((Container)parent, x, y); //parent.getComponentAt(x, y);
      if (c != null && c != parent) return c;

      Component[] comps = ((Container)parent).getComponents();
      Component   find  = null;
      int         fy    = Integer.MAX_VALUE;
      for (int i=0; i<comps.length; i++)
      {
         Rectangle r = comps[i].bounds();
         if (x < r.x  || x > (r.x + r.width)) continue;

         if (r.y < fy && r.y > y)
         {
            fy   = r.y;
            find = comps[i];
         }
      }
      return find;
   }

   private static Component getComponentAtFix(Container top, int x, int y)
   {
      Component[] c = top.getComponents();
      Vector      v = new Vector();
      for (int i=0; i < c.length; i++)
      {
         if (!c[i].isVisible()) continue;
         Rectangle b = c[i].bounds();
         if (b.inside(x, y)) v.addElement(c[i]);
      }

      if (v.size() > 0)
      {
         return (Component)v.elementAt(0);
      }
      return null;
   }


   private void setCursor0(int c)
   {
      Component f = this;
      while((f!=null) && !(f instanceof Frame)) f = f.getParent();
      if(f instanceof Frame){
         ((Frame)f).setCursor(c);
      }
   }

   private void resizeme(int x)
   {
      Rectangle r     = bounds();
      ResizeLayout rl=(ResizeLayout)getParent().getLayout();
      Rectangle rp    = getParent().bounds();

      int       pos   = r.x + x - startx -r.width;
      int       left  = 0;
      int       right = rp.width - 2*r.width + rp.x;

      if (pos > right) pos = right;
      if (pos < left ) pos = left;

      rl.setSeparator(pos,getParent());

      oldrg = startx = 0;
   }


   public boolean mouseEnter(Event ev,int x,int y)
   {
      if(_enable)
      {
         setCursor0(Frame.E_RESIZE_CURSOR);
         return true;
      }
      return super.mouseEnter(ev,x,y);
   }

   public boolean mouseExit(Event ev, int x, int y)
   {
      if(_enable)
      {
         if(drag)
         {
            paintLine(getParent(),oldrg+bounds().x-startx);
            drag = false;
         }
         setCursor0(Frame.DEFAULT_CURSOR);
         return true;
      }
      return super.mouseExit(ev, x, y);
   }

   public boolean mouseDown(Event ev, int x, int y)
   {
      if(_enable && inside(x,y) && ev.modifiers==0)
      {
         if (drag) return super.mouseDown(ev,x,y);
         startx=x;
         oldrg = x;
         state = drag = true;
         paintLine(getParent(), oldrg+bounds().x-startx);
         setCursor0(Frame.E_RESIZE_CURSOR);
         return true;
      }
      return super.mouseDown(ev,x,y);
   }

   public boolean mouseUp(Event ev, int x, int y)
   {
      if(_enable)
      {
         if(state)
         {
            paintLine(getParent(), oldrg+bounds().x-startx);
            resizeme(x);
            state = drag = false;
         }
         return true;
      }
      return super.mouseUp(ev,x,y);
   }

   public boolean mouseDrag(Event ev, int x, int y)
   {
      if(_enable)
      {
         if(state)
         {
            if(oldrg==x) return super.mouseDrag(ev,x,y);
            if(drag) paintLine(getParent(), oldrg+bounds().x-startx);
            else drag = true;
            paintLine(getParent(), x+bounds().x-startx);
            oldrg = x;
         }
         return true;
      }
      return super.mouseDrag(ev,x,y);
   }

   public static void debug(String s)
   {
      System.out.println(s);
   }
}
