package org.jkiss.dbeaver.ext.xugu;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class XuguPreferencesInitializer extends AbstractPreferenceInitializer {

    public XuguPreferencesInitializer()
    {
    }

    @Override
    public void initializeDefaultPreferences()
    {
        // Init default preferences
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
    }

} 