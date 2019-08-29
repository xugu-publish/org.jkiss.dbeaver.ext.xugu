package org.jkiss.dbeaver.ext.xugu.edit;

import java.util.List;
import java.util.Map;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.XuguUtils;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguSynonym;
import org.jkiss.dbeaver.ext.xugu.views.XuguWarningDialog;
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
import org.jkiss.utils.CommonUtils;
/**
 * @author xugu-publish
 * 同义词管理器
 * 进行同义词的创建和删除，不支持修改
 * 包含一个内部界面类，用于进行属性设定
 */
public class XuguSynonymManager extends SQLObjectEditor<XuguSynonym, XuguSchema> implements DBEObjectRenamer<XuguSynonym> {
	@Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }
	
    protected void validateObjectProperties(ObjectChangeCommand command) throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Synonym name cannot be empty");
        }
    }

	@Override
	public DBSObjectCache<? extends DBSObject, XuguSynonym> getObjectsCache(XuguSynonym object) {
		// TODO Auto-generated method stub
		return object.getSchema().synonymCache;
	}
	
	@Override
	protected XuguSynonym createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options) throws DBException 
	{
		XuguSchema schema = (XuguSchema)container;
		return new UITask<XuguSynonym>() {
            @Override
            protected XuguSynonym runTask() {
                NewSynonymDialog dialog = new NewSynonymDialog(UIUtils.getActiveWorkbenchShell(), schema);
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
		String sql = "CREATE ";
		if(synonym.isPublic()) {
			sql += "PUBLIC ";
		}
		sql += "SYNONYM " + synonym.getName() + " FOR " + synonym.getTargetName();
		
		log.debug("[Xugu] Construct create synonym sql: "+sql);
		actions.add(new SQLDatabasePersistAction("Create synonym", sql));
	}
	
	@Override
	protected void addObjectDeleteActions(List<DBEPersistAction> actions,
			SQLObjectEditor<XuguSynonym, XuguSchema>.ObjectDeleteCommand command, Map<String, Object> options) {
		XuguSynonym synonym = command.getObject();
		String sql = "DROP ";
		if(synonym.isPublic()) {
			sql += "PUBLIC ";
		}
		sql+="SYNONYM " + DBUtils.getQuotedIdentifier(synonym);
		
		log.debug("[Xugu] Construct drop synonym sql: "+ sql);
		actions.add(new SQLDatabasePersistAction("Drop synonym", sql));
	}

	@Override
	public void renameObject(DBECommandContext commandContext, XuguSynonym object, String newName) throws DBException {
		// TODO Auto-generated method stub
		throw new DBException("Direct synonym rename is not yet implemented in XuguDB. You should use export/import functions for that.");   
	}
	
	static class NewSynonymDialog extends Dialog {
    	
    	private XuguSynonym synonym;
        private Text nameText;
        private Text tarNameText;
		private Button isPublicButton;

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
        protected Point getInitialSize() {
        	return new Point(300, 200);
        }
        
        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText(XuguMessages.dialog_synonym_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, XuguMessages.dialog_synonym_name, null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            tarNameText = UIUtils.createLabelText(composite, XuguMessages.dialog_synonym_target, null);
            tarNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            isPublicButton = UIUtils.createCheckbox(composite, "Is Public", false);
            
            UIUtils.createInfoLabel(composite, XuguMessages.dialog_synonym_create_info, GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed()
        {
        	if(XuguUtils.checkString(nameText.getText())) {
        		synonym.setName(DBObjectNameCaseTransformer.transformObjectName(synonym, nameText.getText()));
                synonym.setTargetName(DBObjectNameCaseTransformer.transformObjectName(synonym, tarNameText.getText()));
                synonym.setPublic(isPublicButton.getSelection());
                super.okPressed();
        	}else {
        		XuguWarningDialog warnDialog = new XuguWarningDialog(UIUtils.getActiveWorkbenchShell(), "Synonym name cannot be null");
        		warnDialog.open();
        	}
        }
    }
}


