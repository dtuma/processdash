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

public class EmulatedTextField
extends Canvas
{
  protected static EmulatedTextField cursorOwner = null;

  public static final int LEFT  = 1;
  public static final int RIGHT = 2;

  protected StringBuffer buffer         = new StringBuffer("");
  Insets       insets         = new Insets (0,4,0,4);
  Point        textLocation   = new Point (0,0);
  Point        cursorLocation = new Point (0,0);
  Dimension    textSize       = new Dimension(0,0);
  Dimension    cursorSize     = new Dimension(0,0);
  Point        shift          = new Point(0,0);
  Color        cursorColor    = Color.black;
  int          cursorPos      = 0;
  int          align          = LEFT;
  boolean      is3D           = true;
  protected int          selPos = 0, selWidth = 0, startSel = 0;

  public EmulatedTextField() {
    this(null);
  }

  public EmulatedTextField(String s) {
    if (s != null) setText(s);
  }

  public void setText(String s) {
    buffer = new StringBuffer(s);
    setPos(0);
  }

  public String getText() {
    return buffer.toString();
  }

  public void setAlign(int a) {
    align = a;
  }

  public void getAlign(int a) {
    align = a;
    repaint();
  }

  public void setCursorColor(Color c) {
    cursorColor = c;
    if (hasFocus()) repaint();
  }

  public int getCursorPos() {
    return cursorPos;
  }

  public boolean hasFocus() {
    return (this == cursorOwner);
  }

  public void setInsets(Insets i)
  {
    insets.top    = i.top;
    insets.left   = i.left;
    insets.right  = i.right;
    insets.bottom = i.bottom;
    repaint();
  }

  public void set3D(boolean b)
  {
    if (b == is3D) return;
    is3D = b;
    repaint();
  }

  public boolean is3D() {
    return is3D;
  }
  
  public boolean keyDown(Event e, int key)
  {
    if(key>0xFF00) key -= 0xFF00; /* Linux 1.3.1 workaround */
    if (blockKey(e)) return false;
    if (controlKey(key, e.shiftDown())) return false;
    
    if(java.awt.event.KeyEvent.getKeyText(key).indexOf("nknown") != -1){    
    int old_key = key;
    if(System.getProperty("key.locale.conversion")!=null){
        key = key & 0x00FF; // Latin1 with wrong unicode page?
        String encType = System.getProperty("file.encoding");
        if(encType!=null)
            try{
                byte[] b2 = { (byte)key };
                key = (int)(new String(b2, encType)).charAt(0);
            }
            catch(Exception eee){
            }
            Character.UnicodeBlock b3 = Character.UnicodeBlock.of((char)key);
            if(b3==null) key = old_key;
        }    
    }
    if (e.id != Event.KEY_ACTION || key == 0){
       if (inputKey(key)) return false;
    }  
    return super.keyDown(e, key);
  }

  public void blPaste()
  {
    String s = readFromClipboard();
    if (s != null)
    {
      s = filterSymbols( s );
      removeBlock();
      insert(cursorPos, s);
      setPos(cursorPos + s.length());
    }
  }

  protected String filterSymbols( String s )
  {
     return s;
  }

  public String getSelectedText()
  {
    return buffer.toString().substring(selPos, selPos + selWidth);
  }

  public void blCopy() {
    if (!isSelected()) return ;
    writeToClipboard(buffer.toString().substring(selPos, selPos + selWidth));
  }

  public void blDelete()
  {
    if (!isSelected()) return;
    writeToClipboard(buffer.toString().substring(selPos, selPos + selWidth));
    removeBlock();
  }

  protected boolean blockKey(Event e)
  {
    if ((e.controlDown() && e.key == 1025) || e.key == 3)
      blCopy();
    else
    if ((e.shiftDown() && e.key == 1025)|| e.key == 22)
      blPaste();
    else
    if ((e.shiftDown() && e.key == 0x7F)|| e.key == 24)
      blDelete();
    else
      if (!e.controlDown()) return false;

    return true;
  }

  protected boolean inputKey(int key)
  {
    removeBlock();
    char ch = (char)key;
    if (write(key)) seek (1, false);
    return true;
  }

  protected void removeBlock()
  {
    if (isSelected())
    {
      remove(selPos, selWidth);
      setPos(selPos);
      clear();
    }
  }

  protected boolean controlKey(int key, boolean shift)
  {
    boolean b = true;
    switch (key)
    {
      case Event.DOWN  :
      case Event.RIGHT : seek(1, shift);  break;
      case Event.UP  :
      case Event.LEFT  : seek(-1, shift); break;
      case Event.END   : seek2end(shift); break;
      case Event.HOME  : seek2beg(shift); break;
      case 0x7F  : if (!isSelected()) remove(cursorPos, 1);
                   else               removeBlock();
                   break;
      case '\b'  : if (!isSelected())
                   {
                     if (cursorPos > 0)
                     {
                       seek(-1, shift);
                       remove(cursorPos, 1);
                     }
                   }
                   else removeBlock();
                   break;
      case '\n'      :
      case Event.PGUP:
      case Event.PGDN: break;
      case 1025  :   return false; //INS

      case '\t'  : return true;
      default    : return false;
    }

    if (!shift) clear();
    return b;
  }

  public void paint(Graphics g)
  {
    recalc();
    drawBorder(g);
    drawCursor(g);
    drawText  (g);
    drawBlock (g);
  }

  public Insets insets() {
    return insets;
  }

  protected void drawBlock(Graphics g)
  {
    int len = buffer.length();
    if (!isSelected()) return;
    String s = buffer.toString();
    FontMetrics fm = getFontMetrics(getFont());
    int beg = fm.stringWidth(s.substring(0, selPos));
    int end = fm.stringWidth(s.substring(0, selPos + selWidth));
    g.setColor(Color.blue);
    g.fillRect(textLocation.x + shift.x + beg,
               cursorLocation.y + shift.y,
               end - beg,
               textSize.height);
    g.setColor(Color.white);
    g.drawString (s.substring(selPos, selPos + selWidth), textLocation.x + shift.x + beg, textLocation.y + shift.y);
  }

  protected void drawText(Graphics g)
  {
    Dimension d = size();
    g.clipRect   (insets.left, insets.top, d.width-insets.left-insets.right,  d.height-insets.top-insets.bottom);
    g.setColor   (getForeground());
    g.drawString (buffer.toString(), textLocation.x + shift.x, textLocation.y + shift.y);
  }

  protected void drawCursor(Graphics g)
  {
    if (cursorOwner != this) return;
    g.setColor(cursorColor);
    g.fillRect(cursorLocation.x + shift.x, cursorLocation.y + shift.y, cursorSize.width, cursorSize.height);
  }

  protected void drawBorder(Graphics g)
  {
    Dimension d = size();
    if (is3D)
    {
      g.setColor(Color.gray);
      g.drawLine(0, 0, d.width-1, 0);
      g.drawLine(0, 0, 0, d.height-1);
      g.setColor(Color.black);
      g.drawLine(1, 1, d.width-3, 1);
      g.drawLine(1, 1, 1, d.height-3);
      g.setColor(Color.white);
      g.drawLine(0, d.height-1, d.width-1, d.height-1);
      g.drawLine(d.width-1, 0, d.width-1, d.height-1);
    }
    else
    {
      g.setColor(Color.black);
      g.drawRect(0, 0, d.width-1, d.height-1);
    }
  }

  protected boolean seek(int shift, boolean b)
  {
    int len  = buffer.length();
    int npos = getValidPos(shift);

    if (npos > len || npos < 0) return false;

    if (!isSelected() && b)
      startSel = cursorPos;

    setPos(npos);

    if (b)
    {
      if (cursorPos < startSel) select(cursorPos, startSel - cursorPos);
      else                      select(startSel , cursorPos - startSel);
    }

    return true;
  }

  protected int getValidPos(int shift) {
    return cursorPos + shift;
  }

  protected boolean seek2end(boolean b) {
    seek(buffer.length() - cursorPos, b);
    return true;
  }

  protected boolean seek2beg(boolean b) {
    seek(-cursorPos, b);
    return true;
  }

  protected boolean write(int key) {
    buffer.insert(cursorPos, (char)key);
    return true;
  }

  protected void remove(int pos, int size)
  {
    if (pos == buffer.length() || pos < 0) return;
    String s = buffer.toString();
    s = s.substring(0, pos) + s.substring(pos+size);
    buffer = new StringBuffer(s);
    repaintPart ();
  }

  protected int getShift(int x, Dimension d, Insets i)
  {
    if (x < i.left)
      return (i.left - x);

    int w = d.width - i.right;
    if (x > w) return (w - x);

    return 0;
  }

  public void insert(int pos, String str)
  {
    if (pos > buffer.length() || pos < 0) return;
    String s = buffer.toString();
    s = s.substring(0, pos) + str + s.substring(pos);
    buffer = new StringBuffer(s);
    repaintPart ();
  }

  long clickTime = 0;

  public boolean mouseDown(Event e, int x, int y)
  {
    if (cursorOwner != this) requestFocus();
    int pos = calcTextPos(x, y);
    if (pos >= 0 && pos != cursorPos) setPos(pos);
    if (isSelected()) clear();

    long t = System.currentTimeMillis();
    if ((t - clickTime) < 300)
      select(0, buffer.length());
    clickTime = t;

    return super.mouseDown(e, x, y);
  }

  protected int calcTextPos(int x, int y)
  {
     if (buffer.length() == 0) return 0;

     if (x > (shift.x + textSize.width + textLocation.x))
       return buffer.length();

     if ((shift.x + textLocation.x) > x)
       return 0;

     int w = x - shift.x;
     int p = (w * 100)/textSize.width;
     int l = buffer.length();
     int s = (l * p)/100;

     FontMetrics fm = getFontMetrics(getFont());
     String      ss = buffer.toString();
     for (int i = s, j = s + 1, k = i; i>=0 || j<l;)
     {
       if (k>=0 && k<l)
       {
         char   ch = buffer.charAt(k);
         String sx = ss.substring(0, k);
         int    sl = fm.stringWidth(sx) + shift.x + textLocation.x;
         int    cl = fm.charWidth  (ch);
         if (x >= sl && x < sl + cl)
         {
           if (x > (sl + cl/2)) return k+1;
           else                 return k;
           //return 1;
         }
       }

       if (k == j)
       {
         i--;
         j++;
         k = i;
       }
       else k = j;
     }

     return -1;
  }

  protected void setPos(int p) {
    cursorPos = p;
    repaintPart ();
  }

  protected Dimension calcSize()
  {
    Font f = getFont();
    if (f == null) return new Dimension (0,0);
    FontMetrics m = getFontMetrics(f);
    if (m == null) return new Dimension (0,0);
    Insets i = insets();
    String t = buffer.toString();
    return new Dimension (i.left + i.right + m.stringWidth(t) , i.top + i.bottom + m.getHeight());
  }

  protected boolean recalc()
  {
    Dimension d = size();
    if (d.width == 0 || d.height == 0) return false;

    Insets      i   = insets();
    FontMetrics m   = getFontMetrics(getFont());
    if (m == null) return false;

    String      s   = buffer.toString();
    String      sub = s.substring(0, cursorPos);
    int         sl  = m.stringWidth(sub);
    int         rh  = d.height - i.top - i.bottom;

    textSize.height = m.getHeight();
    textSize.width  = m.stringWidth(s);
    textLocation.y  = i.top + (rh + textSize.height)/2 - m.getDescent();

    cursorLocation.x = sl + i.left;
    cursorLocation.y = textLocation.y - textSize.height + m.getDescent();
    if (cursorLocation.y < i.top) cursorLocation.y = i.top;

    cursorSize.width  = 1;
    cursorSize.height = textSize.height;

    if ((cursorLocation.y + cursorSize.height) >= d.height - i.bottom)
      cursorSize.height = d.height - cursorLocation.y - i.bottom;

    switch (align)
    {
      case LEFT  :
      {
        textLocation.x   = i.left;
        cursorLocation.x = sl + i.left;
      } break;
      case RIGHT :
      {
        textLocation.x   = d.width - i.right - textSize.width;
        cursorLocation.x = sl +  textLocation.x;
      } break;
    }

    if ((cursorLocation.x + shift.x) < i.left)
      shift.x = i.left - cursorLocation.x;
    else
    {
      int w = d.width - i.right;
      if ((cursorLocation.x + shift.x) > w) shift.x = w - cursorLocation.x;
    }

    return true;
  }

  public void resize(int w, int h) {
    shift.x = 0;
    super.resize(w, h);
  }

  protected void otdaiFocusTvojuMat() {
    cursorOwner = null;
    repaint();
  }

  public boolean lostFocus(Event e, Object obj)
  {
    if (cursorOwner == this)
    {
      cursorOwner = null;
      clear();
      repaint();
    }
    return super.lostFocus(e, obj);
  }

  public boolean gotFocus(Event e, Object obj)
  {
    if (cursorOwner != null) cursorOwner.otdaiFocusTvojuMat();
    cursorOwner = this;
    if (buffer != null)
    {
      setPos(buffer.length());
      select(0, buffer.length());
    }

    repaint();
    return super.gotFocus(e, obj);
  }

  protected void repaintPart ()
  {
    Insets    i = insets();
    Dimension d = size  ();
    repaint(i.left, i.top, d.width - i.right - i.left + 1, d.height - i.bottom - i.top + 1);
  }

  public Dimension preferredSize() {
    return calcSize();
  }

  public void select(int pos, int w)
  {
    if (selPos == pos && w == selWidth) return;
    selPos   = pos;
    selWidth = w;
    repaintPart();
  }

  public boolean isSelected ()
  {
    int len = buffer.length();
    if (selPos <  0 || selPos >= len || (selPos + selWidth) > len || selWidth == 0) return false;
    return true;
  }

  protected void clear ()
  {
    selWidth = 0;
    repaintPart();
  }

  public boolean mouseDrag(Event e, int x, int y)
  {
    int pos = calcTextPos(x, y);
    if (pos >= 0)
    {
      if (pos < cursorPos)
        select(pos, cursorPos - pos);
      else
        select(cursorPos, pos - cursorPos);
    }
    return super.mouseDrag(e, x, y);
  }

  private static String clipboard;

/**
 *
 * Contributed by <a href="mailto:morten@bilpriser.dk">Morten Raahede Knudsen</a>.
 */
  
  protected static synchronized void writeToClipboard(String s)
  {
    java.awt.datatransfer.Clipboard c = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
    java.awt.datatransfer.StringSelection s2 = new java.awt.datatransfer.StringSelection(s);
    c.setContents(s2,s2);
  }

  protected static synchronized String readFromClipboard()
  {
    java.awt.datatransfer.Clipboard c = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
    java.awt.datatransfer.Transferable t = c.getContents("e");

    if(t.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor))
      try{
        return (String)t.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
      }
      catch(Exception ex){
      }
    return "";
  }
}
