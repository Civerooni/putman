package civerooni.com.putman.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;

import civerooni.com.putman.ui.parts.RequestEditorPart;

public class NewRequestHandler {

    @Execute
    public void execute(IEventBroker broker) {
        broker.post(RequestEditorPart.NEW_REQUEST_TOPIC, Boolean.TRUE);
    }
}
