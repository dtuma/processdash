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
import java.util.Vector;

public class IEChoice
extends Panel
{
    private int wx=0, wy=0;
    private int x=0,y=0;
    private Choice ch;
    private boolean fakeAdded = false;

    private Vector ids;
    private Vector items;
    private String lastval = null;

    public Vector getI1()
    {
       return items;
    }

    public Vector getI2()
    {
       return ids;
    }

    public void setItems(Vector items, Vector ids)
    {
       int i, k = ids.size();
       this.ids = ids;
       this.items = items;

    // System.err.println("+++ " + ids);

       Choice c = new Choice();
       for(i=0;i<k;++i) c.addItem((String)items.elementAt(i));
       for(;i<2;++i) c.addItem(" ");
       setChoice(c);
    }

    public void select(String value)
    {
    // System.err.println("+++ set " + value + " " + ids);
       if(value==null) value = lastval;
       if(value==null) return;
       select(ids.indexOf(lastval = value));
    }

    public String getValue()
    {
       if(lastval==null){
          try{
             int x = ch.getSelectedIndex();
             String s = (String)ids.elementAt(x);
             return lastval = s;
          }
          catch(Exception e){
             return null;
          }
       }
    // System.err.println("+++ get " + lastval);
       return lastval;
    }

    public IEChoice(Choice c)
    {
       super();
       ch = c;
       setLayout(new FitLayout(0,0,0,0));
       add(this.ch);
    }

    public void resize(int wx, int wy)
    {
        Dimension d = ch.preferredSize();
        if(d.height==0){
           Font font = getFont();
           if(font == null) font = getParent().getFont();
           if(font != null){
              FontMetrics  fm = getFontMetrics(font);
              if(fm==null) fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
              d.height = fm.getHeight() + 10;
           }
           else d.height=wy;
        }
        super.resize(wx,d.height);
    }

    public Choice getChoice()
    {
       return ch;
    }

    private void setChoice(Choice x)
    {
       String     osName = System.getProperty("os.name");
       boolean solaris = (osName != null &&
          (osName.equalsIgnoreCase("Solaris") ||
          osName.equalsIgnoreCase("Linux")));

       Color c1=ch.getBackground();
       Color c2=ch.getForeground();
       Font f=ch.getFont();

       Container parent = getParent();
       if(solaris && parent!=null) parent.remove(this);
       removeAll();
       add(x);

       x.setBackground(c1);
       x.setForeground(c2);
       x.setFont(f);
       if(isEnabled()) x.enable();
       else x.disable();

       this.ch = x;
       if(solaris && parent!=null){
          parent.add(this);
          addNotify();
       }

       ((Component)this).invalidate();
       ((Component)this).validate();
       this.ch.requestFocus();
    }

    public void enable()
    {
       ch.enable();
       super.enable();
    }

    public void disable()
    {
       ch.disable();
       super.disable();
    }

    private final static int acceptE[]={
        Event.LIST_SELECT,
        Event.ACTION_EVENT
    };

    public boolean handleEvent(Event e)
    {
    // if(e.id==e.GOT_FOCUS) return true;
    // if(e.id==e.LOST_FOCUS && e.target!=ch) return false;
       if(e.target != this && e.target != ch) return super.handleEvent(e);
    // if (e.id!=e.MOUSE_MOVE && e.id!=e.MOUSE_ENTER && e.id!=e.MOUSE_EXIT)
    //    System.err.println(e);
       if (isSelectionEvent(e)){
          try{
             String s = (String)ids.elementAt(ch.getSelectedIndex());
             sendSelectionEvent(s);
          }
          catch(Exception eflt){
          }
          return true;
       }
    // if (e.target == ch) e.target = this;
       return super.handleEvent(e);
    }

    public boolean keyDown(Event e, int key)
    {
      char c = (char)key;
      if (Character.isLetter(c)){
         c = Character.toLowerCase(c);
         int size = ch.countItems();
         for (int i=0; i<size; i++){
            StringBuffer item = new StringBuffer(ch.getItem(i));
            if  (Character.toLowerCase(item.charAt(0)) == c){
               if (ch.getSelectedIndex()!=i) ch.select(i);
               return true;
            }
         }
      }

      if (key == (char)0x1B){
         try{
            ch.select(ids.indexOf(lastval));
         }
         catch(Exception eee){
         }
         return false;
      }

      if (key == '\n' || key=='\t'){
         try{
            String s = (String)ids.elementAt(ch.getSelectedIndex());
            if(s.equals(lastval)) return false;
            sendSelectionEvent(s);
         }
         catch(Exception eee){
         }
         return false;
      }

      if (key == Event.UP){
         int z = ch.getSelectedIndex() - 1;
         if(z<0) return true;
         ch.select(z);
         return true;
      }

      if (key == Event.DOWN){
         int z = ch.getSelectedIndex() + 1;
         if(z>=ch.countItems()) return true;
         ch.select(z);
         return true;
      }

      return super.keyDown(e, key);
    }

    public void requestFocus()
    {
       ch.requestFocus();
    }

    public boolean lostFocus(Event e, Object o)
    {
       if(e.target==this.ch && ids!=null && lastval!=null){
          try{
             String s = (String)ids.elementAt(ch.getSelectedIndex());
             if(lastval.equals(s)) return true;
             sendSelectionEvent(s);
          }
          catch(Exception eee){
          }
          return false;
       }
       else if(e.target==this) return true;
    // return true;
       return false;
    }

    public boolean gotFocus(Event e, Object o)
    {
       if(ids!=null) return false;
       return true;
    }

    private boolean isSelectionEvent(Event e)
    {
       if(e.target == ch) // && e.arg instanceof String)
          for(int i=0;i<acceptE.length;++i)
             if (acceptE[i]==e.id) return true;
       return false;
    }

    private void sendSelectionEvent(String s)
    {
       if(!s.equals(lastval)){
       // System.err.println("+++ sse " + s + "," + lastval);
          lastval = s;
          getParent().postEvent(new Event(this,Event.ACTION_EVENT,s));
       }
    // fakeFix();
    }

    private void fakeFix()
    {
       if(fakeAdded){
          Choice x = new Choice();
          Choice v = ch;
          int j, k = v.countItems() - 1;
          for(j=0;j<k;++j){
             String s = v.getItem(j);
             if(j==k-1 && s.trim().length()==0) break;
             x.addItem(s);
          }
          fakeAdded = false;
          setChoice(x);
          select(lastval);
       }
    }

    public void addNotify()
    {
       try{
          super.addNotify();
       }
       catch(Exception efck){
       // System.err.println("INI Error: " + efck + " for " + getChoice());
       // efck.printStackTrace();
       }
    }

    public void select(int i)
    {
    // System.err.println("+++ set " + i);
       if(i<0 && !checkIeHack()){
          Choice x = getChoice();
          int j = 0, k = x.countItems();
          for(j=0;j<k;++j)
             if(x.getItem(j).trim().length()==0){
                x.select(j);
                return;
             }
          x.addItem(" ");
          x.select(" ");
          fakeAdded = true;
          return;
       }
       fakeFix();
       try{
          getChoice().select(i);
       }
       catch(Exception efck){
          System.err.println("SEL Error: " + efck + " for " + getChoice());
          efck.printStackTrace();
       }
    }

    private static boolean checkIeHack()
    {
       String jver = "1.0.2.";
       String jven = "Sun";
       try {
         jver = System.getProperty("java.version");
       }
       catch(Throwable t){
       }
       try{
         jven = System.getProperty("java.vendor");
       }
       catch(Throwable t){
       }
   
       if(!jver.startsWith("1.0") && jven.startsWith("Microsoft")) return true;
       return false;
    }
}
