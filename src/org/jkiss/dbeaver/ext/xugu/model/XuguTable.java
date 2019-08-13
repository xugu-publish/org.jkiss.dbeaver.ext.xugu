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
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Maple4Real
 *   表信息衍生类，包含表相关的基本信息
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
        if(dbResult!=null) {
        	//结果集不为空时，persisted设为true
            this.setPersisted(true);
            
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
    
    public static Log getLog() {
		return log;
	}

	public int getDb_id() {
		return db_id;
	}

	public int getUser_id() {
		return user_id;
	}

	public int getSchema_id() {
		return schema_id;
	}

	public int getTable_id() {
		return table_id;
	}

	public String getTable_name() {
		return table_name;
	}

	public int getTemp_type() {
		return temp_type;
	}

	public int getField_num() {
		return field_num;
	}

	public int getParti_type() {
		return parti_type;
	}

	public int getParti_num() {
		return parti_num;
	}

	public String getParti_key() {
		return parti_key;
	}

	public int getAuto_parti_type() {
		return auto_parti_type;
	}

	public int getAuto_parti_span() {
		return auto_parti_span;
	}

	public int getSubparti_type() {
		return subparti_type;
	}

	public int getSubparti_num() {
		return subparti_num;
	}

	public String getSubparti_key() {
		return subparti_key;
	}

	public int getGsto_no() {
		return gsto_no;
	}

	public int getCopy_num() {
		return copy_num;
	}

	public int getBlock_size() {
		return block_size;
	}

	public int getChunk_size() {
		return chunk_size;
	}

	public long getRecord_num() {
		return record_num;
	}

	public int getPctfree() {
		return pctfree;
	}

	public String getFile_type() {
		return file_type;
	}

	public String getFile_path() {
		return file_path;
	}

	public String getRow_delimiter() {
		return row_delimiter;
	}

	public String getCol_delimiter() {
		return col_delimiter;
	}

	public String getBad_file() {
		return bad_file;
	}

	public String getMissing_val() {
		return missing_val;
	}

	public boolean isUse_cache() {
		return use_cache;
	}

	public String getOnline() {
		return online;
	}

	public boolean isIs_sys() {
		return is_sys;
	}

	public boolean isIs_encr() {
		return is_encr;
	}

	public boolean isHave_policy() {
		return have_policy;
	}

	public boolean isOn_commit_del() {
		return on_commit_del;
	}

	public boolean isEna_trans() {
		return ena_trans;
	}

	public boolean isEna_logging() {
		return ena_logging;
	}

	public boolean isValid() {
		return valid;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public int getAcl_mask() {
		return acl_mask;
	}

	public Date getCreate_time() {
		return create_time;
	}

	public String getComments() {
		return comments;
	}

    @Override
    public XuguTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return super.getAttribute(monitor, attributeName);
    }

    @Override
    public Collection<XuguTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        List<XuguTableForeignKey> refs = new ArrayList<>();
        // This is dummy implementation
        // Get references from this schema only
        final Collection<XuguTableForeignKey> allForeignKeys =
            getContainer().foreignKeyCache.getObjects(monitor, getContainer(), this);
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

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return getDDL(monitor, XuguDDLFormat.getCurrentFormat(getDataSource()), options);
    }
}
