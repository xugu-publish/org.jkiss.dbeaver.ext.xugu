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

package org.jkiss.dbeaver.ext.xugu;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

/**
 * Xugu constants
 * 程序中被使用的一些常量
 */
public class XuguConstants {

	/**
     * Connection type
     */
    public enum ConnectionType {
        BASIC,
        TNS,
        CUSTOM
    }
    public static final int DEFAULT_PORT = 5138;
    public static final String DEFAULT_HOST = "localhost";
    public static final String CMD_COMPILE = "org.jkiss.dbeaver.ext.xugu.code.compile"; //$NON-NLS-1$

    public static final String SCHEMA_SYS = "SYS";
    public static final String VIEW_ALL_SOURCE = "ALL_SOURCE";
    public static final String VIEW_DBA_SOURCE = "DBA_SOURCE";
    public static final String VIEW_DBA_TAB_PRIVS = "DBA_TAB_PRIVS";

    public static final String[] SYSTEM_SCHEMAS = {
//        "SYSDBA",
        "SYSAUDITOR",
        "SYSSSO",
        "GUEST",
    };
    
    //默认的连接守护进程休眠时间1分钟
    public static final int DEFAULT_SLEEP_TIME = 60000;

    public static final String PROP_CONNECTION_TYPE = DBConstants.INTERNAL_PROP_PREFIX + "connection-type@";
    public static final String PROP_SERVER_TIMEZONE = DBConstants.INTERNAL_PROP_PREFIX + "serverTimezone@";
    
    public static final String[] TABLE_TYPES = new String[]{"TABLE", "VIEW"};
    
    public static final String TYPE_NAME_ENUM = "enum";
    public static final String TYPE_NAME_SET = "set";
    public static final String YES = "YES";
    
    public static final String COL_ID = "ID";
    public static final String COL_DEFAULT = "DEFAULT";
    public static final String COL_COMPILED = "COMPILED";
    public static final String COL_SORT_LENGTH = "SORTLEN";
    public static final String COL_COLLATION="COLLATION";
    
	//xfc 用户角色 SYSDBA DBA NORMAL
	public static final String PROP_INTERNAL_LOGON = "SYSDBA";
	public static final int EC_FEATURE_NOT_SUPPORTED = 0;
	public static final String OS_AUTH_PROP = null;
	public static final String PROP_USE_RULE_HINT = null;
	public static final String USER_PUBLIC = "GUEST";
	public static final String TYPE_NAME_XML = "XMLTYPE";
    public static final String TYPE_FQ_XML = "SYS.XMLTYPE";
    public static final String TYPE_NAME_BFILE = "BFILE";
    public static final String TYPE_NAME_TIMESTAMP = "TIMESTAMP";
	
    public static final DBSIndexType INDEX_TYPE_BTREE = new DBSIndexType("0", "BTree");
    public static final DBSIndexType INDEX_TYPE_RTREE = new DBSIndexType("1", "RTree");
    public static final DBSIndexType INDEX_TYPE_FULL_TEXT = new DBSIndexType("2","Full text");
    public static final DBSIndexType INDEX_TYPE_BITMAP = new DBSIndexType("3","Bitmap");
    
    public static final String COL_OWNER = "OWNER";
    public static final String COL_TABLE_NAME = "TABLE_NAME";
    public static final String COL_CONSTRAINT_NAME = "CONSTRAINT_NAME";
    public static final String COL_CONSTRAINT_TYPE = "CONSTRAINT_TYPE";
    
    public static final String PROP_OBJECT_DEFINITION = "objectDefinitionText";
    public static final String PROP_OBJECT_BODY_DEFINITION = "extendedDefinitionText";
	
    public static final String PREF_EXPLAIN_TABLE_NAME = "xugu.explain.table";
    public static final String PREF_SUPPORT_ROWID = "xugu.support.rowid";
    public static final String PREF_DBMS_OUTPUT = "xugu.dbms.output";
    public static final String PREF_DBMS_READ_ALL_SYNONYMS = "xugu.read.all.synonyms";
    public static final String PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING = "xugu.disable.script.escape";
    public static final String PREF_KEY_DDL_FORMAT = "xugu.ddl.format";
    
    // 默认值约束
    public static final DBSEntityConstraintType CONSTRAINT_DEFAULT = new DBSEntityConstraintType("xugu.default", "DEFAULT", XuguMessages.model_struct_default, false, false, false, false); //$NON-NLS-1$
    // 引用外键约束
    public static final DBSEntityConstraintType CONSTRAINT_REF_COLUMN = new DBSEntityConstraintType("xugu.ref.column", "Referential integrity", XuguMessages.model_struct_ref_column, false, false, false, false); //$NON-NLS-1$

    public static final int DATA_TYPE_TIMESTAMP_WITH_TIMEZONE = 101;
    public static final int DATA_TYPE_TIMESTAMP_WITH_LOCAL_TIMEZONE = 102;
    
    public static final String XMLTYPE_CLASS_NAME = "XMLType";
    public static final String[] DEFAULT_CHAR_SET = {"GBK", "GB2312", "UTF8"};
    public static final String DEF_PASSWORD_VALUE = "**********"; //$NON-NLS-1$
    public static final String DEF_UNTIL_TIME = "1970-1-1 07:00:00.933";
    
    public static final String[] DEF_DATABASE_AUTHORITY_LIST = {
    		"可创建任何数据库","可修改任何数据库","可删除任何数据库",
    		"可创建任何模式","可修改任何模式","可删除任何模式",
    		"可创建任何表","可修改任何表结构","可删除任何表","可引用任何表","可查询任何表","可插入记录，在任何表","可删除记录，在任何表","可更新记录，在任何表",
    		"可创建任何视图","可修改任何视图结构","可删除任何视图","可查询任何视图","可插入记录，在任何视图","可删除记录，在任何视图","可更新记录，在任何视图",
    		"可创建任何序列值","可修改任何序列值","可删除任何序列值","可读任何序列值","可更新任何序列值","可引用任何序列值",
    		"可创建任何包","可修改任何包","可删除任何包","可执行任何包",
    		"可创建任何存储过程或函数","可修改任何存储过程或函数","可删除任何存储过程或函数","可执行任何存储过程或函数",
    		"可创建任何触发器","可修改任何触发器","可删除任何触发器",
    		"可创建任何索引","可修改任何索引","可删除任何索引",
    		"可创建任何同义词","可修改任何同义词","可删除任何同义词",
    		"可创建任何用户","可修改任何用户","可删除任何用户",
    		"可创建任何定时作业","可修改任何定时作业","可删除任何定时作业",
    		"可创建任何角色","可修改任何角色","可删除任何角色",
    		"可创建任何UDT","可修改任何UDT","可删除任何UDT",
    		"可创建表","可创建视图","可创建序列值","可创建包","可创建存储过程或函数","可创建触发器","可创建索引","可创建同义词","可创建UDT"
    };
    public static final String[] DEF_TABLE_AUTHORITY_LIST= {
    		"可修改表结构","可删除表","可引用表","可读表","可插入记录，在表","可删除记录，在表","可更新记录，在表"
    };
    public static final String[] DEF_VIEW_AUTHORITY_LIST= {
    		"可修改视图结构","可删除视图","可读视图","可插入记录，在视图","可删除记录，在视图","可更新记录，在视图"
    };
    public static final String[] DEF_SEQUENCE_AUTHORITY_LIST= {
    		"可修改序列值","可删除序列值","可读序列值","可更新序列值","可引用序列值"
    };
    public static final String[] DEF_PACKAGE_AUTHORITY_LIST= {
    		"可修改包","可删除包","可执行包"
    };
    public static final String[] DEF_PROCEDURE_AUTHORITY_LIST= {
    		"可修改存储过程或函数","可删除存储过程或函数","可执行存储过程或函数"
    };
    public static final String[] DEF_TRIGGER_AUTHORITY_LIST= {
    		"可修改触发器","可删除触发器"
    };
    public static final String[] DEF_COLUMN_AUTHORITY_LIST= {
    		"可读列","可更新列"
    };
    
    public static final String[] DEF_OBJECT_TYPE_LIST = {
    		"TABLE",
    		"VIEW",
    		"SEQUENCE",
    		"TRIGGER",
    		"PACKAGE",
    		"PROCEDURE",
    		"COLUMN"
    };
}
