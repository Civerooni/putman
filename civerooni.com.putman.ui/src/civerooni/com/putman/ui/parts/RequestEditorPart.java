package civerooni.com.putman.ui.parts;

import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import java.util.function.IntConsumer;

import civerooni.com.putman.core.model.HeaderEntry;
import civerooni.com.putman.core.model.HttpMethod;
import civerooni.com.putman.core.model.RequestModel;
import civerooni.com.putman.core.model.ResponseModel;
import civerooni.com.putman.ui.handlers.SendRequestHandler;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class RequestEditorPart {

    public static final String NEW_REQUEST_TOPIC = "civerooni/putman/ui/newrequest";

    /**
     * Posted when switching tabs to replay a cached response in ResponsePart.
     * ResponsePart subscribes to this in addition to SendRequestHandler.RESPONSE_TOPIC.
     */
    public static final String REPLAY_RESPONSE_TOPIC = "civerooni/putman/ui/response/replay";

    @Inject private IEclipseContext context;
    @Inject private ECommandService commandService;
    @Inject private EHandlerService handlerService;
    @Inject private IEventBroker broker;

    private CTabFolder requestTabs;
    private int tabCounter = 1;

    @PostConstruct
    public void createControls(Composite parent) {
        parent.setLayout(new FillLayout());

        requestTabs = new CTabFolder(parent, SWT.BORDER | SWT.CLOSE);
        requestTabs.setSimple(false);

        requestTabs.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateContextForActiveTab();
            }
        });

        requestTabs.addCTabFolder2Listener(new CTabFolder2Adapter() {
            @Override
            public void close(CTabFolderEvent event) {
                if (requestTabs.getItemCount() == 1) {
                    event.doit = false;
                }
            }
        });

        addNewTab();
    }

    @Inject
    @Optional
    public void onNewRequest(@UIEventTopic(NEW_REQUEST_TOPIC) Object ignored) {
        if (requestTabs == null || requestTabs.isDisposed()) return;
        addNewTab();
    }

    /** Caches the real response from SendRequestHandler in the active tab. */
    @Inject
    @Optional
    public void onResponse(@UIEventTopic(SendRequestHandler.RESPONSE_TOPIC) ResponseModel response) {
        if (requestTabs == null || requestTabs.isDisposed()) return;
        CTabItem active = requestTabs.getSelection();
        if (active != null && !active.isDisposed()) {
            active.setData("response", response);
        }
    }

    /** Saves the response format selection for the active tab. */
    @Inject
    @Optional
    public void onFormatChange(@UIEventTopic(ResponsePart.FORMAT_CHANGE_TOPIC) Integer format) {
        if (requestTabs == null || requestTabs.isDisposed()) return;
        CTabItem active = requestTabs.getSelection();
        if (active != null && !active.isDisposed()) {
            active.setData("responseFormat", format);
        }
    }

    private void addNewTab() {
        RequestModel model = new RequestModel();
        CTabItem tab = new CTabItem(requestTabs, SWT.CLOSE);
        tab.setText("Request " + tabCounter++);
        tab.setData("model", model);
        tab.setData("responseFormat", 0);

        Composite content = new Composite(requestTabs, SWT.NONE);
        createEditorContent(content, model);
        tab.setControl(content);

        requestTabs.setSelection(tab);
        context.set(RequestModel.class, model);
        broker.post(REPLAY_RESPONSE_TOPIC, new ResponseReplay(ResponseModel.empty(), 0));
    }

    private void updateContextForActiveTab() {
        CTabItem selected = requestTabs.getSelection();
        if (selected == null || selected.isDisposed()) return;
        RequestModel model = (RequestModel) selected.getData("model");
        context.set(RequestModel.class, model);
        ResponseModel response = (ResponseModel) selected.getData("response");
        Integer format = (Integer) selected.getData("responseFormat");
        broker.post(REPLAY_RESPONSE_TOPIC, new ResponseReplay(
                response != null ? response : ResponseModel.empty(),
                format != null ? format : 0));
    }

    private void createEditorContent(Composite parent, RequestModel model) {
        parent.setLayout(new GridLayout(1, false));

        // URL bar row
        Composite urlBar = new Composite(parent, SWT.NONE);
        urlBar.setLayout(new GridLayout(3, false));
        urlBar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Combo methodCombo = new Combo(urlBar, SWT.DROP_DOWN | SWT.READ_ONLY);
        methodCombo.setItems(HttpMethod.names());
        methodCombo.select(0);

        Text urlText = new Text(urlBar, SWT.BORDER);
        urlText.setMessage("Enter request URL");
        urlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button sendButton = new Button(urlBar, SWT.PUSH);
        sendButton.setText("Send");

        // Params / Headers / Body tabs
        CTabFolder tabFolder = new CTabFolder(parent, SWT.BORDER);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final boolean[] suppressParamSync = { false };

        // --- Params tab ---
        CTabItem paramsTab = new CTabItem(tabFolder, SWT.NONE);
        paramsTab.setText("Params");

        Composite paramsComposite = new Composite(tabFolder, SWT.NONE);
        paramsComposite.setLayout(new GridLayout(1, false));
        paramsTab.setControl(paramsComposite);

        Composite paramsTableWrapper = new Composite(paramsComposite, SWT.NONE);
        paramsTableWrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Table paramsTable = new Table(paramsTableWrapper, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE | SWT.V_SCROLL);
        paramsTable.setHeaderVisible(true);
        paramsTable.setLinesVisible(true);

        TableColumn paramKeyCol = new TableColumn(paramsTable, SWT.NONE);
        paramKeyCol.setText("Key");
        TableColumn paramValueCol = new TableColumn(paramsTable, SWT.NONE);
        paramValueCol.setText("Value");
        TableColumn paramEnabledCol = new TableColumn(paramsTable, SWT.CENTER);
        paramEnabledCol.setText("Enabled");

        TableColumnLayout paramsTableLayout = new TableColumnLayout();
        paramsTableLayout.setColumnData(paramKeyCol,     new ColumnWeightData(40, 80));
        paramsTableLayout.setColumnData(paramValueCol,   new ColumnWeightData(50, 80));
        paramsTableLayout.setColumnData(paramEnabledCol, new ColumnWeightData(10, 50));
        paramsTableWrapper.setLayout(paramsTableLayout);

        paramsTable.setBackground(paramsTable.getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));

        paramsTable.addListener(SWT.EraseItem, event -> {
            int idx = paramsTable.indexOf((TableItem) event.item);
            boolean isDataRow = idx >= 0 && idx < model.getParams().size();
            boolean isSelected = (event.detail & SWT.SELECTED) != 0;
            if (event.index == 2 && isDataRow) event.detail &= ~SWT.FOREGROUND;
            if (isSelected) return;
            if (isDataRow) {
                HeaderEntry e = model.getParams().get(idx);
                if (!e.isEnabled() || e.getKey().isBlank()) {
                    event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
                    event.gc.fillRectangle(event.x, event.y, event.width, event.height);
                    event.detail &= ~(SWT.BACKGROUND | SWT.FOREGROUND);
                } else {
                    event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
                    event.gc.fillRectangle(event.x, event.y, event.width, event.height);
                    event.detail &= ~SWT.BACKGROUND;
                }
            }
        });

        paramsTable.addListener(SWT.PaintItem, event -> {
            if (event.index == 0 && paramsTable.getItemCount() > 0
                    && event.item == paramsTable.getItem(0)) {
                event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                event.gc.drawLine(0, event.y, paramsTable.getClientArea().width, event.y);
            }
            int idx = paramsTable.indexOf((TableItem) event.item);
            if (idx < 0 || idx >= model.getParams().size()) return;
            HeaderEntry e = model.getParams().get(idx);
            boolean isDimmed = !e.isEnabled() || e.getKey().isBlank();
            boolean isSelected = (event.detail & SWT.SELECTED) != 0;
            if (event.index == 2) {
                TableItem item = (TableItem) event.item;
                String text = item.getText(2);
                Point ext = event.gc.textExtent(text);
                org.eclipse.swt.graphics.Rectangle cell = item.getBounds(2);
                int x = cell.x + (cell.width - ext.x) / 2;
                int y = cell.y + (cell.height - ext.y) / 2;
                if (isDimmed && !isSelected) event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                event.gc.drawText(text, x, y, true);
            } else if (isDimmed && !isSelected) {
                String text = ((TableItem) event.item).getText(event.index);
                event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                event.gc.drawText(text, event.x + 2, event.y + 2, true);
            }
        });

        IntConsumer updateParamRowStyle = idx -> paramsTable.redraw();

        TableEditor paramsEditor = new TableEditor(paramsTable);
        paramsEditor.horizontalAlignment = SWT.LEFT;
        paramsEditor.grabHorizontal = true;
        paramsEditor.minimumWidth = 50;

        paramsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                Control old = paramsEditor.getEditor();
                if (old != null) old.dispose();

                TableItem item = paramsTable.getItem(new Point(e.x, e.y));
                if (item == null) return;
                int row = paramsTable.indexOf(item);
                HeaderEntry entry = model.getParams().get(row);

                int col = -1;
                for (int i = 0; i < 3; i++) {
                    if (item.getBounds(i).contains(e.x, e.y)) { col = i; break; }
                }

                if (col == 0 || col == 1) {
                    Text text = new Text(paramsTable, SWT.NONE);
                    text.setText(item.getText(col));
                    text.selectAll();
                    final int c = col;
                    Runnable commit = () -> {
                        if (text.isDisposed()) return;
                        String val = text.getText();
                        if (c == 0) entry.setKey(val);
                        else entry.setValue(val);
                        item.setText(c, val);
                        updateParamRowStyle.accept(row);
                        text.dispose();
                        suppressParamSync[0] = true;
                        urlText.setText(buildUrlWithParams(model));
                        suppressParamSync[0] = false;
                    };
                    text.addFocusListener(new FocusAdapter() {
                        @Override public void focusLost(FocusEvent e) { commit.run(); }
                    });
                    text.addKeyListener(new KeyAdapter() {
                        @Override public void keyPressed(KeyEvent e) {
                            if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) commit.run();
                            else if (e.keyCode == SWT.ESC) text.dispose();
                        }
                    });
                    paramsEditor.setEditor(text, item, col);
                    text.setFocus();
                } else if (col == 2) {
                    entry.setEnabled(!entry.isEnabled());
                    item.setText(2, entry.isEnabled() ? "\u2611" : "\u2610");
                    updateParamRowStyle.accept(row);
                    suppressParamSync[0] = true;
                    urlText.setText(buildUrlWithParams(model));
                    suppressParamSync[0] = false;
                }
            }
        });

        Composite paramInputRow = new Composite(paramsComposite, SWT.NONE);
        paramInputRow.setLayout(new GridLayout(4, false));
        paramInputRow.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));

        Text paramKeyText = new Text(paramInputRow, SWT.BORDER);
        paramKeyText.setMessage("Key");
        paramKeyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Text paramValueText = new Text(paramInputRow, SWT.BORDER);
        paramValueText.setMessage("Value");
        paramValueText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button addParamButton = new Button(paramInputRow, SWT.PUSH);
        addParamButton.setText("Add");

        Button removeParamButton = new Button(paramInputRow, SWT.PUSH);
        removeParamButton.setText("Remove");

        addParamButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String key = paramKeyText.getText().trim();
                String value = paramValueText.getText().trim();
                if (!key.isEmpty()) {
                    model.getParams().add(new HeaderEntry(key, value, true));
                    TableItem item = new TableItem(paramsTable, SWT.NONE);
                    item.setText(new String[] { key, value, "\u2611" });
                    updateParamRowStyle.accept(paramsTable.getItemCount() - 1);
                    paramKeyText.setText("");
                    paramValueText.setText("");
                    suppressParamSync[0] = true;
                    urlText.setText(buildUrlWithParams(model));
                    suppressParamSync[0] = false;
                }
            }
        });

        removeParamButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = paramsTable.getSelectionIndex();
                if (idx >= 0) {
                    model.getParams().remove(idx);
                    paramsTable.remove(idx);
                    suppressParamSync[0] = true;
                    urlText.setText(buildUrlWithParams(model));
                    suppressParamSync[0] = false;
                }
            }
        });

        // --- Headers tab ---
        CTabItem headersTab = new CTabItem(tabFolder, SWT.NONE);
        headersTab.setText("Headers");

        Composite headersComposite = new Composite(tabFolder, SWT.NONE);
        headersComposite.setLayout(new GridLayout(1, false));
        headersTab.setControl(headersComposite);

        Composite tableWrapper = new Composite(headersComposite, SWT.NONE);
        tableWrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Table headersTable = new Table(tableWrapper, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE | SWT.V_SCROLL);
        headersTable.setHeaderVisible(true);
        headersTable.setLinesVisible(true);

        TableColumn keyCol = new TableColumn(headersTable, SWT.NONE);
        keyCol.setText("Header");
        TableColumn valueCol = new TableColumn(headersTable, SWT.NONE);
        valueCol.setText("Value");
        TableColumn enabledCol = new TableColumn(headersTable, SWT.CENTER);
        enabledCol.setText("Enabled");

        TableColumnLayout tableLayout = new TableColumnLayout();
        tableLayout.setColumnData(keyCol,     new ColumnWeightData(35, 80));
        tableLayout.setColumnData(valueCol,   new ColumnWeightData(55, 80));
        tableLayout.setColumnData(enabledCol, new ColumnWeightData(10, 50));
        tableWrapper.setLayout(tableLayout);

        headersTable.setBackground(headersTable.getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));

        headersTable.addListener(SWT.EraseItem, event -> {
            int idx = headersTable.indexOf((TableItem) event.item);
            boolean isDataRow = idx >= 0 && idx < model.getHeaders().size();
            boolean isSelected = (event.detail & SWT.SELECTED) != 0;
            if (event.index == 2 && isDataRow) event.detail &= ~SWT.FOREGROUND;
            if (isSelected) return;
            if (isDataRow) {
                HeaderEntry e = model.getHeaders().get(idx);
                if (!e.isEnabled() || e.getKey().isBlank() || e.getValue().isBlank()) {
                    event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
                    event.gc.fillRectangle(event.x, event.y, event.width, event.height);
                    event.detail &= ~(SWT.BACKGROUND | SWT.FOREGROUND);
                } else {
                    event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
                    event.gc.fillRectangle(event.x, event.y, event.width, event.height);
                    event.detail &= ~SWT.BACKGROUND;
                }
            }
        });

        headersTable.addListener(SWT.PaintItem, event -> {
            if (event.index == 0 && headersTable.getItemCount() > 0
                    && event.item == headersTable.getItem(0)) {
                event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                event.gc.drawLine(0, event.y, headersTable.getClientArea().width, event.y);
            }
            int idx = headersTable.indexOf((TableItem) event.item);
            if (idx < 0 || idx >= model.getHeaders().size()) return;
            HeaderEntry e = model.getHeaders().get(idx);
            boolean isDimmed = !e.isEnabled() || e.getKey().isBlank() || e.getValue().isBlank();
            boolean isSelected = (event.detail & SWT.SELECTED) != 0;
            if (event.index == 2) {
                TableItem item = (TableItem) event.item;
                String text = item.getText(2);
                Point ext = event.gc.textExtent(text);
                org.eclipse.swt.graphics.Rectangle cell = item.getBounds(2);
                int x = cell.x + (cell.width - ext.x) / 2;
                int y = cell.y + (cell.height - ext.y) / 2;
                if (isDimmed && !isSelected) event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                event.gc.drawText(text, x, y, true);
            } else if (isDimmed && !isSelected) {
                String text = ((TableItem) event.item).getText(event.index);
                event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                event.gc.drawText(text, event.x + 2, event.y + 2, true);
            }
        });

        IntConsumer updateRowStyle = idx -> headersTable.redraw();

        TableEditor editor = new TableEditor(headersTable);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        editor.minimumWidth = 50;

        headersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                Control old = editor.getEditor();
                if (old != null) old.dispose();

                TableItem item = headersTable.getItem(new Point(e.x, e.y));
                if (item == null) return;
                int row = headersTable.indexOf(item);
                HeaderEntry entry = model.getHeaders().get(row);

                int col = -1;
                for (int i = 0; i < 3; i++) {
                    if (item.getBounds(i).contains(e.x, e.y)) { col = i; break; }
                }

                if (col == 0 || col == 1) {
                    Text text = new Text(headersTable, SWT.NONE);
                    text.setText(item.getText(col));
                    text.selectAll();
                    final int c = col;
                    Runnable commit = () -> {
                        if (text.isDisposed()) return;
                        String val = text.getText();
                        if (c == 0) entry.setKey(val);
                        else entry.setValue(val);
                        item.setText(c, val);
                        updateRowStyle.accept(row);
                        text.dispose();
                    };
                    text.addFocusListener(new FocusAdapter() {
                        @Override public void focusLost(FocusEvent e) { commit.run(); }
                    });
                    text.addKeyListener(new KeyAdapter() {
                        @Override public void keyPressed(KeyEvent e) {
                            if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) commit.run();
                            else if (e.keyCode == SWT.ESC) text.dispose();
                        }
                    });
                    editor.setEditor(text, item, col);
                    text.setFocus();
                } else if (col == 2) {
                    entry.setEnabled(!entry.isEnabled());
                    item.setText(2, entry.isEnabled() ? "\u2611" : "\u2610");
                    updateRowStyle.accept(row);
                }
            }
        });

        Composite headerInputRow = new Composite(headersComposite, SWT.NONE);
        headerInputRow.setLayout(new GridLayout(4, false));
        headerInputRow.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));

        Text headerKeyText = new Text(headerInputRow, SWT.BORDER);
        headerKeyText.setMessage("Header name");
        headerKeyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Text headerValueText = new Text(headerInputRow, SWT.BORDER);
        headerValueText.setMessage("Value");
        headerValueText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button addHeaderButton = new Button(headerInputRow, SWT.PUSH);
        addHeaderButton.setText("Add");

        Button removeHeaderButton = new Button(headerInputRow, SWT.PUSH);
        removeHeaderButton.setText("Remove");

        addHeaderButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String key = headerKeyText.getText().trim();
                String value = headerValueText.getText().trim();
                if (!key.isEmpty()) {
                    model.getHeaders().add(new HeaderEntry(key, value, true));
                    TableItem item = new TableItem(headersTable, SWT.NONE);
                    item.setText(new String[] { key, value, "\u2611" });
                    updateRowStyle.accept(headersTable.getItemCount() - 1);
                    headerKeyText.setText("");
                    headerValueText.setText("");
                }
            }
        });

        removeHeaderButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = headersTable.getSelectionIndex();
                if (idx >= 0) {
                    model.getHeaders().remove(idx);
                    headersTable.remove(idx);
                }
            }
        });

        // --- Body tab ---
        CTabItem bodyTab = new CTabItem(tabFolder, SWT.NONE);
        bodyTab.setText("Body");

        Composite bodyComposite = new Composite(tabFolder, SWT.NONE);
        bodyComposite.setLayout(new GridLayout(1, false));
        bodyTab.setControl(bodyComposite);

        Composite bodyToolbar = new Composite(bodyComposite, SWT.NONE);
        bodyToolbar.setLayout(new GridLayout(2, false));
        bodyToolbar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Combo bodyTypeCombo = new Combo(bodyToolbar, SWT.DROP_DOWN | SWT.READ_ONLY);
        bodyTypeCombo.setItems("Raw", "JSON");
        bodyTypeCombo.select(0);

        Label bodyErrorLabel = new Label(bodyToolbar, SWT.NONE);
        bodyErrorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        bodyErrorLabel.setForeground(bodyErrorLabel.getDisplay().getSystemColor(SWT.COLOR_RED));

        Text bodyText = new Text(bodyComposite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        bodyText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        bodyText.addModifyListener(e -> {
            model.setBody(bodyText.getText());
            bodyErrorLabel.setText("");
        });

        final String[] bodyRawSnapshot = { "" };
        bodyTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                bodyErrorLabel.setText("");
                if (bodyTypeCombo.getSelectionIndex() == 1) { // JSON
                    String raw = bodyText.getText();
                    if (!raw.isBlank()) {
                        try {
                            bodyRawSnapshot[0] = raw;
                            bodyText.setText(prettifyJson(raw));
                        } catch (IllegalArgumentException ex) {
                            bodyErrorLabel.setText("Invalid JSON: " + ex.getMessage());
                        }
                    }
                } else { // Raw
                    if (!bodyRawSnapshot[0].isEmpty()) {
                        bodyText.setText(bodyRawSnapshot[0]);
                        bodyRawSnapshot[0] = "";
                    }
                }
            }
        });

        tabFolder.setSelection(0);

        methodCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                model.setMethod(HttpMethod.values()[methodCombo.getSelectionIndex()]);
            }
        });

        urlText.addModifyListener(e -> {
            String url = urlText.getText();
            model.setUrl(url);
            if (!suppressParamSync[0]) {
                refreshParamsTable(url, paramsTable, model);
            }
        });

        sendButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                var cmd = commandService.createCommand(
                        "civerooni.com.putman.ui.command.sendrequest", null);
                handlerService.executeHandler(cmd);
            }
        });
    }

    private static void refreshParamsTable(String url, Table paramsTable, RequestModel model) {
        model.getParams().clear();
        paramsTable.removeAll();

        int queryStart = url.indexOf('?');
        if (queryStart < 0 || queryStart >= url.length() - 1) return;

        String query = url.substring(queryStart + 1);
        for (String pair : query.split("&", -1)) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 && eq < pair.length() - 1 ? pair.substring(eq + 1) : "";
            model.getParams().add(new HeaderEntry(key, value, true));
            TableItem item = new TableItem(paramsTable, SWT.NONE);
            item.setText(new String[] { key, value, "\u2611" });
        }
    }

    private static String buildUrlWithParams(RequestModel model) {
        String url = model.getUrl();
        int queryStart = url.indexOf('?');
        String base = queryStart >= 0 ? url.substring(0, queryStart) : url;

        StringBuilder sb = new StringBuilder(base);
        boolean first = true;
        for (HeaderEntry p : model.getParams()) {
            if (!p.isEnabled() || p.getKey().isBlank()) continue;
            sb.append(first ? '?' : '&');
            sb.append(p.getKey());
            if (!p.getValue().isEmpty()) {
                sb.append('=');
                sb.append(p.getValue());
            }
            first = false;
        }
        return sb.toString();
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
