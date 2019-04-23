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
import org.jkiss.dbeaver.ext.xugu.model.XuguDataType;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * XuguDataTypeManager
 */
public class XuguDataTypeManager extends SQLObjectEditor<XuguDataType, XuguSchema> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguDataType> getObjectsCache(XuguDataType object)
    {
        return object.getSchema().dataTypeCache;
    }

    @Override
    protected XuguDataType createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final XuguSchema parent, Object copyFrom)
    {
        return new UITask<XuguDataType>() {
            @Override
            protected XuguDataType runTask() {
                EntityEditPage editPage = new EntityEditPage(parent.getDataSource(), DBSEntityType.TYPE);
                if (!editPage.edit()) {
                    return null;
                }
                XuguDataType dataType = new XuguDataType(
                    parent,
                    editPage.getEntityName(),
                    false);
                dataType.setObjectDefinitionText("TYPE " + dataType.getName() + " AS OBJECT\n" + //$NON-NLS-1$ //$NON-NLS-2$
                    "(\n" + //$NON-NLS-1$
                    ")"); //$NON-NLS-1$
                return dataType;
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
        final XuguDataType object = objectDeleteCommand.getObject();
        actions.add(
            new SQLDatabasePersistAction("Drop type",
                "DROP TYPE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-1$
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

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actionList, XuguDataType dataType)
    {
        String header = XuguUtils.normalizeSourceName(dataType, false);
        if (!CommonUtils.isEmpty(header)) {
            actionList.add(
                new SQLDatabasePersistAction(
                    "Create type header",
                    "CREATE OR REPLACE " + header)); //$NON-NLS-1$
        }
        String body = XuguUtils.normalizeSourceName(dataType, true);
        if (!CommonUtils.isEmpty(body)) {
            actionList.add(
                new SQLDatabasePersistAction(
                    "Create type body",
                    "CREATE OR REPLACE " + body)); //$NON-NLS-1$
        }
        XuguUtils.addSchemaChangeActions(actionList, dataType);
    }

}

