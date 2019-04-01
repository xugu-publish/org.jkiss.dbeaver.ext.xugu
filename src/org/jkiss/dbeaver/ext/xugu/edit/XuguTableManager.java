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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Xugu table manager
 */
public class XuguTableManager extends SQLTableManager<XuguTable, XuguSchema> implements DBEObjectRenamer<XuguTable> {

    private static final Class<?>[] CHILD_TYPES = {
        XuguTableColumn.class,
        XuguTableConstraint.class,
        XuguTableForeignKey.class,
        XuguTableIndex.class
    };

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguTable> getObjectsCache(XuguTable object)
    {
        return (DBSObjectCache) object.getSchema().tableCache;
    }

    // 在打开新建表窗口前的准备
    @Override
    protected XuguTable createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, XuguSchema parent, Object copyFrom)
    {
        XuguTable table = new XuguTable(parent, "");
        try {
            setTableName(monitor, parent, table);
        } catch (DBException e) {
            log.error(e);
        }
        return table; //$NON-NLS-1$
    }
    
    @Override
    protected void setTableName(DBRProgressMonitor monitor,  XuguSchema parent, XuguTable table) throws DBException {
        table.setName(getNewChildName(monitor, parent, "NEWTABLE"));
        System.out.println("prepare create table "+table.getName());
    }

    @Override
    protected String getNewChildName(DBRProgressMonitor monitor, XuguSchema parent, String baseName) throws DBException {
        for (int i = 0; i<20; i++) {
            String tableName = i == 0 ? baseName : (baseName + "_" + i);
            DBSObject child = parent.getChild(monitor, tableName);
            if (child == null) {
                return tableName;
            }
        }
        return "NEW_TABLE_";
    }
    
    @Override
    protected void addStructObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options) throws DBException {
    	super.addStructObjectCreateActions(monitor, actions, command, options);
//    	XuguTable table = command.getObject();
//        NestedObjectCommand tableProps = command.getObjectCommands().get(table);
//        if (tableProps == null) {
//            log.warn("Object change command not found"); //$NON-NLS-1$
//            return;
//        }
//        //获取完整的表名
//        String tableName = CommonUtils.getOption(options, DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, true) ?
//            table.getFullyQualifiedName(DBPEvaluationContext.DDL) : DBUtils.getQuotedIdentifier(table);
//        System.out.println("table name? "+tableName);
//        String slComment = SQLUtils.getDialectFromObject(table).getSingleLineComments()[0];
//        String lineSeparator = GeneralUtils.getDefaultLineSeparator();
//        StringBuilder createQuery = new StringBuilder(100);
//        createQuery.append("CREATE ").append(getCreateTableType(table)).append(" ").append(tableName).append(" (").append(lineSeparator); //$NON-NLS-1$ //$NON-NLS-2$
//        boolean hasNestedDeclarations = false;
//        Collection<NestedObjectCommand> orderedCommands = getNestedOrderedCommands(command);
//        for (NestedObjectCommand nestedCommand : orderedCommands) {
//            if (nestedCommand.getObject() == table) {
//                continue;
//            }
//            if (excludeFromDDL(nestedCommand, orderedCommands)) {
//                continue;
//            }
//            final String nestedDeclaration = nestedCommand.getNestedDeclaration(monitor, table, options);
//            if (!CommonUtils.isEmpty(nestedDeclaration)) {
//                // Insert nested declaration
//                if (hasNestedDeclarations) {
//                    // Check for embedded comment
//                    int lastLFPos = createQuery.lastIndexOf(lineSeparator);
//                    int lastCommentPos = createQuery.lastIndexOf(slComment);
//                    if (lastCommentPos != -1) {
//                        while (lastCommentPos > 0 && Character.isWhitespace(createQuery.charAt(lastCommentPos - 1))) {
//                            lastCommentPos--;
//                        }
//                    }
//                    if (lastCommentPos < 0 || lastCommentPos < lastLFPos) {
//                        createQuery.append(","); //$NON-NLS-1$
//                    } else {
//                        createQuery.insert(lastCommentPos, ","); //$NON-NLS-1$
//                    }
//                    createQuery.append(lineSeparator); //$NON-NLS-1$
//                }
//                createQuery.append("\t").append(nestedDeclaration); //$NON-NLS-1$
//                hasNestedDeclarations = true;
//            } else {
//                // This command should be executed separately
//                final DBEPersistAction[] nestedActions = nestedCommand.getPersistActions(monitor, options);
//                if (nestedActions != null) {
//                    Collections.addAll(actions, nestedActions);
//                }
//            }
//        }
//
//        createQuery.append(lineSeparator).append(")"); //$NON-NLS-1$
//        appendTableModifiers(monitor, table, tableProps, createQuery, false);
//
//        actions.add( 0, new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_table, createQuery.toString()) );
//        //刷新？
//        table.getDataSource().refreshObject(monitor);
    }
    
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        if (command.getProperties().size() > 1 || command.getProperty("comment") == null) {
            StringBuilder query = new StringBuilder("ALTER TABLE "); //$NON-NLS-1$
            query.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" "); //$NON-NLS-1$
            appendTableModifiers(monitor, command.getObject(), command, query, true);
            actionList.add(new SQLDatabasePersistAction(query.toString()));
        }
    }

    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, NestedObjectCommand<XuguTable, PropertyHandler> command, Map<String, Object> options) {
        if (command.getProperty("comment") != null) {
            actions.add(new SQLDatabasePersistAction(
                "Comment table",
                "COMMENT ON TABLE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS " + SQLUtils.quoteString(command.getObject(), command.getObject().getComment())));
        }
    }

    @Override
    protected void appendTableModifiers(DBRProgressMonitor monitor, XuguTable table, NestedObjectCommand tableProps, StringBuilder ddl, boolean alter)
    {
        // ALTER
        if (tableProps.getProperty("tablespace") != null) { //$NON-NLS-1$
            Object tablespace = table.getTablespace();
            if (tablespace instanceof XuguTablespace) {
                if (table.isPersisted()) {
                    ddl.append("\nMOVE TABLESPACE ").append(((XuguTablespace) tablespace).getName()); //$NON-NLS-1$
                } else {
                    ddl.append("\nTABLESPACE ").append(((XuguTablespace) tablespace).getName()); //$NON-NLS-1$
                }
            }
        }
    }
    
    //修改表名、视图名
    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction(
                "Rename table",
                "ALTER TABLE " + DBUtils.getQuotedIdentifier(command.getObject().getSchema()) + "." + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()) + //$NON-NLS-1$
                    " RENAME TO " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName())) //$NON-NLS-1$
        );
    }

    //删除表、视图
    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        XuguTable object = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP " + (object.isView() ? "VIEW" : "TABLE") +  //$NON-NLS-2$
                    " " + object.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    (!object.isView() && CommonUtils.getOption(options, OPTION_DELETE_CASCADE) ? " CASCADE CONSTRAINTS" : "")
            )
        );
    }

    @NotNull
    @Override
    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    @Override
    public void renameObject(DBECommandContext commandContext, XuguTable object, String newName) throws DBException
    {
        processObjectRename(commandContext, object, newName);
    }

}
