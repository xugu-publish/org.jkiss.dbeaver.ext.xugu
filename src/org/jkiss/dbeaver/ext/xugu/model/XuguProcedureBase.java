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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.IntKeyMap;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

/**
 * GenericProcedure
 */
public abstract class XuguProcedureBase<PARENT extends DBSObjectContainer> extends XuguObject<PARENT> implements DBSProcedure
{
    static final Log log = Log.getLog(XuguProcedureBase.class);

    private DBSProcedureType procedureType;
    private final ArgumentsCache argumentsCache = new ArgumentsCache();

    public XuguProcedureBase(
        PARENT parent,
        String name,
        long objectId,
        DBSProcedureType procedureType)
    {
        super(parent, name, objectId, true);
        this.procedureType = procedureType;
    }

    @Override
    @Property(viewable = true, editable = true, order = 3)
    public DBSProcedureType getProcedureType()
    {
        return procedureType ;
    }

    @Override
    public DBSObjectContainer getContainer()
    {
        return getParentObject();
    }

    public abstract XuguSchema getSchema();

    public abstract Integer getOverloadNumber();

    @Override
    public Collection<XuguProcedureArgument> getParameters(DBRProgressMonitor monitor) throws DBException
    {
        return argumentsCache.getAllObjects(monitor, this);
    }

    static class ArgumentsCache extends JDBCObjectCache<XuguProcedureBase, XuguProcedureArgument> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguProcedureBase procedure) throws SQLException
        {
        	System.out.println("Select all arguments");
        	JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT DEFINE FROM ALL_PROCEDURES " +
                    "WHERE PROC_ID=" + procedure.getObjectId());
        	return dbStat;
        }

        @Override
        protected XuguProcedureArgument fetchObject(@NotNull JDBCSession session, @NotNull XuguProcedureBase procedure, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguProcedureArgument(session.getProgressMonitor(), procedure, resultSet);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, XuguProcedureBase owner, Iterator<XuguProcedureArgument> objectIter)
        {
            IntKeyMap<XuguProcedureArgument> argStack = new IntKeyMap<>();
            while (objectIter.hasNext()) {
                XuguProcedureArgument argument = objectIter.next();
                final int curDataLevel = argument.getDataLevel();
                argStack.put(curDataLevel, argument);
                if (curDataLevel > 0) {
                    objectIter.remove();
                    XuguProcedureArgument parentArgument = argStack.get(curDataLevel - 1);
                    if (parentArgument == null) {
                        log.error("Broken arguments structure for '" + argument.getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL) + "' - no parent argument for argument " + argument.getSequence());
                    } else {
                        parentArgument.addAttribute(argument);
                    }
                }
            }
        }

    }

}
