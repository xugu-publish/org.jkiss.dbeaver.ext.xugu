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

import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;

import java.sql.ResultSet;
import java.sql.Types;

/**
 * Oracle data type attribute
 */
public class XuguDataTypeAttribute extends XuguDataTypeMember implements DBSEntityAttribute, DBSTypedObjectEx
{

    private XuguDataType attrType;
    private XuguDataTypeModifier attrTypeMod;
    private Integer length;
    private Integer precision;
    private Integer scale;

    public XuguDataTypeAttribute(DBRProgressMonitor monitor, XuguDataType dataType, ResultSet dbResult)
    {
        super(dataType, dbResult);
        this.name = JDBCUtils.safeGetString(dbResult, "ATTR_NAME");
        this.number = JDBCUtils.safeGetInt(dbResult, "ATTR_NO");
        this.attrType = XuguDataType.resolveDataType(
            monitor,
            getDataSource(),
            JDBCUtils.safeGetString(dbResult, "ATTR_TYPE_OWNER"),
            JDBCUtils.safeGetString(dbResult, "ATTR_TYPE_NAME"));
        this.attrTypeMod = XuguDataTypeModifier.resolveTypeModifier(JDBCUtils.safeGetString(dbResult, "ATTR_TYPE_MOD"));
        this.length = JDBCUtils.safeGetInteger(dbResult, "LENGTH");
        this.precision = JDBCUtils.safeGetInteger(dbResult, "PRECISION");
        this.scale = JDBCUtils.safeGetInteger(dbResult, "SCALE");
    }

    @Property(viewable = true, editable = true, order = 3)
    public XuguDataType getDataType()
    {
        return attrType;
    }

    @Property(viewable = true, editable = true, order = 4)
    public XuguDataTypeModifier getAttrTypeMod()
    {
        return attrTypeMod;
    }

    @Override
    @Property(viewable = true, editable = true, order = 6)
    public Integer getPrecision()
    {
        return precision == null ? 0 : precision;
    }

    @Property(viewable = true, editable = true, order = 5)
    @Override
    public long getMaxLength()
    {
        return length == null ? 0 : length;
    }

    @Override
    @Property(viewable = true, editable = true, order = 7)
    public Integer getScale()
    {
        return scale == null ? 0 : scale;
    }

    @Override
    public int getTypeID()
    {
        if (attrTypeMod == XuguDataTypeModifier.REF) {
            // Explicitly say that we are reference
            return Types.REF;
        }
        return attrType.getTypeID();
    }

    @Override
    public DBPDataKind getDataKind()
    {
        if (attrTypeMod == XuguDataTypeModifier.REF) {
            // Explicitly say that we are reference
            return DBPDataKind.REFERENCE;
        }
        return attrType.getDataKind();
    }

    @Override
    public String getTypeName()
    {
        return attrType.getFullyQualifiedName(DBPEvaluationContext.DDL);
    }

    @Override
    public String getFullTypeName() {
        return DBUtils.getFullTypeName(this);
    }

    @Override
    public boolean isRequired()
    {
        return false;
    }

    @Override
    public boolean isAutoGenerated()
    {
        return false;
    }

    @Property(viewable = true, order = 2)
    @Override
    public int getOrdinalPosition()
    {
        // Number is 1 based
        return number - 1;
    }

    @Override
    public String getDefaultValue()
    {
        return null;
    }
}