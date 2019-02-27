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
package org.jkiss.dbeaver.ext.xugu.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.xugu.model.XuguConstants;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * BFILE type support
 */
public class XuguBFILEValueHandler extends JDBCContentValueHandler {

    public static final XuguBFILEValueHandler INSTANCE = new XuguBFILEValueHandler();

    @Override
    protected DBDContent fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException
    {
        Object object;

        try {
            object = resultSet.getObject(index);
        } catch (SQLException e) {
            object = null;
        }

        if (object == null) {
            return new XuguContentBFILE(session.getDataSource(), null);
        } else {
            return new XuguContentBFILE(session.getDataSource(), object);
        }
    }

    @Override
    public DBDContent getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return new XuguContentBFILE(session.getDataSource(), null);
        } else if (object instanceof XuguContentBFILE) {
            return copy ? (XuguContentBFILE)((XuguContentBFILE) object).cloneValue(session.getProgressMonitor()) : (XuguContentBFILE) object;
        }
        return super.getValueFromObject(session, type, object, copy);
    }

}
