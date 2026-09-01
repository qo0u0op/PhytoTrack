<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { aiApi } from '../api'

// llama.cpp 模型健康狀態 (後端代理檢查，公開端點)
const modelHealthy = ref<boolean | null>(null)

onMounted (async () => {
  try {
    const { data } = await aiApi.health ()
    modelHealthy.value = data.healthy
  } catch {
    modelHealthy.value = false
  }
})
</script>

<template>
  <!-- Hero 首頁 (Landing Page) -->
  <section class="hero-section py-5">
    <div class="container py-5 text-center text-white">
      <h1 class="display-4 fw-bold">PhytoTrack</h1>
      <p class="lead mb-4">農作物病蟲害診斷諮詢服務系統</p>
      <p class="mb-4">
        協助農友辨識病蟲害、記錄診斷案件，並以本機 AI 模型 (llama.cpp) 提供初步防治建議。
      </p>
      <div class="d-flex justify-content-center gap-2">
        <router-link class="btn btn-light btn-lg" to="/login">立即登入</router-link>
        <router-link class="btn btn-outline-light btn-lg" to="/register">建立帳號</router-link>
      </div>
      <p class="mt-4 mb-0 small">
        模型狀態：
        <span v-if="modelHealthy === null" class="text-warning">檢查中…</span>
        <span v-else-if="modelHealthy" class="badge bg-white text-success">已連線</span>
        <span v-else class="badge bg-warning text-dark">未連線</span>
      </p>
    </div>
  </section>

  <!-- 功能特色 -->
  <div class="container py-5">
    <div class="row g-4">
      <div class="col-md-4">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h5 class="card-title">案件管理</h5>
            <p class="card-text text-muted">
              完整記錄診斷案件：送件人、作物、被害部位、防治建議與診斷簽名人。
            </p>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h5 class="card-title">AI 診斷建議</h5>
            <p class="card-text text-muted">
              輸入症狀描述，由本機運行的 llama.cpp 模型即時產生初步診斷與防治建議。
            </p>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h5 class="card-title">角色權限</h5>
            <p class="card-text text-muted">
              檢視者 (Viewer)、診斷員 (Staff)、管理者 (Admin) 三種角色分層管理。
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>

  <footer class="py-4 bg-dark text-white-50">
    <div class="container text-center small">
      PhytoTrack — Spring Boot 4 + Vue 3 + llama.cpp
    </div>
  </footer>
</template>
