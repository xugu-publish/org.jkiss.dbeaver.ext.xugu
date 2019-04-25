package org.jkiss.dbeaver.ext.xugu.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSObject;

import com.xugu.permission.LoadPermission;

public abstract class XuguAuthorityBase<PARENT extends DBSObject> extends XuguObject<PARENT>{

	private String parentName;
	protected String targetName;
	protected boolean isDatabase;
	
	protected XuguAuthorityBase(PARENT parent, String name, String targetName, boolean isDatabase, boolean persisted) {
		super(parent, name, persisted);
		this.parentName = parent.getName();
		this.targetName = targetName;
		this.isDatabase = isDatabase;
	}

	public boolean isDatabase() {
		return this.isDatabase;
	}
}
