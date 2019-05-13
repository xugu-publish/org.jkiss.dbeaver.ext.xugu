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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.editors.XuguRoleEditor;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguDatabase;
import org.jkiss.dbeaver.ext.xugu.model.XuguRole;
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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * @author Maple4Real
 * 角色管理器
 * 进行索引的创建和删除，不支持修改
 * 包含一个内部界面类，用于进行属性设定
 */
public class XuguRoleManager extends SQLObjectEditor<XuguRole, XuguDataSource> implements DBEObjectRenamer<XuguRole> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public boolean canCreateObject(XuguDataSource parent)
    {
        return true;
    }

    @Override
    public boolean canDeleteObject(XuguRole object)
    {
        return true;
    }
    
    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguRole> getObjectsCache(XuguRole object)
    {
        return object.getDataSource().roleCache;
    }
    
    @Override
    protected XuguRole createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final XuguDataSource parent, Object copyFrom)
    {
    	XuguRole newRole = new XuguRole(parent, monitor, null);
    	//修改已存在用户
        if (copyFrom instanceof XuguRole) {
            XuguRole tplRole = (XuguRole)copyFrom;
            newRole.setName(tplRole.getName());
        }
        //创建新用户
        else {
        	return new UITask<XuguRole>() {
			  @Override
			  protected XuguRole runTask() {
			  	innerDialog dialog = new innerDialog(UIUtils.getActiveWorkbenchShell(), monitor, parent);
			      if (dialog.open() != IDialogConstants.OK_ID) {
			          return null;
			      }
			      XuguRole newRole = dialog.getRole();
			      return newRole;
			  }
        	}.execute();
        }
        System.out.println("Create1 ??");
//        commandContext.addCommand(new CommandCreateUser(newUser), new CreateObjectReflector<>(this), true);
        return newRole;
//		
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
    	//xfc 修改了创建模式的sql语句 暂时不支持设置数据库
        String user = command.getObject().getUserDesc();
        XuguRole role = command.getObject();
        String sql = "CREATE ROLE " + role.getName();
        if(user!=null && !user.equals("")) {
        	sql += " INIT USER "+user;
        }		
        if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct create role sql: "+sql);
        }
        actions.add(new SQLDatabasePersistAction("Create role", sql));
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
    	String sql = "DROP ROLE " + DBUtils.getQuotedIdentifier(command.getObject());
        if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct drop role sql: "+sql);
        }
        actions.add(
            new SQLDatabasePersistAction("Drop role", sql) //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
    	// do nothing
    }
    
    @Override
    public void renameObject(DBECommandContext commandContext, XuguRole role, String newName) throws DBException
    {
        throw new DBException("Direct database rename is not yet implemented in Xugu. You should use export/import functions for that.");
    }
    
    static class innerDialog extends Dialog{
    	private XuguRole role;
        private Text roleText;
        private Text userNameText;
    	

        public innerDialog(Shell parentShell, DBRProgressMonitor monitor, XuguDataSource dataSource)
        {
            super(parentShell);
            this.role = new XuguRole(dataSource, monitor, null);  
        }

        public XuguRole getRole() {
        	return role;
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
            getShell().setText(XuguMessages.dialog_role_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            roleText = UIUtils.createLabelText(composite, XuguMessages.dialog_role_name, null);
            roleText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            userNameText = UIUtils.createLabelText(composite, XuguMessages.dialog_role_user, null);
            userNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            return parent;
        }

        @Override
        protected void okPressed()
        {
        	if(roleText.getText()!=null && !"".equals(roleText.getText())) {
        		role.setName(DBObjectNameCaseTransformer.transformObjectName(role, roleText.getText()));
                role.setUserDesc(DBObjectNameCaseTransformer.transformObjectName(role,userNameText.getText()));
                super.okPressed();
        	}else {
        		XuguWarningDialog warnDialog = new XuguWarningDialog(UIUtils.getActiveWorkbenchShell(), "Role name cannot be null");
        		warnDialog.open();
        	}
        }
    }
}

