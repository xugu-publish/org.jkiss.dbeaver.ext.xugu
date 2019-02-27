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

package org.jkiss.dbeaver.ext.xugu.tools;

import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.model.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ext.xugu.model.dict.XuguConnectionType;
import org.jkiss.dbeaver.ext.xugu.oci.OCIUtils;
import org.jkiss.dbeaver.ext.xugu.oci.XuguHomeDescriptor;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractScriptExecuteWizard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class XuguScriptExecuteWizard extends AbstractScriptExecuteWizard<XuguDataSource,XuguDataSource> {

    private XuguScriptExecuteWizardPageSettings mainPage;

    public XuguScriptExecuteWizard(XuguDataSource oracleSchema)
    {
        super(Collections.singleton(oracleSchema), XuguMessages.tools_script_execute_wizard_page_name);
        this.mainPage = new XuguScriptExecuteWizardPageSettings(this);
    }

    @Override
    public void addPages()
    {
        addPage(mainPage);
        super.addPages();
    }

    @Override
    public void fillProcessParameters(List<String> cmd, XuguDataSource arg) throws IOException
    {
        String sqlPlusExec = RuntimeUtils.getNativeBinaryName("sqlplus"); //$NON-NLS-1$
        File sqlPlusBinary = new File(getClientHome().getPath(), "bin/" + sqlPlusExec); //$NON-NLS-1$
        if (!sqlPlusBinary.exists()) {
            sqlPlusBinary = new File(getClientHome().getPath(), sqlPlusExec);
        }
        if (!sqlPlusBinary.exists()) {
            throw new IOException(NLS.bind(XuguMessages.tools_script_execute_wizard_error_sqlplus_not_found, getClientHome().getDisplayName()));
        }
        String dumpPath = sqlPlusBinary.getAbsolutePath();
        cmd.add(dumpPath);
    }

    @Override
    public XuguHomeDescriptor findNativeClientHome(String clientHomeId)
    {
        return OCIUtils.getOraHome(clientHomeId);
    }

    @Override
    public Collection<XuguDataSource> getRunInfo() {
        return getDatabaseObjects();
    }

    @Override
    protected List<String> getCommandLine(XuguDataSource arg) throws IOException
    {
        List<String> cmd = new ArrayList<>();
        fillProcessParameters(cmd, arg);
        DBPConnectionConfiguration conInfo = getConnectionInfo();
        String url;
        if ("TNS".equals(conInfo.getProviderProperty(XuguConstants.PROP_CONNECTION_TYPE))) { //$NON-NLS-1$
            url = conInfo.getServerName();
        }
        else {
            boolean isSID = XuguConnectionType.SID.name().equals(conInfo.getProviderProperty(XuguConstants.PROP_SID_SERVICE));
            String port = conInfo.getHostPort();
            if (isSID) {
                url = "(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(Host=" + conInfo.getHostName() + ")(Port=" + port + "))(CONNECT_DATA=(SID=" + conInfo.getDatabaseName() + ")))";
            } else {
                url = "//" + conInfo.getHostName() + (port != null ? ":" + port : "") + "/" + conInfo.getDatabaseName(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            }
        }
        final String role = conInfo.getProviderProperty(XuguConstants.PROP_INTERNAL_LOGON);
        if (role != null) {
            url += (" AS " + role);
        }
        cmd.add(conInfo.getUserName() + "/" + conInfo.getUserPassword() + "@" + url); //$NON-NLS-1$ //$NON-NLS-2$
/*

        if (toolWizard.isVerbose()) {
            cmd.add("-v");
        }
        cmd.add("-q");

        cmd.add(toolWizard.getDatabaseObjects().getName());
*/
        return cmd;
    }
}
