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
import org.jkiss.dbeaver.ext.xugu.XuguExecuteSQL_DBA;
import org.jkiss.dbeaver.ext.xugu.XuguExecuteSQL_NORMAL;
import org.jkiss.dbeaver.ext.xugu.XuguExecuteSQL_SYSDBA;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.ext.xugu.model.source.XuguStatefulObject;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
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
        String ddl = JDBCUtils.generateTableDDL(monitor, (XuguTableBase)object, options, true);
        return ddl;
    }

    public static void setCurrentSchema(JDBCSession session, String schema) throws SQLException {
        JDBCUtils.executeSQL(session,
            "SET CURRENT_SCHEMA=" + DBUtils.getQuotedIdentifier(session.getDataSource(), schema));
    }

    public static String getCurrentSchema(JDBCSession session, String role) throws SQLException {
    	String sql = "SHOW CURRENT_SCHEMA";
    	return JDBCUtils.queryString(
	            session,
	            sql);
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
                    return source;//.substring(0, matcher.start(1)) + object.getSchema().getName() + "." + objectName + source.substring(matcher.end(2));
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

//    public static String getSource(DBRProgressMonitor monitor, XuguSourceObject sourceObject, boolean body, boolean insertCreateReplace) throws DBCException
//    {
//        if (sourceObject.getSourceType().isCustom()) {
//            log.warn("Can't read source for custom source objects");
//            return "-- ???? CUSTOM SOURCE";
//        }
//        final String sourceType = sourceObject.getSourceType().name();
//        final XuguSchema sourceOwner = sourceObject.getSchema();
//        if (sourceOwner == null) {
//            log.warn("No source owner for object '" + sourceObject.getName() + "'");
//            return null;
//        }
//        monitor.beginTask("Load sources for '" + sourceObject.getName() + "'...", 1);
//        String sysViewName = XuguConstants.VIEW_DBA_SOURCE;
//        if (!sourceObject.getDataSource().isViewAvailable(monitor, XuguConstants.SCHEMA_SYS, sysViewName)) {
//            sysViewName = XuguConstants.VIEW_ALL_SOURCE;
//        }
//        try (final JDBCSession session = DBUtils.openMetaSession(monitor, sourceOwner, "Load source code for " + sourceType + " '" + sourceObject.getName() + "'")) {
//            try (JDBCPreparedStatement dbStat = session.prepareStatement(
//                "SELECT TEXT FROM ALL_OBJECTS " +
//                    "WHERE TYPE=? AND OWNER=? AND NAME=? " +
//                    "ORDER BY LINE")) {
//                dbStat.setString(1, body ? sourceType + " BODY" : sourceType);
//                dbStat.setString(2, sourceOwner.getName());
//                dbStat.setString(3, sourceObject.getName());
//                dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
//                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
//                    StringBuilder source = null;
//                    int lineCount = 0;
//                    while (dbResult.next()) {
//                        if (monitor.isCanceled()) {
//                            break;
//                        }
//                        final String line = dbResult.getString(1);
//                        if (source == null) {
//                            source = new StringBuilder(200);
//                        }
//                        source.append(line);
//                        lineCount++;
//                        monitor.subTask("Line " + lineCount);
//                    }
//                    if (source == null) {
//                        return null;
//                    }
//                    if (insertCreateReplace) {
//                        return insertCreateReplace(sourceObject, body, source.toString());
//                    } else {
//                        return source.toString();
//                    }
//                }
//            }
//        } catch (SQLException e) {
//            throw new DBCException(e, sourceOwner.getDataSource());
//        } finally {
//            monitor.done();
//        }
//    }

    public static String getSysUserViewName(DBRProgressMonitor monitor, XuguDataSource dataSource, String viewName)
    {
        String dbaView = "DBA_" + viewName;
        if (dataSource.isViewAvailable(monitor, XuguConstants.SCHEMA_SYS, dbaView)) {
            return XuguConstants.SCHEMA_SYS + "." + dbaView;
        } else {
            return XuguConstants.SCHEMA_SYS + ".USER_" + viewName;
        }
    }

    public static String getSysCatalogHint(XuguDataSource dataSource)
    {
        return dataSource.isUseRuleHint() ? "/*+RULE*/" : "";
    }

    static <PARENT extends DBSObject> Object resolveLazyReference(
        DBRProgressMonitor monitor,
        PARENT parent,
        DBSObjectCache<PARENT,?> cache,
        DBSObjectLazy<?> referrer,
        Object propertyId)
        throws DBException
    {
        final Object reference = referrer.getLazyReference(propertyId);
        if (reference instanceof String) {
            Object object;
            if (monitor != null) {
                object = cache.getObject(
                    monitor,
                    parent,
                    (String) reference);
            } else {
                object = cache.getCachedObject((String) reference);
            }
            if (object != null) {
                return object;
            } else {
                log.warn("Object '" + reference + "' not found");
                return reference;
            }
        } else {
            return reference;
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
}
