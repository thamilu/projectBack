package com.eshop.app.seed.provider;

import com.eshop.app.seed.model.TagData;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile({"dev", "test", "local"})
public class CodeBasedTagDataProvider implements TagDataProvider {

    @Override
    public List<TagData> getTags() {
        return List.of(
            TagData.of("new"),
            TagData.of("sale"),
            TagData.of("popular"),
            TagData.of("trending"),
            TagData.of("limited-edition"),
            TagData.of("exclusive"),
            TagData.of("best-seller"),
            TagData.of("featured")
        );
    }
}
