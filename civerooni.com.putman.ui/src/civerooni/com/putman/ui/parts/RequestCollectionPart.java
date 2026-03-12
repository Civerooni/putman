package civerooni.com.putman.ui.parts;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import jakarta.annotation.PostConstruct;

public class RequestCollectionPart {

    @PostConstruct
    public void createControls(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        Label label = new Label(parent, SWT.NONE);
        label.setText("Collections");

        List list = new List(parent, SWT.BORDER | SWT.V_SCROLL);
        list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        list.add("Sample Collection");
    }
}
