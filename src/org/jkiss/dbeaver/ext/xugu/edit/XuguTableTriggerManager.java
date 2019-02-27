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
import org.jkiss.dbeaver.ext.xugu.model.XuguTableBase;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableTrigger;
import org.jkiss.dbeaver.ext.xugu.model.XuguUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.util.List;
import java.util.Map;

/**
 * OracleTableTriggerManager
 */
public class XuguTableTriggerManager extends SQLTriggerManager<XuguTableTrigger, XuguTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguTableTrigger> getObjectsCache(XuguTableTrigger object)
    {
        return object.getTable().triggerCache;
    }

    @Override
    protected XuguTableTrigger createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final XuguTableBase parent, Object copyFrom)
    {
        return new UITask<XuguTableTrigger>() {
            @Override
            protected XuguTableTrigger runTask() {
                EntityEditPage editPage = new EntityEditPage(parent.getDataSource(), DBSEntityType.TRIGGER);
                if (!editPage.edit()) {
                    return null;
                }
                XuguTableTrigger newTrigger = new XuguTableTrigger(parent, editPage.getEntityName());
                newTrigger.setObjectDefinitionText("TRIGGER " + editPage.getEntityName() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
                    "BEGIN\n" + //$NON-NLS-1$
                    "END;"); //$NON-NLS-1$
                return newTrigger;
            }
        }.execute();
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop trigger", "DROP TRIGGER " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    protected void createOrReplaceTriggerQuery(List<DBEPersistAction> actions, XuguTableTrigger trigger)
    {
        String source = XuguUtils.normalizeSourceName(trigger, false);
        if (source == null) {
            return;
        }
        actions.add(new SQLDatabasePersistAction("Create trigger", "CREATE OR REPLACE " + source, true)); //$NON-NLS-2$
        XuguUtils.addSchemaChangeActions(actions, trigger);
    }

}

