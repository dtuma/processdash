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

PERL ?= perl


all: pspForMSE.jar


######################################################################
# Build the jar file for distributing this process set.

DIR=Templates/pspForMSE
JAR_CONTENTS=Templates/pspForMSE.template \
   Templates/PSP1_0_1.template \
   Templates/psp1.0.1/*.* \
	$(DIR)/dataFile.txt \
	$(DIR)/2A/*.* \
	$(DIR)/*B/*.* \
	$(DIR)/R*/dataFile.txt
pspForMSE.jar: prog2A prog3B prog4B manifest dataFile.txt ../pspForMSE.template
	cd ../..; $(JAR) -cfm $(DIR)/pspForMSE.jar $(DIR)/manifest $(JAR_CONTENTS)

######################################################################
# Build the files for program 2A, from PSP0.1

PSP01_FILES=$(notdir $(wildcard ../psp0.1/*.*htm))
PROG_2A_FILES=$(addprefix 2A/,$(PSP01_FILES))
prog2A: $(PROG_2A_FILES)
$(PROG_2A_FILES): 2A/% : ../psp0.1/%
	$(PERL) -p filter cut2A $< > $@


######################################################################
# Build the files for program 3B, from PSP1.0

PSP1_FILES=$(notdir $(wildcard ../psp1/*.*htm))
PROG_3B_FILES=$(addprefix 3B/,$(PSP1_FILES))
prog3B: $(PROG_3B_FILES) 3B/sizeest.class
$(PROG_3B_FILES): 3B/% : ../psp1/%
	$(PERL) -p filter cut3B $< > $@


######################################################################
# Build the files for program 4B, from PSP1.0.1

PSP101_FILES=$(notdir $(wildcard ../psp1.0.1/*.*htm))
PROG_4B_FILES=$(addprefix 4B/,$(PSP101_FILES))
prog4B: $(PROG_4B_FILES) 4B/sizeest.class
$(PROG_4B_FILES): 4B/% : ../psp1.0.1/%
	$(PERL) -p filter cut4B $< > $@


######################################################################
# Make needed copies of the size estimatating template class.

SIZEEST_COPIES=$(addsuffix /sizeest.class,3B 4B)
$(SIZEEST_COPIES): ../psp1/sizeest.class
	cp $< $@

# Other miscellaneous targets

clean:
	rm -f *~ *B/*~ *A/*~ *B/*.*htm *A/*.*htm *B/sizeest.class pspForMSE.jar

install: all
	cp pspForMSE.jar ../../build/pspForMSE.jar
