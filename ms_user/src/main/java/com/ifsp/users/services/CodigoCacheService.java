package com.ifsp.users.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CodigoCacheService {

    private final Cache<String, String> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public void salvarCodigo(String email, String codigo) {
        cache.put(email, codigo);
    }

    public String obterCodigoValido(String email) {
        return cache.getIfPresent(email);
    }

    public void removerCodigo(String email) {
        cache.invalidate(email);
    }
}
