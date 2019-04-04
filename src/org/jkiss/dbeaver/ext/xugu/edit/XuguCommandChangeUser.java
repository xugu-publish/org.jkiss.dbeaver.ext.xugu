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
//import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguUser;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
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