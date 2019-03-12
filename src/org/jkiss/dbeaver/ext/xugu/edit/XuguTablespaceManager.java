package org.jkiss.dbeaver.ext.xugu.edit;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.XuguObjectType;
import org.jkiss.dbeaver.ext.xugu.model.XuguObjectValidateAction;
import org.jkiss.dbeaver.ext.xugu.model.XuguPackage;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguTablespace;
import org.jkiss.dbeaver.ext.xugu.model.XuguUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;
import org.jkiss.utils.CommonUtils;

public class XuguTablespaceManager extends SQLObjectEditor<XuguTablespace, XuguSchema> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguTablespace> getObjectsCache(XuguTablespace object)
    {
        return object.getDataSource().getTablespaceCache();
    }

    @Override
    protected XuguTablespace createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final XuguSchema parent, Object copyFrom)
    {
        return new UITask<XuguTablespace>() {
            @Override
            protected XuguTablespace runTask() {
                EntityEditPage editPage = new EntityEditPage(parent.getDataSource(), DBSEntityType.VIRTUAL_ENTITY);
                if (!editPage.edit()) {
                    return null;
                }
                String tsName = editPage.getEntityName();
                XuguTablespace xuguTableSpace;
				try {
					xuguTableSpace = new XuguTablespace(
					    parent.getDataSource(),
					    tsName);
					return xuguTableSpace;
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                return null;
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
        final XuguTablespace object = objectDeleteCommand.getObject();
        actions.add(
            new SQLDatabasePersistAction("Drop tablespace",
                "DROP TABLESPACE " + object.getName()) //$NON-NLS-1$
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

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actionList, XuguTablespace space)
    {
    	
        try {
            String header = space.getObjectDefinitionText(new VoidProgressMonitor(), DBPScriptObject.EMPTY_OPTIONS);
            //对header进行预处理
            header = header.toUpperCase();
            String str1 = header.substring(header.indexOf("CREATE")+6, header.indexOf("PACKAGE"));
            if(str1.indexOf("OR")==-1) {
            	header = "CREATE OR REPLACE "+header.substring(header.indexOf("PACKAGE"));
            }
            if (!CommonUtils.isEmpty(header)) {
                actionList.add(
                    new XuguObjectValidateAction(
                        space, XuguObjectType.TABLESPACE,
                        "Create tablespace",
                        header)); //$NON-NLS-1$
            }
        } catch (DBException e) {
            log.warn(e);
        }
    }

}

