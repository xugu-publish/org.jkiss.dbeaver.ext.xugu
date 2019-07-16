
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
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.runtime.jobs.InvalidateJob;
import org.jkiss.utils.IntKeyMap;
import org.jkiss.utils.SecurityUtils;
import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    
    public Collection<XuguPlanNode> getPlanNodes()
    {
        return rootNodes;
    }
    
    public void explain()
        throws DBException
    {
        String planQuery = getPlanQueryString();
        try {           
            // Explain plan
        	JDBCStatement dbStat = session.createStatement();
            // Read explained plan
            JDBCResultSet dbResult = dbStat.executeQuery(planQuery);
            rootNodes = new ArrayList<>();
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

	@Override
	public Object getPlanFeature(String feature) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends DBCPlanNode> getPlanNodes(Map<String, Object> options) {
		// TODO Auto-generated method stub
		return null;
	}
}