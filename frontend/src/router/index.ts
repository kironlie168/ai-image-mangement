import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'gallery', component: () => import('@/views/GalleryView.vue') },
    { path: '/image/:id', name: 'image-detail', component: () => import('@/views/ImageDetailView.vue') },
    { path: '/upload', name: 'upload', component: () => import('@/views/UploadView.vue') },
    { path: '/tags', name: 'tags', component: () => import('@/views/TagManageView.vue') },
  ],
})

export default router
