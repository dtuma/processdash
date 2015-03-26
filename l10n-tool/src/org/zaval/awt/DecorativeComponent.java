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

import org.zaval.awt.event.*;
import java.awt.*;
import java.util.*;

public abstract class DecorativeComponent
{
  public static final int CENTER = 1;
  public static final int LEFT   = 2;
  public static final int RIGHT  = 3;

  private Color       bColor  = Color.white;
  private Color       fColor  = Color.black;
  private Font        font;
  private int         align   = CENTER;
  private Dimension   size    = new Dimension(0,0);
  private boolean     isValid = false;
  private Component   parent;
  private Insets      insets   = new Insets(0,2,4,4);
  private ValidateListenerSupport  listener = new ValidateListenerSupport();

  public DecorativeComponent() {
    this(null);
  }

  public DecorativeComponent(Component c) {
    setParent(c);
    invalidate();
  }

  public void addValidateListener(ValidateListener l) {
    listener.addListener(l);
  }

  public void removeValidateListener(ValidateListener l) {
    listener.removeListener(l);
  }

  protected void validatePerform() {
    listener.perform(new ValidateEvent(this, ValidateEvent.VALIDATE));
  }

  protected void invalidatePerform() {
    listener.perform(new ValidateEvent(this, ValidateEvent.INVALIDATE));
  }

  public void setFromComponent(Component c)
  {
    if (c == null) return;
    if (c.getFont()       != null) setFont(c.getFont());
    if (c.getBackground() != null) setBackgroundColor (c.getBackground());
    if (c.getForeground() != null) setForegroundColor (c.getForeground());
  }

  public Component getParent() {
    return parent;
  }

  public void setParent(Component p) {
    parent = p;
  }

  public Font getFont() {
    return font;
  }

  public void setFont(Font f) {
    font = f;
    invalidate();
  }

  public int getAlign() {
    return align;
  }

  public void setAlign(int a) {
    if (a == align) return;
    align = a;
  }

  public Color getBackgroundColor() {
    return bColor;
  }

  public void setBackgroundColor(Color c) {
    bColor = c;
  }

  public Color getForegroundColor() {
    return fColor;
  }

  public void setForegroundColor(Color c) {
    fColor = c;
  }

  public void setSize(Dimension d)
  {
    if (d == size) return;
    size.width  = d.width;
    size.height = d.height;
    invalidate();
  }

  public Dimension getSize() {
    return size;
  }

  public Insets getInsets() {
    return insets;
  }

  public void setInsets(Insets i) {
    insets = i;
  }

  protected void invalidate()
  {
    //System.out.println ("invalidate()1" + isValid);
    if (isValid)
    {
      isValid = false;
      //System.out.println ("invalidate()");
      invalidatePerform();
    }
  }

  protected void validate()
  {
    if (!isValid)
    {
      isValid = true;
      //System.out.println ("validate()");
      validatePerform();
    }
  }

  public boolean isValid() {
    return isValid;
  }

  public void doValidate() {
    validate();
  }

  public void doPaint(int x, int y, int width, int height, Graphics g)
  {
    validate();
    if (!isValid()) return;
    if (width <= 0 || height <= 0) return;
    draw(x, y, width, height, g);
  }

  public void doPaint(int x, int y, Graphics g)
  {
    validate();
    if (!isValid()) return;
    Dimension d = getSize();
    if (d.width <= 0 || d.height <= 0) return;
    draw(x, y, d.width, d.height, g);

  }

  protected abstract void draw (int x, int y, int width, int height, Graphics g);
}
