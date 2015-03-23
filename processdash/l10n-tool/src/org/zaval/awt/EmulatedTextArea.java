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
import org.zaval.awt.ScrollObject;
//import org.zaval.awt.ScrollPanel;

public class EmulatedTextArea
extends EmulatedTextField
implements ScrollObject
{
  boolean      wordWrap       = false;
  int []       lineStart      = new int[LINE_INCR];
  Vector       lineText       = new Vector();
  int          maxTextWidth   = 0;
  int          rowsNumber     = 0; // = 0 - preferredSize.height is determined by the actual number of rows
  int          prefWidth      = 0; // = 0 - preferredSize.width is determined by it if it is assigned a positive value
  int          upRowNum       = 0;
  int          baseTextIndent = 0;
  int          viewWidth      = 0;
  int          viewHeight     = 0;
  Image        internImg      = null;
  int          textWidth      = 0;
  int          textHeight     = 0;
  int          lastVisLine    = 0;
  boolean      addLineFeed    = true;
  boolean      noFontMetric   = false;

  public static final int LINE_INCR = 20;

  public EmulatedTextArea()
  {
     super();
//     insets.right = 2;
//     insets.bottom = 2;
     lineText.addElement( "" );
  }

  public EmulatedTextArea( boolean ww, boolean lf, int rN, int pW )
  {
     this();
     wordWrap = ww;
     rowsNumber = rN;
     prefWidth = pW;
     addLineFeed = lf;
  }

  public void setLineFeed( boolean b )
  {
     addLineFeed = b;
  }

  public void setWordWrap( boolean b )
  {
     wordWrap = b;
  }

  public void setPrefWidth( int p )
  {
     prefWidth = p;
  }

  public void setRowsNumber( int n )
  {
     rowsNumber = n;
  }

  public void setText(String s)
  {
     super.setText( s );
     recalcLines( 0 );
  }

  public boolean gotFocus(Event e, Object obj)
  {
     int wasCP = cursorPos;
     boolean res = super.gotFocus( e, obj );
     selPos = 0;
     selWidth = 0;
     cursorPos = wasCP;
     return res;
  }

  protected boolean controlKey(int key, boolean shift)
  {
    switch (key)
    {
      case Event.DOWN: seek( vertPosShift( cursorPos, 1 ) - cursorPos, shift);  break;
      case Event.UP  : seek( vertPosShift( cursorPos, - 1 ) - cursorPos, shift);  break;
      case Event.HOME: seek(lineStart[lineFromPos(cursorPos)]-cursorPos,shift); break;
      case Event.END :
         int ln = lineFromPos(cursorPos);
         int newPos = buffer.toString().length();
         if ( ln < lineText.size() - 1 ) newPos = adjustPos( lineStart[ln+1] - 1, false );
         seek( newPos - cursorPos, shift );
         break;
      case '\n'      : return false;
      case Event.PGUP:
         upRowNum -= lastVisLine;
         if ( upRowNum < 0 ) upRowNum = 0;
         seek( vertPosShift( cursorPos, - lastVisLine ) - cursorPos, shift);
         break;
      case Event.PGDN:
         upRowNum += lastVisLine;
         if ( upRowNum + lastVisLine >= lineText.size() ) upRowNum = lineText.size() - lastVisLine - 1;
         seek( vertPosShift( cursorPos, lastVisLine ) - cursorPos, shift);  break;
      default        : return super.controlKey( key, shift );
    }
    if ( !shift ) clear();
    return true;
  }

  protected boolean write(int key)
  {
     super.write( key );
     if ( addLineFeed && key == '\n' ) super.write( '\r' );
     recalcLines( cursorPos );
     return true;
  }

  protected String filterSymbols( String s )
  {
     return s;
  }

  protected void repaintPart ()
  {
     repaint();
  }

  protected void remove(int pos, int size)
  {
     if ( pos > 0 && buffer.charAt( pos ) == '\n' && buffer.charAt( pos - 1 ) == '\r' )
     {
        pos--;
        size++;
     }
     if ( pos + size < buffer.length() && buffer.charAt( pos + size - 1 ) == '\r' && buffer.charAt( pos + size ) == '\n' )
        size++;
     super.remove( pos, size );
     recalcLines( pos );
  }

  public void insert(int pos, String str)
  {
     super.insert( pos, str );
     recalcLines( pos );
  }

  public void update(Graphics g)
  {
     paint( g );
  }

  public void paint( Graphics g )
  {
     Dimension d = size();
     if ((d.width != viewWidth) || (d.height != viewHeight) || internImg == null )
     {
        if ( d.width * d.height <= 0 ) return;
        internImg = createImage( d.width, d.height );
        viewWidth  = d.width;
        viewHeight = d.height;
     }
     Graphics internGr = internImg.getGraphics();
     recalc();
     internGr.clearRect(0,0,d.width,d.height);
     drawBorder(internGr);
     internGr.clipRect( insets.left, insets.top, d.width - insets.left - insets.right + 1,
        d.height - insets.top - insets.bottom );
     drawCursor(internGr);
     drawText  (internGr);
     drawBlock (internGr);
     g.drawImage(internImg, 0, 0, this);
     internGr.dispose();
  }

  protected void drawBlock(Graphics g)
  {
    if ( !isSelected() ) return;
    FontMetrics fm = getFontMetrics(getFont());
    int l1 = lineFromPos( selPos );
    int l2 = lineFromPos( selPos + selWidth );
    for ( int i = l1; i <= l2; i++ )
    {
       String s = (String) lineText.elementAt(i);
       int beg = 0;
       int begPos = 0;
       if ( i == l1 )
       {
          begPos = selPos - lineStart[i];
          beg = fm.stringWidth(s.substring(0, begPos));
       }
       int end = fm.stringWidth(s);
       int endPos = s.length();
       if ( i == l2 )
       {
          endPos = selPos + selWidth - lineStart[i];
          end = fm.stringWidth(s.substring(0, endPos));
       }
       g.setColor(Color.blue);
       g.fillRect(textLocation.x + shift.x + beg,
                  insets.top + (i - upRowNum) * fm.getHeight(),
                  end - beg,
                  textSize.height);
       g.setColor(Color.white);
       g.drawString (s.substring(begPos, endPos), insets.left + beg+ shift.x,
          insets.top + baseTextIndent + (i - upRowNum) * fm.getHeight());
    }
  }

  protected void drawText(Graphics g)
  {
    Dimension d = size();
    g.setColor   (getForeground());
    for( int i = upRowNum; i < lineText.size(); i++ )
       g.drawString((String) lineText.elementAt(i), insets.left + shift.x,
          insets.top + baseTextIndent + (i - upRowNum) * textSize.height );
  }

  public Dimension preferredSize()
  {
     int h, w;
     if ( prefWidth == 0 ) w = maxTextWidth;
     else w = prefWidth;
     Font f = getFont();
     if (f == null) return new Dimension (0,0);
     FontMetrics m = getFontMetrics(f);
     if (m == null) return new Dimension (0,0);
     Insets i = insets();
     if ( rowsNumber == 0 ) h = lineText.size();
     else h = rowsNumber;
     return new Dimension( i.left + i.right + w, i.top + i.bottom + m.getHeight() * h );
  }

  public Point getSOLocation()
  {
     return new Point( - shift.x, upRowNum * textSize.height );
  }

  public void setSOLocation(int x, int y)
  {
     shift.x = - x;
     if ( textSize.height > 0 ) upRowNum = y / textSize.height;
     repaint();
  }

  public Dimension getSOSize()
  {
     return new Dimension( maxTextWidth + insets.left + insets.right,
        lineText.size() * textSize.height + insets.top + insets.bottom );
  }

  public Component getScrollComponent()
  {
     return this;
  }

  protected void setLineStart( int pos, int value )
  {
     if ( pos >= lineStart.length )
     {
        int [] nls = new int[ ( pos / LINE_INCR + 1 ) * LINE_INCR ];
        System.arraycopy( lineStart, 0, nls, 0, lineStart.length );
        lineStart = nls;
     }
     lineStart[pos] = value;
  }

  protected int indexOfBlank( String s, int i )
  {
     int i1 = s.indexOf( ' ', i );
     if ( i1 < 0 ) i1 = s.length()-1;
     int i2 = s.indexOf( '\t', i );
     if ( i2 < 0 ) i2 = s.length()-1;
     return Math.min( i1, i2 );
  }

  protected void recalcLines( int position )
  {
     int rowNum = lineFromPos( position );
     int oldLN = lineText.size();
     int oldMW = maxTextWidth;
     Dimension d = size();
     Insets i = insets();
     textWidth = d.width - i.left - i.right;
     textHeight = d.height - i.top - i.bottom;
     if ( textWidth <= 0 || textHeight <= 0 ) return;
     noFontMetric = true;
     Font f = getFont();
     if (f == null) return;
     FontMetrics m = getFontMetrics(f);
     if (m == null) return;
     noFontMetric = false;
     if ( rowNum > lineText.size() ) return;
     String allText = buffer.toString();
     int currLine = rowNum;
     for ( int j = lineText.size() - 1; j >= currLine; j-- ) lineText.removeElementAt( j );
     setLineStart( 0, 0 );
     int currPos = lineStart[currLine];
     int ind;
     do
     {
        ind = allText.indexOf( '\n', currPos );
        int startNext = ind + 1;
        if ( ind < 0 ) ind = allText.length();
        if ( ind > 0 && allText.charAt(ind-1) == '\r' ) ind--;
        String sl = allText.substring( currPos, ind );
        if ( wordWrap && m.stringWidth( sl ) > textWidth )
        {
           int blankInd = indexOfBlank( allText, currPos );
           if( blankInd < ind )
           {
              ind = blankInd + 1;
              sl = allText.substring( currPos, ind );
              String tempSl = sl;
              while( m.stringWidth( tempSl ) < textWidth )
              {
                 sl = tempSl;
                 ind = blankInd + 1;
                 blankInd = indexOfBlank( allText, ind );
                 tempSl = allText.substring( currPos, blankInd + 1 );
              }
              startNext = ind;
           }
        }
        lineText.addElement( sl );
        setLineStart( currLine, currPos );
        currPos = startNext;
        currLine++;
     }
     while( ind < allText.length() );
     maxTextWidth = 0;
     for ( int j = 0; j < lineText.size(); j++ )
     {
        int len = m.stringWidth( (String) lineText.elementAt( j ) );
        if ( maxTextWidth < len ) maxTextWidth  = len;
     }
/*     if ( getParent() != null && ( oldLN != lineText.size() || maxTextWidth != oldMW ) )
     {
        Event e = new Event(this, ScrollPanel.RECALC_LAYOUT, this);
        getParent().postEvent(e);
     }
*/  }

  protected void setPos(int p)
  {
     super.setPos( adjustPos( p, true ) );
  }

  public void select(int pos, int w)
  {
     int ap = adjustPos( pos, true );
     int aw = adjustPos( pos + w, true ) - ap;
     super.select( ap, aw );
  }

  protected boolean seek(int shift, boolean b)
  {
     return super.seek( adjustPos( cursorPos + shift, shift > 0 ) - cursorPos, b );
  }

  protected int adjustPos( int pos, boolean incr )
  {
     int l = lineFromPos( pos );
     int sl = ((String) lineText.elementAt( l )).length();
     if ( l < lineText.size() - 1 && pos - lineStart[l] > sl )
        if ( incr ) return lineStart[l+1];
        else return lineStart[l] + sl;
     return pos;
  }

  protected boolean recalc()
  {
     int wasShiftX = shift.x;
     if ( noFontMetric ) recalcLines( 0 );
     boolean res = super.recalc();
     shift.x = wasShiftX;
     int l = lineFromPos( cursorPos );
     String s = ((String) lineText.elementAt( l ));
     s = s.substring( 0, cursorPos - lineStart[l] );
     FontMetrics m   = getFontMetrics(getFont());
     shift.y = 0;
     baseTextIndent = m.getHeight() - m.getDescent();
     int cursLine = lineFromPos( cursorPos );
     if ( cursLine < upRowNum ) upRowNum = cursLine;
     lastVisLine = textHeight / m.getHeight() - 1;
     if ( lastVisLine < 0 ) lastVisLine = 0;
     if ( cursLine > lastVisLine + upRowNum ) upRowNum = cursLine - lastVisLine;
     cursorLocation.x = insets.left + m.stringWidth( s );
     cursorLocation.y = insets.top + ( l - upRowNum ) * textSize.height;
     if ( ( cursorLocation.x + shift.x ) < insets.left )
        shift.x = insets.left - cursorLocation.x;
     else
     {
        int w = size().width - insets.right;
        if ((cursorLocation.x + shift.x) > w) shift.x = w - cursorLocation.x;
     }
/*     if ( getParent() != null )
     {
        Event e = new Event(this, Event.SCROLL_ABSOLUTE, this);
        getParent().postEvent(e);
     }
*/     return res;
  }

  protected int getLinePos( int ln, FontMetrics fm, int pix )
  {
     if ( ln < 0 ) ln = 0;
     if ( ln >= lineText.size() ) ln = lineText.size() - 1;
     String s = (String) lineText.elementAt(ln);
     for ( int i = 0; i < s.length(); i++ )
       if ( fm.stringWidth( s.substring( 0, i ) ) > pix )
        return lineStart[ln] + i - 1;
     int res = lineStart[ln] + s.length();
     if ( pix > 0 && ln < lineText.size() - 1 &&
        buffer.charAt( lineStart[ln+1] - 1 ) != '\n' ) res--;
     return res;
  }

  protected int calcTextPos(int x, int y)
  {
     FontMetrics fm = getFontMetrics(getFont());
     return getLinePos( ( y - insets.top ) / fm.getHeight() + upRowNum, fm, x - insets.left - shift.x );
  }

  protected int vertPosShift( int currPos, int vertShift )
  {
     int currLine = lineFromPos( currPos );
     FontMetrics fm = getFontMetrics(getFont());
     int pixW = fm.stringWidth( ((String) lineText.elementAt( currLine ) ).
        substring( 0, currPos - lineStart[currLine] ) );
     return getLinePos( currLine + vertShift, fm, pixW );
  }

  protected int lineFromPos( int pos )
  {
     for( int i = lineText.size() - 1; i >= 0; i-- )
        if ( lineStart[i] <= pos ) return i;
     return 0;
  }

  public void resize(int w, int h)
  {
     super.resize(w, h);
     recalcLines( 0 );
  }

  public void reshape(int x, int y, int w, int h)
  {
     Dimension d = size();
     super.reshape(x, y, w, h);
     if ( d.width != w ) recalcLines( 0 );
  }

  public void setEditable(boolean b) {
    // TODO Not yet implemented.    
  }

}
