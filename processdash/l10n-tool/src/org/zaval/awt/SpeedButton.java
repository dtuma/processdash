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

import org.zaval.awt.image.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class  SpeedButton
extends Canvas
{
  private static final int FREE = 1;
  private static final int UP   = 2;
  private static final int DOWN = 3;

  private Image  normImg, upImg, disImg, downImg;

  private int    w, h, state = FREE;

  public void setImageSize(Dimension d)
  {
     w = d.width;
     h = d.height;
  }

  public SpeedButton(Image src, int border, int light, ButtonImageFilter filt)
  {
      w = src.getWidth (this);
      h = src.getHeight(this);
      if(border<0){
         border = -border;
         border = (w * border) / 100;
         if(border<2) border = 2;
      }
      init (src,
         getFilter(src, filt, light, border, false),
         getFilter(src, filt, -light, border, true),
         getFilter(src, filt, -Math.abs(light), 0, false));
  }

  public SpeedButton(Image src, Image up, Image down, Image dis)
  {
    if(src==null) w = h = 0;
    else{
       w = src.getWidth (this);
       h = src.getHeight(this);
    }
    init(src, up, down, dis);
  }

  public SpeedButton(Image src)
  {
    this(src, -5, 30, new BoxButtonFilter());
  }

  private void init(Image src, Image up, Image down, Image dis)
  {
    normImg = src;
    upImg = up;
    disImg = dis;
    downImg = down;
  }

  public Image getFilter(Image src, ButtonImageFilter filt, int light, int border, boolean b)
  {
    filt = (ButtonImageFilter)filt.clone();
    filt.setup(light, border, w, h, b);

  //ImageFilter cropfilter = new CropImageFilter(0, 0, w, h);
  //ImageProducer prod = new FilteredImageSource(src.getSource(), cropfilter);
    ImageProducer prod = src.getSource();
    ImageProducer ip = new FilteredImageSource(prod,filt);
    return createImage(ip);
  }

  public int  getState()
  {
     return state;
  }

  public void setState(int s)
  {
     state=s;
     repaint();
  }

  public Dimension preferredSize()
  {
    if(w<0) w=0;
    if(h<0) h=0;
    return new Dimension(w, h);
  }

  public void paint(Graphics gr)
  {
    Dimension d = size();
    int ww=d.width;
    int hh=d.height;

    if(disImg!=null && !isEnabled())      gr.drawImage(disImg,  0, 0 , ww, hh, this);
    else if(state==FREE && normImg!=null) gr.drawImage(normImg, 0, 0 , ww, hh, this);
    else if(state==DOWN && downImg!=null) gr.drawImage(downImg, 0, 0 , ww, hh, this);
    else if(state==UP && upImg!=null)     gr.drawImage(upImg,   0, 0 , ww, hh, this);
  }

  public boolean mouseUp(Event ev, int x, int y)
  {
     if (!isEnabled() || state == FREE) return true;
     setState(UP);
     getParent().postEvent(new Event(this, Event.ACTION_EVENT, null));
     return true;
  }

  public boolean mouseDown(Event ev, int x, int y)
  {
     if (!isEnabled()) return true;
     setState(DOWN);
     return true;
  }

  public boolean mouseEnter(Event ev, int x, int y)
  {
     setState(UP);
     return true;
  }

  public boolean mouseExit(Event ev, int x, int y)
  {
     setState(FREE);
     return true;
  }
}
