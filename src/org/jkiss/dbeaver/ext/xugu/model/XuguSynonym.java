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
 * Oracle synonym
 */
public class XuguSynonym extends XuguSchemaObject implements DBSAlias {

	private int objectDBID;
    private int objectSchemaID;
    private String objectSchemaName;
    private int objectUserID;
    //private String objectTypeName;
    private String objectName;
    private int targetSchemaID;
    private String targetName;
    private boolean is_pub;
    private boolean valid;
    private boolean deleted;
    private Date createTime;
//    private String dbLink;

    public XuguSynonym(XuguSchema schema, ResultSet dbResult)
    {
        super(schema, dbResult!=null?JDBCUtils.safeGetString(dbResult, "SYNO_NAME"):"NEW_SYNO", true);
        //this.objectTypeName = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
        if(dbResult!=null) {
        	this.objectDBID = JDBCUtils.safeGetInt(dbResult, "DB_ID");
            this.objectSchemaID = JDBCUtils.safeGetInt(dbResult, "SCHEMA_ID");
            this.objectUserID = JDBCUtils.safeGetInt(dbResult, "USER_ID");
            this.objectName = JDBCUtils.safeGetString(dbResult, "SYNO_NAME");
            this.targetSchemaID = JDBCUtils.safeGetInt(dbResult, "TARG_SCHE_ID");
            this.targetName = JDBCUtils.safeGetString(dbResult, "TARG_NAME");
            this.is_pub = JDBCUtils.safeGetBoolean(dbResult, "IS_PUBLIC");
            this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
//            this.deleted = JDBCUtils.safeGetBoolean(dbResult, "DELETED");
            this.createTime = JDBCUtils.safeGetDate(dbResult, "CREATE_TIME");
//            this.dbLink = JDBCUtils.safeGetString(dbResult, "DB_LINK");
        }
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }
    
    public String getTargetName() {
    	return targetName;
    }

    @Property(viewable = true, order = 3)
    public Object getObjectOwner()
    {
        final XuguSchema schema = getDataSource().schemaCache.getCachedObject(objectSchemaName);
        return schema == null ? objectSchemaName : schema;
    }


//    @Property(viewable = true, order = 5)
//    public Object getDbLink(DBRProgressMonitor monitor) throws DBException
//    {
//        return XuguDBLink.resolveObject(monitor, getSchema(), dbLink);
//    }

    @Override
    public DBSObject getTargetObject(DBRProgressMonitor monitor) throws DBException {
        Object object = getObject(monitor);
        if (object instanceof DBSObject) {
            return (DBSObject) object;
        }
        return null;
    }
    
    public void setName(String name) {
    	objectName = name;
    	this.name = name;
    }
    
    public void setTargetName(String name) {
    	targetName = name;
    }

	public Object getObject(DBRProgressMonitor monitor) throws DBException {
        return XuguObjectType.resolveObject(
            monitor,
            getDataSource(),
            null,
            "SYNONYM",
            objectSchemaName,
            objectName);
	}
}
