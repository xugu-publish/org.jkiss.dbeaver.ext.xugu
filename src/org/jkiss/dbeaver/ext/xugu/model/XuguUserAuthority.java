package org.jkiss.dbeaver.ext.xugu.model;

import java.util.Iterator;
import java.util.Vector;

import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class XuguUserAuthority extends XuguAuthorityBase{
	
	protected XuguUserAuthority(DBSObject parent, String name, boolean persisted) {
		super(parent, name, persisted);
	}

	@Override
	@Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
	public Vector<String> getDatabaseAuthority(){
		return super.getDatabaseAuthority();
	}
	
	@Override
	@Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 2)
	public Vector<String> getObjectAuthority(){
		return super.getObjectAuthority();
	}
}
