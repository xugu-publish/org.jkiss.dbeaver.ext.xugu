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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.XuguUtils;
//import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguUser;
import org.jkiss.dbeaver.ext.xugu.model.XuguUserAuthority;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 生成修改用户 command
 */
public class XuguCommandChangeUser extends DBECommandComposite<XuguUser, UserPropertyHandler> {

    protected XuguCommandChangeUser(XuguUser user)
    {
        super(user, XuguMessages.edit_command_change_user_name);
    }

    @Override
    public void updateModel()
    {
        for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
            switch (UserPropertyHandler.valueOf((String) entry.getKey())) {
                case NAME: getObject().setName(CommonUtils.toString(entry.getValue())); break;
                case PASSWORD: getObject().setPassword((CommonUtils.toString(entry.getValue()))); break;
                case LOCKED: getObject().setLocked(CommonUtils.toBoolean(entry.getValue()));break;
                case EXPIRED: getObject().setExpired(CommonUtils.toBoolean(entry.getValue()));break;
                case UNTIL_TIME:getObject().setUntil_time(CommonUtils.toString(entry.getValue()));break;
                default:
                    break;
            }
        }
    }

    @Override
    public void validateCommand() throws DBException
    {
        String passValue = CommonUtils.toString(getProperty(UserPropertyHandler.PASSWORD));
        String confirmValue = CommonUtils.toString(getProperty(UserPropertyHandler.PASSWORD_CONFIRM));
        if (!CommonUtils.isEmpty(passValue) && !CommonUtils.equalObjects(passValue, confirmValue)) {
            throw new DBException("Password confirmation value is invalid");
        }
    }

    @Override
    public DBEPersistAction[] getPersistActions(DBRProgressMonitor monitor, Map<String, Object> options)
    {
        List<DBEPersistAction> actions = new ArrayList<>();
        boolean newUser = !getObject().isPersisted();
        //创建新用户
        if (newUser) {
            actions.add(
                new SQLDatabasePersistAction(XuguMessages.edit_command_change_user_action_create_new_user, "CREATE USER " + getObject().getName()+
                		"\nIDENTIFIED BY "+SQLUtils.quoteString(getObject(), CommonUtils.toString(getProperties().get(UserPropertyHandler.PASSWORD.name())))+" ") { //$NON-NLS-2$
                    @Override
                    public void afterExecute(DBCSession session, Throwable error)
                    {
                        if (error == null) {
                            getObject().setPersisted(true);
                        }
                    }
                });
        }
        //修改旧用户
        else {
        	StringBuilder script = new StringBuilder();
            boolean hasSet;
            hasSet = generateAlterScript(script);
            if (hasSet) {
                actions.add(new SQLDatabasePersistAction(XuguMessages.edit_command_change_user_action_update_user_record, script.toString()));
            }
            //对权限做额外处理
            Collection<XuguUserAuthority> oldAuthorities = getObject().getUserDatabaseAuthorities();
            Collection<XuguUserAuthority> oldAuthorities2 = getObject().getUserObjectAuthorities();
			Iterator<XuguUserAuthority> it;
			
            for(Map.Entry<Object, Object> entry:getProperties().entrySet()) {
            	switch(UserPropertyHandler.valueOf((String)entry.getKey())) {
            		case DATABASE_AUTHORITY:
            			//遍历新权限列表，若旧权限不存在于其中，则做revoke操作
            			it = oldAuthorities.iterator();
            			XuguUserAuthority authority = null;
            			String[] newAuthorities = (String[]) entry.getValue();
            			while(it.hasNext()) {
            				authority = it.next();
        					boolean inListFlag = false;
            				for(int i=0, l=newAuthorities.length; i<l; i++) {
            					if(authority.getName().equals(newAuthorities[i])) {
                					inListFlag = true;
                					break;
                				}
            				}
            				//旧权限不在列表中则revoke
                			if(!inListFlag && authority!=null) {
                				actions.add(new SQLDatabasePersistAction("Revoke user", 
                						"REVOKE "+XuguUtils.transformAuthority(authority.getName(), true)+" FROM "+getObject().getName()));
                			}
            			}
            			//遍历旧权限列表，若新权限不存在于其中，则做grant操作
            			it = oldAuthorities.iterator();
            			for(int i=0, l=newAuthorities.length; i<l; i++) {
            				boolean inListFlag = false;
            				while(it.hasNext()) {
            					authority = it.next();
        						if(authority.getName().equals(newAuthorities[i])) {
                					inListFlag = true;
                					break;
                				}
            				}
            				//新权限不在列表中则grant
                			if(!inListFlag) {
                				actions.add(new SQLDatabasePersistAction("Grant user", 
                						"GRANT "+XuguUtils.transformAuthority(newAuthorities[i], true)+" TO "+getObject().getName()));
                			}
            			}
            			break;
            		case OBJECT_AUTHORITY:
            			it = oldAuthorities2.iterator();
            			String[] newAuthorities2 = (String[]) entry.getValue();
            			String schema = getProperties().get("TARGET_SCHEMA").toString();
            			String objectType = getProperties().get("TARGET_TYPE").toString();
            			String object = getProperties().get("TARGET_OBJECT").toString();
            			String realTargetName = "\""+schema+"\".\""+object+"\"";
            			//遍历新权限列表，若旧权限不存在于其中，则做revoke操作
            			XuguUserAuthority authority2 = null;
            			while(it.hasNext()) {
            				authority2 = it.next();
        					boolean inListFlag = false;
            				for(int i=0, l=newAuthorities2.length; i<l; i++) {
            					if(authority2.getName().contains(newAuthorities2[i]) && authority2.getTargetName().equals(realTargetName)) {
                					inListFlag = true;
                					break;
                				}
            				}
            				//旧权限不在列表中则revoke
                			if(!inListFlag && authority2!=null) {
                				actions.add(new SQLDatabasePersistAction("Revoke user", 
                						"REVOKE "+XuguUtils.transformAuthority(authority2.getName(), false)+" "+realTargetName+" FROM "+getObject().getName()));
                			}
            			}
            			//遍历旧权限列表，若新权限不存在于其中，则做grant操作
            			it = oldAuthorities2.iterator();
            			for(int i=0, l=newAuthorities2.length; i<l; i++) {
            				boolean inListFlag = false;
            				while(it.hasNext()) {
            					authority2 = it.next();
        						if(authority2.getName().contains(newAuthorities2[i]) && authority2.getTargetName().equals(realTargetName)) {
                					inListFlag = true;
                					break;
                				}
            				}
            				//新权限不在列表中则grant
                			if(!inListFlag) {
                				actions.add(new SQLDatabasePersistAction("Grant user", 
                						"GRANT "+XuguUtils.transformAuthority(newAuthorities2[i], false)+" "+realTargetName+" TO "+getObject().getName()));
                			}
            			}
            			break;
            	}
            }
        }
        return actions.toArray(new DBEPersistAction[actions.size()]);
    }

    private boolean generateAlterScript(StringBuilder script) {
        boolean hasSet = false;
        script.append("ALTER USER ").append(getObject().getName()); //$NON-NLS-1$
        for(Map.Entry<Object, Object> entry:getProperties().entrySet()) {
        	String delim = hasSet?",":"";
        	switch(UserPropertyHandler.valueOf((String)entry.getKey())) {
	        	//处理密码更改	
	        	case PASSWORD:
	        		script.append(delim);
	        		script.append("\nIDENTIFIED BY ").append(SQLUtils.quoteString(getObject(), CommonUtils.toString(entry.getValue())));
	                hasSet = true;
	                break;
	            //处理用户名更改
	        	case NAME:
	        		script.append(delim);
	        		script.append("\nRENAME TO ").append(CommonUtils.toString(entry.getValue()));
	            	hasSet = true;
	            	break;
	            //处理加锁
	        	case LOCKED:
	        		script.append(delim);
	        		script.append("\nACCOUNT ");
	        		script.append(CommonUtils.toBoolean(entry.getValue())?"LOCK":"UNLOCK");
	        		hasSet = true;
	        		break;
	        	// 处理密码失效
	        	case EXPIRED:
	        		script.append(delim);
	        		script.append(CommonUtils.toBoolean(entry.getValue())?"\nPASSWORD EXPIRE":"");
	        		hasSet = CommonUtils.toBoolean(entry.getValue())?true:hasSet;
	        		break;
	        		// 处理密码失效
	        	case UNTIL_TIME:
	        		script.append(delim);
	        		script.append("\nVALID UNTIL '").append(CommonUtils.toString(entry.getValue())).append("'");
	        		hasSet = true;
	        		break;
	        	default:
	        		break;
        	}
        }
        return hasSet;
    }

}