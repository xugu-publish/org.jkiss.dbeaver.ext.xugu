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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
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
 * XuguTableTriggerManager
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
            source = "CREATE OR REPLACE TRIGGER "+
            		trigger.getName()+" \n"+
            		timing+" "+event+" ON "+trigger.getTable().getName()+" \n"+
            		type+" \n"+
            		source;
            if(XuguConstants.LOG_PRINT_LEVEL<1) {
            	log.info("Xugu Plugin: Construct create trigger sql: "+source);
            }
            actions.add(new SQLDatabasePersistAction("Create trigger", source, true)); //$NON-NLS-2$
        }
    }
    
    static class NewTriggerDialog extends Dialog {
    	
    	private XuguTrigger trigger;
        private Text nameText;
        private Combo typeCombo;
        private Combo objListCombo;
        private Combo triggerEventCombo;
        private Combo triggerTypeCombo;
        private Button triggerTimingBefore;
        private Button triggerTimingAfter;
        private List<String> tableList;
        private List<String> viewList;

        public NewTriggerDialog(Shell parentShell, XuguTableBase table, DBRProgressMonitor monitor)
        {
            super(parentShell);
            this.trigger = new XuguTrigger(table, ""); 
            XuguSchema parent = table.getSchema();
            tableList = new ArrayList<String>();
            viewList = new ArrayList<String>();
            try {
				Collection<XuguTable> allTables = parent.getTables(monitor);
				Iterator<XuguTable> it1 = allTables.iterator();
				while(it1.hasNext()) {
					tableList.add(it1.next().getName());
				}
				Collection<XuguView> allViews = parent.getViews(monitor);
				Iterator<XuguView> it2 = allViews.iterator();
				while(it2.hasNext()) {
					viewList.add(it2.next().getName());
				}
			} catch (DBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
        	return new Point(300, 400);
        }
        
        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText(XuguMessages.dialog_synonym_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, "Trigger name", null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            typeCombo = UIUtils.createLabelCombo(composite, "OBJECT TYPE", 0);
            typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            typeCombo.add("TABLE");
            typeCombo.add("VIEW");
            typeCombo.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					objListCombo.removeAll();
					if(e.text.equals("TABLE")) {
						for(String str:tableList) {
							objListCombo.add(str);
						}
					}else {
						for(String str:viewList) {
							objListCombo.add(str);
						}
					}
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
            });
            
            objListCombo = UIUtils.createLabelCombo(composite, "OBJECT LIST", 0);
            objListCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            triggerEventCombo = UIUtils.createLabelCombo(composite, "EVENT", 0);
            triggerEventCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerEventCombo.add("INSERT");
            triggerEventCombo.add("UPDATE");
            triggerEventCombo.add("DELETE");
            
            triggerTypeCombo = UIUtils.createLabelCombo(composite, "TYPE", 0);
            triggerTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerTypeCombo.add("ROW");
            triggerTypeCombo.add("STATEMENT");
            
            triggerTimingBefore = UIUtils.createRadioButton(composite, "Before", 1, new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					
				}
            });
            triggerTimingBefore.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerTimingAfter = UIUtils.createRadioButton(composite, "After", 2, new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					
				}
            });
            triggerTimingAfter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            UIUtils.createInfoLabel(composite, "Set Trigger settings", GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed()
        {
        	String source = "BEGIN\n\nEND";
            trigger.setName(DBObjectNameCaseTransformer.transformObjectName(trigger, nameText.getText()));
            trigger.setObjectType(typeCombo.getText());
            
            trigger.setTriggerTime(Integer.parseInt(triggerTimingBefore.getData().toString()));
            trigger.setTriggerType(triggerTypeCombo.getSelectionIndex()+1);
            int event=0;
            switch(triggerEventCombo.getText()) {
	            case "INSERT":
	            	event = 1;
	            	break;
	            case "UPDATE":
	            	event = 2;
	            	break;
	            case "DELETE":
	            	event = 4;
	            	break;
            	default:
            		event = -1;
            		break;
            }
            trigger.setTriggeringEvent(event);
            trigger.setObjectDefinitionText(source);
            super.okPressed();
        }

    }

}

