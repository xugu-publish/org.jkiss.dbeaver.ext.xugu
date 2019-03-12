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

import org.eclipse.swt.widgets.DateTime;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * OracleTable
 */
public class XuguTable extends XuguTablePhysical implements DBPScriptObject
{
    private static final Log log = Log.getLog(XuguTable.class);

    //private XuguDataType tableType;
    //xfc 修改了用户信息的字段
    private int db_id;
	private int user_id;
    private int schema_id;
    private int table_id;
    private String table_name;
    private int temp_type;
    private int field_num;
    
    private int parti_type;
    private int parti_num;
    private String parti_key;
    private int auto_parti_type;
    private int auto_parti_span;
    
    private int subparti_type;
    private int subparti_num;
    private String subparti_key;
    
    private int gsto_no;
    private int copy_num;
    private int block_size;
    private int chunk_size;
    private long record_num;
    private int pctfree;
    private String file_type;
    private String file_path;
    private String row_delimiter;
    private String col_delimiter;
    private String bad_file;
    private String missing_val;
    private boolean use_cache;
    private String online;
    private boolean is_sys;
    private boolean is_encr;
    private boolean have_policy;
    private boolean on_commit_del;
    private boolean ena_trans;
    private boolean ena_logging;
    private boolean valid;
    private boolean deleted;
    private int acl_mask;
    private Date create_time;
    private String comments;
    
    
//    private boolean temporary;
//    private boolean secondary;
//    private boolean nested;

    public class AdditionalInfo extends TableAdditionalInfo {
        private int pctFree;
        private int pctUsed;
        private int iniTrans;
        private int maxTrans;
        private int initialExtent;
        private int nextExtent;
        private int minExtents;
        private int maxExtents;
        private int pctIncrease;
        private int freelists;
        private int freelistGroups;

        private int blocks;
        private int emptyBlocks;
        private int avgSpace;
        private int chainCount;

        private int avgRowLen;
        private int avgSpaceFreelistBlocks;
        private int numFreelistBlocks;

        @Property(category = CAT_STATISTICS, order = 31)
        public int getPctFree() { return pctFree; }
        @Property(category = CAT_STATISTICS, order = 32)
        public int getPctUsed() { return pctUsed; }
        @Property(category = CAT_STATISTICS, order = 33)
        public int getIniTrans() { return iniTrans; }
        @Property(category = CAT_STATISTICS, order = 34)
        public int getMaxTrans() { return maxTrans; }
        @Property(category = CAT_STATISTICS, order = 35)
        public int getInitialExtent() { return initialExtent; }
        @Property(category = CAT_STATISTICS, order = 36)
        public int getNextExtent() { return nextExtent; }
        @Property(category = CAT_STATISTICS, order = 37)
        public int getMinExtents() { return minExtents; }
        @Property(category = CAT_STATISTICS, order = 38)
        public int getMaxExtents() { return maxExtents; }
        @Property(category = CAT_STATISTICS, order = 39)
        public int getPctIncrease() { return pctIncrease; }
        @Property(category = CAT_STATISTICS, order = 40)
        public int getFreelists() { return freelists; }
        @Property(category = CAT_STATISTICS, order = 41)
        public int getFreelistGroups() { return freelistGroups; }
        @Property(category = CAT_STATISTICS, order = 42)
        public int getBlocks() { return blocks; }
        @Property(category = CAT_STATISTICS, order = 43)
        public int getEmptyBlocks() { return emptyBlocks; }
        @Property(category = CAT_STATISTICS, order = 44)
        public int getAvgSpace() { return avgSpace; }
        @Property(category = CAT_STATISTICS, order = 45)
        public int getChainCount() { return chainCount; }
        @Property(category = CAT_STATISTICS, order = 46)
        public int getAvgRowLen() { return avgRowLen; }
        @Property(category = CAT_STATISTICS, order = 47)
        public int getAvgSpaceFreelistBlocks() { return avgSpaceFreelistBlocks; }
        @Property(category = CAT_STATISTICS, order = 48)
        public int getNumFreelistBlocks() { return numFreelistBlocks; }
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public XuguTable(XuguSchema schema, String name)
    {
        super(schema, name);
    }

    public XuguTable(
        DBRProgressMonitor monitor,
        XuguSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
        //xfc 修改了模式名获取的方式
        String typeOwner = schema.getName();
//        if (!CommonUtils.isEmpty(typeOwner)) {
//            tableType = XuguDataType.resolveDataType(
//                monitor,
//                schema.getDataSource(),
//                typeOwner,
//                JDBCUtils.safeGetString(dbResult, "TABLE_TYPE"));
//        }
        if(dbResult!=null) {
        	this.table_name = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
        	this.db_id = JDBCUtils.safeGetInt(dbResult, "DB_ID");
        	this.user_id = JDBCUtils.safeGetInt(dbResult, "USER_ID");
            this.schema_id = JDBCUtils.safeGetInt(dbResult, "SCHEMA_ID");
            this.table_id = JDBCUtils.safeGetInt(dbResult, "TABLE_ID");
            this.table_name = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
            this.temp_type = JDBCUtils.safeGetInt(dbResult, "TEMP_TYPE");
            this.field_num = JDBCUtils.safeGetInt(dbResult, "FIELD_NUM");
            
            this.parti_type = JDBCUtils.safeGetInt(dbResult, "PARTI_TYPE");
            this.parti_num = JDBCUtils.safeGetInt(dbResult, "PARTI_NUM");
            this.parti_key = JDBCUtils.safeGetString(dbResult, "PARTI_KEY");
            this.auto_parti_type = JDBCUtils.safeGetInt(dbResult, "AUTO_PARTI_TYPE");
            this.auto_parti_span = JDBCUtils.safeGetInt(dbResult, "AUTO_PARTI_SPAN");
            
            this.subparti_type = JDBCUtils.safeGetInt(dbResult, "SUBPARTI_TYPE");
            this.subparti_num = JDBCUtils.safeGetInt(dbResult, "SUBPARTI_NUM");
            this.subparti_key = JDBCUtils.safeGetString(dbResult, "SUBPARTI_KEY");
            
            this.gsto_no = JDBCUtils.safeGetInt(dbResult, "GSTO_NO");
            this.copy_num = JDBCUtils.safeGetInt(dbResult, "COPY_NUM");
            this.block_size = JDBCUtils.safeGetInt(dbResult, "BLOCK_SIZE");
            this.chunk_size = JDBCUtils.safeGetInt(dbResult, "CHUNK_SIZE");
            this.record_num = JDBCUtils.safeGetLong(dbResult, "RECORD_NUM");
            this.pctfree = JDBCUtils.safeGetInt(dbResult, "PCTFREE");
            this.use_cache = JDBCUtils.safeGetBoolean(dbResult, "USE_CACHE");
            this.online = JDBCUtils.safeGetString(dbResult, "ONLINE");
            this.is_sys = JDBCUtils.safeGetBoolean(dbResult, "IS_SYS");
            this.on_commit_del = JDBCUtils.safeGetBoolean(dbResult, "ON_COMMIT_DEL");
            this.ena_trans = JDBCUtils.safeGetBoolean(dbResult, "ENA_TRANS");
            this.ena_logging = JDBCUtils.safeGetBoolean(dbResult, "ENA_LOGGING");
            this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
            this.acl_mask = JDBCUtils.safeGetInt(dbResult, "ACL_MASK");
            this.create_time = JDBCUtils.safeGetDate(dbResult, "CREATE_TIME");
            this.comments = JDBCUtils.safeGetString(dbResult, "COMMENTS");
            System.out.println("Xugu table load success "+ this.table_name + " " +this.valid + " " + this.deleted);
        }
    }

    @Override
    public TableAdditionalInfo getAdditionalInfo()
    {
        return additionalInfo;
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
    protected String getTableTypeName()
    {
        return "TABLE";
    }

    @Override
    public boolean isView()
    {
        return false;
    }

//    @Property(viewable = false, order = 5)
//    public XuguDataType getTableType()
//    {
//        return tableType;
//    }

//    @Property(viewable = false, order = 6)
//    public String getIotType()
//    {
//        return iotType;
//    }
//
//    @Property(viewable = false, order = 7)
//    public String getIotName()
//    {
//        return iotName;
//    }
//
//    @Property(viewable = false, order = 10)
//    public boolean isTemporary()
//    {
//        return temporary;
//    }
//
//    @Property(viewable = false, order = 11)
//    public boolean isSecondary()
//    {
//        return secondary;
//    }
//
//    @Property(viewable = false, order = 12)
//    public boolean isNested()
//    {
//        return nested;
//    }

    @Override
    public XuguTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
/*
        // Fake XML attribute handle
        if (tableType != null && tableType.getName().equals(OracleConstants.TYPE_NAME_XML) && OracleConstants.XML_COLUMN_NAME.equals(attributeName)) {
            OracleTableColumn col = getXMLColumn(monitor);
            if (col != null) return col;
        }
*/

        return super.getAttribute(monitor, attributeName);
    }

    @Nullable
//    private XuguTableColumn getXMLColumn(DBRProgressMonitor monitor) throws DBException {
//        for (XuguTableColumn col : CommonUtils.safeCollection(getAttributes(monitor))) {
//            if (col.getDataType() == tableType) {
//                return col;
//            }
//        }
//        return null;
//    }


    @Override
    public Collection<XuguTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        List<XuguTableForeignKey> refs = new ArrayList<>();
        // This is dummy implementation
        // Get references from this schema only
        final Collection<XuguTableForeignKey> allForeignKeys =
            getContainer().foreignKeyCache.getObjects(monitor, getContainer(), null);
        for (XuguTableForeignKey constraint : allForeignKeys) {
            if (constraint.getReferencedTable() == this) {
                refs.add(constraint);
            }
        }
        return refs;
    }

    @Override
    @Association
    public Collection<XuguTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().foreignKeyCache.getObjects(monitor, getContainer(), this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().foreignKeyCache.clearObjectCache(this);
        return super.refreshObject(monitor);
    }

//    @Override
//    public DBDPseudoAttribute[] getPseudoAttributes() throws DBException
//    {
//        if (CommonUtils.isEmpty(this.iotType) && getDataSource().getContainer().getPreferenceStore().getBoolean(XuguConstants.PREF_SUPPORT_ROWID)) {
//            // IOT tables have index id instead of ROWID
//            return new DBDPseudoAttribute[] {
//                XuguConstants.PSEUDO_ATTR_ROWID
//            };
//        } else {
//            return null;
//        }
//    }

//    @Override
//    protected void appendSelectSource(DBRProgressMonitor monitor, StringBuilder query, String tableAlias, DBDPseudoAttribute rowIdAttribute) {
//        if (tableType != null && tableType.getName().equals(XuguConstants.TYPE_NAME_XML)) {
//            try {
//                XuguTableColumn xmlColumn = getXMLColumn(monitor);
//                if (xmlColumn != null) {
//                    query.append("XMLType(").append(tableAlias).append(".").append(xmlColumn.getName()).append(".getClobval()) as ").append(xmlColumn.getName());
//                    if (rowIdAttribute != null) {
//                        query.append(",").append(rowIdAttribute.translateExpression(tableAlias));
//                    }
//                    return;
//                }
//            } catch (DBException e) {
//                log.warn(e);
//            }
//        }
//        super.appendSelectSource(monitor, query, tableAlias, rowIdAttribute);
//    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return getDDL(monitor, XuguDDLFormat.getCurrentFormat(getDataSource()), options);
    }


//    @Nullable
//    @Override
//    public DBPImage getObjectImage() {
//        if (CommonUtils.isEmpty(iotType)) {
//            return DBIcon.TREE_TABLE;
//        } else {
//            return DBIcon.TREE_TABLE_INDEX;
//        }
//    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBException
    {
//        if (!isPersisted()) {
//            additionalInfo.loaded = true;
//            return;
//        }
//        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
//            try (JDBCPreparedStatement dbStat = session.prepareStatement(
//                "SELECT * FROM SYS.ALL_TABLES WHERE OWNER=? AND TABLE_NAME=?")) {
//                dbStat.setString(1, getContainer().getName());
//                dbStat.setString(2, getName());
//                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
//                    if (dbResult.next()) {
//                        additionalInfo.pctFree = JDBCUtils.safeGetInt(dbResult, "PCT_FREE");
//                        additionalInfo.pctUsed = JDBCUtils.safeGetInt(dbResult, "PCT_USED");
//                        additionalInfo.iniTrans = JDBCUtils.safeGetInt(dbResult, "INI_TRANS");
//                        additionalInfo.maxTrans = JDBCUtils.safeGetInt(dbResult, "MAX_TRANS");
//                        additionalInfo.initialExtent = JDBCUtils.safeGetInt(dbResult, "INITIAL_EXTENT");
//                        additionalInfo.nextExtent = JDBCUtils.safeGetInt(dbResult, "NEXT_EXTENT");
//                        additionalInfo.minExtents = JDBCUtils.safeGetInt(dbResult, "MIN_EXTENTS");
//                        additionalInfo.maxExtents = JDBCUtils.safeGetInt(dbResult, "MAX_EXTENTS");
//                        additionalInfo.pctIncrease = JDBCUtils.safeGetInt(dbResult, "PCT_INCREASE");
//                        additionalInfo.freelists = JDBCUtils.safeGetInt(dbResult, "FREELISTS");
//                        additionalInfo.freelistGroups = JDBCUtils.safeGetInt(dbResult, "FREELIST_GROUPS");
//
//                        additionalInfo.blocks = JDBCUtils.safeGetInt(dbResult, "BLOCKS");
//                        additionalInfo.emptyBlocks = JDBCUtils.safeGetInt(dbResult, "EMPTY_BLOCKS");
//                        additionalInfo.avgSpace = JDBCUtils.safeGetInt(dbResult, "AVG_SPACE");
//                        additionalInfo.chainCount = JDBCUtils.safeGetInt(dbResult, "CHAIN_CNT");
//
//                        additionalInfo.avgRowLen = JDBCUtils.safeGetInt(dbResult, "AVG_ROW_LEN");
//                        additionalInfo.avgSpaceFreelistBlocks = JDBCUtils.safeGetInt(dbResult, "AVG_SPACE_FREELIST_BLOCKS");
//                        additionalInfo.numFreelistBlocks = JDBCUtils.safeGetInt(dbResult, "NUM_FREELIST_BLOCKS");
//                    } else {
//                        log.warn("Cannot find table '" + getFullyQualifiedName(DBPEvaluationContext.UI) + "' metadata");
//                    }
//                    additionalInfo.loaded = true;
//                }
//            }
//        }
//        catch (SQLException e) {
//            throw new DBCException(e, getDataSource());
//        }

    }

}
