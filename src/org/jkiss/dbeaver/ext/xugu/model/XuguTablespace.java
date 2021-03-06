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
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * @author Maple4Real
 *   表空间信息类，包含表空间基本信息
 */
public class XuguTablespace extends XuguGlobalObject implements DBPRefreshableObject
{

	private static final Log log = Log.getLog(XuguTablespace.class);
	
    public enum Status {
        ONLINE,
        OFFLINE,
        READ_ONLY
    }

    public enum Contents {
        PERMANENT,
        TEMPORARY
    }

    public enum Logging {
        LOGGING,
        NOLOGGING,
    }

    public enum ExtentManagement {
        DICTIONARY,
        LOCAL
    }

    public enum AllocationType {
        SYSTEM,
        UNIFORM,
        USER,
    }

    public enum SegmentSpaceManagement {
        MANUAL,
        AUTO
    }

    public enum Retention {
        GUARANTEE,
        NOGUARANTEE,
        NOT_APPLY
    }

    private String name;
    private XuguDataFile file;
    final FileCache fileCache = new FileCache();

    private int nodeID;
    private long space_ID;
    private int dataFile_Num;
    private String space_Type;
    private boolean media_Error;
    private String filePath;
    
    public XuguTablespace(XuguDataSource dataSource, String tsName) throws SQLException
    {
    	super(dataSource, true);
    	this.name = tsName;
    }
    
    protected XuguTablespace(XuguDataSource dataSource, ResultSet dbResult) throws SQLException
    {
        super(dataSource, true);
        this.name = JDBCUtils.safeGetString(dbResult, "SPACE_NAME");
        this.nodeID = JDBCUtils.safeGetInt(dbResult, "NODEID");
        this.space_ID = JDBCUtils.safeGetLong(dbResult, "SPACE_ID");
        this.dataFile_Num = JDBCUtils.safeGetInt(dbResult, "DATAFILE_NUM");
        this.space_Type = JDBCUtils.safeGetString(dbResult, "SPACE_TYPE");
        this.media_Error = JDBCUtils.safeGetBoolean(dbResult, "MEDIA_ERROR");
    }

    static class FileCache extends JDBCObjectCache<XuguTablespace, XuguDataFile> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguTablespace owner) throws SQLException
        {
        	StringBuilder desc = new StringBuilder(100);
        	desc.append("SELECT * FROM ");
        	desc.append(owner.getDataSource().getRoleFlag());
        	desc.append("_DATAFILES");
        	SQLUtils.appendFirstClause(desc, true);
        	desc.append("SPACE_ID=");
        	desc.append(owner.getSpaceID());
            
            log.debug("[Xugu] Construct view tablespace sql: "+ desc.toString());
            JDBCPreparedStatement dbStat = session.prepareStatement(desc.toString());
            
            return dbStat;
        }

        @Override
        protected XuguDataFile fetchObject(@NotNull JDBCSession session, @NotNull XuguTablespace owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguDataFile(owner, resultSet, "TEMP_SPACE".equals(owner.getSpaceType()));
        }
    }
    
    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 3)
    public String getName()
    {
        return name;
    }
    
	@Property(viewable = true, editable = false, order = 1)
    public int getNodeID()
    {
        return nodeID;
    }
    
    @Property(viewable = true, editable = false, order = 2)
    public long getSpaceID()
    {
        return space_ID;
    }
    
    @Property(viewable = true, editable = false, order = 4)
    public int getDataFileNum()
    {
        return dataFile_Num;
    }

    @Property(viewable = true, editable = false, order = 5)
    public String getSpaceType()
    {
        return space_Type;
    }
    
    @Property(viewable = true, editable = false, order = 6)
    public boolean getMediaError()
    {
        return media_Error;
    }

	public void setName(String name) {
		// TODO Auto-generated method stub
		this.name = name;
	}
	
	public String getFilePath() {
		return filePath;
	}
	
	public void setFilePath(String path) {
		// TODO Auto-generated method stub
		filePath = path;
	}
	
	public void setNodeID(int id) {
		this.nodeID = id;
	}

	@Association
    public Collection<XuguDataFile> getFiles(DBRProgressMonitor monitor)
        throws DBException
    {
        return fileCache.getAllObjects(monitor, this);
    }
	
    public XuguDataFile getFile()
    {
        return file;
    }

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		fileCache.clearCache();
		return this;
	}
}
