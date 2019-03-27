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
import org.jkiss.dbeaver.ext.xugu.model.XuguTableColumn;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableIndex;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableIndexColumn;
import org.jkiss.dbeaver.ext.xugu.model.XuguTablePhysical;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Xugu index manager
 */
public class XuguIndexManager extends SQLIndexManager<XuguTableIndex, XuguTablePhysical> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguTableIndex> getObjectsCache(XuguTableIndex object)
    {
        return object.getParentObject().getSchema().indexCache;
    }

    @Override
    protected XuguTableIndex createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final XuguTablePhysical parent,
        Object from)
    {
        return new UITask<XuguTableIndex>() {
            @Override
            protected XuguTableIndex runTask() {
                EditIndexPage editPage = new EditIndexPage(
                    XuguMessages.edit_xugu_index_manager_dialog_title,
                    parent,
                    Collections.singletonList(DBSIndexType.OTHER));
                if (!editPage.edit()) {
                    return null;
                }

                StringBuilder idxName = new StringBuilder(64);
                idxName.append(CommonUtils.escapeIdentifier(parent.getName())).append("_") //$NON-NLS-1$
                    .append(CommonUtils.escapeIdentifier(editPage.getSelectedAttributes().iterator().next().getName()))
                    .append("_IDX"); //$NON-NLS-1$
                final XuguTableIndex index = new XuguTableIndex(
                    parent.getSchema(),
                    parent,
                    DBObjectNameCaseTransformer.transformName(parent.getDataSource(), idxName.toString()),
                    editPage.isUnique(),
                    editPage.getIndexType());
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
            		index.addColumn(
                            new XuguTableIndexColumn(
                                index,
                                (XuguTableColumn) tableColumn,
                                colIndex++,
                                !Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, EditIndexPage.PROP_DESC)),
                                null));
                }
                return index;
            }
        }.execute();
    }
    
    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_index,
                getDropIndexPattern(command.getObject())
                    .replace(PATTERN_ITEM_TABLE, command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))
                    .replace(PATTERN_ITEM_INDEX, command.getObject().getName())
                    .replace(PATTERN_ITEM_INDEX_SHORT, DBUtils.getQuotedIdentifier(command.getObject())))
        );
        String t = getDropIndexPattern(command.getObject())
                .replace(PATTERN_ITEM_TABLE, command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))
                .replace(PATTERN_ITEM_INDEX, command.getObject().getName())
                .replace(PATTERN_ITEM_INDEX_SHORT, DBUtils.getQuotedIdentifier(command.getObject()));
        System.out.println("DDDrop index "+t);
    }

    @Override
    protected String getDropIndexPattern(XuguTableIndex index)
    {
        return "DROP INDEX " + PATTERN_ITEM_TABLE + "." + PATTERN_ITEM_INDEX; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
