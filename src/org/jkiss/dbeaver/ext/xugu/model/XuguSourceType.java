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
 * Stored code interface
 */
public enum XuguSourceType {

    TYPE(false),
    PROCEDURE(false),
    FUNCTION(false),
    PACKAGE(false),
    TRIGGER(false),
    TABLESPACE(false),
    VIEW(true),
    MATERIALIZED_VIEW(true);

    private final boolean isCustom;

    XuguSourceType(boolean custom)
    {
        isCustom = custom;
    }

    public boolean isCustom()
    {
        return isCustom;
    }
}
