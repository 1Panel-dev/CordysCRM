<template>
  <n-button type="primary" ghost class="n-btn-outline-primary" @click="handleImport">
    {{ `${t('common.import')}${props.title ?? ''}` }}
  </n-button>

  <ImportModal
    v-model:show="importModal"
    :title="props.title"
    :description-tip="props.descriptionTip"
    :confirm-loading="validateLoading"
    :download-template-api="downloadTemplateApi"
    :show-import-radio="showImportRadio"
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
    :title="props.title"
    @save="importHandler"
    @close="importModal = false"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ValidateInfo } from '@lib/shared/models/system/org';

  import type { CrmFileItem } from '@/components/pure/crm-upload/types';
  import ImportModal from './components/importModal.vue';
  import ValidateModal from './components/validateModal.vue';
  import ValidateResult from './components/validateResult.vue';

  import useProgressBar from '@/hooks/useProgressBar';

  import { importApiMap, ImportApiType, type ImportRequestParams } from './utils';

  const { t } = useI18n();
  const { progress, start, finish } = useProgressBar();
  const Message = useMessage();

  const props = defineProps<{
    apiType: ImportApiType;
    title?: string;
    buttonText?: string;
    descriptionTip?: string; // 描述提示
    customFormId?: string;
  }>();

  const emit = defineEmits<{
    (e: 'importSuccess'): void;
  }>();

  const importModal = ref<boolean>(false);
  const validateLoading = ref<boolean>(false);

  const showImportRadio = computed(() => !([FormDesignKeyEnum.PRICE] as ImportApiType[]).includes(props.apiType));

  function handleImport() {
    importModal.value = true;
  }

  const validateModal = ref<boolean>(false);
  function cancelValidate() {
    validateModal.value = false;
  }

  const initValidateInfo: ValidateInfo = {
    failCount: 0,
    successCount: 0,
    errorMessages: [],
  };

  const validateInfo = ref<ValidateInfo>({
    ...initValidateInfo,
  });

  const validateResultModal = ref<boolean>(false);
  function checkFinished() {
    validateLoading.value = false;
    const { failCount, successCount } = validateInfo.value;
    if (successCount > 0 || failCount > 0) {
      validateResultModal.value = true;
    }
  }

  // 导入
  const fileList = ref<CrmFileItem[]>([]);
  const importLoading = ref<boolean>(false);
  const importType = ref('');

  const downloadTemplateApi = computed(() => {
    const download = importApiMap[props.apiType]?.download;
    return download ? () => download(props.customFormId) : undefined;
  });

  function getImportRequestParams(file: File, type?: string): ImportRequestParams {
    return {
      uploadParams: {
        fileList: [file],
        request: showImportRadio.value ? { importType: type } : undefined,
      },
      customFormId: props.customFormId,
    };
  }

  async function importHandler() {
    try {
      importLoading.value = true;
      const params = getImportRequestParams(fileList.value[0].file as File, importType.value);
      await importApiMap[props.apiType].save(params);
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
  async function validateTemplate(files: CrmFileItem[], type?: string) {
    fileList.value = files;
    if (type) {
      importType.value = type;
    }
    const file = fileList.value[0].file as File;

    // 防止修改后未上传就校验
    try {
      await file.arrayBuffer();
    } catch (err) {
      // eslint-disable-next-line no-console
      console.log(err);
      Message.warning(t('crmImportButton.fileChange'));
      return;
    }
    validateInfo.value = {
      ...initValidateInfo,
    };
    validateLoading.value = true;
    try {
      validateModal.value = true;
      start();

      const result = await importApiMap[props.apiType].preCheck(getImportRequestParams(file, type));
      validateInfo.value = result.data;
      finish();
    } catch (error) {
      validateResultModal.value = false;
      validateModal.value = false;
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      validateLoading.value = false;
    }
  }
</script>

<style scoped></style>
