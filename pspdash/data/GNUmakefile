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

# where are the MS specific jars?
MSJARS  ?= ../../msjars

# build platform dependency
BUILD_PLATFORM ?= CYGWIN
ifeq ($(BUILD_PLATFORM),CYGWIN)
   DS=;
else
   DS=:
endif

# If using javac from JDK1.3, we need these dependencies.  They won't get 
# automatically compiled even though they are explicitly imported by 
# ValueFactory.
EXTRAS=AdditionFunction.class SubtractionFunction.class \
 MultiplicationFunction.class DivisionFunction.class AndFunction.class \
 OrFunction.class NotFunction.class EqualsFunction.class \
 LessThanFunction.class GreaterThanFunction.class \
 LessThanOrEqualsFunction.class GreaterThanOrEqualsFunction.class \
 NotEqualsFunction.class ResultSet.class

######################################################################
# targets
######################################################################


default: data


all: IE NS data


data: DataRepository.class $(EXTRAS)


IE:
ifeq ($(BUILD_PLATFORM),CYGWIN)
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
else
	mkdir -p $(BIN_IE)
	rm -rf $(BIN_IE)/pspdash
	$(JAVAC) \
	    -classpath '$(SRC)$(DS)$(MSJARS)/msclasseszip.jar$(DS)$(MSJARS)/msospclasses.jar$(DS)$(MSJARS)/mswfcclasses.jar' \
	    -d $(BIN_IE) \
	    IEDataApplet.java \
	    IEFieldManager.java \
	    OLEDBAlertWindow.java \
	    ../Settings.java \
	    ../DateFormatter.java
	rm -f $(BIN_IE)/pspdash/data/OLEDBDSLWrapper.class \
	      $(BIN_IE)/pspdash/data/OLEDBListenerWrapper.class
	$(JAVAC) \
	    -classpath '$(SRC)$(DS)$(MSJARS)/msclasseszip.jar$(DS)$(MSJARS)/msospclasses.jar$(DS)$(MSJARS)/mswfcclasses.jar' \
	    -d $(BIN_DATA) \
	    OLEDBListenerWrapper.java \
	    OLEDBDSLWrapper.java
endif

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
	rm -rf $(BIN_DATA)/pspdash
	rm -rf $(BIN_IE)
	rm -rf $(BIN_NS)


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


$(CLASS_FILES) : %.class : $(BIN_DATA)/pspdash/data/%.class


$(BIN_DATA)/pspdash/data/%.class: %.java
	$(JAVAC) \
	    -classpath '../build/PerlTools.jar$(DS)../build/jfreechart.jar' \
	    -deprecation \
	    -sourcepath $(SRC) \
	    -d $(BIN_DATA) \
	    $^
