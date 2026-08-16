package com.eshop.app.user.infrastructure.util;

import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SystemClock {
    public Instant now() {
        return Instant.now();
    }
}
