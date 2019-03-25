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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Collection;

/**
 * XuguTableTrigger
 */
public class XuguTableTrigger extends XuguTrigger<XuguTableBase>
{
    private static final Log log = Log.getLog(XuguTableTrigger.class);

    private XuguSchema ownerSchema;

    public XuguTableTrigger(XuguTableBase table, String name)
    {
        super(table, name);
        this.ownerSchema = table.getSchema();
    }

    public XuguTableTrigger(
        XuguTableBase table,
        ResultSet dbResult)
    {
        super(table, dbResult);
        this.ownerSchema = table.getSchema();
    }

    @Override
    @Property(viewable = true, order = 4)
    public XuguTableBase getTable()
    {
        return parent;
    }

    @Override
    public XuguSchema getSchema() {
        return this.ownerSchema;
    }

    @Association
    public Collection<XuguTriggerColumn> getColumns(DBRProgressMonitor monitor) throws DBException
    {
        return parent.triggerCache.getChildren(monitor, parent, this);
    }

	@Override
	public void setObjectDefinitionText(String source) {
		// TODO Auto-generated method stub
		super.setObjectDefinitionText(source);
	}

}
