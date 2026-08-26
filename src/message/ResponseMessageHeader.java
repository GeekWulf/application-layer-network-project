package src.message;

import src.message.statusCode.RequestCommand;
import src.message.statusCode.ResponseStatus;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ResponseMessageHeader implements MessageHeader {
    private int statusCode;
    private int fileNameLenght;
    private String fileName;
    private int currentByte;
    private boolean EOFFlag;
    private int payloadLength;

    public ResponseMessageHeader(int statusCode, int fileNameLenght, String fileName, int currentByte, int payloadLength) {
        this.statusCode = statusCode;
        this.fileNameLenght = fileNameLenght;
        this.fileName = fileName;
        this.currentByte = currentByte;
        this.payloadLength = payloadLength;
        this.EOFFlag = false;
    }

    public ResponseMessageHeader(int statusCode, int fileNameLenght, String fileName) {
        this(statusCode, fileNameLenght, fileName, 0, 0);
    }

    @Override
    public void setEOFFlag(boolean EOF) {
        this.EOFFlag = EOF;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public int getFileNameLenght() {
        return fileNameLenght;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public int getCurrentByte() {
        return currentByte;
    }

    @Override
    public boolean getEOFFlag() {
        return EOFFlag;
    }

    @Override
    public int getPayloadLength() {
        return payloadLength;
    }

    @Override
    public void writeOutputStream(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(statusCode);
        dataOutputStream.writeInt(fileNameLenght);
        dataOutputStream.write(fileName.getBytes(StandardCharsets.UTF_8));
        dataOutputStream.writeInt(currentByte);
        dataOutputStream.writeBoolean(EOFFlag);
        dataOutputStream.writeInt(payloadLength);
    }

    @Override
    public String toString() {
        String phrase = "";

        switch (statusCode) {
            case 101 :
                phrase = ResponseStatus.IN_PROGRESS.getPharse();
                break;
            case 102 :
                phrase = ResponseStatus.SUCCESS.getPharse();
                break;
            case 103 :
                phrase = ResponseStatus.FAILED.getPharse();
                break;
            case 104 :
                phrase = ResponseStatus.FILE_NOT_FOUND.getPharse();
                break;
            case 201 :
                phrase = ResponseStatus.PUSH_SUCCESS.getPharse();
                break;
            case 202 :
                phrase = ResponseStatus.PUSH_FAILED.getPharse();
                break;
            case 301:
                phrase = ResponseStatus.DELETE_SUCCESS.getPharse();
                break;
            case 500 :
                phrase = ResponseStatus.AUTHORIZED.getPharse();
                break;
            case 501 :
                phrase = ResponseStatus.NOT_AUTHORIZED.getPharse();
                break;
            default :
                break;
        }

        return "[ Response Message ] " + phrase + " " + statusCode + "\n"
                + "file name lenght : " + fileNameLenght + "\n"
                + "file name : " + fileName + "\n"
                + "current byte : " + currentByte + "\n"
                + "payload lenght : " + payloadLength + "\n"
                + "eof flag : " + EOFFlag + "\n";
    }
}
