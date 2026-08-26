package src.message.statusCode;

public enum ResponseStatus {
    IN_PROGRESS(101, "IN PROGRESS"),
    SUCCESS(102, "SUCCESS"),
    FAILED(103, "FAILED"),
    FILE_NOT_FOUND(104, "FILE NOT FOUND"),

    PUSH_SUCCESS(201, "PUSH SUCCESS"),
    PUSH_FAILED(202, "PUSH FAILED"),

    DELETE_SUCCESS(301, "DELETE SUCCESS"),

    AUTHORIZED(500, "AUTHORIZED"),
    NOT_AUTHORIZED(501, "NOT AUTHORIZED");

    private int status;
    private String pharse;

    private ResponseStatus(int status, String pharse) {
        this.status = status;
        this.pharse = pharse;
    }

    public int getStatus() {
        return status;
    }

    public String getPharse() {
        return pharse;
    }
}
