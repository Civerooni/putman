package civerooni.com.putman.core.model;

import java.util.ArrayList;
import java.util.List;

public class RequestModel {
    private HttpMethod method = HttpMethod.GET;
    private String url = "";
    private List<HeaderEntry> headers = new ArrayList<>();
    private List<HeaderEntry> params = new ArrayList<>();
    private String body = "";

    public HttpMethod getMethod() { return method; }
    public void setMethod(HttpMethod method) { this.method = method; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public List<HeaderEntry> getHeaders() { return headers; }
    public List<HeaderEntry> getParams() { return params; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
