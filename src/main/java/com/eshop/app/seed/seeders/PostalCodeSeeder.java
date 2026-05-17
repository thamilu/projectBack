package com.eshop.app.seed.seeders;

import com.eshop.app.location.domain.entity.*;
import com.eshop.app.location.domain.repository.*;
import com.eshop.app.seed.core.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eshop.app.core.util.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * [HARDEN] High-Performance, Low-Memory Seeder for the location master hierarchy.
 * Processes 900,000+ records via topological pre-resolution and batched insertion.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class PostalCodeSeeder extends BaseSeeder<PostalCode, SeederContext> {

    private static final int BATCH_SIZE = 1000;
    private static final String JSON_GZ_PATH = "src/main/resources/seed/postal_codes.json.gz";
    private static final String JSON_PATH = "src/main/resources/seed/postal_codes.json";
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final TalukRepository talukRepository;
    private final PostalCodeRepository postalCodeRepository;
    private final EntityManager entityManager;
    private final com.eshop.app.core.infrastructure.config.properties.SeedProperties seedProperties;
    private final Map<String, Country> countryCache = new HashMap<>();
    private final Map<String, State> stateCache = new HashMap<>();
    private final Map<String, District> districtCache = new HashMap<>();
    private final Map<String, Taluk> talukCache = new HashMap<>();
    private final Set<String> postalCodeKeyCache = new HashSet<>();

    private static String countryKey(String iso) { return normalize(iso); }
    private static String stateKey(Long cId, String name) { return cId + ":" + normalize(name); }
    private static String districtKey(Long sId, String name) { return sId + ":" + normalize(name); }
    private static String talukKey(Long dId, String name) { return dId + ":" + normalize(name); }
    private static String postalKey(String pc, String loc, String po) {
        return normalize(pc) + "|" + normalize(loc) + "|" + normalize(po);
    }
    private static String normalize(String s) { return s == null ? "" : s.trim().toLowerCase(); }

    @Override
    protected List<PostalCode> doSeed(SeederContext context) {
        if (!seedProperties.isLocationsEnabled()) {
            log.info("PostalCodeSeeder: location seeding disabled.");
            return List.of();
        }
        preloadCaches();
        List<File> sourceFiles = resolveSourceFiles();
        if (sourceFiles.isEmpty()) {
            log.warn("No valid JSON sources found. Skipping high-perf seeding.");
            return List.of();
        }
        for (File file : sourceFiles) {
            log.info("🚀 Starting high-perf seeding for: {}", file.getName());

            // Phase 1: Pre-resolve parents (Country/State/District/Taluk)
            log.info("Phase 1: Pre-resolving location hierarchy...");
            try (Reader reader = openReader(file)) {
                preResolveHierarchy(reader);
            } catch (Exception e) {
                throw new com.eshop.app.seed.exception.SeedingException("Phase 1 failed", e,
                        com.eshop.app.seed.exception.SeedingException.SeedPhase.ORCHESTRATION);
            }

            // Phase 2: Bulk insert PostalCodes
            log.info("Phase 2: Performing bulk postal code insertion...");
            try (Reader reader = openReader(file)) {
                int count = processPostalCodes(reader);
                log.info("✅ Phase 2 complete. Processed {} rows for {}", count, file.getName());
            } catch (Exception e) {
                throw new com.eshop.app.seed.exception.SeedingException("Phase 2 failed", e,
                        com.eshop.app.seed.exception.SeedingException.SeedPhase.ORCHESTRATION);
            }
        }
        return List.of();
    }

    private void preResolveHierarchy(Reader reader) throws IOException {
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper();
        try (JsonParser parser = factory.createParser(reader)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) return;
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                    PostalCodeRow row = mapper.readValue(parser, PostalCodeRow.class);
                    if (row.stateName() == null || row.stateName().isBlank() ||
                        row.districtName() == null || row.districtName().isBlank() ||
                        row.pinCode() == null || row.pinCode().isBlank()) {
                        continue;
                    }
                    Country country = resolveCountry(row.isoCode(), row.countryName(), row.phoneCode());
                    State state = resolveState(country, row.stateName(), row.stateCode());
                    District district = resolveDistrict(state, row.districtName());
                    if (row.talukName() != null && !row.talukName().isBlank()) {
                        resolveTaluk(district, row.talukName());
                    }
                }
            }
        }
    }

    private int processPostalCodes(Reader reader) throws IOException {
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper();
        int count = 0;
        try (JsonParser parser = factory.createParser(reader)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) return 0;
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                    PostalCodeRow row = mapper.readValue(parser, PostalCodeRow.class);
                    if (row.stateName() == null || row.stateName().isBlank() ||
                        row.districtName() == null || row.districtName().isBlank() ||
                        row.pinCode() == null || row.pinCode().isBlank()) {
                        continue;
                    }
                    String key = postalKey(row.pinCode(), row.localityName(), row.postOfficeName());
                    if (!postalCodeKeyCache.contains(key)) {
                        PostalCode pc = buildPostalCode(row);
                        if (pc != null) {
                            entityManager.persist(pc);
                            postalCodeKeyCache.add(key);
                            count++;
                            if (count % BATCH_SIZE == 0) {
                                entityManager.flush();
                                entityManager.clear();
                                log.debug("Flushed {} postal codes...", count);
                            }
                        }
                    }
                }
            }
            entityManager.flush();
            entityManager.clear();
        }
        return count;
    }

    private void preloadCaches() {
        log.info("Preloading hierarchy caches...");
        countryRepository.findAll().forEach(c -> countryCache.put(countryKey(c.getIsoCode()), c));
        stateRepository.findAll().forEach(s -> stateCache.put(stateKey(s.getCountry().getId(), s.getName()), s));
        districtRepository.findAll().forEach(d -> districtCache.put(districtKey(d.getState().getId(), d.getName()), d));
        talukRepository.findAll().forEach(t -> talukCache.put(talukKey(t.getDistrict().getId(), t.getName()), t));
        log.info("Preloading postal code keys (Optimized Projection)...");
        postalCodeRepository.findAllKeys().forEach(row -> {
            String pk = postalKey((String) row[0], (String) row[1], (String) row[2]);
            postalCodeKeyCache.add(pk);
        });
        log.info("Caches ready. Hierarchy: {}, PostalCode Keys: {}",
                countryCache.size() + stateCache.size() + districtCache.size() + talukCache.size(),
                postalCodeKeyCache.size());
    }

    private List<File> resolveSourceFiles() {
        List<File> files = new ArrayList<>();
        File gzFile = new File(JSON_GZ_PATH);
        if (gzFile.exists()) files.add(gzFile);
        File seedDir = new File("src/main/resources/seed");
        File[] parts = seedDir.listFiles((dir, name) -> name.startsWith("postal_codes_part") && name.endsWith(".json"));
        if (parts != null) {
            Arrays.sort(parts, Comparator.comparing(File::getName));
            files.addAll(Arrays.asList(parts));
        }
        File jsonFile = new File(JSON_PATH);
        if (jsonFile.exists() && files.isEmpty()) files.add(jsonFile);
        return files;
    }

    private Reader openReader(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        if (file.getName().endsWith(".gz")) is = new GZIPInputStream(is);
        return new RobustJsonReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    private PostalCode buildPostalCode(PostalCodeRow row) {
        Country country = countryCache.get(countryKey(row.isoCode() != null ? row.isoCode() : "IN"));
        State state = stateCache.get(stateKey(country.getId(), row.stateName()));
        District district = districtCache.get(districtKey(state.getId(), row.districtName()));
        Taluk taluk = (row.talukName() != null && !row.talukName().isBlank())
                ? talukCache.get(talukKey(district.getId(), row.talukName())) : null;
        return PostalCode.builder()
                .pinCode(row.pinCode())
                .localityName(TextUtils.toProperCase(row.localityName()))
                .postOfficeName(TextUtils.toProperCase(row.postOfficeName()))
                .taluk(taluk).district(district).state(state).country(country)
                .isActive(true)
                .build();
    }

    private Country resolveCountry(String isoCode, String name, String phoneCode) {
        String iso = isoCode != null ? isoCode : "IN";
        String countryName = name != null ? name.trim() : "India";
        return countryCache.computeIfAbsent(countryKey(iso), k -> countryRepository.save(
                Country.builder()
                        .isoCode(iso.trim().toUpperCase())
                        .name(TextUtils.toProperCase(countryName))
                        .slug(generateSafeSlug(countryName, iso))
                        .phoneCode(phoneCode != null ? phoneCode : "+91")
                        .isActive(true)
                        .build()));
    }

    private State resolveState(Country country, String stateName, String stateCode) {
        return stateCache.computeIfAbsent(stateKey(country.getId(), stateName), k -> {
            String resolvedCode = (stateCode == null || stateCode.isBlank())
                    ? com.eshop.app.location.shared.util.IndianStateMapper.getStateCode(stateName)
                    : stateCode;
            if (resolvedCode == null || resolvedCode.isBlank() || "XX".equalsIgnoreCase(resolvedCode)) {
                String base = stateName == null ? "ST" : stateName.replaceAll("[^a-zA-Z]", "");
                if (base.length() > 5) base = base.substring(0, 5);
                if (base.isBlank()) base = "ST";
                base = base.toUpperCase();
                resolvedCode = base;
                int suffix = 1;
                while (isStateCodeUsed(country.getId(), resolvedCode)) {
                    String sfxStr = String.valueOf(suffix);
                    if (base.length() + sfxStr.length() > 10) {
                        resolvedCode = base.substring(0, 10 - sfxStr.length()) + sfxStr;
                    } else {
                        resolvedCode = base + sfxStr;
                    }
                    suffix++;
                }
            }
            return stateRepository.save(State.builder()
                    .country(country)
                    .name(TextUtils.toProperCase(stateName))
                    .slug(generateSafeSlug(stateName, resolvedCode))
                    .stateCode(resolvedCode.trim().toUpperCase())
                    .isActive(true)
                    .build());
        });
    }

    private boolean isStateCodeUsed(Long countryId, String code) {
        String normalizedCode = code.trim().toUpperCase();
        return stateCache.values().stream().anyMatch(s ->
                s.getCountry().getId().equals(countryId) && s.getStateCode().equals(normalizedCode)
        );
    }

    private District resolveDistrict(State state, String districtName) {
        return districtCache.computeIfAbsent(districtKey(state.getId(), districtName),
                k -> districtRepository.save(District.builder()
                        .state(state)
                        .name(TextUtils.toProperCase(districtName))
                        .slug(generateSafeSlug(districtName, "district"))
                        .isActive(true)
                        .build()));
    }

    private Taluk resolveTaluk(District district, String talukName) {
        return talukCache.computeIfAbsent(talukKey(district.getId(), talukName),
                k -> talukRepository.save(Taluk.builder()
                        .district(district)
                        .name(TextUtils.toProperCase(talukName))
                        .slug(generateSafeSlug(talukName, "taluk"))
                        .isActive(true)
                        .build()));
    }

    private String generateSafeSlug(String input, String fallback) {
        String slug = SlugUtils.generateSlug(input);
        if (slug == null || slug.isBlank()) slug = SlugUtils.generateSlug(fallback);
        return (slug == null || slug.isBlank()) ? "safe-slug-" + UUID.randomUUID().toString().substring(0, 8) : slug;
    }

    @Override
    protected void doCleanup() {
        postalCodeRepository.deleteAllInBatch();
        talukRepository.deleteAllInBatch();
        districtRepository.deleteAllInBatch();
        stateRepository.deleteAllInBatch();
        countryRepository.deleteAllInBatch();
    }

    @Override
    public int order() { return 0; }
}
