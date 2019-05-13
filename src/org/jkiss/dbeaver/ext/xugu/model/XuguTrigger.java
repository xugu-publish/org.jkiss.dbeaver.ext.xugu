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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * XuguTrigger
 */
public class XuguTrigger extends XuguTriggerBase<XuguTableBase>
{
    private static final Log log = Log.getLog(XuguTrigger.class);

    private XuguSchema ownerSchema;
    private List<String> includeCols;
    
    public XuguTrigger(XuguTableBase table, String name)
    {
        super(table, name);
        this.ownerSchema = table.getSchema();
        includeCols = new ArrayList<String>();
    }

    public XuguTrigger(
        XuguTableBase table,
        ResultSet dbResult)
    {
        super(table, dbResult);
        this.ownerSchema = table.getSchema();
        includeCols = new ArrayList<String>();
    }

    @Property(viewable = true, order = 3)
    public String getObjName()
    {
        return parent.getName();
    }
    
    @Override
    @Property(viewable = true, order = 4)
    public XuguTableBase getTable()
    {
        return parent;
    }

    @Override
    public XuguSchema getSchema() {
        return this.ownerSchema;
    }

    @Association
    public Collection<XuguTriggerColumn> getColumns(DBRProgressMonitor monitor) throws DBException
    {
    	Collection<XuguTriggerColumn> res = new ArrayList<>();
    	Collection<XuguTableColumn> tCols = parent.getAttributes(monitor);
    	if(this.includeCols!=null) {
    		if(this.includeCols.size()!=0) {
    			Iterator<XuguTableColumn> it = tCols.iterator();
    			while(it.hasNext()) {
    				XuguTableColumn tempCol = it.next();
    				if(includeCols.contains(tempCol.getName())) {
    					res.add(new XuguTriggerColumn(tempCol.getName(), this, tempCol));
    				}
    			}
    		}else {
    			Iterator<XuguTableColumn> it = tCols.iterator();
    			while(it.hasNext()) {
    				XuguTableColumn tempCol = it.next();
    				res.add(new XuguTriggerColumn(tempCol.getName(), this, tempCol));
    			}
    		}
    		return res;
    	}else {
    		return parent.triggerCache.getChildren(monitor, parent, this);
    	}
    }
    
    public List<String> getIncludeColumns(){
    	return includeCols;
    }
    
    public void setIncludeColumns(List<String> cols) {
    	this.includeCols = cols;
    }
    
	@Override
	public void setObjectDefinitionText(String source) {
		super.setObjectDefinitionText(source);
	}

}
