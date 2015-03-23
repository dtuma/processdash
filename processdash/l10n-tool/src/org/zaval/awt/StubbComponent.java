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

public class StubbComponent
extends Canvas
{
//   private Image img;
   private int   size = 16;

   public Dimension preferredSize() {
     return new Dimension(size,size);
   }

   public StubbComponent() {
     setBackground(Color.lightGray);
   }

   public void setSize(int s)
   {
     if (size == s) return;
     size = s;
     invalidate();
   }

   public void paint(Graphics g)
   {
    /*  Dimension d = size();
      if (img == null || img.getWidth(this) != d.width || !isValid())
      {
        img = createImage(d.width, d.width);
        Graphics gr = img.getGraphics();    */
        draw(g);
  /*    }
      g.drawImage(img,0,0, this);*/
   }

   protected void draw(Graphics g)
   {
     Dimension d = size();
     g.setColor(Color.lightGray);
     g.fillRect(0,0, d.width, d.height);

     int stap = size/3;
     int x = stap;
     int y = stap;
     for (;x<=(d.width*2); x+=stap,y+=stap)
     {
       g.setColor(Color.white);
       g.drawLine(x,0,0,y);

       g.setColor(Color.gray);
       g.drawLine(x+1,0,0,y+1);
     }
   }

   public boolean mouseEnter (Event e, int x, int y)
   {
     return true;
   }

   public boolean mouseExit (Event e, int x, int y)
   {
     return true;
   }
}
