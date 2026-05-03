package com.eshop.app.config.serializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;


/**
 * Lightweight Redis serializer using Jackson but avoiding deprecated Spring serializers.
 */
public class JacksonRedisSerializer implements RedisSerializer<Object> {

    private final ObjectMapper mapper;

    public JacksonRedisSerializer() {
        this(createDefaultMapper());
    }

    public JacksonRedisSerializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    private static ObjectMapper createDefaultMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return om;
    }

    @Override
    public byte[] serialize(Object t) throws SerializationException {
        if (t == null) return new byte[0];
        try {
            return mapper.writeValueAsBytes(t);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializing object", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return mapper.readValue(bytes, Object.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing bytes", e);
        }
    }
}
