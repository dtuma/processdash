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

public class SimpleScrollPanel
extends Panel
implements LayoutManager
{
  private boolean isHor = true;
  private boolean isVer = true;
  private Scrollbar hor;
  private Scrollbar ver;
  private Component comp;
  private Panel     panel;
  private Dimension auf=null;

  public static final int IS_NEED_SCROLL = 646478;

  public Component getScrollableObject()
  {
     return comp;
  }

  public SimpleScrollPanel(Component c)
  {
    comp = c;
    hor  = new Scrollbar(Scrollbar.HORIZONTAL);
    ver  = new Scrollbar(Scrollbar.VERTICAL);
    setLayout(this); // new BorderLayout(0,0));

    panel = new Panel();
    panel.setLayout(null);

    panel.add(comp);
    ver.hide();
    hor.hide();

    add(panel);
    add(hor  );
    add(ver  );

    setBackground(getBackground());
    hor.setBackground(getBackground());
    ver.setBackground(getBackground());
    panel.setBackground(getBackground());
  }

  public void setBackground(Color c)
  {
     super.setBackground(c);
     hor.setBackground  (c);
     ver.setBackground  (c);
     panel.setBackground(c);
  }

  private static final int[] table={
     Event.SCROLL_ABSOLUTE,
     Event.SCROLL_LINE_DOWN,
     Event.SCROLL_LINE_UP,
     Event.SCROLL_PAGE_DOWN,
     Event.SCROLL_PAGE_UP
  };

  private boolean drop(Event e)
  {
     if(!(e.target instanceof Scrollbar)) return true;
     for(int i=0;i<table.length;++i)
        if(e.id==table[i]) return false;
     return true;
  }

  public boolean handleEvent(Event evt)
  {
     if(drop(evt)) return super.handleEvent(evt);
     if (evt.target == ver){
       comp.move(comp.bounds().x, -ver.getValue());
       checkValid();
       return true;
     }

     if (evt.target == hor){
       comp.move(-hor.getValue(), comp.bounds().y);
       checkValid();
       return true;
     }
     return super.handleEvent(evt);
  }

  private void checkValid()
  {
     if(!hor.isEnabled()){
        hor.hide();
        hor.enable();
        ((Component)this).invalidate();
        validate();
     }
     if(!ver.isEnabled()){
        ver.hide();
        ver.enable();
        ((Component)this).invalidate();
        validate();
     }
     comp.repaint();
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
     Dimension zet = comp.preferredSize();
//   return zet;
     if(zet.width==0 || zet.height==0) zet = comp.size();
     zet.width  += ver.preferredSize().width;
     zet.height += hor.preferredSize().height;
     if(auf==null) return zet;
     else return auf;
//   return oops(auf,zet);
  }

  public Dimension minimumLayoutSize(Container parent)
  {
     Dimension zet = comp.minimumSize();
//   return zet;
     if(zet.width==0 || zet.height==0) zet = comp.size();
     zet.width  += ver.preferredSize().width;
     zet.height += hor.preferredSize().height;
     if(auf==null) return zet;
     return oops(auf,zet);
  }

  private Dimension oops(Dimension a, Dimension b) // min(a,b)
  {
     int w = Math.min(a.width,b.width);
     int h = Math.min(a.height,b.height);
     return new Dimension(w,h);
  }

  public void layoutContainer(Container parent)
  {
     Dimension x = comp.preferredSize();
     if(x.width==0 || x.height==0) x = comp.size();

     Rectangle r = bounds();
     comp.move(0,0);
     int hor_h = hor.preferredSize().height;
     int ver_w = ver.preferredSize().width;

     int wx=r.width;
     int wy=r.height;

     boolean seth = false;
     boolean setv= false;

  // System.err.println("Layout: RS=["+wx+","+wy+"], PS=["+
  //    x.width+","+x.height+"], SB=("+hor_h+","+ver_w+")");

     seth = (x.width>wx)  || ((x.height>wy) && (x.width>wx-ver_w));
     setv = (x.height>wy) || ((x.width>wx)  && (x.height>wy-hor_h));

     if(!seth){
  //    System.err.println("   RS.w >= PFS.w");
        if(hor.isVisible()) hor.hide();
        comp.move(0,0);
     }
     else{
  //    System.err.println("   RS.w < PFS.w: "+(x.width - wx));
        hor.move(0,r.height-hor_h);
        hor.resize(r.width-(setv?ver_w:0),hor_h);
        hor.show();
        wy-=hor_h;
        int newh=x.width - wx + (x.height>wy?ver_w:0);
     // if(newh<ver_w) newh+=ver_w;
        hor.setValues(0,wx-(setv?ver_w:0),0,x.width);
        hor.setPageIncrement(wx/2);
  //    System.err.println("   RS'=["+wx+","+wy+"]");
     }

     if(!setv){
  //    System.err.println("   RS.h >= PFS.h");
        if(ver.isVisible()) ver.hide();
        comp.move(0,0);
     }
     else{
  //    System.err.println("   RS.h < PFS.h: "+(x.height - wy));
        ver.move(r.width-ver_w,0);
        ver.resize(ver_w,r.height-(seth?hor_h:0));
        ver.show();
        wx-=ver_w;
        int newh=x.height - wy; // hor_h == ver_w
     // if(newh<hor_h) newh+=hor_h;
        ver.setValues(0,wy,0,x.height);
        ver.setPageIncrement(wy/2);
  //    System.err.println("   RS'=["+wx+","+wy+"]");
     }
  // System.err.println("   panel = ["+wx+","+wy+"]");
     panel.move(0,0);
     panel.resize(wx,wy);
     if(x.width<wx) x.width=wx; // To fit it horizontally
     comp.resize(x.width,x.height); // To scroll it
  // System.err.println("   comp  = ["+x.width+","+x.height+"]");
  }

  private Component get0(Container c)
  {
     return c.getComponent(0);
  }

  public void setMaxSize(Dimension auf)
  {
     this.auf = auf;
  }
}
