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
JHINDEXER ?= //c/java/sun/jh1.1/javahelp/bin/jhindexer.exe

JAVA_FILES=$(wildcard *.java)
CLASS_FILES=$(subst .java,.class,$(JAVA_FILES))

CGI_JAVA=Templates/psp1/sizeest.java \
	Templates/psp3/cyclesum.java \
	Templates/generic/summary.java \
	Templates/control/clearCGI.java \
	Templates/control/dumpData.java \
	Templates/control/exportNow.java \
	Templates/control/importNow.java \
	Templates/control/raiseWindow.java \
	Templates/control/setPath.java \
	Templates/control/showenv.java \
	Templates/control/showConsole.java \
	Templates/control/showTaskSchedule.java \
	Templates/control/startTiming.java \
	Templates/control/stopTiming.java \
	Templates/dash/file.java \
	Templates/dash/pspdiff.java \
	Templates/dash/pspdiffDialog.java \
	Templates/dash/selectRollup.java \
	Templates/reports/psp/r3c.java \
	Templates/reports/psp/r4.java \
	Templates/reports/psp/r5a.java \
	Templates/reports/psp/r5b.java \
	Templates/reports/timelog.java \
	Templates/reports/defectlog.java \
	Templates/reports/dts.java \
	Templates/reports/bar.java \
	Templates/reports/excel.java \
	Templates/reports/ev.java \
	Templates/reports/week.java \
	Templates/reports/form2html.java \
	Templates/reports/line.java \
	Templates/reports/pie.java \
	Templates/reports/radar.java \
	Templates/reports/xy.java \
	Templates/reports/table.java \
	Templates/reports/hist.java \
	Templates/reports/probe/probe.java \
	Templates/reports/probe/HistData.java \
	Templates/reports/probe/Method.java \
	Templates/reports/probe/MethodD.java \
	Templates/reports/probe/AveragingMethod.java \
	Templates/reports/probe/RegressionMethod.java \
	Templates/CFilter.java \
	Templates/CobolFilter.java \
	Templates/SQLFilter.java \
	Templates/ShFilter.java \
	Templates/PascalFilter.java \
	Templates/DefaultFilter.java
CGI_CLASSES=$(subst .java,.class,$(CGI_JAVA))
SIZEEST_COPIES=Templates/psp1.1/sizeest.class \
	Templates/psp1.0.1/sizeest.class \
	Templates/psp2/sizeest.class \
	Templates/psp2.1/sizeest.class \
	Templates/psp3/sizeest.class

JFREECHART_JAVA=Templates/reports/XYPlot.java \
	Templates/reports/PiePlot.java \
	Templates/reports/RadarPlot.java
JFREECHART_CLASSES=$(subst .java,.class,$(JFREECHART_JAVA))

SCR_DCR_FILES=Templates/SCR.globalData \
	Templates/SCR.template \
	Templates/DCR.template \
	Templates/Insp.template \
	Templates/SCR/*.* \
	Templates/SCRcycle/*.* \
	Templates/DCR/*.* \
	Templates/Inspection/*.*


SUN_PLUG_DIR=Templates/help/Topics/Troubleshooting/DataApplet
SUN_PLUG_JAR=$(SUN_PLUG_DIR)/SunPlugin.jar

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


all: data pspdash cgi-scripts $(SUN_PLUG_JAR) build

install: all

build: FORCE
	cd build; $(MAKE)

data: FORCE
	cd data; $(MAKE) all

# for some reason, jhindexer dies horribly when run from cygwin prompt
ifeq ($(BUILD_PLATFORM),CYGWIN)
help-search: FORCE
	cd Templates/help; cmd /c $(JHINDEXER) -c help.config
else
help-search: FORCE
	cd Templates/help; $(JHINDEXER) -c help.config
endif

pspdash: PSPDashboard.class $(CLASS_FILES)


cgi-scripts: $(CGI_CLASSES) $(SIZEEST_COPIES) Templates/reports/excel.iqy 

$(SIZEEST_COPIES): Templates/psp1/sizeest.class
	cp Templates/psp1/sizeest.class $@

$(SUN_PLUG_JAR): $(SUN_PLUG_DIR)/SunPlugin.java
	cd $(SUN_PLUG_DIR); \
	rm -rf pspdash IEDataApplet.java; \
	cp SunPlugin.java IEDataApplet.java; \
	$(JAVAC) -d . IEDataApplet.java
	cd $(SUN_PLUG_DIR); \
	$(JAR) cf SunPlugin.jar pspdash; \
	rm -rf pspdash IEDataApplet.java

Templates/reports/pie.class: $(BIN)/com/jrefinery/chart/PiePlot.class
Templates/reports/radar.class: $(BIN)/com/jrefinery/chart/RadarPlot.class

Templates/reports/xy.class: $(BIN)/com/jrefinery/chart/XYPlot.class $(BIN)/com/jrefinery/chart/DataSources.class

$(CGI_CLASSES) : %.class : %.java
	$(JAVAC) \
	    -classpath 'build$(DS)build/PerlTools.jar$(DS)build/jfreechart.jar$(DS)build/jaxp.jar$(DS)build/crimson.jar' \
	    -deprecation \
	    -d $(dir $<) \
	    -sourcepath '$(SRC)$(DS)$(dir $<)' \
	    $<

add-on-scripts: build/SCR-DCR.jar

build/SCR-DCR.jar: Templates/SCR/manifest $(SCR_DCR_FILES)
	$(JAR) cfm build/SCR-DCR.jar Templates/SCR/manifest $(SCR_DCR_FILES)

Templates/reports/excel.iqy: Templates/reports/excel.class
	cp Templates/reports/excel.class Templates/reports/excel.iqy

DISABLED_WARNINGS=directory-index,img-alt,extension-attribute,extension-markup,attribute-delimiter

weblint:
	weblint -d $(DISABLED_WARNINGS) Templates | \
	grep -v -f weblint-ignore

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
	    -classpath 'build$(DS)build/PerlTools.jar$(DS)build/jfreechart.jar$(DS)build/jh.jar$(DS)build/jaxp.jar$(DS)build/crimson.jar' \
	    -deprecation \
	    -sourcepath $(SRC) \
	    -d $(BIN) \
	    $^

DataSources.class: $(BIN)/com/jrefinery/chart/DataSources.class
XYPlot.class: $(BIN)/com/jrefinery/chart/XYPlot.class
PiePlot.class: $(BIN)/com/jrefinery/chart/PiePlot.class
RadarPlot.class: $(BIN)/com/jrefinery/chart/RadarPlot.class

$(BIN)/com/jrefinery/chart/%.class: Templates/reports/%.java
	$(JAVAC) \
	    -classpath 'build$(DS)build/PerlTools.jar$(DS)build/jfreechart.jar' \
	    -deprecation \
	    -sourcepath $(SRC) \
	    -d $(BIN) \
	    $^
