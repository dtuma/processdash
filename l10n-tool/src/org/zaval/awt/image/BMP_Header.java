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

class BMP_Header
{
    static final int   COMPRESS_RGB  = 0;
    static final int   COMPRESS_RLE8 = 1;
    static final int   COMPRESS_RLE4 = 2;
    static final short BMP_ID        = 0x4d42;

    public short    FileType;       // Must be BMP_ID
    public int      FileSize;
    public short    res1;
    public short    res2;
    public int      dataOffset;

    public int readBytes = 0;

    public  BMP_Header(BMP_InputStream in)
    throws IOException
    {
        FileType = in.readShort();
        if ( FileType != BMP_ID )
           throw new IllegalArgumentException("FileType not BMP: "+ FileType);

        FileSize = in.readInt();
        if ( FileSize < 0 )
           throw new IllegalArgumentException("FileSize < 0 "+ FileSize);

        res1 = in.readShort();
        res2 = in.readShort();
        dataOffset = in.readInt();
        readBytes=14;
    }

    public String toString()
    {
        String res = new String("type "+FileType+" size "+FileSize+" offset "+ dataOffset+ "reses: "+res1+" "+res2);
        return res;
    }
}



