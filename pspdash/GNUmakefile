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

CGI_JAVA=Templates/psp1/sizeest.java \
	Templates/psp3/cyclesum.java \
	Templates/reports/psp/r3c.java \
	Templates/reports/psp/r4.java \
	Templates/reports/psp/r5a.java \
	Templates/reports/psp/r5b.java \
	Templates/reports/timelog.java \
	Templates/reports/defectlog.java \
	Templates/reports/dts.java \
	Templates/reports/bar.java \
	Templates/reports/excel.java \
	Templates/reports/line.java \
	Templates/reports/pie.java \
	Templates/reports/xy.java \
	Templates/reports/table.java
CGI_CLASSES=$(subst .java,.class,$(CGI_JAVA))
SIZEEST_COPIES=Templates/psp1.1/sizeest.class \
	Templates/psp2/sizeest.class \
	Templates/psp2.1/sizeest.class \
	Templates/psp3/sizeest.class

JFREECHART_JAVA=Templates/reports/XYPlot.java \
	Templates/reports/PiePlot.java
JFREECHART_CLASSES=$(subst .java,.class,$(JFREECHART_JAVA))

SCR_DCR_FILES=Templates/SCR.globalData \
	Templates/SCR.template \
	Templates/DCR.template \
	Templates/Insp.template \
	Templates/SCR/*.* \
	Templates/SCRcycle/*.* \
	Templates/DCR/*.* \
	Templates/Inspection/*.*

# build platform dependency
BUILD_PLATFORM ?= CYGWIN
ifeq ($(BUILD_PLATFORM),CYGWIN)
   DS=;
else
   DS=:
endif

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


pspdash: PSPDashboard.class $(CLASS_FILES) Templates/PSP.globalData


cgi-scripts: $(CGI_CLASSES) $(SIZEEST_COPIES) Templates/reports/excel.iqy

$(SIZEEST_COPIES): Templates/psp1/sizeest.class
	cp Templates/psp1/sizeest.class $@

$(CGI_CLASSES) : %.class : %.java $(BIN)/com/jrefinery/chart/XYPlot.class $(BIN)/com/jrefinery/chart/PiePlot.class
	$(JAVAC) \
	    -classpath 'build$(DS)build/PerlTools.jar$(DS)build/jfreechart.jar' \
	    -deprecation \
	    -d $(dir $<) \
	    $<

build/SCR-DCR.jar: $(SCR_DCR_FILES)
	$(JAR) cf build/SCR-DCR.jar $(SCR_DCR_FILES)

Templates/reports/excel.iqy: Templates/reports/excel.class
	cp Templates/reports/excel.class Templates/reports/excel.iqy

Templates/PSP.globalData: Templates/PSP.globalData.pre
	cpp -E -x c Templates/PSP.globalData.pre -o Templates/PSP.globalData -P -traditional

clean:
	cd build; $(MAKE) clean
	cd data; $(MAKE) clean
	rm -f *~ `find Templates -name \*.class -print`


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
	    -classpath 'build$(DS)build/PerlTools.jar$(DS)build/jfreechart.jar' \
	    -deprecation \
	    -sourcepath $(SRC) \
	    -d $(BIN) \
	    $^

XYPlot.class: $(BIN)/com/jrefinery/chart/XYPlot.class
PiePlot.class: $(BIN)/com/jrefinery/chart/PiePlot.class

$(BIN)/com/jrefinery/chart/%.class: Templates/reports/%.java
	$(JAVAC) \
	    -classpath 'build$(DS)build/PerlTools.jar$(DS)build/jfreechart.jar' \
	    -deprecation \
	    -sourcepath $(SRC) \
	    -d $(BIN) \
	    $^
