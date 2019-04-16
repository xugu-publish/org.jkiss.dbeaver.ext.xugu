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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguRole;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguSequence;
import org.jkiss.dbeaver.ext.xugu.model.XuguUser;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLScriptCommand;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
//import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor.ObjectChangeCommand;
//import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor.ObjectCreateCommand;
//import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor.ObjectDeleteCommand;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * XuguUserManager
 */
public class XuguUserManager extends SQLObjectEditor<XuguUser, XuguDataSource> implements DBEObjectMaker<XuguUser, XuguDataSource>, DBECommandFilter<XuguUser> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguUser> getObjectsCache(XuguUser object)
    {
        return object.getDataSource().userCache;
    }

    @Override
    public boolean canCreateObject(XuguDataSource parent)
    {
        return true;
    }

    @Override
    public boolean canDeleteObject(XuguUser object)
    {
        return true;
    }

    //新建用户界面显示前的准备工作
    @Override
    protected XuguUser createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
                                               final XuguDataSource source,
                                               Object copyFrom) {
    	context.getUserParams();
    	XuguUser newUser = new XuguUser(source, null, monitor);
//    	//加载roleList和schemaList
//    	try {
//			Collection<XuguRole> roleList = source.roleCache.getAllObjects(monitor, source);
//			if(roleList!=null && roleList.size()!=0) {
//				Iterator<XuguRole> it = roleList.iterator();
//				String text = "";
//				while(it.hasNext()) {
//					text += it.next().getName()+",";
//				}
//				text = text.substring(0, text.length()-1);
//				newUser.setRoleList(text);
//			}
//			Collection<XuguSchema> schemaList = source.getSchemas(monitor);
//			if(schemaList!=null && schemaList.size()!=0) {
//				Iterator<XuguSchema> it = schemaList.iterator();
//				String text = "";
//				while(it.hasNext()) {
//					text += it.next().getName()+",";
//				}
//				text = text.substring(0, text.length()-1);
//				newUser.setSchemaList(text);
//			}
//		} catch (DBException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
    	        
        //修改已存在用户
        if (copyFrom instanceof XuguUser) {
            XuguUser tplUser = (XuguUser)copyFrom;
            newUser.setName(tplUser.getName());
            newUser.setPassword(tplUser.getPassword());
            newUser.setLocked(tplUser.isLocked());
            newUser.setExpired(tplUser.isExpired());
        }
        //创建新用户
        else {
        	newUser.setPersisted(false);
        	System.out.println(" ?? "+newUser.isPersisted());
        }
        System.out.println("Create1 ??");
//        commandContext.addCommand(new CommandCreateUser(newUser), new CreateObjectReflector<>(this), true);
        return newUser;
    }
    
    //点击确定后 真正执行新建用户操作
    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
    	//refresh the new object
    	XuguUser user = command.getObject();
    	if(command.getProperties()!=null) {
    		String name = command.getProperties().get(UserPropertyHandler.NAME.toString()).toString();
        	String key1 = command.getProperties().get(UserPropertyHandler.PASSWORD.toString()).toString();
        	String key2 = command.getProperties().get(UserPropertyHandler.PASSWORD_CONFIRM.toString()).toString();
        	String roleList = "";
        	if(command.getProperties().get(UserPropertyHandler.ROLE_LIST.toString())!=null)
        		roleList = command.getProperties().get(UserPropertyHandler.ROLE_LIST.toString()).toString();
        	if(!key1.equals(key2)) {
        		log.error("Password confirm different!");
        	}
        	else {
        		user.setName(name);
        		user.setPassword(key1);
        		user.setRoleList(roleList);
        		user.setUntil_time(command.getProperties().get(UserPropertyHandler.UNTIL_TIME.toString()).toString());
        		user.setPersisted(true);
        		StringBuilder sql = new StringBuilder();
        		sql.append("CREATE USER ");
        		sql.append(user.getName());
        		sql.append("\nIDENTIFIED BY '");
        		sql.append(user.getPassword());
        		sql.append("'");
        		if(user.getRoleList()!=null && !"".equals(user.getRoleList())) {
        			sql.append(" DEFAULT ROLE ");
        			String[] roles = user.getRoleList().split(",");
        			for(int i=0; i<roles.length; i++) {
        				sql.append(roles[i]);
        				if(i!=roles.length-1) {
        					sql.append(",");
        				}
        			}
        		}
        		if(user.getUntil_time()!=null) {
        			sql.append("\nVALID UNTIL '");
        			sql.append(user.getUntil_time());
        			sql.append("'");
        		}
        		sql.append(user.isLocked()?" ACCOUNT LOCK":"");
        		sql.append(user.isExpired()?" PASSWORD EXPIRED":"");
        		
                DBEPersistAction action = new SQLDatabasePersistAction("Create User", sql.toString());
                actions.add(action);
        	}
    	}
    }
    
    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
    	String sql = "DROP USER " + command.getObject().getName();
        DBEPersistAction action = new SQLDatabasePersistAction("Drop User", sql);
        actions.add(action);
    }
    
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
    	for(String k:options.keySet()) {
    		System.out.println(options.get(k));
    	}
    	String sql = "ALTER USER " + command.getObject().getName() + " IDENTIFIED BY ";
        DBEPersistAction action = new SQLDatabasePersistAction("Alter User", sql);
        actionList.add(action);
    }

    @Override
    public void filterCommands(DBECommandQueue<XuguUser> queue)
    {
    	System.out.println("Create3 ??"+queue.toString());
    }

}

