<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
  marginBottom?: number
}>()
</script>

<template>
  <div class="page-header" :style="marginBottom != null ? { marginBottom: `${marginBottom}px` } : undefined">
    <div class="header-text">
      <h1 class="page-title">{{ title }}</h1>
      <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
    </div>
    <div class="header-accent" />
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 24px;
}
.header-text {
  margin-bottom: 14px;
}
.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.02em;
  line-height: 1.2;
  margin-bottom: 6px;
}
.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}
.header-accent {
  height: 3px;
  width: 72px;
  border-radius: 4px;
  background: linear-gradient(135deg, #6366f1, #a855f7);
}
</style>
