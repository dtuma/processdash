/*
FileProperties.java : A stand alone utility class.
Copyright (C) 2000 Justin P. McCarthy

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

To further contact the author please email jpmccar@gjt.org

*/

/*
    changes:
        23 Sept, 2000 -- created
*/

//package com.justinsbrain.gnu.util;
package net.sourceforge.processdash.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;


/**
    I got tired of trying to make my .properties and .ini files look nice
    so now I delegated that to this object.  It will order the output
    alphabetically, allows comments to be added to each property
    line, and allows for friendly white space in the output.
    It also provides several new i/o routines for writers, readers, or
    just passing it a filename.
    FileProperties does not make any attempt at recreating the comments
    for each property from the file when loading because of changes that
    may have occured in the file.
    <BR>
    Previously my properties files would look something like this:
    <BR>
    <PRE>
    # This is my web-based mail server properties file.  It containts all initialization parameters.
    # Sat Sept 23, 2000
    database.password=password
    database.name=dbname
    database.user=user
    threads.max_number=20
    database.port=9898

    <BR>
    <BR>

    Now I can get them to look like this:
    <BR>
    # This is my web-based mail server properties
    # file.  It contains all the initialization
    # parameters.
    # Generated on: Sat Sept 23, 2000

    # The name of the database instance we are
    # connection to.
    database.name=dbname

    # The password used when connection to the
    # databse.
    database.password=dbpassword

    # The port number to connect to the database
    # on.
    database.port=9999

    # The name of user to use to connect to the
    # database
    database.user=dbuser

    # The maximum number of threads ever running
    # for this app.  If set to low this will
    # prevent concurrent sessions.  If set to high
    # it might bog down the system with unecessary
    # threads
    threads.max_number=20
    <BR>
    </PRE>


    @author Justin P. McCarthy
    @version 1.0
*/
public class FileProperties
extends Properties
{
    /**
        The comment character used (usually the '#').
    */
    private static final char COMMENT_CHAR = '#';

    /**
        Just the space character, made it a constant because I use it so much.
    */
    private static final char SPACE_CHAR = ' ';

    /**
        Just the tab character, made it a constant because I use it so much.
    */
    private static final char TAB_CHAR = '\t';

    /**
        Just the equals sign, made it a constant because I use it a lot.
    */
    private static final char SEPARATION_CHAR = '=';

    /**
        The header to use in output.
    */
    private String header;

    /**
        The number of blank lines to place between entries.
    */
    private int numBlankLines = 2;

    /**
        The filename, should a default one be set.
    */
    private String filename;

    /**
        Whether FileProperties should time/date stamp each write out.
    */
    private boolean doDateStamp = true;

    /**
        Whether FileProperties should attempt to line wrap comments.
    */
    private boolean lineWrap = true;

    /**
        If this is set it will write to disk all the property keys loaded
        not just the defaults.
    */
    private boolean keepStrangeKeys = false;

    /**
        If FileProperties is line wrapping, at what length to line wrap.
    */
    private int lineWrapLength = 76;

    /**
        The properties object storing the associated comments.
    */
    private Properties comments;

    /**

        @param defaults The defaults to use.
        @param comments The comments for the keys.
    */
    public FileProperties(
        Properties defaults,
        Properties comments)
    {
        super(defaults);
        this.comments = comments;
    }

    /**
        This sets the header.  The header is the first line of the output
        and a comment.  If the header is long you might wish to set word wrap
        on and set the line length to 70 or 80 columns.

        @param newHeader The string that wil be the new header.
        @throws NullPointerException If the string you hand it is null.
    */
    public void setHeader(String newHeader)
    {
        if (newHeader == null)
        {
            throw new NullPointerException("Header cannot be set to null");
        }
        header = newHeader;
    }

    /**
        Gets the current output header if one is set,
        otherwise it will return null.
        @return The current output header, or null if there isn't one.
    */
    public String getHeader()
    {
        return header;
    }

    /**
        This is used to associate a comment with a property.  The comment
        will be primted just prior to the actual property in the final
        file.  if the property it is associated with does not exist in this
        FileProperties objec then the comment will not be printed in the
        final output.

        @param property The property to associate this comment with.
        @param comment The comment for the property.
        @throws NullPointerException if either property or comment is null.
    */
    public void setComment(String property, String comment)
    {
        comments.setProperty(property, comment);
    }

    /**
        Gets the comment for a specific property or null if a comment
        has not been set or if the property does not exist.

        @return the comment for the given property providing both the
            property exists and has a comment,  null otherwise.
        @throws NullPointerException if the property is null.
    */
    public String getComment(String property)
    {
        return comments.getProperty(property);
    }

    /**
        Sets the current output filename.  After the filename has been set
        you can call writeToFile() (the no-args version) and have it
        automatically updated with the current properties.

        @param newFilename The string to use for a filename.
        @throws NullPointerException if the filename is null.
    */
    public void setFilename(String newFilename)
    {
        filename = newFilename;
    }

    /**
        Accessor for the filename used for calls to writeToFile() or
        loadFromFile().

        @return The filename used for calls to updateFile()
            or null if one hasn't been set.
    */
    public String getFilename()
    {
        return filename;
    }

    /**
        If set to true then the first non-header line of the file will
        be written like "Generated on: Sat Sept 23, 200";
        @param doDateStamp Whether the output file should be tim/date stamped.
    */
    public void setDateStamping(boolean doDateStamp)
    {
        this.doDateStamp = doDateStamp;
    }

    /**
        You can check to to see if the date stamp will be added in
        the final output file.
        @return Wether or not the date stamp will be added on output.
    */
    public boolean isDateStamping()
    {
        return doDateStamp;
    }

    /**
        Sets line wrapping.  If line wrapping is set it will initially
        wrap to 76 characters a line.  This can be changed with
        calls to setLineWrapLength.
        @param doLineWrap Wether or not to line wrap.
    */
    public void setLineWrapping(boolean doLineWrap)
    {
        lineWrap = doLineWrap;
    }

    /**
        Accessor to tell if we are currently line wrapping.
        @return wether we are currenly line wrapping.
    */
    public boolean isLineWrapping()
    {
        return lineWrap;
    }

    /**
        This will attempt to wrap lines to this length or shorter.
        It breaks on the first character that is not a letter or digit.
        @param length the length to wrap a line at.
    */
    public void setLineWrapLength(int length)
    {
        lineWrapLength = length;
    }

    /**
        The default line wrapping occurs at 76 characters.  However if it is
        changed it can be accessed via this method.
        @return the current line wrapping point
    */
    public int getLineWrapLength()
    {
        return lineWrapLength;
    }

    /**
        This mutator sets behavior in instances where keys-value pairs
        are found in the file that are not found in the default properties
        object given during construction.  If set to true then they are
        kept and written back out (the data is not pruned) , if set to false
        then they are not written back out.  However if set to false they are
        loaded and can be accessed like any other property.
        <BR>
        I use this for debugging.  I set this false and might use a debug=true
        line in my properties file if something is going wrong.  With this
        set to false that debug line will not be written back out when the
        properties are stored.

        @param keep Whether or not to keep strange keys.
    */
    public void setKeepingStrangeKeys(boolean keep)
    {
        keepStrangeKeys = keep;
    }

    /**
        Whether or not FileProperties is currently keeping strange keys.
        @return Whether or not FileProperties is currently keeping strange keys.
    */
    public boolean isKeepingStrangeKeys()
    {
        return keepStrangeKeys;
    }

    /**
        This is a conveince method.  Usefull if someone has set their filename
        and header and comments and just wants to update the underlying properties
        file.
        @throws IOException If there was a problem updating the file.
        @throws NullPointerException if the filename was never set.
    */
    public void writeToFile()
    throws IOException
    {
        if (filename == null)
        {
            throw new NullPointerException("cannot read from a null!");
        }
        writeToFile(filename);
    }

    /**
        Used to write this properties set to a file.
        @param filename The file to write to.
        @throws IOException if there was a problem writing to the underlying
            file.
    */
    public void writeToFile(String filename)
    throws IOException
    {
        FileWriter out = new FileWriter(filename);
        store(out, header);
        out.close();
    }

    /**
        This is a conveince method.  Usefull if someone has set their filename
        and just needs to load the underlying properties file or refresh from
        it.
        @throws IOException If there was a problem updating the file.
        @throws NullPointerException if the filename was never set.
    */
    public void loadFromFile()
    throws IOException
    {
        if (filename == null)
        {
            throw new NullPointerException("cannot read from a null!");
        }

        loadFromFile(filename);
    }

    /**
        Load this set of properties from the filename given.
        @throws IOException if there was an error loading from the file.
    */
    public void loadFromFile(String filename)
    throws IOException
    {
        FileReader freader = new FileReader(filename);
        load(freader);
        freader.close();
    }

    /**
        Loads this sets of properties from the Reader given.
        @param in The reader to load from.
        @throws IOException if there wasn an error loading from this reader.
        @throws NullPointerException if the reader is null.
    */
    public void load(Reader in)
    throws IOException
    {
        BufferedReader br =
            new BufferedReader(in);

        String readLine;
        int length = 0;
        int index = 0;

        while (br.ready())
        {
            readLine = br.readLine();

            if (readLine == null)
            {
                continue;
            }

            if (readLine.trim().startsWith(COMMENT_CHAR + ""))
            {
                continue;
            }

            length = readLine.length();

            if (length < 3)
            {
                continue;
            }

            index = readLine.indexOf(SEPARATION_CHAR);

            // if true it wasn't a properties line anyway!!!
            // assuming a one character key at least!
            if (index < 1)
            {
                continue;
            }

            setProperty(
                readLine.substring(
                    0,
                    index).trim(),
                readLine.substring(
                    index + 1));
        }
    }

    /**
        Loads this sets of properties from the InputStream given.
        @param in The InputStream to load from.
        @throws IOException if there wasn an error loading from this reader.
        @throws NullPointerException if the InputStream is null.
    */
    public void load(InputStream in)
    throws IOException
    {
        load(new InputStreamReader(in));
    }

    /*
        This method only exists to maintain the integrity of the files
        that FileProperties generates.  It overrides a deprecated api
        in Properties.  This will not return an io exception and
        you have no way of knowing if an error occured on writing this
        properties object to the output stream.

        @param out The outputstream to write this properties object to.
        @param header The header to give the properties file.
        @throws NullPointerException if either the output stream is null.
    public void save(OutputStream out, String header)
    {
        try
        {
            store(out, header);
        }
        catch (IOException ioe)
        {
            / they don't want the exception, so we'll ignore it to.
        }
    }
    */

    /**
        Stores these properties to the output stream given
        and uses the header passed as the first line.  It will only
        break the header if line wrapping is set on.

        @param out The outputstream to write these properties to.
        @param header The header to put in the properties file.
        @throws NulPointerException If out was null.
        @throws IOException If there was a problem writing to this
            output stream.
    */
    public void store(OutputStream out, String header)
    throws IOException
    {
        if (out == null)
        {
            throw new NullPointerException(
                "Cannot write out with a null output stream");
        }

        store(new OutputStreamWriter(out), header);
    }

    /**
        Writing these properties to this output stream.
        @param out The outputstream to write these properties to.
        @throws IOException if there is a problem writing.
        @throws NullPointerException if out is null.
    */
    public void store(OutputStream out)
    throws IOException
    {
        store(out, header);
    }

    /**
        Write these properties to the writer given and uses the header
        passed to it.  It will only break the header of line wrapping
        is set on.

        @param writer The writer to use.
        @param header The header to put in the file.
        @throws NullPointerException if writer is null
        @throws IOException if there was a problem writing to this
            writer.
    */
    public void store(Writer writer, String header)
    throws IOException
    {
        if (writer == null)
        {
            throw new NullPointerException(
                "Cannot write out with a null writer");
        }

        PrintWriter out = new PrintWriter(writer);
        TreeSet sorted;
        Iterator looper;
        int i = 0;
        String item;
        String[] brokenComment;

        if ( ! (header == null) )
        {
            if (lineWrap)
            {
                brokenComment =
                    StringUtils.breakDownString(
                        header,
                        lineWrapLength,
                        true);
                for (i = 0; i < brokenComment.length; i++)
                {
                    out.print(COMMENT_CHAR);
                    out.print(SPACE_CHAR);
                    out.println(brokenComment[i]);
                }
            }
            else
            {
                out.print(COMMENT_CHAR);
                out.print(SPACE_CHAR);
                out.println(header);
            }
        }

        if (doDateStamp)
        {
            out.print(COMMENT_CHAR);
            out.print(SPACE_CHAR);
            out.println("Generated on: " + new Date());
        }

        sorted = new TreeSet(keySet());

//      sorted = new TreeSet(defaults.keySet());
//
//      if (keepStrangeKeys)
//      {
//        sorted.addAll(keySet());
//      }

        looper = sorted.iterator();

        // so can't go adding properties mid write!!!!
        synchronized (this)
        {
            while (looper.hasNext())
            {
                // print the required number of blank lines.
                for (i = 0; i < numBlankLines; i++)
                {
                    out.println();
                }

                item = (String)looper.next();
                if (comments.containsKey(item))
                {
                    if (lineWrap)
                    {
                        brokenComment =
                            StringUtils.breakDownString(
                                comments.getProperty(item),
                                lineWrapLength,
                                true);
                        for (i = 0; i < brokenComment.length; i++)
                        {
                            out.print(COMMENT_CHAR);
                            out.print(SPACE_CHAR);
                            out.println(brokenComment[i]);
                        }
                    }
                    else
                    {
                        out.print(COMMENT_CHAR);
                        out.print(SPACE_CHAR);
                        out.println(comments.getProperty(item));
                    }
                }
                out.print(item);
                out.print(SEPARATION_CHAR);
                out.println((getProperty(item) == null) ? "" : getProperty(item));
            }
        }
        out.flush();
    }

    /**
        Writes this FileProperties objec to the given write with the
        current header set by setHeader(String header)

        @param writer The writer to use.
        @throws NullPointerException if writer is null.
        @throws IOException if there is a problem writing to the properties
            file.
    */
    public void store(Writer writer)
    throws IOException
    {
        store(writer, header);
    }
}
