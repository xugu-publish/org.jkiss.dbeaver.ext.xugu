/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.xugu.data;

import org.jkiss.dbeaver.ext.xugu.model.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;

/**
 * Oracle data types provider
 */
public class XuguValueHandlerProvider implements DBDValueHandlerProvider {

    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDPreferences preferences, DBSTypedObject typedObject)
    {
        switch (typedObject.getTypeID()) {
            case Types.BLOB:
                return XuguBLOBValueHandler.INSTANCE;
            case Types.CLOB:
            case Types.NCLOB:
                return XuguCLOBValueHandler.INSTANCE;
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP_WITH_TIMEZONE:
            case XuguConstants.DATA_TYPE_TIMESTAMP_WITH_TIMEZONE:
                if (((XuguDataSource)dataSource).isDriverVersionAtLeast(12, 2)) {
                    return new XuguTemporalAccessorValueHandler(preferences.getDataFormatterProfile());
                } else {
                    return new XuguTimestampValueHandler(preferences.getDataFormatterProfile());
                }
            case Types.STRUCT:
                return XuguObjectValueHandler.INSTANCE;
        }

        final String typeName = typedObject.getTypeName();
        switch (typeName) {
            case XuguConstants.TYPE_NAME_XML:
            case XuguConstants.TYPE_FQ_XML:
                return XuguXMLValueHandler.INSTANCE;
            case XuguConstants.TYPE_NAME_BFILE:
                return XuguBFILEValueHandler.INSTANCE;
        }

        if (typeName.contains(XuguConstants.TYPE_NAME_TIMESTAMP) || typedObject.getDataKind() == DBPDataKind.DATETIME) {
            return new XuguTimestampValueHandler(preferences.getDataFormatterProfile());
        } else {
            return null;
        }
    }

}