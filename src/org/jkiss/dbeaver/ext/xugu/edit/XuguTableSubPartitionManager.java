package org.jkiss.dbeaver.ext.xugu.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
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
import org.jkiss.dbeaver.ext.xugu.model.XuguTablePhysical;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableSubPartition;
import org.jkiss.dbeaver.ext.xugu.views.XuguWarningDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
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
 * 二级分区管理器
 * 进行二级分区的创建和删除，修改仅支持设置是否在线
 * 包含一个内部界面类，用于进行属性设定
 */
public class XuguTableSubPartitionManager extends SQLObjectEditor<XuguTableSubPartition, XuguTablePhysical>{
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		// TODO Auto-generated method stub
		return FEATURE_SAVE_IMMEDIATELY;
	}
	
	@Override
    protected XuguTableSubPartition createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options)
    {
		XuguTablePhysical parent = (XuguTablePhysical)container;
		
		//仅允许对新创建的表进行添加二级分区操作
		if(parent.isPersisted()==false) {
			return new UITask<XuguTableSubPartition>() {
	            @Override
	            protected XuguTableSubPartition runTask() {
	            	NewTablePartitionDialog dialog = new NewTablePartitionDialog(UIUtils.getActiveWorkbenchShell(), monitor, parent);
	                if (dialog.open() != IDialogConstants.OK_ID) {
	                    return null;
	                }
	                XuguTableSubPartition newTablePartition = dialog.getTablePartition();
	                if(parent.isPersisted()) {
	                	ArrayList<XuguTableSubPartition> partList = (ArrayList<XuguTableSubPartition>) parent.subPartitionCache.getCachedObjects();
	                    if(partList.size()!=0) {
	                    	XuguTableSubPartition model = partList.get(0);
	                    	newTablePartition.setPartiType(model.getPartiType());
	                    	newTablePartition.setPartiKey(model.getPartiKey());
	                    }
	                }else {
	                	// 创建新表
	                }
	                if(newTablePartition.isSubPartition()) {
	                	parent.subPartitionCache.cacheObject(newTablePartition);
	                }
	                return newTablePartition;
	            }
			}.execute();
		}
		new UITask<String>() {
			@Override
			protected String runTask() {
				WarningDialog dialog2 = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "不能再没有任何分区的表上新建分区");       
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
		//表存在时，禁用二级分区操作
		if(command.getObject().getParentObject().isPersisted() == true) {
			new UITask<String>() {
				@Override
				protected String runTask() {
					WarningDialog dialog2 = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "You can not create subpartition on a table");       
	                if (dialog2.open() != IDialogConstants.OK_ID) {
	                    return null;
	                }
					return null;
				}
	    	}.execute();  
		}
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
    	//不允许对二级分区进行删除操作
    	if(command.getObject().getParentObject().isPersisted()==true) {
    		new UITask<String>() {
				@Override
				protected String runTask() {
					WarningDialog dialog2 = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "You can not drop subpartition on a table");       
	                if (dialog2.open() != IDialogConstants.OK_ID) {
	                    return null;
	                }
					return null;
				}
	    	}.execute();  
    	}
    	//若是新增表情况时则直接将改对象从缓存中剔除
    	else {
    		command.getObject().getParentObject().subPartitionCache.removeObject(command.getObject(), true);
    	}
    }
    
    @Override
    public void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException
    {
    	//当表存在时才可进行修改action
    	if(command.getObject().getParentObject().isPersisted() == true && command.getProperty("online") != null) {
    		StringBuilder sql = new StringBuilder("ALTER TABLE ");
        	sql.append(command.getObject().getParentObject().getName());
        	sql.append(" SET SUBPARTITION ");
        	sql.append(command.getObject().getName());
        	sql.append((boolean)command.getProperty("online")?" ONLINE":" OFFLINE");
        	
        	log.debug("[Xugu] Construct add subpartition sql: "+sql.toString());
        	actionList.add(new SQLDatabasePersistAction("Alter Partition", sql.toString()));
    	}
    }
    
    public static class WarningDialog extends Dialog{
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
    	
    	private XuguTableSubPartition partition;
    	private XuguTablePhysical table;
    	private DBRProgressMonitor monitor;
        private Text nameText;
        private Combo typeCombo;
        private Text valueText;
        private Combo colCombo;
        
        public NewTablePartitionDialog(Shell parentShell, DBRProgressMonitor monitor, XuguTablePhysical table)
        {
            super(parentShell);
            this.table = table;
            this.monitor = monitor;
        }
        
        public XuguTableSubPartition getTablePartition() {
        	return this.partition;
        }
        
        @Override
        protected boolean isResizable()
        {
            return true;
        }
        
        @Override
        protected Point getInitialSize() {
        	return new Point(300, 250);
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
            
            typeCombo = UIUtils.createLabelCombo(composite, "Type", 8);
            typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            typeCombo.add("LIST");
            typeCombo.add("RANGE");
            typeCombo.add("HASH");
            
            valueText = UIUtils.createLabelText(composite, XuguMessages.dialog_tablePartition_value, null);
            valueText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            colCombo = UIUtils.createLabelCombo(composite, "column", 8);
            colCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
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
            
            UIUtils.createInfoLabel(composite, XuguMessages.dialog_tablePartition_create_info, GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed()
        {
        	if(XuguUtils.checkString(nameText.getText())) {
        		this.partition = new XuguTableSubPartition(table, false ,"");
                partition.setName(DBObjectNameCaseTransformer.transformObjectName(partition, nameText.getText()));
                partition.setPartiType(DBObjectNameCaseTransformer.transformObjectName(partition, typeCombo.getText()));
                partition.setPartiValue(DBObjectNameCaseTransformer.transformObjectName(partition,valueText.getText())); 
                partition.setPartiKey(DBObjectNameCaseTransformer.transformObjectName(partition, colCombo.getText()));
                partition.setSubPartition(true);
                partition.setOnline(true);
                super.okPressed();
        	}else {
        		XuguWarningDialog warnDialog = new XuguWarningDialog(UIUtils.getActiveWorkbenchShell(), "Table Subpartition name cannot be null");
        		warnDialog.open();
        	}
        }

    }

	@Override
	public DBSObjectCache<? extends DBSObject, XuguTableSubPartition> getObjectsCache(XuguTableSubPartition object) {
		// TODO Auto-generated method stub
		return object.getParentObject().subPartitionCache;
	}
}
