package org.jkiss.dbeaver.ext.xugu.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.XuguUtils;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableColumn;
import org.jkiss.dbeaver.ext.xugu.model.XuguTablePartition;
import org.jkiss.dbeaver.ext.xugu.model.XuguTablePhysical;
import org.jkiss.dbeaver.ext.xugu.views.XuguWarningDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * @author Maple4Real
 * 表分区管理器
 * 进行表分区的创建和删除，修改仅支持设定是否在线
 * 包含一个内部界面类，用于进行属性设定
 */
public class XuguTablePartitionManager extends SQLObjectEditor<XuguTablePartition, XuguTablePhysical>{
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		// TODO Auto-generated method stub
		return FEATURE_SAVE_IMMEDIATELY;
	}
	
	@Override
    protected XuguTablePartition createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options)
    {
		XuguTablePhysical parent = (XuguTablePhysical)container;
		//禁止对没有分区定义的表进行添加分区操作
		if(parent.partitionCache!=null || parent.isPersisted()==false) {
			return new UITask<XuguTablePartition>() {
	            @Override
	            protected XuguTablePartition runTask() {
	            	NewTablePartitionDialog dialog = new NewTablePartitionDialog(UIUtils.getActiveWorkbenchShell(), monitor, parent);
	                if (dialog.open() != IDialogConstants.OK_ID) {
	                    return null;
	                }
	                XuguTablePartition newTablePartition = dialog.getTablePartition();
	                if(parent.isPersisted()) {
	                	ArrayList<XuguTablePartition> partList = (ArrayList<XuguTablePartition>) parent.partitionCache.getCachedObjects();
	                    if(partList.size()!=0) {
	                    	XuguTablePartition model = partList.get(0);
	                    	newTablePartition.setPartiType(model.getPartiType());
	                    	newTablePartition.setPartiKey(model.getPartiKey());
	                    }
	                }
	                return newTablePartition;
	            }
			}.execute();
		}
		new UITask<String>() {
			@Override
			protected String runTask() {
				WarningDialog dialog2 = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "You can not add partition on a table without any partitions");       
                if (dialog2.open() != IDialogConstants.OK_ID) {
                    return null;
                }
				return null;
			}
    	}.execute();   
    	return null;
    }

	@Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
		//新建表时在tablemanager中进行添加处理
		//修改已存在表的分区时新增修改语句
		if(command.getObject().getParentObject().isPersisted() == true) {
			StringBuilder sql = new StringBuilder();
			
			sql.append("ALTER TABLE ");
	    	sql.append(command.getObject().getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
	    	sql.append(" ADD PARTITION ");
	    	sql.append(command.getObject().getName());
	    	switch(command.getObject().getPartiType()) {
	    	case "LIST":
	    		sql.append(" VALUES('");
		    	sql.append(command.getObject().getPartiValue());
		    	sql.append("')");
		    	break;
	    	case "RANGE":
	    		sql.append(" VALUES LESS THAN(");
		    	sql.append(command.getObject().getPartiValue());
		    	sql.append(")");
		    	break;
	    	case "AUTOMATIC":
	    		sql.append(" VALUES LESS THAN(");
	    		sql.append(command.getObject().getPartiValue());
	    		sql.append(")");
	    		break;
	    	}
	    	
	    	log.debug("[Xugu] Construct add table partition sql: "+sql.toString());
			actions.add(new SQLDatabasePersistAction("Modify table, Add Partition", sql.toString()));
		}
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
    	//当表存在时才可进行删除action
    	if(command.getObject().getParentObject().isPersisted() == true) {
    		StringBuilder sql = new StringBuilder("ALTER TABLE ");
    		sql.append(command.getObject().getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
        	sql.append(" DROP PARTITION ");
        	sql.append(command.getObject().getName());
        	
        	log.debug("[Xugu] Construct drop table partition sql: "+sql.toString());
        	actions.add(new SQLDatabasePersistAction("Drop Partition", sql.toString()));
    	}
    	//若是新增表情况时则直接将改对象从缓存中剔除
    	else {
    		command.getObject().getParentObject().partitionCache.removeObject(command.getObject(), true);
    	}
    }
    
    @Override
    public void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException
    {
    	//当表存在时才可进行修改action
    	if(command.getObject().getParentObject().isPersisted() == true && command.getProperty("online") != null) {
    		StringBuilder sql = new StringBuilder("ALTER TABLE ");
    		sql.append(command.getObject().getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
        	sql.append(" SET PARTITION ");
        	sql.append("\"" + command.getObject().getName()+ "\"");
        	sql.append((boolean)command.getProperty("online")?" ONLINE":" OFFLINE");
        	
        	log.debug("[Xugu] Construct alter table partition sql: "+sql.toString());
        	actionList.add(new SQLDatabasePersistAction("Alter Partition", sql.toString()));
    	}
    }
    
    static class WarningDialog extends Dialog{
    	private String warningInfo;
    	public WarningDialog(Shell parentShell, String info)
        {
    		super(parentShell);
    		this.warningInfo = info;
        }
    	@Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText(XuguMessages.dialog_tablePartition_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
            
            Label infoText = UIUtils.createLabel(composite, "Warning:"+this.warningInfo);
            infoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            return parent;
        }
    }
    
    static class NewTablePartitionDialog extends Dialog {
    	
    	private XuguTablePartition partition;
    	private XuguTablePhysical table;
    	private DBRProgressMonitor monitor;
        private Text nameText;
        private Combo typeCombo;
        private Text valueText;
        private Combo colCombo;
        private Button addCol;
        private Button removeCol;
        private Text colText;
        
        //自动扩展分区额外选项
        private Combo autoTypeCombo;
        private Text autoSpanText;
        
        public NewTablePartitionDialog(Shell parentShell, DBRProgressMonitor monitor, XuguTablePhysical table)
        {
            super(parentShell);
            this.table = table;
            this.monitor = monitor;
        }
        
        public XuguTablePartition getTablePartition() {
        	return this.partition;
        }
        
        @Override
        protected boolean isResizable()
        {
            return true;
        }
        
        @Override
        protected Point getInitialSize() {
        	return new Point(500, 450);
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText(XuguMessages.dialog_tablePartition_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, XuguMessages.dialog_tablePartition_name, null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            typeCombo = UIUtils.createLabelCombo(composite, XuguMessages.dialog_tablePartition_type, 0);
            typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            typeCombo.add("LIST");
            typeCombo.add("RANGE");
            typeCombo.add("HASH");
            typeCombo.add("AUTOMATIC");
            
            valueText = UIUtils.createLabelText(composite, XuguMessages.dialog_tablePartition_value, null);
            valueText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            colCombo = UIUtils.createLabelCombo(composite, XuguMessages.dialog_tablePartition_col_Combo_label, 0);
            colCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            //加载字段信息
            try {
				Collection<XuguTableColumn> cols = table.getAttributes(monitor);
				Iterator<XuguTableColumn> it = cols.iterator();		
				while(it.hasNext()) {
					String name = it.next().getName();
					colCombo.add(name);
				}
			} catch (DBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            addCol = UIUtils.createPushButton(composite, XuguMessages.dialog_tablePartition_add_col, null);
            addCol.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            removeCol = UIUtils.createPushButton(composite, XuguMessages.dialog_tablePartition_remove_col, null);
            removeCol.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            colText = UIUtils.createLabelText(composite, XuguMessages.dialog_tablePartition_col_Text_label, null);
            colText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            addCol.addSelectionListener(new SelectionListener() {
    			@Override
    			public void widgetSelected(SelectionEvent e) {
    				// TODO Auto-generated method stub
    				String text = colText.getText();
    				String newCol = colCombo.getText();
    				//追加新的字段 前提是新字段名不存在于字段文本框内容中
    				if(text!=null && !"".equals(text)) {
    					if(text.indexOf(newCol)==-1) {
    						text += ","+colCombo.getText();
    					}
    				}else {
    					text = colCombo.getText();
    				}
    				colText.setText(text);
    			}
    			@Override
    			public void widgetDefaultSelected(SelectionEvent e) {
    				// do nothing
    			}	
            });
            removeCol.addSelectionListener(new SelectionListener() {
    			@Override
    			public void widgetSelected(SelectionEvent e) {
    				// TODO Auto-generated method stub
    				String text = colText.getText();
    				String newCol = colCombo.getText();
    				//删除已有字段 前提是新字段名存在于字段文本框内容中
    				if(text!=null && !"".equals(text)) {
    					int index;
    					if((index = text.indexOf(newCol))!=-1) {
    						text = text.substring(0, index)+text.substring(index+newCol.length());
    						//处理首尾逗号
    						if(text.indexOf(",")==0) {
    							text = text.substring(1);
    						}else if(text.lastIndexOf(",")==text.length()-1) {
    							text = text.substring(0, text.length()-1);
    						}
    						//处理位于中间的逗号
    						text.replaceAll(",,", ",");
    					}
    				}
    				colText.setText(text);
    			}
    			@Override
    			public void widgetDefaultSelected(SelectionEvent e) {
    				// do nothing
    			}	
            });
            
            autoTypeCombo = UIUtils.createLabelCombo(composite, XuguMessages.dialog_tablePartition_col_AutoType_label, 0);
            autoTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            autoTypeCombo.add("YEAR");
            autoTypeCombo.add("MONTH");
            autoTypeCombo.add("DAY");
            autoTypeCombo.add("HOUR");
            
            autoSpanText = UIUtils.createLabelText(composite, XuguMessages.dialog_tablePartition_col_AutoSpan_label, null);
            autoSpanText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            //默认禁用autoTypeCombo和autoSpanText
            autoTypeCombo.setEnabled(false);
            autoSpanText.setEnabled(false);
            
            //如果表已存在且分区也存在则对属性进行预设
            if(table.isPersisted()) {
            	try {
					Collection<XuguTablePartition> parts = table.getPartitions(monitor);
					if(parts!=null) {
						if(parts.iterator().hasNext()) {
							XuguTablePartition part = parts.iterator().next();
							String partType = part.getPartiType();
							String partKey = part.getPartiKey();
							typeCombo.setText(partType);
							partKey = partKey.replaceAll("\"", "");
							colText.setText(partKey);
							typeCombo.setEnabled(false);
							colCombo.setEnabled(false);
							colText.setEnabled(false);
							addCol.setEnabled(false);
							removeCol.setEnabled(false);
							if("AUTOMATIC".equals(partType)) {
								autoTypeCombo.setText(part.getAutoPartiType());
								autoSpanText.setText(part.getAutoPartiSpan().toString());
							}
							autoTypeCombo.setEnabled(false);
							autoSpanText.setEnabled(false);
						}
					}
				} catch (DBException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
            //监听typeCombo，当属性为automatic时，运行设置自动扩展分区属性
            typeCombo.addSelectionListener(new SelectionListener() {
            	//根据选中类型做出动作
				@Override
				public void widgetSelected(SelectionEvent e) {
					String nowType = typeCombo.getText();
					if("AUTOMATIC".equals(nowType) || "HASH".equals(nowType)) {
						autoTypeCombo.setEnabled(true);
						autoSpanText.setEnabled(true);
						colText.setEnabled(false);
						addCol.setEnabled(false);
						removeCol.setEnabled(false);
					}else {
						autoTypeCombo.setEnabled(false);
						autoSpanText.setEnabled(false);
						colText.setEnabled(true);
						addCol.setEnabled(true);
						removeCol.setEnabled(true);
					}
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
            });
            
            UIUtils.createInfoLabel(composite, XuguMessages.dialog_tablePartition_create_info, GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed()
        {
    		if(XuguUtils.checkString(nameText.getText())) {
    			this.partition = new XuguTablePartition(table, false ,"");
                partition.setName(DBObjectNameCaseTransformer.transformObjectName(partition, nameText.getText()));
                partition.setPartiType(DBObjectNameCaseTransformer.transformObjectName(partition, typeCombo.getText()));
                partition.setPartiValue(DBObjectNameCaseTransformer.transformObjectName(partition,valueText.getText())); 
                partition.setSubPartition(false);
                partition.setOnline(true);
                if(autoTypeCombo.getText()!=null && !"".equals(autoTypeCombo.getText())) {
                	partition.setPartiKey(DBObjectNameCaseTransformer.transformObjectName(partition, colCombo.getText()));
                	partition.setAutoPartiType(autoTypeCombo.getText());
                	partition.setAutoPartiSpan(Integer.parseInt(autoSpanText.getText()));
                }else {
                	partition.setPartiKey(DBObjectNameCaseTransformer.transformObjectName(partition, colText.getText()));
                }
                super.okPressed();
    		}else {
    			XuguWarningDialog warnDialog = new XuguWarningDialog(UIUtils.getActiveWorkbenchShell(), "Partition name cannot be null");
        		warnDialog.open();
    		}
        }

    }

	@Override
	public DBSObjectCache<? extends DBSObject, XuguTablePartition> getObjectsCache(XuguTablePartition object) {
		// TODO Auto-generated method stub
		return object.getParentObject().partitionCache;
	}
}
