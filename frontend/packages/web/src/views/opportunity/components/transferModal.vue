<template>
  <CrmModal
    v-model:show="showModal"
    size="small"
    :title="t('common.batchTransfer')"
    :ok-loading="loading"
    :positive-text="t('common.transfer')"
    @confirm="confirmHandler"
    @cancel="closeHandler"
  >
    <div>
      <TransferForm ref="transferFormRef" v-model:form="form" :module-type="moduleType" />
    </div>
  </CrmModal>
</template>

<script lang="ts" setup>
  import { DataTableRowKey, useMessage } from 'naive-ui';

  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import TransferForm from './transferForm.vue';

  import { defaultTransferForm } from '@/config/opportunity';
  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    optIds: DataTableRowKey[];
    moduleType: ModuleConfigEnum;
  }>();

  const emit = defineEmits<{
    (e: 'loadList'): void;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  // TODO 类型
  const form = ref<any>({
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
        try {
          loading.value = true;
          // TODO lmy 联调
          console.log('🤔️ =>', props.optIds);
          closeHandler();
          emit('loadList');
          Message.success(t('common.transferSuccess'));
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        }
      }
    });
  }
</script>

<style lang="less" scoped></style>
