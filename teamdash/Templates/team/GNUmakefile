JAVAC   ?= //c/java/sun/jdk1.3/bin/javac.exe
DASHJAR = ../../../pspdash/build/pspdash.jar

all:
	$(JAVAC) -classpath $(DASHJAR) *.java

process:
	rm -f /cygdrive/c/temp/pfeTest/Test-v2.zip
	/cygdrive/c/java/sun/jdk1.3/bin/java -cp "$(DASHJAR);." GenerateTestProcess
	cd /cygdrive/c/temp/teamtest; rm -rf Templates settings.xml
	cd /cygdrive/c/temp/teamtest; unzip C:\\temp\\pfeTest\\Test-v2.zip
