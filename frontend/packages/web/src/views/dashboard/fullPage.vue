<template>
  <div class="h-full">
    <dashboard
      :title="route.query.title as string"
      :dashboard-id="route.query.id as string"
      :is-favorite="route.query.isFavorite === 'Y'"
      is-full-page
    />
  </div>
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';

  import dashboard from './components/dashboard.vue';

  const route = useRoute();
</script>

<style lang="less" scoped></style>
