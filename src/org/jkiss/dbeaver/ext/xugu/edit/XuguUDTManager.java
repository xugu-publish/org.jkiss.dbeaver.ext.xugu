package org.jkiss.dbeaver.ext.xugu.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguUDT;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * @author Maple4Real
 * 自定义类型管理器
 * 进行自定义类型的创建和删除，不支持修改
 */
public class XuguUDTManager extends SQLObjectEditor<XuguUDT, XuguSchema>{
	
	@Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }
	
    protected void validateObjectProperties(ObjectChangeCommand command) throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Type name cannot be empty");
        }
    }

	@Override
	public DBSObjectCache<? extends DBSObject, XuguUDT> getObjectsCache(XuguUDT object) {
		// TODO Auto-generated method stub
		return object.getSchema().udtCache;
	}

	@Override
	protected XuguUDT createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options) throws DBException 
	{
		XuguSchema schema = (XuguSchema)container;
		return new UITask<XuguUDT>() {
            @Override
            protected XuguUDT runTask() {
            	EntityEditPage page = new EntityEditPage(schema.getDataSource(), DBSEntityType.TYPE);
                if (!page.edit()) {
                    return null;
                }

                XuguUDT udt = new XuguUDT(schema, page.getEntityName());
                udt.setObjectDefinitionText("CREATE TYPE "+page.getEntityName()+" AS OBJECT");
                udt.setExtendedDefinitionText("CREATE TYPE "+page.getEntityName()+" AS ");
                udt.setValid(true);
                return udt;
            }
        }.execute();
	}
	
	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
		XuguUDT udt = command.getObject();
		String sql = null;
		try {
			sql = udt.getObjectDefinitionText(monitor, options);
		} catch (DBException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage(), e);
		}
		if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct create UDT sql: "+sql);
        }
		actions.add(new SQLDatabasePersistAction("Create UDT", sql));
	}
	
    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, NestedObjectCommand<XuguUDT, PropertyHandler> command, Map<String, Object> options) {
        if (command.getProperty("comment") != null) {
        	StringBuilder desc = new StringBuilder(100);
        	desc.append("COMMENT ON TYPE ");
        	desc.append(command.getObject().getName());
        	desc.append(" IS ");
        	desc.append(SQLUtils.quoteString(command.getObject(), command.getObject().getComment()));
        	if(XuguConstants.LOG_PRINT_LEVEL<1) {
            	log.info("Xugu Plugin: Construct add package comment sql: " + desc.toString());
            }
            actions.add(new SQLDatabasePersistAction("Comment Package", desc.toString()));
        }
    }
    
	@Override
	protected void addObjectDeleteActions(List<DBEPersistAction> actions,
			SQLObjectEditor<XuguUDT, XuguSchema>.ObjectDeleteCommand command, Map<String, Object> options) {
		String sql = "DROP TYPE " + DBUtils.getQuotedIdentifier(command.getObject());
		if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct drop UDT sql: "+sql);
        }
		actions.add(
            new SQLDatabasePersistAction("Drop UDT",
                sql) //$NON-NLS-2$
        );
	}
}


