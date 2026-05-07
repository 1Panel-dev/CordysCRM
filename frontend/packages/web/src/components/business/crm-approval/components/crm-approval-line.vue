<template>
  <n-scrollbar x-scrollable>
    <n-timeline :icon-size="20" class="min-w-[300px] gap-[4px]">
      <n-timeline-item>
        <template #icon>
          <div class="timeline-icon-wrapper bg-[var(--primary-8)]">
            <CrmIcon type="iconicon_add" :size="14" color="var(--text-n10)" />
          </div>
        </template>
        <template #header>
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-[8px]">
              <div class="font-semibold">审批节点</div>
              <CrmTag type="info">{{ t(`common.${ProcessTypeEnum.AND_APPROVAL}`) }}</CrmTag>
              <CrmApprovalStatus :status="ProcessStatusEnum.APPROVED" isTag />
              <n-popover trigger="hover">
                <template #trigger>
                  <CrmIcon type="iconicon_info_circle_filled" color="var(--warning-yellow)" :size="16" />
                </template>
                <div>退回原因</div>
              </n-popover>
              <n-popover trigger="hover">
                <template #trigger>
                  <CrmIcon type="iconicon_info_circle_filled" color="var(--error-red)" :size="16" />
                </template>
                <div>自动拒绝</div>
              </n-popover>
              <n-popover trigger="hover">
                <template #trigger>
                  <CrmIcon type="iconicon_info_circle_filled" color="var(--success-green)" :size="16" />
                </template>
                <div>自动同意</div>
              </n-popover>
            </div>
            <div class="text-[var(--text-n4)]">2026-2-32 12:32:22</div>
          </div>
        </template>
        <div class="mb-[16px] mt-[2px] bg-[var(--text-n9)] p-[8px]"></div>
        <CrmFileList
          v-if="currentFileList.length > 0"
          :files="currentFileList as unknown as AttachmentInfo[]"
          class="mt-[8px]"
          readonly
        />
        <n-collapse>
          <template #arrow><div></div></template>
          <n-collapse-item :title="t('common.copyTo')" name="copyTo">
            <template #header>
              <div class="flex items-center gap-[8px]">
                <CrmIcon type="iconicon_send" color="var(--text-n4)" />
                <div>{{ t('common.copyTo') }}</div>
              </div>
            </template>
            <template #header-extra="{ collapsed }">
              <CrmIcon :type="collapsed ? 'iconicon_chevron_right' : 'iconicon_chevron_down'" :size="16" />
            </template>
            <div class="bg-[var(--text-n9)] p-[8px]"></div>
          </n-collapse-item>
        </n-collapse>
      </n-timeline-item>
      <n-timeline-item>
        <template #icon>
          <div class="timeline-icon-wrapper bg-[var(--info-blue)]">
            <CrmIcon type="iconicon_contract" :size="14" color="var(--text-n10)" />
          </div>
        </template>
        <template #header>
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-[8px]">
              <div class="font-semibold">审批节点</div>
              <CrmTag type="info">{{ t(`common.${ProcessTypeEnum.AND_APPROVAL}`) }}</CrmTag>
              <CrmApprovalStatus :status="ProcessStatusEnum.APPROVED" isTag />
            </div>
            <div class="text-[var(--text-n4)]">2026-2-32 12:32:22</div>
          </div>
        </template>
        <div class="mb-[16px] mt-[2px] bg-[var(--text-n9)] p-[8px]"></div>
        <CrmFileList
          v-if="currentFileList.length > 0"
          :files="currentFileList as unknown as AttachmentInfo[]"
          class="mt-[8px]"
          readonly
        />
        <n-collapse>
          <template #arrow><div></div></template>
          <n-collapse-item :title="t('common.copyTo')" name="copyTo">
            <template #header>
              <div class="flex items-center gap-[8px]">
                <CrmIcon type="iconicon_send" color="var(--text-n4)" />
                <div>{{ t('common.copyTo') }}</div>
              </div>
            </template>
            <template #header-extra="{ collapsed }">
              <CrmIcon :type="collapsed ? 'iconicon_chevron_right' : 'iconicon_chevron_down'" :size="16" />
            </template>
            <CrmApprovalApproverContent
              v-if="approvers.length"
              v-model:active-id="activeApproverId"
              :approvers="approvers"
            />
          </n-collapse-item>
        </n-collapse>
      </n-timeline-item>
    </n-timeline>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NCollapse, NCollapseItem, NPopover, NScrollbar, NTimeline, NTimelineItem } from 'naive-ui';

  import { ProcessStatusEnum, ProcessTypeEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApproverItem } from '@lib/shared/models/system/process';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import type { CrmFileItem } from '@/components/pure/crm-upload/types';
  import CrmApprovalApproverContent from '@/components/business/crm-approval/components/crm-approval-approver-content.vue';
  import CrmApprovalStatus from '@/components/business/crm-approval/components/crm-approval-status.vue';
  import CrmFileList from '@/components/business/crm-file-list/index.vue';
  import type { AttachmentInfo } from '@/components/business/crm-form-create/types';

  const { t } = useI18n();

  const currentFileList = ref<CrmFileItem[]>([]);
  const approvers = ref<ApproverItem[]>([]);
  const activeApproverId = ref('');
</script>

<style lang="less" scoped>
  .timeline-icon-wrapper {
    @apply flex items-center justify-center;

    width: 20px;
    height: 20px;
    border-radius: 4px;
  }
  :deep(.n-timeline-item-timeline__icon) {
    margin-top: 2px;
  }
  :deep(.n-timeline-item-timeline__line) {
    top: calc(var(--n-icon-size) + 10px) !important;
  }
</style>
