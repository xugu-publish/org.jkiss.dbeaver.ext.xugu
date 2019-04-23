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

package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

/**
 * Xugu constants
 */
public class XuguConstants {

    public static final String CMD_COMPILE = "org.jkiss.dbeaver.ext.xugu.code.compile"; //$NON-NLS-1$

    public static final int DEFAULT_PORT = 5138;

    public static final String SCHEMA_SYS = "SYS";
    public static final String VIEW_ALL_SOURCE = "ALL_SOURCE";
    public static final String VIEW_DBA_SOURCE = "DBA_SOURCE";
    public static final String VIEW_DBA_TAB_PRIVS = "DBA_TAB_PRIVS";

    public static final String[] SYSTEM_SCHEMAS = {
        "SYSDBA",
        "SYSAUDITOR",
        "SYSSSO",
        "GUEST",
    };

    public static final String PROP_CONNECTION_TYPE = DBConstants.INTERNAL_PROP_PREFIX + "connection-type@";
    public static final String PROP_SID_SERVICE = DBConstants.INTERNAL_PROP_PREFIX + "sid-service@";
    public static final String PROP_CONNECTION_TARGET = "connection_target";
    public static final String PROP_DRIVER_TYPE = DBConstants.INTERNAL_PROP_PREFIX + "driver-type@";
    public static final String PROP_INTERNAL_LOGON = DBConstants.INTERNAL_PROP_PREFIX + "internal-logon@";
    public static final String PROP_TNS_PATH = DBConstants.INTERNAL_PROP_PREFIX + "tns-path@";

    public static final String PROP_SESSION_LANGUAGE = DBConstants.INTERNAL_PROP_PREFIX + "session-language@";
    public static final String PROP_SESSION_TERRITORY = DBConstants.INTERNAL_PROP_PREFIX + "session-territory@";
    public static final String PROP_SESSION_NLS_DATE_FORMAT = DBConstants.INTERNAL_PROP_PREFIX + "session-nls-date-format@";
    public static final String PROP_CHECK_SCHEMA_CONTENT = DBConstants.INTERNAL_PROP_PREFIX + "check-schema-content@";
    public static final String PROP_ALWAYS_SHOW_DBA = DBConstants.INTERNAL_PROP_PREFIX + "always-show-dba@";
    public static final String PROP_ALWAYS_USE_DBA_VIEWS = DBConstants.INTERNAL_PROP_PREFIX + "always-use-dba-views@";
    public static final String PROP_USE_RULE_HINT = DBConstants.INTERNAL_PROP_PREFIX + "use-rule-hint@";

    public static final String OS_AUTH_PROP = DBConstants.INTERNAL_PROP_PREFIX + "os-authentication@";

    public static final String DRIVER_TYPE_THIN = "THIN";
    public static final String DRIVER_TYPE_OCI = "OCI";

    public static final String USER_PUBLIC = "PUBLIC";

    public static final String YES = "YES";

    public static final String TYPE_NAME_XML = "XMLTYPE";
    public static final String TYPE_FQ_XML = "SYS.XMLTYPE";
    public static final String TYPE_NAME_BFILE = "BFILE";
    public static final String TYPE_NAME_DATE = "DATE";
    public static final String TYPE_NAME_TIMESTAMP = "TIMESTAMP";

    public static final int TIMESTAMP_TYPE_LENGTH = 13;
    public static final int DATE_TYPE_LENGTH = 7;

    public static final DBSIndexType INDEX_TYPE_BTREE = new DBSIndexType("0", "BTree");
    public static final DBSIndexType INDEX_TYPE_RTREE = new DBSIndexType("1", "RTree");
    public static final DBSIndexType INDEX_TYPE_FULL_TEXT = new DBSIndexType("2","Full text");
    
    public static final String PROP_OBJECT_DEFINITION = "objectDefinitionText";
    public static final String PROP_OBJECT_BODY_DEFINITION = "extendedDefinitionText";

    public static final String COL_OWNER = "OWNER";
    public static final String COL_TABLE_NAME = "TABLE_NAME";
    public static final String COL_CONSTRAINT_NAME = "CONSTRAINT_NAME";
    public static final String COL_CONSTRAINT_TYPE = "CONSTRAINT_TYPE";

    public static final String XML_COLUMN_NAME = "XML";
    public static final String OBJECT_VALUE_COLUMN_NAME = "OBJECT_VALUE";

    public static final DBDPseudoAttribute PSEUDO_ATTR_ROWID = new DBDPseudoAttribute(
        DBDPseudoAttributeType.ROWID,
        "ROWID",
        "$alias.ROWID",
        null,
        "Unique row identifier",
        true);

    public static final String PREF_EXPLAIN_TABLE_NAME = "xugu.explain.table";
    public static final String PREF_SUPPORT_ROWID = "xugu.support.rowid";
    public static final String PREF_DBMS_OUTPUT = "xugu.dbms.output";
    public static final String PREF_DBMS_READ_ALL_SYNONYMS = "xugu.read.all.synonyms";
    public static final String PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING = "xugu.disable.script.escape";

    public static final String NLS_DEFAULT_VALUE = "Default";
    public static final String PREF_KEY_DDL_FORMAT = "xugu.ddl.format";
    public static final int MAXIMUM_DBMS_OUTPUT_SIZE = 1000000;

    public static final String VAR_ORA_HOME = "XG_HOME";
    public static final String VAR_ORACLE_HOME = "XUGU_HOME";
    public static final String VAR_TNS_ADMIN = "TNS_ADMIN";
    public static final String VAR_PATH = "PATH";
    public static final String VAR_ORACLE_NET_TNS_ADMIN = "xugu.net.tns_admin";

    public static final int DATA_TYPE_TIMESTAMP_WITH_TIMEZONE = 101;
    public static final int DATA_TYPE_TIMESTAMP_WITH_LOCAL_TIMEZONE = 102;

    public static final DBSEntityConstraintType CONSTRAINT_WITH_CHECK_OPTION = new DBSEntityConstraintType("V", "With Check Option", null, false, false, false);
    public static final DBSEntityConstraintType CONSTRAINT_WITH_READ_ONLY = new DBSEntityConstraintType("O", "With Read Only", null, false, false, false);
    public static final DBSEntityConstraintType CONSTRAINT_HASH_EXPRESSION = new DBSEntityConstraintType("H", "Hash expression", null, false, false, false);
    public static final DBSEntityConstraintType CONSTRAINT_REF_COLUMN = new DBSEntityConstraintType("F", "Constraint that involves a REF column", null, false, false, false);
    public static final DBSEntityConstraintType CONSTRAINT_SUPPLEMENTAL_LOGGING = new DBSEntityConstraintType("S", "Supplemental logging", null, false, false, false);
    public static final DBSEntityConstraintType CONSTRAINT_DEFAULT = new DBSEntityConstraintType("D","Constraint that indicates a default value", null, false, false, false);
    public static final DBSEntityConstraintType CONSTRAINT_NOT_NULL = new DBSEntityConstraintType("N","Constraint that indicates this column can not be null", null, false, false, false);
    /**
     * Xugu error codes
     */
    public static final int EC_FEATURE_NOT_SUPPORTED = 17023;
    public static final int EC_PASSWORD_EXPIRED = 28001;

    /**
     * Connection type
     */
    public enum ConnectionType {
        BASIC,
        TNS,
        CUSTOM
    }

    public static final String XMLTYPE_CLASS_NAME = "oracle.xdb.XMLType";
    public static final String BFILE_CLASS_NAME = "oracle.sql.BFILE";
    public static final String TIMESTAMP_CLASS_NAME     = "oracle.sql.TIMESTAMP";
    public static final String TIMESTAMPTZ_CLASS_NAME   = "oracle.sql.TIMESTAMPTZ";
    public static final String TIMESTAMPLTZ_CLASS_NAME  = "oracle.sql.TIMESTAMPLTZ";

    public static final String PLAN_TABLE_DEFINITION =
        "create global temporary table ${TABLE_NAME} (\n" +
            "statement_id varchar2(30),\n" +
            "plan_id number,\n" +
            "timestamp date,\n" +
            "remarks varchar2(4000),\n" +
            "operation varchar2(30),\n" +
            "options varchar2(255),\n" +
            "object_node varchar2(128),\n" +
            "object_owner varchar2(30),\n" +
            "object_name varchar2(30),\n" +
            "object_alias varchar2(65),\n" +
            "object_instance numeric,\n" +
            "object_type varchar2(30),\n" +
            "optimizer varchar2(255),\n" +
            "search_columns number,\n" +
            "id numeric,\n" +
            "parent_id numeric,\n" +
            "depth numeric,\n" +
            "position numeric,\n" +
            "cost numeric,\n" +
            "cardinality numeric,\n" +
            "bytes numeric,\n" +
            "other_tag varchar2(255),\n" +
            "partition_start varchar2(255),\n" +
            "partition_stop varchar2(255),\n" +
            "partition_id numeric,\n" +
            "other long,\n" +
            "distribution varchar2(30),\n" +
            "cpu_cost numeric,\n" +
            "io_cost numeric,\n" +
            "temp_space numeric,\n" +
            "access_predicates varchar2(4000),\n" +
            "filter_predicates varchar2(4000),\n" +
            "projection varchar2(4000),\n" +
            "time numeric,\n" +
            "qblock_name varchar2(30),\n" +
            "other_xml clob\n" +
            ") on commit preserve rows";
    public static final String COL_DEFAULT_CHARACTER_SET_NAME = "UTF-8";

	public static final int DEFAULT_SLEEP_TIME = 60000;	
	public static final String[] DEFAULT_CHAR_SET = {"GBK", "GB2312", "UTF-8"};
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
