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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * OracleView
 */
public class XuguView extends XuguTableBase implements XuguSourceObject
{
    private static final Log log = Log.getLog(XuguView.class);

    public class AdditionalInfo extends TableAdditionalInfo {
        private String typeText;
        private String oidText;
        private String typeOwner;
        private String typeName;
        private XuguView superView;


        @Property(viewable = false, order = 10)
        public Object getType(DBRProgressMonitor monitor) throws DBException
        {
            if (typeOwner == null) {
                return null;
            }
            XuguSchema owner = getDataSource().getSchema(monitor, typeOwner);
            return owner == null ? null : owner.getDataType(monitor, typeName);
        }
        @Property(viewable = false, order = 11)
        public String getTypeText() { return typeText; }
        public void setTypeText(String typeText) { this.typeText = typeText; }
        @Property(viewable = false, order = 12)
        public String getOidText() { return oidText; }
        public void setOidText(String oidText) { this.oidText = oidText; }
        @Property(viewable = false, editable = true, order = 5)
        public XuguView getSuperView() { return superView; }
        public void setSuperView(XuguView superView) { this.superView = superView; }
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();
    private String viewText;

    public XuguView(XuguSchema schema, String name)
    {
        super(schema, name, false);
    }

    public XuguView(
        XuguSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
    }

    @NotNull
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    @Override
    public String getName()
    {
        return super.getName();
    }

    @Override
    public boolean isView()
    {
        return true;
    }

    @Override
    public XuguSourceType getSourceType()
    {
        return XuguSourceType.VIEW;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        if (viewText == null) {
            try {
                viewText = XuguUtils.getDDL(monitor, getTableTypeName(), this, XuguDDLFormat.FULL, options);
            } catch (DBException e) {
                log.warn("Error getting view definition from system package", e);
            }
        }
        return viewText;
    }

    public void setObjectDefinitionText(String source)
    {
        this.viewText = source;
    }

    @Override
    public AdditionalInfo getAdditionalInfo()
    {
        return additionalInfo;
    }

    @Override
    protected String getTableTypeName()
    {
        return "VIEW";
    }

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBException
    {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded && monitor != null) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.valid = XuguUtils.getObjectStatus(monitor, this, XuguObjectType.VIEW);
    }

    public String getViewText() {
        return viewText;
    }

    public void setViewText(String viewText) {
        this.viewText = viewText;
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBException
    {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        String viewDefinitionText = null; // It is truncated definition text
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            boolean isOracle9 = getDataSource().isAtLeastV9();
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TEXT,TYPE_TEXT,OID_TEXT,VIEW_TYPE_OWNER,VIEW_TYPE" + (isOracle9 ? ",SUPERVIEW_NAME" : "") + "\n" +
                    "FROM SYS.ALL_VIEWS WHERE OWNER=? AND VIEW_NAME=?")) {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        viewDefinitionText = JDBCUtils.safeGetString(dbResult, "TEXT");
                        additionalInfo.setTypeText(JDBCUtils.safeGetStringTrimmed(dbResult, "TYPE_TEXT"));
                        additionalInfo.setOidText(JDBCUtils.safeGetStringTrimmed(dbResult, "OID_TEXT"));
                        additionalInfo.typeOwner = JDBCUtils.safeGetStringTrimmed(dbResult, "VIEW_TYPE_OWNER");
                        additionalInfo.typeName = JDBCUtils.safeGetStringTrimmed(dbResult, "VIEW_TYPE");
                        if (isOracle9) {
                            String superViewName = JDBCUtils.safeGetString(dbResult, "SUPERVIEW_NAME");
                            if (!CommonUtils.isEmpty(superViewName)) {
                                additionalInfo.setSuperView(getContainer().getView(monitor, superViewName));
                            }
                        }
                    } else {
                        log.warn("Cannot find view '" + getFullyQualifiedName(DBPEvaluationContext.UI) + "' metadata");
                    }
                    additionalInfo.loaded = true;
                }
            }
        }
        catch (SQLException e) {
            throw new DBCException(e, getDataSource());
        }

        if (viewDefinitionText != null) {
            StringBuilder paramsList = new StringBuilder();
            Collection<XuguTableColumn> attributes = getAttributes(monitor);
            if (attributes != null) {
                paramsList.append("\n(");
                boolean first = true;
                for (XuguTableColumn column : attributes) {
                    if (!first) paramsList.append(",");
                    paramsList.append(DBUtils.getQuotedIdentifier(column));
                    first = false;
                }
                paramsList.append(")");
            }
            viewText = "CREATE OR REPLACE VIEW " + getFullyQualifiedName(DBPEvaluationContext.DDL) + paramsList + "\nAS\n" + viewDefinitionText;
        }
    }

    @Override
    public DBEPersistAction[] getCompileActions()
    {
        return new DBEPersistAction[] {
            new XuguObjectPersistAction(
                XuguObjectType.VIEW,
                "Compile view",
                "ALTER VIEW " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
            )};
    }

}