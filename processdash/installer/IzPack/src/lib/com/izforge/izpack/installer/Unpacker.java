/*
 *  $Id$
 *  IzPack
 *  Copyright (C) 2001-2003 Julien Ponge
 *
 *  File :               Unpacker.java
 *  Description :        The unpacker class.
 *  Author's email :     julien@izforge.com
 *  Author's Website :   http://www.izforge.com
 *
 *  Portions are Copyright (c) 2001 Johannes Lehtinen
 *  johannes.lehtinen@iki.fi
 *  http://www.iki.fi/jle/
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.izforge.izpack.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;

import com.izforge.izpack.ExecutableFile;
import com.izforge.izpack.Pack;
import com.izforge.izpack.PackFile;
import com.izforge.izpack.ParsableFile;
import com.izforge.izpack.util.AbstractUIHandler;
import com.izforge.izpack.util.AbstractUIProgressHandler;
import com.izforge.izpack.util.FileExecutor;
import com.izforge.izpack.util.OsConstraint;

/**
 *  Unpacker class.
 *
 * @author     Julien Ponge
 * @author     Johannes Lehtinen
 */
public class Unpacker extends Thread
{
    /**  The installdata. */
    private AutomatedInstallData idata;

    /**  The installer listener. */
    private AbstractUIProgressHandler handler;

    /**  The uninstallation data. */
    private UninstallData udata;

    /**  The variables substitutor. */
    private VariableSubstitutor vs;

    /**  The instances of the unpacker objects. */
    private static ArrayList instances = new ArrayList();


    /**
     *  The constructor.
     *
     * @param  idata     The installation data.
     * @param  handler   The installation progress handler.
     */
    public Unpacker(AutomatedInstallData idata, AbstractUIProgressHandler handler)
    {
        super("IzPack - Unpacker thread");

        this.idata = idata;
        this.handler = handler;

        // Initialize the variable substitutor
        vs = new VariableSubstitutor(idata.getVariableValueMap());
    }


    /**
     *  Returns the active unpacker instances.
     *
     * @return    The active unpacker instances.
     */
    public static ArrayList getRunningInstances()
    {
        return instances;
    }

    /**  The run method.  */
    public void run()
    {
        instances.add(this);
        try
        {
            //
            // Initialisations
            FileOutputStream out = null;
            ArrayList parsables = new ArrayList();
            ArrayList executables = new ArrayList();
            List packs = idata.selectedPacks;
            int npacks = packs.size();
            handler.startAction ("Unpacking", npacks);
            udata = UninstallData.getInstance();

            // We unpack the selected packs
            for (int i = 0; i < npacks; i++)
            {
                // We get the pack stream
                int n = idata.allPacks.indexOf(packs.get(i));
                InputStream rawIn = getPackAsStream(n);
                if (rawIn == null) {
                    if (((Pack) packs.get(i)).required)
                        System.exit(1);
                    else
                        continue;
                }
                ObjectInputStream objIn = new ObjectInputStream(rawIn);

                // We unpack the files
                int nfiles = objIn.readInt();
                handler.nextStep (((Pack) packs.get(i)).getDisplayName(), i+1, nfiles);
                for (int j = 0; j < nfiles; j++)
                {
                    // We read the header
                    PackFile pf = (PackFile) objIn.readObject();

                    if (OsConstraint.oneMatchesCurrentSystem(pf.osConstraints))
                    {
                        // We translate & build the path
                        String path = translatePath(pf.targetPath);

                        if (path.startsWith("http")) {
                            objIn.skip(pf.length);
                            continue;
                        }

                        File pathFile = new File(path);
                        File dest = pathFile.getParentFile();

                        if (!dest.exists())
                        {
                            if (! dest.mkdirs())
                            {
                                handler.emitError("Error creating directories", "Could not create directory\n"+dest.getPath());
                                handler.stopAction ();
                                return;
                            }
                        }

                        // We add the path to the log,
                        if (fileShouldBeLogged(path)) {
                            udata.addFile(path);
                        }

                        handler.progress (j, path);

                        //if this file exists and should not be overwritten, check
                        //what to do
                        if (pathFile.exists ())
                        {
                            boolean overwritefile = false;

                            // don't overwrite file if the user said so
                            if (pf.override != PackFile.OVERRIDE_FALSE)
                            {
                                if (pf.override == PackFile.OVERRIDE_TRUE)
                                {
                                    overwritefile = true;
                                }
                                else if (pf.override == PackFile.OVERRIDE_UPDATE)
                                {
                                    // check mtime of involved files
                                    // (this is not 100% perfect, because the already existing file might
                                    // still be modified but the new installed is just a bit newer; we would
                                    // need the creation time of the existing file or record with which mtime
                                    // it was installed...)
                                    overwritefile = (pathFile.lastModified() < pf.mtime);
                                }
                                else
                                {
                                    int def_choice = -1;

                                    if (pf.override == PackFile.OVERRIDE_ASK_FALSE)
                                        def_choice = AbstractUIHandler.ANSWER_NO;
                                    if (pf.override == PackFile.OVERRIDE_ASK_TRUE)
                                        def_choice = AbstractUIHandler.ANSWER_YES;

                                    int answer = handler.askQuestion (
                                        idata.langpack.getString ("InstallPanel.overwrite.title") + pathFile.getName (),
                                        idata.langpack.getString ("InstallPanel.overwrite.question") + pathFile.getAbsolutePath(),
                                        AbstractUIHandler.CHOICES_YES_NO, def_choice);

                                    overwritefile = (answer == AbstractUIHandler.ANSWER_YES);
                                }

                            }

                            if (! overwritefile)
                            {
                                objIn.skip(pf.length);
                                continue;
                            }
                            else
                            {
                                pathFile.delete();
                            }

                        }

                        // We copy the file
                        out = new FileOutputStream(pathFile);
                        byte[] buffer = new byte[5120];
                        long bytesCopied = 0;
                        while (bytesCopied < pf.length)
                        {
                            int maxBytes =
                                (pf.length - bytesCopied < buffer.length ?
                                (int) (pf.length - bytesCopied) : buffer.length);
                            int bytesInBuffer = objIn.read(buffer, 0, maxBytes);
                            if (bytesInBuffer == -1)
                                throw new IOException("Unexpected end of stream");

                            out.write(buffer, 0, bytesInBuffer);

                            bytesCopied += bytesInBuffer;
                        }
                        // Cleanings
                        out.close();

                        // Set file modification time if specified
                        if (pf.mtime >= 0)
                            pathFile.setLastModified (pf.mtime);

                        // Empty dirs restoring
                        String _n = pathFile.getName();
                        if (_n.startsWith("izpack-keepme") && _n.endsWith(".tmp"))
                            pathFile.delete();

                    }
                    else
                        objIn.skip(pf.length);

                }

                // Load information about parsable files
                int numParsables = objIn.readInt();
                int k;
                for (k = 0; k < numParsables; k++)
                {
                    ParsableFile pf = (ParsableFile) objIn.readObject();
                    pf.path = translatePath(pf.path);
                    parsables.add(pf);
                }

                // Load information about executable files
                int numExecutables = objIn.readInt();
                for (k = 0; k < numExecutables; k++)
                {
                    ExecutableFile ef = (ExecutableFile) objIn.readObject();
                    ef.path = translatePath(ef.path);
                    if (null != ef.argList && !ef.argList.isEmpty())
                    {
                        String arg = null;
                        for (int j = 0; j < ef.argList.size(); j++)
                        {
                            arg = (String) ef.argList.get(j);
                            arg = translatePath(arg);
                            ef.argList.set(j, arg);
                        }
                    }
                    executables.add(ef);
                    if(ef.executionStage == ExecutableFile.UNINSTALL)
                    {
                        udata.addExecutable(ef);
                    }
                }
                objIn.close();
            }

            // We use the scripts parser
            ScriptParser parser = new ScriptParser(parsables, vs);
            parser.parseFiles();

            // We use the file executor
            FileExecutor executor = new FileExecutor(executables);
            if (executor.executeFiles(ExecutableFile.POSTINSTALL, handler) != 0)
                handler.emitError ("File execution failed", "The installation was not completed");

            // We put the uninstaller
            if (idata.info.getWriteUninstaller())
                putUninstaller();

            // Save user values for future reinstalls
            saveUserValues();

            // The end :-)
            handler.stopAction();
        }
        catch (FileNotFoundException fnfe)
        {
            handler.stopAction();
            handler.emitError("Unable to Write File",
                "The installation process was unable to write to the file:\n"
                + "     " + fnfe.getMessage() + "\n"
                + "As a result, the installation was not successful. Please\n"
                + "check the filesystem permissions and run the installer again.");
            fnfe.printStackTrace();
        }
        catch (Exception err)
        {
            // TODO: finer grained error handling with useful error messages
            handler.stopAction();
            handler.emitError ("An error occured", err.toString());
            err.printStackTrace ();
        }
        finally
        {
            instances.remove(instances.indexOf(this));
        }
    }


    /**
     * Check to see if the specified file should be logged for the uninstaller or not.
     * 
     * @param filePath The complete file path
     */
    private boolean fileShouldBeLogged(String filePath) {
        int lastSlash = filePath.lastIndexOf(File.separatorChar);

        String fileFolder = filePath.substring(0, lastSlash);
        String fileName = filePath.substring(lastSlash+1);

        // TODO : "run-dash.sh" could be a constant somewhere...

        // We log the file if it is neither in the DATA_PATH or in the TEAM_DATA_PATH
        //  or if it IS "run-dash.sh"
        return (!fileFolder.equals(idata.getVariable(ScriptParser.DATA_PATH))
                && !fileFolder.equals(idata.getVariable(ScriptParser.TEAM_DATA_PATH)))
                || fileName.equals("run-dash.sh");
    }


/**
    *  Puts the uninstaller.
    *
    * @exception  Exception  Description of the Exception
    */
    private void putUninstaller() throws Exception
    {
        // Me make the .uninstaller directory
        String dest = translatePath("$INSTALL_PATH") + File.separator +
            "Uninstaller";
        String jar = dest + File.separator + "uninstaller.jar";
        File pathMaker = new File(dest);
        pathMaker.mkdirs();

        // We log the uninstaller deletion information
        UninstallData udata = UninstallData.getInstance();
        udata.setUninstallerJarFilename(jar);
        udata.setUninstallerPath(dest);

        // We open our final jar file
        FileOutputStream out = new FileOutputStream(jar);
        ZipOutputStream outJar = new ZipOutputStream(out);
        idata.uninstallOutJar = outJar;
        outJar.setLevel(9);
        udata.addFile(jar);

        // We copy the uninstaller
        InputStream in = getClass().getResourceAsStream("/res/IzPack.uninstaller");
        ZipInputStream inRes = new ZipInputStream(in);
        ZipEntry zentry = inRes.getNextEntry();
        while (zentry != null)
        {
            // Puts a new entry
            outJar.putNextEntry(new ZipEntry(zentry.getName()));

            // Byte to byte copy
            int unc = inRes.read();
            while (unc != -1)
            {
                outJar.write(unc);
                unc = inRes.read();
            }

            // Next one please
            inRes.closeEntry();
            outJar.closeEntry();
            zentry = inRes.getNextEntry();
        }
        inRes.close();

        // We put the langpack
        in = getClass().getResourceAsStream("/langpacks/" + idata.localeISO3 + ".xml");
        outJar.putNextEntry(new ZipEntry("langpack.xml"));
        int read = in.read();
        while (read != -1)
        {
            outJar.write(read);
            read = in.read();
        }
        outJar.closeEntry();
    }


    /**
     *  Returns a stream to a pack, depending on the installation kind.
     *
     * @param  n              The pack number.
     * @return                The stream.
     * @exception  Exception  Description of the Exception
     */
    private InputStream getPackAsStream(int n) throws Exception
    {
        InputStream in = null;

        if (idata.kind.equalsIgnoreCase("standard") ||
            idata.kind.equalsIgnoreCase("standard-kunststoff"))
            in = getClass().getResourceAsStream("/packs/pack" + n);

        else
            if (idata.kind.equalsIgnoreCase("web") ||
                idata.kind.equalsIgnoreCase("web-kunststoff"))
        {
            /* Special handling for the WebInstallers file.
             *
             * We may have a legal need for packages to be downloaded from
             * different locations.  We also have a legal requirement NOT to
             * download optional packages if the user didn't agree to the
             * license agreement for that package.
             *
             * Also, we want to be able to support hybrid installations, where
             * some packages are contained in the installer, and some are
             * downloaded on demand.  This is convenient for the distribution
             * of core (required) packages.
             */
            String packName = ((Pack) idata.allPacks.get(n)).name;
            String packID = ((Pack) idata.allPacks.get(n)).id;
            String packLabel = packID + "==";
            InputStream kin = getClass().getResourceAsStream
                ("/res/WebInstallers.url");
            BufferedReader kreader = new BufferedReader(new InputStreamReader(kin));
            String line;
            String packageLocation = null;
            while ((line = kreader.readLine()) != null) {
                if (line.startsWith(packLabel)) {
                    packageLocation = line.substring(packLabel.length());
                    System.out.println("location for "+packName+" is "+packageLocation);
                    break;
                }
            }

            if (packageLocation != null &&
                !packageLocation.equalsIgnoreCase("internal")) {
                PackageDownloader downloader = new PackageDownloader
                    (packName, packageLocation, idata.langpack, idata.info.getAppVersion());
                in = downloader.getInputStream();
            } else {
                // the packageLocation is either not specified, or was "internal".
                // attempt to load the package normally.
                in = getClass().getResourceAsStream("/packs/pack" + n);
            }

            if (in == null) {
                Object[] message = {
                    idata.langpack.getString("PackageDownloader.error.info1"),
                    "        " + packName,
                    idata.langpack.getString("PackageDownloader.error.info2").split("[\r\n]+")
                };
                JOptionPane.showMessageDialog(
                    null, message, idata.langpack.getString("PackageDownloader.error.title"),
                    JOptionPane.WARNING_MESSAGE);
            }
        }
        return in;
    }


    /**
     *  Translates a relative path to a local system path.
     *
     * @param  destination  The path to translate.
     * @return              The translated path.
     */
    private String translatePath(String destination)
    {
        // Parse for variables
        destination = vs.substitute(destination, null);

        // Convert the file separator characters
        return destination.replace('/', File.separatorChar);
    }

    private void saveUserValues() {
        Preferences prefs = Preferences.userRoot().node(
              InstallerFrame.USER_VALUES_PREFS_NODE);
        for (int i = 0; i < USER_VALUE_ITEMS.length; i++) {
            maybeSaveUserValue(prefs, USER_VALUE_ITEMS[i]);
        }
    }

    private void maybeSaveUserValue(Preferences prefs, String key) {
        String value = idata.getVariable(key);
        if (value != null)
            prefs.put(key, value);
    }

    private static final String[] USER_VALUE_ITEMS = {
              ScriptParser.INSTALL_PATH, ScriptParser.DATA_PATH,
              ScriptParser.TEAM_DATA_PATH };

}