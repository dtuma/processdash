JAVAC   ?= //c/java/sun/jdk1.3/bin/javac.exe
JAR ?= //C/java/sun/jdk1.2.2/bin/jar.exe
DASHJAR = ../../../pspdash/build/pspdash.jar

all:
	$(JAVAC) -classpath $(DASHJAR) *.java
	cd setup; make

jar:
	cd ../..; $(JAR) cf TeamProcessEditor.jar Templates\\team

clean:
	rm *~ *.class

process:
	rm -f /cygdrive/c/temp/pfeTest/Test-v2.zip
	/cygdrive/c/java/sun/jdk1.3/bin/java -cp "$(DASHJAR);." GenerateTestProcess
	cd /cygdrive/c/temp/teamtest; rm -rf Templates settings.xml
	cd /cygdrive/c/temp/teamtest; unzip C:\\temp\\pfeTest\\Test-v2.zip
