package src;

import src.message.Message;
import src.message.MessageHeader;
import src.message.RequestMessageHeader;
import src.message.ResponseMessageHeader;
import src.message.statusCode.RequestCommand;
import src.message.statusCode.ResponseStatus;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class ServerThread implements Runnable {
    private Socket socket;
    private String serverDirectoryPath;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public ServerThread(Socket socket, String serverDirectoryPath) {
        this.socket = socket;
        this.serverDirectoryPath = serverDirectoryPath;

        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            listenToClient();
        } finally {
            closeConnection();
        }
    }

    public void listenToClient() {
        while (true) {
            Message requestMessage = receiveRequestMessage();
            RequestMessageHeader requestMessageHeader = (RequestMessageHeader) requestMessage.getMessageHeader();
            int commandCode = requestMessageHeader.getCommandCode();

            if (commandCode == RequestCommand.REQUEST_FILE.getCommand() || commandCode == RequestCommand.REQUEST_PUSH.getCommand() || commandCode == RequestCommand.REQUEST_DELETE.getCommand()) {
                String fileName = requestMessageHeader.getFileName();
                byte[] filenameBytes = fileName.getBytes(StandardCharsets.UTF_8);
                int fileNameLength = filenameBytes.length;

                MessageHeader messageHeader = null;
                Message message = null;
                int responseStatusCode = 0;

                if (commandCode == RequestCommand.REQUEST_FILE.getCommand()) {
                    if (!checkFileExists(fileName)) {
                        responseStatusCode = ResponseStatus.FILE_NOT_FOUND.getStatus();
                    } else {
                        responseStatusCode = ResponseStatus.AUTHORIZED.getStatus();
                    }
                } else if (commandCode == RequestCommand.REQUEST_PUSH.getCommand()) {
                    responseStatusCode = ResponseStatus.AUTHORIZED.getStatus();
                } else if (commandCode == RequestCommand.REQUEST_DELETE.getCommand()) {
                    if (requestMessageHeader.getTeacherFlag() == true) {
                        if (checkFileExists(fileName)) {
                            responseStatusCode =  ResponseStatus.AUTHORIZED.getStatus();
                        } else {
                            responseStatusCode = ResponseStatus.FILE_NOT_FOUND.getStatus();
                        }
                    } else {
                        responseStatusCode = ResponseStatus.NOT_AUTHORIZED.getStatus();
                    }
                }

                messageHeader = new ResponseMessageHeader(responseStatusCode, fileNameLength, fileName);
                message = new Message(messageHeader, null);
                sendResponseMessage(message);
            } else if (commandCode == RequestCommand.GET_FILE.getCommand()) {
                sendFile(requestMessage);
            } else if (commandCode == RequestCommand.PUSH_FILE.getCommand()) {
                recieveFile(requestMessage);
            } else if (commandCode == RequestCommand.DELETE_FILE.getCommand()) {
                String fileName = requestMessageHeader.getFileName();
                deleteFile(fileName);
            } else if (commandCode == RequestCommand.END.getCommand()) {
                break;
            }
        }
    }

    public void sendFile(Message message) {
        String fileName = message.getMessageHeader().getFileName();
        String path = serverDirectoryPath + fileName;
        int fileNameLength = message.getMessageHeader().getFileNameLenght();

        try {
            File file = new File(path);
            FileInputStream fileInputStream = new FileInputStream(path);

            byte[] buffer = new byte[4096];

            int byteRead;
            long totalRead = file.length();
            while ((byteRead = fileInputStream.read(buffer)) != -1) {
                int currentByte = message.getMessageHeader().getCurrentByte();

//                if (currentByte > 8000) {
//                    throw new IOException("Simulate read file fail.");
//                }

                byte[] payload = Arrays.copyOfRange(buffer, 0, byteRead);
                currentByte += byteRead;

                MessageHeader responseMessageHeader = null;
                Message responseMessage = null;

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
            MessageHeader errorMessageHeader = new ResponseMessageHeader(ResponseStatus.FAILED.getStatus(), fileNameLength, fileName);
            Message errorMessage = new Message(errorMessageHeader, null);
            errorMessageHeader.setEOFFlag(true);
            sendResponseMessage(errorMessage);
        }
    }

    public void recieveFile(Message message) {
        String fileName = message.getMessageHeader().getFileName();
        int fileNameLength = message.getMessageHeader().getFileNameLenght();

//        String fakePath = "Z:/" + serverDirectoryPath + fileName;
//        Path tempPath = Paths.get(fakePath + fileName + ".tmp");
//        Path finalPath = Paths.get(fakePath + fileName);

        Path tempPath = Paths.get(serverDirectoryPath + fileName + ".tmp");
        Path finalPath = Paths.get(serverDirectoryPath + fileName);

        FileOutputStream fileOutputStream = null;

        int currentByte = 0;
        boolean transmissionSuccess = false;

        try {
            fileOutputStream = new FileOutputStream(tempPath.toFile());

            while (true) {
                byte[] payload = message.getPayload();
                fileOutputStream.write(payload);

                currentByte = message.getMessageHeader().getCurrentByte();

                if (message.getMessageHeader().getEOFFlag() == true) {
                    transmissionSuccess = true;
                    break;
                } else {
                    MessageHeader responseMessageHeader = new ResponseMessageHeader(ResponseStatus.IN_PROGRESS.getStatus(), fileNameLength, fileName, currentByte, 0);
                    message = new Message(responseMessageHeader, null);

                    sendResponseMessage(message);
                    message = receiveRequestMessage();
                }
            }
        } catch (IOException e) {
            // e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                if (transmissionSuccess) {
                    Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.deleteIfExists(tempPath);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int responseStatusCode;
        if  (transmissionSuccess) {
            responseStatusCode = ResponseStatus.SUCCESS.getStatus();
        } else {
            responseStatusCode = ResponseStatus.PUSH_FAILED.getStatus();
        }

        ResponseMessageHeader responseMessageHeader = new ResponseMessageHeader(responseStatusCode, fileNameLength, fileName, currentByte, 0);
        Message responseMessage = new Message(responseMessageHeader, null);
        sendResponseMessage(responseMessage);
    }

    public void deleteFile(String fileName) {
        Path path = Paths.get(serverDirectoryPath + fileName);

        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MessageHeader reponseMessageHeader = new ResponseMessageHeader(ResponseStatus.DELETE_SUCCESS.getStatus(), fileName.length(), fileName);
        Message responseMessage = new Message(reponseMessageHeader, null);
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
        RequestMessageHeader messageHeader = null;

        try {
            int commandCode = dataInputStream.readInt();

            int fileNameLength = dataInputStream.readInt();
            byte[] fileNameBytes = new byte[fileNameLength];
            dataInputStream.readFully(fileNameBytes);
            String fileName = new String(fileNameBytes, StandardCharsets.UTF_8);

            int currentByte = dataInputStream.readInt();

            boolean EOFFlag = dataInputStream.readBoolean();
            boolean teacherFlag = dataInputStream.readBoolean();

            int payloadLength = dataInputStream.readInt();
            byte[] payload = new byte[payloadLength];
            dataInputStream.readFully(payload);

            messageHeader = new RequestMessageHeader(commandCode, fileNameLength, fileName, currentByte, payloadLength);
            messageHeader.setEOFFlag(EOFFlag);
            messageHeader.setTeacherFlag(teacherFlag);
            message = new Message(messageHeader, payload);

            System.out.println(message.toString());
        } catch (IOException e) {
            System.out.println("Something went wrong.");
            System.out.println(e);
        }

        return message;
    }

    public void closeConnection() {
        try {
            System.out.println("Client disconnected.");

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
}