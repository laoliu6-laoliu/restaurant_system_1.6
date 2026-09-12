<template>
  <div class="query-pagination" aria-label="查询分页">
    <button class="btn-cancel" :disabled="query.loading || query.page === 0" @click="query.turn(query.page - 1)">上一页</button>
    <span>第 {{ query.totalPages ? query.page + 1 : 0 }} / {{ query.totalPages }} 页</span>
    <button class="btn-cancel" :disabled="query.loading || query.page + 1 >= query.totalPages" @click="query.turn(query.page + 1)">下一页</button>
  </div>
</template>
<script setup>
defineProps({ query: { type: Object, required: true } })
</script>
