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

public class StatusBarStubbElement
extends StatusBarElement
{
   public StatusBarStubbElement(Component c, int p, Dimension pref)
   {
     super(c, p, pref);
   }

   public StatusBarStubbElement(Component c, int p)
   {
      this(c,p,null);
   }

   public void paint(Graphics gr)
   {
      super.paint(gr);
      Dimension d = size();

      int h  = (d.height*2)/3;
      int c  = h/5;
      int xx = d.width  - 2;
      int yy = d.height - 2;
      int y  = yy - 2;
      int x  = xx - 2;
      if (c % 5 > 0) c++;

      for (int i=0; i<c; i++){
         gr.setColor(Color.gray);
         gr.drawLine(x, yy, xx, y);
         x-=2;
         y-=2;
         gr.setColor(Color.white);
         gr.drawLine(x, yy, xx, y);
         x-=2;
         y-=2;
      }

      gr.setColor(getBackground());
      gr.drawLine(x+2, yy+1, xx, yy+1);
      gr.drawLine(xx+1, y+2, xx+1, yy+1);
   }
}
