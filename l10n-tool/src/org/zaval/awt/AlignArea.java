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

import java.awt.Insets;
import java.awt.Dimension;
import java.awt.Rectangle;

public class AlignArea
extends Align
{
  public static final int INSIDE  = 1;
  public static final int OUTSIDE = 2;

  private int mode   = INSIDE;
  private int sx     = 0;
  private int sy     = 0;
  private Dimension size    = new Dimension(0,0);
  private Insets    insets  = new Insets(0,0,0,0);
  private boolean   isValid = false;
  private Rectangle rect;

  public void setAlign(int a)
  {
    if (getAlign() == a) return;
    super.setAlign(a);
    invalidate();
  }

  public void setSize(Dimension d)
  {
    if (d != null && size != null)
    {
      if (d.width  == size.width &&
          d.height == size.height  ) return ;
    }

    invalidate();
    if (d == null) size = null;
    else           size = new Dimension(d.width, d.height);
  }

  public Dimension getSize() {
    if (size == null) return null;
    return new Dimension(size.width, size.height);
  }

  public void setSizeAlignObj(Dimension d)
  {
    if (sx == d.width && sy == d.height) return;
    invalidate();
    sx = d.width;
    sy = d.height;
  }

  public Dimension getSizeAlignObj() {
    return new Dimension(sx, sy);
  }

  public void setInsets(Insets i)
  {
    if (i != null &&
        i.top    == insets.top  &&
        i.left   == insets.left &&
        i.right  == insets.right&&
        i.bottom == insets.bottom ) return;

    invalidate();
    if (i == null) insets = null;
    else           insets = new Insets(i.top, i.left, i.bottom, i.right);
  }

  public Insets getInsets() {
    return insets;
  }

  public void setMode(int m)
  {
    if (mode == m) return;
    invalidate();
    mode = m;
  }

  public int getMode() {
    return mode;
  }

  public Rectangle getAlignRectangle()
  {
     if (isValid())
     {
       if (rect == null) return null;
       return new Rectangle(rect.x, rect.y, rect.width, rect.height);
     }

     recalc();
     Dimension s = getSize();

     s.width  -= (insets.left + insets.right);
     s.height -= (insets.top  + insets.bottom);

     int       wx = getWidth (sx, size);
     int       wy = getHeight(sy, size);
     int       xx = size.width  - wx;
     int       yy = size.height - wy;
     int       a  = getAlign();
     Rectangle r  = new Rectangle (xx/2, yy/2, wx, wy);

     if ((a&LEFT) > 0)
       r.x = insets.left;
     else
       if ((a&RIGHT) > 0) r.x = xx - insets.right;

     if ((a&TOP) > 0) r.y = insets.top;
     else
       if ((a&BOTTOM) > 0) r.y = yy - insets.bottom;

     rect = r;
     validate();
     return r;
  }

  public boolean isBelongArea(int x, int y) {
    return isBelongArea(this, x, y);
  }

  protected int getWidth(int s, Dimension size) {
    return s;
  }

  protected int getHeight(int s, Dimension size) {
    return s;
  }

  protected void validate() {
    isValid = true;
  }

  protected void recalc() {
  }

  public void invalidate() {
    isValid = false;
  }

  public boolean isValid() {
    return isValid;
  }

  public static boolean isBelongArea(AlignArea a, int x, int y)
  {
     Rectangle r = a.getAlignRectangle();
     if (r == null) return false;

     switch (a.getMode())
     {
       case AlignArea.INSIDE : return  r.inside(x, y);
       case AlignArea.OUTSIDE: return !r.inside(x, y);
     }
     return false;
  }
}
