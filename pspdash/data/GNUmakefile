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
BIN_IE = ../build/IE
BIN_NS = ../build/NS

# Commands to run JDK tools
MSJAVAC ?= //C/java/ms/SDK4.0/bin/jvc.exe
MSCAB   ?= //c/java/ms/SDK4.0/bin/cabarc.exe
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

COMMON_FILES=DataEvent.java \
	DataListener.java \
	DateData.java \
	DoubleData.java \
	MalformedValueException.java \
	NumberData.java \
	Repository.java \
	SaveableData.java \
	SimpleData.java \
	StringData.java

BROWSER_FILES=../DateFormatter.java \
	../Settings.java \
	$(COMMON_FILES) \
	DataApplet.java \
	DataInterpreter.java \
	DateInterpreter.java \
	DoubleInterpreter.java \
	ForbiddenException.java \
	HTMLField.java \
	HTMLFieldManager.java \
	InputName.java \
	InterpreterFactory.java \
	PercentInterpreter.java \
	RemoteException.java \
	RepositoryClient.java \
	StringInterpreter.java

NS_FILES=$(BROWSER_FILES) \
	NSCheckboxField.java \
	NSDataApplet.java \
	NSField.java \
	NSFieldManager.java \
	NSSelectField.java \
	NSTextField.java

IE_FILES=$(BROWSER_FILES) \
	IEDataApplet.java \
	IEFieldManager.java \
	OLEDBAlertWindow.java

DATA_FILES=$(COMMON_FILES) \
	AdditionFunction.java \
	AndFunction.java \
	ArgumentList.java \
	ComparisonFunction.java \
	CppFilter.java \
	DataComparator.java \
	DataConsistencyObserver.java \
	DataList.java \
	DataRepository.java \
	DeferredData.java \
	DivisionFunction.java \
	EqualsFunction.java \
	FrozenData.java \
	FrozenDate.java \
	FrozenDouble.java \
	FrozenString.java \
	FrozenUtil.java \
	GreaterThanFunction.java \
	GreaterThanOrEqualsFunction.java \
	IfList.java \
	InvalidDatafileFormat.java \
	LessThanFunction.java \
	LessThanOrEqualsFunction.java \
	MalformedData.java \
	MultiplicationFunction.java \
	NotEqualsFunction.java \
	NotFunction.java \
	NumberAliasFunction.java \
	NumberFunction.java \
	OrFunction.java \
	PrefixHierarchy.java \
	ProductList.java \
	RemoteException.java \
	RepositoryListener.java \
	RepositoryServer.java \
	ResultSet.java \
	SimpleList.java \
	SortedList.java \
	StringAliasFunction.java \
	SubtractionFunction.java \
	SumList.java \
	TagData.java \
	ValueFactory.java

IE_CLASSES=$(subst .java,.class,$(IE_FILES))
NS_CLASSES=$(subst .java,.class,$(NS_FILES))
DATA_CLASSES=$(subst .java,.class,$(DATA_FILES))

IE_CLASS_FILES=$(addprefix $(BIN_IE)/pspdash/data/,$(IE_CLASSES))
NS_CLASS_FILES=$(addprefix $(BIN_NS)/pspdash/data/,$(NS_CLASSES))
DATA_CLASS_FILES=$(addprefix $(BIN_DATA)/pspdash/data/,$(DATA_CLASSES))


######################################################################
# targets
######################################################################


default: data

data: $(DATA_CLASS_FILES)

$(BIN_DATA)/pspdash/data/%.class: %.java
	$(JAVAC) \
	    -classpath '../build/PerlTools.jar$(DS)../build/jfreechart.jar' \
	    -deprecation \
	    -sourcepath $(SRC) \
	    -d $(BIN_DATA) \
	    $(DATA_FILES)


ifneq ($(BUILD_PLATFORM),CYGWIN)

all: NS data

else

all: IE NS data

IE: $(IE_CLASS_FILES)

$(BIN_IE)/pspdash/data/%.class: %.java
	mkdir -p $(BIN_IE)
	mkdir -p $(BIN_DATA)/pspdash/data
#	rm -rf $(BIN_IE)/pspdash $(BIN_IE)/DataApplet.cab
	$(MSJAVAC) \
	    /cp:p $(SRC) \
	    /cp:p c:/WINDOWS/java/CLASSES/classes.zip \
	    /d $(BIN_IE) \
	    $(IE_FILES)
	mv -f $(BIN_IE)/pspdash/data/OLEDBDSLWrapper.class $(BIN_DATA)/pspdash/data
	mv -f $(BIN_IE)/pspdash/data/OLEDBListenerWrapper.class $(BIN_DATA)/pspdash/data
	cd $(BIN_IE); $(MSCAB) -r -p n DataApplet.cab pspdash\\*

endif


NS: $(NS_CLASS_FILES)

$(BIN_NS)/pspdash/data/%.class: %.java
	mkdir -p $(BIN_NS)
#	rm -rf $(BIN_NS)/pspdash $(BIN_NS)/DataApplet.jar
	$(JAVAC) \
	    -classpath ../build/java40.jar \
	    -deprecation \
	    -sourcepath $(SRC) \
	    -d $(BIN_NS) \
	    $(NS_FILES)
	cd $(BIN_NS); $(JAR) cf DataApplet.jar pspdash


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


$(DATA_CLASSES) : %.class : $(BIN_DATA)/pspdash/data/%.class

USED_ELSEWHERE=Correlation.java \
	DataCorrelator.java \
	GenericFunction.java \
	Integrator.java \
	Inverse.java \
	LinearRegression.java \
	OLEDBDSLWrapper.java \
	OLEDBListenerWrapper.java \
	StudentsT.java

show_missing_files:
	@echo $(filter-out $(IE_FILES) $(NS_FILES) $(DATA_FILES) $(USED_ELSEWHERE),$(wildcard *.java))
