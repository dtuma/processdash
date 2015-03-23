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

public class ResizeLayout
implements LayoutManager
{
  private int fix=-1;

  public ResizeLayout ()
  {
  }

  public Point location(int xx, int yy)
  {
    return new Point(xx<fix?0:2,0);
  }

  public void setSeparator(int sef, Container parent)
  {
      fix=sef;
      layoutAll(parent);
  }

  private void layoutAll(Container c)
  {
     int i;
     LayoutManager l=c.getLayout();
     if(l!=null) l.layoutContainer(c);
     Component[] o=c.getComponents();
     c.repaint();

     for(i=0;i<o.length;++i)
        if(o[i] instanceof Container) layoutAll((Container)o[i]);
  }

// -----------------------------------

  public void addLayoutComponent(String name, Component comp)
  {
  }

  public void removeLayoutComponent(Component comp)
  {
  }

  public Dimension preferredLayoutSize(Container parent)
  {
      int i;
      Panel left=null,right=null;
      Component[] obj=parent.getComponents();
      for(i=0;i<obj.length;++i){
         if(obj[i] instanceof Panel){
            if(left==null) left=(Panel)obj[i];
            else if(right==null) right=(Panel)obj[i];
            else break;
         }
      }

      Dimension d1=left.preferredSize();
      if(fix!=-1) d1.width = fix;
      Dimension d2=right.preferredSize();
      return new Dimension(d1.width+d2.width+5,Math.max(d1.height,d2.height));
  }

  public Dimension minimumLayoutSize(Container parent)
  {
      int i;
      Panel left=null,right=null;
      Component[] obj=parent.getComponents();
      for(i=0;i<obj.length;++i){
         if(obj[i] instanceof Panel){
            if(left==null) left=(Panel)obj[i];
            else if(right==null) right=(Panel)obj[i];
            else break;
         }
      }

      Dimension d1=left.minimumSize();
      if(fix!=-1) d1.width = fix;
      Dimension d2=right.minimumSize();
      return new Dimension(d1.width+d2.width+5,Math.max(d1.height,d2.height));
  }

  public void layoutContainer(Container parent)
  {
      int i;
      Panel left=null,right=null;
      Component rl=null;
      Component[] obj=parent.getComponents();
      for(i=0;i<obj.length;++i){
         if(obj[i] instanceof Panel){
            if(left==null) left=(Panel)obj[i];
            else if(right==null) right=(Panel)obj[i];
            else break;
         }else if(rl==null) rl=obj[i];
      }

      Rectangle r=parent.bounds();
      int wx1=fix==-1 ? left.preferredSize().width : fix;
      int wx2=r.width-wx1-15;
      left.reshape(5,5,wx1,r.height-10);
      right.reshape(10+wx1,5,wx2,r.height-10);
      rl.reshape(5+wx1,5,5,r.height-10);
  }
}
