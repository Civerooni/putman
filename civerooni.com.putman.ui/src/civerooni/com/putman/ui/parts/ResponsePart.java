package civerooni.com.putman.ui.parts;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import civerooni.com.putman.core.model.ResponseModel;
import civerooni.com.putman.ui.handlers.SendRequestHandler;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class ResponsePart {

    public static final String FORMAT_CHANGE_TOPIC = "civerooni/putman/ui/response/format";

    @Inject private IEventBroker broker;

    private Label statusLabel;
    private Text responseBody;
    private Browser htmlBrowser;
    private StackLayout stackLayout;
    private Composite bodyStack;
    private Combo bodyTypeCombo;
    private Label bodyErrorLabel;
    private String rawSnapshot = "";

    @PostConstruct
    public void createControls(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setText("Response Status: \u2014");

        Composite toolbar = new Composite(parent, SWT.NONE);
        toolbar.setLayout(new GridLayout(2, false));
        toolbar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        bodyTypeCombo = new Combo(toolbar, SWT.DROP_DOWN | SWT.READ_ONLY);

        bodyErrorLabel = new Label(toolbar, SWT.NONE);
        bodyErrorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        bodyErrorLabel.setForeground(bodyErrorLabel.getDisplay().getSystemColor(SWT.COLOR_RED));

        bodyStack = new Composite(parent, SWT.NONE);
        stackLayout = new StackLayout();
        bodyStack.setLayout(stackLayout);
        bodyStack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        responseBody = new Text(bodyStack, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        responseBody.setMessage("Response body will appear here");
        responseBody.setEditable(false);

        Browser browser = null;
        try {
            browser = new Browser(bodyStack, SWT.NONE);
        } catch (SWTError e) {
            // Browser not available on this platform — HTML option will be hidden
        }
        htmlBrowser = browser;

        if (htmlBrowser != null) {
            bodyTypeCombo.setItems("Raw", "JSON", "HTML");
        } else {
            bodyTypeCombo.setItems("Raw", "JSON");
        }
        bodyTypeCombo.select(0);
        stackLayout.topControl = responseBody;

        bodyTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                bodyErrorLabel.setText("");
                int sel = bodyTypeCombo.getSelectionIndex();
                broker.post(FORMAT_CHANGE_TOPIC, sel);
                if (sel == 2 && htmlBrowser != null) { // HTML
                    htmlBrowser.setText(responseBody.getText());
                    stackLayout.topControl = htmlBrowser;
                    bodyStack.layout();
                } else {
                    stackLayout.topControl = responseBody;
                    bodyStack.layout();
                    if (sel == 1) { // JSON
                        String raw = responseBody.getText();
                        if (!raw.isBlank()) {
                            try {
                                rawSnapshot = raw;
                                responseBody.setText(prettifyJson(raw));
                            } catch (IllegalArgumentException ex) {
                                bodyErrorLabel.setText("Invalid JSON: " + ex.getMessage());
                            }
                        }
                    } else { // Raw
                        if (!rawSnapshot.isEmpty()) {
                            responseBody.setText(rawSnapshot);
                            rawSnapshot = "";
                        }
                    }
                }
            }
        });
    }

    @Inject
    @Optional
    public void onResponse(@UIEventTopic(SendRequestHandler.RESPONSE_TOPIC) ResponseModel response) {
        if (statusLabel == null || statusLabel.isDisposed()) return;
        // Real response: reset format to Raw
        bodyTypeCombo.select(0);
        bodyErrorLabel.setText("");
        rawSnapshot = "";
        stackLayout.topControl = responseBody;
        bodyStack.layout();
        setStatusAndBody(response);
    }

    @Inject
    @Optional
    public void onReplayResponse(@UIEventTopic(RequestEditorPart.REPLAY_RESPONSE_TOPIC) ResponseReplay replay) {
        if (statusLabel == null || statusLabel.isDisposed()) return;
        bodyErrorLabel.setText("");
        rawSnapshot = "";
        stackLayout.topControl = responseBody;
        bodyStack.layout();
        setStatusAndBody(replay.response);
        applyFormat(replay.format);
    }

    private void setStatusAndBody(ResponseModel response) {
        if (response == null || response.isEmpty()) {
            statusLabel.setText("Response Status: \u2014");
            responseBody.setText("");
        } else if (response.isError()) {
            statusLabel.setText("Response Status: Error");
            responseBody.setText(response.getError());
        } else {
            statusLabel.setText("Response Status: " + response.getStatusCode());
            responseBody.setText(response.getBody() != null ? response.getBody() : "");
        }
        statusLabel.getParent().layout();
    }

    private void applyFormat(int format) {
        bodyTypeCombo.select(format);
        if (format == 2 && htmlBrowser != null) { // HTML
            htmlBrowser.setText(responseBody.getText());
            stackLayout.topControl = htmlBrowser;
            bodyStack.layout();
        } else if (format == 1) { // JSON
            String raw = responseBody.getText();
            if (!raw.isBlank()) {
                try {
                    rawSnapshot = raw;
                    responseBody.setText(prettifyJson(raw));
                } catch (IllegalArgumentException ex) {
                    bodyErrorLabel.setText("Invalid JSON: " + ex.getMessage());
                }
            }
        }
    }

    private static String prettifyJson(String json) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        boolean escape = false;
        String input = json.strip();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escape) {
                sb.append(c);
                escape = false;
                continue;
            }
            if (inString) {
                sb.append(c);
                if (c == '\\') escape = true;
                else if (c == '"') inString = false;
                continue;
            }

            switch (c) {
                case '"' -> { inString = true; sb.append(c); }
                case '{', '[' -> {
                    sb.append(c);
                    int next = nextNonWhitespace(input, i + 1);
                    if (next != -1 && input.charAt(next) != '}' && input.charAt(next) != ']') {
                        indent++;
                        sb.append('\n');
                        appendIndent(sb, indent);
                    }
                }
                case '}', ']' -> {
                    indent--;
                    if (indent < 0) throw new IllegalArgumentException("unexpected '" + c + "'");
                    sb.append('\n');
                    appendIndent(sb, indent);
                    sb.append(c);
                }
                case ',' -> {
                    sb.append(c);
                    sb.append('\n');
                    appendIndent(sb, indent);
                }
                case ':' -> sb.append(": ");
                default -> { if (c > ' ') sb.append(c); }
            }
        }

        if (inString) throw new IllegalArgumentException("unterminated string");
        if (indent != 0) throw new IllegalArgumentException("mismatched brackets");
        return sb.toString();
    }

    private static int nextNonWhitespace(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (s.charAt(i) > ' ') return i;
        }
        return -1;
    }

    private static void appendIndent(StringBuilder sb, int level) {
        sb.append("    ".repeat(level));
    }
}
