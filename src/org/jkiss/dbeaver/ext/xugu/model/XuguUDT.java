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
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAlias;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.Date;
import java.sql.ResultSet;

/**
 * Xugu synonym
 */
public class XuguUDT extends XuguSchemaObject implements DBSAlias {

	private String typeName;
	private String objectSchemaName;
	private String typeHead;
	private String typeBody;

    public XuguUDT(XuguSchema schema, ResultSet dbResult)
    {
        super(schema, dbResult!=null?JDBCUtils.safeGetString(dbResult, "TYPE_NAME"):"NEW_TYPE", true);
        if(dbResult!=null) {
        	this.typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
        	this.typeHead = JDBCUtils.safeGetString(dbResult, "SPEC");
        	this.typeBody = JDBCUtils.safeGetString(dbResult, "BODY");
        }
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return this.typeName;
    }
    
    @Property(viewable = true, editable = true, multiline = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 2)
    public String getTypeHead()
    {
        return this.typeHead;
    }
    
    @Property(viewable = true, editable = true, multiline = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
    public String getTypeBody()
    {
        return this.typeBody;
    }

//    @Property(viewable = false, editable = false)
    public Object getObjectOwner()
    {
        final XuguSchema schema = getDataSource().schemaCache.getCachedObject(objectSchemaName);
        return schema == null ? objectSchemaName : schema;
    }

    @Override
    public DBSObject getTargetObject(DBRProgressMonitor monitor) throws DBException {
        Object object = getObject(monitor);
        if (object instanceof DBSObject) {
            return (DBSObject) object;
        }
        return null;
    }
    
    @Override
    public void setName(String name) {
    	this.name = name;
    	this.typeName = name;
    }

    public Object getObject(DBRProgressMonitor monitor) throws DBException {
        return XuguObjectType.resolveObject(
            monitor,
            getDataSource(),
            null,
            "UDT",
            objectSchemaName,
            typeName);
	}
}
