To build the PSP Dashboard, you will need:

   * the Sun JDK, version 1.3

   * (Windows only) the Microsoft Java SDK, version 4.0

   * a full set of unix-style command line tools, including: 
         cp rm mkdir find sh perl cpp "GNU make" 
     For windows users, the best way to get all of these is to
     download and install the cygwin toolset from
     http://www.cygwin.com/

To compile, set the following environment variables:
   JAVAC should contain the path(*) to the sun java compiler
   JAR should contain the path(*) to the sun jar program

If you are on windows, set the following environment variables:
   MSJAVAC should contain the path(*) to the microsoft java compiler
   MSCAB should contain the path(*) to the microsoft cabarc program

If you are not compiling on Windows, set the environment variable:
   BUILD_PLATFORM to something *other* than "CYGWIN".

(*) IMPORTANT: for Windows users, the paths above need to be expressed
    in cygwin path format instead of Windows path format.  See
    ./GNUmakefile for examples.

Then, from a cygwin bash shell on Windows or any shell on Unix,
cd into the directory containing this file, and type

   make install

(unix users: you may need to type gmake to invoke GNU make.)
