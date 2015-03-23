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

public class BorderedPanel
extends Panel
{
    public static final int NONE   = 0;
    public static final int RAISED = 1;
    public static final int SUNKEN = 2;
    public static final int ETCHED = 3;
    public static final int RAISED2= 4;

    protected int    type     = RAISED;
    protected int    hideMode = 0;
    protected Insets insets = new Insets(2,2,2,2);
    protected static Color [] colors = { Color.white, Color.gray };

    public BorderedPanel()  {
      this(ETCHED);
    }

    public BorderedPanel(int type) {
      this.type = type;
    }

    public void setType(int t)
    {
      if (type == t) return;
      type = t;
      repaint();
    }

    public void setHideMode(int m) {
      if (hideMode == m) return;
      hideMode = m;
    }

    public Insets getInsets() {
      return insets;
    }

/*    public Insets insets() {
      return insets;
    }*/

    public void paint(Graphics g)
    {
   // System.err.println("+++ " + this + ": " + (type == NONE?"NONE":"VISIBLE"));
   // this.list(System.err,0);
      if (type == NONE){
         Rectangle r = bounds();
         g.setColor(getParent().getBackground());
         g.fillRect(0,0,r.width, r.height);
         return;
      }
      super.paint(g);

      Dimension d = size();
      int x       = 0;
      int y       = 0;
      int x2  = d.width - 1;// - 2;
      int y2  = d.height- 1;// - 2;

      leftLine  (g, x, y, x2, y2);
      rightLine (g, x, y, x2, y2);

      topLine (g, x, y, x2, y2);
      bottomLine (g, x, y, x2, y2);
    }

   public void leftLine (Graphics g, int x, int y, int x2, int y2)
   {
     g.setColor(colors[1]);
     switch(type)
     {
       case ETCHED:
            {
              g.setColor(colors[1]);
              g.drawLine(x, y, x, y2 - 1); // Topleft to bottomleft
              g.setColor(colors[0]);
              g.drawLine(x+1, y+1, x+1, y2 - 2); // Topleft to bottomleft
            } break;
       case RAISED2:
            {
              g.setColor(colors[0]);
              g.drawLine(x, y, x, y2 + 2); // Topleft to bottomleft
            } break;
       case RAISED:
            {
              g.setColor(colors[0]);

              g.drawLine(x, y, x, y2 - 1); // Topleft to bottomleft
            } break;
       case SUNKEN:
            {
              g.setColor(colors[1]);
              g.drawLine(x, y, x, y2 - 1); // Topleft to bottomleft
              g.setColor(Color.black);
              g.drawLine(x+1, y+1, x+1, y2 - 2); // Topleft to bottomleft
            }
     }
   }

   public void rightLine (Graphics g, int x, int y, int x2, int y2)
   {
     g.setColor(colors[1]);
     switch(type)
     {
       case ETCHED:
            {
              g.setColor(colors[1]);
              g.drawLine(x2 - 1, y+1, x2 - 1, y2);      // Topright to bottomright
              g.setColor(colors[0]);
              g.drawLine(x2, y, x2, y2);  // Topright to bottomright
            } break;
       case RAISED:
            {
              g.setColor(colors[1]);
              g.drawLine(x2, y, x2, y2);  // Topright to bottomright
              g.setColor(colors[1]);
              g.drawLine(x2 - 1, y + 1, x2 - 1, y2 - 1);  // Topright to bottomright
            } break;
       case SUNKEN:
            {
              g.setColor(colors[0]);
              g.drawLine(x2, y, x2, y2 - 1);              // Topright to bottomright
            //  g.drawLine(x2 - 1, y+1, x2 - 1, y2 - 1);  // Topright to bottomright
            }
            break;
       case RAISED2:
            g.setColor(colors[1]);
            g.drawLine(x2, y, x2, y2);              // Topright to bottomright
            break;
     }
   }

   public void topLine (Graphics g, int x, int y, int x2, int y2)
   {
     g.setColor(colors[1]);
     switch(type)
     {
       case ETCHED:
            {
              g.setColor(colors[1]);
              g.drawLine(x, y , x2 - 1, y);  // Topleft to topright
              g.setColor(colors[0]);
              g.drawLine(x+1, y + 1, x2 - 2, y + 1);  // Topleft to topright
            } break;
       case RAISED2:
            {
              g.setColor(colors[0]);
              g.drawLine(x, y, x2 + 2, y);  // Topleft to topright
            } break;
       case RAISED:
            {
              g.setColor(colors[0]);
              g.drawLine(x, y, x2 - 1, y);  // Topleft to topright*/
            } break;
       case SUNKEN:
            {
              g.setColor(Color.black);
              g.drawLine(x+1, y + 1, x2 - 2, y + 1);  // Topleft to topright
              g.setColor(colors[1]);
              g.drawLine(x, y, x2 - 1, y);  // Topleft to topright
            }
     }
   }

   public void bottomLine (Graphics g, int x, int y, int x2, int y2)
   {
     g.setColor(colors[1]);
     switch(type)
     {
       case ETCHED:
            {
              g.setColor(colors[1]);
              g.drawLine(x, y2 - 1, x2 - 1, y2 - 1); // Bottomleft to bottomright
              g.setColor(colors[0]);
              g.drawLine(x, y2, x2, y2); // Bottomleft to bottomright
            } break;
       case RAISED:
            {
              g.setColor(colors[1]);
              g.drawLine(x, y2, x2 - 1, y2); // Bottomleft to bottomright
              g.setColor(colors[1]);
              g.drawLine(x + 1, y2 - 1, x2 - 2, y2 - 1); // Bottomleft to bottomright
            } break;
       case SUNKEN:
            {
              g.setColor(colors[0]);
              g.drawLine(x, y2, x2, y2);             // Bottomleft to bottomright
              g.drawLine(x+1, y2 - 1, x2 - 1, y2 - 1); // Bottomleft to bottomright
            }
            break;
       case RAISED2:
            g.setColor(colors[1]);
            g.drawLine(x+1, y2, x2, y2); // Bottomleft to bottomright
            break;
     }
   }

   int prevMode = -1;


   public void hide()
   {
   //System.err.println("--- " + this + ": " + type + "/" + prevMode);
     if (hideMode == 0) super.hide();
     else
     {
       if (type == NONE) return;
       prevMode = type;
       setType(NONE);
     }
   }

   public boolean isVisible()
   {
     if (hideMode == 0) return super.isVisible();
     if (type == NONE) return false;
     return true;
   }

   public void show()
   {
   // System.err.println("*** " + this + ": " + type + "/" + prevMode);
      if (hideMode == 0) super.show();
      else{
         if (!super.isVisible()) super.show();
         if (prevMode < 0) return;
         setType(prevMode);
         prevMode = -1;
   //    System.err.println("*** " + this + ": VALIDATE as " + type + "/" + prevMode);
         try{
            super.hide();
            super.show();
            ((Component)this).invalidate();
            ((Component)this).validate();
            repaint();
         }
         catch(Exception e){
            e.printStackTrace();
         }
      }
   }

   // X11 fixes to avoid wrong repaints
   public boolean mouseMove (Event e, int x, int y)
   {
     return true;
   }

   public boolean mouseExit (Event e, int x, int y)
   {
     return true;
   }

   public boolean mouseEnter (Event e, int x, int y)
   {
     return true;
   }
}
