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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.runtime.jobs.InvalidateJob;
import org.jkiss.utils.IntKeyMap;
import org.jkiss.utils.SecurityUtils;

import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Xugu execution plan analyser
 */
public class XuguPlanAnalyser implements DBCPlan {

    private static final Log log = Log.getLog(XuguPlanAnalyser.class);

    private XuguDataSource dataSource;
    private JDBCSession session;
    private String query;
    private List<XuguPlanNode> rootNodes;

    public XuguPlanAnalyser(XuguDataSource dataSource, JDBCSession session, String query)
    {
        this.dataSource = dataSource;
        this.session = session;
        this.query = query;
    }

    @Override
    public String getQueryString()
    {
        return query;
    }

    @Override
    public String getPlanQueryString() throws DBException {

        return "EXPLAIN "+query;
    }

    @Override
    public Collection<XuguPlanNode> getPlanNodes()
    {
        return rootNodes;
    }

    public void explain()
        throws DBException
    {
        String planQuery = getPlanQueryString();
        try {           
        	System.out.println("PPPPPPPPPPlan "+planQuery);
            // Explain plan
        	JDBCStatement dbStat = session.createStatement();

            // Read explained plan
            JDBCResultSet dbResult = dbStat.executeQuery(planQuery);
            rootNodes = new ArrayList<>();
//            IntKeyMap<XuguPlanNode> allNodes = new IntKeyMap<>();
            while (dbResult.next()) {
                XuguPlanNode node = new XuguPlanNode(dataSource, dbResult);
                rootNodes.add(node);
//                allNodes.put(node.getId(), node);
//                if (node.getParent() == null) {
//                    rootNodes.add(node);
//                }
            }
            dbStat.close();
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

}
