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
import org.jkiss.dbeaver.ext.xugu.model.source.XuguStatefulObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * XuguTable base
 * 查询并加载表元信息
 */
public abstract class XuguTableBase extends JDBCTable<XuguDataSource, XuguSchema>
    implements DBPNamedObject2, DBPRefreshableObject, XuguStatefulObject
{
    private static final Log log = Log.getLog(XuguTableBase.class);

    private int id;
    private int tableType;
    
    public static class TableAdditionalInfo {
        volatile boolean loaded = false;

        boolean isLoaded() { return loaded; }
    }

    public static class CommentsValidator implements IPropertyCacheValidator<XuguTableBase> {
        @Override
        public boolean isPropertyCached(XuguTableBase object, Object propertyId)
        {
            return object.comment != null;
        }
    }

    public final TriggerCache triggerCache = new TriggerCache();
    protected abstract String getTableTypeName();

    protected boolean valid;
    private String comment;

    protected XuguTableBase(XuguSchema schema, String name, boolean persisted)
    {
        super(schema, name, persisted);
        System.out.println("new table! "+name);
    }

    protected XuguTableBase(XuguSchema xuguSchema, ResultSet dbResult, int type)
    {
        super(xuguSchema, true);
        // type=0 当前对象为表
        if(type==0) {
        	setName(JDBCUtils.safeGetString(dbResult, "TABLE_NAME"));
            this.id = JDBCUtils.safeGetInt(dbResult, "TABLE_ID");
        }
        // type=1 当前对象为视图
        else {
        	setName(JDBCUtils.safeGetString(dbResult, "VIEW_NAME"));
        	this.id = JDBCUtils.safeGetInt(dbResult, "VIEW_ID");
        }
        this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
        this.tableType = type;
    }

    public int getType() {
    	return tableType;
    }
    
    @Override
    public JDBCStructCache<XuguSchema, ? extends JDBCTable, ? extends JDBCTableColumn> getCache()
    {
        return getContainer().tableCache;
    }

    @Override
    @NotNull
    public XuguSchema getSchema()
    {
        return super.getContainer();
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return getComment();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }

    public int getID() {
    	return this.id;
    }
    
    //获取表注释信息
    @Property(viewable = true, editable = true, updatable = true, multiline = true, order = 100)
    @LazyProperty(cacheValidator = CommentsValidator.class)
    public String getComment(DBRProgressMonitor monitor)
        throws DBException
    {
        if (comment == null) {
        	System.out.println("CCCComents "+getTableTypeName());
        	int tableType = 0;
        	if("VIEW".equals(getTableTypeName())) {
        		tableType = 1;
        	}
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table comments")) {
                comment = JDBCUtils.queryString(
                    session,
                    "SELECT COMMENTS FROM "+this.getDataSource().getRoleFlag()+
                    "_TABLES WHERE TABLE_NAME=? AND TABLE_TYPE=? AND DB_ID=(SELECT DB_ID FROM "+
                    this.getDataSource().getRoleFlag()+"_DATABASES WHERE DB_NAME=?)",
                    getName(),
                    tableType,
                    this.getDataSource().getName());
                if (comment == null) {
                    comment = "";
                }
            } catch (SQLException e) {
                log.warn("Can't fetch table '" + getName() + "' comment", e);
            }
        }
        return comment;
    }

    //获取列名及注释信息
    void loadColumnComments(DBRProgressMonitor monitor) {
        try {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table column comments")) {
                try (JDBCPreparedStatement stat = session.prepareStatement("SELECT COL_NAME,COMMENTS FROM "+
            this.getDataSource().getRoleFlag()+"_COLUMNS cc WHERE cc.TABLE_ID=(SELECT TABLE_ID FROM "+
                		this.getDataSource().getRoleFlag()+"_TABLES WHERE TABLE_NAME=? AND DB_ID=(SELECT DB_ID FROM "+
            this.getDataSource().getRoleFlag()+"_DATABASES WHERE DB_NAME=?))")) {
                    stat.setString(1, getName());
                    stat.setString(2, this.getDataSource().getName());
                    try (JDBCResultSet resultSet = stat.executeQuery()) {
                        while (resultSet.next()) {
                            String colName = resultSet.getString(1);
                            String colComment = resultSet.getString(2);
                            XuguTableColumn col = getAttribute(monitor, colName);
                            if (col == null) {
                                log.warn("Column '" + colName + "' not found in table '" + getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                            } else {
                                col.setComment(CommonUtils.notEmpty(colComment));
                            }
                        }
                    }
                }
            }
            for (XuguTableColumn col : getAttributes(monitor)) {
                col.cacheComment();
            }
        } catch (Exception e) {
            log.warn("Error fetching table '" + getName() + "' column comments", e);
        }
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    @Override
    public Collection<XuguTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().tableCache.getChildren(monitor, getContainer(), this);
    }

    @Override
    public XuguTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().tableCache.getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().constraintCache.clearObjectCache(this);
        return getContainer().tableCache.refreshObject(monitor, getContainer(), this);
    }

    @Association
    public Collection<XuguTableTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getAllObjects(monitor, this);
    }

    @Override
    public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Nullable
    @Override
    @Association
    public Collection<XuguTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().constraintCache.getObjects(monitor, getContainer(), this);
    }

    public XuguTableConstraint getConstraint(DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
    	return getContainer().constraintCache.getObject(monitor, getContainer(), this, ukName);
    }

    public DBSTableForeignKey getForeignKey(DBRProgressMonitor monitor, String ukName) throws DBException
    {
        return DBUtils.findObject(getAssociations(monitor), ukName);
    }

    @Override
    public Collection<XuguTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return getContainer().foreignKeyCache.getAllObjects(monitor, this.getSchema());
    }

    @Override
    public Collection<XuguTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return getContainer().foreignKeyCache.getAllObjects(monitor, this.getSchema());
    }

    public String getDDL(DBRProgressMonitor monitor, XuguDDLFormat ddlFormat, Map<String, Object> options)
        throws DBException
    {
        return XuguUtils.getDDL(monitor, getTableTypeName(), this, ddlFormat, options);
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    public static XuguTableBase findTable(DBRProgressMonitor monitor, XuguDataSource dataSource, String ownerName, String tableName) throws DBException
    {
        XuguSchema refSchema = dataSource.getSchema(monitor, ownerName);
        if (refSchema == null) {
            log.warn("Referenced schema '" + ownerName + "' not found");
            return null;
        } else {
            XuguTableBase refTable = refSchema.tableCache.getObject(monitor, refSchema, tableName);
            if (refTable == null) {
                log.warn("Referenced table '" + tableName + "' not found in schema '" + ownerName + "'");
            }
            return refTable;
        }
    }

    static class TriggerCache extends JDBCStructCache<XuguTableBase, XuguTableTrigger, XuguTriggerColumn> {
        TriggerCache()
        {
            super("TRIGGER_NAME");
        }
        
        //获取触发器信息
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguTableBase owner) throws SQLException
        {
        	StringBuilder builder = new StringBuilder();
        	//对象类型为table
        	if(owner.getType()==0) {
        		builder.append("SELECT *, tr.OBJ_ID AS TABLE_ID\nFROM ");
            	builder.append(owner.getDataSource().getRoleFlag());
            	builder.append("_TRIGGERS tr WHERE SCHEMA_ID=(SELECT SCHEMA_ID FROM ");
            	builder.append(owner.getDataSource().getRoleFlag());
            	builder.append("_SCHEMAS WHERE SCHEMA_NAME=?) AND TABLE_ID=(SELECT TABLE_ID FROM ");
            	builder.append(owner.getDataSource().getRoleFlag());
            	builder.append("_TABLES WHERE TABLE_NAME=?)\n ORDER BY TRIG_NAME");
        	}
        	//对象类型为view
        	else {
        		builder.append("SELECT *, tr.OBJ_ID AS VIEW_ID\nFROM ");
            	builder.append(owner.getDataSource().getRoleFlag());
            	builder.append("_TRIGGERS tr WHERE SCHEMA_ID=(SELECT SCHEMA_ID FROM ");
            	builder.append(owner.getDataSource().getRoleFlag());
            	builder.append("_SCHEMAS WHERE SCHEMA_NAME=?) AND VIEW_ID=(SELECT VIEW_ID FROM ");
            	builder.append(owner.getDataSource().getRoleFlag());
            	builder.append("_VIEWS WHERE VIEW_NAME=?)\n ORDER BY TRIG_NAME");
        	}
        	
            JDBCPreparedStatement dbStat = session.prepareStatement(builder.toString());
            dbStat.setString(1, owner.getSchema().getName());
            dbStat.setString(2, owner.getName());
            return dbStat;
        }

        @Override
        protected XuguTableTrigger fetchObject(@NotNull JDBCSession session, @NotNull XuguTableBase owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguTableTrigger(owner, resultSet);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull XuguTableBase owner, @Nullable XuguTableTrigger forObject) throws SQLException
        {
            //通过获取description中的字段名来查询触发器的列信息
        	String cols = forObject.getDescription();
        	int i1 = cols.indexOf(" OF ");
        	int i2 = cols.indexOf(" ON ");
        	StringBuilder sql = new StringBuilder();
        	sql.append("SELECT * FROM "+ owner.getDataSource().getRoleFlag() +"_COLUMNS WHERE ");
    		sql.append("TABLE_ID=(SELECT TABLE_ID FROM " + owner.getDataSource().getRoleFlag() + "_TABLES WHERE TABLE_NAME='");
    		sql.append(owner.getName());
    		sql.append("' AND SCHEMA_ID='");
    		sql.append(owner.getSchema().getId());
    		sql.append("')");
        	//指定了特殊字段则仅查询指定字段，若没有则直接查该表的所有列
        	if(i1!=-1) {
        		cols = cols.substring(i1+4, i2);
        		String[] col = cols.split(",");
        		sql.append(" AND COL_NAME IN(");
        		for(int i=0; i<col.length; i++) {
        			sql.append("'");
        			sql.append(col[i]);
        			sql.append("'");
        			if(i!=col.length-1) {
        				sql.append(",");
        			}
        		}
        		sql.append(")");
        	}
        	System.out.println("find trigger columns "+sql.toString());
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            return dbStat;
        }

        @Override
        protected XuguTriggerColumn fetchChild(@NotNull JDBCSession session, @NotNull XuguTableBase owner, @NotNull XuguTableTrigger parent, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
        {
            XuguTableBase refTable = XuguTableBase.findTable(
                session.getProgressMonitor(),
                owner.getDataSource(),
                owner.getSchema().getName(),
                owner.getName());
            if (refTable != null) {
                final String columnName = JDBCUtils.safeGetString(dbResult, "COL_NAME");
                XuguTableColumn tableColumn = refTable.getAttribute(session.getProgressMonitor(), columnName);
                if (tableColumn == null) {
                    log.debug("Column '" + columnName + "' not found in table '" + refTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + "' for trigger '" + parent.getName() + "'");
                }
                return new XuguTriggerColumn(session.getProgressMonitor(), parent, tableColumn, dbResult);
            }
        	// do nothing
            return null;
        }

    }

}
