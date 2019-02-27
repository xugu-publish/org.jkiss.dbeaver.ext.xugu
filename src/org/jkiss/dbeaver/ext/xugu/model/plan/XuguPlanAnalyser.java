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
 * Oracle execution plan analyser
 */
public class XuguPlanAnalyser implements DBCPlan {

    private static final Log log = Log.getLog(XuguPlanAnalyser.class);

    private XuguDataSource dataSource;
    private JDBCSession session;
    private String query;
    private List<XuguPlanNode> rootNodes;
    private String planStmtId;
    private String planTableName;

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
        if (planTableName == null) {
            // Detect plan table
            planTableName = dataSource.getPlanTableName(session);
            if (planTableName == null) {
                throw new DBCException("Plan table not found - query can't be explained");
            }
        }

        if (planStmtId == null) {
            planStmtId = SecurityUtils.generateUniqueId();
        }

        return "EXPLAIN PLAN " + "\n" +
                "SET STATEMENT_ID = '" + planStmtId + "'\n" +
                "INTO " + planTableName + "\n" +
                "FOR " + query;
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

            // Delete previous statement rows
            // (actually there should be no statement with this id -
            // but let's do it, just in case)
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "DELETE FROM " + planTableName +
                " WHERE STATEMENT_ID=? ");
            try {
                dbStat.setString(1, planStmtId);
                dbStat.execute();
            } finally {
                dbStat.close();
            }

            // Explain plan
            dbStat = session.prepareStatement(planQuery);
            try {
                try {
                    // Bind parameters if any
                    ParameterMetaData parameterMetaData = dbStat.getParameterMetaData();
                    if (parameterMetaData != null && parameterMetaData.getParameterCount() > 0) {
                        for (int i = 0; i < parameterMetaData.getParameterCount(); i++) {
                            dbStat.setNull(i + 1, Types.VARCHAR);
                        }
                    }
                } catch (Exception e) {
                    log.error(e);
                }
                dbStat.execute();
            } finally {
                dbStat.close();
            }

            // Read explained plan
            dbStat = session.prepareStatement(
                "SELECT * FROM " + planTableName +
                " WHERE STATEMENT_ID=? ORDER BY ID");
            try {
                dbStat.setString(1, planStmtId);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    rootNodes = new ArrayList<>();
                    IntKeyMap<XuguPlanNode> allNodes = new IntKeyMap<>();
                    while (dbResult.next()) {
                        XuguPlanNode node = new XuguPlanNode(dataSource, allNodes, dbResult);
                        allNodes.put(node.getId(), node);
                        if (node.getParent() == null) {
                            rootNodes.add(node);
                        }
                    }
                }
            } finally {
                dbStat.close();
            }

        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

}
