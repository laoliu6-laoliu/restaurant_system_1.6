<template>
  <form class="date-query-bar" @submit.prevent="query.search()">
    <label>开始日期<input type="date" v-model="query.startDate" required /></label>
    <label>结束日期<input type="date" v-model="query.endDate" required /></label>
    <button type="submit" :disabled="query.loading">{{ query.loading ? '查询中…' : '查询' }}</button>
    <button type="button" class="btn-cancel" :disabled="query.loading" @click="query.today()">今天</button>
    <label>每页条数<select v-model.number="query.size" :disabled="query.loading" @change="query.resize()"><option :value="10">10 条</option><option :value="20">20 条</option><option :value="50">50 条</option></select></label>
  </form>
  <p v-if="query.error" class="error" role="alert">{{ query.error }}</p>
  <p v-else class="query-description">{{ query.appliedStart === query.appliedEnd ? query.appliedStart : `${query.appliedStart} 至 ${query.appliedEnd}` }} · {{ query.loading ? '正在加载…' : `共 ${query.totalElements} 条记录` }}</p>
</template>
<script setup>
defineProps({ query: { type: Object, required: true } })
</script>
