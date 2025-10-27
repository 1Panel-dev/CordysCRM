<template>
  <CrmModal
    v-model:show="showModal"
    size="small"
    :title="title"
    show-icon
    :mask-closable="false"
    :type="props.type"
    :ok-button-props="{ disabled: enableReason ? !form.reason : false, type: props.type || 'primary' }"
    class="crm-form-modal"
    :positive-text="t('common.confirmMoveIn')"
    :ok-loading="loading"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <div class="mb-[16px]">{{ contentTip }}</div>
    <n-form v-if="enableReason" ref="formRef" :model="form" label-placement="left" require-mark-placement="left">
      <n-form-item path="reason" :label="t('common.moveInReason')">
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
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { DataTableRowKey, FormInst, NForm, NFormItem, NSelect } from 'naive-ui';

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
    getReasonConfig,
    moveCustomerToPool,
    moveToLeadPool,
  } from '@/api/modules';

  const { t } = useI18n();

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

  const form = ref({
    reason: null,
  });
  const successCount = ref<number>(0);
  const failCount = ref<number>(0);

  const reasonList = ref<Option[]>([]);

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

  function handleCancel() {
    showModal.value = false;
    form.value.reason = null;
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
        });
        successCount.value = success;
        failCount.value = fail;
        showToPoolResultModel.value = true;
      } else {
        const { success, fail } = await moveApiMap[props.reasonKey]({
          id: props.sourceId,
          reasonId: form.value.reason,
        });
        successCount.value = success;
        failCount.value = fail;
        showToPoolResultModel.value = true;
      }
      showModal.value = false;
      emit('refresh');
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    } finally {
      loading.value = false;
    }
  }

  const enableReason = ref(false);
  function handleConfirm() {
    if (enableReason.value) {
      formRef.value?.validate(async (error) => {
        if (!error) {
          handleSave();
        }
      });
    } else {
      handleSave();
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

  watch(
    () => showModal.value,
    (val) => {
      if (val) {
        initReasonConfig();
      }
    }
  );
</script>

<style scoped></style>
