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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.edit.XuguSynonymManager.NewSynonymDialog;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataType;
import org.jkiss.dbeaver.ext.xugu.model.XuguSynonym;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableBase;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableColumn;
import org.jkiss.dbeaver.ext.xugu.views.XuguWarningDialog;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Xugu table column manager
 */
public class XuguTableColumnManager extends SQLTableColumnManager<XuguTableColumn, XuguTableBase> implements DBEObjectRenamer<XuguTableColumn> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguTableColumn> getObjectsCache(XuguTableColumn object)
    {
        return object.getParentObject().getContainer().tableCache.getChildrenCache(object.getParentObject());
    }

    @Override
    protected ColumnModifier[] getSupportedModifiers(XuguTableColumn column, Map<String, Object> options)
    {
        return new ColumnModifier[] {DataTypeModifier, DefaultModifier, NullNotNullModifierConditional};
    }

    @Override
    protected XuguTableColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, XuguTableBase parent, Object copyFrom)
    {
        DBSDataType columnType = findBestDataType(parent.getDataSource(), "varchar2"); //$NON-NLS-1$

        final XuguTableColumn column = new XuguTableColumn(parent);
        column.setName(getNewColumnName(monitor, context, parent));
        column.setDataType((XuguDataType) columnType);
        column.setTypeName(columnType == null ? "INTEGER" : columnType.getName()); //$NON-NLS-1$
        column.setMaxLength(columnType != null && columnType.getDataKind() == DBPDataKind.STRING ? 100 : 0);
        column.setValueType(columnType == null ? Types.INTEGER : columnType.getTypeID());
        column.setOrdinalPosition(-1);
        return column;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
    	 final XuguTableBase table = command.getObject().getTable();
    	 String query = "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " ADD "  + getNestedDeclaration(monitor, table, command, options);
         if(command.getProperty("comment")!=null && !"".equals(command.getProperty("comment"))) {
        	 query += " comment '"+command.getObject().getComment(monitor)+"'";
         }
    	 actions.add(
             new SQLDatabasePersistAction(
                 ModelMessages.model_jdbc_create_new_table_column,
                 query ));
    }
    
    //修改了表结构修改的sql语句
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        final XuguTableColumn column = command.getObject();
        boolean hasComment = command.getProperty("comment") != null;
        //遍历properties 确定每一项新更改均不为空才进行action添加
        boolean flag = true;
        if (!hasComment && command.getProperties().size() > 0) {
        	Map<Object, Object> props = command.getProperties();
        	Collection<Object> propValues = props.values();
        	Iterator<Object> it = propValues.iterator();
        	while(it.hasNext()) {
        		if(it.next()==null || "".equals(it.toString())) {
        			flag = false;
        			break;
        		}
        	}
        	if(flag) {
        		String sql = "ALTER TABLE "+column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL)+" ALTER COLUMN";
        		//修改仅包含默认值做特殊处理(加上set关键字)
            	if(command.getProperty("defaultValue")!=null) {
            		actionList.add(new SQLDatabasePersistAction(
                            "Modify column",
                            sql+ column.getName() +" SET DEFAULT '" + command.getProperty("defaultValue")+"'" )); //$NON-NLS-1$
            	}
            	//对于非空做特殊处理（与mysql语法不一致）
            	else if(command.getProperty("required")!=null) {
            		if((boolean)command.getProperty("required")) {
            			sql += " "+ column.getName() +" SET NOT NULL";
            		}else {
            			sql += " "+ column.getName() +" DROP NOT NULL";
            		}
            		actionList.add(new SQLDatabasePersistAction("Modify column",sql)); 
            	}
            	else {
            		sql += getNestedDeclaration(monitor, column.getTable(), command, options);
            		actionList.add(new SQLDatabasePersistAction(
                            "Modify column",
                            sql)); //$NON-NLS-1$
            	}
        	}else {
        		// do nothing
        	}
        }
        //对注释做特殊处理
        else if (hasComment) {
            actionList.add(new SQLDatabasePersistAction(
                "Comment column",
                "COMMENT ON COLUMN " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + "." + DBUtils.getQuotedIdentifier(column) +
                    " IS '" + column.getComment(new VoidProgressMonitor()) + "'"));
        }
        else {
        	// do nothing
        }
    }

    @Override
    public void renameObject(DBECommandContext commandContext, XuguTableColumn object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        final XuguTableColumn column = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                "Rename column",
                "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " RENAME COLUMN " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) + " TO " +
                    DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName()))
        );
    }

}
