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

import java.io.*;
import java.awt.image.*;

// BMP Header, Win3.1 and on ( version 3 BMP )
// BMP Information header

class BMP_Info_Header
{
    public int   HeaderSize;
    public int   Width;
    public int   Height;
    public short NumOfPlanes;       // Must be 1
    public short BitsPerPixel;      // 1,4,8,16,24
    public int   CompressionMethod; // COMPRESS_XXX
    public int   BitmapSize;    // Size of Bitmap
    public int   HorizRes;      // Pixel per Meter
    public int   VertRes;       // Pixel per Meter
    public int   NumOfColors;   // How many colors needed
    public int   SiginificantColors;    // How many colors important

    public int readBytes = 0;

    public BMP_Info_Header(int w, int h, int bpp, int cn )
    {
        Width = w;
        Height = h;
        BitsPerPixel = (short)bpp;
        NumOfColors = cn;
    }

    public BMP_Info_Header(BMP_InputStream in)
    throws IOException
    {
        HeaderSize  =  in.readInt();
        Width       =  in.readInt();
        Height      =  in.readInt();
        NumOfPlanes =  in.readShort();
        if ( NumOfPlanes != 1 )
            throw new IllegalArgumentException("NumOfPlanes not 1"+ NumOfPlanes);

        BitsPerPixel = in.readShort();
        if ( BitsPerPixel != 24 && BitsPerPixel != 16 && BitsPerPixel != 8 &&
             BitsPerPixel != 4 && BitsPerPixel != 1 )
             throw new IllegalArgumentException("Illegal BitsPerPixel "+BitsPerPixel);

        CompressionMethod = in.readInt();
        if ( CompressionMethod < 0 || CompressionMethod > BMP_Header.COMPRESS_RLE4)
             throw new IllegalArgumentException("Illegal CompressionMethod "+CompressionMethod);
        BitmapSize  = in.readInt();
        HorizRes    = in.readInt();
        VertRes     = in.readInt();
        NumOfColors = in.readInt();
        SiginificantColors = in.readInt();
        readBytes=40;
    }
}
