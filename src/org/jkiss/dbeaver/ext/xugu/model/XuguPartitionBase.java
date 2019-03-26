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
 * Oracle abstract partition
 */
public abstract class XuguPartitionBase<PARENT extends DBSObject> extends XuguObject<PARENT>
{
    public enum PartitionType {
        RANGE,
        HASH,
        LIST,
    }
    
    private String partiName;
    private String partiValue;
    private int partiNo;
    private boolean online;
    private String partiKey;
    private int partiType;

    protected XuguPartitionBase(PARENT parent, boolean subpartition, ResultSet dbResult)
    {
        super(
            parent,
            subpartition ?
                JDBCUtils.safeGetString(dbResult, "SUBPARTI_NAME") :
                JDBCUtils.safeGetString(dbResult, "PARTI_NAME"),
            true);
        this.partiName = subpartition? JDBCUtils.safeGetString(dbResult, "SUBPARTI_NAME"):JDBCUtils.safeGetString(dbResult, "PARTI_NAME");
        this.partiValue = subpartition? JDBCUtils.safeGetString(dbResult, "SUBPARTI_VAL") : JDBCUtils.safeGetString(dbResult, "PARTI_VAL");
        this.partiNo = subpartition? JDBCUtils.safeGetInt(dbResult, "SUBPARTI_NO") : JDBCUtils.safeGetInt(dbResult, "PARTI_NO");
        this.online = subpartition? true : JDBCUtils.safeGetBoolean(dbResult, "ONLINE");
        this.partiKey = JDBCUtils.safeGetString(dbResult, "PARTI_KEY");
        this.partiType = JDBCUtils.safeGetInt(dbResult, "PARTI_TYPE");
    }
    @Property(viewable=true, order=0, name="Name")
    public String getName() {
    	return partiName;
    }
    
    @Property(viewable = true, order = 1, name="No.")
    public int getPartiNo()
    {
        return partiNo;
    }
    
    @Property(viewable=true, order=2, name="Type")
    public String getPartiType() {
    	switch(partiType) {
    	case 1:
    			return "Range";
		case 2:
    			return "List";
		case 3:
    			return "Hash";
		default:
    			return partiType+"";
    	}
    }
    
    @Property(viewable=true, order=3, name="Keys")
    public String getPartiKey() {
    	return partiKey;
    }
    
    @Property(viewable = true, order = 4, name="Value")
    public String getPartiVal()
    {
    	if(partiType==3)
    		return null;
        return partiValue;
    }

    @Property(viewable = true, order = 5, name="Online")
    public boolean isOnline()
    {
        return online;
    }
}
