package org.jkiss.dbeaver.ext.xugu.edit;

import org.jkiss.dbeaver.ext.xugu.model.XuguRole;
import org.jkiss.dbeaver.ext.xugu.model.XuguUser;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyReflector;

public enum RolePropertyHandler implements DBEPropertyHandler<XuguRole>, DBEPropertyReflector<XuguRole> {
    NAME,
    HOST,
    PASSWORD,
    PASSWORD_CONFIRM,
    LOCKED,
    EXPIRED,
    UNTIL_TIME,
    ROLE_LIST,
    DATABASE_AUTHORITY,
    OBJECT_AUTHORITY,
    SUB_OBJECT_AUTHORITY,
    TARGET_SCHEMA,
    TARGET_TYPE,
    TARGET_OBJECT,
    SUB_TARGET_TYPE,
    SUB_TARGET_OBJECT,
    MAX_QUERIES,
    MAX_UPDATES,
    MAX_CONNECTIONS,
    MAX_USER_CONNECTIONS;


    @Override
    public Object getId()
    {
        return name();
    }

    @Override
    public XuguCommandChangeRole createCompositeCommand(XuguRole object)
    {
        return new XuguCommandChangeRole(object);
    }

    @Override
    public void reflectValueChange(XuguRole object, Object oldValue, Object newValue)
    {
    	//为了修改用户名而保留旧名称 不做即时反射更新
//        if (this == NAME) {
//            if (this == NAME) {
//                object.setName(CommonUtils.toString(newValue));
//            }
//            DBUtils.fireObjectUpdate(object);
//        }
    }
}
