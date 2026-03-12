package civerooni.com.putman.core.model;

public class ResponseModel {
    private final int statusCode;
    private final String body;
    private final String error;

    public ResponseModel(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
        this.error = null;
    }

    private ResponseModel(String error) {
        this.statusCode = 0;
        this.body = null;
        this.error = error;
    }

    public static ResponseModel error(String message) {
        return new ResponseModel(message);
    }

    public static ResponseModel empty() {
        return new ResponseModel(-1, null);
    }

    public boolean isEmpty() { return statusCode == -1 && body == null && error == null; }

    public boolean isError() { return error != null; }
    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
    public String getError() { return error; }
}
