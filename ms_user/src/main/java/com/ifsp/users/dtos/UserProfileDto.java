package com.ifsp.users.dtos;

import java.util.List;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String email,
        List<String> roles
) {}
