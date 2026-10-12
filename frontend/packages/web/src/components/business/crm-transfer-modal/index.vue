<template>
  <CrmModal
    v-model:show="showModal"
    :title="title"
    :ok-loading="loading"
    :positive-text="props.positiveText || t('common.transfer')"
    @confirm="confirmHandler"
  >
    <n-alert v-if="props.showApprovalTip" type="warning" class="mb-[16px]">
      <template #icon>
        <CrmIcon type="iconicon_info_circle_filled" :size="20" />
      </template>
      {{ props.showApprovalTip }}
    </n-alert>
    <TransferForm ref="transferFormRef" v-model:form="form" />
  </CrmModal>
</template>

<script lang="ts" setup>
  import { DataTableRowKey, NAlert, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { TransferParams } from '@lib/shared/models/customer/index';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import TransferForm from './transferForm.vue';

  import { defaultTransferForm } from '@/config/opportunity';

  const { t } = useI18n();
  const Message = useMessage();

  interface TransferModalProps {
    title?: string;
    sourceIds: DataTableRowKey[];
    isBatch?: boolean;
    positiveText?: string;
    showApprovalTip?: string;
    saveApi?: (params: TransferParams) => Promise<any>;
  }

  const props = withDefaults(defineProps<TransferModalProps>(), {
    isBatch: true,
  });

  const emit = defineEmits<{
    (e: 'loadList'): void;
    (e: 'confirm', owner: string | null): void;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const title = computed(() => {
    if (props.title) {
      return props.title;
    }
    return props.isBatch ? t('common.batchTransfer') : t('common.transfer');
  });

  const form = ref<TransferParams>({
    ...defaultTransferForm,
  });

  const loading = ref<boolean>(false);

  const transferFormRef = ref<InstanceType<typeof TransferForm>>();

  watch(
    () => showModal.value,
    (val) => {
      if (val) {
        form.value = { ...defaultTransferForm };
      }
    }
  );

  function confirmHandler() {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        if (props.saveApi) {
          try {
            loading.value = true;
            await props.saveApi({
              ...form.value,
              ids: props.sourceIds,
            });
            showModal.value = false;
            emit('loadList');
            Message.success(t('common.transferSuccess'));
          } catch (e) {
            // eslint-disable-next-line no-console
            console.log(e);
          } finally {
            loading.value = false;
          }
        } else {
          emit('confirm', form.value.owner);
        }
      }
    });
  }
</script>

<style lang="less" scoped></style>
