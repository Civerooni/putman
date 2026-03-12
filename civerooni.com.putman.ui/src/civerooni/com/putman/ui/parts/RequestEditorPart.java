package civerooni.com.putman.ui.parts;

import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.SWT;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.custom.CTabFolder;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.function.IntConsumer;

import civerooni.com.putman.core.model.HeaderEntry;
import civerooni.com.putman.core.model.HttpMethod;
import civerooni.com.putman.core.model.RequestModel;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class RequestEditorPart {

    @Inject
    private IEclipseContext context;

    @Inject
    private ECommandService commandService;

    @Inject
    private EHandlerService handlerService;

    @PostConstruct
    public void createControls(Composite parent) {
        RequestModel model = new RequestModel();
        context.set(RequestModel.class, model);

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

        // Headers / Body tabs
        CTabFolder tabFolder = new CTabFolder(parent, SWT.BORDER);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

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

        // Grey background for empty filler rows below data
        headersTable.setBackground(headersTable.getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));

        // Grey background + text for disabled/empty rows — uses owner-draw to bypass Eclipse CSS theming
        headersTable.addListener(SWT.EraseItem, event -> {
            int idx = headersTable.indexOf((TableItem) event.item);
            boolean isDataRow = idx >= 0 && idx < model.getHeaders().size();
            boolean isSelected = (event.detail & SWT.SELECTED) != 0;

            // Always suppress default text rendering for column 2 so PaintItem can center it
            if (event.index == 2 && isDataRow) event.detail &= ~SWT.FOREGROUND;

            if (isSelected) return; // let SWT draw the selection background

            if (isDataRow) {
                HeaderEntry e = model.getHeaders().get(idx);
                if (!e.isEnabled() || e.getKey().isBlank() || e.getValue().isBlank()) {
                    event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
                    event.gc.fillRectangle(event.x, event.y, event.width, event.height);
                    event.detail &= ~(SWT.BACKGROUND | SWT.FOREGROUND);
                } else {
                    // Enabled rows: explicitly fill white so table's grey background doesn't bleed through
                    event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
                    event.gc.fillRectangle(event.x, event.y, event.width, event.height);
                    event.detail &= ~SWT.BACKGROUND;
                }
            }
        });

        headersTable.addListener(SWT.PaintItem, event -> {
            // Reinforce header separator at the top of the first data row
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
                // Always center the checkbox character in the Enabled column
                TableItem item = (TableItem) event.item;
                String text = item.getText(2);
                Point ext = event.gc.textExtent(text);
                org.eclipse.swt.graphics.Rectangle cell = item.getBounds(2);
                int x = cell.x + (cell.width - ext.x) / 2;
                int y = cell.y + (cell.height - ext.y) / 2;
                if (isDimmed && !isSelected) event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                event.gc.drawText(text, x, y, true);
            } else if (isDimmed && !isSelected) {
                // Draw dim text for disabled/empty rows in other columns
                String text = ((TableItem) event.item).getText(event.index);
                event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                event.gc.drawText(text, event.x + 2, event.y + 2, true);
            }
        });

        // Owner-draw derives state from model on each repaint; just trigger a redraw
        IntConsumer updateRowStyle = idx -> headersTable.redraw();

        // Inline cell editor
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

        // Keep model in sync with widgets
        methodCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                model.setMethod(HttpMethod.values()[methodCombo.getSelectionIndex()]);
            }
        });

        urlText.addModifyListener(e -> model.setUrl(urlText.getText()));


        // Send button fires the sendrequest command via E4 DI
        sendButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                var cmd = commandService.createCommand(
                        "civerooni.com.putman.ui.command.sendrequest", null);
                handlerService.executeHandler(cmd);
            }
        });
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
                    // peek ahead: skip whitespace to see if next non-ws char closes immediately
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
