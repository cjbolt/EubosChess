# EubosChess
A basic Java chess engine, written for kicks. Uses jcpi for the UCI protocol parts. Weak, it's slow and doesn't evaluate for king safety, for example. Currently rated at 1424 ELO on the CCRL (for Blitz 2+1 time control). Uses transpostion hashing and quiescence search extension, but doesn't search very deeply (averaging around 200K Nodes/s in a typical blitz game). This is my first chess engine, my first full Java project, my first open source contribution...

Current release version 1.0.8

You can get a binary from the bin folder, alongside a batch file for running Eubos from Arena.

You must have installed either Java 8 or a later version of the JRE on the PC in order to run Eubos. I am currently running Eubos with Java 14 from https://adoptopenjdk.net/

To install Eubos as a UCI engine in the popular Arena Chess GUI:

1. From the MenuBar, Select Engines > Install New Engine
2. In the file browser dialog that appears, change the drop down to *.bat
3. Navigate to the Eubos binary location, select Eubos.bat
4. Select UCI protocol to communicate with Eubos
5. That's it!

Alternatively, you can install the Eubos.jar file directly in Arena, but if you do this, be aware that the memory allocated to the JVM and Eubos will be capped to 2GB.
