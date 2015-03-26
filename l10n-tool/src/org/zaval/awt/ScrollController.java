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

import java.util.*;
import java.awt.*;

public class ScrollController
extends org.zaval.awt.util.Metrics
{
  public static final int SCROLL_SIZE = 16;

  private int           hValue = 0;
  private int           vValue = 0;
  private ScrollArea    area;
  private ScrollObject  sobj;
  private Dimension     pages = new Dimension (0,0);
  private Dimension     lines = new Dimension (1,1);

  public ScrollController() {
  }

  public ScrollController(ScrollArea a, ScrollObject o) {
    setScrollArea  (a);
    setScrollObject(o);
  }

  protected int normilize(Scrollbar sb, int prev, int dt)
  {
    int v = prev + dt;

    if (v < 0) dt -= v;
    else
    if (v >= sb.getMaximum()) dt -= (v - sb.getMaximum());

    return dt;
  }

  protected int next(Scrollbar sb, int prev, int id, int times)
  {
    if (sb == null || !sb.isVisible()) return 0;

    int dt = 0;
    int or = sb.getOrientation();
    switch (id)
    {
      case Event.SCROLL_PAGE_DOWN: {
         if (or == Scrollbar.HORIZONTAL) dt = getHPageSize(id);
         else                            dt = getVPageSize(id);
      } break;
      case Event.SCROLL_LINE_UP  : {
         if (or == Scrollbar.HORIZONTAL) dt = -getHLineSize(id);
         else                            dt = -getVLineSize(id);
      } break;
      case Event.SCROLL_ABSOLUTE  : {
         dt = sb.getValue() - prev;
      } break;
      case Event.SCROLL_LINE_DOWN: {
         if (or == Scrollbar.HORIZONTAL) dt = getHLineSize(id);
         else                            dt = getVLineSize(id);
      } break;
      case Event.SCROLL_PAGE_UP  : {
         if (or == Scrollbar.HORIZONTAL) dt = -getHPageSize(id);
         else                            dt = -getVPageSize(id);
      } break;
    }

    dt *= times;
    dt = normilize(sb, prev, dt);

    return dt;
  }

  public boolean handle(Event e, int times)
  {
     Scrollbar hBar = area.getHBar();
     Scrollbar vBar = area.getVBar();

     if (e.target != hBar&&
         e.target != vBar  ) return false;

     boolean b = (e.id == Event.SCROLL_PAGE_UP  ||
                  e.id == Event.SCROLL_PAGE_DOWN||
                  e.id == Event.SCROLL_ABSOLUTE ||
                  e.id == Event.SCROLL_LINE_UP  ||
                  e.id == Event.SCROLL_LINE_DOWN  );

     if (!b) return false;

     return handle((Scrollbar)e.target, e.id, times);
  }

  protected boolean handle(Scrollbar bar, int id, int times)
  {
     if (bar == null || !bar.isVisible()) return false;
     Scrollbar hBar = area.getHBar();
     Scrollbar vBar = area.getVBar();

     int dx = 0, dy = 0;
     if (bar == hBar)
     {
       dx = next(hBar, hValue, id, times);
       hValue += dx;
       if (dx != 0) hBar.setValue(hValue);
     }
     else
     if (bar == vBar)
     {
       dy = next(vBar, vValue, id, times);
       vValue += dy;
       if (dy != 0) vBar.setValue(vValue);
     }

     if (dx != 0 || dy != 0)
     {
       ScrollObject sobj = getScrollObject();
       Point pos = sobj.getSOLocation();
       sobj.setSOLocation(pos.x - dx, pos.y - dy);
     }

     return true;
  }

  public void clear() {
    hValue = 0;
    vValue = 0;
  }

  public int getMaxHorScroll()
  {
    Scrollbar hBar = area.getHBar();
    Scrollbar vBar = area.getVBar();
    if (hBar == null) return -1;
    Dimension  r  = sobj.getSOSize();
    Dimension  d  = area.getSASize();
    int        wx = d.width;
    int        wy = d.height;
    boolean    needVBar = false;
    boolean b1 = (r.width  >  wx);

    if (vBar != null)
     if (r.height >  wy || (b1 && r.height > (wy-SCROLL_SIZE)))
     {
       needVBar = true;
       wx -= SCROLL_SIZE;
     }

    boolean  b2 = (needVBar && r.width  > wx);
    if (b1 || b2)
    {
      hValue = 0;
      int max = (r.width -  wx);
      return max + 1;
    }

    return -1;
  }

  public int getMaxVerScroll()
  {
    Scrollbar hBar = area.getHBar();
    Scrollbar vBar = area.getVBar();
    if (vBar == null) return -1;
    Dimension  r  = sobj.getSOSize();
    Dimension  d  = area.getSASize();
    int        wx = d.width;
    int        wy = d.height;
    boolean    needHBar = false;

    boolean  b1 = (r.height  >  wy);
    if (hBar != null)
     if (r.width > wx || (b1 && r.width > (wx-SCROLL_SIZE)))
     {
       wy -= SCROLL_SIZE;
       needHBar = true;
     }

    boolean  b2 = (needHBar && r.height  > wy);
    if (b1 || b2)
    {
      vValue = 0;
      int max = (r.height -  wy);
      return max + 1;
    }

    return -1;
  }

  public ScrollArea getScrollArea() {
    return area;
  }

  public ScrollObject getScrollObject() {
    return sobj;
  }

  public void setScrollArea(ScrollArea a) {
    area = a;
    invalidate();
  }

  public void setScrollObject(ScrollObject o) {
    sobj = o;
    invalidate();
  }

  public void recalc()
  {
    clear();
    Scrollbar hBar = area.getHBar();
    Scrollbar vBar = area.getVBar();

    Dimension d = area.getSASize();
    pages.width  = d.width;
    pages.height = d.height;
    if (hBar != null && hBar.isVisible())
    {
      pages.width -= SCROLL_SIZE;
      lines.width  = pages.width/4;
    }
    if (vBar != null && vBar.isVisible())
    {
      pages.height -= SCROLL_SIZE;
      lines.height  = pages.height/4;
    }
  }

  public void setV (int id, int times) {
    clear();
    handle(area.getVBar(),  id, times);
  }

  public void setH (int id, int times) {
    clear();
    handle(area.getHBar(), id, times);
  }

  public int getVPageSize(int id) {
    return pages.height;
  }

  public int getHPageSize(int id) {
    return pages.width;
  }

  public int getHLineSize(int id) {
    return lines.width;
  }

  public int getVLineSize(int id) {
    return lines.height;
  }

  public static Dimension calcPreferredSize(Container parent)
  {
    Component[] c = parent.getComponents();
    int maxx = 0;
    int maxy = 0;

    for (int i = 0; i < c.length; i++)
    {
      if (!c[i].isVisible()) continue;
      Rectangle r = c[i].bounds();
      int mx = r.x + r.width;
      int my = r.y + r.height;
      if (maxx < mx) maxx = mx;
      if (maxy < my) maxy = my;
    }
    return new Dimension(maxx+5, maxy+5);
  }
}
