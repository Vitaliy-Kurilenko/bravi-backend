package ua.com.bravi.bravi.seller.tags.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;
import ua.com.bravi.bravi.seller.tags.domain.TagsMatch;

/**
 * Binds the tag values a URL carries, so it reads {@code /tags/products?tags_match=all} rather than
 * shouting its enum constants. An unknown value fails conversion before the method runs, which is
 * what keeps the target of the method security expression from ever being absent.
 */
@Configuration
public class TagsWebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToTagTargetConverter());
        registry.addConverter(new StringToTagsMatchConverter());
    }

    static class StringToTagTargetConverter implements Converter<String, TagTarget> {

        @Override
        public TagTarget convert(String source) {
            return TagTarget.fromPath(source);
        }
    }

    static class StringToTagsMatchConverter implements Converter<String, TagsMatch> {

        @Override
        public TagsMatch convert(String source) {
            return TagsMatch.fromParam(source);
        }
    }
}
