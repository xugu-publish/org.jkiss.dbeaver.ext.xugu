/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.XuguQueue;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class XuguQueueManager extends SQLObjectEditor<XuguQueue, XuguSchema> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command) throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Queue name cannot be empty");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguQueue> getObjectsCache(XuguQueue object)
    {
        return object.getSchema().queueCache;
    }

    @Override
    protected XuguQueue createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
                                               final XuguSchema schema,
                                               Object copyFrom)
    {
        return new UITask<XuguQueue>() {
            @Override
            protected XuguQueue runTask() {
                EntityEditPage page = new EntityEditPage(schema.getDataSource(), DBSEntityType.SEQUENCE);
                if (!page.edit()) {
                    return null;
                }

                final XuguQueue queue = new XuguQueue(schema, page.getEntityName());
                return queue;
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {

    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {

    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {

    }
}
