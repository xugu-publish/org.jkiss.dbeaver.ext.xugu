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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.ext.xugu.model.source.XuguStatefulObject;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;

import com.xugu.ddl.Parsing;

//import com.xugu.ddl.Parsing;
//
//import utils.Util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * Xugu utils
 */
public class XuguUtils {

    private static final Log log = Log.getLog(XuguUtils.class);

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
        String ddl = new Parsing().loadDDL(conn, object.getSchema().getName(), object.getName());
        return ddl;
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
            	dbStat.setInt(1, Integer.parseInt(objectType.getTypeName()));
                dbStat.setLong(2, object.getSchema().getID());
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
}
