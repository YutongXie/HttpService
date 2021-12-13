package com.zheqiushui.httpservice.util;

import com.sun.org.apache.bcel.internal.generic.RETURN;
import com.zheqiushui.httpservice.model.Constants;

import java.time.ZoneId;
import java.util.*;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/13 22:22
 */
public class RegionUtil {

    public static String getBestRegion(String preferredRegion, Set<String> avaialbeRegion) {
        switch (preferredRegion) {
            case Constants.CLIENT_LOCATED_REGION_US_LATM:
                if (avaialbeRegion.contains(Constants.CLIENT_LOCATED_REGION_EMEA)) {
                    return Constants.CLIENT_LOCATED_REGION_EMEA;
                } else if (avaialbeRegion.contains(Constants.CLIENT_LOCATED_REGION_APAC)) {
                    return Constants.CLIENT_LOCATED_REGION_APAC;
                } else return null;
            case Constants.CLIENT_LOCATED_REGION_EMEA:
                if (avaialbeRegion.contains(Constants.CLIENT_LOCATED_REGION_US_LATM)) {
                    return Constants.CLIENT_LOCATED_REGION_US_LATM;
                } else if (avaialbeRegion.contains(Constants.CLIENT_LOCATED_REGION_APAC)) {
                    return Constants.CLIENT_LOCATED_REGION_APAC;
                } else return null;
            case Constants.CLIENT_LOCATED_REGION_APAC:
                if (avaialbeRegion.contains(Constants.CLIENT_LOCATED_REGION_EMEA)) {
                    return Constants.CLIENT_LOCATED_REGION_EMEA;
                } else if (avaialbeRegion.contains(Constants.CLIENT_LOCATED_REGION_US_LATM)) {
                    return Constants.CLIENT_LOCATED_REGION_US_LATM;
                } else return null;
            default:
                return null;
        }
    }

    public static String determineClientRegion() {
        String location = ZoneId.systemDefault().getId();
        String clientTimeZone = location.contains("/") ? location.substring(0, location.indexOf("/")) : location;

        HashMap<String, List<String>> regionMap = buildCustomizeRegionMapping();

        if (regionMap.get(Constants.CLIENT_LOCATED_REGION_US_LATM).contains(clientTimeZone)) {
            return Constants.CLIENT_LOCATED_REGION_US_LATM;
        }

        if (regionMap.get(Constants.CLIENT_LOCATED_REGION_APAC).contains(clientTimeZone)) {
            return Constants.CLIENT_LOCATED_REGION_APAC;
        }

        if (regionMap.get(Constants.CLIENT_LOCATED_REGION_EMEA).contains(clientTimeZone)) {
            return Constants.CLIENT_LOCATED_REGION_EMEA;
        }

        return Constants.CLIENT_LOCATED_REGION_APAC;
    }

    public static HashMap<String, List<String>> buildCustomizeRegionMapping() {
        Set<String> avaiableRegionSet = extractAvailableRegions();
        HashMap<String, List<String>> regionMap = new HashMap<>();
        regionMap.put(Constants.CLIENT_LOCATED_REGION_APAC, new ArrayList<>());
        regionMap.put(Constants.CLIENT_LOCATED_REGION_EMEA, new ArrayList<>());
        regionMap.put(Constants.CLIENT_LOCATED_REGION_US_LATM, new ArrayList<>());

        avaiableRegionSet.forEach(region -> {
            switch (region) {
                case "Canada":
                    regionMap.get(Constants.CLIENT_LOCATED_REGION_US_LATM).add(region);
                    break;

                case "Beijing":
                    regionMap.get(Constants.CLIENT_LOCATED_REGION_APAC).add(region);
                    break;
                case "Iceland":
                    regionMap.get(Constants.CLIENT_LOCATED_REGION_EMEA).add(region);
                    break;
                default:
                    regionMap.get(Constants.CLIENT_LOCATED_REGION_APAC).add(region);
                    break;

            }
        });
        return regionMap;
    }

    public static Set<String> extractAvailableRegions() {
        TimeZone.setDefault(null);
        System.setProperty("user.timezone", "");
        Set<String> regions = new HashSet<>();
        ZoneId.getAvailableZoneIds().forEach(region -> {
            String r;
            if (region.contains("/")) {
                r = region.substring(0, region.indexOf("/"));
            } else {
                r = region;
            }
            regions.add(r);
        });
        return regions;
    }
}
