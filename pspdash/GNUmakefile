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
SRC = ..

# Directory where class files should be placed
BIN = build

# Commands to run JDK tools
MSJAVAC ?= //c/java/ms/SDK3.2/bin/jvc.exe
JAVAC   ?= //c/java/sun/jdk1.3/bin/javac.exe
JAR     ?= //c/java/sun/jdk1.3/bin/jar.exe

JAVA_FILES=$(wildcard *.java)
CLASS_FILES=$(subst .java,.class,$(JAVA_FILES))

CGI_JAVA=Templates/psp1/sizeest.java
CGI_CLASSES=$(subst .java,.class,$(CGI_JAVA))

# build platform dependency
BUILD_PLATFORM ?= CYGWIN

######################################################################
# targets
######################################################################


default: pspdash cgi-scripts build


all: data pspdash cgi-scripts


install: all
	cd build; $(MAKE)


install-nt: all
	cd build; $(MAKE) install-nt


build: FORCE
	cd build; $(MAKE)


data: FORCE
	cd data; $(MAKE) all


pspdash: PSPDashboard.class $(CLASS_FILES)


cgi-scripts: $(CGI_CLASSES)

$(CGI_CLASSES) : %.class : %.java
	$(JAVAC) \
	    -classpath build \
	    -deprecation \
	    -sourcepath . \
	    -d . \
	    $^


clean:
	cd build; $(MAKE) clean
	cd data; $(MAKE) clean


new: clean all

FORCE:


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




$(CLASS_FILES) : %.class : $(BIN)/pspdash/%.class


$(BIN)/pspdash/%.class: %.java
	$(JAVAC) \
	    -classpath build/PerlTools.jar \
	    -deprecation \
	    -sourcepath $(SRC) \
	    -d $(BIN) \
	    $^
