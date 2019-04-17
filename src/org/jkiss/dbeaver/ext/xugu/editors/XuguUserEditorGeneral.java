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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.jkiss.dbeaver.ext.xugu.model.XuguUserAuthority;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.ControlPropertyCommandListener;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * XuguUserEditorGeneral
 */
public class XuguUserEditorGeneral extends XuguUserEditorAbstract
{
    //static final Log log = Log.getLog(MySQLUserEditorGeneral.class);
    public static final String DEF_PASSWORD_VALUE = "**********"; //$NON-NLS-1$
    public static final String DEF_UNTIL_TIME = "1970-1-1 07:00:00.933";
    
    public static final String[] DEF_DATABASE_AUTHORITY_LIST = {
    		"可创建任何数据库","可修改任何数据库","可删除任何数据库",
    		"可创建任何模式","可修改任何模式","可删除任何模式",
    		"可创建任何表","可修改任何表结构","可删除任何表","可引用任何表","可查询任何表","可插入记录，在任何表","可删除记录，在任何表","可更新记录，在任何表",
    		"可创建任何视图","可修改任何视图结构","可删除任何视图","可查询任何视图","可插入记录，在任何视图","可删除记录，在任何视图","可更新记录，在任何视图",
    		"可创建任何序列值","可修改任何序列值","可删除任何序列值","可读任何序列值","可更新任何序列值","可引用任何序列值",
    		"可创建任何包","可修改任何包","可删除任何包","可执行任何包",
    		"可创建任何存储过程或函数","可修改任何存储过程或函数","可删除任何存储过程或函数","可执行任何存储过程或函数",
    		"可创建任何触发器","可修改任何触发器","可删除任何触发器",
    		"可创建任何索引","可修改任何索引","可删除任何索引",
    		"可创建任何同义词","可修改任何同义词","可删除任何同义词",
    		"可创建任何用户","可修改任何用户","可删除任何用户",
    		"可创建任何定时作业","可修改任何定时作业","可删除任何定时作业",
    		"可创建任何角色","可修改任何角色","可删除任何角色",
    		"可创建任何UDT","可修改任何UDT","可删除任何UDT",
    		"可创建表","可创建视图","可创建序列值","可创建包","可创建存储过程或函数","可创建触发器","可创建索引","可创建同义词","可创建UDT"
    };
    public static final String[] DEF_OBJECT_TYPE_LIST = {
    		"TABLE",
    		"VIEW",
    		"SEQUENCE",
    		"TRIGGER",
    		"PACKAGE",
    		"PROCEDURE"
    };
    
    private PageControl pageControl;
    private boolean isLoaded;
    //private PrivilegeTableControl privTable;
    private boolean newUser;
    private Text userNameText;
    private Text passwordText;
    private Text confirmText;
    private org.eclipse.swt.widgets.List databaseAuthorityList;
    private org.eclipse.swt.widgets.List objectAuthorityList;
    
    private Combo databaseAuthorityCombo;
    
    private Combo objectTypeCombo;
    private Combo schemaCombo;
    private Combo objectCombo;
    private Combo objectAuthorityCombo;
    
    Collection<XuguUserAuthority> authorities;
	ArrayList<String> databaseAuthorities;
	ArrayList<String> objectAuthorities;
    
    private Text roleText;
    private Combo roleCombo;
    private Button addRole;
    private Button removeRole;
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
        Composite container = UIUtils.createPlaceholder(pageControl, 4, 5);
        GridData gd = new GridData(GridData.FILL_BOTH);
        container.setLayoutData(gd);

        newUser = !getDatabaseObject().isPersisted();
        
        Composite loginGroup = UIUtils.createControlGroup(container, "User Properties", 2, GridData.VERTICAL_ALIGN_BEGINNING, 200);
        loginGroup.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        Composite loginGroup2 = UIUtils.createControlGroup(container, "Database Properties", 1, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_BOTH, 250);
        Composite loginGroup3 = UIUtils.createControlGroup(container, "Object Properties", 1, GridData.VERTICAL_ALIGN_BEGINNING, 250);
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
        
        roleCombo = UIUtils.createLabelCombo(loginGroup, XuguMessages.editors_user_editor_general_label_role_list, 0);
        addRole = UIUtils.createPushButton(loginGroup, XuguMessages.editors_user_editor_general_label_add_role, null);
        removeRole = UIUtils.createPushButton(loginGroup, XuguMessages.editors_user_editor_general_label_remove_role, null);
        
        //不允许手动修改角色列表文本框
        roleText = UIUtils.createLabelText(loginGroup, XuguMessages.editors_user_editor_general_label_role_choosen, "");
        ControlPropertyCommandListener.create(this, roleText, UserPropertyHandler.ROLE_LIST);
        roleText.setEnabled(false);
        
        if(newUser) {
        	//无角色信息则禁用角色组件
        	if(getDatabaseObject().getRoleList()!=null) {
        		String[] roles = getDatabaseObject().getRoleList().split(",");
                if(roles!=null && !"".equals(roles[0])) {
                	for(int i=0; i<roles.length; i++) {
                    	roleCombo.add(roles[i]);
                    }
                }else {
                	roleCombo.setEnabled(false);
                	roleText.setEnabled(false);
                	addRole.setEnabled(false);
                	removeRole.setEnabled(false);
                }
        	}else {
            	roleCombo.setEnabled(false);
            	roleText.setEnabled(false);
            	addRole.setEnabled(false);
            	removeRole.setEnabled(false);
            }
        }
        
        addRole.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				String text = roleText.getText();
				String newRole = roleCombo.getText();
				//追加新的角色 前提是新角色名不存在于角色文本框内容中
				if(text!=null && !"".equals(text)) {
					if(text.indexOf(newRole)==-1) {
						text += ","+roleCombo.getText();
					}
				}else {
					text = roleCombo.getText();
				}
				roleText.setText(text);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				System.out.println("in here?");
			}	
        });
        removeRole.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				String text = roleText.getText();
				String newRole = roleCombo.getText();
				//删除已有角色 前提是新角色名存在于角色文本框内容中
				if(text!=null && !"".equals(text)) {
					int index;
					if((index = text.indexOf(newRole))!=-1) {
						text = text.substring(0, index)+text.substring(index+newRole.length());
						//处理首尾逗号
						if(text.indexOf(",")==0) {
							text = text.substring(1);
						}else if(text.lastIndexOf(",")==text.length()-1) {
							text = text.substring(0, text.length()-1);
						}
						//处理位于中间的逗号
						text.replaceAll(",,", ",");
					}
				}
				roleText.setText(text);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				System.out.println("in here?");
			}	
        });
        
        
        //暂时禁止修改用户锁定及口令失效 无论是否新建用户(check类型数据无法被PropertyHandler识别)
        lockCheck.setEnabled(false);
    	expireCheck.setEnabled(false);
        
    	if(!newUser) {
    		roleCombo.setEnabled(false);
    		addRole.setEnabled(false);
    		removeRole.setEnabled(false);
    		roleText.setVisible(false);
    		roleCombo.setVisible(false);
    		addRole.setVisible(false);
    		removeRole.setVisible(false);
    	}
    	
    	//权限处理
    	{
    		//加载用户中的权限信息并分为库级权限和对象级权限两类
    		authorities = getDatabaseObject().getUserAuthorities();
    		databaseAuthorities = new ArrayList<>();
    		objectAuthorities = new ArrayList<>();
    		if(authorities!=null) {
    			Iterator<XuguUserAuthority> it = authorities.iterator();
    			XuguUserAuthority authority;
    			while(it.hasNext()) {
    				authority = it.next();
    				if(authority.isDatabase()) {
    					databaseAuthorities.add(authority.getName());
    				}else {
    					objectAuthorities.add(authority.getName());
    				}
    			}
    		}
    		databaseAuthorityCombo = UIUtils.createLabelCombo(loginGroup2, "Database Authority", 0);
    		for(int i=0; i<DEF_DATABASE_AUTHORITY_LIST.length; i++) {
    			databaseAuthorityCombo.add(DEF_DATABASE_AUTHORITY_LIST[i]);
    		}
    		databaseAuthorityList = new org.eclipse.swt.widgets.List(loginGroup2, SWT.V_SCROLL|SWT.MULTI);
    		databaseAuthorityList.setLayoutData(gd);
    		if(databaseAuthorities!=null) {
    			for(int i=0, l=databaseAuthorities.size(); i<l; i++) {
    				databaseAuthorityList.add(databaseAuthorities.get(i));
    			}
    		}
    		databaseAuthorityList.setParent(loginGroup2);
    		ControlPropertyCommandListener.create(this, databaseAuthorityList, UserPropertyHandler.DATABASE_AUTHORITY);
    		Button addDatabaseAuthority = UIUtils.createPushButton(loginGroup2, "Grant", null);
    		addDatabaseAuthority.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    		Button removeDatabaseAuthority = UIUtils.createPushButton(loginGroup2, "Revoke", null);
    		removeDatabaseAuthority.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    		addDatabaseAuthority.addSelectionListener(new SelectionListener() {
    			@Override
				public void widgetSelected(SelectionEvent e) {
    				String authority = databaseAuthorityCombo.getText();
    				//先判断list文本框中是否已有，已有则不添加
    				String[] nowItems = databaseAuthorityList.getItems();
    				boolean hasItem = false;
    				for(int i=0, l=nowItems.length; i<l; i++) {
    					if(nowItems[i].equals(authority)) {
    						hasItem = true;
    						break;
    					}
    				}
    				if(!hasItem) {
    					databaseAuthorityList.add(authority);
    				}
    				//全部选中
    	        	databaseAuthorityList.selectAll();
    	        	//激活修改监听
    				databaseAuthorityList.notifyListeners(SWT.Modify, null);
    				databaseAuthorityList.deselectAll();
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
    		});
    		removeDatabaseAuthority.addSelectionListener(new SelectionListener() {
    			@Override
				public void widgetSelected(SelectionEvent e) {
    				String authority = databaseAuthorityCombo.getText();
    				//将下拉框中选中的权限从列表框中删除
    				int index=databaseAuthorityList.indexOf(authority);
    				if(index!=-1) {
    					databaseAuthorityList.remove(index);
    				}
    				//全部选中
    	        	databaseAuthorityList.selectAll();
    	        	//激活修改监听
    				databaseAuthorityList.notifyListeners(SWT.Modify, null);
    				databaseAuthorityList.deselectAll();
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
    		});
    		
    		//对象级权限处理
    		objectTypeCombo = UIUtils.createLabelCombo(loginGroup3, "Object Authority", 0);
    		for(int i=0, l=DEF_OBJECT_TYPE_LIST.length; i<l; i++) {
    			objectTypeCombo.add(DEF_OBJECT_TYPE_LIST[i]);
    		}
    		schemaCombo = UIUtils.createLabelCombo(loginGroup3, "Schema List", 0);
    		objectCombo = UIUtils.createLabelCombo(loginGroup3, "Object List", 0);
    		objectTypeCombo.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String type = objectCombo.getText();
					switch(type) {
					case "TABLE":
						
						break;
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
    		});
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
            if (newUser && getDatabaseObject().isPersisted()) {
                newUser = false;
                UIUtils.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        userNameText.setEditable(true);
                        passwordText.setEditable(true);
                    }
                });
            }
        }
    }
    
}
