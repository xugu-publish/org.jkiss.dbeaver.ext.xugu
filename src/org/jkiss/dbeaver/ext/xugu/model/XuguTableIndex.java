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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * OracleTableIndex
 */
public class XuguTableIndex extends JDBCTableIndex<XuguSchema, XuguTablePhysical> implements DBSObjectLazy
{

    private Object tablespace;
    private boolean nonUnique;
    private List<XuguTableIndexColumn> columns;

    public XuguTableIndex(
        XuguSchema schema,
        XuguTablePhysical table,
        String indexName,
        ResultSet dbResult)
    {
        super(schema, table, indexName, null, true);
        String indexTypeName = JDBCUtils.safeGetString(dbResult, "INDEX_TYPE");
        this.nonUnique = !"UNIQUE".equals(JDBCUtils.safeGetString(dbResult, "UNIQUENESS"));
        if (XuguConstants.INDEX_TYPE_NORMAL.getId().equals(indexTypeName)) {
            indexType = XuguConstants.INDEX_TYPE_NORMAL;
        } else if (XuguConstants.INDEX_TYPE_BITMAP.getId().equals(indexTypeName)) {
            indexType = XuguConstants.INDEX_TYPE_BITMAP;
        } else if (XuguConstants.INDEX_TYPE_FUNCTION_BASED_NORMAL.getId().equals(indexTypeName)) {
            indexType = XuguConstants.INDEX_TYPE_FUNCTION_BASED_NORMAL;
        } else if (XuguConstants.INDEX_TYPE_FUNCTION_BASED_BITMAP.getId().equals(indexTypeName)) {
            indexType = XuguConstants.INDEX_TYPE_FUNCTION_BASED_BITMAP;
        } else if (XuguConstants.INDEX_TYPE_DOMAIN.getId().equals(indexTypeName)) {
            indexType = XuguConstants.INDEX_TYPE_DOMAIN;
        } else {
            indexType = DBSIndexType.OTHER;
        }
        this.tablespace = JDBCUtils.safeGetString(dbResult, "TABLESPACE_NAME");
    }

    public XuguTableIndex(XuguSchema schema, XuguTablePhysical parent, String name, boolean unique, DBSIndexType indexType)
    {
        super(schema, parent, name, indexType, false);
        this.nonUnique = !unique;

    }

    @NotNull
    @Override
    public XuguDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    @Property(viewable = true, order = 5)
    public boolean isUnique()
    {
        return !nonUnique;
    }

    @Override
    public Object getLazyReference(Object propertyId)
    {
        return tablespace;
    }

    @Property(viewable = true, order = 10)
    @LazyProperty(cacheValidator = XuguTablespace.TablespaceReferenceValidator.class)
    public Object getTablespace(DBRProgressMonitor monitor) throws DBException
    {
        return XuguTablespace.resolveTablespaceReference(monitor, this, null);
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public List<XuguTableIndexColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    @Nullable
    @Association
    public XuguTableIndexColumn getColumn(String columnName)
    {
        return DBUtils.findObject(columns, columnName);
    }

    void setColumns(List<XuguTableIndexColumn> columns)
    {
        this.columns = columns;
    }

    public void addColumn(XuguTableIndexColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        columns.add(column);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            this);
    }

    @Override
    public String toString()
    {
        return getFullyQualifiedName(DBPEvaluationContext.UI);
    }
}
