package src.message;

import java.io.DataOutputStream;
import java.io.IOException;

public final class Message {
    private MessageHeader messageHeader;
    private byte[] payload;

    public Message(MessageHeader messageHeader, byte[] payload) {
        this.messageHeader = messageHeader;
        this.payload = payload;
    }

    public MessageHeader getMessageHeader() {
        return messageHeader;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void writeOutputStream(DataOutputStream dataOutputStream) throws IOException {
        messageHeader.writeOutputStream(dataOutputStream);
        if (payload != null) {
            dataOutputStream.write(payload);
        }
    }

    @Override
    public String toString() {
        return messageHeader.toString();
    }
}
