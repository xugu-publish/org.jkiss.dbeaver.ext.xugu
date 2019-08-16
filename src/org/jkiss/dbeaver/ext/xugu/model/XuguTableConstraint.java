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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;

import java.sql.ResultSet;

/**
 * @author Maple4Real
 *   约束信息类，包含约束相关的基本信息
 */
public class XuguTableConstraint extends XuguTableConstraintBase {

    private static final Log log = Log.getLog(XuguTableConstraint.class);

    private String searchCondition;
    private String cons_name;
    private char match_type;
    private boolean deferrable;
    private boolean initdeferred;
    private String define;
    private char update_act;
    private char delete_act;
    private boolean is_sys;
    
    private int dbID;
    private int tableID;
    private int refTableID;
    private char consType;
    private boolean enable;
    private boolean valid;
    
    public XuguTableConstraint(XuguTableBase table, String name, DBSEntityConstraintType constraintType, String searchCondition, XuguObjectStatus status)
    {
        super(table, name, constraintType, status, false);
        this.searchCondition = searchCondition;
    }

    public XuguTableConstraint(XuguTableBase table, ResultSet dbResult)
    {
        super(
            table,
            JDBCUtils.safeGetString(dbResult, "CONS_NAME"),
            getConstraintType(JDBCUtils.safeGetString(dbResult, "CONS_TYPE")),
            JDBCUtils.safeGetBoolean(dbResult, "ENABLE") && JDBCUtils.safeGetBoolean(dbResult, "VALID")?XuguObjectStatus.ENABLED:XuguObjectStatus.DISABLED,
            true);
        if(dbResult!=null) {
//        	private int dbID;
//            private int tableID;
//            private int refTableID;
//            private char consType;
//            private boolean enable;
//            private boolean valid;
        	this.dbID = JDBCUtils.safeGetInt(dbResult, "DB_ID");
        	this.tableID = JDBCUtils.safeGetInt(dbResult, "TABLE_ID");
        	this.refTableID = JDBCUtils.safeGetInt(dbResult, "REF_TABLE_ID");
        	this.consType = JDBCUtils.safeGetString(dbResult, "CONS_TYPE").charAt(0);
        	this.enable = JDBCUtils.safeGetBoolean(dbResult, "ENABLE");
        	this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
        	this.cons_name = JDBCUtils.safeGetString(dbResult, "CONS_NAME");
            String s1 = JDBCUtils.safeGetString(dbResult, "MATCH_TYPE");
            if(s1!=null) {
            	this.match_type = s1.charAt(0);
            }
            this.deferrable = JDBCUtils.safeGetBoolean(dbResult, "DEFERRABLE");
            this.initdeferred = JDBCUtils.safeGetBoolean(dbResult, "INITDEFERRED");
            this.define = JDBCUtils.safeGetString(dbResult, "DEFINE");
            this.searchCondition = this.define;
            String s2 = JDBCUtils.safeGetString(dbResult, "UPDATE_ACTION");
            if(s2!=null) {
            	this.update_act = s2.charAt(0);
            }
            String s3 = JDBCUtils.safeGetString(dbResult, "DELETE_ACTION");
            if(s3!=null) {
            	this.delete_act = s3.charAt(0);
            }
            this.is_sys = JDBCUtils.safeGetBoolean(dbResult, "IS_SYS");
        }
    }
    
    @Property(viewable = true, editable = false, updatable=false, order = 3)
    public String getSearchCondition()
    {
        return this.searchCondition;
    }
    
    public String getConstraintName()
    {
        return this.cons_name;
    } 
    
    public char getMatchType() {
    	return this.match_type;
    }
    
    public boolean getDeferrable() {
    	return this.deferrable;
    }

    public boolean getInitDeferrable() {
    	return this.initdeferred;
    }
    
    public String getDefine() {
    	return this.define;
    }
    
    public char getUpdateAction() {
    	return this.update_act;
    }
    
    public char getDeleteAction() {
    	return this.delete_act;
    }
    
    public boolean getIsSys() {
    	return this.is_sys;
    }
    
    @Property(viewable = true, editable = true, updatable=true, order = 4)
    public boolean isEnable() {
    	return this.enable;
    }
    
    public void setEnable(boolean enable) {
    	this.enable = enable;
    }
    
    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            getTable(),
            this);
    }

    public static DBSEntityConstraintType getConstraintType(String code)
    {
        switch (code) {
	        case "C":
	        	return DBSEntityConstraintType.CHECK;
	        case "P":
	        	return DBSEntityConstraintType.PRIMARY_KEY;
	        case "U":
	        	return DBSEntityConstraintType.UNIQUE_KEY;
	        case "F":
	            return DBSEntityConstraintType.FOREIGN_KEY;
	        case "N":
	        	return DBSEntityConstraintType.NOT_NULL;
	        case "D":
	        	return XuguConstants.CONSTRAINT_DEFAULT;
	        case "R":
                return XuguConstants.CONSTRAINT_REF_COLUMN;
            default:
                log.debug("Unsupported Xugu constraint type: " + code);
                return DBSEntityConstraintType.CHECK;
        }
    }
}
