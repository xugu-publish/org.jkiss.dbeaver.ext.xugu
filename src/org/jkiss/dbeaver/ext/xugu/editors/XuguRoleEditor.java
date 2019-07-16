package org.jkiss.dbeaver.ext.xugu.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.edit.RolePropertyHandler;
import org.jkiss.dbeaver.ext.xugu.edit.UserPropertyHandler;
import org.jkiss.dbeaver.ext.xugu.editors.XuguUserEditorAbstract.UserPageControl;
import org.jkiss.dbeaver.ext.xugu.model.XuguAuthorityBase;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.XuguRole;
import org.jkiss.dbeaver.ext.xugu.model.XuguRoleAuthority;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchema;
import org.jkiss.dbeaver.ext.xugu.model.XuguUserAuthority;
import org.jkiss.dbeaver.ext.xugu.views.XuguWarningDialog;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl.ProgressVisualizer;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.ControlPropertyCommandListener;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;

public class XuguRoleEditor extends AbstractDatabaseObjectEditor<XuguRole>{
    
	private PageControl pageControl;
    private boolean isLoaded;
    private boolean newUser;
    private Text roleNameText;
    
    Collection<XuguRoleAuthority> authorities;
	ArrayList<String> databaseAuthorities;
	ArrayList<String> objectAuthorities;
    
    //private Text hostText;
    private CommandListener commandlistener;

    @Override
    public void createPartControl(Composite parent) 
    {
    	
        pageControl = new PageControl(parent);
        Composite container = UIUtils.createPlaceholder(pageControl, 4, 5);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        container.setLayoutData(gd);
        container.setSize(400, 300);

        CTabFolder cf1=new CTabFolder(container,0);
        CTabItem ti1 = new CTabItem(cf1, 1);
        CTabItem ti2 = new CTabItem(cf1, 2);
        CTabItem ti3 = new CTabItem(cf1, 3);
        Composite roleGroup = UIUtils.createControlGroup(cf1, XuguMessages.editors_role_editor_general_label_role_properties_title, 2, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        roleGroup.setSize(200, 200);
        Composite roleGroup2 = UIUtils.createControlGroup(cf1, XuguMessages.editors_authority_editor_database_title, 1, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        roleGroup2.setSize(200, 200);
        Composite roleGroup3 = UIUtils.createControlGroup(cf1, XuguMessages.editors_authority_editor_object_title, 2, GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL, 400);
        roleGroup3.setSize(860, 250);
        
        ti1.setControl(roleGroup);
        ti1.setText(XuguMessages.editors_role_editor_general_label_role_properties_title);
        ti2.setControl(roleGroup2);
        ti2.setText(XuguMessages.editors_authority_editor_database_title);
        ti3.setControl(roleGroup3);
        ti3.setText(XuguMessages.editors_authority_editor_object_title);
        cf1.setSelection(0);
        
        roleNameText = UIUtils.createLabelText(roleGroup, XuguMessages.dialog_role_name, getDatabaseObject().getName());
        roleNameText.setEditable(false);
    	
    	//权限处理
    	{
    		//加载用户中的权限信息并分为库级权限和对象级权限两类 对象权限又分为两个级别
    		authorities = getDatabaseObject().getRoleDatabaseAuthorities();
    		databaseAuthorities = new ArrayList<>();
    		objectAuthorities = new ArrayList<>();
    		
    		XuguAuthorityEditorBase baseEditor = new XuguAuthorityEditorBase(roleGroup2, roleGroup3, 2);
    		baseEditor.setRoleEditor(this);	
    		if(authorities!=null) {
    			Iterator<XuguRoleAuthority> it = authorities.iterator();
    			XuguRoleAuthority authority;
    			while(it.hasNext()) {
    				authority = it.next();
    				if(authority.isDatabase()) {
    					databaseAuthorities.add(authority.getName());
    				}else {
    					objectAuthorities.add(authority.getName());
    				}
    			}
    		}
    		baseEditor.loadDatabaseAuthorities(databaseAuthorities, objectAuthorities);
    		baseEditor.loadDatabaseAuthorityView();
    		baseEditor.loadObjectAuthorityView(getDatabaseObject());
    	}
    	
        pageControl.createProgressPanel();

        commandlistener = new CommandListener();
        getEditorInput().getCommandContext().addCommandListener(commandlistener);
    }

    public void listNotify(org.eclipse.swt.widgets.List target) {
    	target.notifyListeners(SWT.Modify, null);
    }
    
    @Override
    public void dispose()
    {
        if (commandlistener != null) {
            getEditorInput().getCommandContext().removeCommandListener(commandlistener);
        }
        super.dispose();
    }

    @Override
    public void activatePart()
    {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        
        LoadingJob.createService(
            new DatabaseLoadService<List<String>>("test", getExecutionContext()) {
				@Override
				public List<String> evaluate(DBRProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					// TODO Auto-generated method stub
					return null;
				}
            },
            pageControl.createLoadVisualizer())
            .schedule();
    }

    protected PageControl getPageControl()
    {
        return pageControl;
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        // do nothing
    }

    private class PageControl extends ObjectEditorPageControl {
        public PageControl(Composite parent) {
            super(parent, SWT.NONE, XuguRoleEditor.this);
        }
        public ProgressVisualizer<List<String>> createLoadVisualizer() {
            return new ProgressVisualizer<List<String>>() {
                @Override
                public void completeLoading(List<String> privs) {
                    super.completeLoading(privs);
//	                    privTable.fillPrivileges(privs);
//	                    loadGrants();
                }
            };
        }
        @Override
        public void fillCustomActions(IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);
            DatabaseEditorUtils.contributeStandardEditorActions(getSite(), contributionManager);
        }
    }

    private class CommandListener extends DBECommandAdapter {
        @Override
        public void onSave()
        {
            if (newUser && getDatabaseObject().isPersisted()) {
                newUser = false;
                UIUtils.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        }
    }

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}
    
}
