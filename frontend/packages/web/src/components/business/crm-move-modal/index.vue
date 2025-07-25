<template>
  <CrmModal
    v-model:show="showModal"
    size="small"
    :title="title"
    show-icon
    :ok-button-props="{ disabled: !form.reason }"
    class="crm-form-modal"
    :ok-loading="loading"
    @confirm="handleSave"
    @cancel="handleCancel"
  >
    <div class="mb-[16px]">{{ contentTip }}</div>
    <n-form ref="formRef" :model="form" label-placement="left" require-mark-placement="left">
      <n-form-item path="reason" :label="t('common.moveInReason')">
        <n-select v-model:value="form.reason" :placeholder="t('common.pleaseSelect')" clearable :options="reasonList" />
      </n-form-item>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { DataTableRowKey, FormInst, NForm, NFormItem, NSelect, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    formKey: FormDesignKeyEnum.CLUE | FormDesignKeyEnum.CUSTOMER | FormDesignKeyEnum.BUSINESS;
    name: string;
    sourceId: DataTableRowKey[] | DataTableRowKey;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const form = ref({
    reason: null,
  });

  const reasonList = ref([]);

  const title = computed(() => {
    const isArraySourceIds = Array.isArray(props.sourceId);
    switch (props.formKey) {
      case FormDesignKeyEnum.CLUE:
        return isArraySourceIds
          ? t('clue.batchMoveIntoCluePoolTitleTip', { number: props.sourceId.length })
          : t('clue.batchMoveIntoCluePoolTitle', { name: props.name });
      case FormDesignKeyEnum.CUSTOMER:
        return isArraySourceIds
          ? t('customer.moveCustomerToOpenSeaTitleTip', { number: props.sourceId.length })
          : t('customer.batchMoveTitleTip', { name: props.name });
      default:
        break;
    }
  });

  const contentTip = computed(() => {
    switch (props.formKey) {
      case FormDesignKeyEnum.CLUE:
        return t('clue.moveToLeadPoolTip');
      case FormDesignKeyEnum.CUSTOMER:
        return t('customer.batchMoveContentTip');
      default:
        break;
    }
  });

  function handleCancel() {
    showModal.value = false;
    form.value.reason = null;
  }

  const formRef = ref<FormInst | null>(null);
  const loading = ref(false);
  function handleSave() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          showModal.value = false;
          emit('refresh');
          Message.success(t('common.moveInSuccess'));
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          loading.value = false;
        }
      }
    });
  }
</script>

<style scoped></style>
