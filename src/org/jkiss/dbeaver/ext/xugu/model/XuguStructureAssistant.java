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
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;

import java.sql.SQLException;
import java.util.*;

/**
 * XuguStructureAssistant
 */
public class XuguStructureAssistant implements DBSStructureAssistant
{
    static protected final Log log = Log.getLog(XuguStructureAssistant.class);

    private final XuguDataSource dataSource;

    public XuguStructureAssistant(XuguDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public DBSObjectType[] getSupportedObjectTypes()
    {
        return new DBSObjectType[] {
            XuguObjectType.TABLE,
            XuguObjectType.PACKAGE,
            XuguObjectType.CONSTRAINT,
            XuguObjectType.FOREIGN_KEY,
            XuguObjectType.INDEX,
            XuguObjectType.PROCEDURE,
            XuguObjectType.SEQUENCE,
            XuguObjectType.TRIGGER,
            };
    }

    @Override
    public DBSObjectType[] getHyperlinkObjectTypes()
    {
        return new DBSObjectType[] {
            XuguObjectType.TABLE,
            XuguObjectType.PACKAGE,
            XuguObjectType.PROCEDURE,
        };
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes()
    {
        return new DBSObjectType[] {
            XuguObjectType.TABLE,
            XuguObjectType.PACKAGE,
            XuguObjectType.PROCEDURE,
            };
    }

    @NotNull
    @Override
    public List<DBSObjectReference> findObjectsByMask(
        DBRProgressMonitor monitor,
        DBSObject parentObject,
        DBSObjectType[] objectTypes,
        String objectNameMask,
        boolean caseSensitive,
        boolean globalSearch, int maxResults)
        throws DBException
    {
        XuguSchema schema = parentObject instanceof XuguSchema ? (XuguSchema) parentObject : null;
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Find objects by name")) {
            List<DBSObjectReference> objects = new ArrayList<>();

            // Search all objects
            searchAllObjects(session, schema, objectNameMask, objectTypes, caseSensitive, maxResults, objects);

            if (ArrayUtils.contains(objectTypes, XuguObjectType.CONSTRAINT, XuguObjectType.FOREIGN_KEY) && objects.size() < maxResults) {
                // Search constraints
                findConstraintsByMask(session, schema, objectNameMask, objectTypes, maxResults, objects);
            }
            // Sort objects. Put ones in the current schema first
            final XuguSchema activeSchema = dataSource.getDefaultObject();
            objects.sort((o1, o2) -> {
                if (CommonUtils.equalObjects(o1.getContainer(), o2.getContainer())) {
                    return o1.getName().compareTo(o2.getName());
                }
                if (o1.getContainer() == null || o1.getContainer() == activeSchema) {
                    return -1;
                }
                if (o2.getContainer() == null || o2.getContainer() == activeSchema) {
                    return 1;
                }
                return o1.getContainer().getName().compareTo(o2.getContainer().getName());
            });

            return objects;
        }
        catch (SQLException ex) {
            throw new DBException(ex, dataSource);
        }
    }

    private void findConstraintsByMask(
        JDBCSession session,
        final XuguSchema schema,
        String constrNameMask,
        DBSObjectType[] objectTypes,
        int maxResults,
        List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        List<DBSObjectType> objectTypesList = Arrays.asList(objectTypes);
        final boolean hasFK = objectTypesList.contains(XuguObjectType.FOREIGN_KEY);
        final boolean hasConstraints = objectTypesList.contains(XuguObjectType.CONSTRAINT);

        // Load tables
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT " + XuguUtils.getSysCatalogHint((XuguDataSource) session.getDataSource()) + " OWNER, TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE\n" +
                "FROM SYS.ALL_CONSTRAINTS\n" +
                "WHERE CONSTRAINT_NAME like ?" + (!hasFK ? " AND CONSTRAINT_TYPE<>'R'" : "") +
                (schema != null ? " AND OWNER=?" : ""))) {
            dbStat.setString(1, constrNameMask);
            if (schema != null) {
                dbStat.setString(2, schema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final String schemaName = JDBCUtils.safeGetString(dbResult, XuguConstants.COL_OWNER);
                    final String tableName = JDBCUtils.safeGetString(dbResult, XuguConstants.COL_TABLE_NAME);
                    final String constrName = JDBCUtils.safeGetString(dbResult, XuguConstants.COL_CONSTRAINT_NAME);
                    final String constrType = JDBCUtils.safeGetString(dbResult, XuguConstants.COL_CONSTRAINT_TYPE);
                    final DBSEntityConstraintType type = XuguTableConstraint.getConstraintType(constrType);
                    objects.add(new AbstractObjectReference(
                        constrName,
                        dataSource.getSchema(session.getProgressMonitor(), schemaName),
                        null,
                        type == DBSEntityConstraintType.FOREIGN_KEY ? XuguTableForeignKey.class : XuguTableConstraint.class,
                        type == DBSEntityConstraintType.FOREIGN_KEY ? XuguObjectType.FOREIGN_KEY : XuguObjectType.CONSTRAINT) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            XuguSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
                            if (tableSchema == null) {
                                throw new DBException("Constraint schema '" + schemaName + "' not found");
                            }
                            XuguTable table = tableSchema.getTable(monitor, tableName);
                            if (table == null) {
                                throw new DBException("Constraint table '" + tableName + "' not found in catalog '" + tableSchema.getName() + "'");
                            }
                            DBSObject constraint = null;
                            if (hasFK && type == DBSEntityConstraintType.FOREIGN_KEY) {
                                constraint = table.getForeignKey(monitor, constrName);
                            }
                            if (hasConstraints && type != DBSEntityConstraintType.FOREIGN_KEY) {
                                constraint = table.getConstraint(monitor, constrName);
                            }
                            if (constraint == null) {
                                throw new DBException("Constraint '" + constrName + "' not found in table '" + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                            }
                            return constraint;
                        }
                    });
                }
            }
        }
    }

    private void searchAllObjects(final JDBCSession session, final XuguSchema schema, String objectNameMask, DBSObjectType[] objectTypes, boolean caseSensitive, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        StringBuilder objectTypeClause = new StringBuilder(100);
        final List<XuguObjectType> oracleObjectTypes = new ArrayList<>(objectTypes.length + 2);
        for (DBSObjectType objectType : objectTypes) {
            if (objectType instanceof XuguObjectType) {
                oracleObjectTypes.add((XuguObjectType) objectType);
                if (objectType == XuguObjectType.PROCEDURE) {
                    oracleObjectTypes.add(XuguObjectType.FUNCTION);
                } else if (objectType == XuguObjectType.TABLE) {
                    oracleObjectTypes.add(XuguObjectType.VIEW);
                    oracleObjectTypes.add(XuguObjectType.MATERIALIZED_VIEW);
                }
            } else if (DBSProcedure.class.isAssignableFrom(objectType.getTypeClass())) {
                oracleObjectTypes.add(XuguObjectType.FUNCTION);
            }
        }
        for (XuguObjectType objectType : oracleObjectTypes) {
            if (objectTypeClause.length() > 0) {
            	objectTypeClause.append(",");
            }
            objectTypeClause.append("'").append(objectType.getTypeName()).append("'");
        }
        if (objectTypeClause.length() == 0) {
            return;
        }
        // Always search for synonyms
        objectTypeClause.append(",'").append(XuguObjectType.SYNONYM.getTypeName()).append("'");

        // Seek for objects (join with public synonyms)
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT " + XuguUtils.getSysCatalogHint((XuguDataSource) session.getDataSource()) + " DISTINCT OWNER,OBJECT_NAME,OBJECT_TYPE FROM (SELECT OWNER,OBJECT_NAME,OBJECT_TYPE FROM ALL_OBJECTS WHERE " +
                "OBJECT_TYPE IN (" + objectTypeClause + ") AND OBJECT_NAME LIKE ? " +
                (schema == null ? "" : " AND OWNER=?") +
                "UNION ALL\n" +
            "SELECT " + XuguUtils.getSysCatalogHint((XuguDataSource) session.getDataSource()) + " O.OWNER,O.OBJECT_NAME,O.OBJECT_TYPE\n" +
                "FROM ALL_SYNONYMS S,ALL_OBJECTS O\n" +
                "WHERE O.OWNER=S.TABLE_OWNER AND O.OBJECT_NAME=S.TABLE_NAME AND S.OWNER='PUBLIC' AND S.SYNONYM_NAME LIKE ?)" +
                "\nORDER BY OBJECT_NAME")) {
            if (!caseSensitive) {
                objectNameMask = objectNameMask.toUpperCase();
            }
            dbStat.setString(1, objectNameMask);
            if (schema != null) {
                dbStat.setString(2, schema.getName());
            }
            dbStat.setString(schema != null ? 3 : 2, objectNameMask);
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (objects.size() < maxResults && dbResult.next()) {
                    if (session.getProgressMonitor().isCanceled()) {
                        break;
                    }
                    final String schemaName = JDBCUtils.safeGetString(dbResult, "OWNER");
                    final String objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
                    final String objectTypeName = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
                    final XuguObjectType objectType = XuguObjectType.getByType(objectTypeName);
                    if (objectType != null && objectType != XuguObjectType.SYNONYM && objectType.isBrowsable() && oracleObjectTypes.contains(objectType)) {
                        XuguSchema objectSchema = dataSource.getSchema(session.getProgressMonitor(), schemaName);
                        if (objectSchema == null) {
                            log.debug("Schema '" + schemaName + "' not found. Probably was filtered");
                            continue;
                        }
                        objects.add(new AbstractObjectReference(objectName, objectSchema, null, objectType.getTypeClass(), objectType) {
                            @Override
                            public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                                XuguSchema tableSchema = (XuguSchema) getContainer();
                                DBSObject object = objectType.findObject(session.getProgressMonitor(), tableSchema, objectName);
                                if (object == null) {
                                    throw new DBException(objectTypeName + " '" + objectName + "' not found in schema '" + tableSchema.getName() + "'");
                                }
                                return object;
                            }
                        });
                    }
                }
            }
        }
    }


}
