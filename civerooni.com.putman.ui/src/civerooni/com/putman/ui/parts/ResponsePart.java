package civerooni.com.putman.ui.parts;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import civerooni.com.putman.core.model.ResponseModel;
import civerooni.com.putman.ui.handlers.SendRequestHandler;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class ResponsePart {

    private Label statusLabel;
    private Text responseBody;

    @PostConstruct
    public void createControls(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setText("Status: —");

        responseBody = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        responseBody.setMessage("Response body will appear here");
        responseBody.setEditable(false);
        responseBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    @Inject
    @Optional
	public void onResponse(@UIEventTopic(SendRequestHandler.RESPONSE_TOPIC) ResponseModel response) {
        if (statusLabel == null || statusLabel.isDisposed()) return;
        if (response.isError()) {
            statusLabel.setText("Error");
            responseBody.setText(response.getError());
        } else {
            statusLabel.setText("Status: " + response.getStatusCode());
            responseBody.setText(response.getBody() != null ? response.getBody() : "");
        }
        statusLabel.getParent().layout();
    }
}
