package src;
import src.message.Message;
import src.message.MessageHeader;
import src.message.RequestMessageHeader;
import src.message.ResponseMessageHeader;
import src.message.statusCode.RequestCommand;
import src.message.statusCode.ResponseStatus;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

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

            socket = serverSocket.accept();
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            System.out.println("Client accepted");

        } catch (IOException e) {
            System.out.println(e);
        }

        while (true) {
            Message requestMessage = receiveRequestMessage();
            RequestMessageHeader requestMessageHeader = (RequestMessageHeader) requestMessage.getMessageHeader();

            int commandCode = requestMessageHeader.getCommandCode();

            if (commandCode == RequestCommand.REQUEST_FILE.getCommand() || commandCode == RequestCommand.REQUEST_PUSH.getCommand()) {
                String fileName = requestMessageHeader.getFileName();
                byte[] filenameBytes =  fileName.getBytes(StandardCharsets.UTF_8);
                int fileNameLength = filenameBytes.length;

                MessageHeader messageHeader = null;
                Message message = null;

                if (commandCode == RequestCommand.REQUEST_FILE.getCommand()) {
                    if (!checkFileExists(fileName)) {
                        messageHeader = new ResponseMessageHeader(ResponseStatus.FILE_NOT_FOUND.getStatus(), fileNameLength, fileName);
                        message = new Message(messageHeader, null);

                        sendResponseMessage(message);

                        continue;
                    }
                }

                messageHeader = new ResponseMessageHeader(ResponseStatus.AUTHORIZED.getStatus(), fileNameLength, fileName);
                message = new Message(messageHeader, null);

                sendResponseMessage(message);
            } else if (commandCode == RequestCommand.GET_FILE.getCommand()) {
                String fileName = requestMessageHeader.getFileName();
                sendFile(requestMessage);
            } else if (commandCode == RequestCommand.PUSH_FILE.getCommand()) {
                creatFile(requestMessage);
            } else if (commandCode == RequestCommand.END.getCommand()) {
                break;
            }
        }

        closeConnection();
    }

    public void sendFile(Message message) {
        try {
            String fileName = message.getMessageHeader().getFileName();
            String path = serverDirectoryPath + fileName;
            File file = new File(path);
            FileInputStream fileInputStream = new FileInputStream(path);

            byte[] buffer = new byte[4096];

            int byteRead;
            long totalRead = file.length();
            while ((byteRead = fileInputStream.read(buffer)) != -1) {
                int currentByte = message.getMessageHeader().getCurrentByte();

                byte[] payload = Arrays.copyOfRange(buffer,0, byteRead);
                currentByte += byteRead;

                MessageHeader responseMessageHeader = null;
                Message responseMessage = null;

                byte[] fileNameBytes = message.getMessageHeader().getFileName().getBytes(StandardCharsets.UTF_8);
                int fileNameLength = fileNameBytes.length;

                if (currentByte == totalRead) {
                    responseMessageHeader = new ResponseMessageHeader(ResponseStatus.SUCCESS.getStatus(), fileNameLength, fileName, currentByte, byteRead);
                    responseMessageHeader.setEOFFlag(true);
                } else {
                    responseMessageHeader = new ResponseMessageHeader(ResponseStatus.IN_PROGRESS.getStatus(), fileNameLength, fileName, currentByte, byteRead);
                }

                responseMessage = new Message(responseMessageHeader, payload);
                sendResponseMessage(responseMessage);

                if (responseMessage.getMessageHeader().getEOFFlag() != true) {
                    message = receiveRequestMessage();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void creatFile(Message message) {
        String fileName = message.getMessageHeader().getFileName();
        Path path = Paths.get(serverDirectoryPath + fileName);
        FileOutputStream fileOutputStream = null;

        int currentByte = 0;

        try {
            fileOutputStream = new FileOutputStream(path.toFile());

            while (true) {
                byte[] payload = message.getPayload();
                fileOutputStream.write(payload);

                if (message.getMessageHeader().getEOFFlag() == true) {
                    break;
                } else {
                    int fileNameLength = message.getMessageHeader().getFileNameLenght();
                    currentByte = message.getMessageHeader().getCurrentByte();

                    MessageHeader responseMessageHeader = new ResponseMessageHeader(ResponseStatus.IN_PROGRESS.getStatus(), fileNameLength, fileName, currentByte, 0);
                    message = new Message(responseMessageHeader, null);

                    sendResponseMessage(message);
                    message = receiveRequestMessage();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        int fileNameLength = fileNameBytes.length;
        ResponseMessageHeader responseMessageHeader = new ResponseMessageHeader(ResponseStatus.PUSH_SUCCESS.getStatus(), fileNameLength, fileName, currentByte, 0);
        Message responseMessage = new Message(responseMessageHeader, null);
        sendResponseMessage(responseMessage);
    }

    public void sendResponseMessage(Message message) {
        try {
            message.writeOutputStream(dataOutputStream);
            dataOutputStream.flush();

            System.out.println(message.toString());
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public Message receiveRequestMessage() {
        Message message = null;
        MessageHeader messageHeader = null;

        try {
            int commandCode = dataInputStream.readInt();

            int fileNameLength = dataInputStream.readInt();
            byte[] fileNameBytes = new byte[fileNameLength];
            dataInputStream.readFully(fileNameBytes);
            String fileName = new String(fileNameBytes, StandardCharsets.UTF_8);

            int currentByte = dataInputStream.readInt();

            boolean EOFFlag = dataInputStream.readBoolean();

            int payloadLength = dataInputStream.readInt();
            byte[] payload = new byte[payloadLength];
            dataInputStream.readFully(payload);

            messageHeader = new RequestMessageHeader(commandCode, fileNameLength, fileName, currentByte, payloadLength);
            messageHeader.setEOFFlag(EOFFlag);
            message = new Message(messageHeader, payload);

            System.out.println(message.toString());
        } catch (IOException e) {
            System.out.println("Something went wrong");
            System.out.println(e);
        }

        return message;
    }

    public void closeConnection() {
        try {
            System.out.println("Closing connection");

            dataOutputStream.close();
            dataInputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkFileExists(String fileName) {
        Path path = Paths.get(serverDirectoryPath + fileName);

        if (Files.exists(path)) {
            return true;
        }

        return false;
    }

    public static void main(String[] args) {
        Server server = new Server(999);
    }
}