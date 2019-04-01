/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * XuguRole
 */
public class XuguRole extends XuguGlobalObject implements DBARole, DBPRefreshableObject
{
    private static final Log log = Log.getLog(XuguRole.class);

    private String name;
    private int id;
    private String authentication;
    private final UserCache userCache = new UserCache();
    private String userDesc;

    public XuguRole(XuguDataSource dataSource, ResultSet resultSet) {
        super(dataSource, true);
        try {
        	if(resultSet!=null) {
        		if(resultSet.getMetaData().getColumnCount()!=1) {
        			this.name = JDBCUtils.safeGetString(resultSet, "USER_NAME");
                    this.id = JDBCUtils.safeGetInt(resultSet, "USER_ID");
                    this.authentication = JDBCUtils.safeGetString(resultSet, "PASSWORD");
    			}
        	}
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
    	this.name = name;
    }

    @Property(viewable = true, order = 3)
    public String getAuthentication()
    {
        return authentication;
    }

    public int getID() {
    	return this.id;
    }
    
    public String getUserDesc() {
    	return userDesc;
    }
    
    public void setUserDesc(String desc) {
    	userDesc = desc;
    }
    
    @Association
    public Collection<XuguPrivUser> getUserPrivs(DBRProgressMonitor monitor) throws DBException
    {
        return userCache.getAllObjects(monitor, this);
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        userCache.clearCache();
        return this;
    }

    static class UserCache extends JDBCObjectCache<XuguRole, XuguPrivUser> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguRole owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM SYS_USERS WHERE USER_ID IN \n" + 
                    "(SELECT USER_ID FROM SYS_ROLE_MEMBERS WHERE ROLE_ID=?)");
            dbStat.setString(1, owner.getID()+"");
            return dbStat;
        }

        @Override
        protected XuguPrivUser fetchObject(@NotNull JDBCSession session, @NotNull XuguRole owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguPrivUser(owner, resultSet);
        }
    }

}
