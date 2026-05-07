<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <el-menu
    mode="horizontal"
    :ellipsis="false"
    :default-active="route.path"
    router
    style="padding: 0 20px"
  >
    <el-menu-item index="/" style="font-weight: bold; font-size: 18px; margin-right: 30px">
      AI Image Manager
    </el-menu-item>
    <el-menu-item index="/">Gallery</el-menu-item>
    <el-menu-item index="/upload">Upload</el-menu-item>
    <el-menu-item index="/tags">Tags</el-menu-item>
  </el-menu>
</template>
