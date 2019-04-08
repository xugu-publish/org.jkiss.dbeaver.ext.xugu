package org.jkiss.dbeaver.ext.xugu.model;

import java.sql.ResultSet;

public class XuguTableSubPartition extends XuguPartitionBase<XuguTablePhysical> {
//  private List<XuguTablePartition> subPartitions;
	public XuguTableSubPartition(XuguTablePhysical xuguTable,
	        boolean subpartition, String name) {
		super(xuguTable, subpartition, name);
	}
	
  public XuguTableSubPartition(
      XuguTablePhysical xuguTable,
      boolean subpartition,
      ResultSet dbResult)
  {
      super(xuguTable, subpartition, dbResult);
  }
}
