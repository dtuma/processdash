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


# Commands to run JDK tools
JAR ?= //C/java/sun/jdk1.2.2/bin/jar.exe


# non-class Files that need to be a part of the distribution jar file
NON_CLASS_FILES=continue.gif \
		defect.gif \
		defectd.gif \
		icon32.gif \
		mime_types \
		pause.gif \
		pspdash.ad \
		pspdash.ad-comments \
		script.gif \
		scriptd.gif \
		splash.gif \
		state
INSTALLED_FILES=$(addprefix pspdash/,$(NON_CLASS_FILES))

TEMPLATE_FILES:=$(shell cd ..; find Templates -type f -print | fgrep -v CVS | fgrep -v .java | fgrep -v '~' | fgrep -v .cvsignore)

all: pspdash.jar

install: pspdash.jar

# "no templates" version of the jar file.  A little nicer for playing with 
# the HTML and/or CGI stuff at runtime.
install-nt: $(INSTALLED_FILES) manifest perltools templates FORCE
	$(JAR) cfm pspdash.jar manifest pspdash com


# package everything up for distribution
pspdash.jar: $(INSTALLED_FILES) manifest perltools jfreechart templates FORCE
	$(JAR) cfm pspdash.jar manifest pspdash com Templates


# make a local copy of the contents of the templates directory, without
# CVS stuff or emacs backup~ files.
templates: $(TEMPLATE_FILES)

$(TEMPLATE_FILES) : Templates/% : ../Templates/%
	@mkdir -p '$(dir Templates/$*)'
	cp '../Templates/$*' 'Templates/$*'



# copy non-class files if needed.
$(INSTALLED_FILES) : pspdash/% : ../%
	@mkdir -p pspdash
	cp $< $@


# unzip the PerlTools jarfile.
perltools: com/oroinc/text/perl/Perl5Util.class

com/oroinc/text/perl/Perl5Util.class: PerlTools.jar
	$(JAR) -xf PerlTools.jar

# unzip the JFreeChart jarfile.
jfreechart: com/jrefinery/chart/JFreeChart.class com/jrefinery/chart/XYPlot.class

com/jrefinery/chart/JFreeChart.class: jfreechart.jar
	$(JAR) -xf jfreechart.jar
	rm -rf com/jrefinery/util com/jrefinery/chart/ui com/jrefinery/chart/XYPlot.class

com/jrefinery/chart/XYPlot.class: ../Templates/reports/XYPlot.java
	cd ..; make XYPlot.class

clean:
	rm -f pspdash.jar *~
	rm -rf pspdash Templates com META-INF

FORCE:
