import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client3 {

    public static void main(String[] args) throws Exception {
        String username = null;
        AtomicBoolean out = new AtomicBoolean(false);

        BufferedReader userBuffer = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("enter your name: ");
        username = userBuffer.readLine().trim();

        Socket clientSocket = new Socket("localhost", 6789);

        DataOutputStream dataSent = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader dataReceived = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        dataSent.writeBytes(username+"\n");

        Runnable sendMessageHandler = () -> {
            while( !out.get() ) {
                String message = null;

                try {
                    message = userBuffer.readLine();
                    dataSent.writeBytes(message+"\n");

                    if( message.equalsIgnoreCase("--quit")) {
                        out.set(true);
                    }
                }
                catch (IOException e) {
                    if (!out.get()) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        Runnable messagesReceivedHandler = () -> {
            while(!out.get()) {
                String serverMessage = null;

                try {
                    serverMessage = dataReceived.readLine();

                    if(serverMessage.contains("@#@ENDCLIENT@#@")) {
                        System.out.println("you left the chat ...");
                    }
                    else {
                        if(serverMessage != null) {
                            System.out.println(serverMessage);
                        }
                    }
                }
                catch (IOException e) {
                    if (!out.get())  {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        String serverMessage = null;
        try {
            System.out.println("Waiting to be accepted ...");
            serverMessage = dataReceived.readLine();

            System.out.println(serverMessage);

            if( serverMessage.equalsIgnoreCase("ok")) {
                Thread threadReceive = new Thread(messagesReceivedHandler);
                Thread threadSent = new Thread(sendMessageHandler);

                System.out.println("you are aproved! xD");

                threadReceive.start();
                threadSent.start();
            }
            else {
                System.out.println("you are recused ... :c");
                clientSocket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}