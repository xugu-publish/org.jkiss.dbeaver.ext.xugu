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
package org.jkiss.dbeaver.ext.xugu.views;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.xugu.model.XuguCharset;
import org.jkiss.dbeaver.ext.xugu.model.XuguCollation;
import org.jkiss.dbeaver.ext.xugu.model.XuguDataSource;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

/**
 * XuguCreateDatabaseDialog
 */
public class XuguCreateDatabaseDialog extends BaseDialog
{
    public static final String DEFAULT_CHARSET_NAME = "utf8";
    private final XuguDataSource dataSource;
    private String name;
    private XuguCharset charset;
    private XuguCollation collation;

    public XuguCreateDatabaseDialog(Shell parentShell, XuguDataSource dataSource) {
        super(parentShell, "Create database", null);
        this.dataSource = dataSource;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        final Composite composite = super.createDialogArea(parent);

        final Composite group = new Composite(composite, SWT.NONE);
        group.setLayout(new GridLayout(2, false));

        final Text nameText = UIUtils.createLabelText(group, "Database name", "");
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                name = nameText.getText();
                getButton(IDialogConstants.OK_ID).setEnabled(!name.isEmpty());
            }
        });

        final Combo charsetCombo = UIUtils.createLabelCombo(group, "Charset", SWT.BORDER | SWT.DROP_DOWN);
        for (XuguCharset cs : dataSource.getCharsets()) {
            charsetCombo.add(cs.getName());
        }
        charsetCombo.setText(DEFAULT_CHARSET_NAME);
        charset = dataSource.getCharset(DEFAULT_CHARSET_NAME);
        assert charset != null;

        final Combo collationCombo = UIUtils.createLabelCombo(group, "Collation", SWT.BORDER | SWT.DROP_DOWN);
        for (XuguCollation col : charset.getCollations()) {
            collationCombo.add(col.getName());
        }
        collation = charset.getDefaultCollation();
        if (collation != null) {
            UIUtils.setComboSelection(collationCombo, collation.getName());
        }

        charsetCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                charset = dataSource.getCharset(charsetCombo.getText());
                assert charset != null;

                collationCombo.removeAll();
                for (XuguCollation col : charset.getCollations()) {
                    collationCombo.add(col.getName());
                }
                collation = charset.getDefaultCollation();
                if (collation != null) {
                    UIUtils.setComboSelection(collationCombo, collation.getName());
                }
            }
        });
        collationCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                collation = charset.getCollation(collationCombo.getText());
            }
        });

        return composite;
    }

    public String getName() {
        return name;
    }

    public XuguCharset getCharset() {
        return charset;
    }

    public XuguCollation getCollation() {
        return collation;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
}
