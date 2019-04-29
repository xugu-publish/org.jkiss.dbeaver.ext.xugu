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

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.util.ArrayList;
import java.util.List;

/**
 * XuguTableConstraint
 */
public abstract class XuguTableConstraintBase extends JDBCTableConstraint<XuguTableBase> {

    private static final Log log = Log.getLog(XuguTableConstraintBase.class);

    private XuguObjectStatus status;
    private List<XuguTableConstraintColumn> columns;

    public XuguTableConstraintBase(XuguTableBase xuguTable, String name, DBSEntityConstraintType constraintType, XuguObjectStatus status, boolean persisted)
    {
        super(xuguTable, name, null, constraintType, persisted);
        this.status = status;
    }

    protected XuguTableConstraintBase(XuguTableBase xuguTableBase, String name, String description, DBSEntityConstraintType constraintType, boolean persisted)
    {
        super(xuguTableBase, name, description, constraintType, persisted);
    }

    @NotNull
    @Override
    public XuguDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @NotNull
    @Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return constraintType;
    }

    @Property(viewable = true, editable = false, order = 7)
    public XuguObjectStatus getStatus()
    {
        return status;
    }

    @Override
    public List<XuguTableConstraintColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(XuguTableConstraintColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        this.columns.add(column);
    }

    void setColumns(List<XuguTableConstraintColumn> columns)
    {
        this.columns = columns;
    }

}
