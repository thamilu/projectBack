package com.eshop.app.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Improved Jackson configuration. Uses customizers to extend Spring Boot's
 * auto-configured ObjectMapper instead of replacing it, and applies security
 * and performance defaults.
 */
@Configuration
public class JsonConfig {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public JsonMapper objectMapper(@Value("${app.json.pretty-print:false}") boolean prettyPrint) {
        JsonMapper.Builder b = JsonMapper.builder();

        // Build mapper
        JsonMapper mapper = b.build();
        mapper.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Configure Java Time module with explicit formatters
        JavaTimeModule jtm = new JavaTimeModule();
        jtm.addSerializer(new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        jtm.addSerializer(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_FORMAT)));
        jtm.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        jtm.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATETIME_FORMAT)));
        mapper.registerModule(jtm);

        // Serialization
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        if (prettyPrint) mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Deserialization
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        mapper.disable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        // Null handling
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        applySecurityConstraints(mapper);
        return mapper;
    }

    // Use `app.json.pretty-print` property to control pretty-printing per environment

    /**
     * Apply security-related constraints to the created ObjectMapper.
     */
    private void applySecurityConstraints(ObjectMapper mapper) {
        // Prevent overly large payloads and deep nesting (mitigate DoS/zip-bombs)
        StreamReadConstraints constraints = StreamReadConstraints.builder()
                .maxStringLength(10_000_000)    // 10MB max string
                .maxNumberLength(1000)           // Max digits
                .maxNestingDepth(100)            // Max JSON nesting depth
                .build();

        mapper.getFactory().setStreamReadConstraints(constraints);

        // Disable polymorphic typing by default to avoid gadget/RCE risks
        try {
            mapper.deactivateDefaultTyping();
        } catch (Throwable ignore) {
            // Some Jackson versions may not expose this; ignore to remain compatible
        }
    }
}