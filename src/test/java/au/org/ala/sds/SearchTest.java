/***************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
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
package au.org.ala.sds;

import au.org.ala.names.search.ALANameSearcher;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.Configuration;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SearchTest {

    static ALANameSearcher nameSearcher;
    static SensitiveSpeciesFinder finder;

    @BeforeClass
    public static void runOnce() throws Exception {
        System.setProperty("sds.config.file", "/sds-test.properties");
        nameSearcher = new ALANameSearcher(Configuration.getInstance().getNameMatchingIndex());
        String uri = nameSearcher.getClass().getClassLoader().getResource("sensitive-species.xml").toURI().toString();
        finder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder(uri, nameSearcher, true);
    }

    @Test
    public void lookupRufus() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Macropus rufus");
        assertNull(ss);
    }

    @Test
    public void lookupCrex() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Crex crex");
        assertNotNull(ss);
    }

    @Test
    public void lookupMitchellsByLsid() {
        SensitiveTaxon ss = finder.findSensitiveSpeciesByLsid("urn:lsid:biodiversity.org.au:afd.taxon:0217f06f-664c-4c64-bc59-1b54650fa23d");
        assertNotNull(ss);
        assertEquals("Lophochroa leadbeateri", ss.getTaxonName());
    }

    @Test
    public void lookupMitchells() {
        SensitiveTaxon ss = finder.findSensitiveSpecies("Cacatua leadbeateri");
        assertNotNull(ss);
        assertEquals("Lophochroa leadbeateri", ss.getTaxonName());
    }
}
