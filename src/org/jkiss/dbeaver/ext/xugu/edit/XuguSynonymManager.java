package org.jkiss.dbeaver.ext.xugu.edit;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguSynonym;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class XuguSynonymManager extends SQLObjectEditor<XuguSynonym, XuguSchema> implements DBEObjectRenamer<XuguSynonym> {
	@Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }
	
	protected XuguSynonym createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final XuguSchema parent, Object copyFrom) throws DBException {
		return new UITask<XuguSynonym>() {
            @Override
            protected XuguSynonym runTask() {
                NewSynonymDialog dialog = new NewSynonymDialog(UIUtils.getActiveWorkbenchShell(), parent);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                XuguSynonym newSynonym = dialog.getSynonym();
                return newSynonym;
            }
        }.execute();
	}
	
	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
		XuguSynonym synonym = command.getObject();
		String sql = "CREATE SYNONYM " + synonym.getName() + " FOR " + synonym.getTargetName();
		actions.add(new SQLDatabasePersistAction("Create synonym", sql));
	}
	
	@Override
	protected void addObjectDeleteActions(List<DBEPersistAction> actions,
			SQLObjectEditor<XuguSynonym, XuguSchema>.ObjectDeleteCommand command, Map<String, Object> options) {
		actions.add(
            new SQLDatabasePersistAction("Drop synonym",
                "DROP SYNONYM " + DBUtils.getQuotedIdentifier(command.getObject())) //$NON-NLS-2$
        );
	}

	@Override
	public void renameObject(DBECommandContext commandContext, XuguSynonym object, String newName) throws DBException {
		// TODO Auto-generated method stub
		throw new DBException("Direct synonym rename is not yet implemented in XuguDB. You should use export/import functions for that.");   
	}
	
	@Override
	public DBSObjectCache<? extends DBSObject, XuguSynonym> getObjectsCache(XuguSynonym object) {
		// TODO Auto-generated method stub
		return object.getSchema().synonymCache;
	}
	
	static class NewSynonymDialog extends Dialog {
    	
    	private XuguSynonym synonym;
        private Text nameText;
        private Text tarNameText;
		

        public NewSynonymDialog(Shell parentShell, XuguSchema dataSource)
        {
            super(parentShell);
            this.synonym = new XuguSynonym(dataSource, null);  
        }

        public XuguSynonym getSynonym()
        {
            return synonym;
        }
        
        @Override
        protected boolean isResizable()
        {
            return true;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText("Set synonym properties");

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 3, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, "Synonym Name", null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            tarNameText = UIUtils.createLabelText(composite, "Target Name", null);
            tarNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createInfoLabel(composite, "Creating a synonym.", GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed()
        {
            synonym.setName(DBObjectNameCaseTransformer.transformObjectName(synonym, nameText.getText()));
            synonym.setTargetName(DBObjectNameCaseTransformer.transformObjectName(synonym, tarNameText.getText()));
            super.okPressed();
        }

    }

	


}


