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
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
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

public class XuguTablespaceManager extends SQLObjectEditor<XuguTablespace, XuguDataSource> implements DBEObjectRenamer<XuguTablespace> {
	private String dataFileDesc;
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		// TODO Auto-generated method stub
		return FEATURE_SAVE_IMMEDIATELY;
	}
	
	@Override
	public DBSObjectCache<? extends DBSObject, XuguTablespace> getObjectsCache(XuguTablespace object) {
		// TODO Auto-generated method stub
		return object.getDataSource().getTablespaceCache();
	}
	
	@Override
    protected XuguTablespace createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final XuguDataSource parent, Object copyFrom)
    {
        return new UITask<XuguTablespace>() {
            @Override
            protected XuguTablespace runTask() {
                NewTablespaceDialog dialog = new NewTablespaceDialog(UIUtils.getActiveWorkbenchShell(), parent);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                XuguTablespace newTablespace = dialog.getTableSpace();
//                XuguDataFile newDataFile = new XuguDataFile(newTablespace, null, false);
                return newTablespace;
            }
        }.execute();
    }

	@Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
    	//xfc 修改了创建模式的sql语句 暂时不支持设置数据库
        XuguTablespace tablespace = command.getObject();
        String sql = "CREATE TABLESPACE " + tablespace.getName();
        if(command.getObject().getNodeID()>0) {
        	sql += " ON NODE "+command.getObject().getNodeID();
        }else {
        	sql += " ON ALL NODE";
        }
        sql += " DATAFILE '"+tablespace.getFilePath()+"'";
        if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct add tablespace sql: "+sql);
        }
        actions.add(new SQLDatabasePersistAction("Create Tablespace", sql));
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
    	String sql = "DROP TABLESPACE " + DBUtils.getQuotedIdentifier(command.getObject());
    	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct drop tablespace sql: "+sql);
        }
        actions.add(
            new SQLDatabasePersistAction("Drop Tablespace",
               sql) //$NON-NLS-2$
        );
    }
    
    @Override
    public void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException
    {
    	if (command.getProperties().size() > 1 || command.getProperty("comment") == null) {
            StringBuilder query = new StringBuilder("ALTER TABLESPACE "); //$NON-NLS-1$
            query.append(command.getObject().getName()).append(" "); //$NON-NLS-1$
            if(XuguConstants.LOG_PRINT_LEVEL<1) {
            	log.info("Xugu Plugin: Construct alter tablespace sql: "+query.toString());
            }
            actionList.add(new SQLDatabasePersistAction(query.toString()));
        }
    }
    
    static class NewTablespaceDialog extends Dialog {
    	
    	private XuguTablespace space;
        private Text nameText;
        private Text fileText;
        private Text nodeText;
		

        public NewTablespaceDialog(Shell parentShell, XuguDataSource dataSource)
        {
            super(parentShell);
            try {
				this.space = new XuguTablespace(dataSource, null);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        public XuguTablespace getTableSpace() {
        	return space;
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
            
            nodeText = UIUtils.createLabelText(composite, XuguMessages.dialog_tablespace_nodeID, null);
            nodeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            fileText = UIUtils.createLabelText(composite, XuguMessages.dialog_tablespace_filePath, null);
            fileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createInfoLabel(composite, XuguMessages.dialog_tablespace_create_info, GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed()
        {
            space.setName(DBObjectNameCaseTransformer.transformObjectName(space, nameText.getText()));
            space.setFilePath(DBObjectNameCaseTransformer.transformObjectName(space,fileText.getText()));
            if(nodeText.getText()!=null) {
            	space.setNodeID(Integer.parseInt(DBObjectNameCaseTransformer.transformObjectName(space,nodeText.getText())));
            }else {
            	space.setNodeID(0);
            }
            super.okPressed();
        }

    }

	@Override
	public void renameObject(DBECommandContext commandContext, XuguTablespace object, String newName)
			throws DBException {
		// TODO Auto-generated method stub
		throw new DBException("Direct tablespace rename is not yet implemented in XuguDB. You should use export/import functions for that.");
	}

}
