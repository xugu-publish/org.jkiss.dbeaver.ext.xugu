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
    public static final String[] DEF_TABLE_AUTHORITY_LIST= {
    		"可修改表结构","可删除表","可引用表","可读表","可插入记录，在表","可删除记录，在表","可更新记录，在表"
    };
    public static final String[] DEF_VIEW_AUTHORITY_LIST= {
    		"可修改视图结构","可删除视图","可读视图","可插入记录，在视图","可删除记录，在视图","可更新记录，在视图"
    };
    public static final String[] DEF_SEQUENCE_AUTHORITY_LIST= {
    		"可修改序列值","可删除序列值","可读序列值","可更新序列值","可引用序列值"
    };
    public static final String[] DEF_PACKAGE_AUTHORITY_LIST= {
    		"可修改包","可删除包","可执行包"
    };
    public static final String[] DEF_PROCEDURE_AUTHORITY_LIST= {
    		"可修改存储过程或函数","可删除存储过程或函数","可执行存储过程或函数"
    };
    public static final String[] DEF_TRIGGER_AUTHORITY_LIST= {
    		"可修改触发器","可删除触发器"
    };
    public static final String[] DEF_COLUMN_AUTHORITY_LIST= {
    		"可读列","可更新列"
    };
    
    public static final String[] DEF_OBJECT_TYPE_LIST = {
    		"TABLE",
    		"VIEW",
    		"SEQUENCE",
    		"TRIGGER",
    		"PACKAGE",
    		"PROCEDURE",
    		"COLUMN"
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
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        container.setLayoutData(gd);
        container.setSize(400, 200);

        newUser = !getDatabaseObject().isPersisted();
        CTabFolder cf1=new CTabFolder(container,0);
        CTabItem ti1 = new CTabItem(cf1, 1);
        CTabItem ti2 = new CTabItem(cf1, 2);
        CTabItem ti3 = new CTabItem(cf1, 3);
        Composite loginGroup = UIUtils.createControlGroup(cf1, "User Authorities", 2, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        loginGroup.setSize(200, 200);
        Composite loginGroup2 = UIUtils.createControlGroup(cf1, "Database Authorities", 1, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        loginGroup2.setSize(200, 200);
        Composite loginGroup3 = UIUtils.createControlGroup(cf1, "Object Authorities", 2, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        Composite subloginGroupLeft = UIUtils.createControlGroup(loginGroup3, "First Level", 2, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        Composite subloginGroupRight = UIUtils.createControlGroup(loginGroup3, "Second Level", 2, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        loginGroup3.setSize(200, 200);
        ti1.setControl(loginGroup);
        ti1.setText("User Properties");
        ti2.setControl(loginGroup2);
        ti2.setText("Database Authorities");
        ti3.setControl(loginGroup3);
        ti3.setText("Object Authorities");
        cf1.setSelection(1);
        //创建新用户时使用默认数据 修改用户时则使用当前用户数据 对密码做特殊处理 
        password = newUser ? "" : DEF_PASSWORD_VALUE;
        userName = newUser ? "" : getDatabaseObject().getName();
        
        userNameText = UIUtils.createLabelText(loginGroup, XuguMessages.editors_user_editor_general_label_user_name, userName);
        ControlPropertyCommandListener.create(this, userNameText, RolePropertyHandler.NAME);
        
    	
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
    		for(int i=0; i<DEF_DATABASE_AUTHORITY_LIST.length; i++) {
    			databaseAuthorityCombo.add(DEF_DATABASE_AUTHORITY_LIST[i]);
    		}
    		databaseAuthorityList = new org.eclipse.swt.widgets.List(loginGroup2, SWT.V_SCROLL|SWT.MULTI);
    		databaseAuthorityList.setLayoutData(new GridData(200,180));
    		if(databaseAuthorities!=null) {
    			for(int i=0, l=databaseAuthorities.size(); i<l; i++) {
    				databaseAuthorityList.add(databaseAuthorities.get(i));
    			}
    		}
    		databaseAuthorityList.setParent(loginGroup2);
    		ControlPropertyCommandListener.create(this, databaseAuthorityList, RolePropertyHandler.DATABASE_AUTHORITY);
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
    		
    		//二级对象处理
    		Composite subGroup2 = UIUtils.createControlGroup(subloginGroupRight, "", 2, SWT.NO_TRIM, 200);
    		GridData griddata = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
    		subGroup2.setLayoutData(griddata);
    		subObjectTypeCombo = UIUtils.createLabelCombo(subGroup2, "SubObject Type", 0);
    		subObjectCombo = UIUtils.createLabelCombo(subGroup2, "SubObject List", 0);
    		subObjectAuthorityList = new org.eclipse.swt.widgets.List(loginGroup3, SWT.V_SCROLL|SWT.MULTI);
    		subObjectAuthorityList.setLayoutData(new GridData(150,200));
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
						authorityList = DEF_TRIGGER_AUTHORITY_LIST;
						break;
					case "COLUMN":
						authorityList = DEF_COLUMN_AUTHORITY_LIST;
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
    		Composite subGroup = UIUtils.createControlGroup(subloginGroupLeft, "", 2, SWT.NO_TRIM, 200);
    		subGroup.setLayoutData(griddata);
    		//模式下拉框
    		schemaCombo = UIUtils.createLabelCombo(subGroup, "Schema List", 0);
    		Collection<XuguSchema> schemaList = getDatabaseObject().getDataSource().schemaCache.getCachedObjects();
    		Iterator<XuguSchema> it = schemaList.iterator();
    		while(it.hasNext()) {
    			schemaCombo.add(it.next().getName());
    		}
    		//对象类型下拉框
    		objectTypeCombo = UIUtils.createLabelCombo(subGroup, "Object Type", 0);
    		for(int i=0, l=DEF_OBJECT_TYPE_LIST.length; i<l; i++) {
    			objectTypeCombo.add(DEF_OBJECT_TYPE_LIST[i]);
    		}
    		//对象下拉框
    		objectCombo = UIUtils.createLabelCombo(subGroup, "Object List", 0);
    		//可选对象权限下拉框(包括全部一二级权限)
    		objectAuthorityCombo = UIUtils.createLabelCombo(loginGroup3, "Authority", 0);
    		
    		//已选对象权限列表框
    		objectAuthorityList = new org.eclipse.swt.widgets.List(subloginGroupLeft, SWT.V_SCROLL|SWT.MULTI);
    		objectAuthorityList.setLayoutData(new GridData(150,200));
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
						authorityList = DEF_TABLE_AUTHORITY_LIST;
						break;
					case "VIEW":
						authorityList = DEF_VIEW_AUTHORITY_LIST;
						break;
					case "SEQUENCE":
						authorityList = DEF_SEQUENCE_AUTHORITY_LIST;
						break;
					case "PACKAGE":
						authorityList = DEF_PACKAGE_AUTHORITY_LIST;
						break;
					case "PROCEDURE":
						authorityList = DEF_PROCEDURE_AUTHORITY_LIST;
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

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
    		});
    		removeObjectAuthority.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String authority = objectAuthorityCombo.getText();
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
                        passwordText.setEditable(true);
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
