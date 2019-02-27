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
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * OracleSchema
 */
public class XuguSchema extends XuguGlobalObject implements DBSSchema, DBPRefreshableObject, DBPSystemObject, DBSProcedureContainer
{
    private static final Log log = Log.getLog(XuguSchema.class);

    final public TableCache tableCache = new TableCache();
    final public MViewCache mviewCache = new MViewCache();
    final public ConstraintCache constraintCache = new ConstraintCache();
    final public ForeignKeyCache foreignKeyCache = new ForeignKeyCache();
    final public TriggerCache triggerCache = new TriggerCache();
    final public IndexCache indexCache = new IndexCache();
    final public DataTypeCache dataTypeCache = new DataTypeCache();
    final public SequenceCache sequenceCache = new SequenceCache();
    final public QueueCache queueCache = new QueueCache();
    final public PackageCache packageCache = new PackageCache();
    final public SynonymCache synonymCache = new SynonymCache();
    final public DBLinkCache dbLinkCache = new DBLinkCache();
    final public ProceduresCache proceduresCache = new ProceduresCache();
    final public JavaCache javaCache = new JavaCache();
    final public SchedulerJobCache schedulerJobCache = new SchedulerJobCache();
    final public SchedulerProgramCache schedulerProgramCache = new SchedulerProgramCache();
    final public RecycleBin recycleBin = new RecycleBin();

    private long id;
    private String name;
    private Date createTime;
    private transient XuguUser user;

    public XuguSchema(XuguDataSource dataSource, long id, String name)
    {
        super(dataSource, id > 0);
        this.id = id;
        this.name = name;
    }

    public XuguSchema(@NotNull XuguDataSource dataSource, @NotNull ResultSet dbResult)
    {
        super(dataSource, true);
        this.id = JDBCUtils.safeGetLong(dbResult, "USER_ID");
        this.name = JDBCUtils.safeGetString(dbResult, "USERNAME");
        if (CommonUtils.isEmpty(this.name)) {
            log.warn("Empty schema name fetched");
            this.name = "? " + super.hashCode();
        }
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
    }

    public boolean isPublic()
    {
        return XuguConstants.USER_PUBLIC.equals(this.name);
    }

    @Property(order = 200)
    public long getId()
    {
        return id;
    }

    @Property(order = 190)
    public Date getCreateTime() {
        return createTime;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    /**
     * User reference never read directly from database.
     * It is used by managers to create/delete/alter schemas
     * @return user reference or null
     */
    public XuguUser getUser()
    {
        return user;
    }

    public void setUser(XuguUser user)
    {
        this.user = user;
    }

    @Association
    public Collection<XuguTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return indexCache.getObjects(monitor, this, null);
    }

    @Association
    public Collection<XuguTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, XuguTable.class);
    }

    public XuguTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, this, name, XuguTable.class);
    }

    @Association
    public Collection<XuguView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, XuguView.class);
    }

    public XuguView getView(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, this, name, XuguView.class);
    }

    @Association
    public Collection<XuguMaterializedView> getMaterializedViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return mviewCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguDataType> getDataTypes(DBRProgressMonitor monitor)
        throws DBException
    {
        return dataTypeCache.getAllObjects(monitor, this);
    }

    public XuguDataType getDataType(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        XuguDataType type = dataTypeCache.getObject(monitor, this, name);
        if (type == null) {
            final XuguSynonym synonym = synonymCache.getObject(monitor, this, name);
            if (synonym != null && synonym.getObjectType() == XuguObjectType.TYPE) {
                Object object = synonym.getObject(monitor);
                if (object instanceof XuguDataType) {
                    return (XuguDataType)object;
                }
            }
            return null;
        } else {
            return type;
        }
    }

    @Association
    public Collection<XuguQueue> getQueues(DBRProgressMonitor monitor)
        throws DBException
    {
        return queueCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguSequence> getSequences(DBRProgressMonitor monitor)
        throws DBException
    {
        return sequenceCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguPackage> getPackages(DBRProgressMonitor monitor)
        throws DBException
    {
        return packageCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguProcedureStandalone> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return proceduresCache.getAllObjects(monitor, this);
    }

    @Override
    public XuguProcedureStandalone getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        return proceduresCache.getObject(monitor, this, uniqueName);
    }

    @Association
    public Collection<XuguSynonym> getSynonyms(DBRProgressMonitor monitor)
        throws DBException
    {
        return synonymCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguSchemaTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguTableTrigger> getTableTriggers(DBRProgressMonitor monitor)
            throws DBException
    {
        List<XuguTableTrigger> allTableTriggers = new ArrayList<>();
        for (XuguTableBase table : tableCache.getAllObjects(monitor, this)) {
            Collection<XuguTableTrigger> triggers = table.getTriggers(monitor);
            if (!CommonUtils.isEmpty(triggers)) {
                allTableTriggers.addAll(triggers);
            }
        }
        allTableTriggers.sort(Comparator.comparing(XuguTrigger::getName));
        return allTableTriggers;
    }

    @Association
    public Collection<XuguDBLink> getDatabaseLinks(DBRProgressMonitor monitor)
        throws DBException
    {
        return dbLinkCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguJavaClass> getJavaClasses(DBRProgressMonitor monitor)
        throws DBException
    {
        return javaCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguSchedulerJob> getSchedulerJobs(DBRProgressMonitor monitor)
            throws DBException
    {
        return schedulerJobCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguSchedulerProgram> getSchedulerPrograms(DBRProgressMonitor monitor)
            throws DBException
    {
        return schedulerProgramCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguRecycledObject> getRecycledObjects(DBRProgressMonitor monitor)
        throws DBException
    {
        return recycleBin.getAllObjects(monitor, this);
    }

    @Property(order = 90)
    public XuguUser getSchemaUser(DBRProgressMonitor monitor) throws DBException {
        return getDataSource().getUser(monitor, name);
    }

    @Override
    public Collection<DBSObject> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        List<DBSObject> children = new ArrayList<>();
        children.addAll(tableCache.getAllObjects(monitor, this));
        children.addAll(synonymCache.getAllObjects(monitor, this));
        children.addAll(packageCache.getAllObjects(monitor, this));
        return children;
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        final XuguTableBase table = tableCache.getObject(monitor, this, childName);
        if (table != null) {
            return table;
        }
        XuguSynonym synonym = synonymCache.getObject(monitor, this, childName);
        if (synonym != null) {
            return synonym;
        }
        return packageCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSEntity> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return DBSEntity.class;
    }

    @Override
    public synchronized void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        monitor.subTask("Cache tables");
        tableCache.getAllObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            tableCache.loadChildren(monitor, this, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache table indexes");
            indexCache.getObjects(monitor, this, null);
            monitor.subTask("Cache table constraints");
            constraintCache.getObjects(monitor, this, null);
            foreignKeyCache.getObjects(monitor, this, null);
        }
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        tableCache.clearCache();
        foreignKeyCache.clearCache();
        constraintCache.clearCache();
        indexCache.clearCache();
        packageCache.clearCache();
        proceduresCache.clearCache();
        triggerCache.clearCache();
        dataTypeCache.clearCache();
        sequenceCache.clearCache();
        synonymCache.clearCache();
        schedulerJobCache.clearCache();
        recycleBin.clearCache();
        return this;
    }

    @Override
    public boolean isSystem()
    {
        return ArrayUtils.contains(XuguConstants.SYSTEM_SCHEMAS, getName());
    }

    @Override
    public String toString()
    {
        return "Schema " + name;
    }

    private static XuguTableColumn getTableColumn(JDBCSession session, XuguTableBase parent, ResultSet dbResult) throws DBException
    {
        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
        XuguTableColumn tableColumn = columnName == null ? null : parent.getAttribute(session.getProgressMonitor(), columnName);
        if (tableColumn == null) {
            log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "'");
        }
        return tableColumn;
    }

    public static class TableCache extends JDBCStructLookupCache<XuguSchema, XuguTableBase, XuguTableColumn> {

        TableCache()
        {
            super("TABLE_NAME");
            setListOrderComparator(DBUtils.nameComparator());
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner, @Nullable XuguTableBase object, @Nullable String objectName) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "\tSELECT " + XuguUtils.getSysCatalogHint(owner.getDataSource()) + " t.OWNER,t.TABLE_NAME as TABLE_NAME,'TABLE' as OBJECT_TYPE,'VALID' as STATUS,t.TABLE_TYPE_OWNER,t.TABLE_TYPE,t.TABLESPACE_NAME,t.PARTITIONED,t.IOT_TYPE,t.IOT_NAME,t.TEMPORARY,t.SECONDARY,t.NESTED,t.NUM_ROWS \n" +
                    "\tFROM " + XuguUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "ALL_TABLES") + " t\n" +
                    "\tWHERE t.OWNER=? AND NESTED='NO'" + (object == null && objectName == null ? "": " AND t.TABLE_NAME=?") + "\n" +
                "UNION ALL\n" +
                    "\tSELECT " + XuguUtils.getSysCatalogHint(owner.getDataSource()) + " o.OWNER,o.OBJECT_NAME as TABLE_NAME,'VIEW' as OBJECT_TYPE,o.STATUS,NULL,NULL,NULL,'NO',NULL,NULL,o.TEMPORARY,o.SECONDARY,'NO',0 \n" +
                    "\tFROM " + XuguUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "OBJECTS") + " o \n" +
                    "\tWHERE o.OWNER=? AND o.OBJECT_TYPE='VIEW'" + (object == null && objectName == null  ? "": " AND o.OBJECT_NAME=?") + "\n"
                );
            int index = 1;
            dbStat.setString(index++, owner.getName());
            if (object != null || objectName != null) dbStat.setString(index++, object != null ? object.getName() : objectName);
            dbStat.setString(index++, owner.getName());
            if (object != null || objectName != null) dbStat.setString(index, object != null ? object.getName() : objectName);
            return dbStat;
        }

        @Override
        protected XuguTableBase fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            final String tableType = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
            if ("TABLE".equals(tableType)) {
                return new XuguTable(session.getProgressMonitor(), owner, dbResult);
            } else {
                return new XuguView(owner, dbResult);
            }
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner, @Nullable XuguTableBase forTable)
            throws SQLException
        {
            String colsView = "ALL_TAB_COLS";
            if (!owner.getDataSource().isViewAvailable(session.getProgressMonitor(), XuguConstants.SCHEMA_SYS, colsView)) {
                colsView = "ALL_TAB_COLUMNS";
            }
            StringBuilder sql = new StringBuilder(500);
            sql
                .append("SELECT ").append(XuguUtils.getSysCatalogHint(owner.getDataSource())).append("\nc.* " +
                    "FROM SYS.").append(colsView).append(" c\n" +
//                    "LEFT OUTER JOIN SYS.ALL_COL_COMMENTS cc ON CC.OWNER=c.OWNER AND cc.TABLE_NAME=c.TABLE_NAME AND cc.COLUMN_NAME=c.COLUMN_NAME\n" +
                    "WHERE c.OWNER=?");
            if (forTable != null) {
                sql.append(" AND c.TABLE_NAME=?");
            }
/*
            sql.append("\nORDER BY ");
            if (forTable != null) {
                sql.append("c.TABLE_NAME,");
            }
            sql.append("c.COLUMN_ID");
*/
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected XuguTableColumn fetchChild(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull XuguTableBase table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguTableColumn(session.getProgressMonitor(), table, dbResult);
        }

        @Override
        protected void cacheChildren(XuguTableBase parent, List<XuguTableColumn> oracleTableColumns) {
            oracleTableColumns.sort(DBUtils.orderComparator());
            super.cacheChildren(parent, oracleTableColumns);
        }

    }

    /**
     * Index cache implementation
     */
    class ConstraintCache extends JDBCCompositeCache<XuguSchema, XuguTableBase, XuguTableConstraint, XuguTableConstraintColumn> {
        ConstraintCache()
        {
            super(tableCache, XuguTableBase.class, "TABLE_NAME", "CONSTRAINT_NAME");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, XuguSchema owner, XuguTableBase forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder(500);
            sql
                .append("SELECT ").append(XuguUtils.getSysCatalogHint(owner.getDataSource())).append("\n" +
                    "c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.STATUS,c.SEARCH_CONDITION," +
                    "col.COLUMN_NAME,col.POSITION\n" +
                    "FROM SYS.ALL_CONSTRAINTS c, SYS.ALL_CONS_COLUMNS col\n" +
                    "WHERE c.CONSTRAINT_TYPE<>'R' AND c.OWNER=? AND c.OWNER=col.OWNER AND c.CONSTRAINT_NAME=col.CONSTRAINT_NAME");
            if (forTable != null) {
                sql.append(" AND c.TABLE_NAME=?");
            }
            sql.append("\nORDER BY c.CONSTRAINT_NAME,col.POSITION");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, XuguSchema.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected XuguTableConstraint fetchObject(JDBCSession session, XuguSchema owner, XuguTableBase parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguTableConstraint(parent, dbResult);
        }

        @Nullable
        @Override
        protected XuguTableConstraintColumn[] fetchObjectRow(
            JDBCSession session,
            XuguTableBase parent, XuguTableConstraint object, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            final XuguTableColumn tableColumn = getTableColumn(session, parent, dbResult);
            return tableColumn == null ? null : new XuguTableConstraintColumn[] { new XuguTableConstraintColumn(
                object,
                tableColumn,
                JDBCUtils.safeGetInt(dbResult, "POSITION")) };
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, XuguTableConstraint constraint, List<XuguTableConstraintColumn> rows)
        {
            constraint.setColumns(rows);
        }
    }

    class ForeignKeyCache extends JDBCCompositeCache<XuguSchema, XuguTable, XuguTableForeignKey, XuguTableForeignKeyColumn> {
        ForeignKeyCache()
        {
            super(tableCache, XuguTable.class, "TABLE_NAME", "CONSTRAINT_NAME");
        }

        @Override
        protected void loadObjects(DBRProgressMonitor monitor, XuguSchema schema, XuguTable forParent)
            throws DBException
        {
            // Cache schema constraints if not table specified
            if (forParent == null) {
                constraintCache.getAllObjects(monitor, schema);
            }
            super.loadObjects(monitor, schema, forParent);
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, XuguSchema owner, XuguTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder(500);
            sql.append("SELECT ").append(XuguUtils.getSysCatalogHint(owner.getDataSource())).append(" \r\n" +
                "c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.STATUS,c.R_OWNER,c.R_CONSTRAINT_NAME,rc.TABLE_NAME as R_TABLE_NAME,c.DELETE_RULE, \n" +
                "col.COLUMN_NAME,col.POSITION\r\n" +
                "FROM SYS.ALL_CONSTRAINTS c, SYS.ALL_CONS_COLUMNS col, SYS.ALL_CONSTRAINTS rc\n" +
                "WHERE c.CONSTRAINT_TYPE='R' AND c.OWNER=?\n" +
                "AND c.OWNER=col.OWNER AND c.CONSTRAINT_NAME=col.CONSTRAINT_NAME\n" +
                "AND rc.OWNER=c.r_OWNER AND rc.CONSTRAINT_NAME=c.R_CONSTRAINT_NAME");
            if (forTable != null) {
                sql.append(" AND c.TABLE_NAME=?");
            }
            sql.append("\nORDER BY c.CONSTRAINT_NAME,col.POSITION");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, XuguSchema.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected XuguTableForeignKey fetchObject(JDBCSession session, XuguSchema owner, XuguTable parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguTableForeignKey(session.getProgressMonitor(), parent, dbResult);
        }

        @Nullable
        @Override
        protected XuguTableForeignKeyColumn[] fetchObjectRow(
            JDBCSession session,
            XuguTable parent, XuguTableForeignKey object, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            XuguTableColumn column = getTableColumn(session, parent, dbResult);
            return column == null ? null : new XuguTableForeignKeyColumn[] { new XuguTableForeignKeyColumn(
                object,
                column,
                JDBCUtils.safeGetInt(dbResult, "POSITION")) };
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void cacheChildren(DBRProgressMonitor monitor, XuguTableForeignKey foreignKey, List<XuguTableForeignKeyColumn> rows)
        {
            foreignKey.setColumns((List)rows);
        }
    }


    /**
     * Index cache implementation
     */
    class IndexCache extends JDBCCompositeCache<XuguSchema, XuguTablePhysical, XuguTableIndex, XuguTableIndexColumn> {
        IndexCache()
        {
            super(tableCache, XuguTablePhysical.class, "TABLE_NAME", "INDEX_NAME");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, XuguSchema owner, XuguTablePhysical forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ").append(XuguUtils.getSysCatalogHint(owner.getDataSource())).append(" " +
                    "i.OWNER,i.INDEX_NAME,i.INDEX_TYPE,i.TABLE_OWNER,i.TABLE_NAME,i.UNIQUENESS,i.TABLESPACE_NAME,i.STATUS,i.NUM_ROWS,i.SAMPLE_SIZE,\n" +
                    "ic.COLUMN_NAME,ic.COLUMN_POSITION,ic.COLUMN_LENGTH,ic.DESCEND,iex.COLUMN_EXPRESSION\n" +
                    "FROM SYS.ALL_INDEXES i\n" +
                    "JOIN SYS.ALL_IND_COLUMNS ic ON ic.INDEX_OWNER=i.OWNER AND ic.INDEX_NAME=i.INDEX_NAME \n" +
                    "LEFT OUTER JOIN SYS.ALL_IND_EXPRESSIONS iex ON iex.INDEX_OWNER=i.OWNER AND iex.INDEX_NAME=i.INDEX_NAME AND iex.COLUMN_POSITION=ic.COLUMN_POSITION\n" +
                    "WHERE ");
            if (forTable == null) {
                sql.append("i.OWNER=?");
            } else {
                sql.append("i.TABLE_OWNER=? AND i.TABLE_NAME=?");
            }
            sql.append("\nORDER BY i.INDEX_NAME,ic.COLUMN_POSITION");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (forTable == null) {
                dbStat.setString(1, XuguSchema.this.getName());
            } else {
                dbStat.setString(1, XuguSchema.this.getName());
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected XuguTableIndex fetchObject(JDBCSession session, XuguSchema owner, XuguTablePhysical parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguTableIndex(owner, parent, indexName, dbResult);
        }

        @Nullable
        @Override
        protected XuguTableIndexColumn[] fetchObjectRow(
            JDBCSession session,
            XuguTablePhysical parent, XuguTableIndex object, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, "COLUMN_POSITION");
            boolean isAscending = "ASC".equals(JDBCUtils.safeGetStringTrimmed(dbResult, "DESCEND"));
            String columnExpression = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_EXPRESSION");

            XuguTableColumn tableColumn = columnName == null ? null : parent.getAttribute(session.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                return null;
            }

            return new XuguTableIndexColumn[] { new XuguTableIndexColumn(
                object,
                tableColumn,
                ordinalPosition,
                isAscending,
                columnExpression) };
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, XuguTableIndex index, List<XuguTableIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     * DataType cache implementation
     */
    static class DataTypeCache extends JDBCObjectCache<XuguSchema, XuguDataType> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner) throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + XuguUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM SYS.ALL_TYPES WHERE OWNER=? ORDER BY TYPE_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected XuguDataType fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet resultSet) throws SQLException
        {
            return new XuguDataType(owner, resultSet);
        }
    }

    /**
     * Sequence cache implementation
     */
    static class SequenceCache extends JDBCObjectCache<XuguSchema, XuguSequence> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + XuguUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM SYS.ALL_SEQUENCES WHERE SEQUENCE_OWNER=? ORDER BY SEQUENCE_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected XuguSequence fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguSequence(owner, resultSet);
        }
    }

    /**
     * Queue cache implementation
     */
    static class QueueCache extends JDBCObjectCache<XuguSchema, XuguQueue> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + XuguUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM SYS.ALL_QUEUES WHERE OWNER=? ORDER BY NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected XuguQueue fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguQueue(owner, resultSet);
        }
    }

    /**
     * Procedures cache implementation
     */
    static class ProceduresCache extends JDBCObjectLookupCache<XuguSchema, XuguProcedureStandalone> {

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner, @Nullable XuguProcedureStandalone object, @Nullable String objectName) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + XuguUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM " +
                    XuguUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "OBJECTS") + " " +
                    "WHERE OBJECT_TYPE IN ('PROCEDURE','FUNCTION') " +
                    "AND OWNER=? " +
                    (object == null && objectName == null ? "" : "AND OBJECT_NAME=? ") +
                    "ORDER BY OBJECT_NAME");
            dbStat.setString(1, owner.getName());
            if (object != null || objectName != null) dbStat.setString(2, object != null ? object.getName() : objectName);
            return dbStat;
        }

        @Override
        protected XuguProcedureStandalone fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguProcedureStandalone(owner, dbResult);
        }

    }

    static class PackageCache extends JDBCObjectCache<XuguSchema, XuguPackage> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + XuguUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM " +
                XuguUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "OBJECTS") +
                " WHERE OBJECT_TYPE='PACKAGE' AND OWNER=? " +
                " ORDER BY OBJECT_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected XuguPackage fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguPackage(owner, dbResult);
        }

    }

    /**
     * Sequence cache implementation
     */
    static class SynonymCache extends JDBCObjectCache<XuguSchema, XuguSynonym> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner) throws SQLException
        {
            String synonymTypeFilter = (session.getDataSource().getContainer().getPreferenceStore().getBoolean(XuguConstants.PREF_DBMS_READ_ALL_SYNONYMS) ?
                "" :
                "AND O.OBJECT_TYPE NOT IN ('JAVA CLASS','PACKAGE BODY')\n");

            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT OWNER, SYNONYM_NAME, MAX(TABLE_OWNER) as TABLE_OWNER, MAX(TABLE_NAME) as TABLE_NAME, MAX(DB_LINK) as DB_LINK, MAX(OBJECT_TYPE) as OBJECT_TYPE FROM (\n" +
                    "SELECT S.*, NULL OBJECT_TYPE FROM " + XuguUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "SYNONYMS") + " S WHERE S.OWNER = ?\n" +
                    "UNION ALL\n" +
                    "SELECT S.*,O.OBJECT_TYPE FROM " + XuguUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "SYNONYMS") + " S, " + XuguUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "OBJECTS") + " O\n" +
                    "WHERE S.OWNER = ?\n" +
                    synonymTypeFilter +
                    "AND O.OWNER=S.TABLE_OWNER AND O.OBJECT_NAME=S.TABLE_NAME\n" +
                    ")\n" +
                    "GROUP BY OWNER, SYNONYM_NAME\n" +
                    "ORDER BY SYNONYM_NAME");
            dbStat.setString(1, owner.getName());
            dbStat.setString(2, owner.getName());
            return dbStat;
        }

        @Override
        protected XuguSynonym fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguSynonym(owner, resultSet);
        }
    }

    static class MViewCache extends JDBCObjectCache<XuguSchema, XuguMaterializedView> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM SYS.ALL_MVIEWS WHERE OWNER=? " +
                "ORDER BY MVIEW_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected XuguMaterializedView fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguMaterializedView(owner, dbResult);
        }

    }

    static class DBLinkCache extends JDBCObjectCache<XuguSchema, XuguDBLink> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM SYS.ALL_DB_LINKS WHERE OWNER=? " +
                " ORDER BY DB_LINK");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected XuguDBLink fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguDBLink(session.getProgressMonitor(), owner, dbResult);
        }

    }

    static class TriggerCache extends JDBCObjectCache<XuguSchema, XuguSchemaTrigger> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema schema) throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT *\n" +
                "FROM " + XuguUtils.getAdminAllViewPrefix(session.getProgressMonitor(), schema.getDataSource(), "TRIGGERS") + " WHERE OWNER=? AND TRIM(BASE_OBJECT_TYPE) IN ('DATABASE','SCHEMA')\n" +
                "ORDER BY TRIGGER_NAME");
            dbStat.setString(1, schema.getName());
            return dbStat;
        }

        @Override
        protected XuguSchemaTrigger fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema oracleSchema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguSchemaTrigger(oracleSchema, resultSet);
        }
    }

    static class JavaCache extends JDBCObjectCache<XuguSchema, XuguJavaClass> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM SYS.ALL_JAVA_CLASSES WHERE OWNER=? ");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected XuguJavaClass fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguJavaClass(owner, dbResult);
        }

    }

    static class SchedulerJobCache extends JDBCObjectCache<XuguSchema, XuguSchedulerJob> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner)
                throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM SYS.ALL_SCHEDULER_JOBS WHERE OWNER=? ORDER BY JOB_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected XuguSchedulerJob fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet dbResult)
                throws SQLException, DBException
        {
            return new XuguSchedulerJob(owner, dbResult);
        }

    }

    static class SchedulerProgramCache extends JDBCObjectCache<XuguSchema, XuguSchedulerProgram> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner)
                throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM SYS.ALL_SCHEDULER_PROGRAMS WHERE OWNER=? ORDER BY PROGRAM_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected XuguSchedulerProgram fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet dbResult)
                throws SQLException, DBException
        {
            return new XuguSchedulerProgram(owner, dbResult);
        }

    }

    static class RecycleBin extends JDBCObjectCache<XuguSchema, XuguRecycledObject> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner)
            throws SQLException
        {
            final boolean isPublic = owner.isPublic();
            JDBCPreparedStatement dbStat = session.prepareStatement(
                isPublic ?
                    "SELECT * FROM SYS.USER_RECYCLEBIN" :
                    "SELECT * FROM SYS.DBA_RECYCLEBIN WHERE OWNER=?");
            if (!isPublic) {
                dbStat.setString(1, owner.getName());
            }
            return dbStat;
        }

        @Override
        protected XuguRecycledObject fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguRecycledObject(owner, dbResult);
        }

    }

}
