<a href="https://github.com/cjbolt/EubosChess/releases/latest" alt="Latest release">
    <img src="https://img.shields.io/github/v/release/cjbolt/EubosChess?include_prereleases" alt="Latest release">
</a>
<img src="https://img.shields.io/github/actions/workflow/status/cjbolt/EubosChess/maven.yml?branch=master" alt="GitHub Workflow Status">

![Eubos Logo](logo.png)

# EubosChess
Eubos chess is a multi-threaded Java chess engine.

You can play it online, 24/7, via lichess: (https://lichess.org/@/eubos or https://lichess.org/@/baby_eubos). 

Eubos started out as an entirely original engine, based on rather idiosyncratic move generation and an inefficient alpha-beta implementation, but later versions utilise standard algorithms, like negamax search, in order to make it a bit more competitive! The move generation and evaluation is still mostly original work.

It is currently rated around 2350 Elo against other engines, not humans (this is at blitz 2+1 time control, see  http://ccrl.chessdom.com/ccrl/404/).

## Features
Eubos uses a standard alpha-beta negamax algorithm with transposition hashing and quiescence search extension. It utilises lazy move generation and lazy evaluation.

The evaluation function takes account of the following factors
* material balance
* piece mobility
* pawn structure
* king safety
* tactical threats

It knows about draws by 3-fold repetition and insufficient material.

## Configuration
UCI option | Eubos functionality
------------ | -------------
Threads | Sets the number of worker threads that shall be used to perform the search. Configuring 1 means Eubos shall be single-threaded, greater than 1 and it will run multi-threaded.
Hash | Sets the size of the hash table to use, in Megabytes. The hash table is shared by all the threads, it is not duplicated per worker thread.
Move Overhead | Factor for this number of milliseconds on the clock each time a move must be made. This is useful for countering latency in internet games.

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
