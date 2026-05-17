package com.eshop.app.core.web.controller;

import com.eshop.app.core.kernel.ApiConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Parsing Utilities", description = "Data parsing and transformation utilities")
@RestController
@RequestMapping(ApiConstants.Endpoints.PARSE)
public class ParseController {

    @GetMapping("/parse")
    public ResponseEntity<List<String>> parse(@RequestParam("values") String values) {
        if (values == null || values.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<String> parts = Arrays.stream(values.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        return ResponseEntity.ok(parts);
    }
}
