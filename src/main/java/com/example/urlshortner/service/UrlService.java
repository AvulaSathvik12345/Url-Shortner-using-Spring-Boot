package com.example.urlshortner.service;

import com.example.urlshortner.model.UrlMapping;
import com.example.urlshortner.repository.UrlMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

@Service
public class UrlService {
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Autowired
    private UrlMappingRepository repo;
    @Autowired private RedisTemplate<String, String> redis;

    public String shorten(String originalUrl, String alias) {
        String code = (alias != null && !alias.isBlank()) ? alias : generateCode(originalUrl);
        repo.findByShortCode(code).orElseGet(() -> {
            UrlMapping m = new UrlMapping();
            m.setShortCode(code);
            m.setOriginalUrl(originalUrl);
            repo.save(m);
            redis.opsForValue().set("url:" + code, originalUrl, 24, TimeUnit.HOURS);
            return m;
        });
        return code;
    }

    public String resolve(String code) {
        // Redis first
        String cached = redis.opsForValue().get("url:" + code);
        if (cached != null) return cached;

        // DB fallback
        return repo.findByShortCode(code)
                .map(m -> {
                    redis.opsForValue().set("url:" + code, m.getOriginalUrl(), 24, TimeUnit.HOURS);
                    return m.getOriginalUrl();
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String generateCode(String url) {
        long hash = Math.abs((long) url.hashCode());
        StringBuilder sb = new StringBuilder();
        while (hash > 0) {
            sb.append(CHARS.charAt((int)(hash % 62)));
            hash /= 62;
        }
        return sb.reverse().toString().substring(0, Math.min(7, sb.length()));
    }
}