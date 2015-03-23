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
import java.applet.*;
import java.util.*;

public  class  ContextMenu
extends Menu
{
  Rectangle size       = null;
  int       htext      = 0;
  int       stap       = 0;
  int       vdist      = 0;
  int       hdist      = 0;
  int       msize      = 0;
  int       act_option = 0;
  int       prev_option= 0;
  int       index      =-1;
  boolean   act        = false;
  Color     col_mark   = new Color(128);
  Hashtable types      = new Hashtable();

  public  ContextMenu(String name) {
    super(name);
    this.setFont(new Font("Dialog", Font.PLAIN, 12));
  }

 // ====================================================================

  public  ContextMenu(String name, int x, int y)
  {
   this(name);
   size.x = x;
   size.y = y;
  }

 // ====================================================================

  public  boolean isActive()
  {
    return act ;
  }

  // ====================================================================

  public void setActive(boolean a_act)
  {
   act = a_act;
  }

 // ====================================================================

  public  void addCheckit(MenuItem mi, boolean state)
  {
   this.add(mi);
   if (state) types.put(mi.getLabel(), "1");
   else       types.put(mi.getLabel(), "0");
  }

 // ====================================================================

  public  void addCheckit(String name, boolean state)
  {
   this.addCheckit(new MenuItem(name), state);
  }

 // ====================================================================

  public  void add(String name)
  {
   this.add(new MenuItem(name));
  }

 // ====================================================================

  public void addSeparator()
  {
   super.addSeparator();
  }

 // ====================================================================

  public  void remove(int index)
  {
   size.height -= stap;
   super.remove(index);
  }

 // ====================================================================

  public void  remove(MenuComponent mc)
  {
   super.remove(mc);
  }

 // ====================================================================

  public  MenuItem add(MenuItem mi)
  {
    MenuItem ret_mi = super.add(mi);
    FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(getFont());
    int sx = fm.stringWidth(mi.getLabel()) + 2*hdist;

    if (size.width < sx)  size.width = sx;
    size.height += stap;
    return ret_mi;
  }

 // ====================================================================

  public void recalc()
  {
    recalc(getFont());
  }

 // ====================================================================

  public Dimension getSize()
  {
   return new Dimension(size.width, size.height);
  }

 // ====================================================================

  public Rectangle getBounds()
  {
    return new Rectangle(size.x, size.y, size.width, size.height);
  }

 // ====================================================================

  public  Dimension preferredSize()
  {
   return new Dimension(size.width + 2*hdist, size.height + 2*vdist);
  }

 // ====================================================================

  public void recalc(Font fnt)
  {
    FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(fnt);
    hdist = fm.stringWidth("IIIII");
    htext = fm.getMaxAscent() + fm.getMaxDescent() + fm.getLeading();
    vdist = htext/4;
    stap  = vdist + htext;
    msize = htext + htext/4;
    if (size  == null) size        = new Rectangle(0,0,0, 2*vdist);
    else               size.height = vdist + 5;
    int     count = countItems();
    MenuItem mi[] =  new MenuItem[count];
    for (int i=0; i < count; i++)
    {
     mi[i] = getItem(i);
     remove(i);
    }
    for (int i=0; i < count; i++) add(mi[i]);
  }

 // ====================================================================

  public void setFont(Font fnt)
  {
   super.setFont(fnt);
   recalc();
  }

 // ====================================================================

  private void drawSeparator(Graphics gr, int y)
  {
   int x1 = size.x + 2;
   int x2 = size.x + size.width - 3;
   y = y - vdist;
   gr.setColor(Color.gray);
   gr.drawLine(x1, y-1, x2, y-1);
   gr.setColor(Color.white);
   gr.drawLine(x1, y, x2, y);
  }

 // ====================================================================

  public Rectangle getRedrawArea()
  {
   int dy = size.y + vdist + vdist;
   if (prev_option > act_option)
    return new Rectangle(size.x, dy + act_option*stap, size.width, 2*msize);
   else
    return new Rectangle(size.x, dy + prev_option*stap, size.width, 2*msize);
  }

 // ====================================================================

  private void  correctPos()
  {
   if (getParent() == null)  return;
   Dimension d = ((ContextMenuBar)(getParent())).getParentSize();
   if (size.x < hdist) size.x = hdist;
   if (size.y < vdist) size.y = vdist;
   int dx = size.x + size.width  + hdist;
   int dy = size.y + size.height + vdist;
   if (dx > d.width)
    size.x -= (dx - d.width);
   if (dy > d.height)
    size.y -= (dy - d.height);
   if (size.x < 0) size.x = 0;
   if (size.y < 0) size.y = 0;
  }

 // ====================================================================

  public boolean paint(Graphics gr)
  {
   if (!isEnabled()) return false;
   correctPos();

   gr.setFont(getFont());

   gr.setColor(Color.lightGray);
   gr.fillRect(size.x, size.y, size.width, size.height);

   gr.setColor(Color.darkGray);
   gr.drawRect(size.x, size.y, size.width-1, size.height-1);

   gr.setColor(Color.white);
   gr.drawLine(size.x, size.y, size.x + size.width, size.y);
   gr.drawLine(size.x, size.y, size.x, size.y + size.height - 1);

   gr.setColor(col_mark);
   gr.fillRect(size.x+3, size.y + vdist + act_option*stap, size.width-6, msize);

   int x     = size.x + hdist;
   int y     = size.y + stap ;
   int count = countItems();
   for (int i = 0; i < count; i++) drawOption(gr, i);
   return true;
  }

 // ====================================================================

/*  private void  drawCheckit(Graphics gr, int index, String name, int x, int y)
  {
    String  state = (String)types.get(name);
    if (state!=null)
    {
     ContextMenuBar cmb = (ContextMenuBar)(getParent());
     if (state.equals("1"))
      if (index == act_option)
       gr.drawImage(mon , x, y, col_mark, cmb.parent);
      else
       gr.drawImage(on , x, y, col_mark, cmb.parent);
    // else                   gr.drawImage(off, size.x + hdist/4, size.y + vdist + add, cmb.parent);
    }
  }*/

  // ====================================================================

  private void  drawCheckit(Graphics gr, int index, String name, int x, int y)
  {
    String  state = (String)types.get(name);
    if (state!=null)
    {
     ContextMenuBar cmb = (ContextMenuBar)(getParent());
     if (state.equals("1"))
      for (int i=0; i<2; i++)
      {
        gr.drawLine(x + hdist/3+i, y + stap - htext + vdist, x + hdist/2 + i, y + stap);
        gr.drawLine(x + hdist/2+i, y + stap, x + hdist - 3 + i, y + stap - htext + vdist);
      }
     // else                   gr.drawImage(off, size.x + hdist/4, size.y + vdist + add, cmb.parent);
    }
  }

 // ====================================================================

  private void  drawOption(Graphics gr, int index)
  {
    if (index >= countItems()) return;
    MenuItem opt  = getItem(index);
    String   name = opt.getLabel();
    int      y    = size.y + stap;
    int      x    = size.x + hdist;
    int      add  = index*stap;
    if (opt.isEnabled())
    {
     if (name.equals("-"))
     {
      drawSeparator(gr, y + add);
      return;
     }
     if (index != act_option)
       gr.setColor(Color.black);
     else
       gr.setColor(Color.white);
     drawCheckit(gr, index, name, size.x, size.y + add);
     gr.drawString(name, x, y + add);
    }
    else
    {
     gr.setColor(Color.darkGray);
     gr.drawString(name, x, y + add);
     gr.setColor(Color.white);
     gr.drawString(name, x + 1, y + 1 + add);
     if (index != act_option) gr.setColor(Color.darkGray);
     drawCheckit(gr, index, name, size.x, size.y + add);
    }

  }

 // ====================================================================

  public boolean  paintPart(Graphics gr)
  {
   if (!isEnabled()) return false;
   correctPos();

   gr.setFont(getFont());

   gr.setColor(Color.lightGray);
   gr.fillRect(size.x + 1, size.y + vdist + prev_option*stap, size.width-2, msize);
   drawOption(gr, prev_option);

   gr.setColor(col_mark);
   gr.fillRect(size.x + 3, size.y + vdist + act_option*stap, size.width-6, msize);
   drawOption(gr, act_option);
   return true;
  }

 // ====================================================================

  public  boolean  paint(Graphics gr, int x, int y)
  {
   setPos(x, y);
   return paint(gr);
  }

 // ====================================================================

  public  void  setPos(int x, int y)
  {
   size.x = x;
   size.y = y;
  }

 // ====================================================================

  public void pressKey(Event evt)
  {
    int     num  = countItems();
    boolean flag = true;
    if (num == 0) return;
    switch (evt.key)
    {
     case Event.UP  : flag = decOption(); break;
     case Event.DOWN: flag = incOption(); break;
    }
    if (flag) sendEvent(ContextMenuBar.EV_MENU_REDRAW);
  }

 // ====================================================================

  public boolean isCheckit(int act)
  {
   MenuItem mi = getItem(act);
   if (mi == null) return false;
   if (types.get(mi.getLabel())!=null)
    return true;
   return false;
  }

 // ====================================================================

  public void invCheckit(int act)
  {
   if (!isCheckit(act)) return;
   String state = (String)types.get(getItem(act).getLabel());
   if (state.equals("1")) types.put(getItem(act).getLabel(), "0");
   else                   types.put(getItem(act).getLabel(), "1");
  }

 // ====================================================================

  public  void pressEnter()
  {
   MenuItem mi = getItem(act_option);
   if (mi.isEnabled())
   {
    invCheckit(act_option);
    sendEvent(ContextMenuBar.EV_MENU_ENTER);
   }
  }

 // ====================================================================

  public  void pressExit()
  {
   sendEvent(ContextMenuBar.EV_MENU_EXIT);
  }

 // ====================================================================

   private boolean incOption()
   {
     int  num = countItems();
     if (num <= 0) return false;
     prev_option = act_option;
     for (int i=0; i < num; i++)
      if (act_option < (num-1))
      {
       act_option ++;
       if (getItem(act_option).getLabel().equals("-")) continue;
       return true;
      }
      else
      {
       act_option = 0;
       if (getItem(act_option).getLabel().equals("-")) continue;
       return true;
      }
     return false;
   }

 // ====================================================================

   private boolean decOption()
   {
     int  num = countItems();
     if (num <= 0) return false;
     prev_option = act_option;
     for (int i=0; i < num; i++)
      if (act_option > 0)
      {
       act_option --;
       if (getItem(act_option).getLabel().equals("-")) continue;
       return true;
      }
      else
      {
       act_option = (num-1);
       if (getItem(act_option).getLabel().equals("-")) continue;
       return true;
      }
     return false;
   }

 // ====================================================================

  public  void  pressMouse(Event evt)
  {
    int     num  = countItems();
    boolean flag = true;
    if ((num == 0                  )||
        (!this.inside(evt.x, evt.y))  )
    {
     if (evt.clickCount >= 1) pressExit();
     return;
    }
    int option =  (evt.y - size.y) / stap;
    if ((option != act_option)&&(option < num)&&(!getItem(option).getLabel().equals("-")))
    {
     prev_option = act_option;
     act_option  = option;
    }
    else flag = false ;
    if (flag) sendEvent(ContextMenuBar.EV_MENU_REDRAW);
    if ((evt.id == Event.MOUSE_UP)) pressEnter();
  }

 // ====================================================================

  public boolean inside(int x, int y)
  {
   if ( (x > (size.x + size.width ))||
        (x <  size.x               )||
        (y > (size.y + size.height))||
        (y <  size.y               )  ) return false;
   return true;
  }

 // ====================================================================

//  int isLeftKey = 0;
  public boolean handleEvent(Event evt)
  {
   switch (evt.id) {
    case Event.MOUSE_EXIT: pressExit (); break;
    case Event.KEY_PRESS : if (evt.key == 10) pressEnter();
                           else               pressExit (); break;
    case Event.KEY_ACTION: pressKey(evt); break;
    case Event.MOUSE_DRAG: ;
    case Event.MOUSE_MOVE: pressMouse(evt); break;
    case Event.MOUSE_DOWN: {
                            /*if (evt.modifiers != 4) isLeftKey = 1;
                            else                    isLeftKey = 1;*/
                            pressMouse(evt);
                           }  break;
    case Event.MOUSE_UP  : //if (isLeftKey == 1)
                           {
                            pressMouse(evt);
                            //isLeftKey = 0;
                           } break;
   }
   return true; //super.handleEvent(evt);
  }

 // ====================================================================

  private void sendEvent(int id)
  {
   if (getParent() == null) return;
   Event evt = null;
   switch (id)
   {
    case ContextMenuBar.EV_MENU_REDRAW   : evt = new Event(null, ContextMenuBar.EV_MENU_REDRAW   , null); break;
    case ContextMenuBar.EV_MENU_REDRAWALL: evt = new Event(null, ContextMenuBar.EV_MENU_REDRAWALL, null); break;
    case ContextMenuBar.EV_MENU_EXIT     : evt = new Event(null, ContextMenuBar.EV_MENU_EXIT     , null); break;
    case ContextMenuBar.EV_MENU_ENTER    : {
                                            MenuItem trg = getItem(act_option);
                                            evt = new Event(trg, ContextMenuBar.EV_MENU_ENTER, trg.getLabel());
                                           } break;
   }
   if (evt != null)
    ((ContextMenuBar)(getParent())).sendEvent(evt);
  }

 // ====================================================================

}