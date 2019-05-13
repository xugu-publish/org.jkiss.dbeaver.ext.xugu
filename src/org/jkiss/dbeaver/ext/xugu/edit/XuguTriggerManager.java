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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.edit.XuguSynonymManager.NewSynonymDialog;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguSynonym;
import org.jkiss.dbeaver.ext.xugu.model.XuguTable;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableBase;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableColumn;
import org.jkiss.dbeaver.ext.xugu.model.XuguTrigger;
import org.jkiss.dbeaver.ext.xugu.model.XuguUtils;
import org.jkiss.dbeaver.ext.xugu.model.XuguView;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Iterator;

/**
 * @author Maple4Real
 * 触发器管理器
 * 进行触发器的创建和删除，修改相当于创建并替换
 * 包含一个内部界面类，用于进行属性设定
 */
public class XuguTriggerManager extends SQLTriggerManager<XuguTrigger, XuguTableBase> {
	private final static Pattern PATTERN_TRIGGER = Pattern.compile("(TRIGGER)", Pattern.CASE_INSENSITIVE);
    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, XuguTrigger> getObjectsCache(XuguTrigger object)
    {
        return object.getTable().triggerCache;
    }

    @Override
    protected XuguTrigger createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final XuguTableBase parent, Object copyFrom)
    {
        return new UITask<XuguTrigger>() {
            @Override
            protected XuguTrigger runTask() {
            	NewTriggerDialog dialog = new NewTriggerDialog(UIUtils.getActiveWorkbenchShell(), parent, monitor);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                XuguTrigger newTrigger = dialog.getTrigger();
                return newTrigger;
            }
        }.execute();
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
    	String sql = "DROP TRIGGER " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL);
    	if(XuguConstants.LOG_PRINT_LEVEL<1) {
        	log.info("Xugu Plugin: Construct drop trigger sql: "+sql);
        }
        actions.add(
            new SQLDatabasePersistAction("Drop trigger",sql) //$NON-NLS-2$
        );
    }

    @Override
    protected void createOrReplaceTriggerQuery(List<DBEPersistAction> actions, XuguTrigger trigger)
    {
        String source = XuguUtils.normalizeSourceName(trigger, false);
        if (source == null || source.equals("")) {
            return;
        }
        else{
            String event = trigger.getTriggeringEvent();
            String timing = trigger.getTriggerTime();
            String type = trigger.getTriggerType();
            List<String> includeCols = trigger.getIncludeColumns();
            String targetCols = "";
            if(includeCols!=null) {
            	for(String str:includeCols) {
            		targetCols+="\""+str+"\""+",";
            	}
            	if(!"".equals(targetCols)) {
            		targetCols = targetCols.substring(0, targetCols.length()-1);
            		targetCols = " OF "+targetCols;
            	}
            }
            //处理触发器事件字段
            if(event.indexOf(",")!=-1) {
            	event = event.replaceAll(",", " OR ");
            }
            source = "CREATE OR REPLACE TRIGGER "+
            		trigger.getName()+" \n"+
            		timing+" "+event+targetCols+
            		" ON "+trigger.getTable().getName()+" \n"+
            		type+" \n"+
            		source;
            if(XuguConstants.LOG_PRINT_LEVEL<1) {
            	log.info("Xugu Plugin: Construct create trigger sql: "+source);
            }
            actions.add(new SQLDatabasePersistAction("Create trigger", source, true)); //$NON-NLS-2$
            trigger.setPersisted(true);
        }
    }
    
    static class NewTriggerDialog extends Dialog {
    	private DBRProgressMonitor monitor;
    	private XuguTrigger trigger;
    	private XuguTableBase table;
        private Text nameText;
        private Text parentTypeText;
        private Text parentNameText;
        private Combo triggerTypeCombo;
        private Button triggerEventInsert;
        private Button triggerEventUpdate;
        private Button triggerEventDelete;
        private Button triggerTimingBefore;
        private Button triggerTimingAfter;
        private Table colListTable;
        private Collection<XuguTableColumn> colList;
        
        public NewTriggerDialog(Shell parentShell, XuguTableBase table, DBRProgressMonitor monitor)
        {
            super(parentShell);
            this.monitor = monitor;
            this.table = table;
            XuguSchema parent = table.getSchema();
            colList = new ArrayList<>();
        }

        public XuguTrigger getTrigger()
        {
            return trigger;
        }
        
        @Override
        protected boolean isResizable()
        {
            return true;
        }
        
        @Override
        protected Point getInitialSize() {
        	return new Point(450, 550);
        }
        
        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText(XuguMessages.dialog_synonym_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 1, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
            
            nameText = UIUtils.createLabelText(composite, "Trigger name", null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            parentTypeText = UIUtils.createLabelText(composite, "PARENT TYPE", null);
            parentTypeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            parentTypeText.setText(table.getType()==0?"TABLE":"VIEW");
            parentTypeText.setEditable(false);
            
            parentNameText = UIUtils.createLabelText(composite, "PARENT NAME", null);
            parentNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            parentNameText.setText(table.getName());
            parentNameText.setEditable(false);
            
            Composite eventBox = UIUtils.createPlaceholder(composite, 4, 1);
            eventBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            CLabel eventLabel = UIUtils.createInfoLabel(eventBox, "Trigger Event:");
            eventLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerEventInsert = UIUtils.createCheckbox(eventBox, "Insert", false);
            triggerEventInsert.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerEventUpdate = UIUtils.createCheckbox(eventBox, "Update", false);
            triggerEventUpdate.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerEventDelete = UIUtils.createCheckbox(eventBox, "Delete", false);
            triggerEventDelete.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            triggerTypeCombo = UIUtils.createLabelCombo(composite, "TYPE", 0);
            triggerTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerTypeCombo.add("ROW");
            triggerTypeCombo.add("STATEMENT");
            
            Composite timeBox = UIUtils.createPlaceholder(composite, 3, 1);
            timeBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            CLabel timeLabel = UIUtils.createInfoLabel(timeBox, "Trigger Timing:");
            timeLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerTimingBefore = UIUtils.createRadioButton(timeBox, "Before", 1, null);
            triggerTimingBefore.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerTimingAfter = UIUtils.createRadioButton(timeBox, "After", 2, null);
            triggerTimingAfter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            colListTable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
            colListTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            colListTable.setHeaderVisible(true);
            colListTable.setLinesVisible(true);
            colListTable.setHeaderVisible(true);
            colListTable.setLinesVisible(true);
            String[] tableHeader = {"列名", "数据类型", "精度", "标度", "默认值"};  
            for (int i = 0; i < tableHeader.length; i++)  
            {  
                TableColumn tableColumn = new TableColumn(colListTable, SWT.NONE);  
                tableColumn.setText(tableHeader[i]);  
                // 设置表头可移动，默认为false  
                tableColumn.setMoveable(true);  
            } 
            //动态加载所有列信息
            triggerEventUpdate.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String objType = parentTypeText.getText();
					String objName = parentNameText.getText();
					if(!triggerEventUpdate.getSelection()) {
						//清空列信息
						colListTable.removeAll();
					}
					else{
						if(objType!=null && !"".equals(objType) && objName!=null && !"".equals(objName)){
							try {
								colList = table.getAttributes(monitor);
							} catch (DBException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							//重新加载数据
							if(colList.size()!=0) {
								Iterator<XuguTableColumn> it = colList.iterator();
								while(it.hasNext()){
									XuguTableColumn col = it.next();
									TableItem item = new TableItem(colListTable, SWT.NONE);  
									item.setText(new String[] {col.getName(), col.getDataType().toString(), col.getPrecision()+"", col.getScale()+"", col.getDefaultValue()});
								}
							}
							//调整表格大小
							for (int i = 0; i < tableHeader.length; i++)  
				            {  
				            	colListTable.getColumn(i).pack();  
				            }
						}
					}
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
            });
            UIUtils.createInfoLabel(composite, "Set Trigger settings", GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed()
        {
        	String source = "\nBEGIN\n\nEND";
            //设置父对象信息
            this.trigger = new XuguTrigger(table, ""); 

			trigger.setName(DBObjectNameCaseTransformer.transformObjectName(trigger, nameText.getText()));
            trigger.setObjectType(parentTypeText.getText());
            trigger.setTriggerTime(Integer.parseInt(triggerTimingBefore.getData().toString()));
            if(triggerTypeCombo.getSelectionIndex()>-1) {
            	trigger.setTriggerType(triggerTypeCombo.getSelectionIndex()+1);
            }
            int event=0;
            if(triggerEventInsert.getSelection()) {
            	event += 1;
            }
            if(triggerEventUpdate.getSelection()) {
            	event += 2;
            }
            if(triggerEventDelete.getSelection()) {
            	event += 4;
            }
            trigger.setTriggeringEvent(event);
            trigger.setObjectDefinitionText(source);
            //加载列信息
            TableItem[] cols = colListTable.getItems();
            if(cols!=null) {
            	int sum = cols.length;
                int i=0;
                ArrayList<String> includeCols = new ArrayList<String>();
                while(i<sum) {
                	if(cols[i].getChecked()) {
                		includeCols.add(cols[i].getText(0));
                	}
                	i++;
                }
                trigger.setIncludeColumns(includeCols);
            }
            super.okPressed();
        }

    }

}

