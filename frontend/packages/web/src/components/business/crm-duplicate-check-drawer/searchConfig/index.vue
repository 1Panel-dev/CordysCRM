<template>
  <n-button
    v-if="lastScopedOptions.length > 0"
    class="n-btn-outline-primary !px-[8px]"
    type="primary"
    ghost
    @click="handleAdvancedConfig"
  >
    <CrmIcon type="iconicon_set_up" class="cursor-pointer text-[var(--primary-8)]" :size="16" />
  </n-button>
  <searchSettingModal
    v-model:config-list="configList"
    v-model:visible="showAdvancedSettingModal"
    :search-field-map="searchFieldMap"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton } from 'naive-ui';

  import searchSettingModal from './searchSettingModal.vue';

  import type { ScopedOptions } from '../config';
  import { lastScopedOptions } from '../config';
  import useSearchFormConfig from '../useSearchFormConfig';

  const emit = defineEmits<{
    (e: 'init', val: Record<string, any>): void;
  }>();

  const configList = defineModel<ScopedOptions[]>('configList', {
    default: () => lastScopedOptions.value,
  });

  const { initSearchFormConfig, searchFieldMap } = useSearchFormConfig();

  const showAdvancedSettingModal = ref(false);
  function handleAdvancedConfig() {
    showAdvancedSettingModal.value = true;
  }

  onMounted(async () => {
    await initSearchFormConfig();
    configList.value = lastScopedOptions.value;
    emit('init', searchFieldMap);
  });

  defineExpose({
    searchFieldMap,
  });
</script>

<style scoped></style>
