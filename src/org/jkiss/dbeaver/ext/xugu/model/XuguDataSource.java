/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.XuguExecuteSQL_DBA;
import org.jkiss.dbeaver.ext.xugu.XuguExecuteSQL_NORMAL;
import org.jkiss.dbeaver.ext.xugu.model.XuguCharset;
import org.jkiss.dbeaver.ext.xugu.model.plan.XuguPlanAnalyser;
import org.jkiss.dbeaver.ext.xugu.XuguExecuteSQL_SYSDBA;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.model.session.XuguServerSessionManager;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBAPasswordChangeInfo;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.*;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLQueryResult;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XuguDataSource
 * @author Luke
 */
public class XuguDataSource extends JDBCDataSource
    implements DBSObjectSelector, DBCQueryPlanner, IAdaptable {
    private static final Log log = Log.getLog(XuguDataSource.class);

    final public SchemaCache schemaCache = new SchemaCache();
    final public DatabaseCache databaseCache = new DatabaseCache();
    final DataTypeCache dataTypeCache = new DataTypeCache();
    
    private final TablespaceCache tablespaceCache = new TablespaceCache();
    final UserCache userCache = new UserCache();
    final ProfileCache profileCache = new ProfileCache();
    final public RoleCache roleCache = new RoleCache();
    
    private xuguOutputReader outputReader;
    private XuguSchema publicSchema;
    private String activeSchemaName;
    private boolean isAdmin;
    private boolean isAdminVisible;
    private boolean useRuleHint;
    /**
     * userRole 角色属性，用于在查询时设置表名的前缀
     */
    private String userRole;
    private String roleFlag;
    
    private List<XuguCharset> charsets;

    private final Map<String, Boolean> availableViews = new HashMap<>();

    public XuguDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException {
        super(monitor, container, new XuguSQLDialect());
        this.outputReader = new xuguOutputReader();
    }

    @Override
    public Object getDataSourceFeature(String featureId) {
        switch (featureId) {
            case DBConstants.FEATURE_MAX_STRING_LENGTH:
                return 4000;
            default:
            	return super.getDataSourceFeature(featureId);
        }
    }

    public Boolean isViewAvailable(@NotNull DBRProgressMonitor monitor, @NotNull String schemaName, @NotNull String viewName) {
        viewName = viewName.toUpperCase();
        Boolean available;
        synchronized (availableViews) {
            available = availableViews.get(viewName);
        }
        if (available == null) {
            try {
                try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Check view existence")) {
                	String target = "SELECT 1 FROM " + DBUtils.getQuotedIdentifier(this, schemaName) + "." +
                            DBUtils.getQuotedIdentifier(this, viewName) + " WHERE 1<>1";
                    try (final JDBCPreparedStatement dbStat = session.prepareStatement(
                        "SELECT 1 FROM " + DBUtils.getQuotedIdentifier(this, schemaName) + "." +
                        DBUtils.getQuotedIdentifier(this, viewName) + " WHERE 1<>1"))
                    {
                        dbStat.setFetchSize(1);
                        dbStat.execute();
                        available = true;
                    }
                }
            } catch (SQLException e) {
                available = false;
            }
            synchronized (availableViews) {
                availableViews.put(viewName, available);
            }
        }
        return available;
    }

    @Override
    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, JDBCRemoteInstance remoteInstance, @NotNull String purpose) throws DBCException {
        try {
            return super.openConnection(monitor, remoteInstance, purpose);
        } catch (DBCException e) {
        	System.out.println("EEEERror code "+e.getErrorCode());
            if (e.getErrorCode() == XuguConstants.EC_PASSWORD_EXPIRED) {
                if (changeExpiredPassword(monitor, purpose)) {
                    // Retry
                    return openConnection(monitor, remoteInstance, purpose);
                }
            }
            throw e;
        }
    }

    private boolean changeExpiredPassword(DBRProgressMonitor monitor, String purpose) {
        DBPConnectionConfiguration connectionInfo = getContainer().getActualConnectionConfiguration();
        DBAPasswordChangeInfo passwordInfo = DBUserInterface.getInstance().promptUserPasswordChange("Password has expired. Set new password.", connectionInfo.getUserName(), connectionInfo.getUserPassword());
        if (passwordInfo == null) {
            return false;
        }

        // Obtain connection
        try {
            if (passwordInfo.getNewPassword() == null) {
                throw new DBException("You can't set empty password");
            }
            Properties connectProps = getAllConnectionProperties(monitor, purpose, connectionInfo);
            connectProps.setProperty("xugu.jdbc.newPassword", passwordInfo.getNewPassword());

            final String url = getConnectionURL(connectionInfo);
            monitor.subTask("Connecting for expired password change");
            Driver driverInstance = getDriverInstance(monitor);
            try (Connection connection = driverInstance.connect(url, connectProps)) {
                if (connection == null) {
                    throw new DBCException("Null connection returned");
                }
            }

            connectionInfo.setUserPassword(passwordInfo.getNewPassword());
            getContainer().getConnectionConfiguration().setUserPassword(passwordInfo.getNewPassword());
            getContainer().getRegistry().flushConfig();
            return true;
        }
        catch (Exception e) {
            DBUserInterface.getInstance().showError("Error changing password", "Error changing expired password", e);
            return false;
        }
    }

    @Override
    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, boolean setActiveObject) throws DBCException {
        if (outputReader == null) {
            outputReader = new xuguOutputReader();
        }
        // Enable DBMS output
        outputReader.enableServerOutput(
            monitor,
            context,
            outputReader.isServerOutputEnabled());
        if (setActiveObject) {
            setCurrentSchema(monitor, context, getDefaultObject());
        }

        {
            try (JDBCSession session = context.openSession(monitor, DBCExecutionPurpose.META, "Set connection parameters")) {
             // Read charsets and collations
                {
                    charsets = new ArrayList<>();
                    try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW CHARSET")) {
                        try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                            while (dbResult.next()) {
                                XuguCharset charset = new XuguCharset(this, dbResult);
                                charsets.add(charset);
                            }
                        }
                    } catch (SQLException ex) {
                        // Engines are not supported. Shame on it. Leave this list empty
                    }
                    Collections.sort(charsets, DBUtils.<XuguCharset>nameComparator());
                    
                }
            }
        }
    }

    @Override
    protected String getConnectionUserName(@NotNull DBPConnectionConfiguration connectionInfo) {
        final String role = connectionInfo.getProviderProperty(XuguConstants.PROP_INTERNAL_LOGON);
        return role == null ? connectionInfo.getUserName() : connectionInfo.getUserName() + " AS " + role;
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(@NotNull JDBCDatabaseMetaData metaData) {
        return new JDBCDataSourceInfo(metaData);
    }

    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error) {
        Throwable rootCause = GeneralUtils.getRootCause(error);
        if (rootCause instanceof SQLException && ((SQLException) rootCause).getErrorCode() == XuguConstants.EC_FEATURE_NOT_SUPPORTED) {
            return ErrorType.FEATURE_UNSUPPORTED;
        }
        return super.discoverErrorType(error);
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, DBPDriver driver, String purpose, DBPConnectionConfiguration connectionInfo) throws DBCException {
        Map<String, String> connectionsProps = new HashMap<String, String>();
        if (CommonUtils.toBoolean(connectionInfo.getProviderProperty(XuguConstants.OS_AUTH_PROP))) {
            connectionsProps.put("v$session.osuser", System.getProperty(StandardConstants.ENV_USER_NAME));
        }
        return connectionsProps;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isAdminVisible() {
        return isAdmin || isAdminVisible;
    }

    public boolean isUseRuleHint() {
        return useRuleHint;
    }

    public String getRoleFlag() {
    	return this.roleFlag;
    }

    @Association
    public Collection<XuguDatabase> getDatabases(DBRProgressMonitor monitor) throws DBException {
        return databaseCache.getAllObjects(monitor, this);
    }
    
    @Association
    public XuguDatabase getDatabase(DBRProgressMonitor monitor, String name) throws DBException {
        return databaseCache.getObject(monitor, this, name);
    }
    
    @Association
    public Collection<XuguSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        return schemaCache.getAllObjects(monitor, this);
    }

    @Association
    public XuguSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
        if (publicSchema != null && publicSchema.getName().equals(name)) {
        	System.out.println("inhere 2");
            return publicSchema;
        }
        return schemaCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<XuguTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException {
        return getTablespaceCache().getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguUser> getUsers(DBRProgressMonitor monitor) throws DBException {
        return userCache.getAllObjects(monitor, this);
    }

    @Association
    public XuguUser getUser(DBRProgressMonitor monitor, String name) throws DBException {
        return userCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<XuguUserProfile> getProfiles(DBRProgressMonitor monitor) throws DBException {
        return profileCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguRole> getRoles(DBRProgressMonitor monitor) throws DBException {
        return roleCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguSynonym> getPublicSynonyms(DBRProgressMonitor monitor) throws DBException {
        return publicSchema.getSynonyms(monitor);
    }

    public boolean isAtLeastV9() {
        return getInfo().getDatabaseVersion().getMajor() >= 9;
    }

    public boolean isAtLeastV10() {
        return getInfo().getDatabaseVersion().getMajor() >= 10;
    }

    public boolean isAtLeastV11() {
        return getInfo().getDatabaseVersion().getMajor() >= 11;
    }

    public boolean isAtLeastV12() {
        return getInfo().getDatabaseVersion().getMajor() >= 12;
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        super.initialize(monitor);

        DBPConnectionConfiguration connectionInfo = getContainer().getConnectionConfiguration();
        
        {
            String useRuleHintProp = connectionInfo.getProviderProperty(XuguConstants.PROP_USE_RULE_HINT);
            if (useRuleHintProp != null) {
                useRuleHint = CommonUtils.getBoolean(useRuleHintProp, false);
            }
        }
        //xfc 从连接信息中获取userRole
        this.userRole = connectionInfo.getProviderProperty(XuguConstants.PROP_INTERNAL_LOGON);
        if("SYSDBA".equals(this.userRole)) {
        	this.roleFlag = "SYS";
        }else if("DBA".equals(this.userRole)) {
        	this.roleFlag = "DBA";
        }else {
        	this.roleFlag = "ALL";
        }
        this.publicSchema = new XuguSchema(this, 1, XuguConstants.USER_PUBLIC);
        {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load data source meta info")) {
                // Get active schema
                this.setActiveSchemaName(XuguUtils.getCurrentSchema(session, this.userRole));
                if (this.getActiveSchemaName() != null) {
                    if (this.getActiveSchemaName().isEmpty()) {
                        this.setActiveSchemaName(null);
                    }
                }

            } catch (SQLException e) {
                //throw new DBException(e);
                log.warn(e);
            }
        }
        // Cache data types
        {
            List<XuguDataType> dtList = new ArrayList<>();
            for (Map.Entry<String, XuguDataType.TypeDesc> predefinedType : XuguDataType.PREDEFINED_TYPES.entrySet()) {
                XuguDataType dataType = new XuguDataType(this, predefinedType.getKey(), true);
                dtList.add(dataType);
                System.out.println("add data type "+dataType.name);
            }
            this.dataTypeCache.setCache(dtList);
            List<XuguDataType> ttt = this.dataTypeCache.getCachedObjects();
            for(XuguDataType t:ttt) {
            	System.out.println("AAAAAA after add data type "+t.name);
            }
            System.out.println("");
        }
    }

    private void setActiveSchemaName(String currentSchema) {
		// TODO Auto-generated method stub
		
	}

	@Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        super.refreshObject(monitor);

        this.schemaCache.clearCache();
        //this.dataTypeCache.clearCache();
        this.tablespaceCache.clearCache();
        this.userCache.clearCache();
        this.profileCache.clearCache();
        this.roleCache.clearCache();
        this.setActiveSchemaName(null);

        this.initialize(monitor);

        return this;
    }

    @Override
    public Collection<XuguSchema> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        return getSchemas(monitor);
    }

    @Override
    public XuguSchema getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException {
        return getSchema(monitor, childName);
    }

    @Override
    public Class<? extends XuguSchema> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        return XuguSchema.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException {

    }

    @Override
    public boolean supportsDefaultChange() {
        return true;
    }

    @Nullable
    @Override
    public XuguSchema getDefaultObject() {
        return getActiveSchemaName() == null ? null : schemaCache.getCachedObject(getActiveSchemaName());
    }

    //设为活动对象
    @Override
    public void setDefaultObject(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object)
        throws DBException {
        final XuguSchema oldSelectedEntity = getDefaultObject();
        if (!(object instanceof XuguSchema)) {
            throw new IllegalArgumentException("Invalid object type: " + object);
        }
        for (JDBCExecutionContext context : getDefaultInstance().getAllContexts()) {
            setCurrentSchema(monitor, context, (XuguSchema) object);
        }
        setActiveSchemaName(object.getName());

        // Send notifications
        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        if (this.getActiveSchemaName() != null) {
            DBUtils.fireObjectSelect(object, true);
        }
    }

    @Override
    public boolean refreshDefaultObject(@NotNull DBCSession session) throws DBException {
        try {
            final String currentSchema = XuguUtils.getCurrentSchema((JDBCSession) session, this.userRole);
            if (currentSchema != null && !CommonUtils.equalObjects(currentSchema, getActiveSchemaName())) {
                final XuguSchema newSchema = schemaCache.getCachedObject(currentSchema);
                if (newSchema != null) {
                    setDefaultObject(session.getProgressMonitor(), newSchema);
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new DBException(e, this);
        }
    }

    private void setCurrentSchema(DBRProgressMonitor monitor, JDBCExecutionContext executionContext, XuguSchema object) throws DBCException {
        if (object == null) {
            log.debug("Null current schema");
            return;
        }
        try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Set active schema")) {
            XuguUtils.setCurrentSchema(session, object.getName());
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
    }

//    @NotNull
//    @Override
//    public DBCPlan planQueryExecution(@NotNull DBCSession session, @NotNull String query) throws DBException {
//        XuguPlanAnalyser plan = new XuguPlanAnalyser(this, (JDBCSession) session, query);
//        plan.explain();
//        return plan;
//    }

//    @NotNull
//    @Override
//    public DBCPlanStyle getPlanStyle() {
//        return DBCPlanStyle.PLAN;
//    }

    @Nullable
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new XuguStructureAssistant(this));
        } else if (adapter == DBCServerOutputReader.class) {
            return adapter.cast(outputReader);
        } else if (adapter == DBAServerSessionManager.class) {
            return adapter.cast(new XuguServerSessionManager(getDefaultInstance().getDefaultContext(false)));
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void cancelStatementExecute(DBRProgressMonitor monitor, JDBCStatement statement) throws DBException {
        if (driverSupportsQueryCancel()) {
            super.cancelStatementExecute(monitor, statement);
        } else {
            // xugu server doesn't support single query cancel?
            // But we could try to cancel all
            try {
                Connection connection = statement.getConnection().getOriginal();
                BeanUtils.invokeObjectMethod(connection, "cancel");
            } catch (Throwable e) {
                throw new DBException("Can't cancel session queries", e, this);
            }
        }
    }

    private boolean driverSupportsQueryCancel() {
        return true;
    }

    @NotNull
    @Override
    public XuguDataSource getDataSource() {
        return this;
    }

    @NotNull
    @Override
    public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType) {
        if ((typeName.equals(XuguConstants.TYPE_NAME_XML) || typeName.equals(XuguConstants.TYPE_FQ_XML))) {
            return DBPDataKind.CONTENT;
        }
        DBPDataKind dataKind = XuguDataType.getDataKind(typeName);
        if (dataKind != null) {
            return dataKind;
        }
        return super.resolveDataKind(typeName, valueType);
    }

    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public DBSDataType getLocalDataType(String typeName) {
        return dataTypeCache.getCachedObject(typeName);
    }

    @Nullable
    @Override
    public DBSDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull String typeFullName) throws DBException {
        int divPos = typeFullName.indexOf(SQLConstants.STRUCT_SEPARATOR);
        if (divPos == -1) {
            // Simple type name
            return getLocalDataType(typeFullName);
        } else {
            String schemaName = typeFullName.substring(0, divPos);
            String typeName = typeFullName.substring(divPos + 1);
            XuguSchema schema = getSchema(monitor, schemaName);
            if (schema == null) {
                return null;
            }
            return schema.getDataType(monitor, typeName);
        }
    }

//    @Nullable
//    public String getPlanTableName(JDBCSession session)
//        throws DBException
//    {
//        if (planTableName == null) {
//            String[] candidateNames;
//            String tableName = getContainer().getPreferenceStore().getString(XuguConstants.PREF_EXPLAIN_TABLE_NAME);
//            if (!CommonUtils.isEmpty(tableName)) {
//                candidateNames = new String[]{tableName};
//            } else {
//                candidateNames = new String[]{"PLAN_TABLE", "TOAD_PLAN_TABLE"};
//            }
//            for (String candidate : candidateNames) {
//                try {
//                    JDBCUtils.executeSQL(session, "SELECT 1 FROM " + candidate);
//                } catch (SQLException e) {
//                    // No such table
//                    continue;
//                }
//                planTableName = candidate;
//                break;
//            }
//            if (planTableName == null) {
//                final String newPlanTableName = candidateNames[0];
//                // Plan table not found - try to create new one
//                if (!UIUtils.confirmAction(
//                    "xugu PLAN_TABLE missing",
//                    "PLAN_TABLE not found in current user's session. " +
//                        "Do you want DBeaver to create new PLAN_TABLE (" + newPlanTableName + ")?")) {
//                    return null;
//                }
//                planTableName = createPlanTable(session, newPlanTableName);
//            }
//        }
//        return planTableName;
//    }

//    private String createPlanTable(JDBCSession session, String tableName) throws DBException {
//        try {
//            JDBCUtils.executeSQL(session, XuguConstants.PLAN_TABLE_DEFINITION.replace("${TABLE_NAME}", tableName));
//        } catch (SQLException e) {
//            throw new DBException("Error creating PLAN table", e, this);
//        }
//        return tableName;
//    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            //return new QueryTransformerRowNum();
        }
        return super.createQueryTransformer(type);
    }

    private Pattern ERROR_POSITION_PATTERN = Pattern.compile(".+\\s+line ([0-9]+), column ([0-9]+)");
    private Pattern ERROR_POSITION_PATTERN_2 = Pattern.compile(".+\\s+at line ([0-9]+)");
    private Pattern ERROR_POSITION_PATTERN_3 = Pattern.compile(".+\\s+at position\\: ([0-9]+)");

    @Nullable
    @Override
    public ErrorPosition[] getErrorPosition(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @NotNull String query, @NotNull Throwable error) {
        while (error instanceof DBException) {
            if (error.getCause() == null) {
                break;
            }
            error = error.getCause();
        }
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            List<ErrorPosition> positions = new ArrayList<>();
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            while (matcher.find()) {
                DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                pos.info = matcher.group(1);
                pos.line = Integer.parseInt(matcher.group(1)) - 1;
                pos.position = Integer.parseInt(matcher.group(2)) - 1;
                positions.add(pos);
            }
            if (positions.isEmpty()) {
                matcher = ERROR_POSITION_PATTERN_2.matcher(message);
                while (matcher.find()) {
                    DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                    pos.info = matcher.group(1);
                    pos.line = Integer.parseInt(matcher.group(1)) - 1;
                    positions.add(pos);
                }
            }
            if (positions.isEmpty()) {
                matcher = ERROR_POSITION_PATTERN_3.matcher(message);
                while (matcher.find()) {
                    DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                    pos.info = matcher.group(1);
                    pos.position = Integer.parseInt(matcher.group(1)) - 1;
                    positions.add(pos);
                }
            }

            if (!positions.isEmpty()) {
                return positions.toArray(new ErrorPosition[positions.size()]);
            }
        }
        if (error.getCause() != null) {
            // Maybe xuguDatabaseException
            try {
                Object errorPosition = BeanUtils.readObjectProperty(error.getCause(), "errorPosition");
                if (errorPosition instanceof Number) {
                    DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                    pos.position = ((Number) errorPosition).intValue();
                    return new ErrorPosition[]{pos};
                }
            } catch (Exception e) {
                // Nope, its not it
            }

        }
        if (error instanceof SQLException && SQLState.SQL_42000.getCode().equals(((SQLException) error).getSQLState())) {
            try (JDBCSession session = (JDBCSession) context.openSession(monitor, DBCExecutionPurpose.UTIL, "Extract last error position")) {
                try (CallableStatement stat = session.prepareCall(
                    "declare\n" +
                        "  l_cursor integer default dbms_sql.open_cursor; \n" +
                        "begin \n" +
                        "  begin \n" +
                        "  dbms_sql.parse(  l_cursor, ?, dbms_sql.native ); \n" +
                        "    exception \n" +
                        "      when others then ? := dbms_sql.last_error_position; \n" +
                        "    end; \n" +
                        "    dbms_sql.close_cursor( l_cursor );\n" +
                        "end;")) {
                    stat.setString(1, query);
                    stat.registerOutParameter(2, Types.INTEGER);
                    stat.execute();
                    int errorPos = stat.getInt(2);
                    if (errorPos <= 0) {
                        return null;
                    }

                    DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                    pos.position = errorPos;
                    return new ErrorPosition[]{pos};

                } catch (SQLException e) {
                    // Something went wrong
                    log.debug("Can't extract parse error info: " + e.getMessage());
                }
            }
        }
        return null;
    }

    private class xuguOutputReader implements DBCServerOutputReader {
        @Override
        public boolean isServerOutputEnabled() {
            return getContainer().getPreferenceStore().getBoolean(XuguConstants.PREF_DBMS_OUTPUT);
        }

        @Override
        public boolean isAsyncOutputReadSupported() {
            return false;
        }

        public void enableServerOutput(DBRProgressMonitor monitor, DBCExecutionContext context, boolean enable) throws DBCException {
//            String sql = enable ?
//                "BEGIN DBMS_OUTPUT.ENABLE(" + XuguConstants.MAXIMUM_DBMS_OUTPUT_SIZE + "); END;" :
//                "BEGIN DBMS_OUTPUT.DISABLE; END;";
//            try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, (enable ? "Enable" : "Disable ") + "DBMS output")) {
//                JDBCUtils.executeSQL((JDBCSession) session, sql);
//            } catch (SQLException e) {
//                throw new DBCException(e, XuguDataSource.this);
//            }
        }

        @Override
        public void readServerOutput(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @Nullable SQLQueryResult queryResult, @Nullable DBCStatement statement, @NotNull PrintWriter output) throws DBCException {
        	//do nothing
        }
    }

    static class DatabaseCache extends JDBCStructLookupCache<XuguDataSource, XuguDatabase, XuguSchema> {
        
        public DatabaseCache() {
			super("DATABASE_NAME");
		}
        
        @Override
		public JDBCStatement prepareLookupStatement(JDBCSession session, XuguDataSource owner, XuguDatabase object,
				String objectName) throws SQLException {
			return session.prepareStatement(
	                "SELECT * FROM "+owner.roleFlag+"_DATABASES");
		}
        
        @Override
        protected XuguDatabase fetchObject(@NotNull JDBCSession session, @NotNull XuguDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new XuguDatabase(owner, resultSet);
        }

		@Override
		protected JDBCStatement prepareChildrenStatement(JDBCSession session, XuguDataSource owner,
				XuguDatabase forDB) throws SQLException {
			//xfc 修改了获取列信息的sql
            StringBuilder sql = new StringBuilder(500);
            sql.append("SELECT * FROM ");
        	sql.append(owner.roleFlag);
        	sql.append("_SCHEMAS");
        	if (forDB != null) {
                sql.append(" where DB_ID=(SELECT DB_ID FROM ");
                sql.append(owner.roleFlag);
                sql.append("_DATABASES WHERE DB_NAME='");
                sql.append(forDB.getName());
                sql.append("')");
            }
        	JDBCStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected XuguSchema fetchChild(JDBCSession session, XuguDataSource owner, XuguDatabase db,
				JDBCResultSet dbResult) throws SQLException, DBException {
			return new XuguSchema(owner, db, dbResult);
		}
    }
    
    static class SchemaCache extends JDBCObjectCache<XuguDataSource, XuguSchema> {
        SchemaCache() {
            setListOrderComparator(DBUtils.<XuguSchema>nameComparator());
        }
        
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguDataSource owner) throws SQLException {
            StringBuilder schemasQuery = new StringBuilder();

            //过滤条件
            DBSObjectFilter schemaFilters = owner.getContainer().getObjectFilter(XuguSchema.class, null, false);
        	//xfc 根据owner的用户角色选取不同的语句来查询schema
        	schemasQuery.append("SELECT * FROM ");
        	schemasQuery.append(owner.roleFlag);
        	schemasQuery.append("_SCHEMAS");
            System.out.println("QQQQQQ "+schemasQuery.toString());
            JDBCPreparedStatement dbStat = session.prepareStatement(schemasQuery.toString());
            if (schemaFilters != null) {
            	System.out.println("FFFilter is not null! 2");
                JDBCUtils.setFilterParameters(dbStat, 1, schemaFilters);
            }
            System.out.println("QQQQQQ "+dbStat.getQueryString());
            return dbStat;
        }

        @Override
        protected XuguSchema fetchObject(@NotNull JDBCSession session, @NotNull XuguDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new XuguSchema(owner, resultSet);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, XuguDataSource owner, Iterator<XuguSchema> objectIter) {
            setListOrderComparator(DBUtils.<XuguSchema>nameComparator());
            // Add predefined types
            if (!CommonUtils.isEmpty(owner.getActiveSchemaName()) && getCachedObject(owner.getActiveSchemaName()) == null) {
                cacheObject(
                    new XuguSchema(owner, 100, owner.getActiveSchemaName()));
            }
        }
    }
    
    
    static class DataTypeCache extends JDBCObjectCache<XuguDataSource, XuguDataType> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguDataSource owner) throws SQLException {
        	//do nothing
        	return session.prepareStatement("");
        }

        @Override
        protected XuguDataType fetchObject(@NotNull JDBCSession session, @NotNull XuguDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            //return new XuguDataType(owner, resultSet);
        	return null;
        }
    }

    static class TablespaceCache extends JDBCObjectCache<XuguDataSource, XuguTablespace> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguDataSource owner) throws SQLException {
            //xfc 修改了获取表空间信息的sql语句
        	return session.prepareStatement(
                "SELECT * FROM "+owner.roleFlag+"_TABLESPACES");
        }

        @Override
        protected XuguTablespace fetchObject(@NotNull JDBCSession session, @NotNull XuguDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new XuguTablespace(owner, resultSet);
        }
    }

    static class UserCache extends JDBCObjectCache<XuguDataSource, XuguUser> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguDataSource owner) throws SQLException {
            //xfc 修改了获取用户信息的sql语句
        	return session.prepareStatement(
                "SELECT * FROM "+owner.roleFlag+"_USERS");
        }

        @Override
        protected XuguUser fetchObject(@NotNull JDBCSession session, @NotNull XuguDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new XuguUser(owner, resultSet);
        }
    }

    static class RoleCache extends JDBCObjectCache<XuguDataSource, XuguRole> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguDataSource owner) throws SQLException {
            return session.prepareStatement(
                "SELECT * FROM SYS_USERS WHERE IS_ROLE=true");
        }

        @Override
        protected XuguRole fetchObject(@NotNull JDBCSession session, @NotNull XuguDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new XuguRole(owner, resultSet);
        }
    }

    static class ProfileCache extends JDBCStructCache<XuguDataSource, XuguUserProfile, XuguUserProfile.ProfileResource> {
        protected ProfileCache() {
            super("PROFILE");
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguDataSource owner) throws SQLException {
            return session.prepareStatement(
                "SELECT DISTINCT PROFILE FROM DBA_PROFILES ORDER BY PROFILE");
        }

        @Override
        protected XuguUserProfile fetchObject(@NotNull JDBCSession session, @NotNull XuguDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new XuguUserProfile(owner, resultSet);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull XuguDataSource dataSource, @Nullable XuguUserProfile forObject) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT RESOURCE_NAME,RESOURCE_TYPE,LIMIT FROM DBA_PROFILES " +
                    (forObject == null ? "" : "WHERE PROFILE=? ") +
                    "ORDER BY RESOURCE_NAME");
            if (forObject != null) {
                dbStat.setString(1, forObject.getName());
            }
            return dbStat;
        }

        @Override
        protected XuguUserProfile.ProfileResource fetchChild(@NotNull JDBCSession session, @NotNull XuguDataSource dataSource, @NotNull XuguUserProfile parent, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new XuguUserProfile.ProfileResource(parent, dbResult);
        }
    }

    public Collection<XuguCharset> getCharsets()
    {
        return charsets;
    }

    public XuguCharset getCharset(String name)
    {
        for (XuguCharset charset : charsets) {
            if (charset.getName().equals(name)) {
                return charset;
            }
        }
        return null;
    }

	public TablespaceCache getTablespaceCache() {
		return tablespaceCache;
	}

	public String getActiveSchemaName() {
		return activeSchemaName;
	}

	 @NotNull
    @Override
    public DBCPlan planQueryExecution(@NotNull DBCSession session, @NotNull String query) throws DBException {
        XuguPlanAnalyser plan = new XuguPlanAnalyser(this, (JDBCSession) session, query);
        plan.explain();
        return plan;
    }

    @NotNull
    @Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }
    
}
