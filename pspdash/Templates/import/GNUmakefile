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


all: importPSP.jar


######################################################################
# Build the jar file for distributing this process set.

DIR=Templates/import
JAR_CONTENTS=Templates/import.template \
	$(DIR)/dataFile.txt \
	$(DIR)/import.htm
importPSP.jar: ../import.template dataFile.txt import.htm manifest
	cd ../..; $(JAR) -cfm $(DIR)/importPSP.jar $(DIR)/manifest $(JAR_CONTENTS)

clean:
	rm -f *~ importPSP.jar
