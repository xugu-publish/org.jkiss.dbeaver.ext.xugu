package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSParameter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.ResultSet;

/**
 * @author Maple4Real
 *   作业参数信息类
 */
public class XuguSchedulerJobArgument implements DBSParameter
{
    private final XuguSchedulerJob job;
    private String name;
    private String paramDefine;
    private int paramNum;
    
    private int position;
    private final String type;
    private String value;
    private String anyDataValue;
    private String outArgument;

    public XuguSchedulerJobArgument(
        XuguSchedulerJob job,
        ResultSet dbResult)
    {
        this.job = job;
        this.paramNum = JDBCUtils.safeGetInt(dbResult, "JOB_PARAM_NUM");
        this.paramDefine = JDBCUtils.safeGetString(dbResult, "JOB_ACTION");
        this.type = "";
        System.out.println("JOB_PARAM"+this.paramDefine);
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @NotNull
    @Override
    public XuguDataSource getDataSource()
    {
        return job.getDataSource();
    }

    @Override
    public XuguSchedulerJob getParentObject()
    {
        return job;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 10)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 11)
    public int getPosition()
    {
        return position;
    }

    @Property(viewable = true, order = 12)
    public String getType() {
        return type;
    }

    @Property(viewable = true, order = 14)
    public String getValue() {
        return value;
    }

    @Property(viewable = true, order = 15)
    public String getAnyDataValue() {
        return anyDataValue;
    }

    @Property(viewable = true, order = 16)
    public String getOutArgument() {
        return outArgument;
    }

    @NotNull
    @Override
    public DBSTypedObject getParameterType() {
        return getDataSource().getLocalDataType(type);
    }

}
