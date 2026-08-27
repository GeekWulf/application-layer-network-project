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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    private String clientName;
    private String clientRole;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    private final String clientPath = "./testFiles/client/";

    public Client(String addr, int portNumber) {
        String userCmd = null;
        Scanner scanner = new Scanner(System.in);

        System.out.print("What is your role [teacher/student]: ");
        clientRole = scanner.nextLine();

        try {
            socket = new Socket(addr, portNumber);
            System.out.println("Connected.");

            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (UnknownHostException e) {
            System.out.println(e);
            return;
        } catch (IOException e) {
            System.out.println(e);
            return;
        }

        String fileName = null;

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
            } else if (userCmd.equals("delete")) {
                System.out.print("Enter file name: ");
                fileName = scanner.nextLine();

                deleteFile(fileName);
            }
        }

        closeConnection();
    }

    public void recieveFile() {
        FileOutputStream fileOutputStream = null;
        Path tempPath = null;
        Path finalPath = null;
        boolean transmissionSuccess = false;

        try {
            Message message = receiveMessage();
            ResponseMessageHeader responseMessageHeader = (ResponseMessageHeader) message.getMessageHeader();

            if (responseMessageHeader.getStatusCode() == ResponseStatus.FAILED.getStatus()) {
                System.out.println("Failed to receive file from server.");
                return;
            }

            String fileName = message.getMessageHeader().getFileName();
            tempPath = Paths.get(clientPath + fileName + ".tmp");
            finalPath = Paths.get(clientPath + fileName);

            fileOutputStream = new FileOutputStream(tempPath.toFile());

            while (true) {
                if (responseMessageHeader.getStatusCode() == ResponseStatus.FAILED.getStatus()) {
                    break;
                }

                byte[] payload = message.getPayload();
                fileOutputStream.write(payload);

                int fileNameLength = message.getMessageHeader().getFileNameLenght();
                int currentByte = message.getMessageHeader().getCurrentByte();

                if (message.getMessageHeader().getEOFFlag() == true) {
                    transmissionSuccess = true;
                    break;
                } else {
                    MessageHeader requestMessageHeader = new RequestMessageHeader(RequestCommand.GET_FILE.getCommand(), fileNameLength, fileName, currentByte, 0);
                    Message requestMessage = new Message(requestMessageHeader, null);

                    sendMessage(requestMessage);
                    message = receiveMessage();
                    responseMessageHeader = (ResponseMessageHeader) message.getMessageHeader();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                if (transmissionSuccess && tempPath != null && finalPath != null) {
                    Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
                } else if (!transmissionSuccess && tempPath != null) {
                    Files.deleteIfExists(tempPath);
                    System.out.println("Failed to receive file from server.");
                }
            } catch (IOException e) {
                e.printStackTrace();
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
            MessageHeader messageHeader = new RequestMessageHeader(RequestCommand.GET_FILE.getCommand(), fileNameLength, fileName, 0, 0);
            Message requestMessage = new Message(messageHeader, null);

            sendMessage(requestMessage);
            recieveFile();
        } else if (statusCode == ResponseStatus.FILE_NOT_FOUND.getStatus()) {
            System.out.println("File not found.");
        }
    }

    public void sendFile(String fileName) {
        Message responseMessage = sendValifyAccessMessage(fileName, RequestCommand.REQUEST_PUSH.getCommand());
        ResponseMessageHeader responseMessageHeader = (ResponseMessageHeader) responseMessage.getMessageHeader();
        int statusCode = responseMessageHeader.getStatusCode();

        if (statusCode == ResponseStatus.NOT_AUTHORIZED.getStatus()) {
            System.out.println("Not authorized.");
            return;
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

                    responseMessageHeader = (ResponseMessageHeader) responseMessage.getMessageHeader();
                    if (responseMessageHeader.getStatusCode() == ResponseStatus.PUSH_FAILED.getStatus()) {
                        fileInputStream.close();
                        throw new IOException("Failed to send file to server.");
                    }
                }

                fileInputStream.close();
            } catch (IOException e) {
                System.out.println("Failed to push file to server.");
                return;
            }
        }

        responseMessage = receiveMessage();
    }

    public void deleteFile(String fileName) {
        Message responseMessage = sendValifyAccessMessage(fileName, RequestCommand.REQUEST_DELETE.getCommand());
        ResponseMessageHeader responseMessageHeader = (ResponseMessageHeader) responseMessage.getMessageHeader();
        int statusCode = responseMessageHeader.getStatusCode();

        if (statusCode == ResponseStatus.AUTHORIZED.getStatus()) {
            MessageHeader requestMessageHeader = new RequestMessageHeader(RequestCommand.DELETE_FILE.getCommand(), fileName.length(), fileName);
            Message requestMessage = new Message(requestMessageHeader, null);

            sendMessage(requestMessage);
            responseMessage = receiveMessage();
        } else if (statusCode == ResponseStatus.FILE_NOT_FOUND.getStatus()) {
            System.out.println("File not found.");
        } else if (statusCode == ResponseStatus.NOT_AUTHORIZED.getStatus()) {
            System.out.println("You have no permission to delete file.");
        }
    }

    public void sendMessage(Message requestMessage) {
        try {
            if (clientRole.equals("teacher")) {
                RequestMessageHeader r = (RequestMessageHeader)requestMessage.getMessageHeader();
                r.setTeacherFlag(true);
            }

            requestMessage.writeOutputStream(dataOutputStream);
            dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Message receiveMessage() {
        Message message = null;
        ResponseMessageHeader messageHeader = null;

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
            System.out.println("Something went wrong.");
            System.out.println(e);
        }

        return message;
    }

    public Message sendValifyAccessMessage(String fileName, int method) {
        byte[] filenameBytes =  fileName.getBytes(StandardCharsets.UTF_8);
        int fileNameLength = filenameBytes.length;

        RequestMessageHeader messageHeader = new RequestMessageHeader(method, fileNameLength, fileName);
        Message requestMessage = new Message(messageHeader, null);

        if (clientRole.equals("teacher")) {
            messageHeader.setTeacherFlag(true);
        }

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
