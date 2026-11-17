package com.idonice.mini_wsa.controller;

import com.idonice.mini_wsa.dto.ingest.IngestResult;
import com.idonice.mini_wsa.service.IngestService;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.List;

@RestController
@RequestMapping("/v1/events")
public class EventController {

    static final int MAX_BATCH_SIZE = 1000;

    private final IngestService ingestService;

    public EventController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResult> ingest(
            @RequestBody @NotEmpty @Size(max = MAX_BATCH_SIZE) List<JsonNode> items) {

        IngestResult result = ingestService.ingest(items);
        return ResponseEntity.status(statusFor(result)).body(result);
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
