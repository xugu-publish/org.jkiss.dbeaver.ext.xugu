/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.xugu.tools.maintenance;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.XuguTable;
import org.jkiss.dbeaver.ext.xugu.model.XuguTableIndex;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Collection;
import java.util.List;

/**
 * Gather statistics
 */
public class XuguToolGatherStatistics implements IExternalTool
{
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException
    {
        if (!objects.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), objects);
            dialog.open();
        }
    }

    static class SQLDialog extends XuguMaintenanceDialog<DBSObject> {

        private Spinner samplePercent;

        public SQLDialog(IWorkbenchPartSite partSite, Collection<DBSObject> selectedTables)
        {
            super(partSite, "Gather statistics", selectedTables);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, DBSObject object) {
            if (object instanceof XuguTable) {
                XuguTable table = (XuguTable)object;
                String sql = "BEGIN \n" +
                    " DBMS_STATS.GATHER_TABLE_STATS (\n" +
                    " OWNNAME => '" + DBUtils.getQuotedIdentifier(table.getSchema()) + "',\n" +
                    " TABNAME => '" + DBUtils.getQuotedIdentifier(table) + "',\n" +
                    " estimate_percent => " + samplePercent.getSelection() + "\n" +
                    " );\n" +
                    "END;";
                lines.add(sql);
            } else if (object instanceof XuguTableIndex) {
                XuguTableIndex index = (XuguTableIndex)object;
                String sql = "ALTER INDEX " + index.getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPUTE STATISTICS";
                lines.add(sql);
            }
        }

        @Override
        protected void createControls(Composite parent) {
            Group optionsGroup = UIUtils.createControlGroup(parent, "Options", 1, 0, 0);
            optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            samplePercent = UIUtils.createLabelSpinner(optionsGroup, "Sample Percent", 5, 0, 100);
            samplePercent .addSelectionListener(SQL_CHANGE_LISTENER);

            createObjectsSelector(parent);
        }
    }

}
