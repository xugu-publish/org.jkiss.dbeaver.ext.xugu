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
package org.jkiss.dbeaver.ext.xugu.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
//import org.jkiss.dbeaver.ext.xugu.controls.PrivilegeTableControl;
//import org.jkiss.dbeaver.ext.xugu.edit.XuguCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.xugu.edit.UserPropertyHandler;
//import org.jkiss.dbeaver.ext.xugu.model.XuguGrant;
//import org.jkiss.dbeaver.ext.xugu.model.XuguPrivilege;
import org.jkiss.dbeaver.ext.xugu.model.XuguUser;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.ControlPropertyCommandListener;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

/**
 * XuguUserEditorGeneral
 */
public class XuguUserEditorGeneral extends XuguUserEditorAbstract
{
    //static final Log log = Log.getLog(MySQLUserEditorGeneral.class);
    public static final String DEF_PASSWORD_VALUE = "**********"; //$NON-NLS-1$
    public static final String DEF_UNTIL_TIME = "1970-1-1 07:00:00.933";

    private PageControl pageControl;
    private boolean isLoaded;
    //private PrivilegeTableControl privTable;
    private boolean newUser;
    private Text userNameText;
    private Text passwordText;
    private Text confirmText;
    private Button lockCheck;
    private Button expireCheck;
    private Text timeText;
    private String userName="";
    private String password="";
    private String untilTime="";
    private boolean lockFlag;
    private boolean expireFlag;
    //private Text hostText;
    private CommandListener commandlistener;

    @Override
    public void createPartControl(Composite parent) 
    {
        pageControl = new PageControl(parent);

        Composite container = UIUtils.createPlaceholder(pageControl, 2, 5);
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        container.setLayoutData(gd);

        newUser = !getDatabaseObject().isPersisted();
        
        Composite loginGroup = UIUtils.createControlGroup(container, XuguMessages.editors_user_editor_general_group_login, 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 200);

        //创建新用户时使用默认数据 修改用户时则使用当前用户数据 对密码做特殊处理 
        password = newUser ? "" : DEF_PASSWORD_VALUE;
        userName = newUser ? "" : getDatabaseObject().getName();
        untilTime = newUser ? DEF_UNTIL_TIME:getDatabaseObject().getUntil_time().toString();
        lockFlag = newUser ? false:getDatabaseObject().isLocked();
        expireFlag = newUser ? false:getDatabaseObject().isExpired();
        
        userNameText = UIUtils.createLabelText(loginGroup, XuguMessages.editors_user_editor_general_label_user_name, userName);
        ControlPropertyCommandListener.create(this, userNameText, UserPropertyHandler.NAME);
        
        passwordText = UIUtils.createLabelText(loginGroup, XuguMessages.editors_user_editor_general_label_password, password, SWT.BORDER | SWT.PASSWORD);
        ControlPropertyCommandListener.create(this, passwordText, UserPropertyHandler.PASSWORD);

        confirmText = UIUtils.createLabelText(loginGroup, XuguMessages.editors_user_editor_general_label_confirm, password, SWT.BORDER | SWT.PASSWORD);
        ControlPropertyCommandListener.create(this, confirmText, UserPropertyHandler.PASSWORD_CONFIRM);
        
        lockCheck =  UIUtils.createLabelCheckbox(loginGroup, XuguMessages.editors_user_editor_general_label_locked, lockFlag);
        ControlPropertyCommandListener.create(this, lockCheck, UserPropertyHandler.LOCKED);
        
        expireCheck = UIUtils.createLabelCheckbox(loginGroup, XuguMessages.editors_user_editor_general_label_pwd_expired, expireFlag);
        ControlPropertyCommandListener.create(this, expireCheck, UserPropertyHandler.EXPIRED);
        
        timeText = UIUtils.createLabelText(loginGroup, XuguMessages.editors_user_editor_general_label_valid_until, untilTime);
        ControlPropertyCommandListener.create(this, timeText, UserPropertyHandler.UNTIL_TIME);
        
        //暂时禁止修改用户锁定及口令失效
        if(!newUser) {
        	lockCheck.setEnabled(false);
        	expireCheck.setEnabled(false);
        }
        
        pageControl.createProgressPanel();

        commandlistener = new CommandListener();
        getEditorInput().getCommandContext().addCommandListener(commandlistener);
    }

    @Override
    public void dispose()
    {
    	System.out.println("dispose ");
        if (commandlistener != null) {
            getEditorInput().getCommandContext().removeCommandListener(commandlistener);
        }
        super.dispose();
    }

    @Override
    public void activatePart()
    {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        
        LoadingJob.createService(
            new DatabaseLoadService<List<String>>("test", getExecutionContext()) {
				@Override
				public List<String> evaluate(DBRProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					// TODO Auto-generated method stub
					return null;
				}
            },
            pageControl.createLoadVisualizer())
            .schedule();
    }

    @Override
    protected PageControl getPageControl()
    {
        return pageControl;
    }

    //@Override
//    protected void processGrants(List<MySQLGrant> grants)
//    {
//        privTable.fillGrants(grants);
//    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        // do nothing
    }

    private class PageControl extends UserPageControl {
        public PageControl(Composite parent) {
            super(parent);
        }
        public ProgressVisualizer<List<String>> createLoadVisualizer() {
            return new ProgressVisualizer<List<String>>() {
                @Override
                public void completeLoading(List<String> privs) {
                    super.completeLoading(privs);
//                    privTable.fillPrivileges(privs);
//                    loadGrants();
                }
            };
        }

    }

    private class CommandListener extends DBECommandAdapter {
        @Override
        public void onSave()
        {
        	System.out.println("save and out?");
            if (newUser && getDatabaseObject().isPersisted()) {
                newUser = false;
                UIUtils.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        userNameText.setEditable(true);
                        passwordText.setEditable(true);
                        //hostText.setEditable(false);
                    }
                });
            }
        }
    }
    
}
