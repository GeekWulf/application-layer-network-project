package src;
import src.message.RequestMessageHeader;
import src.message.ResponseMessageHeader;
import src.message.Message;
import src.message.MessageHeader;
import src.message.statusCode.RequestCommand;
import src.message.statusCode.ResponseStatus;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    private final String clientPath = "./testFiles/client/";

    public Client(String addr, int portNumber) {
        try {
            socket = new Socket(addr, portNumber);
            System.out.println("Connected");

            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (UnknownHostException e) {
            System.out.println(e);
            return;
        } catch (IOException e) {
            System.out.println(e);
            return;
        }

        String userCmd = null;
        String fileName = null;
        Scanner scanner = new Scanner(System.in);

        while (userCmd == null || !userCmd.equals("exit")) {
            System.out.print("Enter command: ");
            userCmd = scanner.nextLine();

            if (userCmd.equals("get")) {
                System.out.print("Enter file name: ");
                fileName = scanner.nextLine();

                requestFile(fileName);
            } else if (userCmd.equals("put")) {
                System.out.print("Enter file name: ");
                fileName = scanner.nextLine();

                sendFile(fileName);
            }
        }

        closeConnection();
    }

    public void recieveFile() {
        FileOutputStream fileOutputStream = null;

        try {
            Message message = receiveMessage();
            String fileName = message.getMessageHeader().getFileName();
            Path path = Paths.get(clientPath + fileName);

            fileOutputStream = new FileOutputStream(path.toFile());

            while (true) {
                byte[] payload = message.getPayload();
                fileOutputStream.write(payload);

                int fileNameLength = message.getMessageHeader().getFileNameLenght();
                int currentByte = message.getMessageHeader().getCurrentByte();

                if (message.getMessageHeader().getEOFFlag() == true) {
                    break;
                } else {
                    MessageHeader requestMessageHeader = new RequestMessageHeader(RequestCommand.GET_FILE.getCommand(), fileNameLength, fileName, currentByte, 0);
                    Message requestMessage = new Message(requestMessageHeader, null);

                    sendMessage(requestMessage);
                    message = receiveMessage();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void requestFile(String fileName) {
        Message responseMessage = sendValifyAccessMessage(fileName, RequestCommand.REQUEST_FILE.getCommand());
        ResponseMessageHeader responseMessageHeader = (ResponseMessageHeader) responseMessage.getMessageHeader();
        int statusCode = responseMessageHeader.getStatusCode();

        if (statusCode == ResponseStatus.AUTHORIZED.getStatus()) {
            byte[] filenameBytes =  fileName.getBytes(StandardCharsets.UTF_8);
            int fileNameLength = filenameBytes.length;
            MessageHeader messageHeader = new RequestMessageHeader(RequestCommand.GET_FILE.getCommand(), fileNameLength, fileName);
            Message requestMessage = new Message(messageHeader, null);

            sendMessage(requestMessage);

            recieveFile();
        } else if (statusCode == ResponseStatus.FILE_NOT_FOUND.getStatus()) {
            System.out.println("File not found");
        }
    }

    public void sendFile(String fileName) {
        Message responseMessage = sendValifyAccessMessage(fileName, RequestCommand.REQUEST_PUSH.getCommand());
        ResponseMessageHeader responseMessageHeader = (ResponseMessageHeader) responseMessage.getMessageHeader();
        int statusCode = responseMessageHeader.getStatusCode();

        if (statusCode == ResponseStatus.NOT_AUTHORIZED.getStatus()) {
            System.out.println("Not authorized");
        } else  if (statusCode == ResponseStatus.AUTHORIZED.getStatus()) {
            try {
                String path = clientPath + fileName;
                File file = new File(path);
                FileInputStream fileInputStream = new FileInputStream(path);

                byte[] buffer = new byte[4096];

                int byteRead;
                long totalBytes = file.length();
                while ((byteRead = fileInputStream.read(buffer)) != -1) {
                    int currentByte = responseMessage.getMessageHeader().getCurrentByte();

                    byte[] payload = Arrays.copyOfRange(buffer,0, byteRead);
                    currentByte += byteRead;

                    MessageHeader requestMessageHeader = new RequestMessageHeader(RequestCommand.PUSH_FILE.getCommand(), fileName.length(), fileName, currentByte, byteRead);
                    Message requestMessage = new Message(requestMessageHeader, payload);

                    if (currentByte == totalBytes) {
                        requestMessageHeader.setEOFFlag(true);
                    }

                    sendMessage(requestMessage);

                    if (requestMessageHeader.getEOFFlag() != true) {
                        responseMessage = receiveMessage();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        responseMessage = receiveMessage();
    }

    public void sendMessage(Message requestMessage) {
        try {
            requestMessage.writeOutputStream(dataOutputStream);
            dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Message receiveMessage() {
        Message message = null;
        MessageHeader messageHeader = null;

        try {
            int statusCode = dataInputStream.readInt();

            int filenameLength = dataInputStream.readInt();
            byte[] filenameBytes = new byte[filenameLength];
            dataInputStream.readFully(filenameBytes);
            String filename = new String(filenameBytes, StandardCharsets.UTF_8);

            int currentByte = dataInputStream.readInt();

            boolean EOFFlag = dataInputStream.readBoolean();

            int payloadLength = dataInputStream.readInt();
            byte[] payload = new byte[payloadLength];
            dataInputStream.readFully(payload);

            messageHeader = new ResponseMessageHeader(statusCode, filenameLength, filename, currentByte, payloadLength);
            messageHeader.setEOFFlag(EOFFlag);
            message = new Message(messageHeader, payload);

            System.out.println(message.toString());
        } catch (IOException e) {
            System.out.println("Something went wrong");
            System.out.println(e);
        }

        return message;
    }

    public Message sendValifyAccessMessage(String fileName, int method) {
        byte[] filenameBytes =  fileName.getBytes(StandardCharsets.UTF_8);
        int fileNameLength = filenameBytes.length;

        MessageHeader messageHeader = new RequestMessageHeader(method, fileNameLength, fileName);
        Message requestMessage = new Message(messageHeader, null);

        sendMessage(requestMessage);

        Message responseMessage = receiveMessage();

        return responseMessage;
    }

    public void closeConnection() {
        try {
            MessageHeader messageHeader = new RequestMessageHeader(RequestCommand.END.getCommand());
            Message closeConnectionMessage = new Message(messageHeader, null);

            sendMessage(closeConnectionMessage);

            dataOutputStream.close();
            dataInputStream.close();
            socket.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        Client client = new Client("127.0.0.1", 999);
    }
}
