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
package org.zaval.awt.peer;

import org.zaval.awt.*;

import java.awt.*;
import java.util.*;

public class TreeNode
{
    public   ImageResolver imgres=null;

    public   TreeNode sibling;
    public   TreeNode child;
    public   TreeNode parent;
    public   String   text;
    public   String   nameCollImage  = null;
    public   String   nameExpImage   = null;
    public   Image    collapsedImage = null;
    public   Image    expandedImage  = null;
    public   int      depth = -1;
    public   boolean  isExpanded = false;
    public   int      numberOfChildren;
    public   int      contextMenu = -1;
    public   Hashtable  property = null;
    public   boolean  hidden = false;
    public   String   caption;
    public   Image    indicator;

   //constructors

    public void setResolver(ImageResolver imgres) {
      this.imgres=imgres;
    }

    public TreeNode(String text)  {
      this(text, null, null);
    }

    public TreeNode(String text, String nameCollImage, String nameExpImage)
    {
        property            = new Hashtable();
        this.text           = text;
        this.sibling        = null;
        this.child          = null;
        this.nameCollImage  = nameCollImage;
        this.nameExpImage   = nameExpImage;

        if (nameCollImage != null && imgres != null)
          this.collapsedImage = imgres.getImage(nameCollImage);

        if (nameExpImage != null && imgres != null)
          this.expandedImage = imgres.getImage(nameCollImage);

        numberOfChildren = 0;
        caption          = null;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getDepth()  {
        return depth;
    }

    public boolean isExpanded()  {
        return !hidden && isExpanded;
    }

    public boolean isExpandable() {
        return !hidden && (child!=null);
    }

    public void expand()  {
        if (isExpandable()) isExpanded=true;
    }

    public void collapse() {
        isExpanded = false;
    }

    public void toggle()
    {
        if (isExpanded)
        {
            collapse();
        }
        else if (isExpandable())
        {
            expand();
        }
    }

    public Image getImage()
    {
        return ((isExpanded && (expandedImage != null))
                ? expandedImage
                : collapsedImage);
    }

    public Image getExpandedImage()  {
        return (expandedImage != null) ? expandedImage : collapsedImage;
    }

    public Image getCollapsedImage() {
        return collapsedImage;
    }

    public String getNameImage()
    {
      if (getImage() != null )
        return ((isExpanded && (expandedImage != null))
                ? nameExpImage
                : nameCollImage);
      return null;
    }

    public void setExpandedImage(String image)
    {
      this.nameCollImage  = image;
      if (image != null && imgres!=null)
       this.collapsedImage = imgres.getImage(nameCollImage);
    }

    public void setCollapsedImage(String image)
    {
      this.nameExpImage   = image;
      if (image != null && imgres!=null)
       this.expandedImage  = imgres.getImage(nameExpImage );
    }

    public String getText() {
        return text;
    }

    public void setText(String s) {
        text = s;
    }

    public void setContextMenu(int index) {
      contextMenu = index;
    }

    public int getContextMenu() {
      return contextMenu;
    }

    public Object getProperty(String name)  {
     return property.get(name) ;
    }

    public String getStringProperty(String name) {
     return (String)getProperty(name);
    }

    public int getIntProperty(String name)  {
     return Integer.parseInt((String)getProperty(name));
    }

    public void setProperty(String name, Object value)
    {
     property.put(name, value) ;
    }

    public void setStringProperty(String name, String value)  {
      setProperty(name, value);
    }

    public void setIntProperty(String name, int value)  {
      setProperty(name, ""+value);
    }

    public int getNumberOfChildren()  {
     return  numberOfChildren;
    }

    public void setHide(boolean b)    {
       hidden=b;
    }

    public boolean getHide() {
       return hidden;
    }

    public String getCaption()  {
       return caption==null?text:caption;
    }

    public void setCaption(String c)  {
       caption=c;
    }

    public void setCollapsedImage(Image image) {
      this.collapsedImage   = image;
    }

    public void setExpandedImage(Image image) {
      this.expandedImage  = image;
    }

    public void setIndicator (String name)
    {
      if (name == null || imgres == null) indicator = null;
      else indicator = imgres.getImage(name);
    }

    public Image getIndicator()
    {
      return indicator;
    }
}
