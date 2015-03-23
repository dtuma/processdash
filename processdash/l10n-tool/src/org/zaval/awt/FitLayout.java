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

public class FitLayout
implements LayoutManager
{
  protected int gapX, gapY;
  protected int gapW, gapH;

  public FitLayout()
  {
     this(0,0,0,0);
  }

  public FitLayout(int gapX, int gapY, int gapW, int gapH)
  {
     this.gapX=gapX;
     this.gapY=gapY;
     this.gapW=gapW;
     this.gapH=gapH;
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
     int i,wx=0,wy=0;
     Component[] cc = parent.getComponents();
     for(i=0;i<cc.length;++i){
        Dimension d2=cc[i].preferredSize();
        if(d2.width==0 || d2.height==0) d2 = cc[i].size();
        if(wx<d2.width) wx=d2.width;
        if(wy<d2.height) wy=d2.height;
     }
     Insets is = parent.insets();
     return new Dimension(wx+gapX+gapW/*+is.left+is.right*/,
                          wy+gapY+gapH/*+is.top+is.bottom*/);
  }

  public Dimension minimumLayoutSize(Container parent)
  {
     int i,wx=0,wy=0;
     Component[] cc = parent.getComponents();
     for(i=0;i<cc.length;++i){
        Dimension d2=cc[i].minimumSize();
        if(d2.width==0 || d2.height==0) d2 = cc[i].size();
        if(wx<d2.width) wx=d2.width;
        if(wy<d2.height) wy=d2.height;
     }
     Insets is = parent.insets();
     return new Dimension(wx+gapX+gapW/*+is.left+is.right*/,
                          wy+gapY+gapH/*+is.top+is.bottom*/);
  }

  public void layoutContainer(Container parent)
  {
     int i=0,j=-1;
     Rectangle r=parent.bounds();
     Component[] c=parent.getComponents();
     int gx = gapX, gy = gapY, gw = gapW, gh = gapH;

     Insets is = parent.insets();
     for(i=0;i<c.length;++i){
        if(gapX==-1){
           Dimension v = c[i].preferredSize();
           if(v.width >r.width ) v.width = r.width;
           if(v.height>r.height) v.height= r.height;
           gx = gw = (r.width - v.width) /2;
           gy = gh = (r.height- v.height)/2;
        }

        c[i].move(gx + is.left,gy/* + is.top*/);
        c[i].resize(
            r.width - gx - gw  /*- is.right - is.left*/,
            r.height- gy - gh  /*- is.bottom- is.top*/);
        if(c[i].isVisible())
           if(j>=0) c[i].hide();
           else j=i;
     }
  }
}
