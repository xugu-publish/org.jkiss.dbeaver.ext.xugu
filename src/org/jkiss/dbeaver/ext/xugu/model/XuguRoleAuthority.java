package org.jkiss.dbeaver.ext.xugu.model;

import java.util.Vector;

import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
/**
 * @author Maple4Real
 *   角色权限信息类
 */
public class XuguRoleAuthority extends XuguAuthorityBase{
	boolean isDatabase;
	protected XuguRoleAuthority(DBSObject parent, String name, String targetName, boolean isDatabase, boolean persisted) {
		super(parent, name, targetName, isDatabase, persisted);
		this.isDatabase = isDatabase;
	}

	public boolean isDatabase() {
		// TODO Auto-generated method stub
		return this.isDatabase;
	}

	@Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
	public String getTargetName() {
		return this.targetName;
	}
	
}
