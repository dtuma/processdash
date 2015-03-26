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

public class TextAlignArea
extends AlignArea
{
  public static final int STRING_GAP = 0;

  private String      text;
  private String[]    strs = new String[0];
  private String      deliver = "/p";
  private FontMetrics fontMetrics = null;
  private boolean     isMulti = false;

  public TextAlignArea() {
    setAlign(Align.LEFT);
  }

  protected int getWidth(int s, Dimension size)
  {
    if (fontMetrics == null || text == null)  return -1;

    if (!isMulti)
      return fontMetrics.stringWidth(text);

    int max = -1;
    for (int i=0; i<strs.length; i++)
    {
      int len = fontMetrics.stringWidth(strs[i]);
      if (max < len) max = len;
    }
    return max;
  }

  protected int getHeight(int s, Dimension size)
  {
    if (fontMetrics == null || text == null)  return -1;
    if (!isMulti)
      return fontMetrics.getHeight();
    return fontMetrics.getHeight() * strs.length + (strs.length - 1) * STRING_GAP;
  }

  protected void recalc() {
    Dimension d = getSize();
    if(fontMetrics==null) strs = null;
    else strs = breakString(text, fontMetrics, d.width);
  }

  public void setMultiLine(boolean b)
  {
    if (isMulti == b) return;
    isMulti = b;
    invalidate();
  }

  public void setFontMetrics(FontMetrics f)
  {
    fontMetrics = f;
    invalidate();
  }

  public void setText(String t)
  {
    if (t != null && text != null && t.equals(text)) return;
    text = t;
    invalidate();
  }

  public String getText() {
    return text;
  }

  public FontMetrics getFontMetrics() {
    return fontMetrics;
  }

  public void draw(Graphics g, Color col) {
    draw(g, 0, 0, col);
  }

  public void draw(Graphics g, int offx, int offy, Color col)
  {
    Dimension d   = getSize();
    Insets    ins = getInsets();
    Rectangle r   = getAlignRectangle();

    int h = fontMetrics.getHeight();
    int y = r.y + fontMetrics.getHeight() + offy -  fontMetrics.getDescent();
    g.setColor(col);

    int x = r.x + offx;
    if (isMulti)
      for (int i=0; i<strs.length; i++)
      {
        if ((getAlign() & Align.RIGHT)>0)
        {
          int len = fontMetrics.stringWidth(strs[i]);
          if (len > d.width) x = ins.left + offx;
          else               x = r.x + offx + r.width - len;
        }
        g.drawString(strs[i], x, y);
        y += (h + STRING_GAP);
      }

    else
      g.drawString(text, r.x, y);
  }

  public static void next(String s, String[] res)
  {
    int index = s.indexOf(' ');
    if (index < 0)
    {
       res[0] = s;
       res[1] = null;
       return;
    }

    if (index == 0) index++;
    res[0] = s.substring(index);
    res[1] = s.substring(0, index);
  }

  public static String[] breakString(String t, FontMetrics fm, int width)
  {
    if (t != null)
    {
      StringTokenizer st = new StringTokenizer(t, "\n");
      Vector          vv = new Vector();

      while (st.hasMoreTokens()) {
        vv.addElement(st.nextToken());
      }

      Vector   vvv   = new Vector();
      String[] ps    = new String[2];

      for (int i=0; i<vv.size(); i++)
      {
        String ss = (String)vv.elementAt(i);

        String tk = "";
        int    tw = 0;
        int    c  = 0;

        for (;;)
        {
          String token = null;
          next(ss, ps);

          if (ps[1] != null) token = ps[1];
          else               token = ps[0];
          ss = ps[0];

          int len = fm.stringWidth(token);
          if ((tw + len) > width)
          {
            if (tk.length() > 0)
            {
              vvv.addElement(tk);
              tk = token;
              tw = len;
            }
            else
            {
              vvv.addElement(token);
              tk = "";
              tw = 0;
            }
          }
          else
          {
            c++;
            tk += token;
            tw += len;
          }

          if (ps[1] == null) break;
        }

        if (c > 0) vvv.addElement(tk);

      }

      String[] strs = new String[vvv.size()];
      for (int i=0; i<vvv.size(); i++) strs[i] = (String)vvv.elementAt(i);
      return strs;
    }
    return null;
  }
}
