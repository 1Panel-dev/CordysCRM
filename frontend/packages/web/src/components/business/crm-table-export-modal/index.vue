<template>
  <CrmModal
    v-model:show="show"
    size="small"
    :title="t('common.export')"
    :ok-loading="loading"
    :positive-text="t('common.export')"
    :ok-button-props="{
      disabled: !form.fileName.trim().length,
    }"
    @confirm="confirmHandler"
    @cancel="closeHandler"
  >
    <n-form
      ref="formRef"
      :model="form"
      :rules="{
        fileName: [
          {
            required: true,
            message: t('common.nameNotNull'),
          },
        ],
      }"
      label-placement="left"
      label-width="auto"
      require-mark-placement="left"
      class="hidden-feedback min-w-[350px]"
    >
      <n-form-item path="fileName" :label="t('common.name')" required>
        <n-input-group>
          <n-input v-model:value="form.fileName" type="text" :maxlength="50" :placeholder="t('common.pleaseInput')" />
          <n-input-group-label>.xlsx</n-input-group-label>
        </n-input-group>
        <div class="mt-[2px] text-[12px] text-[var(--text-n4)]"> {{ t('common.exportTaskTip') }} </div>
      </n-form-item>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { FormInst, NForm, NFormItem, NInput, NInputGroup, NInputGroupLabel, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ExportTableColumnItem } from '@lib/shared/models/common';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import {
    exportClueAll,
    exportCluePoolAll,
    exportCluePoolSelected,
    exportClueSelected,
    exportCustomerAll,
    exportCustomerOpenSeaAll,
    exportCustomerOpenSeaSelected,
    exportCustomerSelected,
    exportOpportunityAll,
    exportOpportunitySelected,
  } from '@/api/modules';

  const props = defineProps<{
    params: Record<string, any>;
    type: 'customer' | 'clue' | 'opportunity' | 'cluePool' | 'openSea';
    exportColumns: ExportTableColumnItem[];
    isExportAll?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'createSuccess'): void;
  }>();

  const { t } = useI18n();
  const message = useMessage();

  const show = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const typeStringMap = {
    customer: t('menu.customer'),
    clue: t('menu.clue'),
    opportunity: t('menu.opportunity'),
    cluePool: t('module.cluePool'),
    openSea: t('module.openSea'),
  };

  const loading = ref<boolean>(false);
  const formRef = ref<FormInst>();
  const form = ref<Record<string, any>>({
    fileName: '',
  });

  watch(
    () => show.value,
    (newVal) => {
      if (newVal) {
        form.value.fileName = `${dayjs().format('YYYYMMDD-HHmmss')}-${typeStringMap[props.type]}`;
      }
    }
  );

  function closeHandler() {
    formRef.value?.restoreValidation();
  }

  const exportAllApiMap = {
    customer: exportCustomerAll,
    clue: exportClueAll,
    opportunity: exportOpportunityAll,
    cluePool: exportCluePoolAll,
    openSea: exportCustomerOpenSeaAll,
  };

  const exportSelectedApiMap = {
    customer: exportCustomerSelected,
    clue: exportClueSelected,
    opportunity: exportOpportunitySelected,
    cluePool: exportCluePoolSelected,
    openSea: exportCustomerOpenSeaSelected,
  };

  function confirmHandler() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          const exportApi = props.isExportAll ? exportAllApiMap[props.type] : exportSelectedApiMap[props.type];
          await exportApi({
            ...props.params,
            ids: props.params.ids || [],
            fileName: form.value.fileName.trim(),
            headList: props.exportColumns,
          });
          form.value.fileName = '';
          show.value = false;
          message.success(t('common.exportTaskCreate'));
          emit('createSuccess');
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          loading.value = false;
        }
      }
    });
  }
</script>

<style lang="less">
  .hidden-feedback {
    .n-form-item-feedback-wrapper {
      display: none;
    }
  }
</style>
