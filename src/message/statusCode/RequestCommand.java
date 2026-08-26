package src.message.statusCode;

public enum RequestCommand {
    REQUEST_FILE(90, "REQUEST FILE"),
    REQUEST_PUSH(91, "REQUEST PUSH"),
    REQUEST_DELETE(92, "REQUEST DELETE"),
    GET_FILE(100, "GET FILE"),
    PUSH_FILE(200, "PUSH FILE"),
    DELETE_FILE(300, "DELETE FILE"),
    END(900, "END");

    private int command;
    private String method;

    private RequestCommand(int command, String method) {
        this.command = command;
        this.method = method;
    }

    public int getCommand() {
        return command;
    }

    public String getMethod() {
        return method;
    }
}
