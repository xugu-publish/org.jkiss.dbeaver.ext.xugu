package org.jkiss.dbeaver.ext.xugu.model;

import java.util.Iterator;
import java.util.Vector;

import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class XuguUserAuthority extends XuguAuthorityBase{
	private XuguUser parent;
	
	public XuguUserAuthority(XuguUser parent, String name, String targetName, boolean isDatabase, boolean persisted) {
		super(parent, name, targetName, isDatabase ,persisted);
		this.parent = parent;
	}
	
	public XuguUser getUser() {
		return this.parent;
	}
	
	@Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
	public String getUserName() {
		return this.parent.getName();
	}
	
	@Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 2)
	public String getName() {
		return this.name;
	}
	
	@Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
	public String getTargetName() {
		return this.targetName;
	}
	
	public boolean isDatabase() {
		return this.isDatabase;
	}
}
