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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguUser;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLScriptCommand;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

/**
 * XuguUserManager
 */
public class XuguUserManager extends AbstractObjectManager<XuguUser> implements DBEObjectMaker<XuguUser, XuguDataSource>, DBECommandFilter<XuguUser> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguUser> getObjectsCache(XuguUser object)
    {
        return null;
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

    @Override
    public XuguUser createNewObject(DBRProgressMonitor monitor, DBECommandContext commandContext, XuguDataSource parent, Object copyFrom)
    {
        XuguUser newUser = new XuguUser(parent, null);
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
        commandContext.addCommand(new CommandCreateUser(newUser), new CreateObjectReflector<>(this), true);
        return newUser;
    }

    @Override
    public void deleteObject(DBECommandContext commandContext, XuguUser user, Map<String, Object> options)
    {
        commandContext.addCommand(new CommandDropUser(user), new DeleteObjectReflector<>(this), true);
    }

    @Override
    public void filterCommands(DBECommandQueue<XuguUser> queue)
    {
    	System.out.println("Create3 ??"+queue.toString());
    }

    private static class CommandCreateUser extends DBECommandAbstract<XuguUser> {
        protected CommandCreateUser(XuguUser user)
        {
            super(user, XuguMessages.edit_user_manager_command_create_user);
            System.out.println("Create2 ??");
        }
    }

    private static class CommandDropUser extends DBECommandComposite<XuguUser, UserPropertyHandler> {
        protected CommandDropUser(XuguUser user)
        {
            super(user, XuguMessages.edit_user_manager_command_drop_user);
        }
        @Override
        public DBEPersistAction[] getPersistActions(DBRProgressMonitor monitor, Map<String, Object> options)
        {
            return new DBEPersistAction[] {
                new SQLDatabasePersistAction(XuguMessages.edit_user_manager_command_drop_user, "DROP USER " + getObject().getName()) { //$NON-NLS-2$
                    @Override
                    public void afterExecute(DBCSession session, Throwable error)
                    {
                        if (error == null) {
                            getObject().setPersisted(false);
                        }
                    }
                }};
        }
    }
}

