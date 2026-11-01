package com.lastwell.job;

import com.lastwell.error.BadRequestException;
import com.lastwell.error.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobService {

    private static final Set<String> FOOTWEAR = Set.of(
            "oxford", "boot", "loafer", "pump", "sneaker"
    );

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();

    public JobService() {
        seed();
    }

    public Job create(CreateJobRequest request) {
        String footwear = normalizeFootwear(request.footwear());
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        ensureUniqueCode(code, null);

        Instant now = Instant.now();
        Job job = new Job(
                UUID.randomUUID().toString(),
                code,
                request.customer().trim(),
                footwear,
                request.leather().trim(),
                request.issue().trim(),
                request.cobbler().trim(),
                request.dueOn(),
                request.status() == null ? JobStatus.INTAKE : request.status(),
                normalizeNotes(request.notes()),
                normalizeTags(request.tags()),
                now,
                now
        );
        jobs.put(job.id(), job);
        return job;
    }

    public Job get(String id) {
        return Optional.ofNullable(jobs.get(id))
                .orElseThrow(() -> new NotFoundException("Job " + id + " not found"));
    }

    public JobListResponse list(
            JobStatus status,
            String footwear,
            String tag,
            String q,
            int limit,
            int offset
    ) {
        int safeLimit = Math.clamp(limit, 1, 100);
        int safeOffset = Math.max(offset, 0);

        String footwearFilter = footwear == null || footwear.isBlank()
                ? null
                : footwear.trim().toLowerCase(Locale.ROOT);

        List<Job> filtered = jobs.values().stream()
                .filter(j -> status == null || j.status() == status)
                .filter(j -> footwearFilter == null || j.footwear().equals(footwearFilter))
                .filter(j -> tag == null || tag.isBlank()
                        || j.tags().contains(tag.trim().toLowerCase(Locale.ROOT)))
                .filter(j -> q == null || q.isBlank() || matchesQuery(j, q.trim()))
                .sorted(Comparator.comparing(Job::dueOn)
                        .thenComparing(Job::code, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<Job> page = filtered.stream()
                .skip(safeOffset)
                .limit(safeLimit)
                .toList();

        return new JobListResponse(page, filtered.size(), safeLimit, safeOffset);
    }

    public Job update(String id, UpdateJobRequest request) {
        Job existing = get(id);

        if (request.code() != null && request.code().isBlank()) {
            throw new BadRequestException("code must not be blank");
        }
        if (request.customer() != null && request.customer().isBlank()) {
            throw new BadRequestException("customer must not be blank");
        }
        if (request.footwear() != null && request.footwear().isBlank()) {
            throw new BadRequestException("footwear must not be blank");
        }
        if (request.leather() != null && request.leather().isBlank()) {
            throw new BadRequestException("leather must not be blank");
        }
        if (request.issue() != null && request.issue().isBlank()) {
            throw new BadRequestException("issue must not be blank");
        }
        if (request.cobbler() != null && request.cobbler().isBlank()) {
            throw new BadRequestException("cobbler must not be blank");
        }

        String code = request.code() != null
                ? request.code().trim().toUpperCase(Locale.ROOT)
                : existing.code();
        if (!code.equals(existing.code())) {
            ensureUniqueCode(code, id);
        }

        Job updated = new Job(
                existing.id(),
                code,
                request.customer() != null ? request.customer().trim() : existing.customer(),
                request.footwear() != null
                        ? normalizeFootwear(request.footwear())
                        : existing.footwear(),
                request.leather() != null ? request.leather().trim() : existing.leather(),
                request.issue() != null ? request.issue().trim() : existing.issue(),
                request.cobbler() != null ? request.cobbler().trim() : existing.cobbler(),
                request.dueOn() != null ? request.dueOn() : existing.dueOn(),
                request.status() != null ? request.status() : existing.status(),
                request.notes() != null ? normalizeNotes(request.notes()) : existing.notes(),
                request.tags() != null ? normalizeTags(request.tags()) : existing.tags(),
                existing.createdAt(),
                Instant.now()
        );
        jobs.put(id, updated);
        return updated;
    }

    public Job startLasting(String id) {
        return transition(id, JobStatus.INTAKE, JobStatus.LASTING,
                "Only intake jobs can move to lasting");
    }

    public Job startStitching(String id) {
        return transition(id, JobStatus.LASTING, JobStatus.STITCHING,
                "Last the pair before stitching");
    }

    public Job startFinishing(String id) {
        return transition(id, JobStatus.STITCHING, JobStatus.FINISHING,
                "Stitch before finishing");
    }

    public Job markReady(String id) {
        return transition(id, JobStatus.FINISHING, JobStatus.READY,
                "Finish before marking ready");
    }

    public Job markPickedUp(String id) {
        return transition(id, JobStatus.READY, JobStatus.PICKED_UP,
                "Only ready jobs can be picked up");
    }

    public void delete(String id) {
        if (jobs.remove(id) == null) {
            throw new NotFoundException("Job " + id + " not found");
        }
    }

    void clear() {
        jobs.clear();
    }

    private Job transition(String id, JobStatus from, JobStatus to, String message) {
        Job existing = get(id);
        if (existing.status() != from) {
            throw new BadRequestException(message);
        }
        return update(id, new UpdateJobRequest(
                null, null, null, null, null, null, null,
                to,
                null, null
        ));
    }

    private String normalizeFootwear(String footwear) {
        String value = footwear.trim().toLowerCase(Locale.ROOT);
        if (!FOOTWEAR.contains(value)) {
            throw new BadRequestException(
                    "footwear must be one of: oxford, boot, loafer, pump, sneaker");
        }
        return value;
    }

    private void ensureUniqueCode(String code, String exceptId) {
        boolean duplicate = jobs.values().stream()
                .anyMatch(j -> j.code().equals(code) && (exceptId == null || !j.id().equals(exceptId)));
        if (duplicate) {
            throw new BadRequestException("Job code '" + code + "' already exists");
        }
    }

    private boolean matchesQuery(Job job, String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        return job.code().toLowerCase(Locale.ROOT).contains(needle)
                || job.customer().toLowerCase(Locale.ROOT).contains(needle)
                || job.footwear().toLowerCase(Locale.ROOT).contains(needle)
                || job.leather().toLowerCase(Locale.ROOT).contains(needle)
                || job.issue().toLowerCase(Locale.ROOT).contains(needle)
                || job.cobbler().toLowerCase(Locale.ROOT).contains(needle)
                || job.notes().toLowerCase(Locale.ROOT).contains(needle);
    }

    private String normalizeNotes(String notes) {
        return notes == null ? "" : notes.trim();
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String value = tag.trim().toLowerCase(Locale.ROOT);
            if (!value.isBlank()) {
                cleaned.add(value);
            }
        }
        return List.copyOf(new ArrayList<>(cleaned));
    }

    private void seed() {
        create(new CreateJobRequest(
                "LW-101",
                "Nora Ellis",
                "oxford",
                "calf",
                "Worn heel and loose welt",
                "Amina Cole",
                LocalDate.of(2026, 8, 22),
                JobStatus.STITCHING,
                "Match existing heel lift height.",
                List.of("dress", "rush")
        ));
        create(new CreateJobRequest(
                "LW-102",
                "Harbor Trail Club",
                "boot",
                "nubuck",
                "Resole with vibram and restitch rand",
                "Jordan Lee",
                LocalDate.of(2026, 8, 28),
                JobStatus.INTAKE,
                "Keep original laces in the bag.",
                List.of("trail")
        ));
        create(new CreateJobRequest(
                "LW-103",
                "Sam Ortega",
                "loafer",
                "suede",
                "Stretch vamp and recraft penny strap",
                "Amina Cole",
                LocalDate.of(2026, 8, 18),
                JobStatus.READY,
                "Finished with suede brush; ready for pickup.",
                List.of("suede")
        ));
    }
}
