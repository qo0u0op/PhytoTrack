package com.d0w0b.phytotrack.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.d0w0b.phytotrack.exception.ApiException;

/**
 * AI 關閉時的占位控制器，避免 /api/ai/* 回 404「資源不存在」
 * 而改為明確的 AI_DISABLED
 */
@RestController
@RequestMapping ("/api/ai")
@ConditionalOnProperty (name = "ai.enabled", havingValue = "false")
public class AiDisabledController {

  @RequestMapping ("**")
  public ResponseEntity<Void> disabled () {
    throw new ApiException ("AI_DISABLED", HttpStatus.NOT_FOUND, "AI 功能已關閉");
  }
}
