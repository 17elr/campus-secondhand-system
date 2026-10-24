package com.campus.notice;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record Notification(
    @NotNull @Positive Long recipientId,
    @NotBlank @Size(max = 40) String type,
    @NotNull @Positive Long businessId,
    @NotBlank @Size(max = 120) String title,
    @NotBlank @Size(max = 1000) String body,
    @NotNull Instant occurredAt) {}
