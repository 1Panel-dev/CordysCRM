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
    v-model:form-model="formModel"
    v-model:visible="showAdvancedSettingModal"
    :search-field-map="searchFieldMap"
    @refresh="handleRefresh"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import searchSettingModal from './searchSettingModal.vue';

  import { getSearchConfig } from '@/api/modules';

  import type { DefaultSearchSetFormModel, ScopedOptions } from '../config';
  import { defaultSearchSetFormModel, lastScopedOptions } from '../config';
  import useSearchFormConfig from '../useSearchFormConfig';

  const emit = defineEmits<{
    (e: 'init', val: Record<string, any>, formModel: DefaultSearchSetFormModel): void;
  }>();

  const configList = defineModel<ScopedOptions[]>('configList', {
    default: () => lastScopedOptions.value,
  });

  const formModel = ref<DefaultSearchSetFormModel>(cloneDeep(defaultSearchSetFormModel));

  const { initSearchFormConfig, searchFieldMap } = useSearchFormConfig();

  const showAdvancedSettingModal = ref(false);
  function handleAdvancedConfig() {
    showAdvancedSettingModal.value = true;
  }

  async function initSearchDetail() {
    try {
      const res = await getSearchConfig();
      const { sortSetting } = res;
      configList.value = sortSetting.map((value: any) => {
        const currentConfig = lastScopedOptions.value.find((scoped) => value === scoped.value);
        return {
          ...currentConfig,
        };
      });
      formModel.value = cloneDeep(res);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleRefresh() {
    initSearchDetail();
  }

  onMounted(async () => {
    await initSearchFormConfig();
    await initSearchDetail();
    emit('init', searchFieldMap, formModel.value);
  });

  defineExpose({
    searchFieldMap,
    formModel,
  });
</script>

<style scoped></style>
