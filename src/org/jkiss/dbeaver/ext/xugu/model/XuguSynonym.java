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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAlias;
import org.jkiss.dbeaver.model.struct.DBSObject;
import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * @author Maple4Real
 * 同义词信息类，包含同义词相关的基本信息
 */
public class XuguSynonym extends XuguSchemaObject implements DBSAlias {

	private int objectDBID;
	private int objectSchemaID;
    private String objectSchemaName;
    private int objectUserID;
    private String objectName;
    private String targetSchemaName;
    private String targetName;
    private boolean isPublic;
    private boolean valid;
    private boolean deleted;
    private Timestamp createTime;

    public XuguSynonym(XuguSchema schema, String name)
    {
        super(schema, name, false);
    }

    public XuguSynonym(DBRProgressMonitor monitor, JDBCSession session,XuguSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "SYNO_NAME"), true);
        //this.objectTypeName = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
        this.objectDBID = JDBCUtils.safeGetInt(dbResult, "DB_ID");
        this.objectSchemaID = JDBCUtils.safeGetInt(dbResult, "SCHEMA_ID");
        this.objectUserID = JDBCUtils.safeGetInt(dbResult, "USER_ID");
        this.objectName = JDBCUtils.safeGetString(dbResult, "SYNO_NAME");
        this.targetSchemaName = JDBCUtils.safeGetString(dbResult, "SCHEMA_NAME");
        this.targetName = JDBCUtils.safeGetString(dbResult, "TARG_NAME");
        this.isPublic = JDBCUtils.safeGetBoolean(dbResult, "IS_PUBLIC");
        this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
    }

    @Override
    @Property(viewable = true, editable = false, updatable = false, order=1)
    public String getName()
    {
        return this.name;
    }
    
    @Property(viewable = true, editable = false, updatable=false, valueTransformer = DBObjectNameCaseTransformer.class, order = 2)
    public String getTargetSchemaName() {
    	return targetSchemaName;
    }
    
    @Property(viewable = true, editable = true, updatable=false, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
    public String getTargetName() {
    	return targetName;
    }

    @Property(viewable = true, editable = false, updatable=false, valueTransformer = DBObjectNameCaseTransformer.class, order = 4)
    public Timestamp getCreateTime() {
    	return createTime;
    }
    
    @Property(viewable = true, editable = false, updatable=false, valueTransformer = DBObjectNameCaseTransformer.class, order = 5)
    public boolean isPublic() {
    	return isPublic;
    }
    
    @Property(viewable = true, order = 6)
	public boolean isValid() {
		return valid;
	}

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
    	objectName = name;
    	this.name = name;
    }
    
    public int getObjectDBID() {
		return objectDBID;
	}

	public void setObjectDBID(int objectDBID) {
		this.objectDBID = objectDBID;
	}

	public int getObjectSchemaID() {
		return objectSchemaID;
	}

	public void setObjectSchemaID(int objectSchemaID) {
		this.objectSchemaID = objectSchemaID;
	}

	public String getObjectSchemaName() {
		return objectSchemaName;
	}

	public void setObjectSchemaName(String objectSchemaName) {
		this.objectSchemaName = objectSchemaName;
	}

	public int getObjectUserID() {
		return objectUserID;
	}

	public void setObjectUserID(int objectUserID) {
		this.objectUserID = objectUserID;
	}

	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	public void setTargetSchemaID(String targetSchemaName) {
		this.targetSchemaName = targetSchemaName;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}

    public void setTargetName(String name) {
    	targetName = name;
    }
    
    public void setPublic(boolean isPublic) {
    	this.isPublic = isPublic;
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
