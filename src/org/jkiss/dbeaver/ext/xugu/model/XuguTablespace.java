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
import org.jkiss.dbeaver.ext.xugu.model.source.XuguSourceObject;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * Oracle tablespace
 */
//public class XuguTablespace extends XuguGlobalObject implements DBPRefreshableObject
public class XuguTablespace extends XuguGlobalObject
	implements DBPScriptObjectExt, XuguSourceObject
{

    public enum Status {
        ONLINE,
        OFFLINE,
        READ_ONLY
    }

    public enum Contents {
        PERMANENT,
        TEMPORARY,
        UNDO
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
    
    
//    private long blockSize;
//    private long initialExtent;
//    private long nextExtent;
//    private long minExtents;
//    private long maxExtents;
//    private long pctIncrease;
//    private long minExtLen;
//    private Status status;
//    private Contents contents;
//    private Logging logging;
//    private boolean forceLogging;
//    private ExtentManagement extentManagement;
//    private AllocationType allocationType;
//    private boolean pluggedIn;
//    private SegmentSpaceManagement segmentSpaceManagement;
//    private boolean defTableCompression;
//    private Retention retention;
//    private boolean bigFile;
//    private volatile Long availableSize;
//    private volatile Long usedSize;
//
//    final FileCache fileCache = new FileCache();
//    final SegmentCache segmentCache = new SegmentCache();

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
        System.out.println("TTTTable space ?? "+dbResult.getStatement().toString());
        this.name = JDBCUtils.safeGetString(dbResult, "SPACE_NAME");
        this.nodeID = JDBCUtils.safeGetInt(dbResult, "NODEID");
        this.space_ID = JDBCUtils.safeGetLong(dbResult, "SPACE_ID");
        this.dataFile_Num = JDBCUtils.safeGetInt(dbResult, "DATAFILE_NUM");
        this.space_Type = JDBCUtils.safeGetString(dbResult, "SPACE_TYPE");
        this.media_Error = JDBCUtils.safeGetBoolean(dbResult, "MEDIA_ERROR");
//        this.blockSize = JDBCUtils.safeGetLong(dbResult, "BLOCK_SIZE");
//        this.initialExtent = JDBCUtils.safeGetLong(dbResult, "INITIAL_EXTENT");
//        this.nextExtent = JDBCUtils.safeGetLong(dbResult, "NEXT_EXTENT");
//        this.minExtents = JDBCUtils.safeGetLong(dbResult, "MIN_EXTENTS");
//        this.maxExtents = JDBCUtils.safeGetLong(dbResult, "MAX_EXTENTS");
//        this.pctIncrease = JDBCUtils.safeGetLong(dbResult, "PCT_INCREASE");
//        this.minExtLen = JDBCUtils.safeGetLong(dbResult, "MIN_EXTLEN");
//        this.status = CommonUtils.valueOf(Status.class, JDBCUtils.safeGetString(dbResult, "STATUS"), true);
//        this.contents = CommonUtils.valueOf(Contents.class, JDBCUtils.safeGetString(dbResult, "CONTENTS"), true);
//        this.logging = CommonUtils.valueOf(Logging.class, JDBCUtils.safeGetString(dbResult, "LOGGING"), true);
//        this.forceLogging = JDBCUtils.safeGetBoolean(dbResult, "FORCE_LOGGING", "Y");
//        this.extentManagement = CommonUtils.valueOf(ExtentManagement.class, JDBCUtils.safeGetString(dbResult, "EXTENT_MANAGEMENT"), true);
//        this.allocationType = CommonUtils.valueOf(AllocationType.class, JDBCUtils.safeGetString(dbResult, "ALLOCATION_TYPE"), true);
//        this.pluggedIn = JDBCUtils.safeGetBoolean(dbResult, "PLUGGED_IN", "Y");
//        this.segmentSpaceManagement = CommonUtils.valueOf(SegmentSpaceManagement.class, JDBCUtils.safeGetString(dbResult, "SEGMENT_SPACE_MANAGEMENT"), true);
//        this.defTableCompression = "ENABLED".equals(JDBCUtils.safeGetString(dbResult, "DEF_TAB_COMPRESSION"));
//        this.retention = CommonUtils.valueOf(Retention.class, JDBCUtils.safeGetString(dbResult, "RETENTION"), true);
//        this.bigFile = JDBCUtils.safeGetBoolean(dbResult, "BIGFILE", "Y");
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 3)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, editable = true, order = 1)
    public int getNodeID()
    {
        return nodeID;
    }
    
    @Property(viewable = true, editable = true, order = 2)
    public long getSpaceID()
    {
        return space_ID;
    }
    
    @Property(viewable = true, editable = true, order = 4)
    public int getDataFileNum()
    {
        return dataFile_Num;
    }

    @Property(viewable = true, editable = true, order = 5)
    public String getSpaceType()
    {
        return space_Type;
    }
    
    @Property(viewable = true, editable = true, order = 6)
    public boolean getMediaError()
    {
        return media_Error;
    }

    static class SegmentCache extends JDBCObjectCache<XuguTablespace, XuguSegment<XuguTablespace>> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguTablespace owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + XuguUtils.getSysUserViewName(session.getProgressMonitor(), owner.getDataSource(), "SEGMENTS") +
                " WHERE TABLESPACE_NAME=? ORDER BY SEGMENT_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected XuguSegment<XuguTablespace> fetchObject(@NotNull JDBCSession session, @NotNull XuguTablespace owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new XuguSegment<>(session.getProgressMonitor(), owner, resultSet);
        }
    }

    static Object resolveTablespaceReference(DBRProgressMonitor monitor, DBSObjectLazy<XuguDataSource> referrer, @Nullable Object propertyId) throws DBException
    {
        final XuguDataSource dataSource = referrer.getDataSource();
        if (!dataSource.isAdmin()) {
            return referrer.getLazyReference(propertyId);
        } else {
            return XuguUtils.resolveLazyReference(monitor, dataSource, dataSource.getTablespaceCache(), referrer, propertyId);
        }
    }

    public static class TablespaceReferenceValidator implements IPropertyCacheValidator<DBSObjectLazy<XuguDataSource>> {
        @Override
        public boolean isPropertyCached(DBSObjectLazy<XuguDataSource> object, Object propertyId)
        {
            return
                object.getLazyReference(propertyId) instanceof XuguTablespace ||
                object.getLazyReference(propertyId) == null ||
                object.getDataSource().getTablespaceCache().isFullyCached() ||
                !object.getDataSource().isAdmin();
        }
    }
    
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBCException
    {
    	String sourceDeclaration = "";
        if ( monitor != null) {
            sourceDeclaration = XuguUtils.getSource(monitor, this, false, true);
        }
        return sourceDeclaration;
    }
	@Override
	public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public XuguSchema getSchema() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public DBSObjectState getObjectState() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		this.name = name;
	}
	@Override
	public void setObjectDefinitionText(String source) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public XuguSourceType getSourceType() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public DBEPersistAction[] getCompileActions() {
		// TODO Auto-generated method stub
		return null;
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

}
