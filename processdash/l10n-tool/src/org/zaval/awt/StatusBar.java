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

public class StatusBar
extends Panel
implements LayoutManager
{
  public static final int FULL = 1;

  private Insets insets = new Insets(2,1,0,-1);
  private int    hgap   = 2;
  private int    fill   = 0;

  public StatusBar(){
    setLayout(this);
  }

  public int getFill() {
    return fill;
  }

  public void setFill(int f)
  {
    if (fill == f) return;
    fill = f;
    invalidate();
  }

  public void addLayoutComponent(String name, Component comp)
  {
  }

  public void layoutContainer(Container parent)
  {
    Dimension   ds = parent.size();
    Component[] cc = parent.getComponents();
    int         height = getActualHeight(parent);
    int[]       widths = getActualWidths(parent);

    int x = insets.left;
    int y = insets.top;
    for(int j=0; j<cc.length; ++j){
       cc[j].move   (x, y);
       cc[j].resize (widths[j], height);
       x += (widths[j] + hgap);
    }
  }

  public Dimension minimumLayoutSize(Container parent) {
    return preferredLayoutSize(parent);
  }

  public Dimension preferredLayoutSize(Container parent)
  {
    int[] widths = getActualWidths(parent);
    int   height = getActualHeight(parent) + insets.top + insets.bottom;
    int   width  = insets.left + insets.right + hgap * (widths.length-1);
    for (int i=0; i<widths.length; i++) width += widths[i];
    return new Dimension(width, height);
  }

  public void removeLayoutComponent(Component comp)
  {
  }

  protected int getActualHeight(Container parent)
  {
    Component[] cc = parent.getComponents();
    int         ah = 0;
    for(int j=0;j<cc.length;++j){
      Dimension d = cc[j].preferredSize();
      if (ah < d.height) ah = d.height;
    }
    return ah;
  }

  protected int[] getActualWidths(Container parent)
  {
    Dimension   ds = parent.size();
    Component[] cc = parent.getComponents();
    int         aw = 0, j=0;
    int[]       widths = new int[cc.length];
    int         xx     = insets.left + hgap*(cc.length-1);

    ds.width  -= (insets.left + insets.right + hgap*(cc.length-1));
    ds.height -= (insets.top  + insets.bottom);

    for(j=0;j<cc.length;++j){
      Dimension d = cc[j].preferredSize();

      if(!(cc[j] instanceof StatusBarElement)){
        widths[j] = d.width;
        aw += d.width;
        continue;
      }

      if(cc[j] instanceof StatusBarElement){
        StatusBarElement e = (StatusBarElement)cc[j];
        if (e.getPercent()==0){
          aw += d.width;
          widths [j] = d.width;
        }
      }
    }

    ds.width -= aw;
    for(j=0;j<cc.length;++j){
      xx += widths[j];
      if(cc[j] instanceof StatusBarElement){
        StatusBarElement e = (StatusBarElement)cc[j];
        int perc = e.getPercent();
        if (perc == 0) continue;
        widths[j] = (ds.width*perc)/100;
      }
    }

    if (getFill()==FULL){
      xx -= widths[cc.length-1];
      widths[cc.length-1] = ds.width - xx - insets.right;
    }
    return widths;
  }
}
