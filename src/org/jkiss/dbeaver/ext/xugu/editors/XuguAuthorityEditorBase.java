package org.jkiss.dbeaver.ext.xugu.editors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.jkiss.dbeaver.ext.xugu.edit.RolePropertyHandler;
import org.jkiss.dbeaver.ext.xugu.edit.UserPropertyHandler;
import org.jkiss.dbeaver.ext.xugu.model.XuguAuthorityBase;
import org.jkiss.dbeaver.ext.xugu.model.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguGlobalObject;
import org.jkiss.dbeaver.ext.xugu.model.XuguRole;
import org.jkiss.dbeaver.ext.xugu.model.XuguRoleAuthority;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguUser;
import org.jkiss.dbeaver.ext.xugu.model.XuguUserAuthority;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.ControlPropertyCommandListener;

public class XuguAuthorityEditorBase{
	private XuguUserEditorGeneral userEditor;
	private XuguRoleEditor roleEditor;
	private int editorType;
	private Composite parent1;
	private Composite parent2;
	Composite subloginGroupLeft;
	Composite subloginGroupRight;
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
    private Button addDatabaseAuthority;
    private Button removeDatabaseAuthority;
    private Button addObjectAuthority;
    private Button removeObjectAuthority;
    
    Collection<XuguAuthorityBase> authorities;
	ArrayList<String> databaseAuthorities;
	ArrayList<String> objectAuthorities;
	
	//parent1=loginGroup2; parent2=loginGroup3 
	public XuguAuthorityEditorBase(Composite parent1, Composite parent2, int type) {
		this.parent1 = parent1;
		this.parent2 = parent2;
		this.editorType = type;
		databaseAuthorities = new ArrayList<>();
		objectAuthorities = new ArrayList<>();
	}
	
	public void setRoleEditor(XuguRoleEditor editor) {
		this.roleEditor = editor;
	}
	
	public void setUserEditor(XuguUserEditorGeneral editor) {
		this.userEditor = editor;
	}
	
	//加载权限
	public void loadDatabaseAuthorities(ArrayList<String> databaseAuthorities, ArrayList<String> objectAuthorities) {
		this.databaseAuthorities = databaseAuthorities;
		this.objectAuthorities = objectAuthorities;
	}
	
	//加载库级权限视图
	public void loadDatabaseAuthorityView() {
		//加载组件
		databaseAuthorityCombo = UIUtils.createLabelCombo(parent1, "Database Authority", 0);
		databaseAuthorityCombo.setLayoutData(new GridData(400, 20));
		addDatabaseAuthority = UIUtils.createPushButton(parent1, "Grant", null);
		addDatabaseAuthority.setLayoutData(new GridData(420, 30));
		removeDatabaseAuthority = UIUtils.createPushButton(parent1, "Revoke", null);
		removeDatabaseAuthority.setLayoutData(new GridData(420, 30));
		databaseAuthorityList = new org.eclipse.swt.widgets.List(parent1, SWT.V_SCROLL|SWT.MULTI);
		databaseAuthorityList.setLayoutData(new GridData(400,180));
		if(editorType==1) {
			ControlPropertyCommandListener.create(userEditor, databaseAuthorityList, UserPropertyHandler.DATABASE_AUTHORITY);
		}else {
			ControlPropertyCommandListener.create(roleEditor, databaseAuthorityList, RolePropertyHandler.DATABASE_AUTHORITY);	
		}
		//加载库级和对象级权限到组件中
		for(int i=0; i<XuguConstants.DEF_DATABASE_AUTHORITY_LIST.length; i++) {
			databaseAuthorityCombo.add(XuguConstants.DEF_DATABASE_AUTHORITY_LIST[i]);
		}
		if(databaseAuthorities!=null) {
			for(int i=0, l=databaseAuthorities.size(); i<l; i++) {
				databaseAuthorityList.add(databaseAuthorities.get(i));
			}
		}
		//对按钮添加监听事件
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
    	        	//激活相关组件修改监听
    	        	databaseAuthorityList.notifyListeners(SWT.Modify, new Event());
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
    	        	//激活相关组件的修改监听
    				databaseAuthorityList.notifyListeners(SWT.Modify, new Event());
    				databaseAuthorityList.deselectAll();
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});
	}
	
	//加载对象级权限视图
	public void loadObjectAuthorityView(XuguGlobalObject owner) {
		//加载组件
		subloginGroupLeft = UIUtils.createControlGroup(parent2, "First Level", 1, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        subloginGroupRight = UIUtils.createControlGroup(parent2, "Second Level", 1, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        subObjectTypeCombo = UIUtils.createLabelCombo(subloginGroupRight, "SubObject Type", 0);
		subObjectTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		subObjectCombo = UIUtils.createLabelCombo(subloginGroupRight, "SubObject List", 0);
		subObjectCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		subObjectAuthorityList = new org.eclipse.swt.widgets.List(subloginGroupRight, SWT.V_SCROLL|SWT.MULTI);
		subObjectAuthorityList.setLayoutData(new GridData(370,190));
		subObjectAuthorityList.setParent(subloginGroupRight);
		//加载监听
		if(editorType==1) {
			ControlPropertyCommandListener.create(userEditor, subObjectTypeCombo, UserPropertyHandler.SUB_TARGET_TYPE);
    		ControlPropertyCommandListener.create(userEditor, subObjectCombo, UserPropertyHandler.SUB_TARGET_OBJECT);
    		ControlPropertyCommandListener.create(userEditor, subObjectAuthorityList, UserPropertyHandler.SUB_OBJECT_AUTHORITY);
		}else {
    		ControlPropertyCommandListener.create(roleEditor, subObjectTypeCombo, RolePropertyHandler.SUB_TARGET_TYPE);
    		ControlPropertyCommandListener.create(roleEditor, subObjectCombo, RolePropertyHandler.SUB_TARGET_OBJECT);
    		ControlPropertyCommandListener.create(roleEditor, subObjectAuthorityList, RolePropertyHandler.SUB_OBJECT_AUTHORITY);
		}
		subObjectTypeCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String schema = schemaCombo.getText();
				String type = subObjectTypeCombo.getText();
				String object = objectCombo.getText();
				String[] authorityList=null;
				//加载对象信息
				subObjectCombo.removeAll();
				String objectList="";
				if(editorType==1) {
					objectList = ((XuguUser)owner).getObjectList(schema, type, object);
				}
				else {
					objectList = ((XuguRole)owner).getObjectList(schema, type, object);
				}
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
		//模式下拉框
		schemaCombo = UIUtils.createLabelCombo(subloginGroupLeft, "Schema List", 0);
		Collection<XuguSchema> schemaList;
		schemaList = owner.getDataSource().schemaCache.getCachedObjects();
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
		objectAuthorityCombo = UIUtils.createLabelCombo(parent2, "Authority", 0);
		//已选对象权限列表框
		objectAuthorityList = new org.eclipse.swt.widgets.List(subloginGroupLeft, SWT.V_SCROLL|SWT.MULTI);
		objectAuthorityList.setLayoutData(new GridData(370,135));
		objectAuthorityList.setParent(subloginGroupLeft);
		//加载监听
		if(editorType==1) {
			ControlPropertyCommandListener.create(userEditor, objectAuthorityList, UserPropertyHandler.OBJECT_AUTHORITY);
    		ControlPropertyCommandListener.create(userEditor, schemaCombo, UserPropertyHandler.TARGET_SCHEMA);
    		ControlPropertyCommandListener.create(userEditor, objectCombo, UserPropertyHandler.TARGET_OBJECT);
    		ControlPropertyCommandListener.create(userEditor, objectTypeCombo, UserPropertyHandler.TARGET_TYPE);
    	}else {
    		ControlPropertyCommandListener.create(roleEditor, objectAuthorityList, RolePropertyHandler.OBJECT_AUTHORITY);
    		ControlPropertyCommandListener.create(roleEditor, schemaCombo, RolePropertyHandler.TARGET_SCHEMA);
    		ControlPropertyCommandListener.create(roleEditor, objectCombo, RolePropertyHandler.TARGET_OBJECT);
    		ControlPropertyCommandListener.create(roleEditor, objectTypeCombo, RolePropertyHandler.TARGET_TYPE);
    	}
		addObjectAuthority = UIUtils.createPushButton(parent2, "Grant", null);
		addObjectAuthority.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		removeObjectAuthority = UIUtils.createPushButton(parent2, "Revoke", null);
		removeObjectAuthority.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SelectionListener itemChangeListener = new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String type = objectTypeCombo.getText();
				String schema = schemaCombo.getText();
				//加载对象信息
				objectCombo.removeAll();
				String objectList="";
				if(editorType==1) {
					objectList = ((XuguUser)owner).getObjectList(schema, type, "");
				}
				else {
					objectList = ((XuguRole)owner).getObjectList(schema, type, "");
				}
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
						schemaCombo.notifyListeners(SWT.Modify, null);
						objectCombo.notifyListeners(SWT.Modify, null);
						objectTypeCombo.notifyListeners(SWT.Modify, null);
						objectAuthorityList.notifyListeners(SWT.Modify, null);
						objectAuthorityList.deselectAll();
					}else {
						if(!hasAuthority) {
							subObjectAuthorityList.add(authority);
						}
						subObjectAuthorityList.selectAll();
						schemaCombo.notifyListeners(SWT.Modify, null);
						objectCombo.notifyListeners(SWT.Modify, null);
						objectTypeCombo.notifyListeners(SWT.Modify, null);
						subObjectCombo.notifyListeners(SWT.Modify, null);
						subObjectTypeCombo.notifyListeners(SWT.Modify, null);
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
						schemaCombo.notifyListeners(SWT.Modify, null);
						objectCombo.notifyListeners(SWT.Modify, null);
						objectTypeCombo.notifyListeners(SWT.Modify, null);
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
						schemaCombo.notifyListeners(SWT.Modify, null);
						objectCombo.notifyListeners(SWT.Modify, null);
						objectTypeCombo.notifyListeners(SWT.Modify, null);
						subObjectCombo.notifyListeners(SWT.Modify, null);
						subObjectTypeCombo.notifyListeners(SWT.Modify, null);
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
}
