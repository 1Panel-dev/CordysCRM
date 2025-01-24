<template>
  <CrmModal
    v-model:show="showModal"
    size="medium"
    :title="t('org.formExcelImport')"
    :ok-loading="props.confirmLoading"
    :positive-text="t('org.validateTemplate')"
    :ok-button-props="{ disabled: fileList.length < 1 }"
    @cancel="cancel"
    @confirm="validate"
  >
    <div>
      <n-alert type="default" class="mb-[16px]">
        <template #icon>
          <CrmIcon type="iconicon_info_circle_filled" :size="20" />
        </template>
        <div class="flex items-center gap-[16px]">
          {{ t('org.importAlertDesc') }}
          <div class="flex cursor-pointer items-center gap-[8px]" @click="downLoadTemplate">
            <CrmIcon type="iconicon_file-excel_colorful1" :size="16" />
            <div class="text-[var(--primary-8)]">{{ t('org.downloadTemplate') }}</div>
          </div>
        </div>
      </n-alert>
      <CrmUpload
        v-model:file-list="fileList"
        :is-all-screen="true"
        accept="excel"
        :max-size="100"
        size-unit="MB"
        :disabled="validateLoading"
      />
    </div>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NAlert } from 'naive-ui';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmUpload from '@/components/pure/crm-upload/index.vue';
  import type { CrmFileItem } from '@/components/pure/crm-upload/types';

  import { useI18n } from '@/hooks/useI18n';
  import useLocale from '@/locale/useLocale';

  const { currentLocale } = useLocale();

  const { t } = useI18n();

  const props = defineProps<{
    confirmLoading: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'validate', files: CrmFileItem[]): void;
    (e: 'close'): void;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  function cancel() {
    showModal.value = false;
    emit('close');
  }

  const fileList = ref<CrmFileItem[]>([]);

  /**
   * 下载excel模板
   */
  function downLoadTemplate() {
    if (currentLocale.value === 'zh-CN') {
      window.open('/templates/user_import_cn.xlsx', '_blank');
    } else {
      window.open('/templates/user_import_en.xlsx', '_blank');
    }
  }
  /**
   * 校验模板
   */
  const validateLoading = ref<boolean>(false);
  function validate() {
    emit('validate', fileList.value);
  }
</script>

<style scoped></style>
