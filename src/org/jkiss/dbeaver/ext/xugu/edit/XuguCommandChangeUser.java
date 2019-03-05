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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Grant/Revoke privilege command
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
//                case MAX_QUERIES: getObject().setMaxQuestions(CommonUtils.toInt(entry.getValue())); break;
//                case MAX_UPDATES: getObject().setMaxUpdates(CommonUtils.toInt(entry.getValue())); break;
//                case MAX_CONNECTIONS: getObject().setMaxConnections(CommonUtils.toInt(entry.getValue())); break;
//                case MAX_USER_CONNECTIONS: getObject().setMaxUserConnections(CommonUtils.toInt(entry.getValue())); break;
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
        boolean hasSet = false, hasResOptions = false;
        script.append("ALTER USER ").append(getObject().getName()); //$NON-NLS-1$
        
        if (getProperties().containsKey(UserPropertyHandler.PASSWORD.name())) {
            script.append("\nIDENTIFIED BY ").append(SQLUtils.quoteString(getObject(), CommonUtils.toString(getProperties().get(UserPropertyHandler.PASSWORD.name())))).append(" ");
            hasSet = true;
        }
        //用户选项
//        StringBuilder resOptions = new StringBuilder();
//        for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
//            switch (UserPropertyHandler.valueOf((String) entry.getKey())) {
//                case MAX_QUERIES: resOptions.append("\n\tMAX_QUERIES_PER_HOUR ").append(CommonUtils.toInt(entry.getValue())); hasResOptions = true; break; //$NON-NLS-1$
//                case MAX_UPDATES: resOptions.append("\n\tMAX_UPDATES_PER_HOUR ").append(CommonUtils.toInt(entry.getValue())); hasResOptions = true; break; //$NON-NLS-1$
//                case MAX_CONNECTIONS: resOptions.append("\n\tMAX_CONNECTIONS_PER_HOUR ").append(CommonUtils.toInt(entry.getValue())); hasResOptions = true; break; //$NON-NLS-1$
//                case MAX_USER_CONNECTIONS: resOptions.append("\n\tMAX_USER_CONNECTIONS ").append(CommonUtils.toInt(entry.getValue())); hasResOptions = true; break; //$NON-NLS-1$
//            }
//        }
//        if (resOptions.length() > 0) {
//            script.append("\nWITH").append(resOptions);
//        }
        return hasSet || hasResOptions;
    }

}