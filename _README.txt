BEFORE STARTING:

export JAVA_HOME=/Users/tomhill/Library/Java/JavaVirtualMachines/corretto-23.0.2/Contents/Home


TO RUN WITH JAVA:  

  java -cp build/classes/java/main com.worldware.Main superstartrek.bas

Long form:

/Users/tomhill/Library/Java/JavaVirtualMachines/corretto-23.0.2/Contents/Home/bin/java \
  -cp build/classes/java/main com.worldware.Main \
  /Users/tomhill/PycharmProjects/TrekBasic/programs/superstartrek.bas
  

TO RUN WITH GRADLE (slower, but builds if necessary)

./gradlew run -Pargs="/Users/tomhill/PycharmProjects/TrekBasic/programs/superstartrek.bas"