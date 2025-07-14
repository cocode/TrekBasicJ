# <img src="images/logo7.png" alt="Logo" width="50" height="25"> TrekBasicJ
TrekBasicJ is a BASIC interpreter and compiler. 

## TrekBasic Family
TrekBasicJ is part of the TrekBasic family of BASIC programming tools.
* [TrekBasic](https://github.com/cocode/TrekBASIC) - Python version
* [TrekBasicJ](https://github.com/cocode/TrekBasicJ) - Java Version
* [BasicRS](https://github.com/cocode/BasicRS) - Rust version
* [BasicTestSuite](https://github.com/cocode/BasicTestSuite) - A test suite of BASIC Programs
* [TrekBot](https://github.com/cocode/TrekBot) - A tool to exercise the superstartrek program

All versions are intended to by byte-by-byte compatible, but are not
there yet - but they are close. TrekBot and BasicTestSuite are part of
plan to ensure full compabtiblity.

TrekBasic and TrekBasicJ are also compilers, and the compatibility
targets are the same for the compiled versions. A compiler for BasicRS is planned.
## The Interpreter 
The interpreter can run anywhere you can run Java.

BEFORE STARTING:

export JAVA_HOME=/Users/tomhill/Library/Java/JavaVirtualMachines/corretto-23.0.2/Contents/Home

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
% java -Xmx100m -cp build/classes/java/main com.worldware.Tbc x.bas x.exe 
Written LLVM IR to x.exe.ll

clang -o x x.exe.ll
warning: overriding the module target triple with arm64-apple-macosx15.0.0 [-Woverride-module]
1 warning generated.
tomhill@Toms-MacBook-Pro TrekBasicJ % ./x
3

