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





TEMPLATE_DIRS = \

	DCR \

	SCR \

	SCRcycle \

	psp0 \

	psp0.1 \

	psp2.1 \

	psp3 \

	Inspection \

	Timer




CLASSPATH := /usr/afms02/java/jdk/lib/classes.zip:/usr/afms02/java/swing/swingall.jar:/usr/afms02/java/lib/perltools.jar:..




# set CLASSPATH=C:\Program Files\jdk1.2.2\lib\dt.jar;C:\Program Files\jdk1.2.2\lib\tools.jar;..;N:\PSPDash\perltool.jar

all:
	cd data; $(MAKE) all
	javac PSPDashboard.java


clean:
	rm -f *.class data/*.class compile-log.o data/compile-log.o


new: clean all


install: new
	rm /usr/afms02/psp/pspdash/pspdash.ad
	cp *.class *.gif state /usr/afms02/psp/pspdash
	cp data/*.class /usr/afms02/psp/pspdash/data
	for file in `stat -P Templates`; do \
	    cp $$file /usr/afms02/psp/$$file; \
	done
	perl -pi -e 's;\.\./\.\./\.\.;../..;' /usr/afms02/psp/Templates/*/*.htm
	cd /usr/afms02/psp; jar cf psp.jar Templates pspdash
	if [ -d /floppy/floppy0 ]; then \
	    cp /usr/afms02/psp/psp.jar /floppy/floppy0/psp.jar; \
	fi
	cp pspdash.ad /usr/afms02/psp/pspdash




#pcdisk:
pcdisk: new
	rm -rf pcTemp
	mkdir -p pcTemp/pspdash/data
	for file in $(TEMPLATE_DIRS); do \
	    mkdir -p pcTemp/Templates/$$file; \
	done
	cp *.class *.gif state pcTemp/pspdash
	cp data/*.class pcTemp/pspdash/data
	for file in `stat -P Templates`; do \
	    cp $$file pcTemp/$$file; \
	done
	perl -pi -e 's;../../..;../..;' pcTemp/Templates/*/*.htm

	cd pcTemp; jar cf psp.jar Templates pspdash
	if [ -d /floppy/floppy0 ]; then \
	    cp pcTemp/psp.jar /floppy/floppy0/psp.jar; \
	fi



#templates
templates:
	for file in `stat -P Templates`; do \
	    cp $$file /usr/afms02/psp/$$file; \
	done
	perl -pi -e 's;\.\./\.\./\.\.;../..;' /usr/afms02/psp/Templates/*/*.htm
	cd /usr/afms02/psp; jar cf template.jar Templates
	if [ -d /floppy/floppy0 ]; then \
	    cp /usr/afms02/psp/template.jar /floppy/floppy0/template.jar; \
	fi

%.class: %.java
	javac $^

