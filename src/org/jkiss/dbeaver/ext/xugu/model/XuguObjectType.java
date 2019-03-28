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
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.util.HashMap;
import java.util.Map;

/**
 * Object type
 */
public enum XuguObjectType implements DBSObjectType {
//	USER("USER", null, XuguUser.class, null),
	TABLESPACE("TABLESPACE", null, XuguTablespace.class, null),
	ROLE("ROLE", null, XuguRole.class, null),
	CLUSTER("CLUSTER", null, DBSObject.class, null),
    CONSTRAINT ("CONSTRAINT", DBIcon.TREE_CONSTRAINT, XuguTableConstraint.class, null), // fake object
	CONSUMER_GROUP("CONSUMER GROUP", null, DBSObject.class, null),
	CONTEXT("CONTEXT", null, DBSObject.class, null),
	DIRECTORY("DIRECTORY", null, DBSObject.class, null),
	EVALUATION_CONTEXT("EVALUATION CONTEXT", null, DBSObject.class, null),
    FOREIGN_KEY ("FOREIGN KEY", DBIcon.TREE_FOREIGN_KEY, XuguTableForeignKey.class, null), // fake object
	FUNCTION("FUNCTION", DBIcon.TREE_PROCEDURE, XuguProcedureStandalone.class, new ObjectFinder() {
        @Override
        public XuguProcedureStandalone findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
        {
            return schema.proceduresCache.getObject(monitor, schema, objectName);
        }
    }),
	INDEX("INDEX", DBIcon.TREE_INDEX, XuguTableIndex.class, new ObjectFinder() {
        @Override
        public XuguTableIndex findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
        {
            return schema.indexCache.getObject(monitor, schema, objectName);
        }
    }),
	INDEX_PARTITION("INDEX PARTITION", null, DBSObject.class, null),
	INDEXTYPE("INDEXTYPE", null, DBSObject.class, null),
//	JAVA_CLASS("JAVA CLASS", DBIcon.TREE_JAVA_CLASS, XuguJavaClass.class, new ObjectFinder() {
//        @Override
//        public XuguJavaClass findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
//        {
//            return schema.javaCache.getObject(monitor, schema, objectName);
//        }
//    }),
	JAVA_DATA("JAVA DATA", null, DBSObject.class, null),
	JAVA_RESOURCE("JAVA RESOURCE", null, DBSObject.class, null),
	JOB("JOB", null, DBSObject.class, null),
	JOB_CLASS("JOB CLASS", null, DBSObject.class, null),
	LIBRARY("LIBRARY", null, DBSObject.class, null),
	LOB("CONTENT", null, DBSObject.class, null),
	MATERIALIZED_VIEW("MATERIALIZED VIEW", null, DBSObject.class, null),
	OPERATOR("OPERATOR", null, DBSObject.class, null),
	//package的obj_type为
	PACKAGE("18", DBIcon.TREE_PACKAGE, XuguPackage.class, new ObjectFinder() {
        @Override
        public XuguPackage findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
        {
            return schema.packageCache.getObject(monitor, schema, objectName);
        }
    }),
	//package body当作package处理
	PACKAGE_BODY("18", DBIcon.TREE_PACKAGE, XuguPackage.class, new ObjectFinder() {
        @Override
        public XuguPackage findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
        {
            return schema.packageCache.getObject(monitor, schema, objectName);
        }
    }),
	//procedure的obj_type为7
	PROCEDURE("7", DBIcon.TREE_PROCEDURE, XuguProcedureStandalone.class, new ObjectFinder() {
        @Override
        public XuguProcedureStandalone findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
        {
            return schema.proceduresCache.getObject(monitor, schema, objectName);
        }
    }),
	PROGRAM("PROGRAM", null, DBSObject.class, null),
//    QUEUE("QUEUE", null, XuguQueue.class, new ObjectFinder() {
//        @Override
//        public XuguQueue findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
//        {
//            return schema.queueCache.getObject(monitor, schema, objectName);
//        }
//    }),
	RULE("RULE", null, DBSObject.class, null),
	RULE_SET("RULE SET", null, DBSObject.class, null),
	SCHEDULE("SCHEDULE", null, DBSObject.class, null),
	//sequence的obj_type为8
	SEQUENCE("SEQUENCE", DBIcon.TREE_SEQUENCE, XuguSequence.class, new ObjectFinder() {
        @Override
        public XuguSequence findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
        {
            return schema.sequenceCache.getObject(monitor, schema, objectName);
        }
    }),
	SYNONYM("SYNONYM", DBIcon.TREE_SYNONYM, XuguSynonym.class, new ObjectFinder() {
        @Override
        public XuguSynonym findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
        {
            return schema.synonymCache.getObject(monitor, schema, objectName);
        }
    }),
	//table的OBJ_TYPE为5
	TABLE("5", DBIcon.TREE_TABLE, XuguTable.class, new ObjectFinder() {
        @Override
        public XuguTableBase findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
        {
            return schema.tableCache.getObject(monitor, schema, objectName);
        }
    }),
	TABLE_PARTITION("TABLE PARTITION", null, DBSObject.class, null),
	//trigger的OBJ_TYPE为11
	TRIGGER("11", DBIcon.TREE_TRIGGER, XuguTrigger.class, null),
	TYPE("TYPE", DBIcon.TREE_DATA_TYPE, XuguDataType.class, new ObjectFinder() {
        @Override
        public XuguDataType findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
        {
            return schema.dataTypeCache.getObject(monitor, schema, objectName);
        }
    }),
	TYPE_BODY("TYPE BODY", DBIcon.TREE_DATA_TYPE, XuguDataType.class, new ObjectFinder() {
        @Override
        public XuguDataType findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
        {
            return schema.dataTypeCache.getObject(monitor, schema, objectName);
        }
    }),
	//view的OBJ_TYPE为9
	VIEW("9", DBIcon.TREE_VIEW, XuguView.class, new ObjectFinder() {
        @Override
        public XuguView findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
        {
            return schema.tableCache.getObject(monitor, schema, objectName, XuguView.class);
        }
    }),
	WINDOW("WINDOW", null, DBSObject.class, null),
	WINDOW_GROUP("WINDOW GROUP", null, DBSObject.class, null),
	XML_SCHEMA("XML SCHEMA", null, DBSObject.class, null);
    
    private static final Log log = Log.getLog(XuguObjectType.class);

    private static Map<String, XuguObjectType> typeMap = new HashMap<>();

    static {
        for (XuguObjectType type : values()) {
            typeMap.put(type.getTypeName(), type);
        }
    }
    
    public static XuguObjectType getByType(String typeName)
    {
        return typeMap.get(typeName);
    }

    private static interface ObjectFinder {
        DBSObject findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException;
    }
    
    private final String objectType;
    private final DBPImage image;
    private final Class<? extends DBSObject> typeClass;
    private final ObjectFinder finder;

    <OBJECT_TYPE extends DBSObject> XuguObjectType(String objectType, DBPImage image, Class<OBJECT_TYPE> typeClass, ObjectFinder finder)
    {
        this.objectType = objectType;
        this.image = image;
        this.typeClass = typeClass;
        this.finder = finder;
    }

    public boolean isBrowsable()
    {
        return finder != null;
    }

    @Override
    public String getTypeName()
    {
        return objectType;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBPImage getImage()
    {
        return image;
    }

    @Override
    public Class<? extends DBSObject> getTypeClass()
    {
        return typeClass;
    }

    public DBSObject findObject(DBRProgressMonitor monitor, XuguSchema schema, String objectName) throws DBException
    {
        if (finder != null) {
            return finder.findObject(monitor, schema, objectName);
        } else {
            return null;
        }
    }

    public static Object resolveObject(
        DBRProgressMonitor monitor,
        XuguDataSource dataSource,
        String dbLink,
        String objectTypeName,
        String objectOwner,
        String objectName) throws DBException
    {
        if (dbLink != null) {
            return objectName;
        }
        XuguObjectType objectType = XuguObjectType.getByType(objectTypeName);
        if (objectType == null) {
            log.debug("Unrecognized Xugu object type: " + objectTypeName);
            return objectName;
        }
        if (!objectType.isBrowsable()) {
            log.debug("Unsupported Xugu object type: " + objectTypeName);
            return objectName;
        }
        final XuguSchema schema = dataSource.getSchema(monitor, objectOwner);
        if (schema == null) {
            log.debug("Schema '" + objectOwner + "' not found");
            return objectName;
        }
        final DBSObject object = objectType.findObject(monitor, schema, objectName);
        if (object == null) {
            log.debug(objectTypeName + " '" + objectName + "' not found in '" + schema.getName() + "'");
            return objectName;
        }
        return object;
    }

    @Override
    public String toString()
    {
        return objectType;
    }


}
