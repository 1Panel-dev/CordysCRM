<template>
  <n-scrollbar :content-style="{ width: '100%', overflowX: 'hidden' }" x-scrollable>
    <n-timeline :icon-size="20" class="w-full min-w-[300px] gap-[4px]">
      <n-timeline-item>
        <template #icon>
          <div class="timeline-icon-wrapper bg-[var(--primary-8)]">
            <CrmIcon type="iconicon_add" :size="14" color="var(--text-n10)" />
          </div>
        </template>
        <template #header>
          <div class="mb-[16px] flex items-center justify-between">
            <div class="font-semibold leading-[22px]">{{ t('crm.approval.submit') }}</div>
            <div class="text-[var(--text-n4)]">
              {{ dayjs(props.submitter.submitTime).format('YYYY-MM-DD HH:mm') }}
            </div>
          </div>
        </template>
        <div class="flex items-center gap-[8px] bg-[var(--text-n9)] p-[8px]">
          <div class="h-[24px] w-[24px]">
            <CrmAvatar
              :avatar="props.submitter.submitAvatar"
              :word="props.submitter.submitter"
              :is-user="false"
              :size="24"
            />
          </div>
          <div class="one-line-text">{{ props.submitter.submitter }}</div>
        </div>
      </n-timeline-item>
      <n-timeline-item v-for="(node, index) in props.nodes">
        <template #icon>
          <div v-if="node.endNode" class="timeline-icon-wrapper bg-[var(--text-n6)]">
            <CrmIcon type="iconicon_end" :size="14" color="var(--text-n10)" />
          </div>
          <div v-else class="timeline-icon-wrapper bg-[var(--info-blue)]">
            <CrmIcon type="iconicon_contract" :size="14" color="var(--text-n10)" />
          </div>
        </template>
        <template #header>
          <div class="mb-[8px] flex w-full items-center justify-between gap-[8px]">
            <div class="flex flex-1 items-center gap-[8px] overflow-hidden">
              <div class="one-line-text font-semibold !leading-[22px]">{{ node.nodeName }}</div>
              <CrmTag v-if="node.addSignNode" type="info" theme="outline">
                {{ t('common.COUNTERSIGNATURE') }}
              </CrmTag>
              <CrmTag v-else-if="!node.endNode" type="info" theme="outline">
                {{ MultiApproverModeMap[node.multiApproverMode] }}
              </CrmTag>
              <CrmApprovalStatus
                v-if="!node.endNode"
                :status="
                  index > props.currentApprovalNodeIndex && props.currentApprovalNodeIndex !== -1
                    ? ProcessStatusEnum.PENDING
                    : node.approvalStatus
                "
                isTag
                scene="approvalRecord"
                class="font-normal"
              />
              <n-popover v-if="node.returnNode" trigger="hover">
                <template #trigger>
                  <CrmIcon type="iconicon_info_circle_filled" color="var(--warning-yellow)" :size="16" />
                </template>
                <div class="flex flex-col items-center gap-[8px]">
                  <div class="flex items-center gap-[8px]">
                    <CrmIcon type="iconicon_info_circle_filled" color="var(--warning-yellow)" :size="16" />
                    <div>{{ t('crm.approval.fallbackReason') }}</div>
                  </div>
                  <div class="text-[var(--text-n4)]">{{ node.comment }}</div>
                </div>
              </n-popover>
              <!-- <n-popover v-if="node.returnNode" trigger="hover">
                <template #trigger>
                  <CrmIcon type="iconicon_info_circle_filled" color="var(--error-red)" :size="16" />
                </template>
                <div>自动拒绝</div>
              </n-popover>
              <n-popover v-if="node.returnNode" trigger="hover">
                <template #trigger>
                  <CrmIcon type="iconicon_info_circle_filled" color="var(--success-green)" :size="16" />
                </template>
                <div>自动同意</div>
              </n-popover> -->
              <n-popover v-if="node.addSignNode" trigger="hover">
                <template #trigger>
                  <CrmIcon type="iconicon_info_circle_filled" color="var(--warning-yellow)" :size="16" />
                </template>
                <div class="flex flex-col items-center gap-[8px]">
                  <div class="flex items-center gap-[8px]">
                    <CrmIcon type="iconicon_info_circle_filled" color="var(--warning-yellow)" :size="16" />
                    <div>{{ t('crm.approval.addSign') }}</div>
                  </div>
                  <div class="text-[var(--text-n4)]">{{ node.comment }}</div>
                </div>
              </n-popover>
            </div>
            <div class="whitespace-nowrap font-normal text-[var(--text-n4)]">
              {{ dayjs(node.approvalTime).format('YYYY-MM-DD HH:mm') }}
            </div>
          </div>
        </template>
        <div class="mb-[16px] mt-[2px] py-[8px] pl-0">
          <n-collapse v-if="node.taskNodes?.length">
            <template #arrow><div></div></template>
            <n-collapse-item v-for="task in node.taskNodes" :name="task.taskId">
              <template #header>
                <div class="flex items-center gap-[8px]">
                  <div class="relative h-[24px] w-[30px]">
                    <CrmApprovalAvatar
                      :size="24"
                      :approver="{
                        avatar: task.approverAvatar,
                        name: task.approver,
                        id: task.approverId,
                        approveResult: task.approvalStatus,
                      } as any"
                      :sign-node="task.signAction"
                    />
                  </div>
                  <div class="one-line-text max-w-[60px]">{{ task.approver }}</div>
                  <CrmTag v-if="task.sign" type="info" theme="outline">
                    {{ t('common.COUNTERSIGNATURE') }}
                  </CrmTag>
                </div>
              </template>
              <template #header-extra>
                <div class="text-[var(--text-n4)]">
                  {{ task.approvalTime ? dayjs(task.approvalTime).format('YYYY-MM-DD HH:mm') : '-' }}
                </div>
              </template>
              <div class="flex flex-wrap gap-[8px] bg-[var(--text-n9)] p-[8px]">
                <div class="text-[var(--text-n4)]">{{ task.comment }}</div>
              </div>
              <CrmFileList v-if="task.attachments?.length > 0" :files="task.attachments" class="mt-[8px]" readonly />
            </n-collapse-item>
          </n-collapse>
        </div>
        <CrmFileList v-if="node.attachments?.length > 0" :files="node.attachments" class="mt-[8px]" readonly />
        <n-collapse v-if="node.ccNodes?.length">
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
            <div class="flex flex-wrap gap-[8px] bg-[var(--text-n9)] p-[8px]">
              <div v-for="cc in node.ccNodes" :key="cc.ccUserId" class="flex w-[23%] items-center gap-[8px]">
                <div class="h-[24px] w-[24px]">
                  <CrmAvatar :avatar="cc.ccUserAvatar" :word="cc.ccUserName" :is-user="false" :size="24" />
                </div>
                <div class="one-line-text max-w-[60px]">{{ cc.ccUserName }}</div>
              </div>
            </div>
          </n-collapse-item>
        </n-collapse>
      </n-timeline-item>
    </n-timeline>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NCollapse, NCollapseItem, NPopover, NScrollbar, NTimeline, NTimelineItem } from 'naive-ui';
  import dayjs from 'dayjs';

  import { MultiApproverModeEnum, ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApprovalNode } from '@lib/shared/models/system/process';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmApprovalStatus from '@/components/business/crm-approval/components/crm-approval-status.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import CrmFileList from '@/components/business/crm-file-list/index.vue';
  import CrmApprovalAvatar from './crm-approval-avatar.vue';

  const props = defineProps<{
    nodes: ApprovalNode[];
    submitter: {
      submitterId?: string;
      submitAvatar?: string;
      submitter?: string;
      submitTime?: number;
    };
    currentApprovalNode?: ApprovalNode;
    currentApprovalNodeIndex: number;
  }>();

  const { t } = useI18n();

  const MultiApproverModeMap = {
    [MultiApproverModeEnum.ALL]: t('process.process.flow.multiApprovalType.all'),
    [MultiApproverModeEnum.ANY]: t('process.process.flow.multiApprovalType.majority'),
    [MultiApproverModeEnum.SEQUENTIAL]: t('process.process.flow.multiApprovalType.sequential'),
  };
</script>

<style lang="less" scoped>
  .timeline-icon-wrapper {
    @apply flex items-center justify-center;

    width: 20px;
    height: 20px;
    border-radius: 4px;
  }
  :deep(.n-timeline-item-timeline__icon) {
    margin-top: 4px;
  }
  :deep(.n-timeline-item-timeline__line) {
    top: calc(var(--n-icon-size) + 10px) !important;
    background-color: var(--text-n8) !important;
  }
  :deep(.n-timeline-item-content__title) {
    margin-bottom: 16px;
  }
</style>
