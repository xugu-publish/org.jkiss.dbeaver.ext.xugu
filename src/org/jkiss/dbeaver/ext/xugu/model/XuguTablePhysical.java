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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.ext.xugu.XuguUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Maple4Real
 * 加载表相关物理信息（分区信息、表空间信息）
 */
public abstract class XuguTablePhysical extends XuguTableBase implements DBSObjectLazy<XuguDataSource>
{
    private static final Log log = Log.getLog(XuguTablePhysical.class);

    public static final String CAT_STATISTICS = "Statistics";

    //private boolean valid;
    private long rowCount;
    private Long realRowCount;
    private Object tablespace;
    private Integer partitioned;
    public PartitionCache partitionCache;
    public SubPartitionCache subPartitionCache;

    protected XuguTablePhysical(XuguSchema schema, String name)
    {
        super(schema, name, false);
        this.partitionCache = new PartitionCache();
        this.subPartitionCache = new SubPartitionCache();
    }

    protected XuguTablePhysical(
        XuguSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult, 0);

        //加载表分区信息
        this.partitioned = JDBCUtils.safeGetInteger(dbResult, "PARTI_TYPE");
        this.partitionCache = new PartitionCache();
        this.subPartitionCache = new SubPartitionCache();
        System.out.println("yes");
    }

    @Override
    public Object getLazyReference(Object propertyId)
    {
        return tablespace;
    }

    public Object getTablespace() {
        return tablespace;
    }

    public void setTablespace(XuguTablespace tablespace) {
        this.tablespace = tablespace;
    }

    @Override
    @Association
    public Collection<XuguTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        // Read indexes using cache
        return this.getContainer().indexCache.getObjects(monitor, getContainer(), this);
    }

    public XuguTableIndex getIndex(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return this.getContainer().indexCache.getObject(monitor, getContainer(), this, name);
    }

    @Association
    public Collection<XuguTablePartition> getPartitions(DBRProgressMonitor monitor)
        throws DBException
    {
        if (partitionCache == null) {
            return null;
        } else {
            this.partitionCache.getAllObjects(monitor, this);
            return this.partitionCache.getAllObjects(monitor, this);
        }
    }
    
    @Association
    public Collection<XuguTableSubPartition> getSubPartitions(DBRProgressMonitor monitor) throws DBException{
    	if(subPartitionCache == null) {
    		return null;
    	}else {
    		this.subPartitionCache.getAllObjects(monitor, this);
    		return this.subPartitionCache.getAllObjects(monitor, this);
    	}
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().indexCache.clearObjectCache(this);
    	//return this;
        return super.refreshObject(monitor);
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.valid = XuguUtils.getObjectStatus(monitor, this, XuguObjectType.TABLE);
    }
    
    public static class PartitionCache extends JDBCObjectCache<XuguTablePhysical, XuguTablePartition> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguTablePhysical table) throws SQLException
        {        	
        	StringBuilder builder = new StringBuilder();
        	builder.append("SELECT * FROM ");
        	builder.append(table.getSchema().getRoleFlag());
        	builder.append("_PARTIS P INNER JOIN (SELECT PARTI_TYPE, PARTI_KEY, AUTO_PARTI_TYPE, AUTO_PARTI_SPAN, TABLE_ID, TABLE_NAME FROM ");
        	builder.append(table.getSchema().getRoleFlag());
        	builder.append("_TABLES T WHERE TABLE_NAME = '");
        	builder.append(table.getName());
        	builder.append("') ON P.TABLE_ID = T.TABLE_ID");
        	
            final JDBCPreparedStatement dbStat = session.prepareStatement(builder.toString());
            return dbStat;
        }

		@Override
		protected XuguTablePartition fetchObject(JDBCSession session, XuguTablePhysical owner, JDBCResultSet resultSet)
				throws SQLException, DBException {
			// TODO Auto-generated method stub
			return new XuguTablePartition(owner, false, resultSet);
		}
    }

    public static class SubPartitionCache extends JDBCObjectCache<XuguTablePhysical, XuguTableSubPartition>{
    	@Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguTablePhysical table) throws SQLException
        {        	
    		StringBuilder builder = new StringBuilder();
        	builder.append("SELECT * FROM ");
        	builder.append(table.getSchema().getRoleFlag());
        	builder.append("_SUBPARTIS SP INNER JOIN (SELECT SUBPARTI_TYPE, SUBPARTI_KEY, TABLE_ID, TABLE_NAME FROM  ");
        	builder.append(table.getSchema().getRoleFlag());
        	builder.append("_TABLES T WHERE TABLE_NAME = '");
        	builder.append(table.getName());
        	builder.append("') ON SP.TABLE_ID = T.TABLE_ID");
            final JDBCPreparedStatement dbStat = session.prepareStatement(builder.toString());
            return dbStat;
        }

		@Override
		protected XuguTableSubPartition fetchObject(JDBCSession session, XuguTablePhysical owner, JDBCResultSet resultSet)
				throws SQLException, DBException {
			// TODO Auto-generated method stub
			return new XuguTableSubPartition(owner, true, resultSet);
		}
    }
    
    public static class TablespaceListProvider implements IPropertyValueListProvider<XuguTablePhysical> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(XuguTablePhysical object)
        {
            final List<XuguTablespace> tablespaces = new ArrayList<>();
            try {
                tablespaces.addAll(object.getDataSource().getTablespaces(new VoidProgressMonitor()));
            } catch (DBException e) {
                log.error(e);
            }
            tablespaces.sort(DBUtils.<XuguTablespace>nameComparator());
            return tablespaces.toArray(new XuguTablespace[tablespaces.size()]);
        }
    }
}
