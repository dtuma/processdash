To build the PSP Dashboard, you will need:

   * the Sun JDK, version 1.3 or 1.4.  Set the environment variable
     JAVA_HOME to the path to your JDK installation.

   * ant, available from http://jakarta.apache.org

   * (optional, Windows only) to compile support for internet explorer,
     you must install the Microsoft Java SDK, version 4.0.  Ensure
     that this JDK's "bin" directory is in your PATH.

   * (optional) to compile in support for context-sensitive help, you
     must install JavaHelp from Sun.  Set the environment variable
     JAVAHELP_HOME to point to the javahelp installation directory.

Then simply run "ant" on the file "build.xml" in this directory.
