<a href="https://github.com/cjbolt/EubosChess/releases/latest" alt="Latest release">
    <img src="https://img.shields.io/github/v/release/cjbolt/EubosChess?include_prereleases" alt="Latest release">
</a>
<img alt="GitHub Workflow Status" src="https://img.shields.io/github/workflow/status/cjbolt/EubosChess/java-ci-with-maven">

# EubosChess
Eubos chess is a multi-threaded Java chess engine. It was self-built and is playable online 24/7 via lichess (https://lichess.org/@/eubos - it is hosted on Heroku). It is rated around 1500 ELO against other engines, not humans (this is at blitz 2+1 time control, see  http://ccrl.chessdom.com/ccrl/404/). It uses the UCI protocol.

## Features
Eubos uses a standard alpha beta minimax algorithm with transpostion hashing and quiescence search extension.

The evaluation function takes account of the following factors
* material balance
* piece mobility
* pawn structure
* king safety

It knows about draws by 3-fold repetition and insufficient material.

## Current version
Current release version 2.0

## Configuration
UCI option | Eubos functionality
------------ | -------------
Threads | Sets the number of worker threads that shall be used to perform the search. Configuring 1 means Eubos shall be single-threaded, greater than 1 and it will run multi-threaded.
Hash | Sets the size of the hash table to use, in Megabytes. The hash table is shared by all the threads, it is not duplicated per worker thread.

## Installation
You can get a binary from the bin folder, alongside a batch file for running the Eubos engine in a GUI such as Arena.

You must have installed either Java 8 or a later version of the JRE on the PC in order to run Eubos. I am currently running Eubos with Java 14 from https://adoptopenjdk.net/

To install Eubos as a UCI engine in the popular Arena Chess GUI:

1. From the MenuBar, Select Engines > Install New Engine
2. In the file browser dialog that appears, change the drop down to *.bat
3. Navigate to the Eubos binary location, select Eubos.bat
4. Select UCI protocol to communicate with Eubos
5. That's it!

Alternatively, you can install the Eubos.jar file directly in Arena, but if you do this, be aware that the JVM won't be optimised for the Eubos application and also the memory allocated to the JVM will be capped to 2GB.
