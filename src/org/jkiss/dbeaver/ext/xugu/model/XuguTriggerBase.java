package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

import java.sql.ResultSet;
import java.util.Map;

/**
 * @author Maple4Real
 *   触发器基类，包含触发器基本信息
 */
public abstract class XuguTriggerBase<PARENT extends DBSObject> extends XuguObject<PARENT> implements DBSTrigger, DBPQualifiedObject, XuguSourceObject
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
    private int obj_id;
    private String refName;
    private XuguObjectStatus status;
    private boolean valid;
    private boolean deleted;
    private String define;
    private String allDefine;

    public XuguTriggerBase(PARENT parent, String name)
    {
        super(parent, name, false);
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
        //根据obj_id获取？是否代表列id？
        if(parent.getType()==0) {
        	this.obj_id = JDBCUtils.safeGetInt(dbResult, "TABLE_ID");
        }else {
        	this.obj_id = JDBCUtils.safeGetInt(dbResult, "VIEW_ID");
        }
        this.status = JDBCUtils.safeGetBoolean(dbResult, "ENABLE")?XuguObjectStatus.ENABLED:XuguObjectStatus.DISABLED;
        this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
        this.allDefine = JDBCUtils.safeGetString(dbResult, "DEFINE");
        this.define = allDefine.substring(allDefine.indexOf("BEGIN"));
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
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
    
    @Property(viewable = true, editable = false, updatable = false, order = 5)
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
    
    @Property(viewable = true, editable = false, updatable = false, order = 6)
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
    
    @Property(viewable = true, editable = false, updatable = false, order = 7)
    public String getTriggerTime()
    {
    	switch(triggerTime) {
    	case 1:
    		return "BEFORE";
    	case 2:
    		return "REPLACE";
    	case 4:
    		return "AFTER";
    	default:
    		return "NOT SUPPORTED "+triggerTime;
    	}
    }
    
    public void setTriggerTime(int time) {
    	this.triggerTime = time;
    }

    @Nullable
    @Override
    @Property(multiline = true, order = 8)
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
