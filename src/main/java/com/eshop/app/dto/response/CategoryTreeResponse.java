package com.eshop.app.dto.response;

import java.util.ArrayList;
import java.util.List;

public class CategoryTreeResponse {
    private Long id;
    private String name;
    private List<CategoryTreeResponse> children = new ArrayList<>();

    public CategoryTreeResponse() {}

    public CategoryTreeResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CategoryTreeResponse> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryTreeResponse> children) {
        this.children = children;
    }
}
