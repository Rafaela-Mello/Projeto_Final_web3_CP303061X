package com.ifsp.users.dtos;

import java.util.UUID;

public record EmailDto(
        String emailTo,
        String subject,
        String text,
        UUID userId
) { }
