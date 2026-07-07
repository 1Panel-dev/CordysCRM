<template>
  <CrmModal
    v-model:show="showModal"
    size="small"
    :title="title"
    show-icon
    :mask-closable="false"
    :type="props.type"
    :ok-button-props="{ disabled: confirmDisabled, type: props.type || 'primary' }"
    :positive-text="t('common.confirmMoveIn')"
    :ok-loading="loading"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <div class="mb-[16px]">{{ contentTip }}</div>
    <n-form
      v-if="enableReason || isPoolReason"
      ref="formRef"
      :model="form"
      :label-width="90"
      label-placement="left"
      require-mark-placement="left"
    >
      <n-form-item v-if="isPoolReason" path="poolId" :label="poolLabel">
        <n-select
          v-model:value="form.poolId"
          :placeholder="t('common.pleaseSelect')"
          :options="poolOptions"
          :loading="poolLoading"
        >
          <template #empty>
            <div class="flex items-center justify-center">
              <span>{{ poolConfigTip }}</span>
              <n-button text type="primary" @mousedown.prevent @click.stop="handleGoConfig">
                {{ poolConfigActionText }}
              </n-button>
            </div>
          </template>
        </n-select>
      </n-form-item>
      <n-form-item v-if="enableReason" path="reason" :label="t('common.moveInReason')">
        <n-select v-model:value="form.reason" :placeholder="t('common.pleaseSelect')" clearable :options="reasonList" />
      </n-form-item>
    </n-form>
  </CrmModal>
  <toPublicPoolResultModal
    v-model:show="showToPoolResultModel"
    :fail-count="failCount"
    :success-count="successCount"
    :title="resultTitle"
    :reason-key="props.reasonKey"
    @cancel="handleResultCancel"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { DataTableRowKey, FormInst, NButton, NForm, NFormItem, NSelect } from 'naive-ui';

  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { BatchMoveToPublicPoolParams, MoveToPublicPoolParams } from '@lib/shared/models/customer';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import toPublicPoolResultModal from './toPublicPoolResultModal.vue';

  import {
    batchMoveCustomer,
    batchToCluePool,
    getOpenSeaOptions,
    getPoolOptions,
    getReasonConfig,
    moveCustomerToPool,
    moveToLeadPool,
  } from '@/api/modules';
  import useOpenNewPage from '@/hooks/useOpenNewPage';

  import { SystemRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const { openNewPage } = useOpenNewPage();

  export type ReasonKey = ReasonTypeEnum.CLUE_POOL_RS | ReasonTypeEnum.CUSTOMER_POOL_RS;

  const props = defineProps<{
    reasonKey: ReasonKey;
    name: string;
    sourceId: DataTableRowKey[] | DataTableRowKey;
    type?: 'error' | 'success' | 'warning' | 'info';
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const batchMoveApiMap: Record<ReasonKey, (params: BatchMoveToPublicPoolParams) => Promise<any>> = {
    [ReasonTypeEnum.CLUE_POOL_RS]: batchToCluePool,
    [ReasonTypeEnum.CUSTOMER_POOL_RS]: batchMoveCustomer,
  };

  const moveApiMap: Record<ReasonKey, (params: MoveToPublicPoolParams) => Promise<any>> = {
    [ReasonTypeEnum.CLUE_POOL_RS]: moveToLeadPool,
    [ReasonTypeEnum.CUSTOMER_POOL_RS]: moveCustomerToPool,
  };

  const form = ref<{
    reason: string | null;
    poolId: string | null;
  }>({
    reason: null,
    poolId: null,
  });
  const successCount = ref<number>(0);
  const failCount = ref<number>(0);

  const reasonList = ref<Option[]>([]);
  const poolOptions = ref<Option[]>([]);
  const poolLoading = ref(false);
  const enableReason = ref(false);

  const isCluePoolReason = computed(() => props.reasonKey === ReasonTypeEnum.CLUE_POOL_RS);
  const isCustomerPoolReason = computed(() => props.reasonKey === ReasonTypeEnum.CUSTOMER_POOL_RS);
  const isPoolReason = computed(() => isCluePoolReason.value || isCustomerPoolReason.value);
  const isBatchMultiSource = computed(() => Array.isArray(props.sourceId) && props.sourceId.length > 1);
  const poolLabel = computed(() => (isCluePoolReason.value ? t('clue.moveIntoCluePool') : t('customer.moveToOpenSea')));
  const poolConfigTip = computed(() =>
    isCluePoolReason.value ? t('clue.moveIntoCluePoolFailedContent1') : t('customer.moveToOpenSeaFailedContent1')
  );
  const poolConfigActionText = computed(() =>
    isCluePoolReason.value ? t('clue.moveIntoCluePoolFailedContent2') : t('customer.moveToOpenSeaFailedContent2')
  );
  const confirmDisabled = computed(() => {
    if (enableReason.value && !form.value.reason) {
      return true;
    }
    return isPoolReason.value && !form.value.poolId;
  });

  const title = computed(() => {
    const isArraySourceIds = Array.isArray(props.sourceId);
    switch (props.reasonKey) {
      case ReasonTypeEnum.CLUE_POOL_RS:
        return isArraySourceIds
          ? t('clue.batchMoveIntoCluePoolTitleTip', { number: props.sourceId.length })
          : t('clue.moveIntoCluePoolTitle', { name: characterLimit(props.name) });
      case ReasonTypeEnum.CUSTOMER_POOL_RS:
        return isArraySourceIds
          ? t('customer.batchMoveTitleTip', { number: props.sourceId.length })
          : t('customer.moveCustomerToOpenSeaTitleTip', { name: characterLimit(props.name) });
      default:
        break;
    }
  });

  const resultTitle = computed(() =>
    props.reasonKey === ReasonTypeEnum.CUSTOMER_POOL_RS ? t('customer.moveToOpenSea') : t('clue.moveIntoCluePool')
  );

  const contentTip = computed(() =>
    props.reasonKey === ReasonTypeEnum.CLUE_POOL_RS ? t('clue.moveToLeadPoolTip') : t('customer.batchMoveContentTip')
  );

  function resetForm() {
    form.value.reason = null;
    form.value.poolId = null;
    poolOptions.value = [];
    reasonList.value = [];
    enableReason.value = false;
  }

  function handleCancel() {
    showModal.value = false;
    resetForm();
  }

  const showToPoolResultModel = ref(false);
  const formRef = ref<FormInst | null>(null);
  const loading = ref(false);

  async function handleSave() {
    try {
      loading.value = true;
      const isBatch = Array.isArray(props.sourceId);

      if (isBatch) {
        const { success, fail } = await batchMoveApiMap[props.reasonKey]({
          ids: props.sourceId,
          reasonId: form.value.reason,
          poolId: isPoolReason.value ? form.value.poolId : null,
        });
        successCount.value = success;
        failCount.value = fail;
        showToPoolResultModel.value = true;
      } else {
        const { success, fail } = await moveApiMap[props.reasonKey]({
          id: props.sourceId,
          reasonId: form.value.reason,
          poolId: isPoolReason.value ? form.value.poolId : null,
        });
        successCount.value = success;
        failCount.value = fail;
        showToPoolResultModel.value = true;
      }
      if (failCount.value > 0) {
        setTimeout(() => {
          showModal.value = false;
          showToPoolResultModel.value = false;
          if (isBatch) {
            emit('refresh');
          }
        }, 3000);
      } else {
        setTimeout(() => {
          showModal.value = false;
          showToPoolResultModel.value = false;
          emit('refresh');
        }, 1000);
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    } finally {
      loading.value = false;
    }
  }

  function handleConfirm() {
    if (enableReason.value || isPoolReason.value) {
      formRef.value?.validate(async (error) => {
        if (!error) {
          handleSave();
        }
      });
    } else {
      handleSave();
    }
  }

  async function initPoolOptions() {
    if (!isPoolReason.value) {
      return;
    }
    try {
      poolLoading.value = true;
      form.value.poolId = null;
      poolOptions.value = [];
      const options = isCluePoolReason.value ? await getPoolOptions() : await getOpenSeaOptions();
      poolOptions.value = options.map((item) => ({ label: item.name, value: item.id }));
      if (isBatchMultiSource.value) {
        return;
      }
      const defaultPool = [...options].sort((prev, next) => next.createTime - prev.createTime)[0];
      form.value.poolId = defaultPool?.id ?? null;
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    } finally {
      poolLoading.value = false;
    }
  }

  async function initReasonConfig() {
    try {
      const { dictList, enable } = await getReasonConfig(props.reasonKey);
      enableReason.value = enable;
      reasonList.value = dictList.filter((e) => e.id !== 'system').map((e) => ({ label: e.name, value: e.id }));
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  function handleResultCancel() {
    showModal.value = false;
    showToPoolResultModel.value = false;
  }

  function handleGoConfig() {
    showModal.value = false;
    openNewPage(
      SystemRouteEnum.SYSTEM_MODULE,
      isCluePoolReason.value
        ? {
            openCluePoolDrawer: 'Y',
          }
        : {
            openOpenSeaDrawer: 'Y',
          }
    );
  }

  watch(
    () => showModal.value,
    (val) => {
      if (val) {
        resetForm();
        initReasonConfig();
        initPoolOptions();
      } else {
        resetForm();
      }
    }
  );
</script>

<style scoped></style>
