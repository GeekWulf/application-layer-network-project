package src.message;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public interface MessageHeader {
    void setEOFFlag(boolean EOF);
    int getFileNameLenght();
    String getFileName();
    int getCurrentByte();
    boolean getEOFFlag();
    int getPayloadLength();
    void writeOutputStream(DataOutputStream outputStream) throws IOException;
}
