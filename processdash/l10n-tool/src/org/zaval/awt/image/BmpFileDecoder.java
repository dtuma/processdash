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
import java.lang.*;
import java.awt.*;
import java.awt.image.*;

public class BmpFileDecoder
{
    public static int BmpHints = 30;

    BMP_Header header;
    BMP_Info_Header info_header;
    BMP_Palette palette;

    public int[][] bmp_image;
    private BMP_InputStream is;

    public int[][] getMatrix()
    {
       return bmp_image;
    }

    public BmpFileDecoder(InputStream is)
    throws IOException
    {
      init(is);
    }

    public void init(InputStream is_in)
    throws IOException
    {
        this.is = new BMP_InputStream(is_in);
        header      = new BMP_Header(is);
        info_header = new BMP_Info_Header(is);

        if (info_header.NumOfColors != 0 )
        {
            palette = new BMP_Palette(info_header.NumOfColors, is, info_header.BitsPerPixel);
        }
        else {
            // MS Bug workaround.
            // I have to guess how long the Color Table is.
            long at = header.readBytes + info_header.readBytes;
            info_header.NumOfColors = (int) (header.dataOffset - at) / 4;
            palette = new BMP_Palette(info_header.NumOfColors, is, info_header.BitsPerPixel);
        }

        // Now we have all the information. Extract the data itself.

        bmp_image = new int[getHeight()][getWidth()] ;
        int ByteWidth = getByteWidth();
        int pad = ByteWidth % 4;

        // If the file is compress, I decompress immediatly into
        // bmp_image. If not, I first read into tmp_buffer and then
        // transle into bmp_image, if needed.

        if ( info_header.CompressionMethod != BMP_Header.COMPRESS_RGB ) {
           if ( info_header.BitsPerPixel == 8 || info_header.BitsPerPixel == 4 )
                   readRLE(ByteWidth, pad, is,     8/(info_header.CompressionMethod) );
           else throw new IllegalArgumentException("Can't uncompress "+ info_header.BitsPerPixel + "bit files");
        } else { //  No compression
            int tmp_buffer[][] = new int[getHeight()][ByteWidth];
            readRegular(ByteWidth,pad,tmp_buffer, is);
            int w, h, bb;

            // put data into image_data

            int ands[] = { 128,64,32,16,8,4,2,1 };

            if ( info_header.BitsPerPixel == 8 )
                for ( h = 0 ; h < getHeight() ; h++ )
                    for ( w = 0 ; w < ByteWidth ; w++ )
                            bmp_image[h][w] = tmp_buffer[h][w];
            else if ( info_header.BitsPerPixel == 4 )
                for ( h = 0 ; h < getHeight() ; h++ )
                    for ( w = 0 ; w < ByteWidth ; w++ ) {
                        bmp_image[h][w*2] = MSN(tmp_buffer[h][w]);
                        bmp_image[h][w*2+1] = LSN(tmp_buffer[h][w]);
                    }
            else if ( info_header.BitsPerPixel == 1 )
            {
                for ( h = 0 ; h < getHeight() ; h++ )
                    for ( w = 0 ; w < ByteWidth ; w++ )
                        for ( bb = 0 ; bb < 8 ; bb++ )
                            if ( w*8+bb < getWidth() )
                                if ( ((tmp_buffer[h][w])&(ands[bb])) == ands[bb] )
                                    bmp_image[h][w*8+bb] = 1;
                                else bmp_image[h][w*8+bb] = 0;
            }
            else  if (info_header.BitsPerPixel == 24)
                for ( h = 0 ; h < getHeight() ; h++ )
                    for ( w = 0 ; w < getWidth() ; w++ )
                        bmp_image[h][w] = 0xff000000+ ( tmp_buffer[h][w*3]<<16) +
                            ( tmp_buffer[h][w*3+1] << 8 ) + tmp_buffer[h][w*3+2] ;
            else  if (info_header.BitsPerPixel == 16)
                for ( h = 0 ; h < getHeight() ; h++ )
                    for ( w = 0 ; w < getWidth() ; w++ ) {
                        int pixel = (tmp_buffer[h][w*2]) + (tmp_buffer[h][w*2+1] << 8);
                        int r = ((pixel & 0x7c00) >> 10) * 255 / 31;
                        int g = ((pixel & 0x03e0) >>  5) * 255 / 31;
                        int b = (pixel & 0x1f) * 255 / 31;
                        bmp_image[h][w] = 0xff000000 + (b << 16) + (g << 8) + r;
                    }
            else throw new IllegalArgumentException("Illegal BitsPerPixel "+info_header.BitsPerPixel);
        }
    }

    protected void readRegular(int ByteWidth,int pad,int tmp_buffer[][], InputStream iss)
    throws IOException
    {
        for ( int h = 0 ; h < getHeight() ; h++ )  {
            for ( int w = 0 ; w < ByteWidth ; w++ )
                tmp_buffer[h][w] = iss.read();
            if ( pad != 0 )
                for ( int ppad = 0 ; ppad < (4-pad) ; ppad++ ) iss.read();
        }
    }

    public int getWidth()
    {
        return info_header.Width;
    }

    public int getHeight()
    {
        return info_header.Height;
    }

        // 0,0 is top-left, unlike in BMP
    public int getPixel(int x, int y )
    {
        return bmp_image[getHeight()-y-1][x] ;
    }

    public void setPixel(int x, int y, int c )
    {
        bmp_image[getHeight()-y-1][x]  = c ;
    }

    public ColorModel getColorModel()
    {
        return palette.getColorModel();
    }

    private int LSN(int value)
    {
        return ((value) & 0x0f) ;
    }

    private int MSN(int value)
    {
        return (((value) & 0xf0) >> 4);
    }

    private int getByteWidth()
    {
        int ByteWidth = 0 ;
        if ( info_header.BitsPerPixel == 8 ) ByteWidth = getWidth();
        else if ( info_header.BitsPerPixel == 4 ) {
                ByteWidth = getWidth() / 2;
                if ( ByteWidth*2 < getWidth() ) ByteWidth++;
        } else if ( info_header.BitsPerPixel == 1 ) {
                ByteWidth = getWidth() / 8;
                if ( ByteWidth*8 < getWidth() ) ByteWidth++;
        } else if (info_header.BitsPerPixel == 24) ByteWidth = getWidth()*3;
        else if (info_header.BitsPerPixel == 16) ByteWidth = getWidth()*2;
        else throw new IllegalArgumentException("Illegal BitsPerPixel "+info_header.BitsPerPixel);
        return ByteWidth;
    }

    protected void readRLE(int ByteWidth,int pad, BMP_InputStream in, int pixelSize)
    throws java.io.IOException
    {
       int x = 0;
       int y = 0 ;

       for (int i=0; i < header.FileSize ; i++ ) {
           int byte1 = in.read();
           int byte2 = in.read();
           i+=2;

           // If byte 0 == 0, this is an escape code
           if (byte1 == 0) {
               // If escaped, byte 2 == 0 means you are at end of line
               if (byte2 == 0) {
                   x=0;
                   y++;
               // If escaped, byte 2 == 1 means end of bitmap
               } else if ( byte2 == 1 ) return;
               // if escaped, byte 2 == 2 adjusts the current x and y by
               // an offset stored in the next two words
               else if (byte2 == 2) {
                   int xoff = (char) in.readShort();
                   i+= 2;
                   int yoff = (char) in.readShort();
                   i+=2;
                   x += xoff;
                   y +=yoff;
               // Any other value for byte 2 is the number of bytes that you
               // should read as pixel values (these pixels are not run-length
               // encoded ).
               } else {
                   int whichBit = 0;
                   int currByte = in.read();
                   i++;

                   for (int j=0; j < byte2; j++) {
                       if (pixelSize == 4) {
                           if (whichBit == 0) bmp_image[y][x] = (currByte>> 4 ) & 0xf;
                           else {
                                   bmp_image[y][x] = currByte & 0xf ;
                                   currByte = in.read();
                                   i++;
                           }
                           whichBit = (whichBit == 0) ? 1 : 0;
                       } else {
                           bmp_image[y][x] = currByte;
                           currByte = in.read();
                           i++;
                       }
                       x++;
                       if (x >= getWidth()) {
                           x = 0;
                           y++;
                       }
                   }

                   if ((byte2 & 1) == 1) {
                       in.read();
                       i++;
                   }
               }
           // If the first byte was not 0, it is the number of pixels that
           // are encoded by byte 2
           } else {
               for (int j=0; j < byte1; j++) {
                   if (pixelSize == 4) {
                       if ((j & 1) == 0) bmp_image[y][x] = (byte2 >> 4) & 0xf;
                       else bmp_image[y][x] = byte2 & 0xf ;
                   } else bmp_image[y][x] = byte2;
                   x++;
                   if (x >= getWidth()) {
                       x = 0;
                       y++;
                   }
               }
           }
       }
    }
}

