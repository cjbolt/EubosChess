# EubosChess
A basic Java chess engine, written for kicks. Uses jcpi for the UCI protocol parts. Kind of weak, doesn't evaluate for king safety, for example. Now uses transpostion hashing, but because it runs in the Java VM on a bog standard PC, doesn't search very deeply (around 100K Nodes/s, though this is actually quite hard to evaluate due to the hashing). This is my first chess engine, my first full Java project, my first open source contribution...

Current release version 1.0.3

You can get a binary from the bin folder, alongside a batch file for running Eubos from Arena.

You must have installed Java version 8 on the PC to use Eubos (get it from java.com)

To install Eubos as an engine in Area:

1. Select Engines > Install New Engine
2. In the file browser dialog that appears, change the drop down to *.bat
3. Navigate to the Eubos binary location, select the StartEubos.bat
4. Select UCI protocol to communicate with Eubos
5. That's it!
