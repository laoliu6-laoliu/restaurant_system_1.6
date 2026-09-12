<template>
  <div class="dish-image" :class="{ 'dish-image-small': small }">
    <img v-if="source && !failed" :src="source" :alt="dish.mname" loading="lazy" @error="failed = true" />
    <span v-else class="dish-image-fallback">{{ dish.mname }}<small>图片暂未提供</small></span>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
const props = defineProps({ dish: { type: Object, required: true }, small: Boolean })
const failed = ref(false)
const source = computed(() => {
  const url = props.dish.imageUrl || ''
  return /^(\/images\/|https?:\/\/)/i.test(url) ? url : ''
})
watch(source, () => { failed.value = false })
</script>
