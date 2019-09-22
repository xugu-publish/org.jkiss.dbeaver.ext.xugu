package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;
import org.jkiss.dbeaver.ext.xugu.XuguUtils;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Map;

/**
 * @author Maple4Real
 *   触发器基类，包含触发器基本信息
 */
public abstract class XuguTriggerBase<PARENT extends DBSObject> extends XuguObject<PARENT> implements DBSTrigger, DBPQualifiedObject, XuguSourceObject, DBPScriptObjectExt, DBPRefreshableObject
{
    public enum BaseObjectType {
        TABLE,
        VIEW
    }

    public enum ActionType implements DBPNamedObject {
        PLSQL("PL/SQL"),
        CALL("CALL");

        private final String title;

        ActionType(String title)
        {
            this.title = title;
        }

        @NotNull
        @Override
        public String getName()
        {
            return title;
        }
    }

    private BaseObjectType objectType;
    private int triggerType;
    private int triggeringEvent;
    private int triggerTime;
    private int id;
    private String refName;
    private XuguObjectStatus status;
    private boolean valid;
    private boolean deleted;
    protected String define;
    private String allDefine;
    private String triggerCondition;
    private String comment;
    private Timestamp createTime;
    
    public XuguTriggerBase(PARENT parent, String name)
    {
        super(parent, name, false);
    }
    
    public static class CommentsValidator implements IPropertyCacheValidator<XuguTriggerBase> {
        @Override
        public boolean isPropertyCached(XuguTriggerBase object, Object propertyId)
        {
            return object.comment != null;
        }
    }

    public XuguTriggerBase(
        XuguTableBase parent,
        ResultSet dbResult)
    {
        super((PARENT)parent, JDBCUtils.safeGetString(dbResult, "TRIG_NAME"), true);
        //只有一种类型触发器
        this.objectType = BaseObjectType.TABLE;
        this.triggerType = JDBCUtils.safeGetInt(dbResult, "TRIG_TYPE");
        this.triggeringEvent = JDBCUtils.safeGetInt(dbResult, "TRIG_EVENT");
        this.triggerTime = JDBCUtils.safeGetInt(dbResult, "TRIG_TIME");
        this.triggerCondition = JDBCUtils.safeGetString(dbResult, "TRIG_COND");
        //根据obj_id获取？是否代表列id？
        if(parent.getType().getTypeName().equals(XuguObjectType.TABLE.getTypeName())) {
        	this.id = JDBCUtils.safeGetInt(dbResult, "TABLE_ID");
        }else {
        	this.id = JDBCUtils.safeGetInt(dbResult, "VIEW_ID");
        }
        this.status = JDBCUtils.safeGetBoolean(dbResult, "ENABLE")?XuguObjectStatus.ENABLED:XuguObjectStatus.DISABLED;
        this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
        this.allDefine = JDBCUtils.safeGetString(dbResult, "DEFINE");
        this.define = allDefine.substring(allDefine.indexOf("BEGIN"));
        this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
    }

    @NotNull
    @Property(viewable = false, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = -1)
	public int getId() {
		return id;
	}

    @NotNull
    @Override
    @Property(viewable = true, editable = true, updatable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 2)
    public String getObjectType()
    {
        return objectType.toString();
    }

    public void setObjectType(String type) {
    	this.objectType = BaseObjectType.valueOf(type);
    }
    
    @Property(viewable = true, editable = false, updatable = false, order = 4)
    public String getTriggerType()
    {
    	switch(triggerType) {
    	case 1:
    		return "FOR EACH ROW";
    	case 2:
    		return "FOR STATEMENT";
		default:
			return "NOT SUPPORTED "+triggerType;
    	}
    }

    public void setTriggerType(int type) {
    	this.triggerType = type;
    }
    
    @Property(viewable = true, editable = false, updatable = false, order = 7)
    public String getTriggerCondition() {
    	return this.triggerCondition;
    }
    
    public void setTriggerCondition(String condition) {
    	this.triggerCondition = condition;
    }
    
    @Property(viewable = true, editable = false, updatable = false, order = 5)
    public String getTriggeringEvent()
    {
    	switch(triggeringEvent) {
    	case 0:
    		return "";
    	case 1:
    		return "INSERT";
    	case 2:
    		return "UPDATE";
    	case 3:
    		return "INSERT,UPDATE";
    	case 4:
    		return "DELETE";
    	case 5:
    		return "INSERT,DELETE";
    	case 6:
    		return "UPDATE,DELETE";
    	case 7:
    		return "INSERT,DELETE,UPDATE";
    	default:
    		return "NOT SUPPORTED "+triggeringEvent;
    	}
    }
    
    public void setTriggeringEvent(int event) {
    	this.triggeringEvent = event;
    }
    
    @Property(viewable = true, editable = false, updatable = false, order = 6)
    public String getTriggerTime()
    {
    	switch(triggerTime) {
    	case 1:
    		return "BEFORE";
    	case 2:
    		return "INSTEAD OF";
    	case 4:
    		return "AFTER";
    	default:
    		return "INSTEAD OF";
    	}
    }
    
    public void setTriggerTime(int time) {
    	this.triggerTime = time;
    }

    public void setTriggerTime(String time) {
    	switch(time) {
    	case "BEFORE":
    		this.triggerTime = 1;
    		break;
    	case "INSTEAD OF":
    		this.triggerTime = 2;
    		break;
    	case "AFTER":
    		this.triggerTime = 4;
    		break;
    	default:
    		this.triggerTime = -1;
    	}
    }
    
    @Nullable
    @Override
    //@Property(viewable = false, editable = true, updatable = false, multiline = true, order = 8)
    public String getDescription()
    {
        return allDefine;
    }

    @Override
    public XuguSourceType getSourceType()
    {
        return XuguSourceType.TRIGGER;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        return this.define;
    }
    
    @Override
    public void setObjectDefinitionText(String source)
    {
        this.define = source;
    }
    
    @NotNull
    @Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 9)
    public Timestamp getCreateTime() {
    	return createTime;
    }
    

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 10)
    public String getComment()
    {
    	return comment;
    }
    
    @NotNull
    @Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 11)
    public boolean isValid() {
    	return valid;
    }
    
    public void setComment(String comment)
    {
        this.comment = comment;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return valid? DBSObjectState.NORMAL: DBSObjectState.INVALID;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.status = (XuguUtils.getObjectStatus(monitor, this, XuguObjectType.TRIGGER) ? XuguObjectStatus.ENABLED : XuguObjectStatus.DISABLED);
    }

    @Override
    public DBEPersistAction[] getCompileActions()
    {
        return new DBEPersistAction[] {
            new XuguObjectPersistAction(
                XuguObjectType.TRIGGER,
                "Compile trigger",
                "ALTER TRIGGER " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
            )};
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getSchema(),
            this);
    }

    @Override
    public String toString() {
        return getFullyQualifiedName(DBPEvaluationContext.DDL);
    }
}
