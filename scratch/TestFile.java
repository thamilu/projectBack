package com.eshop.app.scratch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TestFile {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PostalCodeRow(
            String isoCode, String countryName, String phoneCode,
            String stateName, String stateCode, String districtName,
            String talukName, String pinCode, String localityName, String postOfficeName) {
    }

    private static String countryKey(String iso) {
        return normalize(iso);
    }

    private static String stateKey(Long cId, String name) {
        return cId + ":" + normalize(name);
    }

    private static String districtKey(Long sId, String name) {
        return sId + ":" + normalize(name);
    }

    private static String talukKey(Long dId, String name) {
        return dId + ":" + normalize(name);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    public static void main(String[] args) {
        File file = new File("src/main/resources/seed/postal_codes.json");
        System.out.println("File exists: " + file.exists());
        if (!file.exists()) {
            return;
        }

        try (InputStream is = new FileInputStream(file)) {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper();

            long count = 0;
            try (JsonParser parser = factory.createParser(is)) {
                if (parser.nextToken() != JsonToken.START_ARRAY) {
                    System.out.println("Not a start array");
                    return;
                }

                Map<String, Long> countryCache = new HashMap<>();
                Map<String, Long> stateCache = new HashMap<>();
                Map<String, Long> districtCache = new HashMap<>();
                Map<String, Long> talukCache = new HashMap<>();

                long countryIdSeq = 1;
                long stateIdSeq = 1;
                long districtIdSeq = 1;
                long talukIdSeq = 1;

                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                        PostalCodeRow row = mapper.readValue(parser, PostalCodeRow.class);
                        count++;

                        try {
                            // Emulate parent resolution logic
                            String iso = row.isoCode() != null ? row.isoCode() : "IN";
                            String countryName = row.countryName() != null ? row.countryName().trim() : "India";
                            String cKey = countryKey(iso);
                            Long countryId = countryCache.get(cKey);
                            if (countryId == null) {
                                countryId = countryIdSeq++;
                                countryCache.put(cKey, countryId);
                            }

                            String stateName = row.stateName();
                            if (stateName == null) {
                                throw new NullPointerException("stateName is null at row " + count);
                            }
                            String sKey = stateKey(countryId, stateName);
                            Long stateId = stateCache.get(sKey);
                            if (stateId == null) {
                                stateId = stateIdSeq++;
                                stateCache.put(sKey, stateId);
                            }

                            String districtName = row.districtName();
                            if (districtName == null) {
                                throw new NullPointerException("districtName is null at row " + count);
                            }
                            String dKey = districtKey(stateId, districtName);
                            Long districtId = districtCache.get(dKey);
                            if (districtId == null) {
                                districtId = districtIdSeq++;
                                districtCache.put(dKey, districtId);
                            }

                            String talukName = row.talukName();
                            if (talukName != null && !talukName.isBlank()) {
                                String tKey = talukKey(districtId, talukName);
                                Long talukId = talukCache.get(tKey);
                                if (talukId == null) {
                                    talukId = talukIdSeq++;
                                    talukCache.put(tKey, talukId);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error at row " + count + ": " + row);
                            e.printStackTrace();
                            break;
                        }
                    }
                }
            }
            System.out.println("Finished dry run of " + count + " rows successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
