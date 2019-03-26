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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableForeignKey;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * OracleTableForeignKey
 */
public class XuguTableForeignKey extends XuguTableConstraintBase implements DBSTableForeignKey
{
    private static final Log log = Log.getLog(XuguTableForeignKey.class);

    private XuguTableConstraint referencedKey;
    private DBSForeignKeyModifyRule deleteRule;
    private DBSForeignKeyModifyRule updateRule;

    public XuguTableForeignKey(
        @NotNull XuguTableBase xuguTable,
        @Nullable String name,
        @Nullable XuguObjectStatus status,
        @NotNull XuguTableConstraint referencedKey,
        @NotNull DBSForeignKeyModifyRule deleteRule,
        @NotNull DBSForeignKeyModifyRule updateRule)
    {
        super(xuguTable, name, DBSEntityConstraintType.FOREIGN_KEY, status, false);
        this.referencedKey = referencedKey;
        this.deleteRule = deleteRule;
        this.updateRule = updateRule;
    }

    public XuguTableForeignKey(
        DBRProgressMonitor monitor,
        XuguTable table,
        ResultSet dbResult)
        throws DBException
    {
        super(
            table,
            JDBCUtils.safeGetString(dbResult, "CONS_NAME"),
            DBSEntityConstraintType.FOREIGN_KEY,
            JDBCUtils.safeGetBoolean(dbResult, "ENABLE") && JDBCUtils.safeGetBoolean(dbResult, "VALID")?XuguObjectStatus.ENABLED:XuguObjectStatus.DISABLED,
            true);

        String refName = JDBCUtils.safeGetString(dbResult, "REF_NAME");
        String refOwnerName = JDBCUtils.safeGetString(dbResult, "SCHEMA_NAME");
        String refTableName = JDBCUtils.safeGetString(dbResult, "REF_TABLE_NAME");
        System.out.println("True foreign info "+refOwnerName+" "+refTableName+" "+refName);
        XuguTableBase refTable = XuguTableBase.findTable(
            monitor,
            table.getDataSource(),
            refOwnerName,
            refTableName);
        if (refTable == null) {
            log.warn("Referenced table '" + DBUtils.getSimpleQualifiedName(refOwnerName, refTableName) + "' not found");
        } else {
            referencedKey = refTable.getConstraint(monitor, refName);
            if (referencedKey == null) {
                log.warn("Referenced constraint '" + refName + "' not found in table '" + refTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                referencedKey = new XuguTableConstraint(refTable, "refName", DBSEntityConstraintType.UNIQUE_KEY, null, XuguObjectStatus.ERROR);
            }
        }
        
        String updateAction = JDBCUtils.safeGetString(dbResult, "UPDATE_ACTION");
        switch(updateAction) {
	        case "n":
	        	this.updateRule = DBSForeignKeyModifyRule.NO_ACTION;
	        	break;
	        case "u":
	        	this.updateRule = DBSForeignKeyModifyRule.SET_NULL;
	        	break;
	        case "d":
	        	this.updateRule = DBSForeignKeyModifyRule.SET_DEFAULT;
	        	break;
	        case "c":
	        	this.updateRule = DBSForeignKeyModifyRule.CASCADE;
	        	break;
	        default:
	        	this.updateRule = DBSForeignKeyModifyRule.NO_ACTION;
	        	break;
        }
        
        String deleteAction = JDBCUtils.safeGetString(dbResult, "DELETE_ACTION");
        switch(deleteAction) {
	        case "n":
	        	this.deleteRule = DBSForeignKeyModifyRule.NO_ACTION;
	        	break;
	        case "u":
	        	this.deleteRule = DBSForeignKeyModifyRule.SET_NULL;
	        	break;
	        case "d":
	        	this.deleteRule = DBSForeignKeyModifyRule.SET_DEFAULT;
	        	break;
	        case "c":
	        	this.deleteRule = DBSForeignKeyModifyRule.CASCADE;
	        	break;
	        default:
	        	this.deleteRule = DBSForeignKeyModifyRule.NO_ACTION;
	        	break;
        }
    }

    @Property(viewable = true, order = 3)
    public XuguTableBase getReferencedTable()
    {
        return referencedKey == null ? null : referencedKey.getTable();
    }

    @Nullable
    @Override
    @Property(id = "reference", viewable = true, order = 4)
    public XuguTableConstraint getReferencedConstraint()
    {
        return referencedKey;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = false, listProvider = ConstraintModifyRuleListProvider.class, order = 5)
    public DBSForeignKeyModifyRule getDeleteRule()
    {
        return deleteRule;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = false, listProvider = ConstraintModifyRuleListProvider.class, order = 6)
    public DBSForeignKeyModifyRule getUpdateRule()
    {
        return updateRule;
    }

    @Override
    public XuguTableBase getAssociatedEntity()
    {
        return getReferencedTable();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            getTable(),
            this);
    }

    public static class ConstraintModifyRuleListProvider implements IPropertyValueListProvider<JDBCTableForeignKey> {

        @Override
        public boolean allowCustomValue()
        {
            return false;
        }

        @Override
        public Object[] getPossibleValues(JDBCTableForeignKey foreignKey)
        {
            return new DBSForeignKeyModifyRule[] {
                DBSForeignKeyModifyRule.NO_ACTION,
                DBSForeignKeyModifyRule.CASCADE,
                DBSForeignKeyModifyRule.SET_NULL,
                DBSForeignKeyModifyRule.SET_DEFAULT };
        }
    }
}
