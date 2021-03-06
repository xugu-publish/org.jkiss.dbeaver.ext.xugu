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

import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataType;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableBase;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableColumn;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author xugu-publish
 * 表字段管理器
 * 进行字段的创建，修改和删除（均相当于修改表结构）
 */
public class XuguTableColumnManager extends SQLTableColumnManager<XuguTableColumn, XuguTableBase> implements DBEObjectRenamer<XuguTableColumn> {

    String first = null;

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
    protected XuguTableColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options)
    {
    	XuguTableBase parent = (XuguTableBase)container;
    	
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
    	String query ;
    	String DEFAULT[] = null;
    	if (getNestedDeclaration(monitor, table, command, options).toString()!=null) {
    		DEFAULT = getNestedDeclaration(monitor, table, command, options).toString().split(" DEFAULT ");   		
    	}    
    	DBPDataKind dataKind = command.getObject().getDataKind();
    	if (dataKind == DBPDataKind.STRING && DEFAULT.length>1) {
    		query = "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " ADD "  + DEFAULT[0] + " DEFAULT " + "'" + DEFAULT[1] + "'";
    	}else {
    		query = "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " ADD "  + getXuguNestedDeclaration(monitor, table, command, options);			
    	}
    	if(command.getProperty("comment")!=null && !"".equals(command.getProperty("comment"))) {
    		query += " comment '"+command.getObject().getComment(monitor)+"'";
    	}
    	
    	log.debug("[Xugu] Construct create table column sql: "+query);
    	actions.add(
    			new SQLDatabasePersistAction(
    					ModelMessages.model_jdbc_create_new_table_column,
    					query ));
    	try {
    		table.getSchema().tableCache.refreshObject(monitor, table.getSchema(), table);
    	} catch (DBException e) {
    		log.error(e);
    	}
    }

    //修改了表结构修改的sql语句
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        final XuguTableColumn column = command.getObject();
        //遍历properties 确定每一项新更改均不为空才进行action添加
        if (command.getProperties().size() > 0) {
        	Map<Object, Object> props = command.getProperties();
        	Collection<Object> propKeys = props.keySet();
        	Collection<Object> propValues = props.values();
        	Iterator<Object> it1 = propKeys.iterator();
        	Iterator<Object> it2 = propValues.iterator();
        	while(it1.hasNext()) {
        		String key = it1.next().toString();
        		Object value = it2.next();
        		if(key.equals("comment")) {
        			
        			if(CommonUtils.isEmpty(value.toString())) {
        				
        			}
        			String sql = "COMMENT ON COLUMN " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + "." + DBUtils.getQuotedIdentifier(column) +
        					" IS '" + column.getComment(new VoidProgressMonitor()) + "'";
        			
        			log.debug("[Xugu] Construct alter column comment sql: "+sql);
        			actionList.add(new SQLDatabasePersistAction(
        					"Comment column",sql));
        		} else {
        			String sql = "ALTER TABLE "+column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL)+" ALTER COLUMN ";
        			switch(key) {
        			case "defaultValue":
        				if(command.getProperty("defaultValue").equals("")) {
        					sql += "\"" + column.getName() + "\"" + " DROP DEFAULT";
        				}else {
        					sql += "\"" + column.getName() + "\"" +" SET DEFAULT '" + command.getProperty("defaultValue")+"'";
        				}
        				break;
        			case "required":
        				if((boolean)command.getProperty("required")) {
        					sql +=  "\"" + column.getName() + "\"" +" SET NOT NULL";
        				}else {
        					sql += "\"" + column.getName() + "\"" +" DROP NOT NULL";
        				}
        				break;
        			default:
        				sql += getXuguNestedDeclaration(monitor, column.getTable(), command, options);
        				break;
        			}
        			if (first!=null&&first.equals(sql)) {
        				first = "";
						continue;
					} else {
						first = sql;
						log.debug("[Xugu] Construct alter table column sql: "+sql);
						actionList.add(new SQLDatabasePersistAction("Modify column",sql)); 
					}
        		}
        	}
        }
        else {
        	// do nothing
        }
        try {
        	XuguTableBase table = column.getTable();
        	XuguSchema schema = table.getSchema();
			table.getDataSource().schemaCache.refreshObject(monitor, schema.getDataSource(), schema);
		} catch (DBException e) {
			log.error(e);
		}
    }
    
    protected StringBuilder getXuguNestedDeclaration(DBRProgressMonitor monitor, XuguTableBase owner, DBECommandAbstract<XuguTableColumn> command, Map<String, Object> options)
    {
    	
    	StringBuilder decl = new StringBuilder(100);
    	XuguTableColumn column = command.getObject();

        // 需特殊处理数据类型
        List<String> xuguDataType = new ArrayList<String>();
        xuguDataType.add("INTERVAL DAY TO SECOND");
        xuguDataType.add("INTERVAL HOUR TO SECOND");
        xuguDataType.add("INTERVAL MINUTE TO SECOND");
        xuguDataType.add("INTERVAL YEAR TO MONTH");
        xuguDataType.add("INTERVAL DAY TO HOUR");
        xuguDataType.add("INTERVAL DAY TO MINUTE");
        xuguDataType.add("INTERVAL HOUR TO MINUTE");
        
        String typeName = column.getTypeName();
        if(xuguDataType.contains(typeName)) {
        	// Create column
        	String columnName = DBUtils.getQuotedIdentifier(column.getDataSource(), column.getName());
        	
        	if (command instanceof SQLObjectEditor.ObjectRenameCommand) {
        		columnName = DBUtils.getQuotedIdentifier(column.getDataSource(), ((ObjectRenameCommand) command).getNewName());
        	}
        	
        	// 特殊数据类型处理
        	decl.append(columnName).append(XuguDataTypeModifier(monitor, column, command));
        } else {
        	decl = super.getNestedDeclaration(monitor, owner, command, options);
        }

        return decl;
    }
    
    protected StringBuilder XuguDataTypeModifier(DBRProgressMonitor monitor, XuguTableColumn column, DBECommandAbstract<XuguTableColumn> command){
    	StringBuilder sql = new StringBuilder(50);
        final String typeName = column.getTypeName();
        DBPDataKind dataKind = column.getDataKind();
        final DBSDataType dataType = super.findBestDataType(column.getDataSource(), typeName);
        
        if (dataType == null) {
        	log.debug("Type name '" + typeName + "' is not supported by driver"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
        	dataKind = dataType.getDataKind();
        }
        
        String modifiers = SQLUtils.getColumnTypeModifiers(column.getDataSource(), column, typeName, dataKind);
        if (modifiers == null) {
        	sql.append(' ').append(typeName);
        	return sql;
        } else if(typeName.equals("INTERVAL DAY TO SECOND")) {
        	String[] split = CommonUtils.split(modifiers.replace("(", "").replace(")", ""), ",");
        	String precision = split[0];
        	String scale = split[1];
        	sql.append(' ').append("INTERVAL DAY").append("(" + precision + ')').append(" TO SECOND").append("(" + scale + ')');
        } else if(typeName.equals("INTERVAL HOUR TO SECOND")) {
        	String[] split = CommonUtils.split(modifiers.replace("(", "").replace(")", ""), ",");
        	String precision = split[0];
        	String scale = split[1];
        	sql.append(' ').append("INTERVAL HOUR").append("(" + precision + ')').append(" TO SECOND").append("(" + scale + ')');
        } else if(typeName.equals("INTERVAL MINUTE TO SECOND")) {
        	String[] split = CommonUtils.split(modifiers.replace("(", "").replace(")", ""), ",");
        	String precision = split[0];
        	String scale = split[1];
        	sql.append(' ').append("INTERVAL MINUTE").append("(" + precision + ')').append(" TO SECOND").append("(" + scale + ')');
        } else if(typeName.equals("INTERVAL YEAR TO MONTH")) {
        	sql.append(' ').append("INTERVAL YEAR").append(modifiers).append(" TO MONTH");
        } else if(typeName.equals("INTERVAL DAY TO HOUR")) {
        	sql.append(' ').append("INTERVAL DAY").append(modifiers).append(" TO HOUR");
        } else if(typeName.equals("INTERVAL DAY TO MINUTE")) {
        	sql.append(' ').append("INTERVAL DAY").append(modifiers).append(" TO MINUTE");
        } else if(typeName.equals("INTERVAL HOUR TO MINUTE")) {
        	sql.append(' ').append("INTERVAL HOUR").append(modifiers).append(" TO MINUTE");
        }
        return sql;
    };
    

    @Override
    public void renameObject(DBECommandContext commandContext, XuguTableColumn object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        final XuguTableColumn column = command.getObject();
        String sql = "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " RENAME COLUMN " +
                DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) + " TO " +
                DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName());
        
        log.debug("[Xugu] Construct rename table column sql: "+sql);
        actions.add(
            new SQLDatabasePersistAction(
                "Rename column",
                sql)
        );
    }

}
