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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * Oracle abstract partition
 */
public abstract class XuguPartitionBase<PARENT extends DBSObject> extends XuguObject<PARENT>
{
    public enum PartitionType {
        RANGE,
        HASH,
        LIST,
    }

    public static final String CAT_PARTITIONING = "Partitioning";

    public static class PartitionInfoBase {
        private PartitionType partitionType;
        private PartitionType subpartitionType;
//        private Object partitionTablespace;

        @Property(category = CAT_PARTITIONING, order = 120)
        public PartitionType getPartitionType()
        {
            return partitionType;
        }

        @Property(category = CAT_PARTITIONING, order = 121)
        public PartitionType getSubpartitionType()
        {
            return subpartitionType;
        }

//        @Property(category = CAT_PARTITIONING, order = 122)
//        public Object getPartitionTablespace()
//        {
//            return partitionTablespace;
//        }

        public PartitionInfoBase(DBRProgressMonitor monitor, XuguDataSource dataSource, ResultSet dbResult) throws DBException
        {
            this.partitionType = CommonUtils.valueOf(PartitionType.class, JDBCUtils.safeGetStringTrimmed(dbResult, "PARTITIONING_TYPE"));
            this.subpartitionType = CommonUtils.valueOf(PartitionType.class, JDBCUtils.safeGetStringTrimmed(dbResult, "SUBPARTITIONING_TYPE"));
//            this.partitionTablespace = JDBCUtils.safeGetStringTrimmed(dbResult, "DEF_TABLESPACE_NAME");
//            if (dataSource.isAdmin()) {
//                this.partitionTablespace = dataSource.getTablespaceCache().getObject(monitor, dataSource, (String) partitionTablespace);
//            }
        }
    }

//    private int position;
    private String partiValue;
    private int partiNo;
    private boolean online;
//    private boolean usable;
//    private Object tablespace;
//    private long numRows;
//    private long sampleSize;
//    private Timestamp lastAnalyzed;

    protected XuguPartitionBase(PARENT parent, boolean subpartition, ResultSet dbResult)
    {
        super(
            parent,
            subpartition ?
                JDBCUtils.safeGetString(dbResult, "SUBPARTI_NAME") :
                JDBCUtils.safeGetString(dbResult, "PARTI_NAME"),
            true);
        this.partiValue = subpartition? JDBCUtils.safeGetString(dbResult, "SUBPARTI_VAL") : JDBCUtils.safeGetString(dbResult, "PARTI_VAL");
        this.partiNo = subpartition? JDBCUtils.safeGetInt(dbResult, "SUBPARTI_NO") : JDBCUtils.safeGetInt(dbResult, "PARTI_NO");
        this.online = subpartition? true : JDBCUtils.safeGetBoolean(dbResult, "ONLINE");
    }

//    @Override
//    public Object getLazyReference(Object propertyId)
//    {
//        return tablespace;
//    }

    @Property(viewable = true, order = 10)
    public String getPartiVal()
    {
        return partiValue;
    }
    
    @Property(viewable = true, order = 10)
    public int getPartiNo()
    {
        return partiNo;
    }

    @Property(viewable = true, order = 11)
    public boolean isOnline()
    {
        return online;
    }

//    @Property(viewable = true, order = 12)
//    @LazyProperty(cacheValidator = XuguTablespace.TablespaceReferenceValidator.class)
//    public Object getTablespace(DBRProgressMonitor monitor) throws DBException
//    {
//        return XuguTablespace.resolveTablespaceReference(monitor, this, null);
//    }

//    @Property(viewable = true, order = 30)
//    public String getHighValue()
//    {
//        return highValue;
//    }
//
//    @Property(viewable = true, order = 40)
//    public long getNumRows()
//    {
//        return numRows;
//    }
//
//    @Property(viewable = true, order = 41)
//    public long getSampleSize()
//    {
//        return sampleSize;
//    }
//
//    @Property(viewable = true, order = 42)
//    public Timestamp getLastAnalyzed()
//    {
//        return lastAnalyzed;
//    }
}
