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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 虚谷数据类型
 */
public class XuguDataType extends XuguObject<DBSObject>
    implements DBSDataType, DBSEntity, DBPQualifiedObject, XuguSourceObject, DBPScriptObjectExt {

    private static final Log log = Log.getLog(XuguDataType.class);

    public static final String TYPE_CODE_COLLECTION = "COLLECTION";
    public static final String TYPE_CODE_OBJECT = "OBJECT";

    static class TypeDesc {
        final DBPDataKind dataKind;
        final int valueType;
        final int length;
        final int precision;
        final int minScale;
        final int maxScale;
        
        private TypeDesc(DBPDataKind dataKind, int valueType, int length, int precision, int minScale, int maxScale)
        {
            this.dataKind = dataKind;
            this.valueType = valueType;
            this.length = length;
            this.precision = precision;
            this.minScale = minScale;
            this.maxScale = maxScale;
        }
    }

    static final Map<String, TypeDesc> PREDEFINED_TYPES = new HashMap<>();
    static final Map<Integer, TypeDesc> PREDEFINED_TYPE_IDS = new HashMap<>();
    //修改了数据类型定义
    static  {
    	PREDEFINED_TYPES.put("INT", new TypeDesc(DBPDataKind.NUMERIC, Types.INTEGER, 10, 0, 0, 0));
    	PREDEFINED_TYPES.put("INTEGER", new TypeDesc(DBPDataKind.NUMERIC, Types.INTEGER, 10, 0, 0, 0));
    	PREDEFINED_TYPES.put("BIGINT", new TypeDesc(DBPDataKind.NUMERIC, Types.BIGINT, 19, 0, 0, 0));
    	PREDEFINED_TYPES.put("FLOAT", new TypeDesc(DBPDataKind.NUMERIC, Types.FLOAT, 38, 0, 6, 0));
    	PREDEFINED_TYPES.put("DOUBLE", new TypeDesc(DBPDataKind.NUMERIC, Types.DOUBLE, 308, 0, 6, 0));
    	PREDEFINED_TYPES.put("TINYINT", new TypeDesc(DBPDataKind.NUMERIC, Types.TINYINT, 3, 0, 0, 0));
    	PREDEFINED_TYPES.put("SMALLINT", new TypeDesc(DBPDataKind.NUMERIC, Types.SMALLINT, 5, 0, 0, 0));
    	PREDEFINED_TYPES.put("NUMERIC", new TypeDesc(DBPDataKind.NUMERIC, Types.NUMERIC, 0, 0, 0, 0));
    	PREDEFINED_TYPES.put("CHAR", new TypeDesc(DBPDataKind.STRING, Types.CHAR, 0, 0, 0, 0));
    	PREDEFINED_TYPES.put("VARCHAR", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 0, 0, 0));
    	PREDEFINED_TYPES.put("CLOB", new TypeDesc(DBPDataKind.CONTENT, Types.CLOB, 0, 0, 0, 0));
    	PREDEFINED_TYPES.put("BOOLEAN", new TypeDesc(DBPDataKind.BOOLEAN, Types.BOOLEAN, 0, 0, 0, 0));
    	PREDEFINED_TYPES.put("BLOB", new TypeDesc(DBPDataKind.CONTENT, Types.BLOB, 0, 0, 0, 0));
        
    	PREDEFINED_TYPES.put("GUID", new TypeDesc(DBPDataKind.ROWID, Types.ROWID, 0, 0, 0, 0));
    	PREDEFINED_TYPES.put("ROWVERSION", new TypeDesc(DBPDataKind.ROWID, Types.ROWID, 0, 0, 0, 0));
    	
    	PREDEFINED_TYPES.put("DATE", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 0, 0, 0, 0));
    	PREDEFINED_TYPES.put("DATETIME", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 6, 0, 0, 0));
    	PREDEFINED_TYPES.put("DATETIME WITH TIME ZONE", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 6, 0, 0, 0));
    	PREDEFINED_TYPES.put("TIME", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 3, 0, 0, 0));
    	PREDEFINED_TYPES.put("TIME WITH TIME ZONE", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 3, 0, 0, 0));
    	PREDEFINED_TYPES.put("TIMESTAMP", new TypeDesc(DBPDataKind.DATETIME, Types.TIMESTAMP, 6, 0, 0, 0));
        
    	PREDEFINED_TYPES.put("INTERVAL YEAR", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 9, 0, 0));
    	PREDEFINED_TYPES.put("INTERVAL MONTH", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 9, 0, 0));
    	PREDEFINED_TYPES.put("INTERVAL DAY", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 9, 0, 0));
    	PREDEFINED_TYPES.put("INTERVAL HOUR", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 9, 0, 0));
    	PREDEFINED_TYPES.put("INTERVAL MINUTE", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 9, 0, 0));
    	PREDEFINED_TYPES.put("INTERVAL SECOND", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 9, 0, 3));
    	PREDEFINED_TYPES.put("INTERVAL YEAR TO MONTH", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 8, 0, 0));
    	PREDEFINED_TYPES.put("INTERVAL DAY TO HOUR", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 7, 0, 0));
    	PREDEFINED_TYPES.put("INTERVAL DAY TO MINUTE", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 6, 0, 0));
    	PREDEFINED_TYPES.put("INTERVAL DAY TO SECOND", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 6, 3, 3));
    	PREDEFINED_TYPES.put("INTERVAL HOUR TO MINUTE", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 7, 0, 0));
    	PREDEFINED_TYPES.put("INTERVAL HOUR TO SECOND", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 7, 3, 3));
    	PREDEFINED_TYPES.put("INTERVAL MINUTE TO SECOND", new TypeDesc(DBPDataKind.STRING, Types.VARCHAR, 0, 7, 3, 3));
       
//    	PREDEFINED_TYPES.put("NULL", new TypeDesc(DBPDataKind.STRING, Types.NULL, 0, 0, 0, 0));
    	PREDEFINED_TYPES.put("GEOMETRY", new TypeDesc(DBPDataKind.OBJECT, Types.JAVA_OBJECT, 0, 0, 0, 0));
    	PREDEFINED_TYPES.put("POINT", new TypeDesc(DBPDataKind.OBJECT, Types.JAVA_OBJECT, 0, 0, 0, 0));
    	PREDEFINED_TYPES.put("BOX", new TypeDesc(DBPDataKind.OBJECT, Types.JAVA_OBJECT, 0, 0, 0, 0));
    	PREDEFINED_TYPES.put("POLYLINE", new TypeDesc(DBPDataKind.OBJECT, Types.JAVA_OBJECT, 0, 0, 0, 0));
    	PREDEFINED_TYPES.put("POLYGON", new TypeDesc(DBPDataKind.OBJECT, Types.JAVA_OBJECT, 0, 0, 0, 0));
    	PREDEFINED_TYPES.put("ROWID", new TypeDesc(DBPDataKind.ROWID, Types.ROWID, 0, 0, 0, 0));
    	
        for (TypeDesc type : PREDEFINED_TYPES.values()) {
            PREDEFINED_TYPE_IDS.put(type.valueType, type);
        }
    }
    
    private String typeCode;
    private byte[] typeOID;
    private Object superType;
//    private final AttributeCache attributeCache;
//    private final MethodCache methodCache;
    private boolean flagPredefined;
    private boolean flagIncomplete;
    private boolean flagFinal;
    private boolean flagInstantiable;
    private TypeDesc typeDesc;
    private int valueType = java.sql.Types.OTHER;
    private String sourceDeclaration;
    private String sourceDefinition;
    private XuguDataType componentType;

    public XuguDataType(DBSObject owner, String typeName, boolean persisted)
    {
        super(owner, typeName, persisted);
//        this.attributeCache = new AttributeCache();
//        this.methodCache = new MethodCache();
        flagPredefined = true;
        findTypeDesc(typeName);
//        if (owner instanceof XuguDataSource) {
//            
//        }
    }

    public XuguDataType(DBSObject owner, ResultSet dbResult)
    {
        super(owner, JDBCUtils.safeGetString(dbResult, "TYPE_NAME"), true);
    }

//    // Use by tree navigator thru reflection
//    public boolean hasMethods()
//    {
//        return methodCache != null;
//    }
//    // Use by tree navigator thru reflection
//    public boolean hasAttributes()
//    {
//        return attributeCache != null;
//    }

    private boolean findTypeDesc(String typeName)
    {
        if (typeName.startsWith("PL/SQL")) {
            // Don't care about PL/SQL types
            return true;
        }
        typeName = normalizeTypeName(typeName);
        this.typeDesc = PREDEFINED_TYPES.get(typeName);
        if (this.typeDesc == null) {
            log.warn("Unknown predefined type: " + typeName);
            return false;
        } else {
            this.valueType = this.typeDesc.valueType;
            return true;
        }
    }

    @Nullable
    public static DBPDataKind getDataKind(String typeName)
    {
        TypeDesc desc = PREDEFINED_TYPES.get(typeName);
        return desc != null ? desc.dataKind : null;
    }

    @Nullable
    @Override
    public XuguSchema getSchema()
    {
        return parent instanceof XuguSchema ? (XuguSchema)parent : null;
    }

    @Override
    public XuguSourceType getSourceType()
    {
        return XuguSourceType.TYPE;
    }

    @Override
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
    public DBEPersistAction[] getCompileActions()
    {
        return new DBEPersistAction[] {
            new XuguObjectPersistAction(
                XuguObjectType.VIEW,
                "Compile type",
                "ALTER TYPE " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
            )};
    }

    @Override
    public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        return sourceDefinition;
    }

    public void setExtendedDefinitionText(String source)
    {
        this.sourceDefinition = source;
    }

    @Override
    public String getTypeName()
    {
        return getFullyQualifiedName(DBPEvaluationContext.DDL);
    }

    @Override
    public String getFullTypeName() {
        return DBUtils.getFullTypeName(this);
    }

    @Override
    public int getTypeID()
    {
        return valueType;
    }

    @Override
    public DBPDataKind getDataKind()
    {
        return JDBCUtils.resolveDataKind(getDataSource(), getName(), valueType);
    }

    @Override
    public Integer getScale()
    {
        return typeDesc == null|| typeDesc.maxScale==0 ? null : typeDesc.maxScale;
    }

    @Override
    public Integer getPrecision()
    {
        return typeDesc == null ? 0 : typeDesc.precision;
    }

    @Override
    public long getMaxLength()
    {
        return CommonUtils.toInt(getPrecision());
    }

    public int getLength() {
    	return typeDesc.length;
    }
    
    @Override
    public int getMinScale()
    {
        return typeDesc == null ? 0 : typeDesc.minScale;
    }

    @Override
    public int getMaxScale()
    {
        return typeDesc == null ? 0 : typeDesc.maxScale;
    }

    @NotNull
    @Override
    public DBCLogicalOperator[] getSupportedOperators(DBSTypedObject attribute) {
        return DBUtils.getDefaultOperators(this);
    }

    @Override
    public DBSObject getParentObject()
    {
        return parent instanceof XuguSchema ?
            parent :
            parent instanceof XuguDataSource ? ((XuguDataSource) parent).getContainer() : null;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, editable = true, order = 2)
    public String getTypeCode()
    {
        return typeCode;
    }

    @Property(hidden = true, viewable = false, editable = false)
    public byte[] getTypeOID()
    {
        return typeOID;
    }

    @Property(viewable = true, editable = true, order = 3)
    public XuguDataType getSuperType(DBRProgressMonitor monitor)
    {
        if (superType  == null) {
            return null;
        } else if (superType instanceof XuguDataType) {
            return (XuguDataType)superType;
        } else {
        	return null;
        }
    }

    @Property(viewable = true, order = 4)
    public boolean isPredefined()
    {
        return flagPredefined;
    }

    @Property(viewable = true, order = 5)
    public boolean isIncomplete()
    {
        return flagIncomplete;
    }

    @Property(viewable = true, order = 6)
    public boolean isFinal()
    {
        return flagFinal;
    }

    @Property(viewable = true, order = 7)
    public boolean isInstantiable()
    {
        return flagInstantiable;
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType()
    {
        return DBSEntityType.TYPE;
    }

    @Override
    @Association
    public Collection<XuguDataTypeAttribute> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
//        return attributeCache != null ? attributeCache.getAllObjects(monitor, this) : null;
    	return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public XuguDataTypeAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException
    {
//        return attributeCache != null ? attributeCache.getObject(monitor, this, attributeName) : null;
    	return null;
    }

    @Nullable
    @Association
    public Collection<XuguDataTypeMethod> getMethods(DBRProgressMonitor monitor)
        throws DBException
    {
//        return methodCache != null ? methodCache.getAllObjects(monitor, this) : null;
    	return null;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Nullable
    @Override
    public Object geTypeExtension() {
        return typeOID;
    }

    @Override
    @Property(viewable = true, order = 8)
    public XuguDataType getComponentType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (componentType != null) {
            return componentType;
        }
        XuguSchema schema = getSchema();
        if (schema == null || !TYPE_CODE_COLLECTION.equals(typeCode)) {
            return null;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load collection types")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT ELEM_TYPE_OWNER,ELEM_TYPE_NAME,ELEM_TYPE_MOD FROM SYS.ALL_COLL_TYPES WHERE OWNER=? AND TYPE_NAME=?")) {
                dbStat.setString(1, schema.getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResults = dbStat.executeQuery()) {
                    if (dbResults.next()) {
                        String compTypeSchema = JDBCUtils.safeGetString(dbResults, "ELEM_TYPE_OWNER");
                        String compTypeName = JDBCUtils.safeGetString(dbResults, "ELEM_TYPE_NAME");
                        //String compTypeMod = JDBCUtils.safeGetString(dbResults, "ELEM_TYPE_MOD");
                        componentType = XuguDataType.resolveDataType(compTypeName);
                    } else {
                        log.warn("Can't resolve collection type [" + getName() + "]");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error reading collection types", e);
        }

        return componentType;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return parent instanceof XuguSchema ?
            DBUtils.getFullQualifiedName(getDataSource(), parent, this) :
            name;
    }

    @Override
    public String toString()
    {
        return getFullyQualifiedName(DBPEvaluationContext.UI);
    }

    public static XuguDataType resolveDataType(String typeName)
    {
        typeName = normalizeTypeName(typeName);
        XuguDataType type = null;
        PREDEFINED_TYPES.get(typeName);
        return type;
    }

    private static String normalizeTypeName(String typeName) {
        if (CommonUtils.isEmpty(typeName)) {
            return "";
        }
        for (;;) {
            int modIndex = typeName.indexOf('(');
            if (modIndex == -1) {
                break;
            }
            int modEnd = typeName.indexOf(')', modIndex);
            if (modEnd == -1) {
                break;
            }
            typeName = typeName.substring(0, modIndex) +
                (modEnd == typeName.length() - 1 ? "" : typeName.substring(modEnd + 1));
        }
        return typeName;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return DBSObjectState.NORMAL;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {

    }

    private class AttributeCache extends JDBCObjectCache<XuguDataType, XuguDataTypeAttribute> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguDataType owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM SYS.ALL_TYPE_ATTRS " +
                "WHERE OWNER=? AND TYPE_NAME=? ORDER BY ATTR_NO");
            dbStat.setString(1, XuguDataType.this.parent.getName());
            dbStat.setString(2, getName());
            return dbStat;
        }
        @Override
        protected XuguDataTypeAttribute fetchObject(@NotNull JDBCSession session, @NotNull XuguDataType owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguDataTypeAttribute(session.getProgressMonitor(), XuguDataType.this, resultSet);
        }
    }

    private class MethodCache extends JDBCObjectCache<XuguDataType, XuguDataTypeMethod> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguDataType owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT m.*,r.RESULT_TYPE_OWNER,RESULT_TYPE_NAME,RESULT_TYPE_MOD\n" +
                "FROM SYS.ALL_TYPE_METHODS m\n" +
                "LEFT OUTER JOIN SYS.ALL_METHOD_RESULTS r ON r.OWNER=m.OWNER AND r.TYPE_NAME=m.TYPE_NAME AND r.METHOD_NAME=m.METHOD_NAME AND r.METHOD_NO=m.METHOD_NO\n" +
                "WHERE m.OWNER=? AND m.TYPE_NAME=?\n" +
                "ORDER BY m.METHOD_NO");
            dbStat.setString(1, XuguDataType.this.parent.getName());
            dbStat.setString(2, getName());
            return dbStat;
        }

        @Override
        protected XuguDataTypeMethod fetchObject(@NotNull JDBCSession session, @NotNull XuguDataType owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguDataTypeMethod(session.getProgressMonitor(), XuguDataType.this, resultSet);
        }
    }

}
