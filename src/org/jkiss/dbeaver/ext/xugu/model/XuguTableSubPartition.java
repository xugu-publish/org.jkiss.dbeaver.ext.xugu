package org.jkiss.dbeaver.ext.xugu.model;

import java.sql.ResultSet;

/**
 * @author Maple4Real
 *   二级表分区信息类
 */
public class XuguTableSubPartition extends XuguPartitionBase<XuguTablePhysical> {
	
	public XuguTableSubPartition(XuguTablePhysical xuguTable,
	        boolean subpartition, String name) 
	{
		super(xuguTable, subpartition, name);
	}
	
	public XuguTableSubPartition(XuguTablePhysical xuguTable,
			boolean subpartition, ResultSet dbResult)
	{
		super(xuguTable, subpartition, dbResult);
	}
}
