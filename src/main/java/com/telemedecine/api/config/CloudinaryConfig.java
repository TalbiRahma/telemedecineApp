package com.telemedecine.api.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dpjyq2yeq",
                "api_key", "462618265623893",
                "api_secret", "2NaOb4Q3AnNPDt8wMIpTS2eELAc",
                "secure", true
        ));
    }
}