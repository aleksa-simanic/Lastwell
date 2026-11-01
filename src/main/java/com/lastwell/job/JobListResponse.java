package com.lastwell.job;

import java.util.List;

public record JobListResponse(
        List<Job> items,
        int total,
        int limit,
        int offset
) {
}
