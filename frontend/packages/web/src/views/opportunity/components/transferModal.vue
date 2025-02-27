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
      <TransForm ref="transferFormRef" v-model:form="form" />
    </div>
  </CrmModal>
</template>

<script lang="ts" setup>
  import { DataTableRowKey, useMessage } from 'naive-ui';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import TransForm from './transferForm.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    optIds: DataTableRowKey[];
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
    head: null,
  });

  function closeHandler() {
    form.value.commanderId = null;
  }

  const loading = ref<boolean>(false);

  const transferFormRef = ref<InstanceType<typeof TransForm>>();

  function confirmHandler() {
    transferFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
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
