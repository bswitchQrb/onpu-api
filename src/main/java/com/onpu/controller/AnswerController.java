package com.onpu.controller;

import com.onpu.dto.AnswerLogResponse;
import com.onpu.dto.AnswerRequest;
import com.onpu.dto.StatsResponse;
import com.onpu.dto.WeakPointResponse;
import com.onpu.service.AnswerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnswerController {

    private final AnswerService answerService;

    public AnswerController(AnswerService answerService) {
        this.answerService = answerService;
    }

    @PostMapping("/answers")
    public ResponseEntity<Map<String, String>> recordAnswer(
            Authentication auth, @Valid @RequestBody AnswerRequest request) {
        Long userId = (Long) auth.getPrincipal();
        answerService.recordAnswer(userId, request);
        return ResponseEntity.ok(Map.of("message", "記録しました"));
    }

    @GetMapping("/answers/history")
    public ResponseEntity<List<AnswerLogResponse>> getHistory(
            Authentication auth,
            @RequestParam(defaultValue = "50") int limit) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(answerService.getHistory(userId, limit));
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(answerService.getStats(userId));
    }

    @GetMapping("/stats/weak-points")
    public ResponseEntity<List<WeakPointResponse>> getWeakPoints(
            Authentication auth,
            @RequestParam String mode,
            @RequestParam(defaultValue = "5") int limit) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(answerService.getWeakPoints(userId, mode, limit));
    }
}
