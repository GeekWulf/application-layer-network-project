package src;

import java.net.*;
import java.io.*;

public class Server {
    private Socket socket;
    private ServerSocket serverSocket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    private final String serverDirectoryPath = "./testFiles/server/";

    public Server(int portNumber) {
        try {
            serverSocket = new ServerSocket(portNumber);
            System.out.println("Server started");

            while (true) {
                socket = serverSocket.accept();
                System.out.println("Client accepted : " + socket.getInetAddress());

                ServerThread serverThread = new ServerThread(socket, serverDirectoryPath);
                Thread thread = new Thread(serverThread);

                thread.start();
            }

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        Server server = new Server(999);
    }
}