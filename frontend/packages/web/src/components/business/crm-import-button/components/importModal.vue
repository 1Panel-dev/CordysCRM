<template>
  <CrmModal
    v-model:show="showModal"
    size="medium"
    :title="`${t('common.import')}${props.title}` || t('crmImportButton.formExcelImport')"
    :ok-loading="props.confirmLoading"
    :positive-text="t('crmImportButton.validateTemplate')"
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
          {{ t('crmImportButton.importAlertDesc') }}
          <div class="flex cursor-pointer items-center gap-[8px]" @click="downLoadTemplate">
            <CrmIcon type="iconicon_file-excel_colorful" :size="16" />
            <div class="text-[var(--primary-8)]">{{ t('crmImportButton.downloadTemplate') }}</div>
          </div>
        </div>
      </n-alert>
      <CrmUpload
        v-model:file-list="fileList"
        :is-all-screen="true"
        accept="excel"
        :max-size="100"
        size-unit="MB"
        directory-dnd
        :file-type-tip="t('crmImportButton.onlyAllowFileTypeTip')"
        :disabled="validateLoading"
      />
    </div>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NAlert, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { downloadByteFile } from '@lib/shared/method';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmUpload from '@/components/pure/crm-upload/index.vue';
  import type { CrmFileItem } from '@/components/pure/crm-upload/types';

  const { loading } = useMessage();
  const { currentLocale } = useLocale(loading);

  const { t } = useI18n();

  const props = defineProps<{
    confirmLoading: boolean;
    title?: string; // 标题
    descriptionTip?: string; // 描述提示内容
    downloadTemplateApi?: () => Promise<any>; // 下载模板Api
  }>();

  const emit = defineEmits<{
    (e: 'validate', files: CrmFileItem[]): void;
    (e: 'close'): void;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const fileList = ref<CrmFileItem[]>([]);

  function cancel() {
    showModal.value = false;
    fileList.value = [];
    emit('close');
  }

  function extractFileNameFromHeader(dispositionHeader?: string): string | null {
    if (!dispositionHeader) return null;

    const match = dispositionHeader.match(/filename="?(.+?)"?$/);
    return match?.[1] ? decodeURIComponent(match[1]) : null;
  }

  /**
   * 下载excel模板
   */
  async function downLoadTemplate() {
    try {
      if (props.downloadTemplateApi) {
        const res = await props.downloadTemplateApi();
        const fileName = extractFileNameFromHeader(res.headers['content-disposition']) ?? 'template.xlsx';
        downloadByteFile(res.data, fileName);
      } else {
        const lang = currentLocale.value === 'zh-CN' ? 'cn' : 'en';
        window.open(`/templates/user_import_${lang}.xlsx`, '_blank');
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
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
