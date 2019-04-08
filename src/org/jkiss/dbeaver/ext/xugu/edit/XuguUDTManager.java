package org.jkiss.dbeaver.ext.xugu.edit;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguSequence;
import org.jkiss.dbeaver.ext.xugu.model.XuguUDT;
import org.jkiss.dbeaver.ext.xugu.model.XuguUDT;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

public class XuguUDTManager extends SQLObjectEditor<XuguUDT, XuguSchema>{
	@Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }
	
	@Override
	protected XuguUDT createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final XuguSchema parent, Object copyFrom) throws DBException {
		return new UITask<XuguUDT>() {
            @Override
            protected XuguUDT runTask() {
            	EntityEditPage page = new EntityEditPage(parent.getDataSource(), DBSEntityType.TYPE);
                if (!page.edit()) {
                    return null;
                }

                final XuguUDT udt = new XuguUDT(parent, null);
                udt.setTypeHead("CREATE TYPE "+page.getEntityName()+" AS OBJECT");
                udt.setTypeBody("CREATE TYPE "+page.getEntityName()+" AS ");
                udt.setName(page.getEntityName());
                return udt;
            }
        }.execute();
	}
	
	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
		XuguUDT udt = command.getObject();
		String sql = udt.getTypeHead();
		actions.add(new SQLDatabasePersistAction("Create UDT", sql));
	}
	
	@Override
	protected void addObjectDeleteActions(List<DBEPersistAction> actions,
			SQLObjectEditor<XuguUDT, XuguSchema>.ObjectDeleteCommand command, Map<String, Object> options) {
		actions.add(
            new SQLDatabasePersistAction("Drop UDT",
                "DROP TYPE " + DBUtils.getQuotedIdentifier(command.getObject())) //$NON-NLS-2$
        );
	}
	
	@Override
	public DBSObjectCache<? extends DBSObject, XuguUDT> getObjectsCache(XuguUDT object) {
		// TODO Auto-generated method stub
		return object.getSchema().udtCache;
	}
}

