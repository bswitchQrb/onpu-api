package com.onpu.controller;

import com.onpu.service.QuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping("/weighted")
    public ResponseEntity<Map<String, String>> getWeightedQuestion(
            Authentication auth,
            @RequestParam String mode,
            @RequestBody List<String> allQuestions) {
        Long userId = (Long) auth.getPrincipal();
        String question = questionService.getWeightedQuestion(userId, mode, allQuestions);
        return ResponseEntity.ok(Map.of("question", question));
    }
}
