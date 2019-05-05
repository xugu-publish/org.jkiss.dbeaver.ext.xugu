package org.jkiss.dbeaver.ext.xugu.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguSequence;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class XuguSequenceManager extends SQLObjectEditor<XuguSequence, XuguSchema> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command) throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Sequence name cannot be empty");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguSequence> getObjectsCache(XuguSequence object)
    {
        return object.getSchema().sequenceCache;
    }

    @Override
    protected XuguSequence createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
                                               final XuguSchema schema,
                                               Object copyFrom)
    {
        return new UITask<XuguSequence>() {
            @Override
            protected XuguSequence runTask() {
                EntityEditPage page = new EntityEditPage(schema.getDataSource(), DBSEntityType.SEQUENCE);
                if (!page.edit()) {
                    return null;
                }

                final XuguSequence sequence = new XuguSequence(schema, page.getEntityName());
                sequence.setIncrementBy(new BigDecimal(1));
                sequence.setMinValue(new BigDecimal(0));
                sequence.setCycle(false);
                return sequence;
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        String sql = buildStatement(command.getObject(), false);
        if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct create sequence sql: "+sql);
        }
        actions.add(new SQLDatabasePersistAction("Create Sequence", sql));

        String comment = buildComment(command.getObject());
        if (comment != null) {
        	if(XuguConstants.LOG_PRINT_LEVEL<1) {
            	log.info("Xugu Plugin: Construct add comment to sequence sql: "+comment);
            }
            actions.add(new SQLDatabasePersistAction("Comment on Sequence", comment));
        }
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        String sql = buildStatement(command.getObject(), true);
        if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct alter sequence sql: "+sql.toString());
        }
        actionList.add(new SQLDatabasePersistAction("Alter Sequence", sql));

        String comment = buildComment(command.getObject());
        if (comment != null) {
        	if(XuguConstants.LOG_PRINT_LEVEL<1) {
            	log.info("Xugu Plugin: Construct alter sequence comment sql: "+comment);
            }
            actionList.add(new SQLDatabasePersistAction("Comment on Sequence", comment));
        }
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        String sql = "DROP SEQUENCE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL);
        if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct drop sequence sql: "+sql);
        }
        DBEPersistAction action = new SQLDatabasePersistAction("Drop Sequence", sql);
        actions.add(action);
    }

    private String buildStatement(XuguSequence sequence, Boolean forUpdate)
    {
        StringBuilder sb = new StringBuilder();
        if (forUpdate) {
            sb.append("ALTER SEQUENCE ");
        } else {
            sb.append("CREATE SEQUENCE ");
        }
        sb.append(sequence.getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" ");

        if (sequence.getIncrementBy() != null) {
            sb.append("INCREMENT BY ").append(sequence.getIncrementBy()).append(" ");
        }
        if (sequence.getMinValue() != null) {
            sb.append("MINVALUE ").append(sequence.getMinValue()).append(" ");
        }
        if (sequence.getMaxValue() != null) {
            sb.append("MAXVALUE ").append(sequence.getMaxValue()).append(" ");
        }

        if (sequence.isCycle()) {
            sb.append("CYCLE ");
        } else {
            sb.append("NOCYCLE ");
        }
        if (sequence.getCacheValue() > 0) {
            sb.append("CACHE ").append(sequence.getCacheValue()).append(" ");
        } else {
            sb.append("NOCACHE ");
        }
        if (sequence.isOrder()) {
            sb.append("ORDER ");
        } else {
            sb.append("NOORDER ");
        }

        return sb.toString();
    }

    private String buildComment(XuguSequence sequence)
    {
        if (!CommonUtils.isEmpty(sequence.getDescription())) {
            return "COMMENT ON SEQUENCE " + sequence.getFullyQualifiedName(DBPEvaluationContext.DDL) + " IS " + SQLUtils.quoteString(sequence, sequence.getDescription());
        }
        return null;
    }

}
