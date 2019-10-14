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
import org.jkiss.dbeaver.ext.xugu.XuguUtils;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema.UDTCache;
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author xugu-publish
   *  自定义类型信息类
 */
public class XuguUDT extends XuguSchemaObject 
	implements XuguSourceObject,DBPScriptObjectExt, DBSObjectContainer, DBSDataType, DBPRefreshableObject
{

	private final UDTCache udtCache = new UDTCache();
	private int    typeId;
	private String typeName;
	private String objectSchemaName;
	protected boolean valid;
    private String    comment;
    private Timestamp createTime;
	private String typeHead;
	private String typeBody;

    public XuguUDT(XuguSchema schema, String name)
    {
        super(schema, name, false);
    }

    public XuguUDT(XuguSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "TYPE_NAME"), true);
        this.typeId = JDBCUtils.safeGetInt(dbResult, "TYPE_ID");
        this.typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
        this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
        this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.objectSchemaName = JDBCUtils.safeGetString(dbResult, "SCHEMA_NAME");
        this.typeHead = JDBCUtils.safeGetString(dbResult, "SPEC");
        this.typeBody = JDBCUtils.safeGetString(dbResult, "BODY");
    }

    @Property(viewable = true, editable = false, order = 1)
    public int getTypeId() {
    	return typeId;
    }
    
    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 2)
    public String getName()
    {
        return this.typeName;
    }
    
    @Property(viewable = true, editable = false, order = 3)
	public String getObjectSchemaName() {
		return objectSchemaName;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 18)
	public String getComment() {
		return comment;
	}

    @Property(viewable = true, editable = false, order = 5)
	public Timestamp getCreateTime() {
		return createTime;
	}

    @Property(viewable = true, editable = false, order = 6)
    public boolean isValid() {
    	return valid;
    }
    
    public Object getObjectOwner()
    {
        final XuguSchema schema = getDataSource().schemaCache.getCachedObject(objectSchemaName);
        return schema == null ? objectSchemaName : schema;
    }
    
    @Override
    public void setName(String name) {
    	this.name = name;
    	this.typeName = name;
    }
    
	public void setTypeId(int typeId) {
		this.typeId = typeId;
	}

	public void setObjectSchemaName(String objectSchemaName) {
		this.objectSchemaName = objectSchemaName;
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

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

    @Override
    public XuguSourceType getSourceType()
    {
        return XuguSourceType.TYPE;
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

	@Override
	public String getTypeName() {
		if (name!=null) {
			if(this.name.indexOf(".")!=-1) {
				return this.name.split(".")[1];
			}else {
				return this.name;
			}			
		} else {
			return this.name;
		}
	}

	@Override
	public String getFullTypeName() {
		return typeName;
	}

	@Override
	public int getTypeID() {
		return typeId;
	}

	@Override
	public DBPDataKind getDataKind() {
		// TODO Auto-generated method stub
		return DBPDataKind.OBJECT;
	}

	@Override
	public Integer getScale() {
		return -1;
	}

	@Override
	public Integer getPrecision() {
		return -1;
	}

	@Override
	public long getMaxLength() {
		return -1;
	}

	@Override
	public Object geTypeExtension() {
		return null;
	}

	@Override
	public DBSDataType getComponentType(DBRProgressMonitor monitor) throws DBException {
		return null;
	}

	@Override
	public int getMinScale() {
		return -1;
	}

	@Override
	public int getMaxScale() {
		return -1;
	}

	@Override
	public DBCLogicalOperator[] getSupportedOperators(DBSTypedObject attribute) {
		return null;
	}

	@Override
    @Property(hidden = false, editable = true, updatable = true, order = 15)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		// TODO Auto-generated method stub
		return typeHead;
	}

	@Override
    @Property(hidden = false, editable = true, updatable = true, order = 16)
	public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return typeBody;
	}

	@Override
	public void setObjectDefinitionText(String typeHead) {
		// TODO Auto-generated method stub
		this.typeHead = typeHead;
	}
	
	public void setExtendedDefinitionText(String typeBody)
    {
        this.typeBody = typeBody;
    }

	@Override
	public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return udtCache.getAllObjects(monitor, this.parent);
	}

	@Override
	public DBSObject getChild(DBRProgressMonitor monitor, String childName) throws DBException {
		// TODO Auto-generated method stub
		return udtCache.getObject(monitor, this.parent, childName);
	}

	@Override
	public Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return XuguDataType.class;
	}

	@Override
	public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {
		// TODO Auto-generated method stub
		udtCache.getAllObjects(monitor, this.getSchema());
	}

	@Override
	public DBSObjectState getObjectState() {
		// TODO Auto-generated method stub
		return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	@Override
	public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
		// TODO Auto-generated method stub
		this.valid = XuguUtils.getObjectStatus(monitor, this, XuguObjectType.UDT);
	}

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		this.udtCache.clearCache();
        return this;
	}

	@Override
	public DBEPersistAction[] getCompileActions() {
		// TODO Auto-generated method stub
		List<DBEPersistAction> actions = new ArrayList<>();
        /*if (!CommonUtils.isEmpty(sourceDeclaration)) */{
            actions.add(
                new XuguObjectPersistAction(
                    XuguObjectType.PACKAGE,
                    "Compile udt head",
                    "ALTER TYPE " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
                ));
        }
        if (!CommonUtils.isEmpty(typeHead)) {
            actions.add(
                new XuguObjectPersistAction(
                    XuguObjectType.PACKAGE_BODY,
                    "Compile udt body",
                    "ALTER TYPE " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE BODY"
                ));
        }
        return actions.toArray(new DBEPersistAction[actions.size()]);
	}
}
