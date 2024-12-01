import java.io.*;
import java.net.*;
import java.util.LinkedList;

public class Server {

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
                    System.out.println("Server Running ...");
                    System.out.println("Waiting for a new connection");

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
                            User user = connectedUsers.get(i);

                            if( user.getDataReceived().ready() ) {
                                sentMessage = user.getDataReceived().readLine();

                                if( IsServerCommand( sentMessage ) ) {

                                    if( !user.isHasVotted() && Singleton._count > 0 ) {
                                        switch (sentMessage.toLowerCase()) {

                                            case "--yes":
                                                user.setIsAccepted(true);
                                                user.setHasVotted(true);
                                                Singleton._count--;

                                                System.out.println("User '"+user.getName()+"' has been accepted!");
                                                break;

                                            case "--no":
                                                user.setIsAccepted(false);
                                                user.setHasVotted(false);
                                                Singleton._count--;

                                                System.out.println("User '"+user.getName()+"' was not accepted!");
                                                break;
                                            default:
                                                System.out.println("Command not found ...");
                                                break;

                                        }
                                    }
                                    else {
                                        if( Singleton._count == 0 ) {
                                            if(sentMessage.equalsIgnoreCase("sair")) {
                                                User userLeaving = user;
                                                System.out.println(user.getName()+" left the chat");

                                                for (int j = 0; j < connectedUsersCount; j++) {
                                                    User userSentCommand = connectedUsers.get(j);
                                                    if(userSentCommand == user) {
                                                        userSentCommand.getDataSent().writeBytes(user.getName()+" left the chat"+"\n");
                                                    }
                                                    else {
                                                        userLeaving.getDataSent().writeBytes("@#@ENDCLIENT@#@"+"\n");
                                                    }
                                                }

                                                connectedUsers.remove(i);
                                                userLeaving.getConnectionSocket().close();
                                                sendMessageToAll = false;
                                                userRemovedCount = 1;
                                            }
                                        }
                                    }
                                }
                                else {
                                    if( sendMessageToAll ) {
                                        for (int j = 0; j < connectedUsersCount; j++) {
                                            User userToSendMessage = connectedUsers.get(j);

                                            if(userToSendMessage != user) {
                                                try {
                                                    userToSendMessage.getDataSent().writeBytes(user.getName()+": "+sentMessage+"\n");
                                                } catch (IOException e) {
                                                    throw new RuntimeException(e);
                                                }
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
                                        User newUser = new User(Singleton._name, Singleton._first, true);

                                        synchronized ( connectedUsers ) {
                                            connectedUsers.add(newUser);
                                        }

                                        newUser.getDataSent().writeBytes("ok"+"\n");
                                        newUser.getDataSent().writeBytes("Welcome "+newUser.getName()+" :)"+"\n");
                                    }
                                    else {
                                        System.out.println("Voting started");
                                        Singleton._count = connectedUsers.size();

                                        for(User user : connectedUsers) {
                                            user.getDataSent().writeBytes(Singleton._name+" wants to join the chat.\n" +
                                                    "Type --yes to accept or --no to decline"+"\n");
                                        }

                                        while ( Singleton._count > 0 ) {
                                            Thread.sleep(4000);
                                            System.out.println(Singleton._count+" users left to vote ...");
                                        }

                                        System.out.println("Voting finished");

                                        boolean userHasBeenAceppted = true;

                                        for(User user : connectedUsers ) {
                                            if(!user.isAccepted()) {
                                                userHasBeenAceppted = false;
                                            }

                                            user.setHasVotted(true);
                                        }

                                        if(userHasBeenAceppted) {
                                            Singleton._first = pendingUsers.removeFirst();

                                            User newUser = new User(Singleton._name, Singleton._first, true);

                                            newUser.getDataSent().writeBytes("ok"+"\n");
                                            connectedUsers.add(newUser);

                                            for(User user : connectedUsers ) {
                                                if(user == newUser) {
                                                    user.getDataSent().writeBytes("Welcome "+newUser.getName()+" :)"+"\n");
                                                }
                                                else {
                                                    user.getDataSent().writeBytes(newUser.getName() + " joined the chat"+"\n");
                                                }
                                            }

                                        }
                                        else {

                                            User user = new User(null, pendingUsers.getFirst(), true);
                                            user.getDataSent().writeBytes("Sorry, you were not allowed to join the conversation ... :s "+"\n");

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
