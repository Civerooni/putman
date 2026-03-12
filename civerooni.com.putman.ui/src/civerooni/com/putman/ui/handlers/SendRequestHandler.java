package civerooni.com.putman.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;

import civerooni.com.putman.core.http.PutmanHttpClient;
import civerooni.com.putman.core.model.RequestModel;
import civerooni.com.putman.core.model.ResponseModel;

public class SendRequestHandler {

    public static final String RESPONSE_TOPIC = "civerooni/putman/ui/response";

    @Execute
    public void execute(@Optional RequestModel requestModel, IEventBroker broker) {
        if (requestModel == null) {
            broker.post(RESPONSE_TOPIC, ResponseModel.error("No request configured."));
            return;
        }

        PutmanHttpClient.getInstance().send(requestModel)
                .thenAccept(response -> broker.post(RESPONSE_TOPIC, response));
    }
}
