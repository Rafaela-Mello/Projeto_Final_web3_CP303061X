package com.ifsp.users.dtos;

import com.ifsp.users.enums.RoleName;

public record CreateUserDto(
        String email,
        String password,
        RoleName role
) {}
