<script setup lang="ts">
import AppNavbar from '@/components/AppNavbar.vue'
import AppSidebar from '@/components/AppSidebar.vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const showSidebar = () => route.name === 'gallery'
</script>

<template>
  <div class="app-layout">
    <AppNavbar />
    <div class="app-body">
      <aside v-if="showSidebar()" class="app-sidebar">
        <AppSidebar />
      </aside>
      <main class="app-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; }

.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}

.app-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.app-sidebar {
  width: 260px;
  border-right: 1px solid #e4e7ed;
  background: #fff;
  overflow-y: auto;
  flex-shrink: 0;
}

.app-main {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
</style>
