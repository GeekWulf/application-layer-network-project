package src.message;

import src.message.statusCode.RequestCommand;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RequestMessageHeader implements MessageHeader {
    private int commandCode;
    private int fileNameLenght;
    private String fileName;
    private int currentByte;
    private int payloadLength;
    private boolean EOFFlag;
    private boolean teacherFlag;

    public RequestMessageHeader(int commandCode, int fileNameLenght, String fileName, int currentByte, int payloadLength) {
        this.commandCode = commandCode;
        this.fileNameLenght = fileNameLenght;
        this.fileName = fileName;
        this.currentByte = currentByte;
        this.payloadLength = payloadLength;
        this.EOFFlag = false;
        this.teacherFlag = false;
    }

    public RequestMessageHeader(int commandCode, int fileNameLenght, String fileName) {
        this(commandCode, fileNameLenght, fileName, 0, 0);
    }

    public RequestMessageHeader(int commandCode) {
        this(commandCode, 0, "");
    }

    @Override
    public void setEOFFlag(boolean EOFFlag) {
        this.EOFFlag = EOFFlag;
    }

    public void setTeacherFlag(boolean teacherFlag) {
        this.teacherFlag = teacherFlag;
    }

    public int getCommandCode() {
        return commandCode;
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

    public boolean getTeacherFlag() {
        return teacherFlag;
    }

    @Override
    public int getPayloadLength() {
        return payloadLength;
    }

    @Override
    public void writeOutputStream(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(commandCode);
        dataOutputStream.writeInt(fileNameLenght);
        dataOutputStream.write(fileName.getBytes(StandardCharsets.UTF_8));
        dataOutputStream.writeInt(currentByte);
        dataOutputStream.writeBoolean(EOFFlag);
        dataOutputStream.writeBoolean(teacherFlag);
        dataOutputStream.writeInt(payloadLength);
    }

    @Override
    public String toString() {
        String method = "";

        switch (commandCode) {
            case 100 :
                method = RequestCommand.GET_FILE.getMethod();
                break;
            case 200 :
                method = RequestCommand.PUSH_FILE.getMethod();
                break;
            case 300 :
                method = RequestCommand.DELETE_FILE.getMethod();
                break;
            case 90 :
                method = RequestCommand.REQUEST_FILE.getMethod();
                break;
            case 91 :
                method = RequestCommand.REQUEST_PUSH.getMethod();
                break;
            case 92 :
                method = RequestCommand.REQUEST_DELETE.getMethod();
                break;
            case 900 :
                method = RequestCommand.END.getMethod();
                break;
            default :
                break;
        }

        return "[ Request Message ] " + method + "\n"
                + "file name length : " + fileNameLenght + "\n"
                + "file name : " + fileName + "\n"
                + "current byte : " + currentByte + "\n"
                + "payload length : " + payloadLength + "\n"
                + "eof flag : " + EOFFlag + "\n"
                + "teacher flag : " + teacherFlag + "\n";
    }
}
