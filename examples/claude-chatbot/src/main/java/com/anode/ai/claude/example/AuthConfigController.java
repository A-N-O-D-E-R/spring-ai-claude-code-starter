package com.anode.ai.claude.example;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Config API: lets the user provide the Claude token at runtime. The token is write-only. */
@RestController
@RequestMapping("/api/config/token")
class AuthConfigController {

    record TokenRequest(String token) {}

    private final CredentialStore store;

    AuthConfigController(CredentialStore store) {
        this.store = store;
    }

    @GetMapping
    Map<String, Object> status() {
        Map<String, Object> body = new HashMap<>();
        body.put("configured", store.isSet());
        body.put("type", store.kind());
        body.put("hint", store.hint());
        return body;
    }

    @PutMapping
    Map<String, Object> set(@RequestBody TokenRequest request) {
        if (request == null || request.token() == null || request.token().isBlank())
            throw new IllegalArgumentException("token is required");
        store.set(request.token());
        return status();
    }

    @DeleteMapping
    ResponseEntity<Void> clear() {
        store.clear();
        return ResponseEntity.noContent().build();
    }
}
