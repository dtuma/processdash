#!/bin/sh

# This file is a shell script, designed to run the Process Dashboard
# Launcher app on a Unix/Linux system.

# "java" needs to be in your path.  If it is are not in your path by
# default, you should insert a line here to add it.
# export PATH=$PATH:/path/to/java/dir

# retrieve the directory this script is located in.
BASEDIR=$(dirname "$0")

# Runs the Process Dashboard Launcher.  You can edit the file below if 
# needed to add other command line arguments for java (such as memory
# settings, etc)
#
java -jar "$BASEDIR/launcher.jar" $@
