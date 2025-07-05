# TrekBasicJ
TrekBasicJ is a BASIC interpreter and compiler. 

They provide an identical language, and share a parser.

## The Interpreter 
The interpreter can run anywhere you can run Java.

BEFORE STARTING:

export JAVA_HOME=/Users/tomhill/Library/Java/JavaVirtualMachines/corretto-23.0.2/Contents/Home

### Run one program

java -cp build/classes/java/main com.worldware.Main superstartrek.bas

### Run the BASIC shell
java -cp build/classes/java/main com.worldware.BasicShell x.bas

## The Compiler
The compiler can run anywhere you can have clang (c compiler) generate an executable for. This covers dozens of
platforms. Every common architecture and a lot of uncommon ones.

% java -Xmx100m -cp build/classes/java/main com.worldware.Tbc x.bas x.exe







/Users/tomhill/Library/Java/JavaVirtualMachines/corretto-23.0.2/Contents/Home/bin/java \
  -cp build/classes/java/main com.worldware.Main \
  /Users/tomhill/PycharmProjects/TrekBasic/programs/superstartrek.bas
  

TO RUN WITH GRADLE (slower, but builds if necessary)

./gradlew run -Pargs="/Users/tomhill/PycharmProjects/TrekBasic/programs/superstartrek.bas"


TO COMPILE (not full Support yet)
% java -Xmx100m -cp build/classes/java/main com.worldware.Tbc x.bas x.exe 
Written LLVM IR to x.exe.ll

clang -o x x.exe.ll
warning: overriding the module target triple with arm64-apple-macosx15.0.0 [-Woverride-module]
1 warning generated.
tomhill@Toms-MacBook-Pro TrekBasicJ % ./x
3

