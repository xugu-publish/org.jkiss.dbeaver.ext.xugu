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
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguDatabase;
import org.jkiss.dbeaver.ext.xugu.views.XuguWarningDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;
import java.util.Map;

/**
 * @author Maple4Real
 * 数据库管理器
 * 进行数据库的增加和删除
 * 不支持重命名
 * 包含一个内部界面类，用于进行属性设定
 */
public class XuguDatabaseManager extends SQLObjectEditor<XuguDatabase, XuguDataSource> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<XuguDataSource, XuguDatabase> getObjectsCache(XuguDatabase object)
    {
        return object.getDataSource().databaseCache;
    }

    @Override
    protected XuguDatabase createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final XuguDataSource parent, Object copyFrom)
    {
    	return new UITask<XuguDatabase>() {
            @Override
            protected XuguDatabase runTask() {
                NewDBDialog dialog = new NewDBDialog(UIUtils.getActiveWorkbenchShell(), parent);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                XuguDatabase newDB = dialog.getDB();
                return newDB;
            }
        }.execute();
    }

    //禁止删除数据库
    @Override
    public boolean canDeleteObject(XuguDatabase object)
    {
        return false;
    }
    
    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        XuguDatabase database = command.getObject();
        String sql = "CREATE DATABASE " + database.getName();
        if(database.getCharset()!=null) {
        	sql +=" CHARACTER SET '"+database.getCharset()+"'";
        }
        if(database.getTimeZone()!=null) {
        	sql +=" TIME ZONE '"+database.getTimeZone()+"'";
        }
        if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct create database sql: "+sql.toString());
        }
        actions.add(new SQLDatabasePersistAction("Create database", sql));
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
    	//do nothing
    }

    
    
    static class NewDBDialog extends Dialog {
    	
    	private XuguDatabase database;
        private Text nameText;
		private Combo charsetCombo;
		private Combo isAddCombo;
		private Combo hourCombo;
		private Combo minuteCombo;

        public NewDBDialog(Shell parentShell, XuguDataSource dataSource)
        {
            super(parentShell);
            this.database = new XuguDatabase(dataSource, "");
        }
        
        @Override
        protected boolean isResizable()
        {
            return true;
        }

        @Override
        protected Point getInitialSize() {
        	return new Point(300, 350);
        }
        
        private XuguDatabase getDB() {
        	return database;
        }
        
        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText(XuguMessages.dialog_database_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 3);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, XuguMessages.dialog_database_name, null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            charsetCombo = UIUtils.createLabelCombo(composite, "Charset", 0);
            for(int i=0; i<XuguConstants.DEFAULT_CHAR_SET.length; i++) {
            	charsetCombo.add(XuguConstants.DEFAULT_CHAR_SET[i]);
            }
            
            isAddCombo = UIUtils.createLabelCombo(composite, "Add", 0);
            isAddCombo.add("+");
            isAddCombo.add("-");
            
            hourCombo = UIUtils.createLabelCombo(composite, "Hour", 0);
            for(int i=0; i<24; i++) {
            	if(i<10) {
            		hourCombo.add("0"+i);
            	}else {
            		hourCombo.add(i+"");
            	}
            }
            
            minuteCombo = UIUtils.createLabelCombo(composite, "Minute", 0);
            for(int i=0; i<60; i++) {
            	if(i<10) {
            		minuteCombo.add("0"+i);
            	}else {
            		minuteCombo.add(i+"");
            	}
            }

            UIUtils.createInfoLabel(composite, XuguMessages.dialog_database_create_info, GridData.FILL_HORIZONTAL, 3);

            return parent;
        }

        @Override
        protected void okPressed()
        {
            database.setName(DBObjectNameCaseTransformer.transformObjectName(database, nameText.getText()));
            database.setCharset(DBObjectNameCaseTransformer.transformObjectName(database, charsetCombo.getText()));
            String timeZone = "GMT"+isAddCombo.getText()+hourCombo.getText()+":"+minuteCombo.getText();
            database.setTimeZone(DBObjectNameCaseTransformer.transformObjectName(database, timeZone));
            super.okPressed();
        }

    }
}

