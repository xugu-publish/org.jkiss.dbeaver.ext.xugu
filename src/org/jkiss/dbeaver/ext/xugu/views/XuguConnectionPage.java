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
package org.jkiss.dbeaver.ext.xugu.views;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.xugu.Activator;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFolder;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;
import java.util.TimeZone;
/**
 * XuguConnectionPage
 */
public class XuguConnectionPage extends ConnectionPageAbstract implements ICompositeDialogPage
{
    private Text hostText;
    private Text portText;
    private Text dbText;
    private Text usernameText;
    private Text passwordText;

    private ClientHomesSelector homesSelector;

    private Combo tnsNameCombo;
	private TabFolder connectionTypeFolder;
    private ClientHomesSelector oraHomeSelector;
    private Text connectionUrlText;
    private Button osAuthCheck;

    private ControlsListener controlModifyListener;
    private XuguConstants.ConnectionType connectionType = XuguConstants.ConnectionType.BASIC;

    private static ImageDescriptor logoImage = Activator.getImageDescriptor("icons/xugu_logo.png"); //$NON-NLS-1$
    private TextWithOpenFolder tnsPathText;


    private boolean activated = false;

    private static ImageDescriptor Xugu_LOGO_IMG = Activator.getImageDescriptor("icons/Xugu_logo.png");
//    private static ImageDescriptor MARIADB_LOGO_IMG = Activator.getImageDescriptor("icons/mariadb_logo.png");
    private Combo serverTimezoneCombo;
    private Combo roleCombo;


    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void createControl(Composite composite)
    {
        //Composite group = new Composite(composite, SWT.NONE);
        //group.setLayout(new GridLayout(1, true));
        ModifyListener textListener = new ModifyListener()
        {
            @Override
            public void modifyText(ModifyEvent e)
            {
                if (activated) {
                    site.updateButtons();
                }
            }
        };
        final int fontHeight = UIUtils.getFontHeight(composite);

        Composite addrGroup = UIUtils.createPlaceholder(composite, 2);
        GridLayout gl = new GridLayout(2, false);
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        Label hostLabel = UIUtils.createControlLabel(addrGroup,XuguMessages.dialog_connection_host);
        hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        hostText = new Text(addrGroup, SWT.BORDER);
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        hostText.addModifyListener(textListener);

        Label portLabel = UIUtils.createControlLabel(addrGroup, XuguMessages.dialog_connection_port);
        portLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        portText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = fontHeight * 10;
        portText.setLayoutData(gd);
        portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        portText.addModifyListener(textListener);

        Label dbLabel = UIUtils.createControlLabel(addrGroup, XuguMessages.dialog_connection_database);
        dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        dbText = new Text(addrGroup, SWT.BORDER);
        dbText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        dbText.addModifyListener(textListener);

        Label usernameLabel = UIUtils.createControlLabel(addrGroup, XuguMessages.dialog_connection_user_name);
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        usernameText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = fontHeight * 20;
        usernameText.setLayoutData(gd);
        usernameText.addModifyListener(textListener);

        Label passwordLabel = UIUtils.createControlLabel(addrGroup, XuguMessages.dialog_connection_password);
        passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        Composite passPH = UIUtils.createPlaceholder(addrGroup, 2, 5);
        passPH.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        passwordText = new Text(passPH, SWT.BORDER | SWT.PASSWORD);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = fontHeight * 20;
        passwordText.setLayoutData(gd);
        passwordText.addModifyListener(textListener);
        
        roleCombo = UIUtils.createLabelCombo(addrGroup, XuguMessages.dialog_connection_server_role, SWT.READ_ONLY);
        roleCombo.add(XuguMessages.dialog_connection_role_normal);
        roleCombo.add(XuguMessages.dialog_connection_role_sysdba);
        roleCombo.add(XuguMessages.dialog_connection_role_dba);
        UIUtils.createHorizontalLine(addrGroup, 2, 10);
        roleCombo.select(0);

        serverTimezoneCombo = UIUtils.createLabelCombo(addrGroup, XuguMessages.dialog_connection_server_timezone, SWT.READ_ONLY);
        serverTimezoneCombo.add(XuguMessages.dialog_connection_auto_detect);
        {
            String[] tzList = TimeZone.getAvailableIDs();
            for (String tzID : tzList) {
                //TimeZone timeZone = TimeZone.getTimeZone(tzID);
                serverTimezoneCombo.add(tzID);
            }
        }
        serverTimezoneCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        
//        if (!XuguUtils.isMariaDB(getSite().getDriver())) {
//            
//        }
//
//        homesSelector = new ClientHomesSelector(addrGroup, SWT.NONE, XuguMessages.dialog_connection_local_client);
//        gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
//        gd.horizontalSpan = 2;
//        homesSelector.getPanel().setLayoutData(gd);

        createDriverPanel(addrGroup);
        setControl(addrGroup);
    }

    @Override
    public boolean isComplete()
    {
        return hostText != null && portText != null && 
            !CommonUtils.isEmpty(hostText.getText()) &&
            !CommonUtils.isEmpty(portText.getText());
    }

    @Override
    public void loadSettings()
    {
        super.loadSettings();

        DBPDriver driver = getSite().getDriver();
        if (!activated) {
            // We set image only once at activation
            // There is a bug in Eclipse which leads to SWTException after wizard image change
        	setImageDescriptor(Xugu_LOGO_IMG);
//            if (driver != null && driver.getId().equalsIgnoreCase(XuguConstants.DRIVER_ID_MARIA_DB)) {
//                setImageDescriptor(MARIADB_LOGO_IMG);
//            } else {
//                
//            }
        }

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else {
                hostText.setText(XuguConstants.DEFAULT_HOST);
            }
        }
        if (portText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                portText.setText(String.valueOf(connectionInfo.getHostPort()));
            } else if (site.getDriver().getDefaultPort() != null) {
                portText.setText(site.getDriver().getDefaultPort());
            } else {
                portText.setText("");
            }
        }
        if (dbText != null) {
            dbText.setText(CommonUtils.notEmpty(connectionInfo.getDatabaseName()));
        }
        if (usernameText != null) {
            usernameText.setText(CommonUtils.notEmpty(connectionInfo.getUserName()));
        }
        if (passwordText != null) {
            passwordText.setText(CommonUtils.notEmpty(connectionInfo.getUserPassword()));
        }
        if (roleCombo != null) {
            roleCombo.setText(CommonUtils.notEmpty(connectionInfo.getServerName()));
        }
        if (serverTimezoneCombo != null) {
            String tzProp = connectionInfo.getProviderProperty(XuguConstants.PROP_SERVER_TIMEZONE);
            if (CommonUtils.isEmpty(tzProp)) {
                serverTimezoneCombo.select(0);
            } else {
                serverTimezoneCombo.setText(tzProp);
            }
        }
//
//        homesSelector.populateHomes(site.getDriver(), connectionInfo.getClientHomeId(), site.isNew());
//
//        activated = true;
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (hostText != null) {
            connectionInfo.setHostName(hostText.getText().trim());
        }
        if (portText != null) {
            connectionInfo.setHostPort(portText.getText().trim());
        }
        if (dbText != null) {
            connectionInfo.setDatabaseName(dbText.getText().trim());
        }
        if (usernameText != null) {
            connectionInfo.setUserName(usernameText.getText().trim());
        }
        if (passwordText != null) {
            connectionInfo.setUserPassword(passwordText.getText());
        }
        connectionInfo.setServerName(roleCombo.getText());
        if (serverTimezoneCombo != null) {
            if (serverTimezoneCombo.getSelectionIndex() == 0 || CommonUtils.isEmpty(serverTimezoneCombo.getText())) {
                connectionInfo.removeProviderProperty(XuguConstants.PROP_SERVER_TIMEZONE);
            } else {
                connectionInfo.setProviderProperty(XuguConstants.PROP_SERVER_TIMEZONE, serverTimezoneCombo.getText());
            }
        }
        //xfc 根据下拉框设置用户选择的角色
//        if (roleCombo.getText().equals("SYSDBA")&&(!connectionInfo.getUserName().equals("SYSDBA")||!dbText.getText().equals("SYSTEM"))) {
//        	new UITask<String>() {
//        		@Override
//        		protected String runTask() {
//        			WarningDialog dialog2 = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "登陆失败，SYSDBA角色仅针对系统库超级管理员开放");       
//        			if (dialog2.open() != IDialogConstants.OK_ID) {
//        				return null;
//        			}
//        			return null;
//        		}
//        	}.execute(); 
//        	} else {
			connectionInfo.setProviderProperty(XuguConstants.PROP_INTERNAL_LOGON, roleCombo.getText().toUpperCase(Locale.ENGLISH));						
//		}
//        if (roleCombo.getText()=="SYSDBA"||) {
//			
//		} 
        
//        if (homesSelector != null) {
//              connectionInfo.setClientHomeId(homesSelector.getSelectedHome());
//        }
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
            getShell().setText(XuguMessages.dialog_connection_connection);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
            
            Label infoText = UIUtils.createLabel(composite, "Warning:"+this.warningInfo);
            infoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            return parent;
        }
    }
    
    private void updateUI()
    {
        if (activated) {
            site.updateButtons();
        }
    }
    
    private class ControlsListener implements ModifyListener, SelectionListener {
        @Override
        public void modifyText(ModifyEvent e) {
            updateUI();
        }
        @Override
        public void widgetSelected(SelectionEvent e) {
            updateUI();
        }
        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            updateUI();
        }
    }
    
    public IDialogPage[] getSubPages()
    {
        return new IDialogPage[] {
            new DriverPropertiesDialogPage(this)
        };
    }

	@Override
	public IDialogPage[] getSubPages(boolean extrasOnly) {
		// TODO Auto-generated method stub
		return new IDialogPage[] {
            new DriverPropertiesDialogPage(this)
        };
	}

}
