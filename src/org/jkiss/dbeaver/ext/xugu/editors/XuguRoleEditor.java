package org.jkiss.dbeaver.ext.xugu.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.edit.RolePropertyHandler;
import org.jkiss.dbeaver.ext.xugu.edit.UserPropertyHandler;
import org.jkiss.dbeaver.ext.xugu.editors.XuguUserEditorAbstract.UserPageControl;
import org.jkiss.dbeaver.ext.xugu.model.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguRole;
import org.jkiss.dbeaver.ext.xugu.model.XuguRoleAuthority;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguUserAuthority;
import org.jkiss.dbeaver.ext.xugu.views.XuguWarningDialog;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl.ProgressVisualizer;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.ControlPropertyCommandListener;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;

public class XuguRoleEditor extends AbstractDatabaseObjectEditor<XuguRole>{
    
	private PageControl pageControl;
    private boolean isLoaded;
    //private PrivilegeTableControl privTable;
    private boolean newUser;
    private Text userNameText;
    private org.eclipse.swt.widgets.List databaseAuthorityList;
    private org.eclipse.swt.widgets.List objectAuthorityList;
    private org.eclipse.swt.widgets.List subObjectAuthorityList;
    
    private Combo databaseAuthorityCombo;
    
    private Combo objectTypeCombo;
    private Combo schemaCombo;
    private Combo objectCombo;
    private Combo objectAuthorityCombo;
    private Combo subObjectTypeCombo;
    private Combo subObjectCombo;
    
    Collection<XuguRoleAuthority> authorities;
	ArrayList<String> databaseAuthorities;
	ArrayList<String> objectAuthorities;
    
    //private Text hostText;
    private CommandListener commandlistener;

    @Override
    public void createPartControl(Composite parent) 
    {
    	
        pageControl = new PageControl(parent);
        Composite container = UIUtils.createPlaceholder(pageControl, 4, 5);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        container.setLayoutData(gd);
        container.setSize(400, 300);

        newUser = !getDatabaseObject().isPersisted();
        CTabFolder cf1=new CTabFolder(container,0);
        CTabItem ti1 = new CTabItem(cf1, 1);
        CTabItem ti2 = new CTabItem(cf1, 2);
        CTabItem ti3 = new CTabItem(cf1, 3);
        Composite loginGroup = UIUtils.createControlGroup(cf1, "Role Properties", 2, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        loginGroup.setSize(200, 200);
        Composite loginGroup2 = UIUtils.createControlGroup(cf1, "Database Authorities", 1, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        loginGroup2.setSize(200, 200);
        Composite loginGroup3 = UIUtils.createControlGroup(cf1, "", 2, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        Composite subloginGroupLeft = UIUtils.createControlGroup(loginGroup3, "First Level", 1, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        Composite subloginGroupRight = UIUtils.createControlGroup(loginGroup3, "Second Level", 1, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        subloginGroupLeft.setLayoutData(new GridData(400,250));
        subloginGroupRight.setLayoutData(new GridData(400,250));
        loginGroup3.setSize(860, 250);
        ti1.setControl(loginGroup);
        ti2.setControl(loginGroup2);
        ti2.setText("Database Authorities");
        ti3.setControl(loginGroup3);
        ti3.setText("Object Authorities");
        cf1.setSelection(1);
    	
    	//权限处理
    	{
    		//加载用户中的权限信息并分为库级权限和对象级权限两类 对象权限又分为两个级别
    		authorities = getDatabaseObject().getRoleDatabaseAuthorities();
    		databaseAuthorities = new ArrayList<>();
    		objectAuthorities = new ArrayList<>();
    		if(authorities!=null) {
    			Iterator<XuguRoleAuthority> it = authorities.iterator();
    			XuguRoleAuthority authority;
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
    		for(int i=0; i<XuguConstants.DEF_DATABASE_AUTHORITY_LIST.length; i++) {
    			databaseAuthorityCombo.add(XuguConstants.DEF_DATABASE_AUTHORITY_LIST[i]);
    		}
    		databaseAuthorityCombo.setLayoutData(new GridData(400, 20));
    		Button addDatabaseAuthority = UIUtils.createPushButton(loginGroup2, "Grant", null);
    		addDatabaseAuthority.setLayoutData(new GridData(420, 30));
    		Button removeDatabaseAuthority = UIUtils.createPushButton(loginGroup2, "Revoke", null);
    		removeDatabaseAuthority.setLayoutData(new GridData(420, 30));
    		databaseAuthorityList = new org.eclipse.swt.widgets.List(loginGroup2, SWT.V_SCROLL|SWT.MULTI);
    		databaseAuthorityList.setLayoutData(new GridData(400,180));
    		if(databaseAuthorities!=null) {
    			for(int i=0, l=databaseAuthorities.size(); i<l; i++) {
    				databaseAuthorityList.add(databaseAuthorities.get(i));
    			}
    		}
    		databaseAuthorityList.setParent(loginGroup2);
    		ControlPropertyCommandListener.create(this, databaseAuthorityList, RolePropertyHandler.DATABASE_AUTHORITY);
    		addDatabaseAuthority.addSelectionListener(new SelectionListener() {
    			@Override
				public void widgetSelected(SelectionEvent e) {
    				String authority = databaseAuthorityCombo.getText();
    				if(authority!=null && authority.length()!=0) {
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
    				if(authority!=null && authority.length()!=0) {
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
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
    		});
    		
    		//二级对象处理
    		subObjectTypeCombo = UIUtils.createLabelCombo(subloginGroupRight, "SubObject Type", 0);
    		subObjectTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    		subObjectCombo = UIUtils.createLabelCombo(subloginGroupRight, "SubObject List", 0);
    		subObjectCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    		subObjectAuthorityList = new org.eclipse.swt.widgets.List(subloginGroupRight, SWT.V_SCROLL|SWT.MULTI);
    		subObjectAuthorityList.setLayoutData(new GridData(370,170));
    		subObjectAuthorityList.setParent(subloginGroupRight);
    		ControlPropertyCommandListener.create(this, subObjectTypeCombo, RolePropertyHandler.SUB_TARGET_TYPE);
    		ControlPropertyCommandListener.create(this, subObjectCombo, RolePropertyHandler.SUB_TARGET_OBJECT);
    		ControlPropertyCommandListener.create(this, subObjectAuthorityList, RolePropertyHandler.SUB_OBJECT_AUTHORITY);
    		subObjectTypeCombo.addSelectionListener(new SelectionListener() {
    			@Override
				public void widgetSelected(SelectionEvent e) {
					String schema = schemaCombo.getText();
    				String type = subObjectTypeCombo.getText();
    				String object = objectCombo.getText();
    				String[] authorityList=null;
    				//加载对象信息
					subObjectCombo.removeAll();
					String objectList = getDatabaseObject().getObjectList(schema, type, object);
					String[] objects = objectList.split(",");
					for(int i=0, l=objects.length; i<l; i++) {
						subObjectCombo.add(objects[i]);
					}
    				switch(type) {
    				case "TRIGGER":
						authorityList = XuguConstants.DEF_TRIGGER_AUTHORITY_LIST;
						break;
					case "COLUMN":
						authorityList = XuguConstants.DEF_COLUMN_AUTHORITY_LIST;
						break;
    				}
    				if(authorityList!=null) {
						objectAuthorityCombo.removeAll();
						for(int i=0, l=authorityList.length; i<l; i++) {
							objectAuthorityCombo.add(authorityList[i]);
						}
					}
    				//清空二级权限列表
    				subObjectAuthorityList.removeAll();
    			}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
    		});
    		//二级对象权限监听
    		subObjectCombo.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String subType = subObjectTypeCombo.getText();
					String schema = schemaCombo.getText();
					String object = objectCombo.getText();
					String subObject = subObjectCombo.getText();
					String keyWord="";
					switch(subType) {
					case "TRIGGER":
						keyWord = "触发器";
						break;
					case "COLUMN":
						keyWord = "列";
						break;
					}
					//从全部对象权限中加载符合条件的已有二级对象权限
					subObjectAuthorityList.removeAll();
					Iterator<String> it = objectAuthorities.iterator();
					while(it.hasNext()) {
						String temp = it.next();
						if(temp.contains(keyWord) && temp.contains("\""+schema+"\".\""+object+"\".\""+subObject+"\"")) {
							subObjectAuthorityList.add(temp.substring(0, temp.indexOf(":")));
						}
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
    		});
    		
    		//一级对象级权限处理
//    		Composite subGroup = UIUtils.createControlGroup(subloginGroupLeft, "", 2, SWT.NO_TRIM, 400);
//    		subGroup.setLayoutData(new GridData(400,200));
    		//模式下拉框
    		schemaCombo = UIUtils.createLabelCombo(subloginGroupLeft, "Schema List", 0);
    		Collection<XuguSchema> schemaList = getDatabaseObject().getDataSource().schemaCache.getCachedObjects();
    		Iterator<XuguSchema> it = schemaList.iterator();
    		while(it.hasNext()) {
    			schemaCombo.add(it.next().getName());
    		}
    		//对象类型下拉框
    		objectTypeCombo = UIUtils.createLabelCombo(subloginGroupLeft, "Object Type", 0);
    		for(int i=0, l=XuguConstants.DEF_OBJECT_TYPE_LIST.length; i<l; i++) {
    			objectTypeCombo.add(XuguConstants.DEF_OBJECT_TYPE_LIST[i]);
    		}
    		//对象下拉框
    		objectCombo = UIUtils.createLabelCombo(subloginGroupLeft, "Object List", 0);
    		//可选对象权限下拉框(包括全部一二级权限)
    		objectAuthorityCombo = UIUtils.createLabelCombo(loginGroup3, "Authority", 0);
    		//已选对象权限列表框
    		objectAuthorityList = new org.eclipse.swt.widgets.List(subloginGroupLeft, SWT.V_SCROLL|SWT.MULTI);
    		objectAuthorityList.setLayoutData(new GridData(370,150));
    		objectAuthorityList.setParent(subloginGroupLeft);
    		ControlPropertyCommandListener.create(this, objectAuthorityList, RolePropertyHandler.OBJECT_AUTHORITY);
    		ControlPropertyCommandListener.create(this, schemaCombo, RolePropertyHandler.TARGET_SCHEMA);
    		ControlPropertyCommandListener.create(this, objectCombo, RolePropertyHandler.TARGET_OBJECT);
    		ControlPropertyCommandListener.create(this, objectTypeCombo, RolePropertyHandler.TARGET_TYPE);

    		Button addObjectAuthority = UIUtils.createPushButton(loginGroup3, "Grant", null);
    		addObjectAuthority.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    		Button removeObjectAuthority = UIUtils.createPushButton(loginGroup3, "Revoke", null);
    		removeObjectAuthority.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    		SelectionListener itemChangeListener = new SelectionListener() {
    			@Override
				public void widgetSelected(SelectionEvent e) {
					String type = objectTypeCombo.getText();
					String schema = schemaCombo.getText();
					//加载对象信息
					objectCombo.removeAll();
					String objectList = getDatabaseObject().getObjectList(schema, type, "");
					String[] objects = objectList.split(",");
					
					for(int i=0, l=objects.length; i<l; i++) {
						objectCombo.add(objects[i]);
					}
					//加载权限信息
					String[] authorityList=null;
					switch(type) {
					case "TABLE":
						authorityList = XuguConstants.DEF_TABLE_AUTHORITY_LIST;
						break;
					case "VIEW":
						authorityList = XuguConstants.DEF_VIEW_AUTHORITY_LIST;
						break;
					case "SEQUENCE":
						authorityList = XuguConstants.DEF_SEQUENCE_AUTHORITY_LIST;
						break;
					case "PACKAGE":
						authorityList = XuguConstants.DEF_PACKAGE_AUTHORITY_LIST;
						break;
					case "PROCEDURE":
						authorityList = XuguConstants.DEF_PROCEDURE_AUTHORITY_LIST;
						break;
					}
					if(authorityList!=null) {
						objectAuthorityCombo.removeAll();
						for(int i=0, l=authorityList.length; i<l; i++) {
							objectAuthorityCombo.add(authorityList[i]);
						}
					}
					//清空一级权限列表
					objectAuthorityList.removeAll();
					//当一级对象改变时二级对象权限随之清空
					subObjectCombo.removeAll();
					subObjectTypeCombo.removeAll();
					subObjectAuthorityList.removeAll();
					//然后根据一级对象类型重新加载二级对象权限类型
					if(type.equals("TABLE") || type.equals("VIEW")) {
						subObjectTypeCombo.add("COLUMN");
						subObjectTypeCombo.add("TRIGGER");
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
    		};
    		objectTypeCombo.addSelectionListener(itemChangeListener);
    		schemaCombo.addSelectionListener(itemChangeListener);
    		//一级对象权限监听
    		objectCombo.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String type = objectTypeCombo.getText();
					String schema = schemaCombo.getText();
					String object = objectCombo.getText();
					String keyWord="";
					switch(type) {
					case "TABLE":
						keyWord = "表";
						break;
					case "VIEW":
						keyWord = "视图";
						break;
					case "SEQUENCE":
						keyWord = "序列值";
						break;
					case "PACKAGE":
						keyWord = "包";
						break;
					case "PROCEDURE":
						keyWord = "存储过程或函数";
						break;
					}
					//加载符合条件的已有权限
					objectAuthorityList.removeAll();
					Iterator<String> it = objectAuthorities.iterator();
					while(it.hasNext()) {
						String temp = it.next();
						if(temp.contains(keyWord) && temp.contains("\""+schema+"\".\""+object+"\"")) {
							objectAuthorityList.add(temp.substring(0, temp.indexOf(":")));
						}
						
					}
					//当一级对象改变时二级对象权限随之清空
					subObjectCombo.removeAll();
					subObjectTypeCombo.removeAll();
					subObjectAuthorityList.removeAll();
					//然后根据一级对象重新加载二级对象权限类型
					if(type.equals("TABLE") || type.equals("VIEW")) {
						subObjectTypeCombo.add("COLUMN");
						subObjectTypeCombo.add("TRIGGER");
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
    		});
    		addObjectAuthority.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String authority = objectAuthorityCombo.getText();
					if(authority!=null && authority.length()!=0) {
						String[] authorityList = null;
						//区分一级对象还是二级对象
						boolean isFirstLevel = true;
						if(authority!=null && (authority.contains("列")||authority.contains("触发器"))){
							authorityList = subObjectAuthorityList.getItems();
							isFirstLevel = false;
						}else {
							authorityList = objectAuthorityList.getItems();
						}
						boolean hasAuthority = false;
						for(int i=0, l=authorityList.length; i<l; i++) {
							if(authority.equals(authorityList[i])) {
								hasAuthority = true;
								break;
							}
						}
						if(isFirstLevel) {
							if(!hasAuthority) {
								objectAuthorityList.add(authority);
							}
							objectAuthorityList.selectAll();
							objectAuthorityList.notifyListeners(SWT.Modify, null);
							objectAuthorityList.deselectAll();
						}else {
							if(!hasAuthority) {
								subObjectAuthorityList.add(authority);
							}
							subObjectAuthorityList.selectAll();
							subObjectAuthorityList.notifyListeners(SWT.Modify, null);
							subObjectAuthorityList.deselectAll();
						}
					}
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
    		});
    		removeObjectAuthority.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String authority = objectAuthorityCombo.getText();
					if(authority!=null && authority.length()!=0) {
						String[] authorityList = null;
						//区分一级对象还是二级对象
						boolean isFirstLevel = true;
						if(authority!=null && (authority.contains("列")||authority.contains("触发器"))){
							authorityList = subObjectAuthorityList.getItems();
							isFirstLevel = false;
						}else {
							authorityList = objectAuthorityList.getItems();
						}
						if(isFirstLevel) {
							boolean hasAuthority = false;
							for(int i=0, l=authorityList.length; i<l; i++) {
								if(authority.equals(authorityList[i])) {
									hasAuthority = true;
									break;
								}
							}
							if(hasAuthority) {
								objectAuthorityList.remove(authority);
							}
							objectAuthorityList.selectAll();
							objectAuthorityList.notifyListeners(SWT.Modify, null);
							objectAuthorityList.deselectAll();
						}else {
							boolean hasAuthority = false;
							for(int i=0, l=authorityList.length; i<l; i++) {
								if(authority.equals(authorityList[i])) {
									hasAuthority = true;
									break;
								}
							}
							if(hasAuthority) {
								subObjectAuthorityList.remove(authority);
							}
							subObjectAuthorityList.selectAll();
							subObjectAuthorityList.notifyListeners(SWT.Modify, null);
							subObjectAuthorityList.deselectAll();
						}
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

    protected PageControl getPageControl()
    {
        return pageControl;
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        // do nothing
    }

    private class PageControl extends ObjectEditorPageControl {
        public PageControl(Composite parent) {
            super(parent, SWT.NONE, XuguRoleEditor.this);
        }
        public ProgressVisualizer<List<String>> createLoadVisualizer() {
            return new ProgressVisualizer<List<String>>() {
                @Override
                public void completeLoading(List<String> privs) {
                    super.completeLoading(privs);
//	                    privTable.fillPrivileges(privs);
//	                    loadGrants();
                }
            };
        }
        @Override
        public void fillCustomActions(IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);
            DatabaseEditorUtils.contributeStandardEditorActions(getSite(), contributionManager);
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
                    }
                });
            }
        }
    }

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}
    
}
