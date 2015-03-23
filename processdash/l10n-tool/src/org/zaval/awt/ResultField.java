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

public class ResultField
extends Canvas
{
  TextAlignArea alignArea = new TextAlignArea();
  boolean       is3D      = false;

  public ResultField(String t) {
    setText(t);
  }

  public ResultField() {
    alignArea.setInsets(new Insets(0,2,0,0));
  }

  public void set3D(boolean b)
  {
    if (b == is3D) return ;
    is3D = b;
    repaint();
  }

  public TextAlignArea getAlignArea() {
    return alignArea;
  }

  public String getText() {
    return alignArea.getText();
  }

  public void setText(String text)  {
    alignArea.setText((text));
    repaint();
  }

  public void setFont(Font f){
    super.setFont(f);
    alignArea.invalidate();
  }

  public void reshape(int x, int y, int w, int h) {
    super.reshape(x, y, w, h);
    alignArea.setSize(new Dimension(w, h));
  }

  public void resize(int w, int h) {
    super.resize(w, h);
    alignArea.setSize(new Dimension(w, h));
  }

  public void paint(Graphics g)
  {
     super.paint(g);
     if (alignArea.getFontMetrics()==null)
       alignArea.setFontMetrics(getFontMetrics(getFont()));

     if (is3D)
     {
       Dimension d = size();
       g.setColor(Color.white);
       g.drawLine(d.width-1, 0, d.width-1, d.height-1);
       g.drawLine(0, d.height-1, d.width-1, d.height-1);

       g.setColor(Color.gray);
       g.drawLine(0, 0, d.width-1, 0);
       g.drawLine(0, 0, 0, d.height-1);

       g.clipRect(2, 2, d.width-3, d.height-3);
     }

     alignArea.draw(g, getForeground());
  }

  public Dimension preferredSize()
  {
    if (!isValid()) validate();
    Rectangle r = alignArea.getAlignRectangle();
    return new Dimension(r.width, r.height);
  }

  public boolean mouseDown (Event e, int x, int y)
  {
    requestFocus();
    return true;
  }

  public boolean mouseUp (Event e, int x, int y)
  {
    return true;
  }

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


