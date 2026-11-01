package com.lastwell.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateJobRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 120) String customer,
        @NotBlank @Size(max = 40) String footwear,
        @NotBlank @Size(max = 80) String leather,
        @NotBlank @Size(max = 200) String issue,
        @NotBlank @Size(max = 120) String cobbler,
        @NotNull LocalDate dueOn,
        JobStatus status,
        @Size(max = 2000) String notes,
        @Size(max = 20) List<@Size(max = 40) String> tags
) {
}
