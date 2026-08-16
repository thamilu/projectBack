package com.eshop.app.user.infrastructure.util;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NonceGenerator {
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
