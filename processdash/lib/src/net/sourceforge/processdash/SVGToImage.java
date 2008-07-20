// This file is based on the class com.kitfox.svg.app.ant.SVGToImageAntTask,
// developed by Mark McKay as part of the SVG Salamander project, and
// distributed under the LGPL.
//
// Changes copyright (C) 2006 Tuma Solutions, LLC
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lessser General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash;



import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.app.beans.SVGIcon;

/**
 * Translates a group of SVG files into images.
 *
 * Parameters:
 * destDir - If present, specifices a directory to write SVG files to.  Otherwise
 * writes images to directory SVG file was found in
 * verbose - If true, prints processing information to the console
 * format - File format for output images.  The java core javax.imageio.ImageIO
 * class is used for creating images, so format strings will depend on what
 * files your system is configured to handle.  By default, "gif", "jpg" and "png"
 * files are guaranteed to be present.  If omitted, "png" is used by default.
 *
 * Example:
 * &lt;SVGToImage destDir="${index.java}" format="jpg" verbose="true"&gt;
 *    &lt;fileset dir="${dir1}"&gt;
 *        &lt;include name="*.svg"/&gt;
 *    &lt;/fileset&gt;
 *    &lt;fileset dir="${dir2}"&gt;
 *        &lt;include name="*.svg"/&gt;
 *    &lt;/fileset&gt;
 * &lt;/SVGToImage&gt;
 *
 *
 * @author kitfox, tuma
 */
public class SVGToImage extends Task {

    private Vector filesets = new Vector();
    boolean verbose = false;
    File destDir;
    private String format = "png";
    Color bgColor = null;
    int maxWidth = -1;
    int maxHeight = -1;
    boolean antiAlias = true;
    boolean clipToViewBox = false;

    /** Creates a new instance of IndexLoadObjectsAntTask */
    public SVGToImage() {
    }


    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setBg(String bgColor) {
        try {
            this.bgColor = decodeColor(bgColor);
        } catch (Exception e) {
            throw new BuildException("Unrecognized color specification '"
                    + bgColor + "'");
        }
    }

    private Color decodeColor(String s) {
        if (s == null || s.length() == 0)
            return null;

        if (s.indexOf(',') != -1) {
            String[] comp = s.split("\\D+");
            int[] num = new int[comp.length];
            for (int i = 0; i < num.length; i++)
                num[i] = Integer.parseInt(comp[i]);
            if (num.length == 4)
                return new Color(num[1], num[2], num[3], num[0]);
            else if (num.length == 3)
                return new Color(num[0], num[1], num[2]);
            else
                throw new IllegalArgumentException();
        }

        return Color.decode(s);
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public void setAntiAlias(boolean antiAlias) {
        this.antiAlias = antiAlias;
    }

    public void setClipToViewBox(boolean clipToViewBox) {
        this.clipToViewBox = clipToViewBox;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Adds a set of files.
     */
    public void addFileset(FileSet set) {
        filesets.add(set);
    }



    public void execute() {
        if (verbose)
            log("Building SVG images");

        for (Iterator it = filesets.iterator(); it.hasNext();) {
            FileSet fs = (FileSet) it.next();
            FileScanner scanner = fs.getDirectoryScanner(getProject());
            String[] files = scanner.getIncludedFiles();

            try {
                File basedir = scanner.getBasedir();

                if (verbose)
                    log("Scanning " + basedir);

                for (int i = 0; i < files.length; i++) {
                    translate(basedir, files[i]);
                }
            } catch (Exception e) {
                throw new BuildException(e);
            }
        }
    }

    private void translate(File baseDir, String shortName) {
        File source = new File(baseDir, shortName);

        if (verbose)
            log("Reading file: " + source);

        Matcher matchName = Pattern.compile("(.*)\\.svg",
                Pattern.CASE_INSENSITIVE).matcher(shortName);
        if (matchName.matches()) {
            shortName = matchName.group(1);
        }
        shortName += "." + format;

        SVGIcon icon = new SVGIcon();
        icon.setSvgURI(source.toURI());
        icon.setAntiAlias(antiAlias);

        maybeResizeIcon(icon);

        int width = icon.getIconWidth();
        int height = icon.getIconHeight();
        icon.setClipToViewbox(clipToViewBox);
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g = new ImageResizingGraphics(g);

        if (bgColor != null) {
            g.setColor(bgColor);
            g.fillRect(0, 0, width, height);
        }

        g.setClip(0, 0, width, height);
        icon.paintIcon(null, g, 0, 0);
        g.dispose();

        File outFile = destDir == null ? new File(baseDir, shortName)
                : new File(destDir, shortName);
        if (verbose)
            log("Writing file: " + outFile);

        try {
            ImageIO.write(image, format, outFile);
        } catch (IOException e) {
            log("Error writing image: " + e.getMessage());
            throw new BuildException(e);
        }

        SVGCache.getSVGUniverse().clear();
    }


    private void maybeResizeIcon(SVGIcon icon) {
        if (maxWidth == -1 && maxHeight == -1)
            return;

        int iconWidth = icon.getIconWidth();
        int iconHeight = icon.getIconHeight();

        double widthScale = 1.0;
        if (maxWidth != -1 && iconWidth > maxWidth)
            widthScale = ((double) maxWidth) / ((double) iconWidth);

        double heightScale = 1.0;
        if (maxHeight != -1 && iconHeight > maxHeight)
            heightScale = ((double) maxHeight) / ((double) iconHeight);

        double scale = Math.min(widthScale, heightScale);
        if (scale == 1.0)
            return;

        int newWidth = (int) Math.round(iconWidth * scale);
        int newHeight = (int) Math.round(iconHeight * scale);
        icon.setPreferredSize(new Dimension(newWidth, newHeight));
        icon.setScaleToFit(true);
    }
}
