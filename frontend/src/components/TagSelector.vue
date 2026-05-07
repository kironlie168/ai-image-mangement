<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useTagStore } from '@/stores/tagStore'
import { storeToRefs } from 'pinia'

const tagStore = useTagStore()
const { tags, loading } = storeToRefs(tagStore)

const props = defineProps<{
  modelValue: number[]
  multiple?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number[]]
}>()

const selectedValues = computed({
  get: () => [...props.modelValue],
  set: (val: number | number[]) => {
    emit('update:modelValue', Array.isArray(val) ? val : [val])
  }
})

onMounted(() => {
  if (!tags.value.length) tagStore.fetchTags()
})
</script>

<template>
  <el-select
    v-model="selectedValues"
    :multiple="multiple ?? true"
    filterable
    clearable
    placeholder="Select tags"
    :loading="loading"
    style="width: 100%"
  >
    <el-option
      v-for="tag in tags"
      :key="tag.id"
      :label="tag.name"
      :value="tag.id"
    >
      <el-tag :color="tag.color" size="small" effect="dark">{{ tag.name }}</el-tag>
    </el-option>
    <template #empty>
      <el-empty :image-size="40" description="No tags available" />
    </template>
  </el-select>
</template>
