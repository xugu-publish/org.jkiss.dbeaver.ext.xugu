package org.jkiss.dbeaver.ext.xugu.edit;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.edit.XuguSchemaManager.NewUserDialog;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableBase;
import org.jkiss.dbeaver.ext.xugu.model.XuguTablePartition;
import org.jkiss.dbeaver.ext.xugu.model.XuguTablePhysical;
import org.jkiss.dbeaver.ext.xugu.model.XuguTablespace;
import org.jkiss.dbeaver.ext.xugu.model.XuguUser;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class XuguTablePartitionManager extends SQLObjectEditor<XuguTablePartition, XuguTablePhysical>{
	private String dataFileDesc;
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		// TODO Auto-generated method stub
		return FEATURE_SAVE_IMMEDIATELY;
	}
	
	@Override
    protected XuguTablePartition createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final XuguTablePhysical parent, Object copyFrom)
    {
        return new UITask<XuguTablePartition>() {
            @Override
            protected XuguTablePartition runTask() {
                NewTablePartitionDialog dialog = new NewTablePartitionDialog(UIUtils.getActiveWorkbenchShell(), parent);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                XuguTablePartition newTablePartition = dialog.getTablePartition();
//                XuguDataFile newDataFile = new XuguDataFile(newTablespace, null, false);
                return newTablePartition;
            }
        }.execute();
    }

	@Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
    	StringBuilder sql = new StringBuilder("ALTER TABLE ");
    	sql.append(command.getObject().getParentObject().getName());
    	sql.append(" ADD PARTITION ");
    	sql.append(command.getObject().getName());
    	sql.append(" VALUES('");
    	sql.append(command.getObject().getPartiValue());
    	sql.append("')");
    	actions.add(new SQLDatabasePersistAction("Add Partition", sql.toString()));
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
    	StringBuilder sql = new StringBuilder("ALTER TABLE ");
    	sql.append(command.getObject().getParentObject().getName());
    	sql.append(" DROP PARTITION ");
    	sql.append(command.getObject().getName());
    	actions.add(new SQLDatabasePersistAction("Drop Partition", sql.toString()));
    }
    
    @Override
    public void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException
    {
    	if(command.getProperty("online") == null) {
    		StringBuilder sql = new StringBuilder("ALTER TABLE ");
        	sql.append(command.getObject().getParentObject().getName());
        	sql.append(" SET PARTITION ");
        	sql.append(command.getObject().getName());
        	sql.append((boolean)command.getProperty("online")?" ONLINE":" OFFLINE");
        	actionList.add(new SQLDatabasePersistAction("Alter Partition", sql.toString()));
    	}
    }
    
    static class NewTablePartitionDialog extends Dialog {
    	
    	private XuguTablePartition partition;
    	private XuguTablePhysical table;
        private Text nameText;
        private Text valueText;

        public NewTablePartitionDialog(Shell parentShell, XuguTablePhysical table)
        {
            super(parentShell);
            this.table = table;
        }
        
        public XuguTablePartition getTablePartition() {
        	return this.partition;
        }
        
        @Override
        protected boolean isResizable()
        {
            return true;
        }
        
        @Override
        protected Point getInitialSize() {
        	return new Point(300, 250);
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText(XuguMessages.dialog_tablespace_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, XuguMessages.dialog_tablespace_name, null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            valueText = UIUtils.createLabelText(composite, XuguMessages.dialog_tablespace_nodeID, null);
            valueText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createInfoLabel(composite, XuguMessages.dialog_tablespace_create_info, GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed()
        {
        	this.partition = new XuguTablePartition(table, false ,"");
            partition.setName(DBObjectNameCaseTransformer.transformObjectName(partition, nameText.getText()));
            partition.setPartiValue(DBObjectNameCaseTransformer.transformObjectName(partition,valueText.getText()));
            partition.setOnline(true);
            super.okPressed();
        }

    }

	@Override
	public DBSObjectCache<? extends DBSObject, XuguTablePartition> getObjectsCache(XuguTablePartition object) {
		// TODO Auto-generated method stub
		return null;
	}
}
