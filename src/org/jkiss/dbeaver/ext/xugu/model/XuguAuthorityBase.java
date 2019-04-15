package org.jkiss.dbeaver.ext.xugu.model;

import java.util.Iterator;
import java.util.Vector;

import org.jkiss.dbeaver.model.struct.DBSObject;

import com.xugu.permission.LoadPermission;

public abstract class XuguAuthorityBase<PARENT extends DBSObject> extends XuguObject<PARENT>{

	private String parentName;
	private Vector<Object> authorityList;
	private Vector<String> databaseAuthority;
	private Vector<String> objectAuthority;
	
	protected XuguAuthorityBase(PARENT parent, String name, boolean persisted) {
		super(parent, name, persisted);
		this.parentName = parent.getName();
		this.authorityList = new LoadPermission().loadPermission(((XuguDataSource)parent.getDataSource()).connection, parentName);
		Iterator<Object> it = authorityList.iterator();
		databaseAuthority = new Vector<>();
		objectAuthority = new Vector<>();
		while(it.hasNext()) {
			String temp = it.next().toString();
			if(temp.indexOf(":")==-1) {
				databaseAuthority.add(temp);
			}else {
				String head = temp.split(":")[0];
				String values = temp.substring(temp.indexOf(":"));
				String[] valueList = values.split(",");
				for(int i=0; i<valueList.length; i++) {
					objectAuthority.add(head+valueList[i]);
				}
			}
		}
	}

	protected Vector<Object> getAuthorityList(){
		return authorityList;
	}
	
	public Vector<String> getDatabaseAuthority(){
		return databaseAuthority;
	}
	
	public Vector<String> getObjectAuthority(){
		return objectAuthority;
	}
}
