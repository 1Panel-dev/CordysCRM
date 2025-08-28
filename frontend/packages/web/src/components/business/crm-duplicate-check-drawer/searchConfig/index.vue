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
    :init="initSearchDetail"
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
  import { defaultSearchSetFormModel, lastScopedOptions, scopedOptions } from '../config';
  import useSearchFormConfig from '../useSearchFormConfig';

  const emit = defineEmits<{
    (e: 'init', val: Record<string, any>, formModel: DefaultSearchSetFormModel): void;
    (e: 'initConfigList'): void;
  }>();

  const configList = defineModel<ScopedOptions[]>('configList', {
    default: () => scopedOptions,
  });

  const formModel = ref<DefaultSearchSetFormModel>(cloneDeep(defaultSearchSetFormModel));

  const { initSearchFormConfig, searchFieldMap, allFieldMap } = useSearchFormConfig();

  const showAdvancedSettingModal = ref(false);
  function handleAdvancedConfig() {
    showAdvancedSettingModal.value = true;
  }

  const originSearchFields = ref({});
  async function initSearchDetail(isInit = false) {
    try {
      const res = await getSearchConfig();
      const { sortSetting } = res;

      const optionsMap = new Map(scopedOptions.map((item) => [item.value, item]));
      configList.value = sortSetting.map((val: any) => optionsMap.get(val)).filter(Boolean) as ScopedOptions[];

      formModel.value = cloneDeep(res);
      if (isInit) {
        originSearchFields.value = cloneDeep(res.searchFields);
        emit('init', allFieldMap, cloneDeep(res));
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleRefresh() {
    initSearchDetail(true);
  }

  onMounted(async () => {
    await initSearchFormConfig();
    await initSearchDetail(true);
    emit('initConfigList');
  });

  watch(
    () => originSearchFields.value,
    (val) => {
      if (val) {
        nextTick(() => {
          formModel.value.searchFields = val;
        });
      }
    }
  );

  defineExpose({
    searchFieldMap,
    formModel,
  });
</script>

<style scoped></style>
