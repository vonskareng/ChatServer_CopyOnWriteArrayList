// Rebecka Skareng
// Server, multi-threaded, accepting several simultaneous clients.

package paradis.assignment4;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

class ChatServer implements Runnable {
    private final int PORT = 8000;
    private final int MAX_CLIENTS = 2;
    private final Executor executor = Executors.newFixedThreadPool(MAX_CLIENTS + 1);
    //Egen copyonwritearraylist
    private final CopyOnWriteArrayList<ClientSession> clients = new CopyOnWriteArrayList<>();
    //blockingqueue för att se till så det senaste mottagna meddelandet skickas först
    private final BlockingQueue<String> messages = new LinkedBlockingQueue<String>();

    private ChatServer() {
        executor.execute(new MessageSender());
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            SocketAddress serverSocketAddress = serverSocket.getLocalSocketAddress();
            System.out.println("Server started.");
            System.out.println("Listening (" + serverSocketAddress + ").");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientSession session = new ClientSession(clientSocket);
                executor.execute(session);
                clients.add(session);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    //Klass som hanterar logiken att lyssna på klienters skickade meddelanden och initiering
    class ClientSession implements Runnable {

        private Socket clientSocket;
        private PrintWriter socketWriter;
        private BufferedReader socketReader;
        private String clientName = "";

        public ClientSession(Socket socket) {
            clientSocket = socket;
            try {
                socketWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            SocketAddress remoteSocketAddress = clientSocket.getRemoteSocketAddress();
            SocketAddress localSocketAddress = clientSocket.getLocalSocketAddress();
            System.out.println("Accepted client " + remoteSocketAddress + " (" + localSocketAddress + ").");

            try {
                String threadInfo = " (" + Thread.currentThread().getName() + ").";
                String inputLine = socketReader.readLine();
                System.out.println("Received: \"" + inputLine + "\" from " + remoteSocketAddress + threadInfo);

                // First message is client name.
                clientName = inputLine;

                while (inputLine != null) {
                    messages.put(clientName + ": " + inputLine);
                    System.out.println(
                            "Sent: \"" + inputLine + "\" to " + clientName + " " + remoteSocketAddress + threadInfo);
                    inputLine = socketReader.readLine();
                    System.out.println("Received: \"" + inputLine + "\" from " + clientName + " " + remoteSocketAddress
                            + threadInfo);

                }
                System.out.println("Closing connection " + remoteSocketAddress + " (" + localSocketAddress + ").");
                clients.remove(this);
            } catch (Exception exception) {
                System.out.println(exception);
            } finally {
                try {
                    if (socketWriter != null)
                        socketWriter.close();
                    if (socketReader != null)
                        socketReader.close();
                    if (clientSocket != null)
                        clientSocket.close();
                } catch (Exception exception) {
                    System.out.println(exception);
                }
            }
        }

    }

    //Kör på en egen tråd och hämtar medelanden från kön och skickar parallelt till alla klienter
    //genom en parallel ström som fås av datastrukturen som håller alla referenser till klienterna.
    class MessageSender implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    String message = messages.take();
                    Stream<ClientSession> socketStream = clients.stream().parallel();
                    socketStream.forEach(session -> {
                        session.socketWriter.println(message);
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void main(String[] args) {
        Thread thread = new Thread(new ChatServer());
        thread.start();

    }

}
