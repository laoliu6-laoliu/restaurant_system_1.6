<template>
  <section class="panel-card">
    <header>
      <div>
        <h3>{{ title }}</h3>
        <p>{{ subtitle }}</p>
      </div>
    </header>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th v-for="[, label] in columns" :key="label">{{ label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in rows" :key="index">
            <td v-for="[key] in columns" :key="key">{{ row[key] }}</td>
          </tr>
          <tr v-if="rows.length === 0">
            <td :colspan="columns.length">暂无数据，请先导入 resjk_restaurant.sql 或新增数据。</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: String,
  subtitle: String,
  columns: Array,
  rows: Array
});
</script>
