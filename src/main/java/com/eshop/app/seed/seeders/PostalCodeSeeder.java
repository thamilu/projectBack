package com.eshop.app.seed.seeders;

import com.eshop.app.entity.*;
import com.eshop.app.repository.*;
import com.eshop.app.seed.core.BaseSeeder;
import com.eshop.app.seed.core.SeederContext;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eshop.app.util.SlugUtils;
import com.eshop.app.util.TextUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * [HARDEN] Ultra-High-Performance Seeder for the location master hierarchy.
 * Designed for 43,000+ rows with minimal memory footprint and maximum throughput.
 *
 * <p><b>Optimizations:</b>
 * <ul>
 *   <li><b>Two-Pass Seeding</b>: Phase 1 pre-resolves parents; Phase 2 performs pure batch inserts.</li>
 *   <li><b>Projection Caching</b>: Deduplication uses minimal-field projections, not full entities.</li>
 *   <li><b>Manual Session Management</b>: Periodic {@code flush()} and {@code clear()} prevents memory leaks.</li>
 *   <li><b>Topological Batching</b>: Parents are saved once, eliminating thousands of single-row roundtrips.</li>
 * </ul>
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class PostalCodeSeeder extends BaseSeeder<PostalCode, SeederContext> {

    private static final int BATCH_SIZE = 1000;
    private static final String JSON_GZ_PATH = "src/main/resources/seed/postal_codes.json.gz";
    private static final String JSON_PATH    = "src/main/resources/seed/postal_codes.json";

    private final CountryRepository  countryRepository;
    private final StateRepository    stateRepository;
    private final DistrictRepository districtRepository;
    private final TalukRepository    talukRepository;
    private final PostalCodeRepository postalCodeRepository;
    private final EntityManager entityManager;
    private final com.eshop.app.config.properties.SeedProperties seedProperties;

    private final Map<String, Country>  countryCache  = new HashMap<>();
    private final Map<String, State>    stateCache    = new HashMap<>();
    private final Map<String, District> districtCache = new HashMap<>();
    private final Map<String, Taluk>    talukCache    = new HashMap<>();
    private final Set<String>           postalCodeKeyCache = new HashSet<>();

    private static String countryKey(String iso)                { return normalize(iso); }
    private static String stateKey(Long cId, String name)       { return cId + ":" + normalize(name); }
    private static String districtKey(Long sId, String name)    { return sId + ":" + normalize(name); }
    private static String talukKey(Long dId, String name)       { return dId + ":" + normalize(name); }
    private static String postalKey(String pc, String loc, String po) {
        return normalize(pc) + "|" + normalize(loc) + "|" + normalize(po);
    }
    private static String normalize(String s)                   { return s == null ? "" : s.trim().toLowerCase(); }

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
            try (InputStream is = openStream(file)) {
                preResolveHierarchy(is);
            } catch (Exception e) {
                throw new com.eshop.app.seed.exception.SeedingException("Phase 1 failed", e, com.eshop.app.seed.exception.SeedingException.SeedPhase.ORCHESTRATION);
            }

            // Phase 2: Bulk insert PostalCodes
            log.info("Phase 2: Performing bulk postal code insertion...");
            try (InputStream is = openStream(file)) {
                int count = processPostalCodes(is);
                log.info("✅ Phase 2 complete. Processed {} rows for {}", count, file.getName());
            } catch (Exception e) {
                throw new com.eshop.app.seed.exception.SeedingException("Phase 2 failed", e, com.eshop.app.seed.exception.SeedingException.SeedPhase.ORCHESTRATION);
            }
        }

        return List.of(); // Result size tracking handled via metrics/logs
    }

    /**
     * Pass 1: Reads the JSON stream and ensures all parent entities exist in DB and cache.
     * Uses topological sorting (implicit) to minimize DB calls.
     */
    private void preResolveHierarchy(InputStream is) throws IOException {
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper();

        try (JsonParser parser = factory.createParser(is)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) return;

            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                    PostalCodeRow row = mapper.readValue(parser, PostalCodeRow.class);
                    
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

    /**
     * Pass 2: Performs pure batch insertion of PostalCode entities.
     * Since all parents are pre-resolved in Phase 1, this is highly efficient.
     */
    private int processPostalCodes(InputStream is) throws IOException {
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper();
        int count = 0;

        try (JsonParser parser = factory.createParser(is)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) return 0;

            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                    PostalCodeRow row = mapper.readValue(parser, PostalCodeRow.class);
                    
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
            String pk = postalKey((String)row[0], (String)row[1], (String)row[2]);
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

    private InputStream openStream(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        return file.getName().endsWith(".gz") ? new GZIPInputStream(is) : is;
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
                .taluk(taluk)
                .district(district)
                .state(state)
                .country(country)
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
                        .slug(SlugUtils.generateSlug(countryName))
                        .phoneCode(phoneCode != null ? phoneCode : "+91")
                        .isActive(true)
                        .build()
        ));
    }

    private State resolveState(Country country, String stateName, String stateCode) {
        return stateCache.computeIfAbsent(stateKey(country.getId(), stateName), k -> {
            String resolvedCode = (stateCode == null || stateCode.isBlank()) 
                ? com.eshop.app.util.IndianStateMapper.getStateCode(stateName) : stateCode;
            if (resolvedCode == null || resolvedCode.isBlank()) {
                resolvedCode = stateName.length() >= 2 ? stateName.substring(0, 2).toUpperCase() : "XX";
            }
            return stateRepository.save(State.builder()
                        .country(country)
                        .name(TextUtils.toProperCase(stateName))
                        .slug(SlugUtils.generateSlug(stateName))
                        .stateCode(resolvedCode.trim().toUpperCase())
                        .isActive(true)
                        .build());
        });
    }

    private District resolveDistrict(State state, String districtName) {
        return districtCache.computeIfAbsent(districtKey(state.getId(), districtName), k -> 
            districtRepository.save(District.builder()
                        .state(state)
                        .name(TextUtils.toProperCase(districtName))
                        .slug(SlugUtils.generateSlug(districtName))
                        .isActive(true)
                        .build()));
    }

    private Taluk resolveTaluk(District district, String talukName) {
        return talukCache.computeIfAbsent(talukKey(district.getId(), talukName), k -> 
            talukRepository.save(Taluk.builder()
                        .district(district)
                        .name(TextUtils.toProperCase(talukName))
                        .slug(SlugUtils.generateSlug(talukName))
                        .isActive(true)
                        .build()));
    }

    @Override protected void doCleanup() {
        postalCodeRepository.deleteAllInBatch();
        talukRepository.deleteAllInBatch();
        districtRepository.deleteAllInBatch();
        stateRepository.deleteAllInBatch();
        countryRepository.deleteAllInBatch();
    }

    @Override public int order() { return 0; }

    public record PostalCodeRow(
            String isoCode, String countryName, String phoneCode,
            String stateName, String stateCode, String districtName,
            String talukName, String pinCode, String localityName, String postOfficeName
    ) {}
}
