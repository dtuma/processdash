IMPORTANT NOTICE:

This is the source code for an OBSOLETE version of the Process
Dashboard.  The current source code under active development is
located in the "processdash" module of cvs, not the "pspdash" module.




----------------------------------------------------------------------

To build the PSP Dashboard, you will need:

   * the Sun JDK, version 1.4.  Set the environment variable
     JAVA_HOME to the path to your JDK installation directory.

   * ant, available from http://ant.apache.org/

   * (optional, Windows only) to compile support for internet
     explorer, you must install the Microsoft Java SDK, version 4.0.
     Ensure that this JDK's "bin" directory is in your PATH.

   * (optional) to compile in support for context-sensitive help, you
     must install JavaHelp from Sun.  Set the environment variable
     JAVAHELP_HOME to point to the javahelp installation directory.

Then simply run "ant" on the file "build.xml" in this directory.
