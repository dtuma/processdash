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

public class ExGridLayout
implements LayoutManager
{
  private static int hackIE401 = -1;

  protected Hashtable comptable;
  protected GridBagConstraints defaultConstraints;

  protected int startx=0;
  protected int starty=0;
  protected int cols=0,rows=0;

  protected int[] widths;
  protected int[] heights;

  public static int MAX_X=2560;
  public static int MAX_Y=2560;

  public static int MIN_W=1; // empty cell has a size {1,1}
  public static int MIN_H=1;

  protected GridBagConstraints[] bags=null;
  protected Dimension[] sizes=null;

  protected double max_wx = 10.0;
  protected double max_wy = 10.0;

  public void setMaxExpanded(double wx, double wy)
  {
     max_wx = wx;
     max_wy = wy;
  }

  public void setLimits(int a, int b)
  {
     MAX_X=Math.max(a,MAX_X);
     MAX_Y=Math.max(b,MAX_Y);
     widths=new int[MAX_X+1];
     heights=new int[MAX_Y+1];
  }

  public ExGridLayout ()
  {
    widths=new int[MAX_X+1];
    heights=new int[MAX_Y+1];
    comptable = new Hashtable();
    defaultConstraints = new GridBagConstraints();
    initFix();
  }

  public ExGridLayout (int realw, int realh)
  {
    widths=new int[MAX_X+1];
    heights=new int[MAX_Y+1];
    comptable = new Hashtable();
    defaultConstraints = new GridBagConstraints();
    initFix();
  }

  public void setConstraints(Component comp, GridBagConstraints constraints)
  {
    bags=null;
    sizes=null;
    comptable.put(comp, constraints.clone());
  }

  public GridBagConstraints getConstraints(Component comp)
  {
    GridBagConstraints constraints = (GridBagConstraints)comptable.get(comp);
    return constraints;
  }

  public void updateComponent(Container owner, Component old, Component nw)
  {
     GridBagConstraints gs=getConstraints(old);
     if(comptable.containsKey(old)){
        comptable.remove(old);
        comptable.put(nw,gs);
        owner.remove(old);
        owner.add(nw);
        if(bags==null) return;
        arrange(owner,nw);
     }
  }

  public Point location(int xx, int yy)
  {
    Point loc = new Point(0,0);
    int x=startx,y=starty,i;
    for(i=0;i<cols;++i){
       x+=widths[i];
       if(x>xx) break;
    }
    loc.x=i;
    x=starty;
    for(i=0;i<rows;++i){
       x+=heights[i];
       if(x>yy) break;
    }
    loc.y=i;
    return loc;
  }

  private void initFix()
  {
     if(hackIE401==-1){
        String jver = "1.0.2.";
        String jven = "Sun";
        try{
           jver = System.getProperty("java.version");
        }
        catch(Throwable t){
        }
        try{
           jven = System.getProperty("java.vendor");
        }
        catch(Throwable t){
        }

      //System.err.print("Vendor: "+jven+", JDK version: "+jver);
        if(!jver.startsWith("1.0") && jven.startsWith("Microsoft")) hackIE401 = 1;
        else hackIE401 =  0;
      //if(hackIE401==1) System.err.println(", hack is needed");
      //else System.err.println();
     }
  }

  private Dimension getSize(Container parent, boolean pref)
  {
      int i;
      makeInfo(pref);
      Insets z = parent.insets();
      int x=z.left+z.right,y=z.top+z.bottom;
      for(i=0;i<cols;++i) x+=widths[i];
      for(i=0;i<rows;++i) y+=heights[i];
      return new Dimension(x,y);
  }

  private void makeInfo(boolean pref)
  {
     int i=comptable.size(),j,k;

     if(bags!=null && bags.length==i) return;

     bags=new GridBagConstraints[i];
     sizes=new Dimension[i];

     Component comps;

     i=0;
     Enumeration en = comptable.keys();
     while(en.hasMoreElements()){
        comps=(Component)en.nextElement();
        GridBagConstraints gb=(GridBagConstraints)comptable.get(comps);
        sizes[i]=pref?comps.preferredSize():comps.minimumSize();
        sizes[i].width+=gb.insets.right+gb.insets.left;
        sizes[i].height+=gb.insets.top+gb.insets.bottom;
        bags[i++]=gb;
        k=gb.gridx+gb.gridwidth;
     // if(k>MAX_X){
     //    System.err.println("X_over: "+comps+","+sizes[i-1]+","+k+
     //       gb.gridx+","+gb.gridwidth);
     //    k=MAX_X;
     // }
        cols=Math.max(cols,k);
        k=gb.gridy+gb.gridheight;
     // if(k>MAX_Y){
     //    System.err.println("Y_over: "+comps+","+sizes[i-1]+","+k+
     //       gb.gridy+","+gb.gridheight);
     //    k=MAX_Y;
     // }
        rows=Math.max(rows,k);
     }
  // System.err.println("Cols="+cols+", rows="+rows);


     for(i=0;i<cols;++i) widths[i]=MIN_W;
     for(i=0;i<rows;++i) heights[i]=MIN_H;

     for(i=0;i<bags.length;++i){
        for(j=0;j<bags[i].gridwidth;++j){
           k=bags[i].gridx+j;
           widths[k]=Math.max(sizes[i].width/bags[i].gridwidth,widths[k]);
        }
        for(j=0;j<bags[i].gridheight;++j){
           k=bags[i].gridy+j;
           heights[k]=Math.max(sizes[i].height/bags[i].gridheight,heights[k]);
        }
     }
  }

  private void arrange(Container parent)
  {
     arrange(parent, null);
  }

  private void arrange(Container parent, Component modf)
  {
     makeInfo(true);
     Component[] comps=parent.getComponents();
     Rectangle r=parent.bounds();
     Insets pz = parent.insets();
     int x,y,w,h;
     int i,j;

     Dimension d=getSize(parent,true);
   //d.width  -= pz.left+pz.right;
   //d.height -= pz.top+pz.bottom;

     if(r.width<5 || r.height<5) return; // too small control area

     double fx = (double)(r.width ) / d.width;
     double fy = (double)(r.height) / d.height;

     if(fx>max_wx) fx=max_wx;
     if(fy>max_wy) fy=max_wy;

     for(i=0;i<comps.length;++i){
        GridBagConstraints gb = (GridBagConstraints)comptable.get(comps[i]);
        if(gb==null){
           comps[i].hide();
           continue;
        }
        if(modf!=null && modf!=comps[i]) continue;
        x=y=0;
        w=h=0;
        for(j=0;j<gb.gridx;++j) x+=widths[j];
        for(;j<gb.gridx+gb.gridwidth;++j) w+=widths[j];
        for(j=0;j<gb.gridy;++j) y+=heights[j];
        for(;j<gb.gridy+gb.gridheight;++j) h+=heights[j];

        x+=gb.insets.left;
        y+=gb.insets.top;
        w-=gb.insets.right+gb.insets.left;
        h-=gb.insets.bottom+gb.insets.top;
        if(w<=0 || h<=0){
        // comps[i].hide();
           continue;
        }

        Dimension z = comps[i].preferredSize();

        if(gb.fill==gb.HORIZONTAL){
           h=z.height;
           z.width=w;
        }
        else if(gb.fill==gb.VERTICAL){
           w=z.width;
           z.height=h;
        }
        else if(gb.fill==gb.BOTH){
           z.width=w;
           z.height=h;
        }
     // if(gb.fill==gb.NONE){
     //    w=z.width;
     //    h=z.height;
     // }

        if(w>z.width){
           int wrap=w-z.width;
           if(gb.anchor==gb.CENTER){
              x+=wrap/2;
              w=z.width;
           }
           if(gb.anchor==gb.EAST ||
              gb.anchor==gb.NORTHEAST ||
              gb.anchor==gb.SOUTHEAST){
                 x+=wrap;
                 w=z.width;
           }
        }

        if(h>z.height){
           int wrap=h-z.height;
           if(gb.anchor==gb.CENTER){
              y+=wrap/2;
              h=z.height;
           }
           else if(gb.anchor==gb.SOUTH
              || gb.anchor==gb.SOUTHWEST
              || gb.anchor==gb.SOUTHEAST){
                 y+=wrap;
                 h=z.height;
           }
        }

        h=(int)(fy*h+0.5);
        w=(int)(fx*w+0.5);

     // if(comps[i].isVisible()) comps[i].show();
        comps[i].resize(w,h);
        comps[i].move(
           (int)(x*fx)+pz.left,
           (int)(y*fy)+pz.top);
        // IE 4.01 bugfix
        if(hackIE401 == 1 && !comps[i].isVisible()){
           comps[i].show();
           comps[i].hide();
        }
     }
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
    return getSize(parent, true);
  }

  public Dimension minimumLayoutSize(Container parent)
  {
    return getSize(parent, false);
  }

  public void layoutContainer(Container parent)
  {
//   System.err.println("Arrange: "+parent);
     boolean isVis=parent.isVisible();
     if(isVis) parent.hide();
     try{
        arrange(parent);
//      System.err.println("Arranged "+parent);
     }
     finally{
        if(isVis){
           parent.show();
           parent.repaint();
        }
     }
  }

  public String toString()
  {
    return getClass().getName();
  }
}
