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

public class MultiLabel
extends Canvas
{
    public final static int LEFT = 0;
    public final static int CENTER = 1;
    public final static int RIGHT = 2;
    protected String[] lines;
    protected int lcount = 0;
    protected int width = 0, height, ascent;
    protected int[] widths;
    protected int align = LEFT;
    public int LMAX = 50;
    protected boolean isVc = false;
    protected boolean withHeader = false;
    protected Font flf = new Font("Dialog",Font.BOLD,10);

    public void setText(String text)
    {
        newLabel(text);
        width=0;
        measure();
        repaint();
    }
    public void setTitleFont(Font fx)
    {
        flf=fx;
        width=0;
        measure();
        repaint();
    }
    public boolean isHeaderPainting(){ return withHeader; }
    public void headerPaint(boolean s){ withHeader=s;}
    public Font getTitleFont()
    {
        return flf;
    }
    public MultiLabel(String text, int align, int LMAX)
    {
        super();
        this.align=align;
        this.LMAX = LMAX;
        newLabel(text);
        //measure();
    }
    public MultiLabel(String text, int align)
    {
        this(text,align,40);
    }
    public MultiLabel(String text)
    {
        this(text,LEFT,40);
    }
    public void setFont(Font x)
    {
        super.setFont(x);
        width=0;
        measure();
        repaint();
    }
    public void setForeground(Color color)
    {
        super.setForeground(color);
        repaint();
    }
    public void addNotify(){ super.addNotify();measure();}
    public Dimension preferredSize()
    {
        return new Dimension(width,height*lcount);
    }
    public Dimension minimumSize()
    {
        return new Dimension(width,height*lcount);
    }
    public void measure()
    {
        Font x = this.getFont();
        if(x==null) return;
        FontMetrics fm = getFontMetrics(x);
        if(fm==null) return;
        height=fm.getHeight();
        ascent=fm.getAscent();
        int i;
        for(i=0;i<lcount;++i){
            widths[i]=fm.stringWidth(lines[i]);
            if(width<widths[i]) width=widths[i];
        }
    }
    private void newLabel(String text)
    {
        StringTokenizer st = new StringTokenizer (text,"^",true);
        lcount = st.countTokens()+1;
        lines  = new String[lcount*5];
        widths = new int[lcount*5];
        int i,j=0,f=0;
        for(i=0;i<lcount-1;++i){
            String x = st.nextToken();
            if(x.equals("^")) ++f;
            else f=0;
            if(f>1 || f==0){
                if(f>0) lines[j++]=" ";
                else{
                    while(x.length()>LMAX){
                        int k = LMAX;
                        k = x.lastIndexOf(" ",LMAX);
                        if(k<0) k = LMAX;
                        lines[j++]=x.substring(0,k);
                        x = x.substring(k,x.length());
                    }
                    lines[j++]=x;
                }
            }
            if(j==lcount*5-1) break;
        }
        lcount=j;
    }
    public void vertCenter(boolean isVc)
    {
        this.isVc = isVc;
    }

    private boolean fullrepaint=false;

    public void repaint()
    {
        synchronized(this) {
            fullrepaint = true;
        }
        super.repaint();
    }

    public void repaint(int x, int y, int w, int h)
    {
        synchronized(this) {
            fullrepaint = true;
        }
        super.repaint(x,y,w,h);
    }

    public void paint(Graphics gr)
    {
        synchronized(this) {
            fullrepaint = false;
        }
        int x,y,i;
        if(gr==null) return;
        else if(width==0) measure();
        Dimension d = size();
        gr.clearRect(0,0,d.width,d.height);
        y=ascent+ 1;//(d.height-lcount*height)/2;
        if(withHeader){
            x=(d.width-widths[0])/2;
            Font cur = gr.getFont();
            gr.setFont(flf);
            gr.drawString(lines[0],x,y);
            gr.setFont(cur);
            y+=height;
            i=1;
        }
        else i=0;
        if(isVc) y+=(d.height-height*lcount)/2;
        for(;i<lcount;++i){
            if(align==LEFT) x=1;
            else if(align==RIGHT) x=d.width-1-widths[i];
            else x=(d.width-widths[i])/2;
            gr.drawString(lines[i],x,y);
            y+=height;
        }
    }
}
