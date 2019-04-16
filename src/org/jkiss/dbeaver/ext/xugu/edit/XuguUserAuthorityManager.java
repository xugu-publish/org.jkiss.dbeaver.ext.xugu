package org.jkiss.dbeaver.ext.xugu.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguUDT;
import org.jkiss.dbeaver.ext.xugu.model.XuguUser;
import org.jkiss.dbeaver.ext.xugu.model.XuguUserAuthority;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
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

public class XuguUserAuthorityManager extends SQLObjectEditor<XuguUserAuthority, XuguUser>{

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		// TODO Auto-generated method stub
		return FEATURE_EDITOR_ON_CREATE;
	}

	@Override
	public DBSObjectCache<? extends DBSObject, XuguUserAuthority> getObjectsCache(XuguUserAuthority object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected XuguUserAuthority createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			XuguUser parent, Object copyFrom) throws DBException {
		return new UITask<XuguUserAuthority>() {
            @Override
            protected XuguUserAuthority runTask() {
            	EntityEditPage page = new EntityEditPage(parent.getDataSource(), DBSEntityType.TYPE);
                if (!page.edit()) {
                    return null;
                }
                final XuguUserAuthority userAuthority = new XuguUserAuthority(parent, "", "", false);
                return userAuthority;
            }
        }.execute();
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions,
			SQLObjectEditor<XuguUserAuthority, XuguUser>.ObjectCreateCommand command, Map<String, Object> options) {
		actions.add(
	            new SQLDatabasePersistAction("Grant User",
	                "GRANT " + command.getObject().getName() +" TO "+command.getObject().getUser().getName()) //$NON-NLS-2$
	        );
	}

	@Override
	protected void addObjectDeleteActions(List<DBEPersistAction> actions,
			SQLObjectEditor<XuguUserAuthority, XuguUser>.ObjectDeleteCommand command, Map<String, Object> options) {
		String query = "Revoke " + command.getObject().getName() +" FROM "+command.getObject().getUser().getName();
		actions.add(
	            new SQLDatabasePersistAction("Revoke User",
	                "Revoke " + command.getObject().getName() +" FROM "+command.getObject().getUser().getName()) //$NON-NLS-2$
	        );
	}

}
