package civerooni.com.putman.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;

public class NewRequestHandler {
    @Execute
    public void execute() {
        System.out.println("NewRequestHandler: create new request tab");
    }
}
