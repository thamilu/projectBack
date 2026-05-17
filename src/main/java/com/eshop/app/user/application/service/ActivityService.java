package com.eshop.app.user.application.service;

import com.eshop.app.admin.api.response.AdminDashboardResponse.ActivityItem;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ActivityService {

    // Placeholder implementation until a real repository exists
    public List<ActivityItem> getRecentActivities(int limit) {
        return List.of(
                ActivityItem.builder().action("System startup").description("Application started").type("system")
                        .timestamp(Instant.now()).relativeTime("just now").build());
    }
}
