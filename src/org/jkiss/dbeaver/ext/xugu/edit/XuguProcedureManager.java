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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateProcedurePage;

import java.util.List;
import java.util.Map;

/**
 * OracleProcedureManager
 */
public class XuguProcedureManager extends SQLObjectEditor<XuguProcedureStandalone, XuguSchema> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguProcedureStandalone> getObjectsCache(XuguProcedureStandalone object)
    {
        return object.getSchema().proceduresCache;
    }

    @Override
    protected XuguProcedureStandalone createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final XuguSchema parent, Object copyFrom)
    {
        return new UITask<XuguProcedureStandalone>() {
            @Override
            protected XuguProcedureStandalone runTask() {
                CreateProcedurePage editPage = new CreateProcedurePage(parent);
                if (!editPage.edit()) {
                    return null;
                }
                return new XuguProcedureStandalone(
                    parent,
                    editPage.getProcedureName(),
                    editPage.getProcedureType());
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand objectCreateCommand, Map<String, Object> options)
    {
        createOrReplaceProcedureQuery(actions, objectCreateCommand.getObject());
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand objectDeleteCommand, Map<String, Object> options)
    {
        final XuguProcedureStandalone object = objectDeleteCommand.getObject();
        actions.add(
            new SQLDatabasePersistAction("Drop procedure",
                "DROP " + object.getProcedureType().name() + " " + object.getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-1$ //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand objectChangeCommand, Map<String, Object> options)
    {
        createOrReplaceProcedureQuery(actionList, objectChangeCommand.getObject());
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actionList, XuguProcedureStandalone procedure)
    {
        String source = XuguUtils.normalizeSourceName(procedure, false);
        if (source == null) {
            return;
        }
        actionList.add(new XuguObjectValidateAction(procedure, XuguObjectType.PROCEDURE, "Create procedure", source)); //$NON-NLS-2$
        XuguUtils.addSchemaChangeActions(actionList, procedure);
    }

}
