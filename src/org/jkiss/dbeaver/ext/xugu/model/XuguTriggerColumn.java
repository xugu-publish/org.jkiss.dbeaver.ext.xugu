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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTriggerColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;

/**
 * XuguTriggerColumn
 */
public class XuguTriggerColumn extends AbstractTriggerColumn
{
    private static final Log log = Log.getLog(XuguTriggerColumn.class);

    private XuguTriggerBase trigger;
    private String name;
    private XuguTableColumn tableColumn;

    public XuguTriggerColumn(
        DBRProgressMonitor monitor,
        XuguTriggerBase trigger,
        XuguTableColumn tableColumn,
        ResultSet dbResult) throws DBException
    {
        this.trigger = trigger;
        this.tableColumn = tableColumn;
        this.name = JDBCUtils.safeGetString(dbResult, "COL_NAME");
        //不存在col_list列 是否等价于define？
    }

    XuguTriggerColumn(XuguTriggerBase trigger, XuguTriggerColumn source)
    {
        this.trigger = trigger;
        this.tableColumn = source.tableColumn;
    }

    @Override
    public XuguTriggerBase getTrigger()
    {
        return trigger;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Override
    @Property(viewable = true, order = 2)
    public XuguTableColumn getTableColumn()
    {
        return tableColumn;
    }

    @Override
    public int getOrdinalPosition()
    {
        return 0;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    @Override
    public XuguTriggerBase getParentObject()
    {
        return trigger;
    }

    @NotNull
    @Override
    public XuguDataSource getDataSource()
    {
        return trigger.getDataSource();
    }

    @Override
    public String toString() {
        return getName();
    }
}
