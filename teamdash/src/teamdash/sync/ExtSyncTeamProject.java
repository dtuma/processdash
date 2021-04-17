// Copyright (C) 2017-2021 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package teamdash.sync;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.w3c.dom.Element;

import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;

import teamdash.wbs.TeamProject;


public class ExtSyncTeamProject extends TeamProject {

    private ResourceCollection collection;

    public ExtSyncTeamProject(ResourceCollection collection) {
        super(NONEXISTENT_IGNORED_DIR, "");
        this.collection = collection;
        reload();
    }


    @Override
    public void reload() {
        // our TeamProject superclass calls reload() as part of its constructor,
        // before we have an opportunity to initialize the collection field.
        // Detect that scenario to avoid NullPointerException.
        if (collection != null)
            super.reload();
    }


    /**
     * Read data from our collection instead of the filesystem.
     */
    @Override
    protected InputStream openInputStream(File file) throws IOException {
        InputStream in = collection.getInputStream(file.getName());
        if (in == null)
            throw new FileNotFoundException(file.getName());
        return in;
    }


    /**
     * Write data to our collection instead of the filesystem.
     */
    @Override
    protected OutputStream openOutputStream(File file) throws IOException {
        OutputStream out = collection.getOutputStream(file.getName(), 0);
        if (out == null)
            throw new FileNotFoundException(file.getName());
        return out;
    }


    /**
     * In a master project/subproject environment, the TeamProject object
     * automatically creates ImportDirectory objects for all of the interrelated
     * projects. That is unnecessary for our purposes, so we override this
     * method to skip the ImportDirectory creation step.
     */
    @Override
    protected ImportDirectory getProjectDataDirectory(Element e,
            boolean checkExists) {
        return null;
    }


    private static final File NONEXISTENT_IGNORED_DIR = new File(
            "ExtSyncTeamProject nonexistent directory");

}
