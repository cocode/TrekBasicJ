do this before running gradlew

export JAVA_HOME=/Users/tomhill/Library/Java/JavaVirtualMachines/corretto-23.0.2/Contents/Home


/Users/tomhill/Library/Java/JavaVirtualMachines/corretto-23.0.2/Contents/Home/bin/java \
  -cp build/classes/java/main com.worldware.Main \
  /Users/tomhill/PycharmProjects/TrekBasic/programs/superstartrek.bas
  
./gradlew run -Pargs="/Users/tomhill/PycharmProjects/TrekBasic/programs/superstartrek.bas"
  