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

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;
import org.jkiss.utils.CommonUtils;

/**
 * @author Maple4Real
 * 外键管理器
 * 进行外键的增加
 */
public class XuguForeignKeyManager extends SQLForeignKeyManager<XuguTableForeignKey, XuguTableBase> {


    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguTableForeignKey> getObjectsCache(XuguTableForeignKey object)
    {
        return object.getParentObject().getSchema().foreignKeyCache;
    }
    
    @Override
    protected XuguTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options)
    {
    	XuguTableBase table = (XuguTableBase)container;
        return new UITask<XuguTableForeignKey>() {
            @Override
            protected XuguTableForeignKey runTask() {
                EditForeignKeyPage editPage = new EditForeignKeyPage(
                    XuguMessages.edit_xugu_foreign_key_manager_dialog_title,
                    new XuguTableForeignKey(
                            table,
                            null,
                            XuguObjectStatus.ENABLED,
                            null,
                            DBSForeignKeyModifyRule.NO_ACTION,
                            DBSForeignKeyModifyRule.NO_ACTION),
                    new DBSForeignKeyModifyRule[] {
                        DBSForeignKeyModifyRule.NO_ACTION,
                        DBSForeignKeyModifyRule.CASCADE,
                        DBSForeignKeyModifyRule.SET_NULL,
                        DBSForeignKeyModifyRule.SET_DEFAULT });
                if (!editPage.edit()) {
                    return null;
                }

                final XuguTableForeignKey foreignKey = new XuguTableForeignKey(
                    table,
                    null,
                    XuguObjectStatus.ENABLED,
                    (XuguTableConstraint) editPage.getUniqueConstraint(),
                    editPage.getOnDeleteRule(),
                    editPage.getOnUpdateRule());
                foreignKey.setName(getNewConstraintName(monitor, foreignKey));
                foreignKey.setEnable(foreignKey.getStatus() == XuguObjectStatus.ENABLED);
                int colIndex = 1;
                for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                    foreignKey.addColumn(
                        new XuguTableForeignKeyColumn(
                            foreignKey,
                            (XuguTableColumn) tableColumn.getOwnColumn(),
                            colIndex++,
                            (XuguTableColumn) tableColumn.getRefColumn()));
                }
                return foreignKey;
            }
        }.execute();
    }
    
    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
    	XuguTableForeignKey foreignKey = (XuguTableForeignKey)command.getObject();
        XuguTableBase table = command.getObject().getTable();
        
        StringBuilder decl = new StringBuilder(100);
    	decl.append("ALTER TABLE ");
    	decl.append(table.getFullyQualifiedName(DBPEvaluationContext.DDL));
    	decl.append(" ADD ");
    	decl.append(getNestedDeclaration(monitor, table, command, options));
    	decl.append(";");
    	decl.append(CommonUtils.getLineSeparator());
    	
    	decl.append("ALTER TABLE ");
    	decl.append(table.getFullyQualifiedName(DBPEvaluationContext.DDL));
    	decl.append(foreignKey.isEnable()? " ENABLE" : " DISABLE");
    	decl.append(" CONSTRAINT ");
    	decl.append(foreignKey.getName());
    	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct create foreign key sql: "+ decl.toString());
        }
    	
        actions.add(
            new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_foreign_key, decl.toString()) //$NON-NLS-1$ //$NON-NLS-2$
        );
    }
    
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
    	XuguTableForeignKey fk = (XuguTableForeignKey) command.getObject();
    	XuguTableBase table = fk.getTable();
    	String sql = "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + (fk.isEnable()? " ENABLE" : " DISABLE") +" CONSTRAINT "+ fk.getName();
    	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct alter foreign key sql: "+sql);
        }
    	actionList.add(new SQLDatabasePersistAction("Alter foreign key", sql));
    }
}
