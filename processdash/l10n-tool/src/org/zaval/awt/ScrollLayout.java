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
import java.io.*;

public class ScrollLayout
implements LayoutManager
{
   protected Component hs, vs, stubb, center;

   public ScrollLayout() {
     super();
   }

   public void  addLayoutComponent(String s, Component c)
   {
      if (s != null)
      {
        if (s.equals("East"))  vs = c;
        else
        if (s.equals("South")) hs = c;
        else
        if (s.equals("Center")) center = c;
        else
        if (s.equals("Stubb"))
        {
          stubb = c;
          stubb.resize(0,0);
          stubb.setBackground(Color.lightGray);
          return;
        }
      }
   }

   protected Insets getInsets(Container c) {
     return c.insets();
   }

   public void removeLayoutComponent(Component c)
   {
     if (c      == vs) vs     = null;
     if (c      == hs) hs     = null;
     if (center == c ) center = null;
     if (stubb  == c ) stubb  = null;
   }

   public Dimension  minimumLayoutSize(Container target) {
     return preferredLayoutSize(target);
   }

   public Dimension preferredLayoutSize(Container target)
   {
        Dimension dim = new Dimension(0, 0);

        boolean hb = (hs != null && hs.isVisible());
        boolean vb = (vs != null && vs.isVisible());

        if (vb)
        {
            Dimension d = vs.preferredSize();
            dim.width += d.width;
            dim.height = Math.max(d.height, dim.height);
        }

        if ((center != null) && center.isVisible())
        {
            Dimension d = center.preferredSize();
            dim.width += d.width;
            dim.height = Math.max(d.height, dim.height);
        }

        if (hb)
        {
            Dimension d = hs.preferredSize();
            dim.width = Math.max(d.width, dim.width);
            dim.height += d.height;
        }

        Insets insets = getInsets(target);
        dim.width  += insets.left + insets.right;
        dim.height += insets.top + insets.bottom;
        return dim;
    }


   public void layoutContainer(Container parent)
   {
      Insets    insets = getInsets(parent);
      Rectangle target = parent.bounds();

      int top    = insets.top;
      int bottom = target.height - insets.bottom;
      int left   = insets.left;
      int right  = target.width - insets.right;
      int s      = ScrollController.SCROLL_SIZE;

      boolean hb = (hs != null && hs.isVisible());
      boolean vb = (vs != null && vs.isVisible());

      if (hb) bottom -= s;
      if (vb) right  -= s;

      if (hb)
        hs.reshape(left, bottom, right, s);

      if (vb)
        vs.reshape(right, top, s, bottom);

      if ((center != null) && center.isVisible())
        layoutCenter(parent, left, top, right - left, bottom - top);

      if (hb && vb)
      {
        if (stubb != null)
          stubb.reshape(right, bottom, s, s);
      }
      else
      {
        if (stubb != null) stubb.resize(0,0);
      }
   }

   public Dimension getAreaSize(Component c)
   {
     Rectangle b = c.bounds();
     Dimension d = new Dimension(b.width, b.height);

     if (hs.isVisible())
     {
       Rectangle r = hs.bounds();
       if ((b.y + b.height) < r.y) d.height += r.height;
     }

     if (vs.isVisible())
     {
       Rectangle r = vs.bounds();
       if ((b.x + b.width) < r.x) d.width += r.width;
     }

     return d;
   }

   protected void layoutCenter(Container cont, int x, int y, int w, int h) {
     center.reshape(x, y, w, h);
   }
}
