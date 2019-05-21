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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.XuguUtils;
import org.jkiss.dbeaver.ext.xugu.edit.XuguSynonymManager.NewSynonymDialog;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguSynonym;
import org.jkiss.dbeaver.ext.xugu.model.XuguView;
import org.jkiss.dbeaver.ext.xugu.views.XuguWarningDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
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
 * 视图管理器
 * 进行视图的创建和删除，修改相当于创建并替换
 */
public class XuguViewManager extends SQLObjectEditor<XuguView, XuguSchema> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("View name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getViewText())) {
            throw new DBException("View definition cannot be empty");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguView> getObjectsCache(XuguView object)
    {
        return (DBSObjectCache) object.getSchema().viewCache;
    }

    @Override
    protected XuguView createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, XuguSchema parent, Object copyFrom)
    {
    	return new UITask<XuguView>() {
            @Override
            protected XuguView runTask() {
                NewViewDialog dialog = new NewViewDialog(UIUtils.getActiveWorkbenchShell(), parent);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
            	XuguView newView = dialog.getView();
            	newView.setViewText("CREATE VIEW " + newView.getFullyQualifiedName(DBPEvaluationContext.DDL) + " AS\nSELECT");
                return newView;
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        createOrReplaceViewQuery(monitor, actions, command);
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        createOrReplaceViewQuery(monitor, actionList, command);
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
    	String sql = "DROP VIEW " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL);
    	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct drop view sql: "+sql);
        }
        actions.add(
            new SQLDatabasePersistAction("Drop view", sql) //$NON-NLS-2$
        );
    }

    private void createOrReplaceViewQuery(DBRProgressMonitor monitor, List<DBEPersistAction> actions, DBECommandComposite<XuguView, PropertyHandler> command)
    {
        XuguView view = command.getObject();
        boolean replace = view.isReplace();
        boolean force = view.isForce();
        if (replace) {
        	//增加replace关键字的语句
        	if(view.getViewText().toUpperCase().indexOf("REPLACE")==-1) {
        		String newText = view.getViewText();
        		newText = newText.substring(0, newText.toUpperCase().indexOf("CREATE")+6)+" OR REPLACE"+newText.substring(newText.indexOf("CREATE")+6);
        		view.setViewText(newText);
        	}
        }else {
        	//删除replace关键字的语句
        	if(view.getViewText().toUpperCase().indexOf("REPLACE")!=-1) {
        		String newText = view.getViewText();
        		newText = newText.substring(0, newText.toUpperCase().indexOf("CREATE")+6)+newText.substring(newText.indexOf("REPLACE")+7);
        		view.setViewText(newText);
        	}
        }
        if(force) {
        	//增加force关键字的语句
        	if(view.getViewText().toUpperCase().indexOf("FORCE")==-1) {
        		String newText = view.getViewText();
        		int index1 = newText.indexOf("CREATE");
        		int index2 = newText.indexOf("REPLACE");
        		if(index2!=-1) {
        			newText = newText.substring(0, index2+7)+" FORCE"+newText.substring(index2+7);
        		}else {
        			newText = newText.substring(0, index1+6)+" FORCE"+newText.substring(index1+6);
        		}
        		view.setViewText(newText);
        	}
        }else {
        	//删除force关键字
        	if(view.getViewText().toUpperCase().indexOf("FORCE")!=-1) {
        		String newText = view.getViewText();
        		int index1 = newText.indexOf("CREATE");
        		int index2 = newText.indexOf("REPLACE");
        		if(index2!=-1) {
        			newText = newText.substring(0, index2+7)+newText.substring(newText.indexOf("FORCE")+5);
        		}else {
        			newText = newText.substring(0, index1+6)+newText.substring(newText.indexOf("FORCE")+5);
        		}
        		view.setViewText(newText);
        	}
        }
        if(actions.size()!=0) {
        	actions.remove(0);
        }
        if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct create view sql: "+view.getViewText());
        }
        actions.add(0, new SQLDatabasePersistAction("Create view", view.getViewText()));
        boolean hasComment = command.getProperty("comment") != null;
        if (hasComment) {
        	String sql = "COMMENT ON VIEW " + view.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS '" + view.getComment() + "'";
        	if(XuguConstants.LOG_PRINT_LEVEL<1) {
            	log.info("Xugu Plugin: Construct add view comment sql: "+view.getViewText());
            }
            actions.add(new SQLDatabasePersistAction(
                "Comment table",
                sql));
        }
        System.out.println("VVVView "+view.getName());
    }
    
    static class NewViewDialog extends Dialog {
    	
    	private XuguView view;
        private Text nameText;

        public NewViewDialog(Shell parentShell, XuguSchema dataSource)
        {
            super(parentShell);
            this.view = new XuguView(dataSource, null);  
        }

        public XuguView getView()
        {
            return view;
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
            getShell().setText(XuguMessages.dialog_view_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, XuguMessages.dialog_view_name, null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            return parent;
        }

        @Override
        protected void okPressed()
        {
        	
    		if(XuguUtils.checkString(nameText.getText())) {
    			view.setName(DBObjectNameCaseTransformer.transformObjectName(view, nameText.getText()));
                super.okPressed();
    		}else {
    			XuguWarningDialog warnDialog = new XuguWarningDialog(UIUtils.getActiveWorkbenchShell(), "View name cannot be null");
        		warnDialog.open();
    		}
        }

    }
}

