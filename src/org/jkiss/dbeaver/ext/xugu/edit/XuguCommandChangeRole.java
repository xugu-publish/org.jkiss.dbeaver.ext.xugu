package org.jkiss.dbeaver.ext.xugu.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguMessages;
import org.jkiss.dbeaver.ext.xugu.XuguUtils;
import org.jkiss.dbeaver.ext.xugu.model.XuguRole;
import org.jkiss.dbeaver.ext.xugu.model.XuguRoleAuthority;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;
/**
 * @author Maple4Real
 * 角色属性修改逻辑
 * 根据界面上设置的用户相关属性生成指定数据库操作action
 */
public class XuguCommandChangeRole extends DBECommandComposite<XuguRole, RolePropertyHandler> {

    protected XuguCommandChangeRole(XuguRole role)
    {
        super(role, "Alter Role");
    }

    @Override
    public void updateModel()
    {
        for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
            switch (RolePropertyHandler.valueOf((String) entry.getKey())) {
                case NAME: getObject().setName(CommonUtils.toString(entry.getValue())); break;
                default:
                    break;
            }
        }
    }

    @Override
    public DBEPersistAction[] getPersistActions(DBRProgressMonitor monitor, Map<String, Object> options)
    {
        List<DBEPersistAction> actions = new ArrayList<>();
        boolean newRole = !getObject().isPersisted();
        //创建新角色
        if (newRole) {
        	String user = getObject().getUserDesc();
            XuguRole role = getObject();
            String sql = "CREATE ROLE " + role.getName();
            if(user!=null && !user.equals("")) {
            	sql += " INIT USER "+user;
            }
            actions.add(new SQLDatabasePersistAction("Create role", sql));
        }
        //修改角色权限
        else {
        	StringBuilder script = new StringBuilder();
            //对权限做额外处理
            //库级权限
            Collection<XuguRoleAuthority> oldAuthorities = getObject().getRoleDatabaseAuthorities();
            //对象级权限
            Collection<XuguRoleAuthority> oldAuthorities2 = getObject().getRoleObjectAuthorities();
            //二级对象权限
            Collection<XuguRoleAuthority> oldAuthorities3 = getObject().getRoleSubObjectAuthorities();
			Iterator<XuguRoleAuthority> it;
			it = oldAuthorities3.iterator();
			while(it.hasNext()) {
				XuguRoleAuthority authority = it.next();
				if(!(authority.getName().contains("列")||authority.getName().contains("触发器"))) {
					oldAuthorities3.remove(authority);
				}
			}
			String schema = "";
			String objectType = "";
			String object = "";
			String realTargetName = "";
			String[] newAuthorities = null;
			XuguRoleAuthority authority = null;
            for(Map.Entry<Object, Object> entry:getProperties().entrySet()) {
            	switch(RolePropertyHandler.valueOf((String)entry.getKey())) {
            		case DATABASE_AUTHORITY:
            			//遍历新权限列表，若旧权限不存在于其中，则做revoke操作
            			it = oldAuthorities.iterator();
            			newAuthorities = (String[]) entry.getValue();
            			while(it.hasNext()) {
            				authority = it.next();
        					boolean inListFlag = false;
            				for(int i=0, l=newAuthorities.length; i<l; i++) {
            					if(authority.getName().equals(newAuthorities[i])) {
                					inListFlag = true;
                					break;
                				}
            				}
            				//旧权限不在列表中则revoke
                			if(!inListFlag && authority!=null) {
                				actions.add(new SQLDatabasePersistAction("Revoke role", 
                						"REVOKE "+XuguUtils.transformAuthority(authority.getName(), true)+" FROM "+getObject().getName()));
                			}
            			}
            			//遍历旧权限列表，若新权限不存在于其中，则做grant操作
            			it = oldAuthorities.iterator();
            			for(int i=0, l=newAuthorities.length; i<l; i++) {
            				boolean inListFlag = false;
            				while(it.hasNext()) {
            					authority = it.next();
        						if(authority.getName().equals(newAuthorities[i])) {
                					inListFlag = true;
                					break;
                				}
            				}
            				//新权限不在列表中则grant
                			if(!inListFlag) {
                				actions.add(new SQLDatabasePersistAction("Grant user", 
                						"GRANT "+XuguUtils.transformAuthority(newAuthorities[i], true)+" TO "+getObject().getName()));
                			}
            			}
            			break;
            		case OBJECT_AUTHORITY:
            			it = oldAuthorities2.iterator();
            			newAuthorities = (String[]) entry.getValue();
            			schema = getProperties().get("TARGET_SCHEMA").toString();
            			objectType = getProperties().get("TARGET_TYPE").toString();
            			object = getProperties().get("TARGET_OBJECT").toString();
            			realTargetName = "\""+schema+"\".\""+object+"\"";
            			//遍历新权限列表，若旧权限不存在于其中，则做revoke操作
            			while(it.hasNext()) {
            				authority = it.next();
        					boolean inListFlag = false;
            				for(int i=0, l=newAuthorities.length; i<l; i++) {
            					if(authority.getName().contains(newAuthorities[i]) && authority.getTargetName().equals(realTargetName)) {
                					inListFlag = true;
                					break;
                				}
            				}
            				//旧权限不在列表中则revoke
                			if(!inListFlag && authority!=null) {
            					actions.add(new SQLDatabasePersistAction("Revoke role", 
                						"REVOKE "+XuguUtils.transformAuthority(authority.getName(), false)+" "+"\""+schema+"\".\""+object+"\""+" FROM "+getObject().getName()));	
                			}
            			}
            			//遍历旧权限列表，若新权限不存在于其中，则做grant操作
            			it = oldAuthorities2.iterator();
            			for(int i=0, l=newAuthorities.length; i<l; i++) {
            				boolean inListFlag = false;
            				while(it.hasNext()) {
            					authority = it.next();
        						if(authority.getName().contains(newAuthorities[i]) && authority.getTargetName().equals(realTargetName)) {
                					inListFlag = true;
                					break;
                				}
            				}
            				//新权限不在列表中则grant
                			if(!inListFlag) {
            					actions.add(new SQLDatabasePersistAction("Grant role", 
                						"GRANT "+XuguUtils.transformAuthority(newAuthorities[i], false)+" "+realTargetName+" TO "+getObject().getName()));	
                			}
            			}
            			break;
            		case SUB_OBJECT_AUTHORITY:
            			it = oldAuthorities3.iterator();
            			newAuthorities = (String[]) entry.getValue();
            			schema = getProperties().get("TARGET_SCHEMA").toString();
            			object = getProperties().get("TARGET_OBJECT").toString();
            			objectType = getProperties().get("TARGET_TYPE").toString();
            			String subObject = getProperties().get("SUB_TARGET_OBJECT").toString();
            			String subObjectType = getProperties().get("SUB_TARGET_TYPE").toString();
            			realTargetName = "\""+schema+"\".\""+object+"\""+".\""+subObject+"\"";
            			
            			//遍历新权限列表，若旧权限不存在于其中，则做revoke操作
            			while(it.hasNext()) {
            				authority = it.next();
        					boolean inListFlag = false;
            				for(int i=0, l=newAuthorities.length; i<l; i++) {
            					if(authority.getName().contains(newAuthorities[i]) && authority.getTargetName().equals(realTargetName)) {
                					inListFlag = true;
                					break;
                				}
            				}
            				//旧权限不在列表中则revoke
                			if(!inListFlag && authority!=null) {
                				if(!"COLUMN".equals(subObjectType)) {
                					actions.add(new SQLDatabasePersistAction("Revoke role", 
                    						"REVOKE "+XuguUtils.transformAuthority(authority.getName(), false)+" "+"\""+schema+"\".\""+object+"\""+" FROM "+getObject().getName()));	
                				}
                				//对列对象做特殊处理
                				else {
                					actions.add(new SQLDatabasePersistAction("Revoke role", 
                    						"REVOKE "+XuguUtils.transformColumnAuthority(authority.getName())+"("+subObject+") ON "+"\""+schema+"\".\""+object+"\""+" FROM "+getObject().getName()));
                				}
                			}
            			}
            			//遍历旧权限列表，若新权限不存在于其中，则做grant操作
            			it = oldAuthorities3.iterator();
            			for(int i=0, l=newAuthorities.length; i<l; i++) {
            				boolean inListFlag = false;
            				while(it.hasNext()) {
            					authority = it.next();
        						if(authority.getName().contains(newAuthorities[i]) && authority.getTargetName().equals(realTargetName)) {
                					inListFlag = true;
                					break;
                				}
            				}
            				//新权限不在列表中则grant
                			if(!inListFlag) {
                				if(!"COLUMN".equals(subObjectType)) {
                					actions.add(new SQLDatabasePersistAction("Grant role", 
                    						"GRANT "+XuguUtils.transformAuthority(newAuthorities[i], false)+" "+realTargetName+" TO "+getObject().getName()));	
                				}
                				//对列类型做特殊处理
                				else {
                					actions.add(new SQLDatabasePersistAction("Grant role", 
                    						"GRANT "+XuguUtils.transformColumnAuthority(newAuthorities[i])+"("+subObject+") ON "+"\""+schema+"\".\""+object+"\""+" TO "+getObject().getName()));
                				}
                			}
            			}
            			break;
            	}
            }
        }
        return actions.toArray(new DBEPersistAction[actions.size()]);
    }
}
