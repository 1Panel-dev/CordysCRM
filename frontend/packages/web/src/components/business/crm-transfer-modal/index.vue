<template>
  <CrmModal
    v-model:show="showModal"
    size="small"
    :title="title"
    :ok-loading="loading"
    :positive-text="props.positiveText || t('common.transfer')"
    @confirm="confirmHandler"
    @cancel="closeHandler"
  >
    <TransferForm ref="transferFormRef" v-model:form="form" />
  </CrmModal>
</template>

<script lang="ts" setup>
  import { DataTableRowKey, useMessage } from 'naive-ui';

  import type { TransferParams } from '@lib/shared/models/customer/index';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import TransferForm from './transferForm.vue';

  import { defaultTransferForm } from '@/config/opportunity';
  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();
  const Message = useMessage();

  interface TransferModalProps {
    title?: string;
    sourceIds: DataTableRowKey[];
    isBatch?: boolean;
    positiveText?: string;
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

  function closeHandler() {
    form.value = { ...defaultTransferForm };
  }

  const loading = ref<boolean>(false);

  const transferFormRef = ref<InstanceType<typeof TransferForm>>();

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
