To build the PSP Dashboard, you will need:

   * the Sun JDK, version 1.4.  Set the environment variable
     JAVA_HOME to the path to your JDK installation directory.

   * ant, available from http://ant.apache.org/

   * (optional) to compile in support for context-sensitive help, you
     must install JavaHelp 2.0 from Sun.  Set the environment variable
     JAVAHELP_HOME to point to the javahelp installation directory.

Then simply run "ant" on the file "build.xml" in this directory.


Optional: If you use Eclipse to edit Java code, take note:

   * A .classpath and .project file are included for your use.  The
     .classpath file makes use of two Eclipse "path variables" that
     you will probably need to define manually: ANT_HOME and JDK_HOME.

   * Some of the java files in the project are auto-generated during
     the ant build process.  As a result, you will need to run "ant"
     at least once to generate these files, then tell Eclipse to
     "refresh" the project.

   * Although you can edit and refactor the code with Eclipse, you'll
     need to use the ant script to package the process dashboard into
     the final, distributable "jar" file.
