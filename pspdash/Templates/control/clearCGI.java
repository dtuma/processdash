// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


import pspdash.*;
import java.io.IOException;
import java.util.Date;

public class clearCGI extends TinyCGIBase {

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        getTinyWebServer().clearClassLoaderCaches();

        out.println("<HTML><HEAD><TITLE>Classes cleared</TITLE></HEAD>");
        out.println("<BODY><H1>Classes cleared</H1>");
        out.println("The classloader cache was cleared at " + new Date());
        out.println("</BODY></HTML>");
    }

}
