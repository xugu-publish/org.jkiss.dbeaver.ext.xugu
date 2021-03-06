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
import org.jkiss.dbeaver.ext.xugu.XuguUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSPackage;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.utils.CommonUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author xugu-publish
   *   包含包相关的基本信息，以及存储过程缓存
 */
public class XuguPackage extends XuguSchemaObject
    implements XuguSourceObject, DBPScriptObjectExt, DBSObjectContainer, DBSPackage, DBPRefreshableObject, DBSProcedureContainer
{
    private final ProceduresCache proceduresCache = new ProceduresCache();
    protected boolean valid;
    private String comment;
    private Timestamp createTime;
	private String sourceDeclaration;
    private String sourceDefinition;

    public XuguPackage(XuguSchema schema, String name)
    {
        super(schema, name, false);
    }

    public XuguPackage(XuguSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "PACK_NAME"), true);
        this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
        this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.sourceDeclaration = JDBCUtils.safeGetString(dbResult, "SPEC");
        this.sourceDefinition = JDBCUtils.safeGetString(dbResult, "BODY");
        if(this.sourceDeclaration == null) {
        	this.sourceDeclaration = "--Null Package header";
        }
        if(this.sourceDefinition == null) {
        	this.sourceDefinition = "-- Null Package header";
        }
    }

    @Override
    @Property(viewable = true, editable = false, updatable = false, order=1)
    public String getName() {
    	return this.name;
    }
    
    @Property(viewable = true, editable = true, updatable = true, order=2)
    public String getComment() {
    	return comment;
    }
    
    @Property(viewable = true, editable = false, updatable = false, order=3)
    public Timestamp getCreateTime() {
    	return createTime;
    }
    
    @Property(viewable = true, order = 4)
    public boolean isValid()
    {
        return valid;
    }

    public void setValid(boolean valid) {
    	this.valid = valid;
    }
    
	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}

    @Override
    public XuguSourceType getSourceType()
    {
        return XuguSourceType.PACKAGE;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBCException
    {
        return sourceDeclaration;
    }

    @Override
    public void setObjectDefinitionText(String sourceDeclaration)
    {
        this.sourceDeclaration = sourceDeclaration;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        return sourceDefinition;
    }

    public void setExtendedDefinitionText(String source)
    {
        this.sourceDefinition = source;
    }

    @Override
    @Association
    public Collection<XuguProcedurePackaged> getProcedures(DBRProgressMonitor monitor) throws DBException
    {
        return proceduresCache.getAllObjects(monitor, this);
    }

    @Override
    public XuguProcedurePackaged getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        return proceduresCache.getObject(monitor, this, uniqueName);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return proceduresCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException
    {
        return proceduresCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSObject> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return XuguProcedurePackaged.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException
    {
        proceduresCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        this.proceduresCache.clearCache();
        return this;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.valid = XuguUtils.getObjectStatus(monitor, this, XuguObjectType.PACKAGE);
    }

    @Override
    public DBEPersistAction[] getCompileActions()
    {
        List<DBEPersistAction> actions = new ArrayList<>();
        /*if (!CommonUtils.isEmpty(sourceDeclaration)) */{
            actions.add(
                new XuguObjectPersistAction(
                    XuguObjectType.PACKAGE,
                    "Compile package",
                    "ALTER PACKAGE " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
                ));
        }
        if (!CommonUtils.isEmpty(sourceDefinition)) {
            actions.add(
                new XuguObjectPersistAction(
                    XuguObjectType.PACKAGE_BODY,
                    "Compile package body",
                    "ALTER PACKAGE " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE BODY"
                ));
        }
        return actions.toArray(new DBEPersistAction[actions.size()]);
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    static class ProceduresCache extends JDBCObjectCache<XuguPackage, XuguProcedurePackaged> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguPackage owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT P.*,CASE WHEN A.DATA_TYPE IS NULL THEN 'PROCEDURE' ELSE 'FUNCTION' END as PROCEDURE_TYPE FROM ALL_PROCEDURES P\n" +
                "LEFT OUTER JOIN ALL_ARGUMENTS A ON A.OWNER=P.OWNER AND A.PACKAGE_NAME=P.OBJECT_NAME AND A.OBJECT_NAME=P.PROCEDURE_NAME AND A.ARGUMENT_NAME IS NULL AND A.DATA_LEVEL=0\n" +
                "WHERE P.OWNER=? AND P.OBJECT_NAME=?\n" +
                "ORDER BY P.PROCEDURE_NAME");
            dbStat.setString(1, owner.getSchema().getName());
            dbStat.setString(2, owner.getName());
            return dbStat;
        }

        @Override
        protected XuguProcedurePackaged fetchObject(@NotNull JDBCSession session, @NotNull XuguPackage owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new XuguProcedurePackaged(owner, dbResult);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, XuguPackage owner, Iterator<XuguProcedurePackaged> objectIter)
        {
            Map<String, XuguProcedurePackaged> overloads = new HashMap<>();
            while (objectIter.hasNext()) {
                final XuguProcedurePackaged proc = objectIter.next();
                if (CommonUtils.isEmpty(proc.getName())) {
                    objectIter.remove();
                    continue;
                }
                final XuguProcedurePackaged overload = overloads.get(proc.getName());
                if (overload == null) {
                    overloads.put(proc.getName(), proc);
                } else {
                    if (overload.getOverloadNumber() == null) {
                        overload.setOverload(1);
                    }
                    proc.setOverload(overload.getOverloadNumber() + 1);
                    overloads.put(proc.getName(), proc);
                }
            }
        }
    }

}
