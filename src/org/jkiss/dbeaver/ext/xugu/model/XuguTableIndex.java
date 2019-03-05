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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * XuguTableIndex
 */
//public class XuguTableIndex extends JDBCTableIndex<XuguSchema, XuguTablePhysical> implements DBSObjectLazy
public class XuguTableIndex extends JDBCTableIndex<XuguSchema, XuguTablePhysical>
{

//    private Object tablespace;
    private boolean nonUnique;
    private List<XuguTableIndexColumn> columns;

	private int index_id;
    private String index_name;
    private String index_typeName;
    private boolean is_primary;
    private boolean is_unique;
    private boolean is_local;
    private int parti_type;
    private int parti_num;
    private String parti_key;
    private int gsto_no;
    private int copy_num;
    private int block_size;
    private int chunk_size;
    private int field_num;
    private String keys;
    private String filter;
    private String vocable;
    private String lexer;
    private int word_len;
    private boolean enable_trans;
    private Date create_time;
    private boolean is_sys;
    private boolean keepin_cache;
    private boolean nologging;
    private boolean valid;
    
    public XuguTableIndex(
        XuguSchema schema,
        XuguTablePhysical table,
        String indexName,
        ResultSet dbResult)
    {
        super(schema, table, indexName, null, true);
        this.index_typeName = JDBCUtils.safeGetString(dbResult, "INDEX_TYPE");
//        this.nonUnique = !"UNIQUE".equals(JDBCUtils.safeGetString(dbResult, "UNIQUENESS"));
        if (XuguConstants.INDEX_TYPE_BTREE.getId().equals(this.index_typeName)) {
            indexType = XuguConstants.INDEX_TYPE_BTREE;
        } else if (XuguConstants.INDEX_TYPE_RTREE.getId().equals(this.index_typeName)) {
            indexType = XuguConstants.INDEX_TYPE_RTREE;
        } else if (XuguConstants.INDEX_TYPE_FULL_TEXT.getId().equals(this.index_typeName)) {
            indexType = XuguConstants.INDEX_TYPE_FULL_TEXT;
        } else {
            indexType = DBSIndexType.OTHER;
        }
        if(dbResult!=null) {
        	this.index_id = JDBCUtils.safeGetInt(dbResult, "INDEX_ID");
        	this.index_name = JDBCUtils.safeGetString(dbResult, "INDEX_NAME");
        	this.index_typeName = JDBCUtils.safeGetString(dbResult, "INDEX_TYPE");
        	this.is_primary = JDBCUtils.safeGetBoolean(dbResult, "IS_PRIMARY");
        	this.is_unique = JDBCUtils.safeGetBoolean(dbResult, "IS_UNIQUE");
        	this.is_local = JDBCUtils.safeGetBoolean(dbResult, "IS_LOCAL");
        	this.parti_type = JDBCUtils.safeGetInt(dbResult, "PARTI_TYPE");
        	this.parti_num = JDBCUtils.safeGetInt(dbResult, "PARTI_NUM");
        	this.parti_key = JDBCUtils.safeGetString(dbResult, "PARTI_KEY");
        	this.gsto_no = JDBCUtils.safeGetInt(dbResult, "GSTO_NO");
        	this.copy_num = JDBCUtils.safeGetInt(dbResult, "COPY_NUM");
        	this.block_size = JDBCUtils.safeGetInt(dbResult, "BLOCK_SIZE");
        	this.chunk_size = JDBCUtils.safeGetInt(dbResult, "CHUNK_SIZE");
        	this.field_num = JDBCUtils.safeGetInt(dbResult, "FIELD_NUM");
        	this.keys = JDBCUtils.safeGetString(dbResult, "KEYS");
        	this.filter = JDBCUtils.safeGetString(dbResult, "FILTER");
        	this.vocable = JDBCUtils.safeGetString(dbResult, "VOCABLE");
        	this.lexer = JDBCUtils.safeGetString(dbResult, "LEXER");
        	this.word_len = JDBCUtils.safeGetInt(dbResult, "WORD_LEN");
        	this.enable_trans = JDBCUtils.safeGetBoolean(dbResult, "ENABLE_TRANS");
        	this.create_time = JDBCUtils.safeGetDate(dbResult, "CREATE_TIME");
        	this.is_sys = JDBCUtils.safeGetBoolean(dbResult, "IS_SYS");
        	this.keepin_cache = JDBCUtils.safeGetBoolean(dbResult, "KEEPIN_CACHE");
        	this.nologging = JDBCUtils.safeGetBoolean(dbResult, "INDEX_ID");
        	this.valid = JDBCUtils.safeGetBoolean(dbResult, "INDEX_ID");
        }
//        this.tablespace = JDBCUtils.safeGetString(dbResult, "TABLESPACE_NAME");
    }

    public XuguTableIndex(XuguSchema schema, XuguTablePhysical parent, String name, boolean unique, DBSIndexType indexType)
    {
        super(schema, parent, name, indexType, false);
        this.nonUnique = !unique;

    }

    @NotNull
    @Override
    public XuguDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    @Property(viewable = true, order = 5)
    public boolean isUnique()
    {
        return !nonUnique;
    }

//    @Override
//    public Object getLazyReference(Object propertyId)
//    {
//        return tablespace;
//    }

//    @Property(viewable = true, order = 10)
//    @LazyProperty(cacheValidator = XuguTablespace.TablespaceReferenceValidator.class)
//    public Object getTablespace(DBRProgressMonitor monitor) throws DBException
//    {
//        return XuguTablespace.resolveTablespaceReference(monitor, this, null);
//    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public List<XuguTableIndexColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    @Nullable
    @Association
    public XuguTableIndexColumn getColumn(String columnName)
    {
        return DBUtils.findObject(columns, columnName);
    }

    void setColumns(List<XuguTableIndexColumn> columns)
    {
        this.columns = columns;
    }

    public void addColumn(XuguTableIndexColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        columns.add(column);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            this);
    }

    @Override
    public String toString()
    {
        return getFullyQualifiedName(DBPEvaluationContext.UI);
    }
    
    public boolean isNonUnique() {
		return nonUnique;
	}

	public List<XuguTableIndexColumn> getColumns() {
		return columns;
	}

	public int getIndex_id() {
		return index_id;
	}

	public String getIndex_name() {
		return index_name;
	}

	public String getIndex_typeName() {
		return index_typeName;
	}

	public boolean isIs_primary() {
		return is_primary;
	}

	public boolean isIs_unique() {
		return is_unique;
	}

	public boolean isIs_local() {
		return is_local;
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

	public int getField_num() {
		return field_num;
	}

	public String getKeys() {
		return keys;
	}

	public String getFilter() {
		return filter;
	}

	public String getVocable() {
		return vocable;
	}

	public String getLexer() {
		return lexer;
	}

	public int getWord_len() {
		return word_len;
	}

	public boolean isEnable_trans() {
		return enable_trans;
	}

	public Date getCreate_time() {
		return create_time;
	}

	public boolean isIs_sys() {
		return is_sys;
	}

	public boolean isKeepin_cache() {
		return keepin_cache;
	}

	public boolean isNologging() {
		return nologging;
	}

	public boolean isValid() {
		return valid;
	}

	public void setNonUnique(boolean nonUnique) {
		this.nonUnique = nonUnique;
	}

	public void setIndex_id(int index_id) {
		this.index_id = index_id;
	}

	public void setIndex_name(String index_name) {
		this.index_name = index_name;
	}

	public void setIndex_typeName(String index_typeName) {
		this.index_typeName = index_typeName;
	}

	public void setIs_primary(boolean is_primary) {
		this.is_primary = is_primary;
	}

	public void setIs_unique(boolean is_unique) {
		this.is_unique = is_unique;
	}

	public void setIs_local(boolean is_local) {
		this.is_local = is_local;
	}

	public void setParti_type(int parti_type) {
		this.parti_type = parti_type;
	}

	public void setParti_num(int parti_num) {
		this.parti_num = parti_num;
	}

	public void setParti_key(String parti_key) {
		this.parti_key = parti_key;
	}

	public void setGsto_no(int gsto_no) {
		this.gsto_no = gsto_no;
	}

	public void setCopy_num(int copy_num) {
		this.copy_num = copy_num;
	}

	public void setBlock_size(int block_size) {
		this.block_size = block_size;
	}

	public void setChunk_size(int chunk_size) {
		this.chunk_size = chunk_size;
	}

	public void setField_num(int field_num) {
		this.field_num = field_num;
	}

	public void setKeys(String keys) {
		this.keys = keys;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public void setVocable(String vocable) {
		this.vocable = vocable;
	}

	public void setLexer(String lexer) {
		this.lexer = lexer;
	}

	public void setWord_len(int word_len) {
		this.word_len = word_len;
	}

	public void setEnable_trans(boolean enable_trans) {
		this.enable_trans = enable_trans;
	}

	public void setCreate_time(Date create_time) {
		this.create_time = create_time;
	}

	public void setIs_sys(boolean is_sys) {
		this.is_sys = is_sys;
	}

	public void setKeepin_cache(boolean keepin_cache) {
		this.keepin_cache = keepin_cache;
	}

	public void setNologging(boolean nologging) {
		this.nologging = nologging;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}
}
