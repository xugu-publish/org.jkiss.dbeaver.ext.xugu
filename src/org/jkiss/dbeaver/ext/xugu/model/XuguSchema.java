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
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * XuguSchema
 * @author luke
 */
public class XuguSchema extends XuguGlobalObject implements DBSSchema, DBPRefreshableObject, DBPSystemObject, DBSProcedureContainer
{
    private static final Log log = Log.getLog(XuguSchema.class);

    final public TableCache tableCache = new TableCache();
    final public ViewCache viewCache = new ViewCache();
    final public ConstraintCache constraintCache = new ConstraintCache();
    final public ForeignKeyCache foreignKeyCache = new ForeignKeyCache();
    final public IndexCache indexCache = new IndexCache();
    final public SequenceCache sequenceCache = new SequenceCache();
    final public PackageCache packageCache = new PackageCache();
    final public SynonymCache synonymCache = new SynonymCache();
    final public UDTCache udtCache = new UDTCache();
    final public ProceduresCache proceduresCache = new ProceduresCache();
    final public SchedulerJobCache schedulerJobCache = new SchedulerJobCache();

    private long id;
    private String name;
    private String roleFlag;
    private XuguDatabase parent;
    private transient XuguUser user;

    /**
     * 通过指定模式ID和模式名称构造一个新的模式对象
     * @param dataSource 数据源
     * @param id 模式ID
     * @param name 模式名称
     */
    public XuguSchema(XuguDataSource dataSource, long id, String name)
    {
        super(dataSource, id > 0);
        System.out.println("new Schema!1 "+name);
        this.id = id;
        this.name = name;
        this.roleFlag = dataSource.getRoleFlag();
    }

    /**
     * 通过结果集构造一个新的模式对象，同时指定其所属数据库
     * @param dataSource 数据源
     * @param parent 所属数据库
     * @param dbResult 查询结果集
     */
    public XuguSchema(@NotNull XuguDataSource dataSource, XuguDatabase parent, ResultSet dbResult) {
    	super(dataSource, true);
    	System.out.println("new Schema!2 "+JDBCUtils.safeGetString(dbResult, "SCHEMA_NAME"));
    	this.id = JDBCUtils.safeGetLong(dbResult, "SCHEMA_ID");
        this.name = JDBCUtils.safeGetString(dbResult, "SCHEMA_NAME");
        this.roleFlag = dataSource.getRoleFlag();
        this.parent = parent;
        if (CommonUtils.isEmpty(this.name)) {
            log.warn("Empty schema name fetched");
            this.name = "? " + super.hashCode();
        }
    }
    
    /**
     *  通过结果集构造一个新的模式对象
     * @param dataSource 
     * @param dbResult
     */
    public XuguSchema(@NotNull XuguDataSource dataSource, @NotNull ResultSet dbResult)
    {
        super(dataSource, true);
        System.out.println("new Schema!3 "+JDBCUtils.safeGetString(dbResult, "SCHEMA_NAME"));
        this.id = JDBCUtils.safeGetLong(dbResult, "SCHEMA_ID");
        this.name = JDBCUtils.safeGetString(dbResult, "SCHEMA_NAME");
        this.roleFlag = dataSource.getRoleFlag();
        if (CommonUtils.isEmpty(this.name)) {
            log.warn("Empty schema name fetched");
            this.name = "? " + super.hashCode();
        }
    }
    

    public boolean isPublic()
    {
        return XuguConstants.USER_PUBLIC.equals(this.name);
    }

    @Property(order = 200)
    public long getID()
    {
        return id;
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

    public int getDBID(XuguSchema schema, JDBCSession session) {
    	try {
	    	String dbName = schema.getDataSource().connection.getCatalog();
	        int dbID = schema.getDataSource().databaseCache.getObject(session.getProgressMonitor(), schema.getDataSource(), dbName).getID();
            return dbID;
        } catch (DBException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
    }
    
    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    public XuguUser getUser()
    {
        return user;
    }

    public void setUser(XuguUser user)
    {
        this.user = user;
    }
    
    public String getRoleFlag() {
    	return roleFlag;
    }
    
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        return udtCache.getCachedObjects();
    }

    /**
     * 从索引缓存中获取模式包含的所有索引信息（提供给界面展示）
     * @param monitor 监控
     * @return list 索引列表
     * @throws DBException
     */
    @Association
    public Collection<XuguTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
    	Collection<XuguTableIndex> list = indexCache.getObjects(monitor, this, null);
        return list;
    }

    /**
     * 从表缓存中获取模式包含的所有表信息（提供给界面展示）
     * @param monitor 监控
     * @return list 表列表
     * @throws DBException
     */
    @Association
    public Collection<XuguTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
    	Collection<XuguTable> list = tableCache.getTypedObjects(monitor, this, XuguTable.class);
        return list;
    }

    /**
     * 根据表名从缓存中获取指定的表信息
     * @param monitor 监控
     * @param name 表名
     * @return table 表对象
     * @throws DBException
     */
    public XuguTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
    	XuguTable table = tableCache.getObject(monitor, this, name, XuguTable.class);
        return table;
    }

    /**
     * 从视图缓存中获取所有视图信息（提供给界面展示）
     * @param monitor 监控
     * @return list 视图列表
     * @throws DBException
     */
    @Association
    public Collection<XuguView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
    	Collection<XuguView> list = viewCache.getAllObjects(monitor, this);
        return list;
    }

    /**
     * 根据视图名从缓存中获取指定的视图信息
     * @param monitor 监控
     * @param name 视图名
     * @return view 视图对象
     * @throws DBException
     */
    public XuguView getView(DBRProgressMonitor monitor, String name)
        throws DBException
    {
    	XuguView view = viewCache.getObject(monitor, this, name, XuguView.class);
    	return view;
    }

    /**
     * 从序列缓存中获取全部的缓存信息（提供给界面展示）
     * @param monitor 监控
     * @return list 序列列表
     * @throws DBException
     */
    @Association
    public Collection<XuguSequence> getSequences(DBRProgressMonitor monitor)
        throws DBException
    {
    	Collection<XuguSequence> list = sequenceCache.getAllObjects(monitor, this);
        return list;
    }

    /**
     * 从包缓存中获取全部的包信息（提供给界面展示）
     * @param monitor 监控
     * @return list 包列表
     * @throws DBException
     */
    @Association
    public Collection<XuguPackage> getPackages(DBRProgressMonitor monitor)
        throws DBException
    {
        Collection<XuguPackage> list = packageCache.getAllObjects(monitor, this);
        return list;
    }

    /**
     * 从存储过程缓存中获取全部的存储过程信息（提供给界面展示）
     * @param monitor 监控
     * @return list 存储过程列表
     * @throws DBException
     */
    @Override
    @Association
    public Collection<XuguProcedureStandalone> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        Collection<XuguProcedureStandalone> list = proceduresCache.getAllObjects(monitor, this);
        return list;
    }

    /**
     * 根据存储过程名从缓存中获取指定的存储过程信息
     * @param monitor 监控
     * @param uniqueName 存储过程名
     * @return procedure 存储过程对象
     * @throws DBException
     */
    @Override
    public XuguProcedureStandalone getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        XuguProcedureStandalone procedure = proceduresCache.getObject(monitor, this, uniqueName);
        return procedure;
    }

    /**
     * 从同义词缓存中获取全部的同义词信息
     * @param monitor 监控
     * @return list 同义词列表
     * @throws DBException
     */
    @Association
    public Collection<XuguSynonym> getSynonyms(DBRProgressMonitor monitor)
        throws DBException
    {
        Collection<XuguSynonym> list = synonymCache.getAllObjects(monitor, this);
        return list;
    }

    /**
     * 根据指定的同义词名从缓存中获取指定的同义词信息
     * @param monitor 监控
     * @param name 同义词名
     * @return synonym 同义词对象
     * @throws DBException
     */
    public XuguSynonym getSynonym(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        XuguSynonym synonym = synonymCache.getObject(monitor, this, name, XuguSynonym.class);
        return synonym;
    }

    /**
     * 从用户自定义数据类型缓存中获取全部的自定义数据类型信息
     * @param monitor 监控
     * @return list 数据类型列表
     * @throws DBException
     */
    @Association
    public Collection<XuguUDT> getUDTs(DBRProgressMonitor monitor)
        throws DBException
    {
        Collection<XuguUDT> list = udtCache.getAllObjects(monitor, this);
        return list;
    }

    /**
     * 根据指定的用户自定义数据类型名从缓存中获取指定的数据类型信息
     * @param monitor 监控
     * @param name 数据类型名
     * @return udt 数据类型对象
     * @throws DBException
     */
    public XuguUDT getUDT(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        XuguUDT udt = udtCache.getObject(monitor, this, name, XuguUDT.class);
        return udt;
    }
    
    /**
     * 从触发器缓存中获取全部的触发器信息
     * @param monitor 监控
     * @return list 触发器列表
     * @throws DBException
     */
//    @Association
//    public Collection<XuguSchemaTrigger> getTriggers(DBRProgressMonitor monitor)
//        throws DBException
//    {
//        return triggerCache.getAllObjects(monitor, this);
//    }

    
//    @Association
//    public Collection<XuguTableTrigger> getTableTriggers(DBRProgressMonitor monitor)
//            throws DBException
//    {
//        List<XuguTableTrigger> allTableTriggers = new ArrayList<>();
//        for (XuguTableBase table : tableCache.getAllObjects(monitor, this)) {
//            Collection<XuguTableTrigger> triggers = table.getTriggers(monitor);
//            if (!CommonUtils.isEmpty(triggers)) {
//                allTableTriggers.addAll(triggers);
//            }
//        }
//        allTableTriggers.sort(Comparator.comparing(XuguTrigger::getName));
//        return allTableTriggers;
//    }

    /**
     * 从数据库连接缓存中获取全部的数据库连接信息
     * @param monitor 监控
     * @return list 数据库连接列表
     * @throws DBException
     */
//    @Association
//    public Collection<XuguDBLink> getDatabaseLinks(DBRProgressMonitor monitor)
//        throws DBException
//    {
//    	Collection<XuguDBLink> list = dbLinkCache.getAllObjects(monitor, this);
//    	return list;
//    }
    
    /**
     * 从作业缓存中获取全部的作业信息
     * @param monitor 监控
     * @return list 作业列表
     * @throws DBException
     */
    @Association
    public Collection<XuguSchedulerJob> getSchedulerJobs(DBRProgressMonitor monitor)
            throws DBException
    {
        Collection<XuguSchedulerJob> list = schedulerJobCache.getAllObjects(monitor, this);
        return list;
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
        children.addAll(viewCache.getAllObjects(monitor, this));
        children.addAll(constraintCache.getAllObjects(monitor, this));
        children.addAll(foreignKeyCache.getAllObjects(monitor, this));
//        children.addAll(triggerCache.getAllObjects(monitor, this));
        children.addAll(indexCache.getAllObjects(monitor, this));
//        children.addAll(dataTypeCache.getAllObjects(monitor, this));
        children.addAll(sequenceCache.getAllObjects(monitor, this));
        children.addAll(packageCache.getAllObjects(monitor, this));
        children.addAll(synonymCache.getAllObjects(monitor, this));
        children.addAll(udtCache.getAllObjects(monitor, this));
//        children.addAll(dbLinkCache.getAllObjects(monitor, this));
        children.addAll(schedulerJobCache.getAllObjects(monitor, this));
        return children;
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        XuguTableBase table = tableCache.getObject(monitor, this, childName);
        if (table != null) {
            System.out.println("TTTTTTTTt "+table.getName()+" ");
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
        monitor.subTask("Cache views");
        viewCache.getAllObjects(monitor, this);
        monitor.subTask("Cache udts");
        udtCache.getAllObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            tableCache.loadChildren(monitor, this, null);
            monitor.subTask("Cache view columns");
            viewCache.loadChildren(monitor, this, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache table indexes");
            indexCache.getObjects(monitor, this, null);
            monitor.subTask("Cache table constraints");
            constraintCache.getObjects(monitor, this, null);
            foreignKeyCache.getObjects(monitor, this, null);
            monitor.subTask("Cache indexes");
            indexCache.getAllObjects(monitor, this);
            monitor.subTask("Cache sequences");
            sequenceCache.getAllObjects(monitor, this);
            monitor.subTask("Cache packages");
            packageCache.getAllObjects(monitor, this);
            monitor.subTask("Cache synonyms");
            synonymCache.getAllObjects(monitor, this);
            monitor.subTask("Cache job");
            schedulerJobCache.getAllObjects(monitor, this);
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
//        triggerCache.clearCache();
//        dataTypeCache.clearCache();
        sequenceCache.clearCache();
        synonymCache.clearCache();
        udtCache.clearCache();
        schedulerJobCache.clearCache();
        return this.getDataSource().schemaCache.refreshObject(monitor, this.getDataSource(), this);
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
        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COL_NAME");
        
        return getTableColumn(session, parent, columnName);
    }
    
    private static XuguTableColumn getTableColumn(JDBCSession session, XuguTableBase parent, String columnName) throws DBException
    {
    	//将keys字段中的引号去掉（是否可支持多列？）
    	if(columnName==null) {
    		return null;
    	}
        columnName = columnName.replaceAll("\"", "");
        //que 获取到的列为空？
        XuguTableColumn tableColumn = columnName == null ? null : parent.getAttribute(session.getProgressMonitor(), columnName);
        if (tableColumn == null) {
            log.debug("GetTableColumn Column '" + columnName + "' not found in table '" + parent.getName() + "'");
        }
        return tableColumn;
    }

    /**
     *	表缓存
     */
    public static class TableCache extends JDBCStructLookupCache<XuguSchema, XuguTableBase, XuguTableColumn> {

        TableCache()
        {
            super("TABLE_NAME");
            setListOrderComparator(DBUtils.nameComparator());
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner, @Nullable XuguTableBase object, @Nullable String objectName) throws SQLException {
        	//xfc 根据schema name 查询所有表信息
        	StringBuilder sql = new StringBuilder();
        	sql.append("SELECT * FROM ");
			sql.append(owner.roleFlag);
			sql.append("_TABLES WHERE SCHEMA_ID=");
			sql.append(owner.id);
			sql.append(" AND DB_ID=");
			sql.append(owner.getDBID(owner, session));
        	//当有检索条件时 只查询指定表 用于新建表之后的刷新工作
        	if(object!=null) {
        		sql.append(" AND TABLE_ID = ");
        		sql.append(object.getID());
        	}
        	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        		log.info("Xugu Plugin: Construct select tables sql: "+sql.toString());
        	}
        	JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
        	System.out.println("find tables stmt "+dbStat.getQueryString());
            return dbStat;
        }

        @Override
        protected XuguTableBase fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
        	//xfc 修改object_type字段为table_type 并修改为int类型
            int tableType = JDBCUtils.safeGetInt(dbResult, "TABLE_TYPE");
            if (tableType==0) {
                return new XuguTable(session.getProgressMonitor(), owner, dbResult);
            } else {
                return new XuguView(session.getProgressMonitor(), session, owner, dbResult);
            }
        }

        // 获取列信息
        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner, @Nullable XuguTableBase forTable)
            throws SQLException
        {
        	//xfc 修改了获取列信息的sql
            StringBuilder sql = new StringBuilder(500);
            sql.append("SELECT * FROM ");
        	sql.append(owner.roleFlag);
        	sql.append("_COLUMNS");
        	sql.append(" WHERE DB_ID=");
        	sql.append(owner.getDBID(owner, session));
            if (forTable != null) {
                sql.append(" AND TABLE_ID=");
                sql.append(forTable.getID());
                sql.append(" AND DB_ID=");
                sql.append(owner.getDBID(owner, session));
            }
            if(XuguConstants.LOG_PRINT_LEVEL<1) {
            	log.info("Xugu Plugin: Construct select table columns sql: "+sql.toString());
            }
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            return dbStat;
        }

        @Override
        protected XuguTableColumn fetchChild(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull XuguTableBase table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguTableColumn(session.getProgressMonitor(), table, dbResult);
        }

        @Override
        protected void cacheChildren(XuguTableBase parent, List<XuguTableColumn> xuguTableColumns) {
        	xuguTableColumns.sort(DBUtils.orderComparator());
            super.cacheChildren(parent, xuguTableColumns);
        }
    }

    /**
     *	约束缓存
     */
    class ConstraintCache extends JDBCCompositeCache<XuguSchema, XuguTableBase, XuguTableConstraint, XuguTableConstraintColumn> {
        ConstraintCache()
        {
        	//xfc 修改约束名字段为 CONS_NAME
            super(tableCache, XuguTableBase.class, "TABLE_NAME", "CONS_NAME");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, XuguSchema owner, XuguTableBase forTable)
            throws SQLException
        {
        	//xfc 修改了获取约束信息的sql
            StringBuilder sql = new StringBuilder(500);
            sql.append("SELECT DISTINCT *, DEFINE AS COL_NAME FROM ");
        	sql.append(owner.roleFlag);
        	sql.append("_CONSTRAINTS INNER JOIN (SELECT s.SCHEMA_NAME, t.TABLE_ID FROM ");
            sql.append(owner.roleFlag);
            sql.append("_SCHEMAS s INNER JOIN ");
            sql.append(owner.roleFlag);
            sql.append("_TABLES t USING(SCHEMA_ID) ");
            if (forTable != null) {
                sql.append("WHERE TABLE_ID=");
                sql.append(forTable.getID());
            }
            sql.append(") USING(TABLE_ID)");
            sql.append(" WHERE DB_ID=");
            sql.append(owner.getDBID(owner, session));
            if(XuguConstants.LOG_PRINT_LEVEL<1) {
            	log.info("Xugu Plugin: Construct select constraints sql: "+sql.toString());
            }
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
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
            //处理多列的情况
            String colName = JDBCUtils.safeGetStringTrimmed(dbResult, "DEFINE");
            if(colName!=null && !colName.equals("")) {
            	if(colName.indexOf(",")!=-1) {
                	if(colName.indexOf("(")!=-1) {
                		colName = colName.substring(colName.indexOf("(")+1, colName.indexOf(")"));
                	}
                	System.out.println("CCCCCCCCoLLLLLL "+colName);
                	String[] colNames = colName.split(",");
                	XuguTableConstraintColumn[] con_cols = new XuguTableConstraintColumn[colNames.length];
                	for(int i=0; i<colNames.length; i++) {
                		System.out.println("SSingle col "+colNames[i]);
                		XuguTableColumn tableColumn = getTableColumn(session, parent, colNames[i]);
                		con_cols[i] = new XuguTableConstraintColumn(
                                object,
                                tableColumn,
                                tableColumn.getOrdinalPosition());
                	}
                	return con_cols;
                }
                //处理单列但是带括号情况
                else if(colName.indexOf("(")!=-1) {
                	String realColName = colName.substring(colName.indexOf("(")+1, colName.indexOf(")"));
                	XuguTableColumn tableColumn = getTableColumn(session, parent, realColName);
                	return tableColumn == null ? null : new XuguTableConstraintColumn[] { new XuguTableConstraintColumn(
                            object,
                            tableColumn,
                            tableColumn.getOrdinalPosition()) };
                }
                //正常的单列情况
                else {
                	colName = colName.replaceAll("\"", "");
                	XuguTableColumn tableColumn = getTableColumn(session, parent, colName);
                	return tableColumn == null ? null : new XuguTableConstraintColumn[] { new XuguTableConstraintColumn(
                            object,
                            tableColumn,
                            tableColumn.getOrdinalPosition()) };
                }
            }
            XuguTableColumn tableColumn = getTableColumn(session, parent, dbResult);
        	//xfc COL_NO无法从结果集直接获取 选择从column中调用get方法
            return tableColumn == null ? null : new XuguTableConstraintColumn[] { new XuguTableConstraintColumn(
                object,
                tableColumn,
                tableColumn.getOrdinalPosition()) };
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, XuguTableConstraint constraint, List<XuguTableConstraintColumn> rows)
        {
            constraint.setColumns(rows);
        }
    }

    /**
     *	外键缓存
     */
    class ForeignKeyCache extends JDBCCompositeCache<XuguSchema, XuguTable, XuguTableForeignKey, XuguTableForeignKeyColumn> {
        ForeignKeyCache()
        {
        	//xfc 修改约束名字段为 CONS_NAME
            super(tableCache, XuguTable.class, "TABLE_NAME", "CONS_NAME");
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
        	//xfc 修改了获取外键信息的sql
            StringBuilder sql = new StringBuilder(500);
            sql.append("SELECT DISTINCT *, f.DEFINE AS COL_NAME,"
            		+ " t2.TABLE_NAME AS REF_TABLE_NAME,"
            		+ " f2.CONS_NAME AS REF_NAME FROM ");
        	sql.append(owner.roleFlag);
        	sql.append("_CONSTRAINTS f INNER JOIN (SELECT s.SCHEMA_NAME, t.TABLE_ID FROM ");
            sql.append(owner.roleFlag);
            sql.append("_SCHEMAS s INNER JOIN ");
            sql.append(owner.roleFlag);
            sql.append("_TABLES t USING(SCHEMA_ID) ");
            if (forTable != null) {
                sql.append("WHERE TABLE_ID=");
                sql.append(forTable.getID());
            }
            sql.append(") USING(TABLE_ID) INNER JOIN (SELECT t2.TABLE_NAME, t2.TABLE_ID FROM ");
            sql.append(owner.roleFlag);
            sql.append("_TABLES t2) ON f.REF_TABLE_ID=t2.TABLE_ID JOIN ");
            sql.append(owner.roleFlag);
            sql.append("_CONSTRAINTS f2 ON f.REF_TABLE_ID=f2.TABLE_ID ");
            sql.append("WHERE CONS_TYPE='F' AND DB_ID=");
            sql.append(owner.getDBID(owner, session));
            if(XuguConstants.LOG_PRINT_LEVEL<1) {
            	log.info("Xugu Plugin: Construct select foreign keys sql: "+sql.toString());
            }
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());

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
            if(dbResult!=null) {
            	String colName = JDBCUtils.safeGetStringTrimmed(dbResult, "DEFINE");
            	//处理多列的情况
            	if(colName.indexOf("(")!=-1) {
                	colName = colName.substring(colName.indexOf("(")+1, colName.indexOf(")"));
                	System.out.println("CCCCCCCCoLLLLLL2 "+colName);
                	String[] colNames = colName.split(",");
                	XuguTableForeignKeyColumn[] con_cols = new XuguTableForeignKeyColumn[colNames.length];
                	for(int i=0; i<colNames.length; i++) {
                		System.out.println("SSingle col "+colNames[i]);
                		XuguTableColumn tableColumn = getTableColumn(session, parent, colNames[i]);
                		con_cols[i] = new XuguTableForeignKeyColumn(
                                object,
                                tableColumn,
                                tableColumn.getOrdinalPosition());
                	}
                	return con_cols;
                }
            }
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
     *	索引缓存
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
        	//xfc 修改了获取索引信息的sql
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT DISTINCT *, KEYS AS COL_NAME FROM ");
            sql.append(owner.roleFlag);
            sql.append("_INDEXES");
            sql.append(" WHERE DB_ID=");
            sql.append(owner.getDBID(owner, session));
            if (forTable != null) {
                sql.append(" AND TABLE_ID=");
                sql.append(forTable.getID());
            }
            if(XuguConstants.LOG_PRINT_LEVEL<1) {
        		log.info("Xugu Plugin: Construct select indexes sql: "+sql.toString());
        	}
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
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
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COL_NAME");
            columnName = columnName.replaceAll("\"", "");
            //处理多字段情况
            if(columnName.indexOf(",")!=-1) {
            	String[] cols = columnName.split(",");
            	XuguTableIndexColumn[] res = new XuguTableIndexColumn[cols.length];
            	for(int i=0; i<cols.length; i++) {
            		XuguTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), cols[i]);
                    if (tableColumn == null) {
                        log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                        return null;
                    }
                    XuguTableIndexColumn tempIndexCol = new XuguTableIndexColumn(object, tableColumn);
                    res[i] = tempIndexCol;
            	}
            	return res;
            }else {
            	XuguTableColumn tableColumn = columnName == null ? null : parent.getAttribute(session.getProgressMonitor(), columnName);
                if (tableColumn == null) {
                    log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                    return null;
                }
                return new XuguTableIndexColumn[] { new XuguTableIndexColumn(object, tableColumn) };
            }
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, XuguTableIndex index, List<XuguTableIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     *	数据类型缓存 
     */
    static class DataTypeCache extends JDBCObjectCache<XuguSchema, XuguDataType> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner) throws SQLException
        {
        	//do nothing
            return null;
        }

        @Override
        protected XuguDataType fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet resultSet) throws SQLException
        {
            //return new XuguDataType(owner, resultSet);
        	return null;
        }
    }

    /**
     *	序列缓存
     */
    static class SequenceCache extends JDBCObjectCache<XuguSchema, XuguSequence> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner) throws SQLException
        {
        	//xfc 修改了获取sequence信息的sql
        	StringBuilder sql = new StringBuilder();
        	sql.append("SELECT * FROM ");
        	sql.append(owner.roleFlag);
        	sql.append("_SEQUENCES");
        	sql.append(" WHERE DB_ID=");
        	sql.append(owner.getDBID(owner, session));
        	sql.append(" ORDER BY SEQ_NAME");
        	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        		log.info("Xugu Plugin: Construct select sequences sql: "+sql.toString());
        	}
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            return dbStat;
        }

        @Override
        protected XuguSequence fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguSequence(owner, resultSet);
        }
    }

    /**
     *	存储过程缓存
     */
    static class ProceduresCache extends JDBCObjectLookupCache<XuguSchema, XuguProcedureStandalone> {

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner, @Nullable XuguProcedureStandalone object, @Nullable String objectName) throws SQLException {
            //xfc 修改了获取存储过程信息的sql语句
        	StringBuilder sql = new StringBuilder();
        	sql.append("SELECT * FROM ");
        	sql.append(owner.roleFlag);
        	sql.append("_PROCEDURES WHERE SCHEMA_ID=");
        	sql.append(owner.id);
        	sql.append(" AND DB_ID=");
        	sql.append(owner.getDBID(owner, session));
        	//当有检索条件时 只查询指定表 用于新建表之后的刷新工作
        	if(object!=null) {
        		sql.append(" AND PROC_ID = ");
        		sql.append(object.getObjectId());
        	}
        	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        		log.info("Xugu Plugin: Construct select procedures sql: "+sql.toString());
        	}
        	JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            return dbStat;
        }

        @Override
        protected XuguProcedureStandalone fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguProcedureStandalone(session.getProgressMonitor(), owner, dbResult);
        }
    }

    /**
     *	包缓存
     */
    static class PackageCache extends JDBCObjectCache<XuguSchema, XuguPackage> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner)
            throws SQLException
        {
        	//xfc 修改了获取所有包信息的sql语句
        	StringBuilder sql = new StringBuilder();
        	sql.append("SELECT * FROM ");
        	sql.append(owner.roleFlag);
        	sql.append("_PACKAGES WHERE SCHEMA_ID=");
        	sql.append(owner.id);
        	sql.append(" AND DB_ID=");
        	sql.append(owner.getDBID(owner, session));
        	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        		log.info("Xugu Plugin: Construct select packages sql: "+sql.toString());
        	}
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
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
     *	同义词缓存
     */
    static class SynonymCache extends JDBCObjectCache<XuguSchema, XuguSynonym> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner) throws SQLException
        {
        	//xfc 修改了获取同义词信息的语句
        	StringBuilder sql = new StringBuilder();
        	sql.append("SELECT * FROM ");
        	sql.append(owner.roleFlag);
        	sql.append("_SYNONYMS WHERE SCHEMA_ID=");
        	sql.append(owner.id);
        	//加上公有的同义词
        	sql.append(" OR SCHEMA_ID=0");
        	sql.append(" AND DB_ID=");
        	sql.append(owner.getDBID(owner, session));
        	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        		log.info("Xugu Plugin: Construct select synonyms sql: "+sql.toString());
        	}
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            return dbStat;
        }

        @Override
        protected XuguSynonym fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguSynonym(owner, resultSet);
        }
    }
    
    /**
     *	用户自定义数据类型缓存
     */
    static class UDTCache extends JDBCObjectCache<XuguSchema, XuguUDT> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner) throws SQLException
        {
        	//xfc 修改了获取同义词信息的语句
        	StringBuilder sql = new StringBuilder();
        	sql.append("SELECT * FROM ");
        	sql.append(owner.roleFlag);
        	sql.append("_TYPES WHERE SCHEMA_ID=");
        	sql.append(owner.id);
        	sql.append(" AND DB_ID=");
        	sql.append(owner.getDBID(owner, session));
        	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        		log.info("Xugu Plugin: Construct select UDTs sql: "+sql.toString());
        	}
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            return dbStat;
        }

        @Override
        protected XuguUDT fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguUDT(owner, resultSet);
        }
    }
    
    /**
     *	视图缓存
     */
    static class ViewCache extends JDBCStructLookupCache<XuguSchema, XuguView, XuguTableColumn> {
    	
    	ViewCache()
        {
            super("VIEW_NAME");
            setListOrderComparator(DBUtils.nameComparator());
        }
    	
        @Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner, XuguView object,
				String objectName)
            throws SQLException
        {
        	System.out.println("select all views");
        	//xfc 修改了获取所有视图信息的sql
        	StringBuilder sql = new StringBuilder();
        	sql.append("SELECT * FROM ");
        	sql.append(owner.roleFlag);
        	sql.append("_VIEWS WHERE SCHEMA_ID=");
        	sql.append(owner.getID());
        	sql.append(" AND DB_ID=");
        	sql.append(owner.getDBID(owner, session));
        	if(object!=null) {
        		sql.append(" AND VIEW_ID=");
        		sql.append(object.getId());
        	}
        	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        		log.info("Xugu Plugin: Construct select views sql: "+sql.toString());
        	}
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            return dbStat;
        }

        @Override
        protected XuguView fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguView(session.getProgressMonitor(), session, owner, dbResult);
        }
        
     // 获取列信息
        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner, @Nullable XuguView forView)
            throws SQLException
        {
        	//xfc 修改了获取列信息的sql
            StringBuilder sql = new StringBuilder(500);
            sql.append("SELECT * FROM ");
        	sql.append(owner.roleFlag);
        	sql.append("_VIEW_COLUMNS WHERE DB_ID=");
        	sql.append(owner.getDBID(owner, session));
            if (forView != null) {
                sql.append(" AND VIEW_ID=");
                sql.append(forView.getId());
            }
            if(XuguConstants.LOG_PRINT_LEVEL<1) {
        		log.info("Xugu Plugin: Construct select view columns sql: "+sql.toString());
        	}
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            return dbStat;
        }

        @Override
        protected XuguTableColumn fetchChild(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull XuguView view, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguTableColumn(session.getProgressMonitor(), view, dbResult);
        }

        @Override
        protected void cacheChildren(XuguView parent, List<XuguTableColumn> xuguTableColumns) {
        	xuguTableColumns.sort(DBUtils.orderComparator());
            super.cacheChildren(parent, xuguTableColumns);
        }
    }

    /**
     *	作业缓存
     */
    static class SchedulerJobCache extends JDBCObjectCache<XuguSchema, XuguSchedulerJob> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguSchema owner)
                throws SQLException
        {
        	//xfc 修改了获取所有job信息的sql语句
        	StringBuilder sql = new StringBuilder();
        	sql.append("SELECT * FROM ");
        	sql.append(owner.roleFlag);
        	sql.append("_JOBS WHERE DB_ID=");
        	sql.append(owner.getDBID(owner, session));
        	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        		log.info("Xugu Plugin: Construct select jobs sql: "+sql.toString());
        	}
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            return dbStat;
        }

        @Override
        protected XuguSchedulerJob fetchObject(@NotNull JDBCSession session, @NotNull XuguSchema owner, @NotNull JDBCResultSet dbResult)
                throws SQLException, DBException
        {
            return new XuguSchedulerJob(session.getProgressMonitor(), session, owner, dbResult);
        }
    }

}
