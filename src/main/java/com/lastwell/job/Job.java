package com.lastwell.job;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record Job(
        String id,
        String code,
        String customer,
        String footwear,
        String leather,
        String issue,
        String cobbler,
        LocalDate dueOn,
        JobStatus status,
        String notes,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
}
