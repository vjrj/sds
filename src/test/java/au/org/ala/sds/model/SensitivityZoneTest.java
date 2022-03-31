/***************************************************************************
 * Copyright (C) 2011 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.sds.model;

import au.org.ala.sds.util.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SensitivityZoneTest {
    @BeforeClass
    public static void runOnce() throws Exception {
        TestUtils.initConfig();
    }

    @Test
    public void isInAustralia() {
        assertTrue(SensitivityZone.isInAtlasCountry(SensitivityZoneFactory.getZone(SensitivityZone.NSW)));
        assertFalse(SensitivityZone.isInAtlasCountry(SensitivityZoneFactory.getZone(SensitivityZone.NOT_ATLAS_COUNTRY_CODE)));
    }
}
