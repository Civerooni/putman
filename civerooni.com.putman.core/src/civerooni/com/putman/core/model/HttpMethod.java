package civerooni.com.putman.core.model;

public enum HttpMethod {
    GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS;

    public static String[] names() {
        HttpMethod[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name();
        }
        return names;
    }
}
