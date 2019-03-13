//package org.jkiss.dbeaver.ext.xugu.editors;
//
//import java.lang.reflect.InvocationTargetException;
//import java.util.List;
//
//import org.eclipse.jface.action.IContributionManager;
//import org.eclipse.swt.SWT;
//import org.eclipse.swt.layout.GridData;
//import org.eclipse.swt.widgets.Composite;
//import org.eclipse.swt.widgets.Text;
//import org.jkiss.dbeaver.ext.xugu.XuguMessages;
//import org.jkiss.dbeaver.ext.xugu.model.XuguSynonym;
//import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
//import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
//import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
//import org.jkiss.dbeaver.ui.LoadingJob;
//import org.jkiss.dbeaver.ui.UIUtils;
//import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
//import org.jkiss.dbeaver.ui.controls.ProgressPageControl.ProgressVisualizer;
//import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
//import org.jkiss.dbeaver.ui.editors.ControlPropertyCommandListener;
//import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;
//
//public class XuguSynonymEditor extends AbstractDatabaseObjectEditor<XuguSynonym>{
//	//static final Log log = Log.getLog(MySQLUserEditorGeneral.class);
//    public static final String DEF_PASSWORD_VALUE = "**********"; //$NON-NLS-1$
//
//    private PageControl pageControl;
//    private boolean isLoaded;
//    //private PrivilegeTableControl privTable;
//    private boolean newUser;
//    private Text userNameText;
//    private Text passwordText;
//    private Text confirmText;
//    private String password="";
//    //private Text hostText;
//    private CommandListener commandlistener;
//
//    @Override
//    public void createPartControl(Composite parent) 
//    {
//        pageControl = new PageControl(parent);
//
//        Composite container = UIUtils.createPlaceholder(pageControl, 2, 5);
//        GridData gd = new GridData(GridData.FILL_VERTICAL);
//        container.setLayoutData(gd);
//
//        newUser = !getDatabaseObject().isPersisted();
//        //newUser = true;
//        System.out.println("new user??? "+newUser);
//        {
//            Composite loginGroup = UIUtils.createControlGroup(container, XuguMessages.editors_user_editor_general_group_login, 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 200);
//
//            userNameText = UIUtils.createLabelText(loginGroup, XuguMessages.editors_user_editor_general_label_user_name, getDatabaseObject().getName());
//            //userNameText.setEditable(newUser);
//            if (newUser) {
//                ControlPropertyCommandListener.create(this, userNameText, UserPropertyHandler.NAME);
//            }
//
//            password = newUser ? "" : DEF_PASSWORD_VALUE; //$NON-NLS-1$
//            passwordText = UIUtils.createLabelText(loginGroup, XuguMessages.editors_user_editor_general_label_password, password, SWT.BORDER | SWT.PASSWORD);
//            ControlPropertyCommandListener.create(this, passwordText, UserPropertyHandler.PASSWORD);
//
//            confirmText = UIUtils.createLabelText(loginGroup, XuguMessages.editors_user_editor_general_label_confirm, password, SWT.BORDER | SWT.PASSWORD);
//            ControlPropertyCommandListener.create(this, confirmText, UserPropertyHandler.PASSWORD_CONFIRM);
//        }
//        pageControl.createProgressPanel();
//
//        commandlistener = new CommandListener();
//        getEditorInput().getCommandContext().addCommandListener(commandlistener);
//    }
//
//    @Override
//    public void dispose()
//    {
//    	System.out.println("dispose ");
//        if (commandlistener != null) {
//            getEditorInput().getCommandContext().removeCommandListener(commandlistener);
//        }
//        super.dispose();
//    }
//
//    @Override
//    public void activatePart()
//    {
//        if (isLoaded) {
//            return;
//        }
//        isLoaded = true;
//        
//        LoadingJob.createService(
//            new DatabaseLoadService<List<String>>("test", getExecutionContext()) {
//				@Override
//				public List<String> evaluate(DBRProgressMonitor monitor)
//						throws InvocationTargetException, InterruptedException {
//					// TODO Auto-generated method stub
//					return null;
//				}
//            },
//            pageControl.createLoadVisualizer())
//            .schedule();
//    }
//
//
//    //@Override
////    protected void processGrants(List<MySQLGrant> grants)
////    {
////        privTable.fillGrants(grants);
////    }
//
//    @Override
//    public void refreshPart(Object source, boolean force)
//    {
//        // do nothing
//    }
//
//    private class PageControl extends UserPageControl {
//        public PageControl(Composite parent) {
//            super(parent);
//        }
//        public ProgressVisualizer<List<String>> createLoadVisualizer() {
//            return new ProgressVisualizer<List<String>>() {
//                @Override
//                public void completeLoading(List<String> privs) {
//                    super.completeLoading(privs);
////                    privTable.fillPrivileges(privs);
////                    loadGrants();
//                }
//            };
//        }
//
//    }
//
//    protected class UserPageControl extends ObjectEditorPageControl {
//        public UserPageControl(Composite parent) {
//            super(parent, SWT.NONE, XuguSynonymEditor.this);
//        }
//
////        public ProgressVisualizer<List<MySQLGrant>> createGrantsLoadVisualizer() {
////            return new ProgressVisualizer<List<MySQLGrant>>() {
////                @Override
////                public void completeLoading(List<MySQLGrant> grants) {
////                    super.completeLoading(grants);
////                    processGrants(grants);
////                }
////            };
////        }
//
//        @Override
//        public void fillCustomActions(IContributionManager contributionManager) {
//            super.fillCustomActions(contributionManager);
//            DatabaseEditorUtils.contributeStandardEditorActions(getSite(), contributionManager);
//        }
//        
//    }
//    
//    private class CommandListener extends DBECommandAdapter {
//        @Override
//        public void onSave()
//        {
//        	System.out.println("save and out?");
//            if (newUser && getDatabaseObject().isPersisted()) {
//                newUser = false;
//                UIUtils.asyncExec(new Runnable() {
//                    @Override
//                    public void run() {
//                        userNameText.setEditable(false);
//                        passwordText.setEditable(false);
//                        //hostText.setEditable(false);
//                    }
//                });
//            }
//        }
//    }
//
//	@Override
//	public void setFocus() {
//		// TODO Auto-generated method stub
//		
//	}
//}
