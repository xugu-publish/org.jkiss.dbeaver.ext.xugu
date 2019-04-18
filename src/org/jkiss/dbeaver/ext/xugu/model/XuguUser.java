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
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema.TableCache;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.impl.AbstractObjectCache;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
//import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
//import org.jkiss.dbeaver.model.meta.LazyProperty;
//import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import com.xugu.permission.LoadPermission;

//import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * XuguUser
 */
public class XuguUser extends XuguGlobalObject implements DBAUser, DBPRefreshableObject, DBPSaveableObject
{
    private static final Log log = Log.getLog(XuguUser.class);
    
    private Vector<Object> authorityList;
	private Vector<String> authorityKey;
	private Vector<String> authorityValue;
	
	private XuguUserAuthority authority;
	DBRProgressMonitor monitor;
    //xfc 修改了用户信息的字段
    private int db_id;
	private int user_id;
    private String user_name;
    private boolean is_role;
    private String password;
    private Timestamp start_time;
    private String until_time;
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
    private Connection conn;
    private String role_list;
    private Collection<XuguUserAuthority> userAuthorities;
    private String schema_list;
    public XuguUser(XuguDataSource dataSource, ResultSet resultSet, DBRProgressMonitor monitor) {
        super(dataSource, true);
        this.monitor = monitor;
        if(resultSet!=null) {
        	try {
				conn = resultSet.getStatement().getConnection();
//				loadGrants();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	this.db_id = JDBCUtils.safeGetInt(resultSet, "DB_ID");
            this.user_id = JDBCUtils.safeGetInt(resultSet, "USER_ID");
            this.user_name = JDBCUtils.safeGetString(resultSet, "USER_NAME");
            this.is_role = JDBCUtils.safeGetBoolean(resultSet, "IS_ROLE");
            this.password = JDBCUtils.safeGetString(resultSet, "PASSWORD");
            this.start_time = JDBCUtils.safeGetTimestamp(resultSet, "START_TIME");
            
            this.until_time = JDBCUtils.safeGetString(resultSet, "UNTIL_TIME");
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
        //加载roleList和schemaList
        Collection<XuguRole> roleList;
		try {
			roleList = dataSource.roleCache.getAllObjects(monitor, dataSource);
		
			if(roleList!=null && roleList.size()!=0) {
				Iterator<XuguRole> it = roleList.iterator();
				String text = "";
				while(it.hasNext()) {
					text += it.next().getName()+",";
				}
				text = text.substring(0, text.length()-1);
				this.setRoleList(text);
			}
			Collection<XuguSchema> schemaList = dataSource.getSchemas(monitor);
			if(schemaList!=null && schemaList.size()!=0) {
				Iterator<XuguSchema> it = schemaList.iterator();
				String text = "";
				
				while(it.hasNext()) {
					XuguSchema tempSchema = it.next();
					//构造schemalist
					text += tempSchema.getName()+",";
				}
				text = text.substring(0, text.length()-1);
				this.setSchemaList(text);
			}
		} catch (DBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//加载权限
		Vector<Object> authorities = new LoadPermission().loadPermission(conn, this.user_name);
		userAuthorities = new ArrayList<>();
		Iterator<Object> it = authorities.iterator();
		while(it.hasNext()) {
			String temp = it.next().toString();
			//对象级权限
			if(temp.indexOf("\"")!=-1) {
				String targetName = temp.substring(temp.indexOf("\""));
				XuguUserAuthority one = new XuguUserAuthority(this, temp, targetName, false, expired);
				userAuthorities.add(one);
			}
			//库级权限
			else {
				XuguUserAuthority one = new XuguUserAuthority(this, temp, null, true, expired);
				userAuthorities.add(one);
			}
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
	
    public String getPassword() {
    	return this.password;
    }
    
    public void setPassword(String password)
    {
        this.password = password;
    }
    
	public boolean isIs_role() {
		return is_role;
	}

	public Timestamp getStart_time() {
		return start_time;
	}

	@Property(viewable = true, editable = true, updatable=true, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
	public String getUntil_time() {
		return until_time;
	}
	
	public void setUntil_time(String time) {
		this.until_time = time;
	}
	
	@Property(viewable = true, editable = true, updatable=true, valueTransformer = DBObjectNameCaseTransformer.class, order = 4)
	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
	@Property(viewable = true, editable = true, updatable=true, valueTransformer = DBObjectNameCaseTransformer.class, order = 5)
	public boolean isExpired() {
		return expired;
	}
	
	public void setExpired(boolean expired) {
		this.expired = expired;
	}
	
	@Property(viewable=false, editable = true)
	public String getRoleList() {
		return this.role_list;
	}
	
	public void setRoleList(String list) {
		this.role_list = list;
	}
	
	@Property(viewable=false, editable = true)
	public String getSchemaList() {
		return this.schema_list;
	}
	
	public void setSchemaList(String list) {
		this.schema_list = list;
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

	public int getTemp_space_quota() {
		return temp_space_quota;
	}

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
	
	@Property(viewable = true, order = 6)
	public Vector<String> getAuthorityKey(){
		return authorityKey;
	}
	
	@Property(viewable = true, order = 7)
	public Vector<String> getAuthorityValue(){
		return authorityValue;
	}
	
	public XuguUserAuthority getAuthority() {
		return authority;
	}
	
	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		return this.getDataSource().userCache.refreshObject(monitor, this.getDataSource(), this);
	}

	public Collection<XuguUserAuthority> getUserAuthorities(){
		return userAuthorities;
	}
	
	public Collection<XuguUserAuthority> getUserDatabaseAuthorities(){
		Collection<XuguUserAuthority> res = new ArrayList<>();
		Iterator<XuguUserAuthority> it = userAuthorities.iterator();
		while(it.hasNext()) {
			XuguUserAuthority authority = it.next();
			if(authority.isDatabase) {
				res.add(authority);
			}
		}
		return res;
	}
	
	public Collection<XuguUserAuthority> getUserObjectAuthorities(){
		Collection<XuguUserAuthority> res = new ArrayList<>();
		Iterator<XuguUserAuthority> it = userAuthorities.iterator();
		while(it.hasNext()) {
			XuguUserAuthority authority = it.next();
			if(!authority.isDatabase) {
				res.add(authority);
			}
		}
		return res;
	}
	
	public void addUserAuthority(XuguUserAuthority authority) {
		userAuthorities.add(authority);
	}
	
	public void removeAuthority(XuguUserAuthority authority) {
		userAuthorities.remove(authority);
	}

	public String getObjectList(String schema, String Type) {
		try {
			Collection<XuguTable> tableList = null;
			Collection<XuguView> viewList = null;
			Collection<XuguSequence> seqList = null;
			Collection<XuguPackage> pacList = null;
			Collection<XuguProcedureStandalone> procList = null;
			Collection<XuguTableTrigger> triList = null;
			switch(Type) {
			case "TABLE":
				tableList = this.getDataSource().schemaCache.getCachedObject(schema).getTables(monitor);
				break;
			case "VIEW":
				viewList = this.getDataSource().schemaCache.getCachedObject(schema).getViews(monitor);
				break;
			case "SEQUENCE":
				seqList = this.getDataSource().schemaCache.getCachedObject(schema).getSequences(monitor);
				break;
			case "PACKAGE":
				pacList = this.getDataSource().schemaCache.getCachedObject(schema).getPackages(monitor);
				break;
			case "PROCEDURE":
				procList = this.getDataSource().schemaCache.getCachedObject(schema).getProcedures(monitor);
				break;
			case "TRIGGER":
				tableList = this.getDataSource().schemaCache.getCachedObject(schema).getTables(monitor);
				Iterator<XuguTable> it = tableList.iterator();
				while(it.hasNext()) {
					XuguTable table = it.next();
					if(triList==null) {
						triList = table.getTriggers(monitor);
					}else {
						triList.addAll(table.getTriggers(monitor));
					}
					
				}
			}
				
			if(tableList!=null && "TABLE".equals(Type)) {
				String res = "";
				Iterator<XuguTable> it = tableList.iterator();
				while(it.hasNext()) {
					res += it.next().getName()+",";
				}
				if(res.length()>0) {
					res = res.substring(0, res.length()-1);
				}
				return res;
			}
			if(viewList!=null) {
				String res = "";
				Iterator<XuguView> it = viewList.iterator();
				while(it.hasNext()) {
					res += it.next().getName()+",";
				}
				if(res.length()>0) {
					res = res.substring(0, res.length()-1);
				}
				return res;
			}
			if(seqList!=null) {
				String res = "";
				Iterator<XuguSequence> it = seqList.iterator();
				while(it.hasNext()) {
					res += it.next().getName()+",";
				}
				if(res.length()>0) {
					res = res.substring(0, res.length()-1);
				}
				return res;
			}
			if(pacList!=null) {
				String res = "";
				Iterator<XuguPackage> it = pacList.iterator();
				while(it.hasNext()) {
					res += it.next().getName()+",";
				}
				if(res.length()>0) {
					res = res.substring(0, res.length()-1);
				}
				return res;
			}
			if(procList!=null) {
				String res = "";
				Iterator<XuguProcedureStandalone> it = procList.iterator();
				while(it.hasNext()) {
					res += it.next().getName()+",";
				}
				if(res.length()>0) {
					res = res.substring(0, res.length()-1);
				}
				return res;
			}
			if(triList!=null && triList.size()>0) {
				String res = "";
				Iterator<XuguTableTrigger> it = triList.iterator();
				XuguTableTrigger trigger = it.next();
				while(it.hasNext()) {
					res += trigger.getTable().getName()+"."+trigger.getName()+",";
				}
				if(res.length()>0) {
					res = res.substring(0, res.length()-1);
				}
				return res;
			}
		} catch (DBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
//	public void loadGrants() {
//		// 获取connection
//		System.out.println("ok?");
//		new TestClass().print();
//		System.out.println("ok?");
//		new Parsing().loadDDL((com.xugu.cloudjdbc.Connection) this.conn, "SYSDBA", "TEST1");
//		Vector<Object> grants = new LoadPermission().loadPermission(this.conn, this.user_name);
//		System.out.println(grants.size());
//	}
}
