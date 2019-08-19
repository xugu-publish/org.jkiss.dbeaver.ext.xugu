/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.xugu.edit;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.XuguUtils;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguUser;
import org.jkiss.dbeaver.ext.xugu.views.XuguWarningDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author xugu-publish
 * 模式管理器
 * 进行模式的创建和删除，修改(重命名或添加注释信息)
 * 包含一个内部界面类，用于进行属性设定
 */
public class XuguSchemaManager extends SQLObjectEditor<XuguSchema, XuguDataSource> implements DBEObjectRenamer<XuguSchema> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }
    
    protected void validateObjectProperties(ObjectChangeCommand command) throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) 
        {
            throw new DBException("Schema name cannot be empty");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguSchema> getObjectsCache(XuguSchema object)
    {
        return object.getDataSource().schemaCache;
    }

    @Override
    protected XuguSchema createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options)
    {
    	XuguDataSource parent = (XuguDataSource)container;
        return new UITask<XuguSchema>() 
        {
            @Override
            protected XuguSchema runTask() {
                NewUserDialog dialog = new NewUserDialog(UIUtils.getActiveWorkbenchShell(), parent, monitor);
                if (dialog.open() != IDialogConstants.OK_ID) 
                {
                    return null;
                }
                XuguSchema newSchema = new XuguSchema(parent, -1, dialog.getSchema().getName());
                newSchema.setUser(dialog.getUser());
                return newSchema;
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
    	//xfc 修改了创建模式的sql语句 暂时不支持设置数据库
        XuguUser user = command.getObject().getUser();
        XuguSchema schema = command.getObject();
        StringBuilder desc = new StringBuilder();
        
        if (!CommonUtils.isEmpty(schema.getName())) 
        {
            desc.append("CREATE SCHEMA ");
            desc.append(DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), schema.getName()));
            if(!CommonUtils.isEmpty(user.getName())) 
            {
            	desc.append(" AUTHORIZATION ");
            	desc.append(DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), user.getName()));
            }
        }
        if(XuguConstants.LOG_PRINT_LEVEL<1) 
        {
        	log.info("Xugu Plugin: Construct create schema sql: " + desc.toString());
        }
        
        actions.add(new SQLDatabasePersistAction("create schema", desc.toString()));
    }

	// 修改模式名称
    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectRenameCommand command, Map<String, Object> options)
    { 
    	StringBuilder desc = new StringBuilder(100);
    	if (!CommonUtils.isEmpty(command.getNewName())) 
        {
            desc.append("ALTER SCHEMA ");
            desc.append(DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()));
            desc.append(" RENAME TO ");
            desc.append(DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName().toUpperCase()));
        }
    	
    	if(XuguConstants.LOG_PRINT_LEVEL<1) 
    	{
        	log.info("Xugu Plugin: Construct rename schema sql: " + desc.toString());
        }
    	
    	actionList.add(new SQLDatabasePersistAction("rename schema", desc.toString()));
    }

    // 对模式的修改只能是添加注释信息
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException 
    {
        String comment = buildComment(command.getObject());
        if (comment != null) 
        {
        	if(XuguConstants.LOG_PRINT_LEVEL<1) 
        	{
            	log.info("Xugu Plugin: Construct alter schema comment sql: "+comment);
            }
            actionList.add(new SQLDatabasePersistAction("Comment on Schema", comment));
        }
    }
    
    @Override
	public void renameObject(DBECommandContext commandContext, XuguSchema object, String newName) throws DBException 
    {		
    	processObjectRename(commandContext, object, newName);
    	//在执行完重命名后，修改对象名称（用于前台数据刷新）
    	object.setName(newName);
	}

	@Override
	protected void addObjectDeleteActions(List<DBEPersistAction> actions, SQLObjectEditor<XuguSchema, XuguDataSource>.ObjectDeleteCommand command, Map<String, Object> options) 
	{
		String sql = "DROP SCHEMA " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getObject().getName());
		if(XuguConstants.LOG_PRINT_LEVEL<1) 
		{
        	log.info("Xugu Plugin: Construct drop schema sql: "+sql);
        }
        actions.add(new SQLDatabasePersistAction("drop schema", sql));
	}
    
    static class NewUserDialog extends Dialog {
    	
    	private XuguSchema schema;
    	private XuguUser user;
        private Text nameText;
        private XuguDataSource dataSource;
        private Combo schemaOwner;
        private DBRProgressMonitor monitor;

        public NewUserDialog(Shell parentShell, XuguDataSource dataSource, DBRProgressMonitor monitor)
        {
            super(parentShell);
            this.schema = new XuguSchema(dataSource, -1, null);
            this.user = new XuguUser(dataSource, null, monitor);
            this.dataSource = dataSource;
            this.monitor = monitor;
        }

        public XuguUser getUser()
        {
            return user;
        }

        public XuguSchema getSchema() {
        	return schema;
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

            nameText = UIUtils.createLabelText(composite, XuguMessages.dialog_schema_name, null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            schemaOwner = UIUtils.createLabelCombo(composite, XuguMessages.dialog_schema_user, 0);
            schemaOwner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            try {
				Collection<XuguUser> userList = this.dataSource.userCache.getAllObjects(monitor, this.dataSource);
				Iterator<XuguUser> it = userList.iterator();
				while(it.hasNext()) 
				{
					XuguUser user = it.next();
					schemaOwner.add(user.getName());
				}
			} catch (DBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            UIUtils.createInfoLabel(composite, XuguMessages.dialog_schema_create_info, GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed()
        {
        	if(XuguUtils.checkString(nameText.getText())) 
        	{
        		user.setName(DBObjectNameCaseTransformer.transformObjectName(user, schemaOwner.getText()));
                schema.setName(DBObjectNameCaseTransformer.transformObjectName(schema,nameText.getText()));
                schema.setUser(user);
                super.okPressed();
        	} else {
        		XuguWarningDialog warnDialog = new XuguWarningDialog(UIUtils.getActiveWorkbenchShell(), "Schema name cannot be null!");
        		warnDialog.open();
        	}
        }

    }
    
    private String buildComment(XuguSchema schema)
    {
        if (!CommonUtils.isEmpty(schema.getComment())) 
        {
            return "COMMENT ON SCHEMA " + schema.getName() + " IS " + SQLUtils.quoteString(schema, schema.getComment());
        }
        return null;
    }
}

