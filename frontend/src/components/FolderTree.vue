<script setup lang="ts">
import { onMounted } from 'vue'
import { useFolderStore } from '@/stores/folderStore'
import { storeToRefs } from 'pinia'

const folderStore = useFolderStore()
const { folderTree, loading } = storeToRefs(folderStore)

const props = defineProps<{
  modelValue: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
}>()

onMounted(() => {
  if (!folderTree.value.length) folderStore.fetchTree()
})

function onSelect(data: any) {
  emit('update:modelValue', data?.id ?? null)
}
</script>

<template>
  <el-tree
    :data="folderTree"
    :props="{ label: 'name', children: 'children' }"
    node-key="id"
    :highlight-current="true"
    :expand-on-click-node="true"
    :filter-node-method="(value: string, data: any) => data.name.includes(value)"
    @node-click="onSelect"
    :loading="loading"
  />
</template>
