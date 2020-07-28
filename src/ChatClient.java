import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

/**
 *
 * [Add your documentation here]
 *
 * @author Austin Barrow
 * @author Jacob Sandefur
 * @version 4/27/2020
 */
final class ChatClient {
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;

    private final String server;
    private final String username;
    private final int port;

    private ChatClient(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    /*
     * This starts the Chat Client
     */
    private boolean start() {
        // Create a socket
        try {
            socket = new Socket(server, port);
        } catch (IOException e) {
            System.out.println( "Don't try to connect before the server starts!");
            return false;
            //e.printStackTrace();
        }

        // Create your input and output streams
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // This thread will listen from the server for incoming messages
        Runnable r = new ListenFromServer();
        Thread t = new Thread(r);
        t.start();

        // After starting, send the clients username to the server.
        try {
            sOutput.writeObject(username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }


    /*
     * This method is used to send a ChatMessage Objects to the server
     */
    private void sendMessage(ChatMessage msg) throws IOException {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            sOutput.close();
            sInput.close();
            e.printStackTrace();
        }
        //sOutput.flush();
    }


    /*
     * To start the Client use one of the following command
     * > java ChatClient
     * > java ChatClient username
     * > java ChatClient username portNumber
     * > java ChatClient username portNumber serverAddress
     *
     * If the portNumber is not specified 1500 should be used
     * If the serverAddress is not specified "localHost" should be used
     * If the username is not specified "Anonymous" should be used
     */
    public static void main(String[] args) throws IOException {
        // Get proper arguments and override defaults

        boolean isExit = false;
        Scanner scan = new Scanner( System.in );

        String username;
        int port;
        String serverAddress;

        switch( args.length ) {
            case 3:
                username = args[0];
                port = Integer.parseInt( args[1] );
                serverAddress = args[2];
                break;
            case 2:
                username = args[0];
                port = Integer.parseInt( args[1] );
                serverAddress = "localhost";
                break;
            case 1:
                username = args[0];
                port = 1500;
                serverAddress = "localhost";
                break;
            default:
                username = "Anonymous";
                port = 1500;
                serverAddress = "localhost";
        }

        String hostname;

        // Create your client and start it
        ChatClient client = new ChatClient( serverAddress, port, username );
        //ChatClient client = new ChatClient("localhost", 1500, "CS 180 Student");
        boolean serverStart = client.start();

        // Send an empty message to the server
        if ( !serverStart || !client.socket.isConnected() ) {
            System.out.print("");
            isExit = true;
        } else {
            try {
                client.sendMessage(new ChatMessage( "just joined", 0, null));
            } catch ( SocketException ignored) {
                isExit = true;
            }
        }
        //client.sOutput.flush();
        //client.sendMessage( new ChatMessage( "yeet", 0 ));

        while ( !isExit ) {
            String message = scan.nextLine();

            if ( message.toLowerCase().contains( "/logout" ) ) {
                client.sendMessage(new ChatMessage(message, 1, null));
                try {
                    client.sOutput.close();
                    client.sInput.close();
                    isExit = true;
                    System.out.println("You have been successfully logged out.");
                    //client.socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if ( message.contains( "/msg" ) ) {
                String[] msg = message.split(" ");
                String privateMessage = "";
                for ( int i = 2; i < msg.length; i++ ) {
                    privateMessage = privateMessage + msg[i] + " ";
                }
                client.sendMessage( new ChatMessage( privateMessage , 2, msg[1] ));
            } else if ( message.contains( "/list" ) ) {
                client.sendMessage( new ChatMessage( null, 3, null ));
            } else {
                client.sendMessage( new ChatMessage( message, 0 , null) );
            }
        }

        //BufferedReader userInputReader = new BufferedReader( new InputStreamReader( client.socket.getInputStream() ));
        //String message = userInputReader.readLine();
        //client.sendMessage( new ChatMessage( message, 0));

    }


    /**
     * This is a private class inside of the ChatClient
     * It will be responsible for listening for messages from the ChatServer.
     * ie: When other clients send messages, the server will relay it to the client.
     *
     * @author Austin Barrow
     * @author Jacob Sandefur
     * @version 4/27/2020
     */
    private final class ListenFromServer implements Runnable {
        public void run() {
            try {
                while ( true ) {
                    String msg = (String) sInput.readObject();
                    System.out.print(msg);
                }
            } catch (IOException | ClassNotFoundException e) {
                // e.printStackTrace();
            }
        }
    }
}