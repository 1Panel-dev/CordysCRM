<template>
  <n-popover class="!p-0" :show-arrow="false" :disabled="props.disabled" @update:show="handlePopoverShow">
    <template #trigger>
      <slot>
        <CrmApprovalStatus :status="props.status" />
      </slot>
    </template>
    <div class="crm-approval-popover">
      <div class="crm-approval-popover__header">
        <div class="crm-approval-popover__title">{{ title }}</div>
        <n-button v-if="showMore" text type="primary" class="!text-[14px]" @click="emit('more')">
          {{ t('common.more') }}
        </n-button>
      </div>
      <n-spin :show="loading">
        <n-scrollbar class="max-h-[40vh]">
          <CrmApprovalApproverContent v-model:active-id="activeApproverId" :approvers="approvers" />
        </n-scrollbar>
      </n-spin>
    </div>
  </n-popover>
</template>

<script setup lang="ts">
  import { NButton, NPopover, NScrollbar, NSpin } from 'naive-ui';

  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ProcessStatusType } from '@lib/shared/models/system/process';

  import CrmApprovalApproverContent from '@/components/business/crm-approval-approver-content/index.vue';
  import CrmApprovalStatus from '@/components/business/crm-approval-status/index.vue';
  import { type ApproverItem } from '@/components/business/crm-approver-avatar-list/index.vue';

  import useRejectPopoverDetail, { type ApprovalPopoverFormKeyType } from './useApprovalPopoverDetail';

  const props = withDefaults(
    defineProps<{
      status: ProcessStatusType;
      formKey: ApprovalPopoverFormKeyType;
      sourceId?: string;
      title?: string;
      showMore?: boolean;
      disabled?: boolean;
    }>(),
    {
      title: '',
      showMore: true,
    }
  );

  const emit = defineEmits<{
    (e: 'more', sourceId?: string): void;
  }>();

  const { t } = useI18n();
  const { getApprovalPopoverDetail } = useRejectPopoverDetail();

  const loading = ref(false);
  const approvers = ref<ApproverItem[]>([]);

  const activeApproverId = ref('');

  const title = computed(() => props.title || t('common.approver'));

  async function initDetail() {
    if (!props.sourceId) return;
    loading.value = true;
    try {
      const res = await getApprovalPopoverDetail(props.formKey, props.sourceId);
      // todo xinxinwu
      approvers.value = res.approveUserList || [];
      approvers.value = [
        {
          approveReason:
            '原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因1',
          approveResult: ProcessStatusEnum.APPROVED,
          id: '3164961142886311',
          name: '吴鑫鑫',
          avatar:
            'https://s1-imfile.feishucdn.com/static-resource/v1/v3_007d_71e27f5a-1b9a-46fc-bac5-cfd897657dag~?image_size=240x240&cut_type=&quality=&format=png&sticker_format=.webp',
        },
        {
          approveReason:
            '原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因2',
          approveResult: ProcessStatusEnum.UNAPPROVED,
          id: '31649611184211',
          name: '吴鑫鑫',
          avatar:
            'https://s1-imfile.feishucdn.com/static-resource/v1/v3_007d_71e27f5a-1b9a-46fc-bac5-cfd897657dag~?image_size=240x240&cut_type=&quality=&format=png&sticker_format=.webp',
        },
        {
          approveReason:
            '原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因原因3',
          approveResult: ProcessStatusEnum.APPROVING,
          id: '3164961142884211',
          name: '吴鑫鑫',
          avatar:
            'https://s1-imfile.feishucdn.com/static-resource/v1/v3_007d_71e27f5a-1b9a-46fc-bac5-cfd897657dag~?image_size=240x240&cut_type=&quality=&format=png&sticker_format=.webp',
        },
      ];
      activeApproverId.value = approvers.value[0]?.id ?? '';
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handlePopoverShow(show: boolean) {
    if (!show) return;
    if (!props.sourceId) return;
    initDetail();
  }
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
    font-size: 14px;
    color: var(--text-n4);
    line-height: 20px;
  }
</style>
