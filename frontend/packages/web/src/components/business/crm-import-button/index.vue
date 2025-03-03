<template>
  <n-button type="primary" ghost class="n-btn-outline-primary" @click="handleImport">
    {{ props.buttonText }}
  </n-button>

  <ImportModal
    v-model:show="importModal"
    :title="props.title"
    :description-tip="props.descriptionTip"
    :confirm-loading="validateLoading"
    @validate="validateTemplate"
  />

  <ValidateModal
    v-model:show="validateModal"
    :percent="progress"
    @cancel="cancelValidate"
    @check-finished="checkFinished"
  />

  <ValidateResult
    v-model:show="validateResultModal"
    :validate-info="validateInfo"
    :import-loading="importLoading"
    @save="importHandler"
    @close="importModal = false"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, useMessage } from 'naive-ui';

  import type { ValidateInfo } from '@lib/shared/models/system/org';

  import type { CrmFileItem } from '@/components/pure/crm-upload/types';
  import ImportModal from './components/importModal.vue';
  import ValidateModal from './components/validateModal.vue';
  import ValidateResult from './components/validateResult.vue';

  import { useI18n } from '@/hooks/useI18n';
  import useProgressBar from '@/hooks/useProgressBar';

  const { t } = useI18n();
  const { progress, start, finish } = useProgressBar();
  const Message = useMessage();

  const props = defineProps<{
    title?: string;
    buttonText?: string;
    descriptionTip?: string; // 描述提示
    validateApi: (file: File) => Promise<{ data: ValidateInfo }>; // 导入校验Api
    importSaveApi: (file: File) => Promise<any>; // 导入保存Api
  }>();

  const emit = defineEmits<{
    (e: 'importSuccess'): void;
  }>();

  const importModal = ref<boolean>(false);
  const validateLoading = ref<boolean>(false);

  function handleImport() {
    importModal.value = true;
  }

  const validateModal = ref<boolean>(false);
  function cancelValidate() {
    validateModal.value = false;
  }

  const validateResultModal = ref<boolean>(false);
  function checkFinished() {
    validateLoading.value = false;
    validateResultModal.value = true;
  }

  const validateInfo = ref<ValidateInfo>({
    failCount: 0,
    successCount: 0,
    errorMessages: [],
  });

  // 导入
  const fileList = ref<CrmFileItem[]>([]);
  const importLoading = ref<boolean>(false);

  async function importHandler() {
    try {
      importLoading.value = true;
      await props.importSaveApi(fileList.value[0].file as File);
      Message.success(t('common.importSuccess'));

      emit('importSuccess');
      validateResultModal.value = false;
      importModal.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      importLoading.value = false;
    }
  }

  // 校验导入模板
  async function validateTemplate(files: CrmFileItem[]) {
    fileList.value = files;
    validateLoading.value = true;
    try {
      validateModal.value = true;
      start();

      const result = await props.validateApi(fileList.value[0].file as File);
      validateInfo.value = result.data;
      finish();
    } catch (error) {
      validateModal.value = false;
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      validateLoading.value = false;
    }
  }
</script>

<style scoped></style>
