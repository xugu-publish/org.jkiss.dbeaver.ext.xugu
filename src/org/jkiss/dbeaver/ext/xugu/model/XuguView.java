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
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ext.xugu.XuguUtils;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;

/**
 * 	@author xugu-publish
 *  视图信息类
 */
public class XuguView extends XuguTableBase implements XuguSourceObject
{

    private String viewText;
    public XuguView(XuguSchema schema, String name)
    {
        super(schema, name, false);
    }

    public XuguView(DBRProgressMonitor monitor, JDBCSession session, XuguSchema schema, ResultSet dbResult)
    {
        super(schema, dbResult, XuguObjectType.VIEW);
        this.viewText = JDBCUtils.safeGetString(dbResult, "DEFINE");
    }
    
    @Override
    @Association
    public Collection<XuguTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
    	return getContainer().viewCache.getChildren(monitor, getContainer(), this);
    }
    
    @Override
    public XuguTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().viewCache.getChild(monitor, getContainer(), this, attributeName);
    }
    
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = 15)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        return viewText;
    }

    @Override
    public XuguSourceType getSourceType()
    {
        return XuguSourceType.VIEW;
    }
    
    @Override
    public void setObjectDefinitionText(String source)
    {
        this.viewText = source;
    }

    @Override
    public boolean isView()
    {
        return true;
    }

    @Override
    protected String getTableTypeName()
    {
        return XuguObjectType.VIEW.getTypeName();
    }
    
    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.valid = XuguUtils.getObjectStatus(monitor, this, XuguObjectType.VIEW);
    }

    public String getViewText() {
        return viewText;
    }

    public void setViewText(String viewText) {
        this.viewText = viewText;
    }

    @Override
    public DBEPersistAction[] getCompileActions()
    {
        return new DBEPersistAction[] {
            new XuguObjectPersistAction(
                XuguObjectType.VIEW,
                "Compile view",
                "ALTER VIEW " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " RECOMPILE"
            )};
    }
}
