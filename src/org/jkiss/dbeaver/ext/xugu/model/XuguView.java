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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.xml.validation.Schema;

/**
 * OracleView
 */
public class XuguView extends XuguTableBase implements XuguSourceObject
{
    private static final Log log = Log.getLog(XuguView.class);
    private String viewText;
    private Collection<XuguTableColumn> columns = new ArrayList<XuguTableColumn>();
    public XuguView(XuguSchema schema, String name)
    {
        super(schema, name, false);
    }

    public XuguView(DBRProgressMonitor monitor, JDBCSession session, XuguSchema schema, ResultSet dbResult)
    {
        super(schema, dbResult, 1);
        this.viewText = JDBCUtils.safeGetString(dbResult, "DEFINE");
        innerSetColumns(monitor, session, schema);
    }

    private XuguTable innerFetchTable(DBRProgressMonitor monitor, JDBCSession session, XuguSchema schema, String tableName) {
    	try {
	    	StringBuilder sql = new StringBuilder();
	    	sql.append("SELECT * FROM ");
	    	sql.append(getContainer().getRoleFlag());
	    	sql.append("_TABLES WHERE SCHEMA_ID=");
	    	sql.append(schema.getId());
	    	//当有检索条件时 只查询指定表 用于新建表之后的刷新工作
			sql.append(" AND TABLE_NAME = '");
			sql.append(tableName);
			sql.append("'");
    	
			final JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			ResultSet res = dbStat.executeQuery();
			XuguTable table = new XuguTable(monitor, schema, res);
			dbStat.close();
			return table;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
    
    private void innerFetchColumns(DBRProgressMonitor monitor, JDBCSession session, XuguTable table, String tableName, String colName) {
    	StringBuilder sql = new StringBuilder(500);
        sql.append("SELECT * FROM ");
    	sql.append(getContainer().getRoleFlag());
    	sql.append("_COLUMNS");
        sql.append(" where COL_NAME='");
        sql.append(colName);
        sql.append("' AND TABLE_ID=(SELECT TABLE_ID FROM ");
        sql.append(getContainer().getRoleFlag());
        sql.append("_TABLES WHERE TABLE_NAME='");
        sql.append(tableName);
        sql.append("')");
        try {
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			ResultSet set = dbStat.executeQuery();
			if(set!=null) {
    			//为了构造函数可以正常获取数据需要先遍历
    			while(set.next()) {
    				set.getInt(1);
    				set.getInt(2);
    				set.getInt(3);
    				set.getString(4);
    			}
    			XuguTableColumn column = new XuguTableColumn(monitor, table, set);
    			columns.add(column);
    		}
    		dbStat.close();
		} catch (SQLException | DBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void innerSetColumns(DBRProgressMonitor monitor, JDBCSession session, XuguSchema schema) {
    	String define = viewText.substring(viewText.toUpperCase().indexOf("SELECT")+6, viewText.toUpperCase().indexOf("FROM"));
    	String[] defines = define.split(",");
    	String nowTable = "";
    	XuguTable table = null;
    	for(int i=0; i<defines.length; i++) {
    		String tableName = defines[i].split("\\.")[0].trim();
    		tableName = tableName.substring(tableName.indexOf("\"")+1, tableName.lastIndexOf("\""));
    		if(i==0) {
    			nowTable = tableName;
    		}
    		String colName = defines[i].split("\\.")[1].trim();
    		colName = colName.substring(colName.indexOf("\"")+1, colName.lastIndexOf("\""));
    		//目标若尚未缓存则手动获取列信息
    		if(getContainer().tableCache.getCachedObject(tableName)==null) {
    			//对于表只需缓存一次结果集
    			if(nowTable!=tableName || i==0) {
    				table = innerFetchTable(monitor, session, schema, tableName);
    			}
    			innerFetchColumns(monitor, session, table, tableName, colName);
    		}
    		else {
    			try {
					columns.add(getContainer().tableCache.getCachedObject(tableName).getAttribute(monitor, colName));
				} catch (DBException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    }
    
    @Association
    public Collection<XuguTableColumn> getColumns(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
    	return columns;
    }
    
    @NotNull
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    @Override
    public String getName()
    {
        return super.getName();
    }

    @Override
    public boolean isView()
    {
        return true;
    }

    @Override
    public XuguSourceType getSourceType()
    {
        return XuguSourceType.VIEW;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        return viewText;
    }

    public void setObjectDefinitionText(String source)
    {
        this.viewText = source;
    }

    @Override
    protected String getTableTypeName()
    {
        return "VIEW";
    }
    
    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.valid = XuguUtils.getObjectStatus(monitor, this, XuguObjectType.VIEW);
    }

    public String getViewText() {
        return viewText;
    }

    public void setViewText(String viewText) {
        this.viewText = viewText;
    }

    @Override
    public DBEPersistAction[] getCompileActions()
    {
        return new DBEPersistAction[] {
            new XuguObjectPersistAction(
                XuguObjectType.VIEW,
                "Compile view",
                "ALTER VIEW " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
            )};
    }

}
