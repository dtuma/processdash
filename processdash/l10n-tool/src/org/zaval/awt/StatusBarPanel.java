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

public class StatusBarPanel
extends Panel
implements LayoutManager
{
  private Vector elements = new Vector();

  public StatusBarPanel(){
    setLayout(this);
  }

  public void addStatusBarComponent(Component c, int p)
  {
    StatusBarElement e = new StatusBarElement(c, p);
    elements.addElement(e);
    add(e);
  }

  public void addLayoutComponent(String name, Component comp) {
  }

  public void layoutContainer(Container parent)
  {
     Dimension     pd    = preferredLayoutSize(parent);
     Dimension     d     = parent.size();
     int           size  = elements.size();
     int           width = d.width;
     if (size > 1) d.width -= (2*(size-1));

     int x = 0;
     for (int i=0; i<size; i++){
        StatusBarElement c  = (StatusBarElement)elements.elementAt(i);
        Dimension        ps = c.preferredSize();
        if (i > 0) x += 2;
        int perc = c.getPercent();

        if (i == (size-1)) ps.width = width - x;
        else if (perc > 0) ps.width = (d.width*perc)/100;

        c.move(x, d.height - pd.height);
        c.resize(ps.width, pd.height);
        x += ps.width;
     }
  }

  public Dimension minimumLayoutSize(Container parent)
  {
    return preferredLayoutSize(parent);
  }

  public Dimension preferredLayoutSize(Container parent)
  {
    Dimension d    = new Dimension (0,0);
    Dimension pd   = parent.size();
    int       size = elements.size();
    if (size > 1) pd.width -= (2*(size-1));

    for (int i=0; i<size; i++){
      StatusBarElement c    = (StatusBarElement)elements.elementAt(i);
      Dimension        d1   = c.preferredSize();
      int              perc = c.getPercent();
      if (perc > 0) d1.width = (pd.width*perc)/100;
      d.width += d1.width;
      if (d1.height > d.height) d.height = d1.height;
    }
    return d;
  }

  public void removeLayoutComponent(Component comp)
  {
  }
}
