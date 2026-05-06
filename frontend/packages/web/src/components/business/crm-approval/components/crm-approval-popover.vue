<template>
  <n-popover class="!p-0" :show-arrow="false" :disabled="popoverDisabled" @update:show="handlePopoverShow">
    <template #trigger>
      <slot>
        <CrmApprovalStatus :status="props.status" :class="{ 'cursor-pointer': !popoverDisabled }" />
      </slot>
    </template>

    <div class="crm-approval-popover">
      <div class="crm-approval-popover__header">
        <div class="crm-approval-popover__title">{{ displayTitle }}</div>

        <n-button v-if="props.showMore" text type="primary" class="!text-[14px]" @click="emit('more', props.sourceId)">
          {{ t('common.more') }}
        </n-button>
      </div>

      <n-spin :show="loading">
        <n-scrollbar class="max-h-[40vh]">
          <CrmApprovalApproverContent
            v-if="approvers.length"
            v-model:active-id="activeApproverId"
            :approvers="approvers"
          />

          <div v-else class="crm-approval-popover__empty">
            {{ t('common.noData') }}
          </div>
        </n-scrollbar>
      </n-spin>
    </div>
  </n-popover>
</template>

<script setup lang="ts">
  import { NButton, NPopover, NScrollbar, NSpin } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ApprovalPopoverDetail, ApproverItem, ProcessStatusType } from '@lib/shared/models/system/process';

  import CrmApprovalApproverContent from '@/components/business/crm-approval/components/crm-approval-approver-content.vue';
  import CrmApprovalStatus from '@/components/business/crm-approval/components/crm-approval-status.vue';

  import { getResourceApprovingDetail } from '@/api/modules';

  export type ApprovalPopoverFormKeyType =
    | FormDesignKeyEnum.CONTRACT
    | FormDesignKeyEnum.INVOICE
    | FormDesignKeyEnum.OPPORTUNITY_QUOTATION
    | FormDesignKeyEnum.ORDER;

  const props = withDefaults(
    defineProps<{
      status: ProcessStatusType;
      formKey: ApprovalPopoverFormKeyType;
      sourceId?: string;
      title?: string;
      showMore?: boolean;
      disabled?: boolean;
      transformData?: (detail: ApprovalPopoverDetail) => ApproverItem[];
    }>(),
    {
      title: '',
      showMore: true,
      disabled: false,
    }
  );

  const emit = defineEmits<{
    (e: 'more', sourceId?: string): void;
  }>();

  const { t } = useI18n();

  const loading = ref(false);
  const loaded = ref(false);
  const approvers = ref<ApproverItem[]>([]);
  const activeApproverId = ref('');

  const displayTitle = computed(() => props.title || t('common.approver'));
  const popoverDisabled = computed(() => props.disabled || !props.sourceId || approvers.value.length === 0);

  function getApprovers(detail: ApprovalPopoverDetail) {
    return props.transformData ? props.transformData(detail) : detail.approveUserList;
  }

  async function initDetail() {
    if (!props.sourceId || loading.value) return;

    loading.value = true;

    try {
      const detail = await getResourceApprovingDetail(props.sourceId);
      approvers.value = getApprovers(detail);
      activeApproverId.value = approvers.value[0]?.id ?? '';
      loaded.value = true;
    } catch (error) {
      approvers.value = [];
      activeApproverId.value = '';
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handlePopoverShow(show: boolean) {
    if (!show || loaded.value) return;
    initDetail();
  }

  watch(
    () => props.sourceId,
    () => {
      loaded.value = false;
      approvers.value = [];
      activeApproverId.value = '';
    }
  );
</script>

<style scoped lang="less">
  .crm-approval-popover {
    padding: 16px;
    width: 344px;
    border-radius: 12px;
    background: var(--text-n10);
  }
  .crm-approval-popover__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .crm-approval-popover__title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-n1);
    line-height: 20px;
  }
  .crm-approval-popover__empty {
    padding-top: 12px;
    font-size: 14px;
    color: var(--text-n4);
    line-height: 20px;
  }
</style>
