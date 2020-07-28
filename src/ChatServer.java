import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * [Add your documentation here]
 *
 * @author Austin Barrow
 * @author Jacob Sandefur
 * @version 4/27/2020
 */
final class ChatServer {
    private static int uniqueId = 0;
    private final List<ClientThread> clients = new ArrayList<>();
    private final int port;
    private static final Object GATEKEEPER = new Object();
    private final String badWords;
    private final ChatFilter filter;


    private ChatServer(int port, String badWords) throws IOException {
        this.port = port;
        this.badWords = badWords;

        filter = new ChatFilter( badWords );
    }

    /*
     * This is what starts the ChatServer.
     * Right now it just creates the socketServer and adds a new ClientThread to a list to be handled
     */
    private void start() {
        System.out.println( "These are the banned words for this server:" );
        for ( String badWord : filter.badWords ) {
            System.out.println( badWord );
        }

        System.out.println( "\nThe Server has now been started, and is awaiting clients!\n");

        System.out.println( "Chat Log" );
        System.out.println( "------------------------------------------------" );

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while ( true ) {
                Socket socket = serverSocket.accept();
                Runnable r = new ClientThread(socket, uniqueId++);
                Thread t = new Thread(r);

                boolean isConnected = true;
                try {
                    for ( ClientThread clientThread : clients ) {
                        if ( ((ClientThread) r).username.equals( clientThread.username ) ) {
                            ((ClientThread) r).writeMessage( "Username already taken. Restart and choose " +
                                    "a different one.\n" );
                            socket.close();
                            isConnected = false;
                        }
                    }
                } catch (IOException e ) {
                    e.printStackTrace();
                }

                if ( isConnected ) {
                    clients.add((ClientThread) r);
                    t.start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     *  > java ChatServer
     *  > java ChatServer portNumber
     *  If the port number is not specified 1500 is used
     */
    public static void main(String[] args) throws IOException {
        int port;
        String fileName = null;
        if ( args.length == 0 ) {
            port = 1500;
            fileName = "badwords.txt";
        } else {
            try {
                port = Integer.parseInt( args[0] );
            } catch ( NumberFormatException e ) {
                port = 1500;
                fileName = args[0];
            }

            if ( fileName == null ) {
                fileName = args[1];
            }
        }


        ChatServer server = new ChatServer( port, fileName );
        //ChatServer server = new ChatServer(1500);
        //while( true ) {
        server.start();
        //}
    }

    private void broadcast( String message ) throws IOException {

        message = filter.filter( message );
        String messageLog = "hi";

        for ( ClientThread clientThread : clients ) {
            synchronized ( GATEKEEPER ) {
                Date date = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat( "HH:mm:ss" );
                String stringDate = formatter.format( date ) + ": ";
                clientThread.writeMessage( stringDate + message + "\n" );
                messageLog = stringDate + message;

            }

        }

        System.out.println( messageLog );

    }

    private void remove( int id ) {
        ClientThread clientThreadToRemove = null;

        synchronized ( GATEKEEPER ) {
            clients.removeIf(clientThread -> clientThread.id == id);
        }

//        for( ClientThread clientThread : clients ) {
//            if( clientThread.id == id ) {
//                clients.remove( clientThread );
//            }
//        }
    }


    /**
     * This is a private class inside of the ChatServer
     * A new thread will be created to run this every time a new client connects.
     *
     * @author Austin Barrow
     * @author Jacob Sandefur
     * @version 4/27/2020
     */
    private final class ClientThread implements Runnable {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;
        boolean isExit = false;

        private ClientThread(Socket socket, int id) {
            this.id = id;
            this.socket = socket;
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        /*
         * This is what the client thread actually runs.
         */
        @Override
        public void run() {
            // Read the username sent to you by client
            try {
                cm = (ChatMessage) sInput.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println(username + ": Ping");

            // Send message back to the client
            try {
                sOutput.writeObject("We have received your username! Welcome to the chat.\n");
                //sOutput.writeObject( username + " has joined.\n");
                broadcast( username + " has joined." );
            } catch (IOException e) {
                e.printStackTrace();
            }

            while ( !isExit ) {
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }

                if ( cm.getType() == 1 ) {
                    try {
                        broadcast( username + " has logged out" );
                        remove( this.id );
                        close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if ( cm.getType() == 0) {
                    try {
                        broadcast( username + ": " + cm.getMessage() );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if ( cm.getType() == 2 ) {
                    try {
                        directMessage( cm.getMessage(), cm.getRecipient() );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if ( cm.getType() == 3 ) {
                    try {
                        list();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void directMessage(String message, String recipient) throws IOException {

            if ( username.equals( recipient ) ) {
                synchronized ( GATEKEEPER ) {
                    writeMessage( "You can not direct message yourself!\n" );
                }
            } else {
                message = filter.filter( message );

                int count = 0;
                String message1 = null;
                String message2 = null;
                for ( ClientThread clientThread : clients) {
                    if ( clientThread.username.equals( recipient )) {
                        synchronized ( GATEKEEPER ) {
                            count = 1;
                            Date date = new Date();
                            SimpleDateFormat formatter = new SimpleDateFormat( "HH:mm:ss" );
                            String stringDate = formatter.format( date ) + ": ";

                            message1 = stringDate + "[Private Msg from " + username + "] " + message + "\n";
                            message1 = filter.filter( message1 );
                            clientThread.writeMessage( message1 );

                            message2 = stringDate + "[Private Msg to " + clientThread.username + "] " + message + "\n";
                            writeMessage( message2 );

                            message = stringDate + "[Private Msg from " + username + " to " + clientThread.username +
                                    "] " + message + "\n";
                        }
                    }
                }

                if ( count == 0 ) {
                    synchronized ( GATEKEEPER ) {
                        writeMessage( "There is no user online with the desired username.\n" );
                    }
                }

                System.out.println( message );
            }
        }

        public void list() throws IOException {
            writeMessage( "List of Users:\n");
            for ( ClientThread clientThread: clients) {
                if ( !username.equals( clientThread.username ) ) {
                    synchronized ( GATEKEEPER ) {
                        writeMessage( clientThread.username + "\n" );
                    }
                }
            }
        }

        private boolean writeMessage( String message ) throws IOException {
            if ( socket.isConnected() ) {
                sOutput.writeObject( message );
                return true;
            } else {
                return false;
            }
        }

        private void close( ) throws IOException {
            sOutput.close();
            sInput.close();
            socket.close();
            isExit = true;
        }
    }
}