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

public interface AlignConstants
{
  public static final int LEFT   = 0x1;
  public static final int RIGHT  = 0x2;
  public static final int TOP    = 0x4;
  public static final int BOTTOM = 0x8;
  public static final int TLEFT  = LEFT  | TOP;
  public static final int TRIGHT = RIGHT | TOP;
  public static final int BLEFT  = LEFT  | BOTTOM;
  public static final int BRIGHT = RIGHT | BOTTOM;
  public static final int CENTER = 0;


  public static final String STR_LEFT   = "left";
  public static final String STR_RIGHT  = "right";
  public static final String STR_TOP    = "top";
  public static final String STR_BOTTOM = "bottom";
  public static final String STR_TLEFT  = "topLeft";
  public static final String STR_TRIGHT = "topRight";
  public static final String STR_BLEFT  = "bottomLeft";
  public static final String STR_BRIGHT = "bottomRight";
  public static final String STR_CENTER = "center";
}
