# PSP Dashboard - Data Automation Tool for PSP-like processes
# Copyright (C) 1999  United States Air Force
# 
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
# 
# The author(s) may be contacted at:
# OO-ALC/TISHD
# Attn: PSP Dashboard Group
# 6137 Wardleigh Road
# Hill AFB, UT 84056-5843
# 
# E-Mail POC:  ken.raisor@hill.af.mil

# Codebase for source files
SRC = ../..

# Directory where class files should be placed
# DO NOT place these in directories along with source code - the directories
# themselves will be nuked by code below.
BIN_DATA = ../build
BIN_IE = ../Templates/IE
BIN_NS = ../Templates/NS

# Commands to run JDK tools
MSJAVAC ?= //C/java/ms/SDK4.0/bin/jvc.exe
JAVAC   ?= //C/java/sun/jdk1.2.2/bin/javac.exe
JAR     ?= //C/java/sun/jdk1.2.2/bin/jar.exe


######################################################################
# targets
######################################################################


default: data


all: IE NS data


data: DataRepository.class


IE:
	mkdir -p $(BIN_IE)
	rm -rf $(BIN_IE)/pspdash
	$(MSJAVAC) \
	    /cp:p $(SRC) \
	    /cp:p c:/WINDOWS/java/CLASSES/classes.zip \
	    /d $(BIN_IE) \
	    IEDataApplet.java \
	    IEFieldManager.java \
	    OLEDBAlertWindow.java \
	    ../Settings.java \
	    ../DateFormatter.java
	rm -f $(BIN_IE)/pspdash/data/OLEDBDSLWrapper.class \
	      $(BIN_IE)/pspdash/data/OLEDBListenerWrapper.class
	$(MSJAVAC) \
	    /cp:p $(SRC) \
	    /cp:p c:/WINDOWS/java/CLASSES/classes.zip \
	    /d $(BIN_DATA) \
	    OLEDBListenerWrapper.java \
	    OLEDBDSLWrapper.java


NS:
	mkdir -p $(BIN_NS)
	rm -rf $(BIN_NS)/pspdash
	$(JAVAC) \
	    -classpath ../build/java40.jar \
	    -deprecation \
	    -sourcepath $(SRC) \
	    -d $(BIN_NS) \
	    NSDataApplet.java


clean:
	rm -rf $(BIN_DATA)/pspdash/data
	rm -rf $(BIN_IE)/pspdash
	rm -rf $(BIN_NS)/pspdash


new: clean all




######################################################################
# Rules that make it easy to recompile a particular file, by running
# "make foo.class"
#
# this convenience approach will not work properly for IE or NS
# classes; the resulting class files will be placed into the main data
# directory, not into the appropriate IE or NS class directories.  Thus,
# they may be useful for checking to see if a file compiles, but not
# for building a workable installation.
######################################################################


JAVA_FILES=$(wildcard *.java)
CLASS_FILES=$(subst .java,.class,$(JAVA_FILES))


$(CLASS_FILES) : %.class : $(BIN)/pspdash/data/%.class


$(BIN)/pspdash/data/%.class: %.java
	$(JAVAC) \
	    -classpath ../build/PerlTools.jar \
	    -deprecation \
	    -sourcepath $(SRC) \
	    -d $(BIN_DATA) \
	    $^
