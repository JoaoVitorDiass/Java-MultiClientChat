import java.io.*;
import java.net.*;
import java.util.LinkedList;

public class Server {

    public static final String DEFAULT = "\033[0m";
    public static final String YELLOW = "\033[33m";

    private static boolean IsServerCommand(String command) {
        return command.startsWith("--");
    }

    public static void main(String[] args) throws Exception {

        LinkedList<User> connectedUsers = new LinkedList<>();
        LinkedList<Socket> pendingUsers = new LinkedList<>();

        ServerSocket serverSocket = new ServerSocket(6789);

        Runnable connectionHandler = () -> {

            while(true) {
                try {
                    System.out.println("Waiting for a new connection ...");

                    Socket connectionSocket = serverSocket.accept();

                    System.out.println("New connection has been accepted: "+connectionSocket.getInetAddress().getHostAddress());

                    synchronized (pendingUsers) {
                        pendingUsers.add(connectionSocket);
                        BufferedReader dataReceived = new BufferedReader(
                                new InputStreamReader( pendingUsers.getFirst().getInputStream() )
                        );
                        Singleton._name = dataReceived.readLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        };

        Thread connectionThread = new Thread(connectionHandler);
        connectionThread.start();

            Runnable userHandler = () -> {
                while( true ) {

                    boolean sendMessageToAll  = true;
                    String sentMessage  = null;
                    int connectedUsersCount;
                    int userRemovedCount = 0;

                    synchronized (connectedUsers) {
                        connectedUsersCount = connectedUsers.size();
                    }

                    for (int i = 0; i < connectedUsersCount - userRemovedCount; i++) {
                        try {
                            User userReceived = connectedUsers.get(i);

                            if( userReceived.getDataReceived().ready() ) {
                                sentMessage = userReceived.getDataReceived().readLine();

                                if( IsServerCommand( sentMessage ) ) {

                                    if( !userReceived.isHasVotted() && Singleton._count > 0 ) {

                                        switch (sentMessage.toLowerCase()) {

                                            case "--yes":
                                                userReceived.setIsAccepted(true);
                                                userReceived.setHasVotted(true);
                                                Singleton._count--;

                                                System.out.println("User '"+userReceived.getName()+"' has been accepted!");
                                                break;

                                            case "--no":
                                                userReceived.setIsAccepted(false);
                                                userReceived.setHasVotted(true);
                                                Singleton._count--;

                                                System.out.println("User '"+userReceived.getName()+"' was not accepted!");
                                                break;
                                            default:
                                                userReceived.getDataSent().writeBytes(YELLOW + "Command not found ...\n" + DEFAULT);
                                                break;

                                        }
                                    }
                                    else {
                                        userReceived.getDataSent().writeBytes(YELLOW + "Command not found ...\n" + DEFAULT);
                                    }
                                    sendMessageToAll = false;
                                }

                                if( Singleton._count == 0 ) {
                                    if(sentMessage.equalsIgnoreCase("sair")) {

                                        User userLeaving = userReceived;

                                        System.out.println(userLeaving.getName()+" left the chat\n");

                                        for (int j = 0; j < connectedUsersCount; j++) {
                                            User userSentCommand = connectedUsers.get(j);

                                            if(userSentCommand != userLeaving) {
                                                userSentCommand.getDataSent().writeBytes(YELLOW + userLeaving.getName()+" left the chat\n" + DEFAULT);
                                            }
                                            else  {
                                                userLeaving.getDataSent().writeBytes(YELLOW + "You left the chat\n" + DEFAULT);
                                                userLeaving.getDataSent().writeBytes("@#@ENDCLIENT@#@\n");
                                            }
                                        }

                                        connectedUsers.remove(i);
                                        userLeaving.getConnectionSocket().close();
                                        sendMessageToAll = false;
                                        userRemovedCount = 1;
                                    }
                                }

                                if( sendMessageToAll ) {
                                    for (int j = 0; j < connectedUsersCount; j++) {
                                        User userToSendMessage = connectedUsers.get(j);

                                        if(userToSendMessage != userReceived) {
                                            try {
                                                userToSendMessage.getDataSent().writeBytes(userReceived.getName()+": "+sentMessage+"\n");
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                    }
                                }

                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };

            Thread userThread = new Thread(userHandler);
            userThread.start();

            Runnable managerHandler = () -> {
                while(true) {
                    try {
                        synchronized ( pendingUsers ) {

                            if( !pendingUsers.isEmpty() ) {
                                if( pendingUsers.getFirst().isConnected() ) {
                                    if( connectedUsers.isEmpty() ) {
                                        System.out.println("first user logged in");
                                        Singleton._first = pendingUsers.getFirst();
                                        pendingUsers.removeFirst();
                                        User newUser = new User(Singleton._first, true);
                                        newUser.setName(Singleton._name);

                                        synchronized ( connectedUsers ) {
                                            connectedUsers.add(newUser);
                                        }

                                        newUser.getDataSent().writeBytes("OK\n");
                                        newUser.getDataSent().writeBytes(YELLOW + "Welcome "+newUser.getName()+" :)\n" + DEFAULT);
                                    }
                                    else {
                                        System.out.println("Voting started");
                                        Singleton._count = connectedUsers.size();

                                        for(User user1 : connectedUsers) {
                                            user1.getDataSent().writeBytes(YELLOW + Singleton._name+" wants to join the chat.\n" +
                                                    "Type --yes to accept or --no to decline\n" + DEFAULT);
                                        }

                                        while ( Singleton._count > 0 ) {
                                            Thread.sleep(4000);
                                            System.out.println(Singleton._count+" users left to vote ...");
                                        }

                                        System.out.println("Voting finished");

                                        boolean userHasBeenAceppted = true;

                                        for(User user2 : connectedUsers ) {
                                            if(!user2.isAccepted()) {
                                                userHasBeenAceppted = false;
                                            }

                                            user2.setHasVotted(false);
                                        }

                                        if(userHasBeenAceppted) {
                                            Singleton._first = pendingUsers.removeFirst();

                                            User newUser = new User( Singleton._first, true);
                                            newUser.setName(Singleton._name);

                                            newUser.getDataSent().writeBytes("OK\n");
                                            connectedUsers.add(newUser);

                                            for(User user3 : connectedUsers ) {
                                                if(user3 == newUser) {
                                                    user3.getDataSent().writeBytes(YELLOW + "you are aproved! Welcome "+newUser.getName()+" :)\n" + DEFAULT);
                                                }
                                                else {
                                                    user3.getDataSent().writeBytes(YELLOW + newUser.getName() + " joined the chat\n" + DEFAULT);
                                                }
                                            }
                                        }
                                        else {

                                            User user = new User( pendingUsers.getFirst(), true );
                                            user.getDataSent().writeBytes(YELLOW + "Sorry, you were not allowed to join the conversation ... :s\n" + DEFAULT);

                                            user = null;

                                            pendingUsers.getFirst().close();
                                            pendingUsers.removeFirst();
                                        }
                                    }
                                }
                                else {
                                    pendingUsers.getFirst().close();
                                    pendingUsers.removeFirst();
                                }
                            }
                        }
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            };

            Thread managerThread = new Thread(managerHandler);
            managerThread.start();
    }
}
