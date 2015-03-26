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

public class Toolbar
extends Panel
implements LayoutManager
{
   private Vector v = new Vector();

   public Toolbar()
   {
      super();
      setLayout(this);
   }

   public void add(int id, Component button)
   {
      add(button);
      while(v.size()<=id) v.addElement("");
      v.setElementAt(button, id);
   }

   public boolean action(Event e, Object o)
   {
      if(e.target instanceof SpeedButton){
         int id = v.indexOf(e.target);
         getParent().postEvent(new Event(this,e.ACTION_EVENT,Integer.toString(id)));
         return true;
      }
      return false;
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

   public Dimension minimumLayoutSize(Container parent)
   {
      return preferredLayoutSize(parent);
   }

   public Dimension preferredLayoutSize(Container parent)
   {
      int maxx = 0, maxy = 0, j;
      Component[] v = getComponents();
      for(j=0;j<v.length;++j){
         Component c = v[j];
         Dimension d = c.preferredSize();
         maxx += /* 1 + */ d.width;
         maxy = Math.max(maxy, d.height);
      }
      return new Dimension(maxx/*-1*/,maxy);
   }

   public void layoutContainer(Container parent)
   {
      int x,y,w,h,j;
      Dimension real = parent.size();
      Dimension want = preferredLayoutSize(parent);
      Insets p_i  = parent.insets();

      if(real.width==0 || real.height==0) return;
   // double cfx  = (double)real.width/(double)want.width;

      x = p_i.left;
      y = p_i.top;

      Component[] v = getComponents();
      for(j=0;j<v.length;++j){
         Component c = v[j];
         Dimension d = c.preferredSize();
         w = d.width; //(int)((double)d.width * cfx);
         h = real.height;

         c.resize(w,h);
         c.move(x + p_i.left, y + p_i.top);
         x += w /* + 1 */;
      }
   }

   public void setObjectsSize(Dimension d)
   {
      int j;
      for(j=0;j<v.size();++j){
         Component c = (Component)v.elementAt(j);
         if(c instanceof SpeedButton){
            SpeedButton cc = (SpeedButton)c;
            cc.setImageSize(d);
         }
      }
   }

   public int count()
   {
      return v.size();
   }

   public void setEnabled(int j, boolean state)
   {
      if (j >= v.size()) return;
      Object o = v.elementAt(j);
      if (!(o instanceof Component)) return;
      Component c = (Component) o;
      if(state) c.enable();
      else c.disable();
   }
}
