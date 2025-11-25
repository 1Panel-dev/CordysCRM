<template>
  <CrmModal
    v-model:show="showModal"
    size="small"
    :title="t('common.batchApproval')"
    :ok-loading="loading"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <n-form ref="formRef" :model="form">
      <n-form-item
        :rule="[
          {
            required: true,
            message: t('common.notNull', { value: `${t('opportunity.quotation.approvalResult')}` }),
          },
        ]"
        require-mark-placement="left"
        label-placement="left"
        path="approvalStatus"
        :label="t('opportunity.quotation.approvalResult')"
      >
        <n-select
          v-model:value="form.approvalStatus"
          :placeholder="t('common.pleaseSelect')"
          :options="reviewOptions"
        />
      </n-form-item>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, NForm, NFormItem, NSelect, useMessage } from 'naive-ui';

  import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { BatchOperationResult } from '@lib/shared/models/opportunity';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { batchApprove } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const props = defineProps<{
    quotationIds: (string | number)[];
  }>();

  const emit = defineEmits<{
    (e: 'refresh', val: BatchOperationResult): void;
  }>();

  const form = ref<{
    approvalStatus: QuotationStatusEnum | null;
  }>({
    approvalStatus: null,
  });

  const reviewOptions = ref([
    { label: t('common.pass'), value: QuotationStatusEnum.APPROVED },
    { label: t('common.unPass'), value: QuotationStatusEnum.UNAPPROVED },
  ]);

  function handleCancel() {
    form.value.approvalStatus = null;
    showModal.value = false;
  }

  const loading = ref(false);
  const formRef = ref<FormInst | null>(null);

  function handleConfirm() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          const result = await batchApprove({
            ids: props.quotationIds,
            approvalStatus: form.value.approvalStatus as QuotationStatusEnum,
          });
          emit('refresh', result);
          handleCancel();
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
