package com.ifsp.users.dtos;

import com.ifsp.users.enums.RoleName;

public record UpdateProfileDto(
        String name,
        RoleName role
) {}
