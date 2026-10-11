package com.jp.elearningjp.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${CLOUD_NAME}")
    private String cloudName;

    @Value("${CLOUD_API_KEY}")
    private String apiKey;

    @Value("${CLOUD_API_SECRET}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                // Mặc định sử dụng HTTPS cho mọi URL trả về
                "secure", true
        );
        return new Cloudinary(config);
    }
}
