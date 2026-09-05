package com.d0w0b.phytotrack.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.d0w0b.phytotrack.dto.AiDtos.AnalyzeRequest;
import com.d0w0b.phytotrack.dto.AiDtos.AnalyzeResponse;
import com.d0w0b.phytotrack.service.AIService;

import java.util.Map;

/**
 * AI 診斷控制器 (AI Controller)
 *
 * 權限：僅診斷員與管理者 (STAFF / ADMIN) 可使用 AI 診斷；
 * 健康檢查為公開端點，供前端在首頁或設定頁顯示模型狀態。
 */
@RestController
@RequestMapping ("/api/ai")
@ConditionalOnProperty (name = "ai.enabled", havingValue = "true", matchIfMissing = true)
public class AIController {

  private final AIService aiService;

  public AIController (AIService aiService) {
    this.aiService = aiService;
  }

  /** AI 診斷 (非串流，一次回傳完整建議) */
  @PostMapping ("/analyze")
  @PreAuthorize ("hasAnyRole ('STAFF', 'ADMIN')")
  public ResponseEntity<AnalyzeResponse> analyze (@Valid @RequestBody AnalyzeRequest request) {
    return ResponseEntity.ok (aiService.analyze (request));
  }

  /** llama.cpp 健康檢查 (公開端點) */
  @GetMapping ("/health")
  public ResponseEntity<Map<String, Boolean>> health () {
    return ResponseEntity.ok (Map.of ("healthy", aiService.isHealthy ()));
  }
}