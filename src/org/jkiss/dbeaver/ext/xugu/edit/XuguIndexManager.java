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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableColumn;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableIndex;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableIndexColumn;
import org.jkiss.dbeaver.ext.xugu.model.XuguTablePhysical;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;
import org.jkiss.utils.CommonUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Maple4Real
 * 索引管理器
 * 进行索引的增加和删除
 */
public class XuguIndexManager extends SQLIndexManager<XuguTableIndex, XuguTablePhysical> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguTableIndex> getObjectsCache(XuguTableIndex object)
    {
        return object.getParentObject().getSchema().indexCache;
    }

    @Override
    protected XuguTableIndex createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container,Object from,  Map<String, Object> options)
    {
    	XuguTablePhysical table = (XuguTablePhysical) container;
    	
    	final XuguTableIndex index = new XuguTableIndex(
            table.getSchema(),
            table,
            "INDEX",
            true,
            DBSIndexType.UNKNOWN);
        return new UITask<XuguTableIndex>() {
            @Override
            protected XuguTableIndex runTask() {
            	List<DBSIndexType> indexTypes = new ArrayList<>();
            	indexTypes.add(XuguConstants.INDEX_TYPE_BTREE);
            	indexTypes.add(XuguConstants.INDEX_TYPE_RTREE);
            	indexTypes.add(XuguConstants.INDEX_TYPE_FULL_TEXT);
            	EditIndexPage editPage = new EditIndexPage(
                    XuguMessages.edit_xugu_index_manager_dialog_title,
                    index,
                    indexTypes);
                if (!editPage.edit()) {
                    return null;
                }

                StringBuilder idxName = new StringBuilder(64);
                idxName.append(CommonUtils.escapeIdentifier(table.getName())).append("_") //$NON-NLS-1$
                    .append(CommonUtils.escapeIdentifier(editPage.getSelectedAttributes().iterator().next().getName()))
                    .append("_IDX"); //$NON-NLS-1$
                index.setName(DBObjectNameCaseTransformer.transformName(table.getDataSource(), idxName.toString()));
                index.setUnique(editPage.isUnique());
                index.setIndexType(editPage.getIndexType());
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
            		index.addColumn(
                            new XuguTableIndexColumn(
                                index,
                                (XuguTableColumn) tableColumn,
                                colIndex++,
                                !Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, EditIndexPage.PROP_DESC)),
                                null));
                }
                return index;
            }
        }.execute();
    }
    
    //重新组装创建index语句，增加local关键字
    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
    	super.addObjectCreateActions(monitor, actions, command, options);
    	String sql = actions.get(0).getScript();
    	actions.remove(0);
    	XuguTableIndex index = command.getObject();
    	if(index.isIs_local()) {
    		sql += " LOCAL";
    	}
    	actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_index, sql));
    }
    
    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_index,
                getDropIndexPattern(command.getObject())
                    .replace(PATTERN_ITEM_TABLE, command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))
                    .replace(PATTERN_ITEM_INDEX, command.getObject().getName())
                    .replace(PATTERN_ITEM_INDEX_SHORT, DBUtils.getQuotedIdentifier(command.getObject())))
        );
        String t = getDropIndexPattern(command.getObject())
                .replace(PATTERN_ITEM_TABLE, command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))
                .replace(PATTERN_ITEM_INDEX, command.getObject().getName())
                .replace(PATTERN_ITEM_INDEX_SHORT, DBUtils.getQuotedIdentifier(command.getObject()));
    }

    @Override
    protected String getDropIndexPattern(XuguTableIndex index)
    {
        return "DROP INDEX " + PATTERN_ITEM_TABLE + "." + PATTERN_ITEM_INDEX; //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    //为了设置local属性实现的继承自EditIndexPage的界面类
    private class InnerIndexPage extends EditIndexPage{
    	private Combo globalCombo;
    	private boolean flag;
    	
		public InnerIndexPage(String title, DBSTableIndex index, Collection<DBSIndexType> indexTypes) {
			super(title, index, indexTypes);
			// TODO Auto-generated constructor stub
		}
    	
		@Override
	    protected void createContentsBeforeColumns(Composite panel)
	    {
			super.createContentsBeforeColumns(panel);
			 UIUtils.createControlLabel(panel, "Is Local");
		     globalCombo = new Combo(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
		     globalCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		     globalCombo.add("GLOBAL");
		     globalCombo.add("LOCAL");
		     globalCombo.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					String text = globalCombo.getText();
					if("GLOBAL".equals(text)) {
						flag =  false;
					}else {
						flag = true;
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
		    	 
		     });
	    }
		
		protected boolean isLocal() {
			return flag;
		}
    }
}
