package com.ifsp.users.dtos;

import java.util.List;

public record UserProfileDto(
        Long id,
        String email,
        List<String> roles
) {}
