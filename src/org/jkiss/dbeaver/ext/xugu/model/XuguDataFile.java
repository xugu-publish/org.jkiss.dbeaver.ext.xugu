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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;

/**
 * Oracle tablespace file
 */
public class XuguDataFile extends XuguObject<XuguTablespace> {

    public enum OnlineStatus {
        SYSOFF,
        SYSTEM,
        OFFLINE,
        ONLINE,
        RECOVER,
    }

    private long nodeID;
    private long spaceID;
    private String path;
    private int fileNo;
    private BigDecimal max_size;
    private BigDecimal step_size;
    
    private final XuguTablespace tablespace;
//    private long id;
//    private long relativeNo;
//    private BigDecimal bytes;
//    private BigDecimal blocks;
//    private BigDecimal maxBytes;
//    private BigDecimal maxBlocks;
//    private long incrementBy;
//    private BigDecimal userBytes;
//    private BigDecimal userBlocks;
//
//    private boolean available;
//    private boolean autoExtensible;
//    private OnlineStatus onlineStatus;

    private boolean temporary;

    protected XuguDataFile(XuguTablespace tablespace, ResultSet dbResult, boolean temporary)
    {
        super(
            tablespace,
            JDBCUtils.safeGetString(dbResult, "PATH").substring(JDBCUtils.safeGetString(dbResult, "PATH").lastIndexOf("/")+1),
            true);
        this.tablespace = tablespace;
        this.temporary = temporary;
        this.nodeID = JDBCUtils.safeGetLong(dbResult, "NODEID");
        this.spaceID = JDBCUtils.safeGetLong(dbResult, "SPACE_ID");
        this.path = JDBCUtils.safeGetString(dbResult, "PATH");
        this.max_size = JDBCUtils.safeGetBigDecimal(dbResult, "MAX_SIZE");
        this.step_size = JDBCUtils.safeGetBigDecimal(dbResult, "STEP_SIZE");
        this.fileNo = JDBCUtils.safeGetInt(dbResult, "FILE_NO");
//        if (!this.temporary) {
//            this.onlineStatus = CommonUtils.valueOf(OnlineStatus.class, JDBCUtils.safeGetStringTrimmed(dbResult, "ONLINE_STATUS"));
//        }
    }

    public XuguTablespace getTablespace()
    {
        return tablespace;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(order = 2)
    public long getNodeID()
    {
        return nodeID;
    }

    @Property(order = 3)
    public long getSpaceID()
    {
        return spaceID;
    }

    @Property(order = 3)
    public int getFileNo()
    {
        return fileNo;
    }
    
    @Property(viewable = true, order = 6)
    public BigDecimal getMaxSize()
    {
        return max_size;
    }

    @Property(viewable = true, order = 7)
    public BigDecimal getStepSize()
    {
        return step_size;
    }

    @Property(order = 14)
    public boolean isTemporary()
    {
        return temporary;
    }

}
