package com.rojas.syncpass.backend.web;

import com.rojas.syncpass.backend.dto.ApiDtos.SyncBatchRequest;
import com.rojas.syncpass.backend.dto.ApiDtos.SyncBatchResponse;
import com.rojas.syncpass.backend.dto.ApiDtos.SyncPullDto;
import com.rojas.syncpass.backend.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/batch")
    public SyncBatchResponse batch(@Valid @RequestBody SyncBatchRequest request) {
        return syncService.batch(request);
    }

    @GetMapping("/pull")
    public SyncPullDto pull() {
        return syncService.pull();
    }
}
