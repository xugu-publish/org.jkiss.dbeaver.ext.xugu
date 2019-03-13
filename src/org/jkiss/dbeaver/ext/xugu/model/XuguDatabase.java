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
package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource.SchemaCache;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * GenericCatalog
 */
public class XuguDatabase extends XuguGlobalObject
{
    private XuguDataSource dataSource;
    private String name;
    private Integer databaseSize;
    private boolean persisted;

    public XuguDatabase(XuguDataSource dataSource, String name)
    {
    	super(dataSource, true);
        this.dataSource = dataSource;
        this.name = name;
        System.out.println("DB name "+name);
    }
    
    public XuguDatabase(XuguDataSource dataSource, ResultSet dbResult)
    {
    	super(dataSource, true);
        this.dataSource = dataSource;
        if (dbResult != null) {
            this.name = JDBCUtils.safeGetString(dbResult, "DB_NAME");
            System.out.println("DB name "+name);
            persisted = true;
        } else {
            this.additionalInfo.loaded = true;
            this.additionalInfo.defaultCharset = dataSource.getCharset("utf8");
            persisted = false;
        }
    }

    
    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private XuguCharset defaultCharset;
//        private MySQLCollation defaultCollation;
        private String sqlPath;

        public XuguCharset getDefaultCharset()
        {
            return defaultCharset;
        }

        public void setDefaultCharset(XuguCharset defaultCharset)
        {
            this.defaultCharset = defaultCharset;
        }

    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<XuguDatabase> {
        @Override
        public boolean isPropertyCached(XuguDatabase object, Object propertyId)
        {
            return object.additionalInfo.loaded;
        }
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBCException {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    // for internal use only
    public AdditionalInfo getAdditionalInfo() {
        return additionalInfo;
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        XuguDataSource dataSource = getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM ALL_DATABASES WHERE DB_NAME=?")) {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        additionalInfo.defaultCharset = dataSource.getCharset(JDBCUtils.safeGetString(dbResult, XuguConstants.COL_DEFAULT_CHARACTER_SET_NAME));
                    }
                    additionalInfo.loaded = true;
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, dataSource);
        }
    }

    @Association
    public Collection<XuguSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        return dataSource.getSchemas(monitor);
    }

    @Association
    public XuguSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
        return dataSource.getSchema(monitor, name);
    }
    
    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public XuguDataSource getDataSource()
    {
        return dataSource;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return name + " [" + dataSource.getContainer().getName() + "]";
    }

}
