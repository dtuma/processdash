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
package org.zaval.awt.dialog;

import org.zaval.awt.*;

import java.io.File;
import java.awt.*;
import java.util.*;

public class EditDialog
extends Dialog
{
   private TextField edit;
   private Button    ok, cancel;
   private boolean   isApply;
   private Component listener;
   private IELabel label;

   public EditDialog(Frame f, String s, boolean b, Component l)
   {
      super(f, s, b);
      setLayout(new GridBagLayout());

      label = new IELabel( "Name" );
      constrain(this, label, 0,0,1,1,
         GridBagConstraints.NONE,GridBagConstraints.WEST,
         0.0,0.0,5,5,5,5);
      constrain(this,edit = new TextField(20), 1,0,4,1,
         GridBagConstraints.HORIZONTAL,GridBagConstraints.WEST,
         1.0,0.0,5,5,5,5);

      ok     = new Button("Ok");
      cancel = new Button("Cancel");

      Panel p = new Panel();
      p.setLayout(new GridLayout(1,2,2,0));
      p.add (ok);
      p.add (cancel);

      constrain(this,p, 0,1,2,1,
         GridBagConstraints.NONE,
         GridBagConstraints.EAST,
         1.0,0.0,5,5,5,5);

      listener = l;
      pack();
   }

   public void setText(String t) {
     if (t == null) edit.setText("");
     else           edit.setText(t);
   }

   public String getText() {
     return edit.getText();
   }

   public void setButtonsCaption(String o, String c) {
     ok.setLabel(o);
     cancel.setLabel(c);
   }

   public void setLabelCaption( String l )
   {
      label.setText( l );
   }

   public boolean handleEvent(Event e)
   {
      if (e.id == Event.WINDOW_DESTROY || ( e.target == cancel && e.id == Event.ACTION_EVENT ) )
      {
        isApply = false;
        dispose();
      }

      if (e.target==ok && e.id==Event.ACTION_EVENT)
      {
        isApply = true;
        listener.postEvent(new Event(this, e.ACTION_EVENT, edit.getText()));
        dispose();
      }

      return super.handleEvent(e);
   }

   public boolean isApply() {
     return isApply;
   }

   public void toCenter()
   {
     Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
     Dimension d = getSize();
     move((s.width - d.width)/2, (s.height - d.height)/2);
   }

    static public void constrain(Container c, Component p,
        int x, int y, int w, int h,
        int f, int a,
        double wx, double wy,
        int t, int l,int r,int b)
    {
        GridBagConstraints cc = new GridBagConstraints();

        cc.gridx=x;
        cc.gridy=y;
        cc.gridwidth=w;
        cc.gridheight=h;

        cc.fill=f;
        cc.anchor=a;
        cc.weightx=wx;
        cc.weighty=wy;

        if(t+b+l+r>0)
                cc.insets = new Insets(t,l,b,r);
        LayoutManager lm = c.getLayout();
        if(lm instanceof GridBagLayout){
           GridBagLayout gbl = (GridBagLayout)lm;
           gbl.setConstraints(p,cc);
        }
        else if(lm instanceof ExGridLayout){
           ExGridLayout gbl = (ExGridLayout)lm;
           gbl.setConstraints(p,cc);
        }
        c.add(p);
    }

    static public void constrain(Container c, Component p,
        int x, int y, int w, int h)
    {
        constrain(c,p,x,y,w,h,
            GridBagConstraints.NONE,
            GridBagConstraints.NORTHWEST,
            1.0,1.0,
            0,0,5,3);
    }

    static public void constrain(Container c, Component p,
        int x, int y, int w, int h,
        int f, int a)
    {
        constrain(c,p,x,y,w,h,f,a,
            1.0,1.0,
            0,0,5,3);
    }

    public boolean keyDown( Event e, int key ){
      if ((e.target == ok && key == Event.ENTER) || (e.target == edit && key == Event.ENTER)){
        isApply = true;
        listener.postEvent(new Event(this, e.ACTION_EVENT, edit.getText()));
        dispose();
        return true;
      }
      if ( e.target == cancel && key == Event.ENTER ){
        isApply = false;
        dispose();
        return true;
      }
      return false;
    }
}
