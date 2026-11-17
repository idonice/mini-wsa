package com.idonice.mini_wsa.dto.ingest;

import java.util.List;

public record IngestResult(
        int received,
        int ingested,
        int duplicates,
        List<EventError> rejected,   
        List<EventError> failed    
) {}
