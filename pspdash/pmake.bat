cd \src\pspdash\pspdash
del *.class
cd data
del *.class

set CLASSPATH=C:\Program Files\Netscape\Communicator\Program\java\classes:C:\jdk1.2.2\lib\dt.jar;C:\jdk1.2.2\lib\tools.jar;..\..
javac DataApplet.java

set CLASSPATH=C:\Program Files\Netscape\Communicator\Program\java\classes\java40.jar;..\..
javac NSDataApplet.java

cd ..
set CLASSPATH=C:\jdk1.2.2\lib\dt.jar;C:\jdk1.2.2\lib\tools.jar;..;c:\src\pspdash\perltools.jar
javac PSPDashboard.java
