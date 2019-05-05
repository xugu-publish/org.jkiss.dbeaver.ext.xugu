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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Maple4Real
 *	编码方式
 */
public class XuguCharset extends XuguInformation {

    private String name;
    private Connection conn;
    private List<XuguCollation> collations = new ArrayList<>();

    public XuguCharset(XuguDataSource dataSource, Connection conn, String name)
        throws SQLException
    {
        super(dataSource);
        this.name = name;
        this.conn = conn;
        this.loadInfo();
    }

    public void loadInfo()
        throws SQLException
    {
    	Statement stmt = this.conn.createStatement();
    	ResultSet rs = stmt.executeQuery("SELECT * FROM SYS_CHARSETS WHERE CHARSET_NAME='"+this.name+"'");
    	while(rs.next()) {
    		XuguCollation temp = new XuguCollation(this, rs);
    		addCollation(temp);
    	}
    }

    void addCollation(XuguCollation collation)
    {
        collations.add(collation);
        Collections.sort(collations, DBUtils.nameComparator());
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public List<XuguCollation> getCollations()
    {
        return collations;
    }

    @Property(viewable = true, order = 2)
    public XuguCollation getDefaultCollation()
    {
        for (XuguCollation collation : collations) {
            return collation;
        }
        return null;
    }

    public XuguCollation getCollation(String name) {
        for (XuguCollation collation : collations) {
            if (collation.getName().equals(name)) {
                return collation;
            }
        }
        return null;
    }

    @Nullable
    @Override
    @Property(viewable = true, multiline = true, order = 100)
    public String getDescription()
    {
        return name;
    }

}
