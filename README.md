# <img src="images/logo7.png" alt="Logo" width="50" height="25"> TrekBasicJ
TrekBasicJ is a BASIC interpreter and compiler. 

## TrekBasic Family
TrekBasicJ is part of the TrekBasic family of BASIC programming tools.
* [TrekBasic](https://github.com/cocode/TrekBASIC) - Basic compiler and interpreter in Python
* [TrekBasicJ](https://github.com/cocode/TrekBasicJ) - Basic compiler and interpreter in Java
* [BasicRS](https://github.com/cocode/BasicRS) - Basic compiler written in Rust
* [BasicTestSuite](https://github.com/cocode/BasicTestSuite) - A test suite of BASIC Programs
* [TrekBot](https://github.com/cocode/TrekBot) - A tool to exercise the superstartrek program

All versions, interpreted and compiled, are intended to by byte-by-byte compatible, but are not
there yet - but they are close. TrekBot and BasicTestSuite are part of the
plan to ensure full compatibility.

## The Interpreter 
The interpreter can run anywhere you can run Java.

### Before Starting

Set JAVA_HOME to your JDK. 

For example:

```
export JAVA_HOME=/Users/homedir/Library/Java/JavaVirtualMachines/corretto-23.0.2/Contents/Home
```

### Run one program

java -cp build/classes/java/main com.worldware.Main superstartrek.bas

### Run the BASIC shell
java -cp build/classes/java/main com.worldware.BasicShell x.bas

## The Compiler
TrekBasicJ is not a full compiler - it generates code that can be 
processed by a compiler backend to produce an executable.

TrekBasicJ generates [intermediate representation](https://en.wikipedia.org/wiki/Intermediate_representation) for [LLVM](https://llvm.org/), and
this is compiled with [clang](https://clang.llvm.org/), which is availably basically 
everywhere,

The compiled code can run anywhere you can have clang (c compiler) generate an executable for. This covers dozens of
platforms. Every common architecture and a lot of uncommon ones.

% java -Xmx100m -cp build/classes/java/main com.worldware.Tbc x.bas x.exe







/Users/tomhill/Library/Java/JavaVirtualMachines/corretto-23.0.2/Contents/Home/bin/java \
  -cp build/classes/java/main com.worldware.Main \
  /Users/tomhill/PycharmProjects/TrekBasic/programs/superstartrek.bas
  

TO RUN WITH GRADLE (slower, but builds if necessary)

./gradlew run -Pargs="/Users/tomhill/PycharmProjects/TrekBasic/programs/superstartrek.bas"


TO COMPILE (not full Support yet)
% java -Xmx100m -cp build/classes/java/main com.worldware.Tbc x.bas x 
Written LLVM IR to x.ll

clang -o x x.ll
warning: overriding the module target triple with arm64-apple-macosx15.0.0 [-Woverride-module]
1 warning generated.
 % ./x
3

