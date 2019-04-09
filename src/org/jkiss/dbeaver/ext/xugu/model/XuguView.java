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
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.xml.validation.Schema;

/**
 * OracleView
 */
public class XuguView extends XuguTableBase implements XuguSourceObject
{
    private static final Log log = Log.getLog(XuguView.class);
    private String viewText;
    private int viewId;
    private boolean force;
    private boolean replace;
    private Collection<XuguTableColumn> columns = new ArrayList<XuguTableColumn>();
    public XuguView(XuguSchema schema, String name)
    {
        super(schema, name, false);
    }

    public XuguView(DBRProgressMonitor monitor, JDBCSession session, XuguSchema schema, ResultSet dbResult)
    {
        super(schema, dbResult, 1);
        this.viewId = JDBCUtils.safeGetInt(dbResult, "VIEW_ID");
        this.viewText = JDBCUtils.safeGetString(dbResult, "DEFINE");
        this.force = false;
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
    
    public int getId() {
    	return viewId;
    }
    
    @NotNull
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    @Override
    public String getName()
    {
        return super.getName();
    }

    @Override
    public boolean isView()
    {
        return true;
    }

    @Override
    public XuguSourceType getSourceType()
    {
        return XuguSourceType.VIEW;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        return viewText;
    }

    @Override
    public void setObjectDefinitionText(String source)
    {
        this.viewText = source;
    }

    @Property(viewable=true, editable=true, updatable=true, order=2)
    public boolean isReplace() {
    	return this.replace;
    }
    
    public void setReplace(boolean replace) {
    	this.replace = replace;
    }
    
    @Property(viewable=true, editable=true, updatable=true, order=3)
    public boolean isForce() {
    	return this.force;
    }
    
    public void setForce(boolean force) {
    	this.force = force;
    }
    
    @Override
    protected String getTableTypeName()
    {
        return "VIEW";
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
                "ALTER VIEW " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
            )};
    }

}
