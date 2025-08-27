<template>
  <CrmModal
    v-model:show="visible"
    :title="t('module.desensitizationSet')"
    :positive-text="t('common.save')"
    :negative-text="t('common.reset')"
    :width="816"
  >
    <searchSetting
      ref="searchSettingRef"
      v-model:form-model="formModel"
      is-not-required-validate
      :scoped-options="scopedOptions"
      :search-field-map="searchFieldMap"
    />
    <template #footer>
      <div class="flex items-center justify-end gap-[12px]">
        <n-button :disabled="loading" secondary @click="handleReset">
          {{ t('common.reset') }}
        </n-button>
        <n-button :loading="loading" type="primary" @click="handleConfirm">
          {{ t('common.save') }}
        </n-button>
      </div>
    </template>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import {
    DefaultSearchSetFormModel,
    defaultSearchSetFormModel,
    scopedOptions,
  } from '@/components/business/crm-duplicate-check-drawer/config';
  import searchSetting from '@/components/business/crm-duplicate-check-drawer/searchConfig/searchSetting.vue';
  import useSearchFormConfig from '@/components/business/crm-duplicate-check-drawer/useSearchFormConfig';

  import { getModuleSearchMaskConfig, moduleSearchMaskConfig } from '@/api/modules';

  const Message = useMessage();

  const { t } = useI18n();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const formModel = ref<DefaultSearchSetFormModel>({
    ...cloneDeep(defaultSearchSetFormModel),
  });

  const loading = ref(false);
  const searchSettingRef = ref<InstanceType<typeof searchSetting>>();

  function handleConfirm() {
    searchSettingRef.value?.formRef?.validate(async (errors) => {
      if (!errors) {
        loading.value = true;
        try {
          await moduleSearchMaskConfig({
            searchFields: formModel.value.searchFields,
          });
          Message.success(t('common.saveSuccess'));
          visible.value = false;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        } finally {
          loading.value = false;
        }
      }
    });
  }

  function handleReset() {
    searchSettingRef.value?.formRef?.restoreValidation();
    formModel.value = cloneDeep(defaultSearchSetFormModel);
  }

  const { initSearchFormConfig, searchFieldMap } = useSearchFormConfig();

  const originMaskSearchFields = ref({});
  async function initModuleSearchMaskConfig() {
    try {
      const res = await getModuleSearchMaskConfig();
      originMaskSearchFields.value = cloneDeep(res.searchFields);
      formModel.value.searchFields = cloneDeep(res.searchFields);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => originMaskSearchFields.value,
    (val) => {
      if (val) {
        nextTick(() => {
          formModel.value.searchFields = val;
        });
      }
    }
  );

  watch(
    () => visible.value,
    async (val) => {
      if (val) {
        await initModuleSearchMaskConfig();
        initSearchFormConfig();
      }
    }
  );
</script>

<style scoped></style>
