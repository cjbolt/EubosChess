<a href="https://github.com/cjbolt/EubosChess/releases/latest" alt="Latest release">
    <img src="https://img.shields.io/github/v/release/cjbolt/EubosChess?include_prereleases" alt="Latest release">
</a>
<img src="https://img.shields.io/github/actions/workflow/status/cjbolt/EubosChess/maven.yml?branch=master" alt="GitHub Workflow Status">

![Eubos Logo, courtesy of Graham Banks](logo.png "Image Credit: Graham Banks")

# Eubos
Eubos chess is a multi-threaded Java chess engine.

You can play it online, 24/7, via lichess: (https://lichess.org/@/eubos or https://lichess.org/@/baby_eubos). 

Eubos started out as an entirely original engine, based on rather idiosyncratic move generation and an inefficient alpha-beta implementation, but later versions use standard algorithms, like negascout search, in order to make it a bit more competitive!

It is currently rated around 2500 Elo against other engines, not humans (this is at blitz 2+1 time control, see  http://ccrl.chessdom.com/ccrl/404/).

## History
Version | Brief Notes
------------ | -------------
0.x | Self-built and esoteric implementation with significant novel code. Brute force search, no reductions. Based on the David Levy book 'How Computers Play Chess'
1.x | Continued fixing bugs and optimising the v0.x codebase. Incremental improvements and basic engine programming algorithms were implemented following discovery of CPW.
2.x | Added support for lazy SMP multi-threading, changed to a standard negamax search and started implementing reductions.
3.x | Changed to negascout implementation for search, tweaks to evaluation and reductions
4.x | Changed to neural net based evaluation function (1x128 hidden layer, trained using a datbase of self-generated supervised learning)

## Features
Eubos uses a standard alpha-beta negascout algorithm with transposition hashing and quiescence search extension. It uses staged move generation with look-up tables pre-calculated. It knows about draws by 3-fold repetition and insufficient material.

As of version 4.0 it uses a neural net based evaluation function (all prior versions were hand crafted evaluation).

Originally the search was Shannon Type A, i.e brute force, searching every single move to a certain depth before searching deeper. Since around version 2.1 it has been Shannon Type B, i.e. it uses a selective search. Not all moves are searched to the same depth. Some are considered relatively unimportant and ignored. To this end, pruning and reduction algorithms used are late move reduction, null move pruning and futility pruning.

## Configuration
UCI option | Eubos functionality
------------ | -------------
Threads | Sets the number of worker threads that shall be used to perform the search. Configuring 1 means Eubos shall be single-threaded, greater than 1 and it will run multi-threaded.
Hash | Sets the size of the hash table to use, in Megabytes. The hash table is shared by all the threads, it is not duplicated per worker thread.
Move Overhead | Factor for this number of milliseconds on the clock each time a move must be made. This is useful for countering latency in internet games.
Training Data Generation | A boolean enabling exporting a bullet format text file with the FEN and score at each ply search
Random Move | A boolean used to generate random games to widen the training data coverage

## Installation
You can get a binary from the bin folder, alongside a batch file for running the Eubos engine in a GUI such as Arena or Cutechess.

You must have installed either Java 11 or a later version of the JRE on the PC in order to run Eubos. I am currently running Eubos with Java 14 from https://adoptopenjdk.net/

### Arena
To install Eubos as a UCI engine in the popular Arena Chess GUI:

1. From the MenuBar, Select Engines > Install New Engine
2. In the file browser dialog that appears, change the drop down to *.bat
3. Navigate to the Eubos binary location, select Eubos.bat
4. Select UCI protocol to communicate with Eubos
5. That's it!

Alternatively, you can install the Eubos.jar file directly in Arena, but if you do this, be aware that the JVM won't be optimised for the Eubos application and also the memory allocated to the JVM will be capped to 2GB.

## Credits
Thanks to David Levy and Monty Newborn for their excellent books on Computer Chess and Games Programming. Particularly, I never would have written Eubos without 'How Computers Play Chess' (1991).

Thanks to Dominik Klein for writing 'Neural Networks for Chess' (2023). A really nice book that was the departure point for me writing Eubos v4.0. I love the spirit of this book and I was happy to buy the print version from Amazon. I can even forgive you the many typos ;)

For v4.0, I have to credit and offer thanks to Jamie whiting, author of the Bullet Neural Net training tool. His excellent code and documentation were invaluable. I must also thank and credit the author of Bagatur, from which I derived the basic inference code for accessing a Bullet trained neural net in Java. I have refactored that a bit to adapt it to my needs, but it really gave me a quick start. I also used the Bagatur v2 net whilst initially evaluating the approach. I also want to thank the author of Leorik, for his helpful forum posts on moving to a neural net eval and thoughtfully and gratiously sharing his experience in engine programming.

For v1.x to v3.x, I have to thank all of the contributors to the Chess Programming Wiki and particularly the authors of; microC; TSCE; the CPW engine; Virutor; Dragon; Tarrasch Toy Engine, and Stockfish.

Also to the developers of Arena and Cutechess.

Thanks to the CCRL for all their test efforts.

Thanks to the author of lichess-bot API as well.
