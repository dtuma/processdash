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


all: pspForEng.jar


######################################################################
# Build the jar file for distributing this process set.

DIR=Templates/pspForEng
JAR_CONTENTS=Templates/pspForEng.template \
	$(DIR)/dataFile.txt \
	$(DIR)/*A/*.* \
	$(DIR)/R*/dataFile.txt
pspForEng.jar: prog2A prog4A prog5A prog7A prog8A manifest dataFile.txt ../pspForEng.template
	cd ../..; $(JAR) -cfm $(DIR)/pspForEng.jar $(DIR)/manifest $(JAR_CONTENTS)

######################################################################
# Build the files for program 2A, from PSP0.1

PSP01_FILES=$(notdir $(wildcard ../psp0.1/*.*htm))
PROG_2A_FILES=$(addprefix 2A/,$(PSP01_FILES))
prog2A: $(PROG_2A_FILES)
$(PROG_2A_FILES): 2A/% : ../psp0.1/%
	$(PERL) -p filter cut2A $< > $@


######################################################################
# Build the files for program 4A, from PSP1.0

PSP1_FILES=$(notdir $(wildcard ../psp1/*.*htm))
PROG_4A_FILES=$(addprefix 4A/,$(PSP1_FILES))
prog4A: $(PROG_4A_FILES) 4A/sizeest.class
$(PROG_4A_FILES): 4A/% : ../psp1/%
	$(PERL) -p filter cut4A $< > $@


######################################################################
# Build the files for program 5A, from PSP1.1

PSP11_FILES=$(notdir $(wildcard ../psp1.1/*.*htm))
PROG_5A_FILES=$(addprefix 5A/,$(PSP11_FILES))
prog5A: $(PROG_5A_FILES) 5A/sizeest.class
$(PROG_5A_FILES): 5A/% : ../psp1.1/%
	$(PERL) -p filter cut5A $< > $@


######################################################################
# Build the files for program 7A, from PSP2

PSP2_FILES=$(notdir $(wildcard ../psp2/*.*htm))
PROG_7A_FILES=$(addprefix 7A/,$(PSP2_FILES))
prog7A: $(PROG_7A_FILES) 7A/sizeest.class
$(PROG_7A_FILES): 7A/% : ../psp2/%
	$(PERL) -p filter cut7A $< > $@


######################################################################
# Build the files for program 8A, from PSP2.1

PSP21_FILES=$(notdir $(wildcard ../psp2.1/*.*htm))
PROG_8A_FILES=$(addprefix 8A/,$(PSP21_FILES))
prog8A: $(PROG_8A_FILES) 8A/sizeest.class
$(PROG_8A_FILES): 8A/% : ../psp2.1/%
	$(PERL) -p filter cut8A $< > $@


######################################################################
# Make needed copies of the size estimating template class.

SIZEEST_COPIES=$(addsuffix /sizeest.class,4A 5A 7A 8A)
$(SIZEEST_COPIES): ../psp1/sizeest.class
	cp $< $@

# Other miscellaneous targets

install: all
	cp pspForEng.jar ../../build/pspForEng.jar

clean:
	rm -f *~ *A/*~ *A/*.*htm *A/sizeest.class
