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
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * Xugu abstract partition
 */
public abstract class XuguPartitionBase<PARENT extends DBSObject> extends XuguObject<PARENT>
{
    public enum PartitionType {
        RANGE,
        HASH,
        LIST,
        AUTO
    }
    
    private String partiValue;
    private int partiNo;
    private boolean online;
    private String partiKey;
    private int partiType;
    private int autoPartiType;
    private int autoPartiSpan;
    private boolean isSubParti;
    private boolean isAuto;

    protected XuguPartitionBase(PARENT xuguTable,
	        boolean subpartition, String name) {
    	super(
    			xuguTable, name,
                true);
    	this.isSubParti = subpartition;
	}
    
    protected XuguPartitionBase(PARENT parent, boolean subpartition, ResultSet dbResult)
    {
        super(
            parent,
            subpartition ?
                JDBCUtils.safeGetString(dbResult, "SUBPARTI_NAME") :
                JDBCUtils.safeGetString(dbResult, "PARTI_NAME"),
            true);
        this.isSubParti = subpartition;
//        this.partiName = subpartition? JDBCUtils.safeGetString(dbResult, "SUBPARTI_NAME"):JDBCUtils.safeGetString(dbResult, "PARTI_NAME");
        this.partiValue = subpartition? JDBCUtils.safeGetString(dbResult, "SUBPARTI_VAL") : JDBCUtils.safeGetString(dbResult, "PARTI_VAL");
        this.partiNo = subpartition? JDBCUtils.safeGetInt(dbResult, "SUBPARTI_NO") : JDBCUtils.safeGetInt(dbResult, "PARTI_NO");
        this.online = subpartition? true : JDBCUtils.safeGetBoolean(dbResult, "ONLINE");
        this.partiKey = subpartition? JDBCUtils.safeGetString(dbResult, "SUBPARTI_KEY"):JDBCUtils.safeGetString(dbResult, "PARTI_KEY");
        this.partiType = subpartition? JDBCUtils.safeGetInt(dbResult, "SUBPARTI_TYPE"):JDBCUtils.safeGetInt(dbResult, "PARTI_TYPE");
        if(JDBCUtils.safeGetInteger(dbResult, "AUTO_PARTI_TYPE")!=null) {
        	this.isAuto = true;
        	this.autoPartiType = JDBCUtils.safeGetInt(dbResult, "AUTO_PARTI_TYPE");
        	this.autoPartiSpan = JDBCUtils.safeGetInt(dbResult, "AUTO_PARTI_SPAN");
        }else {
        	this.isAuto = false;
        }
    }
    
    @Property(viewable = true, order = 1, updatable = true, editable=true)
    public int getPartiNo()
    {
        return partiNo;
    }
    
    @Property(viewable=true, order=2, updatable=true, editable=true)
    public String getPartiType() {
    	switch(partiType) {
    	case 1:
    			return this.isAuto?"AUTOMATIC":"RANGE";
		case 2:
    			return "LIST";
		case 3:
    			return "HASH";
		default:
    			return partiType+"";
    	}
    }
    
    public void setPartiType(String partiType) {
    	switch(partiType) {
    	case "RANGE":
    		this.partiType = 1;
    		this.isAuto = false;
    		break;
    	case "LIST":
    		this.partiType = 2;
    		break;
    	case "HASH":
    		this.partiType = 3;
    		break;
    	case "AUTOMATIC":
    		this.partiType = 1;
    		this.isAuto = true;
    		break;
    	}
    }
    
    @Property(viewable=true, order=3, updatable=true, editable=true)
    public String getPartiKey() {
    	return partiKey;
    }
    
    public void setPartiKey(String partiKey) {
    	this.partiKey = partiKey;
    }
    
    @Property(viewable = true, order = 4, updatable=true, editable=true)
    public String getPartiValue()
    {
    	if(partiType==3)
    		return null;
        return partiValue;
    }

    public void setPartiValue(String value)
    {
    	//对于时间类型进行特殊处理（加入引号）
    	if(value.indexOf("-")!=-1 && value.indexOf("'")==-1) {
    		if(value.indexOf(",")==-1) {
    			this.partiValue = "'"+value+"'";
    		}else {
    			String[] values = value.split(",");
    			for(int i=0; i<values.length; i++) {
    				this.partiValue += "'"+values[i]+"'";
    				if(i!=values.length) {
        				this.partiValue += ",";
        			}
    			}
    		}
    		String[] values = value.split(",");
    	}else {
    		this.partiValue = value;
    	}
    }
    
    @Property(viewable = true, order = 5, updatable=true, editable=true)
    public String getAutoPartiType() {
    	switch(this.autoPartiType) {
    	case 1:
    		return "YEAR";
    	case 2:
    		return "MONTH";
    	case 3:
    		return "DAY";
    	case 4:
    		return "HOUR";
    	default:
    		return "NULL";	
    	}
    }
    
    public void setAutoPartiType(String type) {
    	switch(type) {
    	case "YEAR":
    		this.autoPartiType = 1;
    		break;
    	case "MONTH":
    		this.autoPartiType = 2;
    		break;
    	case "DAY":
    		this.autoPartiType = 3;
    		break;
    	case "HOUR":
    		this.autoPartiType = 4;
    		break;
    	default:
    		this.autoPartiType = 0;
    		break;
    	}
    }
    
    @Property(viewable = true, order = 6, updatable=true, editable=true)
    public Integer getAutoPartiSpan()
    {
    	if(!this.isAuto)
    		return null;
        return this.autoPartiSpan;
    }

    public void setAutoPartiSpan(int value)
    {
    	this.autoPartiSpan = value;
    }
    
    @Property(viewable = true, order = 5, updatable=true, editable=true)
    public boolean isOnline()
    {
        return online;
    }
    
    public void setOnline(boolean flag) {
    	this.online = flag;
    }
    
    public boolean isSubPartition() {
    	return isSubParti;
    }
    
    public void setSubPartition(boolean subFlag) {
    	this.isSubParti = subFlag;
    }
    
}
