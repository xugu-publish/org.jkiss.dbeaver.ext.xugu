/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;

/**
 * Oracle sequence
 */
public class XuguSequence extends XuguSchemaObject implements DBSSequence {

    
    private long cacheSize;
    private BigDecimal lastValue;
    
    private int seq_id;
    private String seq_name;
    private boolean flagCycle;
    private boolean flagOrder;
    private int cacheVal;
    private BigDecimal curVal;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private BigDecimal incrementBy;
    private Date createTime;
    private boolean is_sys;
    private boolean valid;
    private boolean deleted;
    private String comments;
    
    public XuguSequence(XuguSchema schema, String name) {
        super(schema, name, false);
        this.minValue = null;
        this.maxValue = null;
        this.incrementBy = new BigDecimal(0);
        this.cacheSize = 0;
        this.lastValue = new BigDecimal(0);
        this.flagCycle = false;
        this.flagOrder = false;
    }

    public XuguSequence(XuguSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "SEQ_NAME"), true);
        this.minValue = JDBCUtils.safeGetBigDecimal(dbResult, "MIN_VAL");
        this.maxValue = JDBCUtils.safeGetBigDecimal(dbResult, "MAX_VAL");
        this.incrementBy = JDBCUtils.safeGetBigDecimal(dbResult, "STEP_VAL");
        
        this.flagCycle = JDBCUtils.safeGetBoolean(dbResult, "IS_CYCLE");
        this.flagOrder = JDBCUtils.safeGetBoolean(dbResult, "IS_ORDER");
        this.seq_id = JDBCUtils.safeGetInt(dbResult, "SEQ_ID");
        this.seq_name = JDBCUtils.safeGetString(dbResult, "SEQ_NAME");
        this.cacheVal = JDBCUtils.safeGetInt(dbResult, "CACHE_VAL");
        this.curVal = JDBCUtils.safeGetBigDecimal(dbResult, "CURR_VAL");
        this.createTime = JDBCUtils.safeGetDate(dbResult, "CREATE_TIME");
        this.is_sys = JDBCUtils.safeGetBoolean(dbResult, "IS_SYS");
        this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
        this.deleted = JDBCUtils.safeGetBoolean(dbResult, "DELETED");
        this.comments = JDBCUtils.safeGetString(dbResult, "COMMENTS");
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 2)
    public BigDecimal getCurValue()
    {
        return curVal;
    }

    public void setCurValue(BigDecimal curVal) {
        this.curVal = curVal;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 3)
    public BigDecimal getMinValue()
    {
        return minValue;
    }

    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 4)
    public BigDecimal getMaxValue()
    {
        return maxValue;
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 5)
    public BigDecimal getIncrementBy()
    {
        return incrementBy;
    }

    public void setIncrementBy(BigDecimal incrementBy) {
        this.incrementBy = incrementBy;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 6)
    public int getCacheValue()
    {
        return cacheVal;
    }

    public void setCacheValue(int cacheVal) {
        this.cacheVal = cacheVal;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 7)
    public boolean isCycle()
    {
        return flagCycle;
    }

    public void setCycle(boolean flagCycle) {
        this.flagCycle = flagCycle;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 8)
    public boolean isOrder()
    {
        return flagOrder;
    }

    public void setOrder(boolean flagOrder) {
        this.flagOrder = flagOrder;
    }

	@Override
	public Number getLastValue() {
		// TODO Auto-generated method stub
		return getCurValue();
	}
}
