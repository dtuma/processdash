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
package org.zaval.awt.image;

import java.awt.image.*;

public class BoxButtonFilter
extends ButtonImageFilter
{
    private int light, border, width, height;
    private boolean pressed;

    public void setup(int light, int border, int w, int h, boolean b)
    {
       this.light = light;
       this.border = border;
       width = w;
       height= h;
       pressed=b;
    }

    public Object clone()
    {
       ButtonImageFilter b = new BoxButtonFilter();
       b.setup(light, border, width, height, pressed);
       return b;
    }

    public int filterRGB(int x, int y, int rgb)
    {
        boolean brighter = light > 0;
        int percent = 100;
        int defpercent = Math.abs(light);

        int hb=height-border;
        int wb=width -border;

        if (x>=border && y>=border && y<=hb && x<=wb)
           if (pressed) {
               brighter = false;
               percent = defpercent >> 1;
           }
           else{
               brighter = true;
               percent = defpercent >> 1;
           //  return rgb & 0xFFFFFFFF;
           }
        else if (x < border && y < height - x){
              brighter = !pressed;
              percent = defpercent;
        }
        else if(y < border && x < width - y){
              brighter = !pressed;
              percent = defpercent;
        }
        else if (x >= wb || y >= hb) {
            brighter = pressed;
            percent = defpercent;
        }
        return filterRGB(rgb, brighter, percent);
    }

    public int filterRGB(int rgb, boolean brighter, int percent)
    {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = (rgb >> 0) & 0xff;
        int npercent = 100 - percent;
        if (brighter) {
            r = (255 - ((255 - r) * (npercent) / 100));
            g = (255 - ((255 - g) * (npercent) / 100));
            b = (255 - ((255 - b) * (npercent) / 100));
        } else {
            r = (r * (npercent) / 100);
            g = (g * (npercent) / 100);
            b = (b * (npercent) / 100);
        }
        return (r << 16) | (g << 8) | (b << 0) | 0xFF000000;
    }
}
