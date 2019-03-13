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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguDatabase;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguUser;
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
 * OracleSchemaManager
 */
public class XuguSchemaManager extends SQLObjectEditor<XuguSchema, XuguDatabase> implements DBEObjectRenamer<XuguSchema> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguSchema> getObjectsCache(XuguSchema object)
    {
        return object.getDataSource().schemaCache;
    }

    @Override
    protected XuguSchema createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final XuguDatabase parent, Object copyFrom)
    {
        return new UITask<XuguSchema>() {
            @Override
            protected XuguSchema runTask() {
                NewUserDialog dialog = new NewUserDialog(UIUtils.getActiveWorkbenchShell(), parent.getDataSource());
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                XuguSchema newSchema = new XuguSchema(parent.getDataSource(), -1, dialog.getSchema().getName());
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
        String sql = "CREATE SCHEMA " + schema.getName() +" AUTHORIZATION " +user.getName();
        System.out.println("CCCCCSQL "+sql);		
        actions.add(new SQLDatabasePersistAction("Create schema", sql));
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop schema",
                "DROP SCHEMA " + DBUtils.getQuotedIdentifier(command.getObject()) + " CASCADE") //$NON-NLS-2$
        );
    }
    
    @Override
    public void renameObject(DBECommandContext commandContext, XuguSchema schema, String newName) throws DBException
    {
        throw new DBException("Direct database rename is not yet implemented in Oracle. You should use export/import functions for that.");
    }

    static class NewUserDialog extends Dialog {
    	
    	private XuguSchema schema;
    	private XuguUser user;
        private Text nameText;
        //private Text dbNameText;
        private Text userNameText;
		

        public NewUserDialog(Shell parentShell, XuguDataSource dataSource)
        {
            super(parentShell);
            this.schema = new XuguSchema(dataSource, -1, null);
            this.user = new XuguUser(dataSource);   
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
            getShell().setText("Set schema properties");

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 3, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, "Schema Name", null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            //xfc 增加了设置数据库名和用户名
//            dbNameText  = UIUtils.createLabelText(composite, "DB Name", null);
//            dbNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            userNameText = UIUtils.createLabelText(composite, "User Name", null);
            userNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createInfoLabel(composite, "Creating a schema.", GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed()
        {
            user.setName(DBObjectNameCaseTransformer.transformObjectName(user, userNameText.getText()));
            schema.setName(DBObjectNameCaseTransformer.transformObjectName(schema,nameText.getText()));
            schema.setUser(user);
            super.okPressed();
        }

    }

}

