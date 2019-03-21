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

package org.jkiss.dbeaver.ext.xugu.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchedulerJob;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

/**
 * Oracle Scheduler Job Action editor
 */
public class SchedulerJobActionEditor extends SQLSourceViewer<XuguSchedulerJob> {

    @Override
    protected String getCompileCommandId()
    {
        return XuguConstants.CMD_COMPILE;
    }

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
    	XuguSchedulerJob schedulerJob = getSourceObject();
    	return schedulerJob.getAction().toString();
    }

    @Override
    protected void setSourceText(DBRProgressMonitor monitor, String sourceText) {
        getInputPropertySource().setPropertyValue(
            monitor,
            XuguConstants.PROP_OBJECT_BODY_DEFINITION,
            sourceText);
    }

    @Override
    protected boolean isReadOnly() {
        return true;
    }
}
