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
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * @author Maple4Real
 *   存储过程衍生类，包括存储过程名、定义、参数等具体信息
 */
public class XuguProcedureStandalone extends XuguProcedureBase<XuguSchema> implements XuguSourceObject, DBPRefreshableObject
{

    private boolean valid;
    private String sourceDeclaration;
    private Collection<XuguProcedureArgument> procParams;
    public XuguProcedureStandalone(DBRProgressMonitor monitor,
        XuguSchema schema,
        ResultSet dbResult)
    {
        super(
            schema,
            JDBCUtils.safeGetString(dbResult, "PROC_NAME"),
            JDBCUtils.safeGetLong(dbResult, "PROC_ID"),
            DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, "RET_TYPE")==null?"PROCEDURE":"FUNCTION"));
        
        this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
        System.out.println(JDBCUtils.safeGetString(dbResult, "PROC_NAME"));
        //通过define字段手动解析参数列表（仅支持查看）
        this.sourceDeclaration = JDBCUtils.safeGetString(dbResult, "DEFINE");
        if(this.sourceDeclaration!=null) {
        	String reg = "";
            if(this.sourceDeclaration.toUpperCase().indexOf("IS")!=-1) {
            	reg = "IS";
            }else if(this.sourceDeclaration.toUpperCase().indexOf("AS")!=-1) {
            	reg = "AS";
            }else {
            	reg = null;
            }
            if(reg!=null) {
            	String head = this.sourceDeclaration.substring(0, this.sourceDeclaration.toUpperCase().indexOf(reg));
                if(head.indexOf("(")!=-1) {
                	String param = head.substring(head.indexOf("(")+1, head.indexOf(")"));
                    String[] params = param.split(",");
                    this.procParams = new ArrayList<XuguProcedureArgument>();
                    for(int i=0; i<params.length; i++) {
                    	params[i] = params[i].trim();
                    	String mode = "";
                    	if(params[i].toUpperCase().indexOf("IN OUT")!=-1) {
                    		mode = "IN OUT";
                    	}else if(params[i].toUpperCase().indexOf("IN")!=-1) {
                    		mode = "IN";
                    	}else if(params[i].toUpperCase().indexOf("OUT")!=-1) {
                    		mode = "OUT";
                    	}
                    	String[] duals = params[i].split(" ");
                    	procParams.add(new XuguProcedureArgument(monitor, this,duals[0], duals[duals.length-1], mode));
                    	System.out.println(JDBCUtils.safeGetString(dbResult, "PROC_NAME")+" proc params :"+duals[0]+" "+duals[duals.length-1]);
                    }
                }
            }
        }
    }
    
    public XuguProcedureStandalone(XuguSchema xuguSchema, String name, DBSProcedureType procedureType)
    {
        super(xuguSchema, name, 0L, procedureType);
        sourceDeclaration =
        	"CREATE OR REPLACE "+procedureType.name() + " " + name + GeneralUtils.getDefaultLineSeparator() +
            "IS" + GeneralUtils.getDefaultLineSeparator() +
            "BEGIN" + GeneralUtils.getDefaultLineSeparator() +
            "END " + name + ";" + GeneralUtils.getDefaultLineSeparator();
    }

    @Override
    public Collection<XuguProcedureArgument> getParameters(DBRProgressMonitor monitor) throws DBException
    {
        return this.procParams;
    }
    
    @Property(viewable = true, order = 3)
    public boolean isValid()
    {
        return valid;
    }

    public void setValid(boolean valid) {
    	this.valid = valid;
    }
    
    @Override
    public XuguSchema getSchema()
    {
        return getParentObject();
    }
    
    @Override
    public XuguSourceType getSourceType()
    {
        return getProcedureType() == DBSProcedureType.PROCEDURE ?
            XuguSourceType.PROCEDURE :
            XuguSourceType.FUNCTION;
    }

    @Override
    public Integer getOverloadNumber()
    {
        return null;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getSchema(),
            this);
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBCException
    {
        return sourceDeclaration;
    }

    @Override
    public void setObjectDefinitionText(String sourceDeclaration)
    {
        this.sourceDeclaration = sourceDeclaration;
    }

    @Override
    public DBEPersistAction[] getCompileActions()
    {
        return new DBEPersistAction[] {
            new XuguObjectPersistAction(
                getProcedureType() == DBSProcedureType.PROCEDURE ?
                    XuguObjectType.PROCEDURE : XuguObjectType.FUNCTION,
                "Compile procedure",
                "ALTER " + getSourceType().name() + " " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
            )};
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.valid = XuguUtils.getObjectStatus(monitor, this,
            getProcedureType() == DBSProcedureType.PROCEDURE ?
                    XuguObjectType.PROCEDURE : XuguObjectType.FUNCTION);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
    	getSchema().proceduresCache.clearCache();
    	return getSchema().proceduresCache.refreshObject(monitor, getSchema(), this);
    }
}
