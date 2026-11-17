package com.idonice.mini_wsa.controller;

import com.idonice.mini_wsa.dto.ingest.IngestResult;
import com.idonice.mini_wsa.dto.samples.SamplesResult;
import com.idonice.mini_wsa.model.Action;
import com.idonice.mini_wsa.model.Category;
import com.idonice.mini_wsa.service.EventSamplesService;
import com.idonice.mini_wsa.service.IngestService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1/events")
public class EventController {

    static final int MAX_BATCH_SIZE = 1000;
    static final int MAX_SAMPLES_LIMIT = 100;

    private final IngestService ingestService;
    private final EventSamplesService samplesService;

    public EventController(IngestService ingestService, EventSamplesService samplesService) {
        this.ingestService = ingestService;
        this.samplesService = samplesService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResult> ingest(
            @RequestBody
            @NotEmpty(message = "A request must contain at least 1 event")
            @Size(max = MAX_BATCH_SIZE, message = "A request must contain at most " + MAX_BATCH_SIZE + " events")
            List<JsonNode> items) {

        IngestResult result = ingestService.ingest(items);
        return ResponseEntity.status(statusFor(result)).body(result);
    }

    @GetMapping("/samples")
    public SamplesResult samples(
            @RequestParam(required = false) Long configId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Action action,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "limit must be at least 1")
            @Max(value = MAX_SAMPLES_LIMIT, message = "limit must be at most " + MAX_SAMPLES_LIMIT)
            int limit,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "offset must not be negative")
            int offset) {

        if (from != null && to != null && !from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be before 'to'");
        }
        return samplesService.getSamples(configId, from, to, category, action, limit, offset);
    }

    // 201 if anything was accepted, else 400 (all invalid) or 500 (storing failed)
    private HttpStatus statusFor(IngestResult result) {
        boolean anythingAccepted = result.ingested() > 0 || result.duplicates() > 0;
        if (anythingAccepted) {
            return HttpStatus.CREATED;
        }
        if (result.failed().isEmpty()) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
