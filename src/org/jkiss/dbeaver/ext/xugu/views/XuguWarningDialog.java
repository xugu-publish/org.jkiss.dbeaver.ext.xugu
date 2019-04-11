package org.jkiss.dbeaver.ext.xugu.views;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ui.UIUtils;

public class XuguWarningDialog extends Dialog{
	private String warningInfo;
	
	public XuguWarningDialog(Shell parentShell, String warningInfo) {
		super(parentShell);
		this.warningInfo = warningInfo;
	}
	
	@Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Point getInitialSize() {
    	return new Point(300,200);
    }
    
    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(XuguMessages.dialog_schema_create_title);

        Control container = super.createDialogArea(parent);
        Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createInfoLabel(composite, this.warningInfo, GridData.FILL_HORIZONTAL, 2);
        
        return parent;
    }

}
