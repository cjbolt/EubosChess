package eubos.main;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class EubosSocketWrapper extends Thread {
    static final int PORT_NUMBER = 50000;
    private ServerSocket serverSocket;
    private Socket socket;

    private static final String CMD_TERMINATOR = "\r\n";
    private static final String QUIT_CMD = "quit"+CMD_TERMINATOR;

    // to chess for android direction
    private OutputStream outputStreamToChessForAndroid;
    private PrintWriter toChessForAndroid;

    // from chess for android direction
    private InputStream inputStreamFromChessForAndroid;
    private BufferedReader fromChessForAndroid;

    // to eubos engine direction
    private PipedWriter toEubosEngine;

    // from eubos engine direction
    private final ByteArrayOutputStream fromEubosEngine = new ByteArrayOutputStream();

    private Thread eubosThread;

    public String getLatestUciMessages() {
        String latestMessages = uciMessageLog;
        uciMessageLog = "";
        return latestMessages;
    }

    private String uciMessageLog = "";

    public boolean isEngineRunning() {
        return engineRunning;
    }

    boolean engineRunning = false;

    public EubosSocketWrapper() {
        try {
            serverSocket = new ServerSocket(PORT_NUMBER);
        } catch (IOException e) {
            System.err.println("Trouble on port "+ PORT_NUMBER + ": " + e);
        }
    }

    public void run() {
        try {
            socket = serverSocket.accept();
            setupEubosEngine();
            engineCommandHandler engineTxThread = new engineCommandHandler();
            guiCommandHandler engineRxThread = new guiCommandHandler();
            engineTxThread.start();
            engineRxThread.start();
            do {
                // Poll for engine getting closed
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (engineRunning);
            disposeResources();
        } catch (IOException e) {
            System.err.println("Trouble with connection "+ e);
        }
    }

    private class engineCommandHandler extends Thread {
        public void run() {
            try {
                String nextUciCommandFromGui;
                do {
                    if (fromChessForAndroid.ready()) {
                        // Check for comms from GUI
                        nextUciCommandFromGui = fromChessForAndroid.readLine();
                        nextUciCommandFromGui = nextUciCommandFromGui.trim();
                        // remove any double spaces
                        nextUciCommandFromGui = nextUciCommandFromGui.replace("  ", " ") + CMD_TERMINATOR;
                        toEubosEngine.write(nextUciCommandFromGui);
                        if (nextUciCommandFromGui.equals(QUIT_CMD)) {
                            engineRunning = false;
                        }
                        uciMessageLog += nextUciCommandFromGui;
                    }
                } while ( engineRunning );
            } catch (IOException e) {
                System.err.println("Trouble with connection "+ e);
            }
        }
    }

    private class guiCommandHandler extends Thread {
        public void run() {
            String nextUciCommandFromEubos;
            do {
                // Poll for comms from Eubos
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                nextUciCommandFromEubos = fromEubosEngine.toString();
                if (nextUciCommandFromEubos != null && !nextUciCommandFromEubos.isEmpty()) {
                    fromEubosEngine.reset();
                    String eubosUciCommands = addCmdTerminators(nextUciCommandFromEubos);
                    toChessForAndroid.write(eubosUciCommands);
                    toChessForAndroid.flush();
                    uciMessageLog += eubosUciCommands;
                }
            } while ( engineRunning );
        }
    }

    private String addCmdTerminators(String recievedCmd) {
        String parsedCmd = "";
        Scanner scan = new Scanner(recievedCmd);
        while (scan.hasNextLine()) {
            parsedCmd += (scan.nextLine() + CMD_TERMINATOR);
        }
        scan.close();
        return parsedCmd;
    }

    private void setupEubosEngine() {
        try {
            final boolean AUTO_FLUSH = true;

            // Setup the input and output from CFA
            outputStreamToChessForAndroid = socket.getOutputStream();
            inputStreamFromChessForAndroid = socket.getInputStream();
            toChessForAndroid = new PrintWriter(outputStreamToChessForAndroid, AUTO_FLUSH);
            fromChessForAndroid = new BufferedReader(new InputStreamReader(inputStreamFromChessForAndroid));

            // Setup the Comms from this wrapper in and out of Eubos
            System.setOut(new PrintStream(fromEubosEngine));
            toEubosEngine = new PipedWriter();

            // Start engine
            eubosThread = new Thread(new EubosEngineMain(toEubosEngine));
            eubosThread.start();
            engineRunning = true;
        } catch (IOException e) {
            System.err.println("Trouble with connection "+ e);
        }
    }

    private void disposeResources() {
        try {
            // Dispose of  the input and output from CFA
            outputStreamToChessForAndroid.close();
            inputStreamFromChessForAndroid.close();
            toChessForAndroid.close();
            fromChessForAndroid.close();

            // Setup the Comms from this wrapper in and out of Eubos
            System.setOut(System.out);
            toEubosEngine.close();

            // Close the sockets
            serverSocket.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Trouble with connection "+ e);
        }
    }
}
