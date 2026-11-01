package com.lastwell.job;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record UpdateJobRequest(
        @Size(max = 20) String code,
        @Size(max = 120) String customer,
        @Size(max = 40) String footwear,
        @Size(max = 80) String leather,
        @Size(max = 200) String issue,
        @Size(max = 120) String cobbler,
        LocalDate dueOn,
        JobStatus status,
        @Size(max = 2000) String notes,
        @Size(max = 20) List<@Size(max = 40) String> tags
) {
}
