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
package org.jkiss.dbeaver.ext.xugu;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
//import org.jkiss.dbeaver.ext.xugu.model.dict.XuguConnectionType;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocationManager;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//加载数据源信息
public class XuguDataSourceProvider extends JDBCDataSourceProvider {

    public XuguDataSourceProvider()
    {
    }

    @Override
    public long getFeatures()
    {
        return FEATURE_SCHEMAS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo)
    {
        XuguConstants.ConnectionType connectionType;
        String conTypeProperty = connectionInfo.getProviderProperty(XuguConstants.PROP_CONNECTION_TYPE);
        if (conTypeProperty != null) {
            connectionType = XuguConstants.ConnectionType.valueOf(CommonUtils.toString(conTypeProperty));
        } else {
            connectionType = XuguConstants.ConnectionType.BASIC;
        }
        if (connectionType == XuguConstants.ConnectionType.CUSTOM) {
            return connectionInfo.getUrl();
        }
        StringBuilder url = new StringBuilder(100);
        url.append("jdbc:xugu://"); //$NON-NLS-1$

        if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
            url.append(connectionInfo.getHostName());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            url.append(":"); //$NON-NLS-1$
            url.append(connectionInfo.getHostPort());
        }

        if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
        	url.append("/");
            url.append(connectionInfo.getDatabaseName());
        }
        return url.toString();
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container)
        throws DBException
    {
        return new XuguDataSource(monitor, container);
    }
}
