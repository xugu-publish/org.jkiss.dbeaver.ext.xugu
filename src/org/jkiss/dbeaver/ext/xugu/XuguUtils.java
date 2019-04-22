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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Xugu utils
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
}
