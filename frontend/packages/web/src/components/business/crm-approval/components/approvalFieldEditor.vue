<template>
  <div class="flex h-full min-h-0 flex-col">
    <div
      class="mb-[8px] min-h-0 flex-1"
      :class="props.fieldPermissions.filter((e) => e.permissionType === 'EDIT').length ? 'pb-[64px]' : 'pb-[24px]'"
    >
      <CrmFormCreate
        v-if="isEditing && props.fieldPermissions.length"
        ref="formCreateRef"
        v-model:loading="formLoading"
        :form-key="props.descriptionFormKey || props.formKey"
        :source-id="props.sourceId"
        need-init-detail
        is-edit
        inline-approval-edit
        :approval-field-permissions="props.fieldPermissions"
        :other-save-params="otherSaveParams"
        :custom-form-id="props.customFormId"
        @ready="props.setLoading(false)"
        @saved="handleSaved"
      />
      <CrmFormDescription
        v-else
        ref="formDescriptionRef"
        :form-key="props.descriptionFormKey || props.formKey"
        :field-permissions="props.fieldPermissions"
        :source-id="props.sourceId"
        :column="props.column"
        :refresh-key="props.refreshKey"
        :refresh-form-config="props.refreshFormConfig"
        label-width="auto"
        value-align="start"
        tooltip-position="top-start"
        :readonly="props.readonly"
        :is-contract-table-detail="props.isContractTableDetail"
        :other-save-params="otherSaveParams"
        :custom-form-id="props.customFormId"
        @init="handleDescriptionInit"
        @open-customer-detail="emit('openCustomerDetail', $event)"
        @open-contract-detail="emit('openContractDetail', $event)"
        @open-opportunity-detail="emit('openOpportunityDetail', $event)"
        @open-quotation-detail="emit('openQuotationDetail', $event)"
        @refresh="emit('refresh', $event)"
      />
    </div>
    <div
      v-if="props.fieldPermissions.filter((e) => e.permissionType === 'EDIT').length"
      class="absolute bottom-0 z-10 flex w-full shrink-0 justify-start border-t border-[var(--text-n8)] bg-[var(--text-n10)] py-[16px]"
    >
      <n-button v-if="!isEditing" type="primary" ghost @click="startEditing">
        {{ t('common.edit') }}
      </n-button>
      <template v-else>
        <n-button type="default" class="mr-[12px]" @click="cancelEditing">
          {{ t('common.cancel') }}
        </n-button>
        <n-button type="primary" :loading="formLoading" @click="formCreateRef?.handleSave()">
          {{ t('common.save') }}
        </n-button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig } from '@lib/shared/models/system/module';
  import type { ApprovalFieldPermission, ApprovalNodeTaskNode } from '@lib/shared/models/system/process';

  import CrmFormCreate from '@/components/business/crm-form-create/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';

  const props = withDefaults(
    defineProps<{
      formKey: FormDesignKeyEnum;
      descriptionFormKey?: FormDesignKeyEnum;
      sourceId: string;
      fieldPermissions: ApprovalFieldPermission[];
      taskNode?: ApprovalNodeTaskNode;
      refreshKey?: number;
      refreshFormConfig?: boolean;
      column?: number;
      readonly?: boolean;
      isContractTableDetail?: boolean;
      customFormId?: string;
      approvalTaskId?: string;
      setEditing: (editing: boolean) => void;
      setLoading: (loading: boolean) => void;
    }>(),
    {
      refreshKey: 0,
      refreshFormConfig: false,
      column: 2,
      readonly: false,
      isContractTableDetail: false,
    }
  );

  const emit = defineEmits<{
    (
      e: 'init',
      collaborationType?: CollaborationType,
      sourceName?: string,
      detail?: Record<string, any>,
      config?: FormConfig
    ): void;
    (e: 'refresh', params?: { id: string }): void;
    (e: 'saved'): void;
    (e: 'openCustomerDetail', params: { customerId: string; inCustomerPool: boolean; poolId: string }): void;
    (e: 'openContractDetail', params: { id: string }): void;
    (e: 'openOpportunityDetail', params: { id: string }): void;
    (e: 'openQuotationDetail', params: { id: string }): void;
  }>();

  const { t } = useI18n();
  const isEditing = ref(false);
  const formLoading = ref(false);
  const formCreateRef = ref<InstanceType<typeof CrmFormCreate>>();
  const formDescriptionRef = ref<InstanceType<typeof CrmFormDescription>>();
  const otherSaveParams = computed(() => ({
    updateType: 'approval',
    approvalTaskId: props.approvalTaskId || props.taskNode?.taskId,
    ...(props.customFormId ? { customFormId: props.customFormId } : {}),
  }));

  function startEditing() {
    props.setLoading(true);
    isEditing.value = true;
    props.setEditing(true);
  }

  function cancelEditing() {
    props.setLoading(true);
    isEditing.value = false;
    props.setEditing(false);
  }

  function handleDescriptionInit(
    collaborationType?: CollaborationType,
    sourceName?: string,
    detail?: Record<string, any>,
    config?: FormConfig
  ) {
    emit('init', collaborationType, sourceName, detail, config);
    props.setLoading(false);
  }

  function handleSaved() {
    isEditing.value = false;
    props.setEditing(false);
    emit('saved');
  }

  function handleFormChange(callback?: () => Promise<void>) {
    if (isEditing.value) {
      if (callback) {
        formCreateRef.value?.handleSaveWithCallback(callback);
      } else {
        formCreateRef.value?.handleSave();
      }
      return;
    }
    formDescriptionRef.value?.handleFormChange(callback);
  }

  function initFormDescription() {
    formDescriptionRef.value?.initFormDescription();
  }

  defineExpose({ handleFormChange, initFormDescription });
</script>
