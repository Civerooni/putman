package civerooni.com.putman.core.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PutmanLogger {

    // JUL level mapping:
    //   trace → FINEST
    //   debug → FINE
    //   info  → INFO
    //   warn  → WARNING
    //   error → SEVERE

    private final Logger delegate;

    private PutmanLogger(Logger delegate) {
        this.delegate = delegate;
    }

    public static PutmanLogger getLogger(Class<?> clazz) {
        return new PutmanLogger(Logger.getLogger(clazz.getName()));
    }

    public void trace(String msg) { delegate.log(Level.FINEST, msg); }
    public void debug(String msg) { delegate.log(Level.FINE, msg); }
    public void info(String msg)  { delegate.log(Level.INFO, msg); }
    public void warn(String msg)  { delegate.log(Level.WARNING, msg); }
    public void error(String msg) { delegate.log(Level.SEVERE, msg); }
    public void error(String msg, Throwable t) { delegate.log(Level.SEVERE, msg, t); }
}
