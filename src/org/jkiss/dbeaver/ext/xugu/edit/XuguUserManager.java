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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguUser;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;
import java.util.Map;

/**
 * @author Maple4Real
 * 用户管理器
 * 进行用户的创建，修改和删除
 * 包含一个内部界面类，用于进行属性设定
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
        }
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
//        		user.setUntil_time(command.getProperties().get(UserPropertyHandler.UNTIL_TIME.toString()).toString());
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
//        		if(user.getUntil_time()!=null) {
//        			sql.append("\nVALID UNTIL '");
//        			sql.append(user.getUntil_time());
//        			sql.append("'");
//        		}
        		sql.append(user.isLocked()?" ACCOUNT LOCK":"");
        		sql.append(user.isExpired()?" PASSWORD EXPIRED":"");
        		if(XuguConstants.LOG_PRINT_LEVEL<1) {
                	log.info("Xugu Plugin: Construct create user sql: "+sql.toString());
                }
                DBEPersistAction action = new SQLDatabasePersistAction("Create User", sql.toString());
                actions.add(action);
        	}
    	}
    }
    
    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
    	String sql = "DROP USER " + command.getObject().getName();
    	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct drop user sql: "+sql);
        }
        DBEPersistAction action = new SQLDatabasePersistAction("Drop User", sql);
        actions.add(action);
    }
    
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
    	for(String k:options.keySet()) {
    		log.debug(options.get(k));
    	}
    	String sql = "ALTER USER " + command.getObject().getName() + " IDENTIFIED BY ";
    	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct alter user sql: "+sql);
        }
        DBEPersistAction action = new SQLDatabasePersistAction("Alter User", sql);
        actionList.add(action);
    }

    @Override
    public void filterCommands(DBECommandQueue<XuguUser> queue)
    {
    	
    }

}

