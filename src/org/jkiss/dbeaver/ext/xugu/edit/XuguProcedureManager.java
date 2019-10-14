/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.xugu.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.xugu.model.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateProcedurePage;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.xugu.XuguUtils;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author xugu-publish
 * 存储过程管理器
 * 进行存储过程的创建和删除（修改等同于创建并替换）
 */
public class XuguProcedureManager extends SQLObjectEditor<XuguProcedureStandalone, XuguSchema> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    protected void validateObjectProperties(ObjectChangeCommand command) throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Procedure name cannot be empty");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguProcedureStandalone> getObjectsCache(XuguProcedureStandalone object)
    {
        return object.getSchema().proceduresCache;
    }
    
    @Override
    protected XuguProcedureStandalone createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options)
    {
    	XuguProcedureStandalone procedure = new XuguProcedureStandalone(
            (XuguSchema) container,
            "",
            DBSProcedureType.PROCEDURE);
        return new UITask<XuguProcedureStandalone>() {
            @Override
            protected XuguProcedureStandalone runTask() {
                CreateProcedurePage editPage = new CreateProcedurePage(procedure);
                if (!editPage.edit()) {
                    return null;
                }
                
                StringBuilder desc = new StringBuilder(100);
                desc.append("CREATE OR REPLACE ");
                desc.append(editPage.getProcedureType());
                desc.append(" ");
                desc.append(editPage.getProcedureName());
                if(editPage.getProcedureType().equals(DBSProcedureType.PROCEDURE)) 
                {
                	desc.append(GeneralUtils.getDefaultLineSeparator());
                	desc.append("IS ");
                	desc.append(GeneralUtils.getDefaultLineSeparator());
                	desc.append("BEGIN ");
                } else {
                	desc.append(GeneralUtils.getDefaultLineSeparator());
                	desc.append("-- Return DataType --");
                	desc.append(GeneralUtils.getDefaultLineSeparator());
                	desc.append("RETURN ");
                	desc.append(GeneralUtils.getDefaultLineSeparator());
                	desc.append("AS ");
                	desc.append(GeneralUtils.getDefaultLineSeparator());
                	desc.append("-- Variable Declaration --");
                	desc.append(GeneralUtils.getDefaultLineSeparator());
                	desc.append("BEGIN ");
                }
                desc.append(GeneralUtils.getDefaultLineSeparator());
                desc.append("-- Procedure/Function body --");
                desc.append(GeneralUtils.getDefaultLineSeparator());
                desc.append("END ");
//                desc.append(editPage.getProcedureName());
                desc.append(";");
                
                procedure.setName(editPage.getProcedureName());
                procedure.setObjectDefinitionText(desc.toString());
                procedure.setValid(true);
                return procedure;
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand objectCreateCommand, Map<String, Object> options)
    {
        createOrReplaceProcedureQuery(actions, objectCreateCommand.getObject()); 
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
    	if (command.getProperty("objectDefinitionText") != null) 
    	{
    		createOrReplaceProcedureQuery(actionList, command.getObject());
    	}
    }

    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, NestedObjectCommand<XuguProcedureStandalone, PropertyHandler> command, Map<String, Object> options) {
        if (command.getProperty("comment") != null) {
        	StringBuilder desc = new StringBuilder(100);
        	desc.append("COMMENT ON PROCEDURE ");
        	desc.append(command.getObject().getName());
        	desc.append(" IS ");
        	desc.append(SQLUtils.quoteString(command.getObject(), command.getObject().getComment()));
        	
        	log.debug("[Xugu] Construct add procedure comment sql: " + desc.toString());
            actions.add(new SQLDatabasePersistAction("Comment Procedure", desc.toString()));
        }
    }
    
    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand objectDeleteCommand, Map<String, Object> options)
    {
        final XuguProcedureStandalone object = objectDeleteCommand.getObject();
        String sql = "DROP " + object.getProcedureType().name() + " " + object.getFullyQualifiedName(DBPEvaluationContext.DDL);
        
        log.debug("[Xugu] Construct drop procedure sql: "+sql);
        actions.add(new SQLDatabasePersistAction("Drop procedure",sql));
    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actionList, XuguProcedureStandalone procedure)
    {
        String source = XuguUtils.normalizeSourceName(procedure, false);
        if (source == null) {
            return;
        }
        //强制增加CREATE OR REPLACE关键字
//        String keyWord = XuguObjectType.PROCEDURE.getTypeName().equals(procedure.getProcedureType().toString())?XuguObjectType.PROCEDURE.getTypeName():XuguObjectType.FUNCTION.getTypeName();
//    	Pattern p = Pattern.compile("("+keyWord+")", Pattern.CASE_INSENSITIVE);
//        Matcher m = p.matcher(source);
//        if(m.find()) {
//        	keyWord = m.group(0);
//        }
//        int index = source.indexOf(keyWord);
//        source = "CREATE OR REPLACE " + keyWord.toUpperCase() + source.substring(index+keyWord.length());
        
        log.debug("[Xugu] Construct create procedure sql: "+source);
        actionList.add(new XuguObjectValidateAction(procedure, XuguObjectType.PROCEDURE, "Create procedure", source)); //$NON-NLS-2$
    }

}
