package com.lastwell.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobServiceTest {

    private JobService service;

    @BeforeEach
    void setUp() {
        service = new JobService();
        service.clear();
    }

    @Test
    void createAndGetJob() {
        Job created = service.create(new CreateJobRequest(
                "lw-210",
                "Sam Ortega",
                "pump",
                "patent",
                "Broken heel stem",
                "Jordan Lee",
                LocalDate.of(2026, 9, 1),
                JobStatus.INTAKE,
                "Keep spare lifts.",
                List.of("Dress", "dress", "rush")
        ));

        Job fetched = service.get(created.id());
        assertEquals("LW-210", fetched.code());
        assertEquals(JobStatus.INTAKE, fetched.status());
        assertEquals("pump", fetched.footwear());
        assertEquals(List.of("dress", "rush"), fetched.tags());
    }

    @Test
    void listFiltersByFootwearAndTag() {
        service.create(new CreateJobRequest(
                "LW-A",
                "Client A",
                "oxford",
                "calf",
                "Loose welt",
                "Amina Cole",
                LocalDate.of(2026, 8, 10),
                JobStatus.STITCHING,
                null,
                List.of("dress")
        ));
        service.create(new CreateJobRequest(
                "LW-B",
                "Client B",
                "boot",
                "nubuck",
                "Resole",
                "Jordan Lee",
                LocalDate.of(2026, 8, 11),
                JobStatus.INTAKE,
                null,
                List.of("trail")
        ));

        JobListResponse oxfords = service.list(null, "oxford", "dress", null, 50, 0);
        assertEquals(1, oxfords.total());
        assertEquals("LW-A", oxfords.items().getFirst().code());
    }

    @Test
    void lastStitchFinishReadyPickup() {
        Job intake = service.create(new CreateJobRequest(
                "LW-C",
                "Amina Cole",
                "sneaker",
                "calf",
                "Worn outsole",
                "Jordan Lee",
                LocalDate.of(2026, 8, 15),
                JobStatus.INTAKE,
                null,
                List.of("sample")
        ));

        assertEquals(JobStatus.LASTING, service.startLasting(intake.id()).status());
        assertEquals(JobStatus.STITCHING, service.startStitching(intake.id()).status());
        assertEquals(JobStatus.FINISHING, service.startFinishing(intake.id()).status());
        assertEquals(JobStatus.READY, service.markReady(intake.id()).status());
        assertEquals(JobStatus.PICKED_UP, service.markPickedUp(intake.id()).status());
    }

    @Test
    void stitchRejectsIntake() {
        Job intake = service.create(new CreateJobRequest(
                "LW-D",
                "Temp",
                "boot",
                "nubuck",
                "Split rand",
                "Amina Cole",
                LocalDate.of(2026, 8, 16),
                JobStatus.INTAKE,
                null,
                List.of()
        ));

        assertThrows(com.lastwell.error.BadRequestException.class,
                () -> service.startStitching(intake.id()));
    }

    @Test
    void deleteRemovesJob() {
        Job created = service.create(new CreateJobRequest(
                "LW-E",
                "Temp",
                "oxford",
                "calf",
                "Heel lift",
                "Amina Cole",
                LocalDate.of(2026, 8, 1),
                JobStatus.INTAKE,
                null,
                List.of()
        ));
        service.delete(created.id());
        assertThrows(com.lastwell.error.NotFoundException.class, () -> service.get(created.id()));
    }

    @Test
    void searchMatchesLeather() {
        service.create(new CreateJobRequest(
                "LW-F",
                "Harbor Club",
                "loafer",
                "suede",
                "Stretch vamp",
                "Jordan Lee",
                LocalDate.of(2026, 8, 8),
                JobStatus.INTAKE,
                null,
                List.of("club")
        ));

        JobListResponse result = service.list(null, null, null, "suede", 10, 0);
        assertEquals(1, result.total());
        assertTrue(result.items().getFirst().leather().toLowerCase().contains("suede"));
    }
}
