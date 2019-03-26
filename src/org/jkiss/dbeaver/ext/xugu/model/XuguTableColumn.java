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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * OracleTableColumn
 */
public class XuguTableColumn extends JDBCTableColumn<XuguTableBase> implements DBSTableColumn, DBSTypedObjectEx, DBPHiddenObject, DBPNamedObject2
{
    private static final Log log = Log.getLog(XuguTableColumn.class);

    private XuguDataType type;
    private XuguDataTypeModifier typeMod;
    private String comment;
    private boolean hidden;

    private int dbID;
    private int tableID;
    private boolean varying;
    private boolean isSerial;
    private int serialID;
    private String timeStamp_t;
    private String collator;
    private String colHistory;
    private int domainID;
    private boolean deleted;
    private boolean isVirtual;
    private String comments;
    private double repetRate;
    private double dispersion;
    private String maxVal;
    private String minVal;
    
    private int colNo;
    private String colName;
    private Integer scale;
    private String defVal;
    private boolean notNull;
    
    public XuguTableColumn(XuguTableBase table)
    {
        super(table, false);
    }

    public XuguTableColumn(
        DBRProgressMonitor monitor,
        XuguTableBase table,
        ResultSet dbResult)
        throws DBException
    {
        super(table, true);
        // Read default value first because it is of LONG type and has to be read before others
        //根据表和视图区分要获取的字段
        if(dbResult!=null) {
        	//type=0时为表 type=1时为视图
        	if(table.getType()==0) {
        		this.dbID = JDBCUtils.safeGetInt(dbResult, "DB_ID");
                this.tableID = JDBCUtils.safeGetInt(dbResult, "TABLE_ID");
                this.colNo = JDBCUtils.safeGetInt(dbResult, "COL_NO");
                this.colName = JDBCUtils.safeGetString(dbResult, "COL_NAME");
                this.typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
                this.varying = JDBCUtils.safeGetBoolean(dbResult, "VARYING");
                this.notNull = JDBCUtils.safeGetBoolean(dbResult, "NOT_NULL");
                this.isSerial = JDBCUtils.safeGetBoolean(dbResult, "IS_SERIAL");
                this.serialID = JDBCUtils.safeGetInt(dbResult, "SERIAL_ID");
                this.timeStamp_t = JDBCUtils.safeGetString(dbResult, "TIMESTAMP_T");
                this.collator = JDBCUtils.safeGetString(dbResult, "COLLATOR");
                this.defVal = JDBCUtils.safeGetString(dbResult, "DEF_VAL");
                this.domainID = JDBCUtils.safeGetInt(dbResult, "DOMAIN_ID");
                this.deleted = JDBCUtils.safeGetBoolean(dbResult, "DELETED");
                this.isVirtual = JDBCUtils.safeGetBoolean(dbResult, "IS_VIRTUAL");
                this.comments = JDBCUtils.safeGetString(dbResult, "COMMENTS");
                this.repetRate = JDBCUtils.safeGetDouble(dbResult, "REPET_RATE");
                this.dispersion = JDBCUtils.safeGetDouble(dbResult, "DISPERSION");
                this.maxVal = JDBCUtils.safeGetString(dbResult, "MAX_VAL");
                this.minVal = JDBCUtils.safeGetString(dbResult, "MIN_VAL");
                
                setDefaultValue(this.defVal);
                setName(this.colName);
                setOrdinalPosition(this.colNo);
        	}else {
        		this.dbID = JDBCUtils.safeGetInt(dbResult, "DB_ID");
                this.tableID = JDBCUtils.safeGetInt(dbResult, "VIEW_ID");
                this.colNo = JDBCUtils.safeGetInt(dbResult, "COL_NO");
                this.colName = JDBCUtils.safeGetString(dbResult, "COL_NAME");
                this.typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
                this.varying = JDBCUtils.safeGetBoolean(dbResult, "VARYING");
                setName(this.colName);
                setOrdinalPosition(this.colNo);
        	}
        	//对数据类型、精度标度进行统一处理
        	if("DATETIME".equals(this.typeName)) {
        		String t = JDBCUtils.safeGetString(dbResult, "TIMESTAMP_T");
        		if("n".equals(t)) {
        			this.typeName = "DATETIME";
        		}else {
        			this.typeName = "TIMESTAMP";
        		}
        	}
        	else if("CHAR".equals(this.typeName)){
        		boolean v = JDBCUtils.safeGetBoolean(dbResult, "VARYING");
        		if(v) {
        			this.typeName = "VARCHAR";
        		}else {
        			this.typeName = "CHAR";
        		}
        	}
        	this.type = new XuguDataType(this, this.typeName, true);
        	
            if (this.type != null) {
                this.typeName = type.getFullyQualifiedName(DBPEvaluationContext.DDL);
                this.valueType = type.getTypeID();
                this.maxLength = this.type.getLength();
                if("NUMERIC".equals(this.typeName)){
                	this.scale = JDBCUtils.safeGetInt(dbResult, "SCALE")%65536;
                	this.precision = (JDBCUtils.safeGetInt(dbResult, "SCALE")-this.scale)/65536;
                	this.maxLength = this.precision;
                }else if("CHAR".equals(this.typeName)||"VARCHAR".equals(this.typeName)) {
                	this.precision = JDBCUtils.safeGetInt(dbResult, "SCALE");
                	this.maxLength = this.precision;
                	this.scale = null;
                }else if(this.typeName.matches("INTERVAL(.*)")) {
                	this.scale = JDBCUtils.safeGetInt(dbResult, "SCALE")%65536;
                	this.precision = (JDBCUtils.safeGetInt(dbResult, "SCALE")-this.scale)/65536;
                }else {
                	this.precision = this.type.getPrecision();
                	this.scale = this.type.getMaxScale();
                }
            }
            if (typeMod == XuguDataTypeModifier.REF) {
                this.valueType = Types.REF;
            }
            setRequired(this.notNull);
            setScale(this.scale==null||this.scale==0?null:this.scale);
            setPrecision(this.precision==null||this.precision==0?null:this.precision);
            setMaxLength(this.maxLength);
        }
        
    }
	@NotNull
    @Override
    public XuguDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnDataTypeListProvider.class)
    public XuguDataType getDataType()
    {
        return type;
    }

    public void setDataType(XuguDataType type)
    {
        this.type = type;
        this.typeName = type == null ? "" : type.getFullyQualifiedName(DBPEvaluationContext.DDL);
    }

    @Property(viewable = true, order = 30)
    public XuguDataTypeModifier getTypeMod()
    {
        return typeMod;
    }

    //@Property(name = "Data Type", viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
    @Override
    public String getTypeName()
    {
        return super.getTypeName();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 40)
    @Override
    public long getMaxLength()
    {
        return super.getMaxLength();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 41)
    public Integer getPrecision()
    {
        return super.getPrecision();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 42)
    public Integer getScale()
    {
        return super.getScale();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 50)
    @Override
    public boolean isRequired()
    {
        return super.isRequired();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 70)
    @Override
    public String getDefaultValue()
    {
        return super.getDefaultValue();
    }

    @Override
    public boolean isAutoGenerated()
    {
        return false;
    }

    public static class CommentLoadValidator implements IPropertyCacheValidator<XuguTableColumn> {
        @Override
        public boolean isPropertyCached(XuguTableColumn object, Object propertyId)
        {
            return object.comment != null;
        }
    }

    @Property(viewable = true, editable = true, updatable = true, multiline = true, order = 100)
    @LazyProperty(cacheValidator = CommentLoadValidator.class)
    public String getComment(DBRProgressMonitor monitor)
    {
        if (comment == null) {
            // Load comments for all table columns
            getTable().loadColumnComments(monitor);
        }
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    void cacheComment() {
        if (this.comment == null) {
            this.comment = "";
        }
    }

    @Nullable
    @Override
    public String getDescription() {
        return comment;
    }

    @Override
    public boolean isHidden()
    {
        return hidden;
    }

    public static class ColumnDataTypeListProvider implements IPropertyValueListProvider<XuguTableColumn> {

        @Override
        public boolean allowCustomValue()
        {
            return false;
        }

        @Override
        public Object[] getPossibleValues(XuguTableColumn column)
        {
            List<DBSDataType> dataTypes = new ArrayList<>(column.getTable().getDataSource().getLocalDataTypes());
            if (!dataTypes.contains(column.getDataType())) {
            	XuguDataType t=column.getDataType();
            	if(t!=null) {
            		dataTypes.add(column.getDataType());
            	}
            }
            Collections.sort(dataTypes, DBUtils.nameComparator());
            return dataTypes.toArray(new DBSDataType[dataTypes.size()]);
        }
    }

}
