package com.lastwell.job;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Job create(@Valid @RequestBody CreateJobRequest request) {
        return jobService.create(request);
    }

    @GetMapping
    public JobListResponse list(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String footwear,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return jobService.list(status, footwear, tag, q, limit, offset);
    }

    @GetMapping("/{id}")
    public Job get(@PathVariable String id) {
        return jobService.get(id);
    }

    @PutMapping("/{id}")
    public Job update(@PathVariable String id, @Valid @RequestBody UpdateJobRequest request) {
        return jobService.update(id, request);
    }

    @PostMapping("/{id}/last")
    public Job startLasting(@PathVariable String id) {
        return jobService.startLasting(id);
    }

    @PostMapping("/{id}/stitch")
    public Job startStitching(@PathVariable String id) {
        return jobService.startStitching(id);
    }

    @PostMapping("/{id}/finish")
    public Job startFinishing(@PathVariable String id) {
        return jobService.startFinishing(id);
    }

    @PostMapping("/{id}/ready")
    public Job markReady(@PathVariable String id) {
        return jobService.markReady(id);
    }

    @PostMapping("/{id}/pickup")
    public Job markPickedUp(@PathVariable String id) {
        return jobService.markPickedUp(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        jobService.delete(id);
    }
}
