<template>
  <div class="flex gap-[8px] p-[16px]">
    <n-button class="n-btn-outline-primary" type="primary" ghost @click="configDrawerVisible = true"
      >创建自定义表单</n-button
    >
    <n-button class="n-btn-outline-primary" type="primary" ghost @click="handleEdit">编辑自定义表单</n-button>
    <CustomFormConfigDrawer v-model:visible="configDrawerVisible" :source-id="currentSourceId" />
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton } from 'naive-ui';

  import CustomFormConfigDrawer from './components/customFormConfigDrawer/index.vue';

  const configDrawerVisible = ref(false);
  const currentSourceId = ref();

  function handleEdit() {
    currentSourceId.value = 'mock-custom-form-id';
    configDrawerVisible.value = true;
  }
</script>

<style scoped></style>
