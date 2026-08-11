package com.telemedecine.api.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class StringToLocalDateConverter implements Converter<String, LocalDate> {

    @Override
    public LocalDate convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        // Trim any whitespace or newline characters
        String trimmedDate = source.trim();
        return LocalDate.parse(trimmedDate);
    }
}