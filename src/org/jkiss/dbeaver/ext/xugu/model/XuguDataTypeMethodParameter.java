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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSParameter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.ResultSet;

/**
 * Xugu data type attribute
 */
public class XuguDataTypeMethodParameter implements DBSParameter {

    private final XuguDataTypeMethod method;
    private String name;
    private int number;
    private XuguParameterMode mode;
    private XuguDataType type;
    private XuguDataTypeModifier typeMod;

    public XuguDataTypeMethodParameter(DBRProgressMonitor monitor, XuguDataTypeMethod method, ResultSet dbResult)
    {
        this.method = method;
        this.name = JDBCUtils.safeGetString(dbResult, "PARAM_NAME");
        this.number = JDBCUtils.safeGetInt(dbResult, "PARAM_NO");
        this.mode = XuguParameterMode.getMode(JDBCUtils.safeGetString(dbResult, "PARAM_MODE"));
        this.type = XuguDataType.resolveDataType(JDBCUtils.safeGetString(dbResult, "PARAM_TYPE_NAME"));
        this.typeMod = XuguDataTypeModifier.resolveTypeModifier(
            JDBCUtils.safeGetString(dbResult, "PARAM_TYPE_MOD"));
    }

    @Override
    public DBSObject getParentObject()
    {
        return method;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return method.getDataSource();
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    public int getNumber()
    {
        return number;
    }

    @Property(viewable = true, order = 3)
    public XuguParameterMode getMode()
    {
        return mode;
    }

    @Property(id = "dataType", viewable = true, order = 4)
    public XuguDataType getType()
    {
        return type;
    }

    @Property(id = "dataTypeMod", viewable = true, order = 5)
    public XuguDataTypeModifier getTypeMod()
    {
        return typeMod;
    }

    @NotNull
    @Override
    public DBSTypedObject getParameterType() {
        return type;
    }
}
