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

public class ScrollPanel
extends Panel
implements ScrollArea, LayoutManager
{
   private Scrollbar         hBar = new Scrollbar(Scrollbar.HORIZONTAL);
   private Scrollbar         vBar = new Scrollbar(Scrollbar.VERTICAL);
   private Panel             mainPanel = new Panel();
   private ScrollLayout      layout = new ScrollLayout();
   private ScrollController  metrics;

   public ScrollPanel(ScrollObject obj) {
     init(new ScrollController(this, obj));
   }

   public ScrollPanel(ScrollController m) {
     init(m);
   }

   protected void init(ScrollController m)
   {
     metrics = m;
     if (metrics == null) metrics = new ScrollController(this, null);
     if (metrics.getScrollArea() == null) metrics.setScrollArea(this);

     setLayout(layout);
     add("Center", mainPanel);
     add("East",   vBar);
     add("South",  hBar);
     add("Stubb",  new StubbComponent());

     ScrollObject obj = metrics.getScrollObject();
     init(mainPanel, obj.getScrollComponent());
   }

   protected void init(Container p, Component c) {
     p.setLayout(this);
     p.add(c);
   }

   public boolean handleEvent(Event e) {
     if (metrics.handle(e, 1)) return true;
     return super.handleEvent(e);
   }

   public void invalidate () {
     super.invalidate();
     if (metrics != null) metrics.invalidate();
   }

   public void reshape (int x, int y, int w, int h)
   {
     super.reshape(x, y, w, h);
     if (metrics != null) metrics.invalidate();
     invalidate();
     recalc();
   }

   public void layout() {
     recalc();
     super.layout();
   }

   public void recalc ()
   {
     if (!metrics.isValid()) metrics.validate();

     ScrollObject sobj = metrics.getScrollObject();
     int maxV  = metrics.getMaxVerScroll();
     int maxH  = metrics.getMaxHorScroll();

     if (maxV < 0) vBar.hide();
     else
     {
       vBar.setValues(0, 0, 0, maxV);
       sobj.setSOLocation(0, 0);
       vBar.show();
     }

     if (maxH < 0) hBar.hide();
     else
     {
       hBar.setValues(0, 0, 0, maxH);
       sobj.setSOLocation(0, 0);
       hBar.show();
     }

     sobj.setSOLocation(0, 0);
   }

   public Scrollbar  getVBar() {
     return vBar;
   }

   public Scrollbar  getHBar() {
     return hBar;
   }

   public Dimension getSASize() {
      return size();
   }

   protected ScrollController getMetrics()
   {
      return metrics;
   }

   public void  addLayoutComponent(String s, Component c) {
   }

   public void removeLayoutComponent(Component c) {}

   public Dimension  minimumLayoutSize(Container target) {
     return preferredLayoutSize(target);
   }

   public Dimension preferredLayoutSize(Container target)
   {
     Component[] c = target.getComponents();
     if (c.length > 0) return c[0].preferredSize();
     return new Dimension(0, 0);
   }

   public void layoutContainer(Container target)
   {
     Component[] c  = target.getComponents();
     ScrollArea  a  = metrics.getScrollArea();
     Dimension   td = target.size();
     for (int i=0; i<c.length; i++)
     {
       Dimension d = c[i].preferredSize();
       if (!a.getVBar().isVisible()) d.height= td.height;
       if (!a.getHBar().isVisible()) d.width = td.width;

       c[i].resize(d.width, d.height);
     }
   }
}
