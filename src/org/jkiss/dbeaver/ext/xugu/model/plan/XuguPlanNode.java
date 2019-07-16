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

package org.jkiss.dbeaver.ext.xugu.model.plan;



import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;

import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNodeKind;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import org.jkiss.dbeaver.model.meta.Property;

import org.jkiss.utils.IntKeyMap;



import java.sql.ResultSet;

import java.sql.SQLException;

import java.util.ArrayList;

import java.util.Collection;

import java.util.List;



/**

 * Xugu execution plan node

 */

public class XuguPlanNode implements DBCPlanNode {
	
	private final XuguDataSource dataSource;
    private XuguPlanNode parent;
    private List<XuguPlanNode> nested;
    private String planPath;
    private String objectName;

    public XuguPlanNode(XuguDataSource dataSource, ResultSet dbResult) {
    	this.dataSource = dataSource;
    	if(dbResult!=null) {
    		this.planPath = JDBCUtils.safeGetString(dbResult, "plan_path");
    		this.objectName = "plan path";
    		this.parent = null;
    		this.nested = null;
    	}
    }

    public XuguPlanNode(XuguDataSource dataSource, IntKeyMap<XuguPlanNode> prevNodes, ResultSet dbResult) throws SQLException
    {
        this.dataSource = dataSource;
        Integer parent_id = JDBCUtils.safeGetInteger(dbResult, "parent_id");
        if (parent_id != null) {
            parent = prevNodes.get(parent_id);
        }
        if (parent != null) {
            if (parent.nested == null) {
                parent.nested = new ArrayList<>();
            }
            parent.nested.add(this);
        }
    }

    @Override
    public XuguPlanNode getParent()
    {
        return parent;
    }

    @Override
    public Collection<XuguPlanNode> getNested()
    {
        return nested;
    }
    
    @Property(name="path", order=0, viewable=true, description="plan path")
    public String getPlanPath()
    {
        return planPath;
    }

	@Override
	public DBCPlanNodeKind getNodeKind() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNodeName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNodeType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNodeCondition() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNodeDescription() {
		// TODO Auto-generated method stub
		return null;
	}
}
