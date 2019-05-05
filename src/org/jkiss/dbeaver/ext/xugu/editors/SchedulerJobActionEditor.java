
package org.jkiss.dbeaver.ext.xugu.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.model.XuguSchedulerJob;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

/**
 * Xugu Scheduler Job Action editor
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
