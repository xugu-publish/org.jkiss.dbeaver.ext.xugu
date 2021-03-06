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
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * XuguProcedureArgument
 */
public class XuguProcedureParameter implements DBSProcedureParameter, DBSTypedObject
{
    private final XuguProcedureBase procedure;
    private String name;
    private int position;
    private int dataLevel;
    private int sequence;
    private XuguParameterMode mode;
    private XuguDataType type;
    private XuguDataType dataType;
    private String packageTypeName;
    private int dataLength;
    private int dataScale;
    private int dataPrecision;
    private String define;
    private List<XuguProcedureParameter> attributes;

    public XuguProcedureParameter(DBRProgressMonitor monitor, XuguProcedureBase procedure, String name, String datatype, String mode) {
    	this.procedure = procedure;
    	this.name = name;
    	//对int做转化
    	if("INT".equals(datatype.toUpperCase())) {
    		datatype = "INTEGER";
    	}
    	this.dataType = new XuguDataType(this, datatype.toUpperCase(), true);
    	this.dataScale = this.dataType.getMaxScale();
    	this.dataPrecision = this.dataType.getPrecision();
    	this.mode = XuguParameterMode.getMode(mode);
    }
    
    public XuguProcedureParameter(
        DBRProgressMonitor monitor,
        XuguProcedureBase procedure,
        ResultSet dbResult)
    {
        this.procedure = procedure;
        if(dbResult!=null) {
        	this.define = JDBCUtils.safeGetString(dbResult, "DEFINE");
        }
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @NotNull
    @Override
    public XuguDataSource getDataSource()
    {
        return procedure.getDataSource();
    }

    @Override
    public XuguProcedureBase getParentObject()
    {
        return procedure;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 10)
    public String getName()
    {
        if (CommonUtils.isEmpty(name)) {
            if (dataLevel == 0) {
                // Function result
                return "RESULT";
            } else {
                // Collection element
                return "ELEMENT";
            }
        }
        return name;
    }

    public boolean isResultArgument() {
        return CommonUtils.isEmpty(name) && dataLevel == 0;
    }

    @Property(viewable = true, order = 11)
    public int getPosition()
    {
        return position;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 20)
    public DBSProcedureParameterKind getParameterKind()
    {
        return mode == null ? DBSProcedureParameterKind.UNKNOWN : mode.getParameterKind();
    }

    @Property(viewable = true, order = 21)
    public Object getType()
    {
        return packageTypeName != null ?
            packageTypeName :
            dataType == null ? type : dataType;
    }

    @Override
    @Property(viewable = true, order = 30)
    public long getMaxLength()
    {
        return dataLength;
    }

    @Override
    public String getTypeName()
    {
        return type == null ? packageTypeName : type.getName();
    }

    @Override
    public String getFullTypeName() {
        return DBUtils.getFullTypeName(this);
    }

    @Override
    public int getTypeID()
    {
        return type == null ? 0 : type.getTypeID();
    }

    @Override
    public DBPDataKind getDataKind()
    {
        return type == null ? DBPDataKind.OBJECT : type.getDataKind();
    }

    @Override
    @Property(viewable = true, order = 40)
    public Integer getScale()
    {
        return dataScale;
    }

    @Override
    @Property(viewable = true, order = 50)
    public Integer getPrecision()
    {
        return dataPrecision;
    }

    public int getDataLevel()
    {
        return dataLevel;
    }

    public int getSequence()
    {
        return sequence;
    }

    @Association
    public Collection<XuguProcedureParameter> getAttributes()
    {
        return attributes;
    }

    void addAttribute(XuguProcedureParameter attribute)
    {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }
        attributes.add(attribute);
    }

    public boolean hasAttributes()
    {
        return !CommonUtils.isEmpty(attributes);
    }

    @NotNull
    @Override
    public DBSTypedObject getParameterType() {
        return this;
    }
}
