<template>
  <aside class="sidebar">
    <h2>{{ title }}</h2>
    <nav>
      <a v-for="(item, index) in items" :key="item" :class="{ active: index === 0 }">{{ item }}</a>
    </nav>
  </aside>
</template>

<script setup>
defineProps({
  title: String,
  items: Array
});
</script>
