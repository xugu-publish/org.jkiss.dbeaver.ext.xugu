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

package org.jkiss.dbeaver.ext.xugu;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.XuguDDLFormat;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguObjectType;
import org.jkiss.dbeaver.ext.xugu.model.XuguPackage;
import org.jkiss.dbeaver.ext.xugu.model.XuguProcedureStandalone;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguSequence;
import org.jkiss.dbeaver.ext.xugu.model.XuguTable;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableBase;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableColumn;
import org.jkiss.dbeaver.ext.xugu.model.XuguTrigger;
import org.jkiss.dbeaver.ext.xugu.model.XuguView;
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.ext.xugu.model.source.XuguStatefulObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import com.xugu.ddl.Parsing;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 
  * 提供常用方法
 * some new comments
 */
public class XuguUtils {

    private static final Log log = Log.getLog(XuguUtils.class);

    private static Map<String, Integer> typeMap = new HashMap<>();
    public static final String COLUMN_POSTFIX_PRIV = "_priv";

    static {
        typeMap.put("bit", java.sql.Types.BIT);
        typeMap.put("bool", java.sql.Types.BOOLEAN);
        typeMap.put("boolean", java.sql.Types.BOOLEAN);
        typeMap.put("tinyint", java.sql.Types.TINYINT);
        typeMap.put("smallint", java.sql.Types.SMALLINT);
        typeMap.put("mediumint", java.sql.Types.INTEGER);
        typeMap.put("int", java.sql.Types.INTEGER);
        typeMap.put("integer", java.sql.Types.INTEGER);
        typeMap.put("int24", java.sql.Types.INTEGER);
        typeMap.put("bigint", java.sql.Types.BIGINT);
        typeMap.put("real", java.sql.Types.DOUBLE);
        typeMap.put("float", java.sql.Types.REAL);
        typeMap.put("decimal", java.sql.Types.DECIMAL);
        typeMap.put("dec", java.sql.Types.DECIMAL);
        typeMap.put("numeric", java.sql.Types.DECIMAL);
        typeMap.put("double", java.sql.Types.DOUBLE);
        typeMap.put("double precision", java.sql.Types.DOUBLE);
        typeMap.put("char", java.sql.Types.CHAR);
        typeMap.put("varchar", java.sql.Types.VARCHAR);
        typeMap.put("date", java.sql.Types.DATE);
        typeMap.put("time", java.sql.Types.TIME);
        typeMap.put("year", java.sql.Types.DATE);
        typeMap.put("timestamp", java.sql.Types.TIMESTAMP);
        typeMap.put("datetime", java.sql.Types.TIMESTAMP);

        typeMap.put("tinyblob", java.sql.Types.BINARY);
        typeMap.put("blob", java.sql.Types.LONGVARBINARY);
        typeMap.put("mediumblob", java.sql.Types.LONGVARBINARY);
        typeMap.put("longblob", java.sql.Types.LONGVARBINARY);

        typeMap.put("tinytext", java.sql.Types.VARCHAR);
        typeMap.put("text", java.sql.Types.VARCHAR);
        typeMap.put("mediumtext", java.sql.Types.VARCHAR);
        typeMap.put("longtext", java.sql.Types.VARCHAR);

        typeMap.put(XuguConstants.TYPE_NAME_ENUM, java.sql.Types.CHAR);
        typeMap.put(XuguConstants.TYPE_NAME_SET, java.sql.Types.CHAR);
        typeMap.put("geometry", java.sql.Types.BINARY);
        typeMap.put("binary", java.sql.Types.BINARY);
        typeMap.put("varbinary", java.sql.Types.VARBINARY);
    }

    public static int typeNameToValueType(String typeName)
    {
        Integer valueType = typeMap.get(typeName.toLowerCase(Locale.ENGLISH));
        return valueType == null ? java.sql.Types.OTHER : valueType;
    }
    
    //判断字符串是否可用
    public static boolean checkString(String str) {
    	if(str==null || "".equals(str.trim())) {
    		return false;
    	}
    	return true;
    }
    
    public static List<String> collectPrivilegeNames(ResultSet resultSet)
    {
        // Now collect all privileges columns
        try {
            List<String> privs = new ArrayList<>();
            ResultSetMetaData rsMetaData = resultSet.getMetaData();
            int colCount = rsMetaData.getColumnCount();
            for (int i = 0; i < colCount; i++) {
                String colName = rsMetaData.getColumnName(i + 1);
                if (colName.toLowerCase(Locale.ENGLISH).endsWith(COLUMN_POSTFIX_PRIV)) {
                    privs.add(colName.substring(0, colName.length() - COLUMN_POSTFIX_PRIV.length()));
                }
            }
            return privs;
        } catch (SQLException e) {
            log.debug(e);
            return Collections.emptyList();
        }
    }

    public static Map<String, Boolean> collectPrivileges(List<String> privNames, ResultSet resultSet)
    {
        // Now collect all privileges columns
        Map<String, Boolean> privs = new TreeMap<>();
        for (String privName : privNames) {
            privs.put(privName, "Y".equals(JDBCUtils.safeGetString(resultSet, privName + COLUMN_POSTFIX_PRIV)));
        }
        return privs;
    }

    public static String determineCurrentDatabase(JDBCSession session) throws DBCException {
        // Get active schema
        try {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT DATABASE()")) {
                try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getString(1);
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }
    
    public static String transformColumnAuthority(String authority) {
    	String action ="";
    	if(authority!=null) {
    		//处理动词
        	if(authority.contains("读")) {
        		action = "SELECT";
        	}else if(authority.contains("更新")){
        		action = "UPDATE";
        	}
    	}
    	return action;
    }
    
    public static String transformAuthority(String authority, boolean isDatabase) {
    	String action ="";
    	String type = "";
    	String any ="ANY";
    	if(authority!=null) {
    		//处理动词
        	if(authority.contains("创建")) {
        		action = "CREATE";
        	}else if(authority.contains("修改")){
        		action = "ALTER";
        	}else if(authority.contains("删除")) {
        		action = "DROP";
        	}else if(authority.contains("查询")) {
    			action = "SELECT";
    		}else if(authority.contains("读")) {
    			action = "SELECT";
    		}else if(authority.contains("插入")) {
    			action = "INSERT";
    		}else if(authority.contains("删除")) {
    			action = "DELETE";
    		}else if(authority.contains("更新")) {
    			action = "UPDATE";
    		}else if(authority.contains("更改")) {
    			action = "UPDATE";
    		}else if(authority.contains("引用")) {
    			action = "REFERENCES";
    		}
        	
        	//处理名词
        	if(authority.contains("数据库")) {
        		type = "DATABASE";
        	}else if(authority.contains("模式")) {
        	    type = "SCHEMA";
        	}else if(authority.contains("表")) {
        		type = "TABLE";
        	}else if(authority.contains("视图")) {
        		type = "VIEW";
        	}else if(authority.contains("序列值")) {
        		type = "SEQUENCE";
        	}else if(authority.contains("包")) {
        		type = "PACKAGE";
        	}else if(authority.contains("存储过程")) {
        		type = "PROCEDURE";
        	}else if(authority.contains("触发器")) {
        		type = "TRIGGER";
        	}else if(authority.contains("列")){
        		type = "COLUMN";
        	}else if(authority.contains("索引")) {
        		type = "INDEX";
        	}else if(authority.contains("回滚段")) {
        		type = "UNDO SEGMENT";
        	}else if(authority.contains("同义词")) {
        		type = "SYNONYM";
        	}else if(authority.contains("快照")) {
        		type = "SNAPSHOT";
        	}else if(authority.contains("用户")) {
        		type = "USER";
        	}else if(authority.contains("作业")) {
        		type = "JOB";
        	}else if(authority.contains("角色")) {
        		type = "ROLE";
        	}else if(authority.contains("数据库路径")) {
        		type = "DIR";
        	}else if(authority.contains("UDT")) {
        		type = "OBJECT";
        	}
        	if(!authority.contains("任何")) {
        		any = "";
        	}
        	if(isDatabase) {
        		return action+" "+any+" "+type;
        	}else {
        		return action+" ON";
        	}
    	}
    	return "";
    }
    
    public static String getDDL(
        DBRProgressMonitor monitor,
        String objectType,
        XuguTableBase object,
        XuguDDLFormat ddlFormat,
        Map<String, Object> options) throws DBException
    {
        String objectFullName = DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL);
        monitor.beginTask("Load sources for " + objectType + " '" + objectFullName + "'...", 1);
        Connection conn = object.getDataSource().getConnection();
        Parsing pp = new Parsing();
        String ddl = pp.loadDDL((com.xugu.cloudjdbc.Connection)conn, object.getSchema().getName(), object.getName());
        return ddl;
    }

    public static int getDBIdleTime(Connection conn) {
    	try {
    		Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SHOW MAX_IDLE_TIME");
			if(rs.next()) {
				return rs.getInt(1);
			}
			return -1;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
    }
    
    public static void setCurrentSchema(JDBCSession session, String schema) throws SQLException {
        JDBCUtils.executeSQL(session,
            "SET CURRENT_SCHEMA=" + DBUtils.getQuotedIdentifier(session.getDataSource(), schema));
    }

    public static String getCurrentSchema(JDBCSession session, String role) throws SQLException {
    	String sql = "SHOW CURRENT_SCHEMA";
    	JDBCStatement s = session.createStatement();
    	JDBCResultSet rs = s.executeQuery(sql);
    	if (rs.next()) {
    		String res = rs.getString(1);
    		rs.close();
    		s.close();
            return res;
    	}else {
    		rs.close();
    		s.close();
    		return null;
    	}
    }

    public static String normalizeSourceName(XuguSourceObject object, boolean body)
    {
        try {
            String source = body ?
                ((DBPScriptObjectExt)object).getExtendedDefinitionText(null) :
                object.getObjectDefinitionText(null, DBPScriptObject.EMPTY_OPTIONS);
            if (source == null) {
                return null;
            }
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                object.getSourceType() + (body ? "\\s+BODY" : "") +
                "\\s(\\s*)([\\w$\\.]+)[\\s\\(]+", java.util.regex.Pattern.CASE_INSENSITIVE);
            final Matcher matcher = pattern.matcher(source);
            if (matcher.find()) {
                String objectName = matcher.group(2);
                if (objectName.indexOf('.') == -1) {
                    if (!objectName.equalsIgnoreCase(object.getName())) {
                        object.setName(DBObjectNameCaseTransformer.transformObjectName(object, objectName));
                        object.getDataSource().getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object));
                    }
                    return source;
                }
            }
            return source.trim();
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }

    public static void addSchemaChangeActions(List<DBEPersistAction> actions, XuguSourceObject object)
    {
        actions.add(0, new SQLDatabasePersistAction(
            "Set target schema",
            "SET CURRENT_SCHEMA=" + object.getSchema().getName(),
            DBEPersistAction.ActionType.INITIALIZER));
        if(object.getDataSource().getDefaultObject()!=null) {
        	if (object.getSchema() != object.getDataSource().getDefaultObject()) {
                actions.add(new SQLDatabasePersistAction(
                    "Set current schema",
                    "SET CURRENT_SCHEMA=" + object.getDataSource().getDefaultObject().getName(),
                    DBEPersistAction.ActionType.FINALIZER));
            }
        }
    }

    //xfc 修改了获取对象状态的sql和逻辑
    public static boolean getObjectStatus(
        DBRProgressMonitor monitor,
        XuguStatefulObject object,
        XuguObjectType objectType)
        throws DBCException
    {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, object, "Refresh state of " + objectType.getTypeName() + " '" + object.getName() + "'")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM ALL_OBJECTS WHERE OBJ_TYPE=? AND SCHEMA_ID=? AND OBJ_NAME=?")) {
                //在xugu数据库中 obj_type字段为int类型
            	dbStat.setString(1, objectType.getTypeName());
                dbStat.setLong(2, object.getSchema().getId());
                dbStat.setString(3, DBObjectNameCaseTransformer.transformObjectName(object, object.getName()));
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        return true;
                    } else {
                        log.warn(objectType.getTypeName() + " '" + object.getName() + "' not found in system dictionary");
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, object.getDataSource());
        }
    }

    public static String insertCreateReplace(XuguSourceObject object, boolean body, String source) {
        String sourceType = object.getSourceType().name();
        if (body) {
            sourceType += " BODY";
        }
        Pattern srcPattern = Pattern.compile("^(" + sourceType + ")\\s+(\"{0,1}\\w+\"{0,1})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = srcPattern.matcher(source);
        if (matcher.find()) {
            return
                "CREATE OR REPLACE " + matcher.group(1) + " " +
                DBUtils.getQuotedIdentifier(object.getSchema()) + "." + matcher.group(2) +
                source.substring(matcher.end());
        }
        return source;
    }
    
    public static String getObjectList(XuguDataSource source, DBRProgressMonitor monitor,  String schemaName, String Type, String tableName) {
    	try {
			Collection<XuguTable> tableList = null;
			Collection<XuguView> viewList = null;
			Collection<XuguSequence> seqList = null;
			Collection<XuguPackage> pacList = null;
			Collection<XuguProcedureStandalone> procList = null;
			Collection<XuguTrigger> triList = null;
			List<XuguTableColumn> colList = null;
			switch(Type) {
			case "TABLE":
				tableList = source.schemaCache.getCachedObject(schemaName).getTables(monitor);
				break;
			case "VIEW":
				viewList = source.schemaCache.getCachedObject(schemaName).getViews(monitor);
				break;
			case "SEQUENCE":
				seqList = source.schemaCache.getCachedObject(schemaName).getSequences(monitor);
				break;
			case "PACKAGE":
				pacList = source.schemaCache.getCachedObject(schemaName).getPackages(monitor);
				break;
			case "PROCEDURE":
				procList = source.schemaCache.getCachedObject(schemaName).getProcedures(monitor);
				break;
			case "TRIGGER":
				triList = source.schemaCache.getCachedObject(schemaName).getTable(monitor, tableName).getTriggers(monitor);
				break;
			case "COLUMN":
				XuguSchema schema = source.getSchema(monitor, schemaName);
				XuguTable table = source.schemaCache.getCachedObject(schemaName).getTable(monitor, tableName);
				colList = source.schemaCache.getCachedObject(schemaName).tableCache.getChildren(monitor, schema, table);
				break;
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
				Iterator<XuguTrigger> it = triList.iterator();
				XuguTrigger trigger = it.next();
				while(it.hasNext()) {
					res += trigger.getTable().getName()+"."+trigger.getName()+",";
				}
				if(res.length()>0) {
					res = res.substring(0, res.length()-1);
				}
				return res;
			}
			if(colList!=null && colList.size()>0) {
				String res="";
				Iterator<XuguTableColumn> it = colList.iterator();
				while(it.hasNext()) {
					res += it.next().getName()+",";
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
    
    public static DBException createDBException(String reason) 
    {
    	return new DBException(reason);
    }
}
