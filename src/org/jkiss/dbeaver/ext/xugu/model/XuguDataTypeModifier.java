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

/**
* Data type modifier
*/
public enum XuguDataTypeModifier {
    REF,
    POINTER;

    public static XuguDataTypeModifier resolveTypeModifier(String typeMod)
    {
        if (typeMod == null || typeMod.length() == 0) {
            return null;
        } else if ("REF".equals(typeMod)) {
            return REF;
        } else {
            return POINTER;
        }
    }
}
