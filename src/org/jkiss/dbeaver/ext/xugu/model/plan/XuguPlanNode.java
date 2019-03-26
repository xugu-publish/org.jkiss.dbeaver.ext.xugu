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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
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
    
    public XuguPlanNode(XuguDataSource dataSource, ResultSet dbResult) {
    	this.dataSource = dataSource;
    	if(dbResult!=null) {
    		this.planPath = JDBCUtils.safeGetString(dbResult, "plan_path");
    		System.out.println("PPPPPPPPPath "+this.planPath);
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
    
    public String getPlanPath()
    {
        return planPath;
    }

}