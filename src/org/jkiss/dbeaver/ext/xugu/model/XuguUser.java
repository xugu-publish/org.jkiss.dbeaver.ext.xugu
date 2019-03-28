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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
//import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
//import org.jkiss.dbeaver.model.meta.LazyProperty;
//import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
//import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * OracleUser
 */
public class XuguUser extends XuguGlobalObject implements DBAUser, DBPRefreshableObject, DBPSaveableObject
{
    private static final Log log = Log.getLog(XuguUser.class);
    
    //xfc 修改了用户信息的字段
    private int db_id;
	private int user_id;
    private String user_name;
    private boolean is_role;
    private byte[] password;
    private Timestamp start_time;
    private Timestamp until_time;
    private boolean locked;
    private boolean expired;
    private Timestamp pass_set_time;
    private Timestamp pass_set_period;
    private String alias;
    private boolean is_sys;
    private String trust_ip;
//    private int mem_quota;
    private int temp_space_quota;
//    private int undo_space_quota;
    private int cursor_quota;
    private int session_quota;
    private int io_quota;
    private Timestamp create_time;
    private Timestamp last_modi_time;

    public XuguUser(XuguDataSource dataSource, ResultSet resultSet) {
        super(dataSource, true);
        if(resultSet!=null) {
        	this.db_id = JDBCUtils.safeGetInt(resultSet, "DB_ID");
            this.user_id = JDBCUtils.safeGetInt(resultSet, "USER_ID");
            this.user_name = JDBCUtils.safeGetString(resultSet, "USER_NAME");
            this.is_role = JDBCUtils.safeGetBoolean(resultSet, "IS_ROLE");
            this.password = JDBCUtils.safeGetBytes(resultSet, "PASSWORD");
            this.start_time = JDBCUtils.safeGetTimestamp(resultSet, "START_TIME");
            this.until_time = JDBCUtils.safeGetTimestamp(resultSet, "UNTIL_TIME");
            this.locked = JDBCUtils.safeGetBoolean(resultSet, "LOCKED");
            this.expired = JDBCUtils.safeGetBoolean(resultSet, "EXPIRED");
            this.pass_set_time = JDBCUtils.safeGetTimestamp(resultSet, "PASS_SET_TIME");
            this.pass_set_period = JDBCUtils.safeGetTimestamp(resultSet, "PASS_SET_PERIOD");
            this.alias = JDBCUtils.safeGetString(resultSet, "ALIAS");
            this.is_sys = JDBCUtils.safeGetBoolean(resultSet, "IS_SYS");
            this.trust_ip = JDBCUtils.safeGetString(resultSet, "TRUST_IP");
//            this.mem_quota = JDBCUtils.safeGetInt(resultSet, "MEM_QUOTA");
            this.temp_space_quota = JDBCUtils.safeGetInt(resultSet, "TEMP_SPACE_QUOTA");
//            this.undo_space_quota = JDBCUtils.safeGetInt(resultSet, "UNDO_SPACE_QUOTA");
            this.cursor_quota = JDBCUtils.safeGetInt(resultSet, "CURSOR_QUOTA");
            this.session_quota = JDBCUtils.safeGetInt(resultSet, "SESSION_QUOTA");
            this.io_quota = JDBCUtils.safeGetInt(resultSet, "IO_QUOTA");
            this.create_time = JDBCUtils.safeGetTimestamp(resultSet, "CREATE_TIME");
            this.last_modi_time = JDBCUtils.safeGetTimestamp(resultSet, "LAST_MODI_TIME");
        }
    }

    public static Log getLog() {
		return log;
	}

	public int getDb_id() {
		return db_id;
	}

	public int getUser_id() {
		return user_id;
	}

	@Override
	@Property(viewable = true, order = 0)
	public String getName() {
		return user_name;
	}

    public void setName(String name)
    {
        this.user_name = name;
    }
	
    public byte[] getPassword() {
    	return this.password;
    }
    
    public void setPassword(byte[] password)
    {
        this.password = password;
    }
    
	public boolean isIs_role() {
		return is_role;
	}

	public Timestamp getStart_time() {
		return start_time;
	}

	public Timestamp getUntil_time() {
		return until_time;
	}

	public boolean isLocked() {
		return locked;
	}

	public boolean isExpired() {
		return expired;
	}

	public Timestamp getPass_set_time() {
		return pass_set_time;
	}

	public Timestamp getPass_set_period() {
		return pass_set_period;
	}

	public String getAlias() {
		return alias;
	}

	public boolean isIs_sys() {
		return is_sys;
	}

	public String getTrust_ip() {
		return trust_ip;
	}

//	public int getMem_quota() {
//		return mem_quota;
//	}

	public int getTemp_space_quota() {
		return temp_space_quota;
	}

//	public int getUndo_space_quota() {
//		return undo_space_quota;
//	}

	public int getCursor_quota() {
		return cursor_quota;
	}

	public int getSession_quota() {
		return session_quota;
	}

	public int getIo_quota() {
		return io_quota;
	}

	public Timestamp getCreate_time() {
		return create_time;
	}

	public Timestamp getLast_modi_time() {
		return last_modi_time;
	}

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return this;
	}
}
