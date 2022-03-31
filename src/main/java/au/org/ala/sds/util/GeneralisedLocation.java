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
package au.org.ala.sds.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;

import au.org.ala.sds.model.ConservationInstance;
import au.org.ala.sds.model.SensitivityInstance;
import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.validation.MessageFactory;

/**
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneralisedLocation {
    private final String originalLatitude;
    private final String originalLongitude;
    private final List<SensitivityZone> zones;
    private final List<SensitivityInstance> instances;
    private final String locationGeneralisation;
    private String generalisedLatitude;
    private String generalisedLongitude;
    private String generalisationInMetres;
    private String generalisationToApplyInMetres;
    private String description;
    private boolean sensitive;

    public GeneralisedLocation(String latitude, String longitude, List<SensitivityInstance> instances, List<SensitivityZone> zones) {
        this.originalLatitude = latitude;
        this.originalLongitude = longitude;
        this.zones = zones;
        this.instances = instances;
        this.locationGeneralisation = getLocationGeneralisation();
        this.sensitive = true;
        generaliseCoordinates();
    }

    public boolean isGeneralised() {
        if (StringUtils.isBlank(originalLatitude) || StringUtils.isBlank(originalLongitude)) {
            return false;
        } else {
            return !originalLatitude.equals(generalisedLatitude) || !originalLongitude.equals(generalisedLongitude);
        }
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public String getOriginalLatitude() {
        return originalLatitude;
    }

    public String getOriginalLongitude() {
        return originalLongitude;
    }

    public String getGeneralisedLatitude() {
        return generalisedLatitude;
    }

    public String getGeneralisedLongitude() {
        return generalisedLongitude;
    }

    public String getGeneralisationInMetres() {
        return generalisationInMetres;
    }

    public String getGeneralisationToApplyInMetres() {
        return generalisationToApplyInMetres;
    }

    public String getDescription() {
        return description;
    }

    public List<SensitivityInstance> getSensitivityInstances() {
        return this.instances;
    }

    private void generaliseCoordinates() {

        generalisationInMetres = "";
        if (this.locationGeneralisation == null) {
            // Not sensitive at given location
            generalisedLatitude = originalLatitude;
            generalisedLongitude = originalLongitude;
            String state = SensitivityZone.getZoneDescriptions(zones);
            description = MessageFactory.getMessageText(MessageFactory.LOCATION_NOT_GENERALISED, state.equalsIgnoreCase("Outside Australia") ? state : "in " + state);
            sensitive = false;
            return;
        }

        if (StringUtils.isBlank(this.originalLatitude) || StringUtils.isBlank(this.originalLongitude)) {
            // location not provided
            generalisedLatitude = originalLatitude;
            generalisedLongitude = originalLongitude;
            description = MessageFactory.getMessageText(MessageFactory.LOCATION_MISSING);
            sensitive = this.locationGeneralisation != null;
            return;
        }

        if (this.locationGeneralisation.equalsIgnoreCase("WITHHOLD")) {
            generalisedLatitude = "";
            generalisedLongitude = "";
            description = MessageFactory.getMessageText(MessageFactory.LOCATION_WITHHELD);
        } else if (this.locationGeneralisation.equalsIgnoreCase("100km") || this.locationGeneralisation.equalsIgnoreCase("50km")) {
            generaliseCoordinates(1);
            if (this.locationGeneralisation.equalsIgnoreCase("50km")){
                generalisationToApplyInMetres = "50000";
            } else {
                generalisationToApplyInMetres = "100000";
            }
            if (isGeneralised()) {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_GENERALISED, SensitivityZone.getZoneDescriptions(zones), "1.0");
                generalisationInMetres = generalisationToApplyInMetres;
            } else {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_ALREADY_GENERALISED, SensitivityZone.getZoneDescriptions(zones), "1.0");
            }
        } else if (this.locationGeneralisation.equalsIgnoreCase("10km")) {
            generaliseCoordinates(1);
            generalisationToApplyInMetres = "10000";
            if (isGeneralised()) {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_GENERALISED, SensitivityZone.getZoneDescriptions(zones), "0.1");
                generalisationInMetres = generalisationToApplyInMetres;
            } else {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_ALREADY_GENERALISED, SensitivityZone.getZoneDescriptions(zones), "0.1");
            }
        } else if (this.locationGeneralisation.equalsIgnoreCase("1km") || this.locationGeneralisation.equalsIgnoreCase("2km")) {
            generaliseCoordinates(2);
            if (this.locationGeneralisation.equalsIgnoreCase("2km")){
                generalisationToApplyInMetres = "2000";
            } else {
                generalisationToApplyInMetres = "1000";
            }

            if (isGeneralised()) {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_GENERALISED, SensitivityZone.getZoneDescriptions(zones), "0.01");
                generalisationInMetres = generalisationToApplyInMetres;
            } else {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_ALREADY_GENERALISED, SensitivityZone.getZoneDescriptions(zones), "0.01");
            }
        } else if (this.locationGeneralisation.equalsIgnoreCase("100m")) {
            generaliseCoordinates(3);
            generalisationToApplyInMetres = "100";
            if (isGeneralised()) {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_GENERALISED, SensitivityZone.getZoneDescriptions(zones), "0.001");
                generalisationInMetres = generalisationToApplyInMetres;
            } else {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_ALREADY_GENERALISED, SensitivityZone.getZoneDescriptions(zones), "0.001");
            }
        } else {
            generalisedLatitude = originalLatitude;
            generalisedLongitude = originalLongitude;
            description = "Location not generalised the severity of generalisation is not specified or is unrecognised.";
            sensitive = false;
        }
    }

    public boolean coordinatesWithheld(){
        return getGeneralisationInMetres().equals("") && getGeneralisedLatitude() != null && getGeneralisedLatitude().equals("");
    }

    private void generaliseCoordinates(int decimalPlaces) {
        generalisedLatitude = round(originalLatitude, decimalPlaces);
        generalisedLongitude = round(originalLongitude, decimalPlaces);
    }

    private String round(String number, int decimalPlaces) {
        if (number == null || number.equals("")) {
            return "";
        } else {
            BigDecimal bd = new BigDecimal(number);
            if (bd.scale() > decimalPlaces) {
                return String.format(Locale.ROOT,"%." + decimalPlaces + "f", bd);
            } else {
                return number;
            }
        }
    }

    private String getLocationGeneralisation() {
        String generalisation = null;

        //this is where zones are matched to sensitive zone...
        for (SensitivityInstance si : instances) {
            if (si instanceof ConservationInstance) {
                if (zones.contains(si.getZone()) || (si.getZone().getId().equals(SensitivityZone.ATLAS_COUNTRY_CODE) && SensitivityZone.isInAtlasCountry(zones))) {
                    generalisation = maxGeneralisation(generalisation, ((ConservationInstance) si).getLocationGeneralisation());
                }
            }
        }
        return generalisation;
    }

    private String maxGeneralisation(String generalisation1, String generalisation2) {
        int gen1 = toInt(generalisation1);
        int gen2 = toInt(generalisation2);

        if (gen1 > gen2) {
            return generalisation1;
        } else {
            return generalisation2;
        }
    }

    private int toInt(String generalisation) {
        if (generalisation == null) {
            return 0;
        } else if (generalisation.equalsIgnoreCase("WITHHOLD")) {
            return Integer.MAX_VALUE;
        } else if (generalisation.equalsIgnoreCase("10km")) {
            return 10000;
        } else if (generalisation.equalsIgnoreCase("1km")) {
            return 1000;
        } else if (generalisation.equalsIgnoreCase("2km")) {
            return 2000;
        } else if (generalisation.equalsIgnoreCase("100m")) {
            return 100;
        } else {
            return 0;
        }
    }

}
