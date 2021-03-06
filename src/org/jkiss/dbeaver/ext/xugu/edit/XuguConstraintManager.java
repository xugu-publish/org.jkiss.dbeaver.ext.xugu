/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.xugu.edit;

import java.util.List;
import java.util.Map;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;

/**
 * @author Maple4Real
 * 约束管理器
 * 进行约束的增加删除和修改
 */
public class XuguConstraintManager extends SQLConstraintManager<XuguTableConstraint, XuguTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguTableConstraint> getObjectsCache(XuguTableConstraint object)
    {
        return object.getParentObject().getSchema().constraintCache;
    }
    
    @Override
    protected XuguTableConstraint createDatabaseObject( DBRProgressMonitor monitor, DBECommandContext context, final Object container,Object from,  Map<String, Object> options)
    {
    	XuguTableBase parent = (XuguTableBase) container;
    	 
        return new UITask<XuguTableConstraint>() {
            @Override
            protected XuguTableConstraint runTask() {
            	EditConstraintPage editPage = new EditConstraintPage(
                    XuguMessages.edit_xugu_constraint_manager_dialog_title,
                    parent,
                    new DBSEntityConstraintType[] {
                        DBSEntityConstraintType.PRIMARY_KEY,
                        DBSEntityConstraintType.UNIQUE_KEY,
                        DBSEntityConstraintType.CHECK},
                    	true
                    );
                if (!editPage.edit()) {
                    return null;
                }

                final XuguTableConstraint constraint = new XuguTableConstraint(
                    parent,
                    editPage.getConstraintName(),
                    editPage.getConstraintType(),
                    editPage.getConstraintExpression(),
                    editPage.isEnableConstraint() ? XuguObjectStatus.ENABLED : XuguObjectStatus.DISABLED);
                constraint.setEnable(constraint.getStatus() == XuguObjectStatus.ENABLED);
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    constraint.addColumn(
                        new XuguTableConstraintColumn(
                            constraint,
                            (XuguTableColumn) tableColumn,
                            colIndex++));
                }
                return constraint;
            }
        }.execute();
    }

    @NotNull
    @Override
    protected String getAddConstraintTypeClause(XuguTableConstraint constraint) {
        if (constraint.getConstraintType() == DBSEntityConstraintType.UNIQUE_KEY) {
            return "UNIQUE"; //$NON-NLS-1$
        }
        return super.getAddConstraintTypeClause(constraint);
    }
    
    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions,
                                          ObjectCreateCommand command, Map<String, Object> options)
    {
    	XuguTableConstraint constraint = (XuguTableConstraint) command.getObject();
    	XuguTableBase table = constraint.getTable();
    	StringBuilder decl = new StringBuilder(100);
    	decl.append("ALTER TABLE ");
    	decl.append(table.getFullyQualifiedName(DBPEvaluationContext.DDL));
    	decl.append(" ADD ");
    	decl.append(getNestedDeclaration(monitor, table, command, options));
    	decl.append(";");
    	decl.append(CommonUtils.getLineSeparator());
    	
    	decl.append("ALTER TABLE ");
    	decl.append(table.getFullyQualifiedName(DBPEvaluationContext.DDL));
    	decl.append(constraint.isEnable()? " ENABLE" : " DISABLE");
    	decl.append(" CONSTRAINT ");
    	decl.append(constraint.getName());

    	log.debug("[Xugu] Construct create constraint sql: "+ decl.toString());
        actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_constraint, decl.toString()));
    }
    
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
    	XuguTableConstraint constraint = (XuguTableConstraint) command.getObject();
    	XuguTableBase table = constraint.getTable();
    	String sql = "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + (constraint.isEnable()? " ENABLE" : " DISABLE") +" CONSTRAINT "+ constraint.getName();

    	log.debug("[Xugu] Construct alter constraint sql: "+sql);
    	actionList.add(
                new SQLDatabasePersistAction(
                		"Alter constraint", sql
                	));
    }
    
    //重写组装check条件的语句
    @Override
    protected void appendConstraintDefinition(StringBuilder decl, DBECommandAbstract<XuguTableConstraint> command) {
    	String type[] = decl.toString().split(" ");
    	decl.append(" (");
    	if (type[2].equals("CHECK")) {
    		decl.append(command.getObject().getSearchCondition());
		} else {
			List<? extends DBSEntityAttributeRef> attrs = command.getObject().getAttributeReferences(new VoidProgressMonitor());
			if (attrs != null) {
				boolean firstColumn = true;
				for (DBSEntityAttributeRef constraintColumn : attrs) {
					final DBSEntityAttribute attribute = constraintColumn.getAttribute();
					if (attribute == null) {
						continue;
					}
					if (!firstColumn) decl.append(","); //$NON-NLS-1$
					firstColumn = false;
					decl.append(DBUtils.getQuotedIdentifier(attribute));
				}
			}			
		}
    	decl.append(")");
    }
}
