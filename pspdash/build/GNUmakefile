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
		script.gif \
		scriptd.gif \
		splash.gif \
		state
INSTALLED_FILES=$(addprefix pspdash/,$(NON_CLASS_FILES))

all: pspdash.jar

install: pspdash.jar

# package everything up for distribution
pspdash.jar: $(INSTALLED_FILES) manifest perltools templates FORCE
	$(JAR) cfm pspdash.jar pspdash manifest com Templates


# make a local copy of the contents of the templates directory, without
# CVS stuff or emacs backup~ files.
templates: FORCE
	rm -rf Templates
	cp -R ../Templates .
	rm -rf `find Templates -name CVS -print`
	rm -f `find Templates -type f -print | fgrep '~'`


# copy non-class files if needed.
$(INSTALLED_FILES) : pspdash/% : ../%
	cp $< pspdash


# unzip the PerlTools jarfile.
perltools: com/oroinc/text/perl/Perl5Util.class

com/oroinc/text/perl/Perl5Util.class: PerlTools.jar
	$(JAR) -xf PerlTools.jar


unused:
	mkdir -p install/pspdash/data
	cp pspdash.jar PerlTools.jar install
	cp ../pspdash.ad install/pspdash.ini
	cp -R Templates install
	rm -rf `find install -name CVS -print`
	mv install/Templates/IE/pspdash/data/OLEDBListenerWrapper.class install/Templates/IE/pspdash/data/OLEDBDSLWrapper.class install/pspdash/data
	cd install; zip -R pspdash-install.zip pspdash Templates pspdash.ini pspdash.jar PerlTools.jar




clean:
	rm -f pspdash.jar 
	rm -rf pspdash Templates

FORCE:
