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
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
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
    protected XuguTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final XuguTableBase table, Object from)
    {
        return new UITask<XuguTableForeignKey>() {
            @Override
            protected XuguTableForeignKey runTask() {
                EditForeignKeyPage editPage = new EditForeignKeyPage(
                    XuguMessages.edit_xugu_foreign_key_manager_dialog_title,
                    table,
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
                    null,
                    (XuguTableConstraint) editPage.getUniqueConstraint(),
                    editPage.getOnDeleteRule(),
                    editPage.getOnUpdateRule());
                foreignKey.setName(getNewConstraintName(monitor, foreignKey));
                int colIndex = 1;
                for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                    foreignKey.addColumn(
                        new XuguTableForeignKeyColumn(
                            foreignKey,
                            (XuguTableColumn) tableColumn.getOwnColumn(),
                            colIndex++));
                }
                return foreignKey;
            }
        }.execute();
    }
}
