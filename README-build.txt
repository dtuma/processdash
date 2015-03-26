To build the PSP Dashboard, you will need:

   * the Sun JDK, version 1.6.  Set the environment variable
     JAVA_HOME to the path to your JDK installation directory.

   * ant, available from http://ant.apache.org/

   * (optional) to compile in support for context-sensitive help, you
     must install JavaHelp 2.0 from Sun.  Set the environment variable
     JAVAHELP_HOME to point to the javahelp installation directory.

   * (optional) to build the installer, you will need to install the
     bcel library (http://jakarta.apache.org/bcel/) into the "lib"
     directory of your ant distribution.

Then simply run "ant" on the file "build.xml" in this directory.


Many aspects of the build process are configurable via ant properties.
You can override any <property> in the build.xml file by creating a
file called build.properties in this directory.  Place name=value pairs
in that file in java.util.Properties format, and your values will take
precedence over the ones defined in build.xml.


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

   * Due to differences in the way ant and Eclipse build the project,
     ant will be compiling files into a subdirectory called "antbin"
     and Eclipse will be compiling into a subdirectory called "bin".
     This is normal.  Don't try to switch it back, or Eclipse will
     spend a lot of time thrashing each time files in its output
     directory are changed by ant.

   * For running or debugging from within Eclipse, create a launcher
     with these properties:

     - The main class is net.sourceforge.processdash.ProcessDashboard

     - The working directory should a directory on your hard drive
       somewhere, where dashboard data files can be stored.  You
       probably do NOT want to accept the Eclipse default - if you do,
       your project directory will be littered with dashboard data
       files each time you run or debug.  You might also want to avoid
       performing debug sessions on your real pspdata directory.  The
       best approach is therefore to create an empty scratch directory
       on your hard drive somewhere and use it as the working
       directory for Eclipse run/debug sessions.
