# EubosChess
A basic Java chess engine self-built and playable on lichess (https://lichess.org/@/eubos, it is hosted on Heroku). Eubos is rated around 1400-1500 ELO (against other engines - not human - at blitz 2+1, see  http://ccrl.chessdom.com/ccrl/404/).

Eubos uses transpostion hashing and quiescence search extension, but doesn't search very deeply (averaging in the region of 290 KNodes/s in a typical blitz game on an i5 PC). It uses the UCI protocol.

Current release version 1.1.4

You can get a binary from the bin folder, alongside a batch file for running the Eubos engine in a GUI such as Arena.

You must have installed either Java 8 or a later version of the JRE on the PC in order to run Eubos. I am currently running Eubos with Java 14 from https://adoptopenjdk.net/

To install Eubos as a UCI engine in the popular Arena Chess GUI:

1. From the MenuBar, Select Engines > Install New Engine
2. In the file browser dialog that appears, change the drop down to *.bat
3. Navigate to the Eubos binary location, select Eubos.bat
4. Select UCI protocol to communicate with Eubos
5. That's it!

Alternatively, you can install the Eubos.jar file directly in Arena, but if you do this, be aware that the memory allocated to the JVM and Eubos will be capped to 2GB.
