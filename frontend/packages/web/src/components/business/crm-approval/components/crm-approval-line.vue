<template>
  <n-scrollbar x-scrollable>
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
          <n-tooltip trigger="hover">
            <template #trigger>
              <div class="one-line-text">{{ props.submitter.submitter }}</div>
            </template>
            {{ props.submitter.submitter }}
          </n-tooltip>
        </div>
        <div v-if="props.submitter.comment" class="mt-[8px] bg-[var(--text-n9)] p-[8px]">
          <div class="text-[var(--text-n4)]">{{ props.submitter.comment }}</div>
        </div>
      </n-timeline-item>
      <n-timeline-item v-for="(node, index) in props.nodes" :key="node.nodeId">
        <template #icon>
          <div v-if="node.endNode" class="timeline-icon-wrapper" :class="getIconClass(node)">
            <CrmIcon type="iconicon_end" :size="14" color="var(--text-n10)" />
          </div>
          <div v-else class="timeline-icon-wrapper" :class="getIconClass(node)">
            <CrmIcon type="iconicon_contract" :size="14" color="var(--text-n10)" />
          </div>
        </template>
        <template #header>
          <n-collapse :default-expanded-names="node.taskNodes?.length && !node.endNode ? [node.nodeId] : []">
            <template #arrow>
              <div></div>
            </template>
            <n-collapse-item :name="node.nodeId" :disabled="!node.taskNodes?.length || node.endNode">
              <template #header>
                <div class="mb-[8px] flex w-full items-center justify-between gap-[8px]">
                  <div class="flex flex-1 items-center gap-[8px] overflow-hidden">
                    <n-tooltip trigger="hover">
                      <template #trigger>
                        <div class="one-line-text font-semibold !leading-[22px] text-[var(--text-n1)]">
                          {{ node.nodeName }}
                        </div>
                      </template>
                      {{ node.nodeName }}
                    </n-tooltip>
                    <CrmTag v-if="!node.endNode && node.taskNodes?.length > 1" type="info" theme="outline">
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
                    <n-popover v-if="node.backNode" trigger="hover" placement="left-start">
                      <template #trigger>
                        <CrmIcon type="iconicon_info_circle_filled" color="var(--warning-yellow)" :size="16" />
                      </template>
                      <div class="flex max-w-[400px] flex-col items-center gap-[8px]">
                        <div class="mr-auto flex items-center gap-[8px]">
                          <CrmIcon type="iconicon_info_circle_filled" color="var(--warning-yellow)" :size="16" />
                          <div>{{ t('crm.approval.fallbackReason') }}</div>
                        </div>
                        <div class="text-[var(--text-n4)]">{{ node.backReason }}</div>
                        <n-scrollbar :content-style="{ maxHeight: '400px' }" class="mt-[8px]">
                          <CrmFileList
                            v-if="node.backAttachments?.length > 0"
                            :files="node.backAttachments"
                            class="mt-[8px]"
                            readonly
                          />
                        </n-scrollbar>
                      </div>
                    </n-popover>
                  </div>
                </div>
              </template>
              <template v-if="node.taskNodes?.length && !node.endNode" #header-extra="{ collapsed }">
                <CrmIcon :type="collapsed ? 'iconicon_chevron_right' : 'iconicon_chevron_down'" :size="16" />
              </template>
              <div class="mb-[16px] mt-[2px] py-[8px] pl-0">
                <n-collapse
                  v-if="node.taskNodes?.length"
                  :default-expanded-names="
                    node.taskNodes.filter((e) => e.comment || e.attachments?.length).map((e) => e.taskId)
                  "
                >
                  <template #arrow>
                    <div></div>
                  </template>
                  <n-collapse-item v-for="task in node.taskNodes" :name="task.taskId" class="!ml-0">
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
                        <n-tooltip trigger="hover">
                          <template #trigger>
                            <div class="one-line-text max-w-[60px]">{{ task.approver }}</div>
                          </template>
                          {{ task.approver }}
                        </n-tooltip>
                        <n-popover v-if="task.sign" trigger="hover" placement="left-start">
                          <template #trigger>
                            <CrmTag type="info" theme="outline" tooltipDisabled>
                              {{ t('common.COUNTERSIGNATURE') }}
                            </CrmTag>
                          </template>
                          <div class="max-w-[400px] flex-col items-center gap-[8px]">
                            <div class="mr-auto flex items-center gap-[8px]">
                              <CrmIcon type="iconicon_info_circle_filled" color="var(--warning-yellow)" :size="16" />
                              <div>{{ t('crm.approval.addSign') }}</div>
                            </div>
                            <div class="text-[var(--text-n4)]">{{ task.signComment }}</div>
                            <n-scrollbar :content-style="{ maxHeight: '400px' }" class="mt-[8px]">
                              <CrmFileList
                                v-if="task.signAttachments?.length > 0"
                                :files="task.signAttachments"
                                class="mt-[8px]"
                                readonly
                              />
                            </n-scrollbar>
                          </div>
                        </n-popover>
                      </div>
                    </template>
                    <template #header-extra>
                      <div class="text-[var(--text-n4)]">
                        {{ task.approvalTime ? dayjs(task.approvalTime).format('YYYY-MM-DD HH:mm') : '-' }}
                      </div>
                    </template>
                    <div
                      v-if="
                        [
                          ProcessStatusEnum.APPROVED,
                          ProcessStatusEnum.AUTO_APPROVED,
                          ProcessStatusEnum.UNAPPROVED,
                          ProcessStatusEnum.AUTO_UNAPPROVED,
                        ].includes(task.approvalStatus) && task.comment
                      "
                      class="flex flex-wrap gap-[8px] bg-[var(--text-n9)] p-[8px]"
                    >
                      <div class="text-[var(--text-n4)]">{{ task.comment }}</div>
                    </div>
                    <CrmFileList
                      v-if="task.attachments?.length > 0"
                      :files="task.attachments"
                      class="mt-[8px]"
                      readonly
                    />
                  </n-collapse-item>
                </n-collapse>
              </div>
              <CrmFileList v-if="node.attachments?.length > 0" :files="node.attachments" class="mt-[8px]" readonly />
              <n-collapse v-if="node.ccNodes?.length">
                <template #arrow>
                  <div></div>
                </template>
                <n-collapse-item :title="t('common.copyTo')" name="copyTo" class="!ml-0">
                  <template #header>
                    <div class="flex items-center gap-[8px]">
                      <CrmIcon type="iconicon_send" color="var(--text-n4)" />
                      <div>{{ t('common.copyTo') }}</div>
                    </div>
                  </template>
                  <template #header-extra="{ collapsed }">
                    <div class="flex items-center gap-[16px]">
                      <div class="text-[var(--text-n4)]">
                        {{ t('crm.approval.copyToTip', { count: node.ccNodes.length }) }}
                      </div>
                      <CrmIcon :type="collapsed ? 'iconicon_chevron_right' : 'iconicon_chevron_down'" :size="16" />
                    </div>
                  </template>
                  <div class="flex flex-wrap gap-[8px] bg-[var(--text-n9)] p-[8px]">
                    <div v-for="cc in node.ccNodes" :key="cc.ccUserId" class="flex items-center gap-[8px]">
                      <div class="h-[24px] w-[24px]">
                        <CrmAvatar :avatar="cc.ccUserAvatar" :word="cc.ccUserName" :is-user="false" :size="24" />
                      </div>
                      <n-tooltip trigger="hover">
                        <template #trigger>
                          <div class="one-line-text font-normal">{{ cc.ccUserName }}</div>
                        </template>
                        {{ cc.ccUserName }}
                      </n-tooltip>
                    </div>
                  </div>
                </n-collapse-item>
              </n-collapse>
            </n-collapse-item>
          </n-collapse>
        </template>
      </n-timeline-item>
    </n-timeline>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NCollapse, NCollapseItem, NPopover, NScrollbar, NTimeline, NTimelineItem, NTooltip } from 'naive-ui';
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
      comment?: string;
    };
    currentApprovalNode?: ApprovalNode;
    currentApprovalNodeIndex: number;
    finallyResult?: ProcessStatusEnum;
  }>();

  const { t } = useI18n();

  const MultiApproverModeMap = {
    [MultiApproverModeEnum.ALL]: t('process.process.flow.multiApprovalType.all'),
    [MultiApproverModeEnum.ANY]: t('process.process.flow.multiApprovalType.majority'),
    [MultiApproverModeEnum.SEQUENTIAL]: t('process.process.flow.multiApprovalType.sequential'),
  };

  function getIconClass(node: ApprovalNode) {
    const { approvalStatus, endNode, nodeId } = node;
    if (endNode) {
      if (
        props.currentApprovalNode?.nodeId === nodeId ||
        (props.finallyResult && [ProcessStatusEnum.REVOKED, ProcessStatusEnum.UNAPPROVED].includes(props.finallyResult))
      ) {
        return 'bg-[var(--success-green)]';
      }
      return 'bg-[var(--text-n4)]';
    }
    if (!approvalStatus && props.currentApprovalNode?.nodeId === nodeId) {
      return 'bg-[var(--info-blue)]';
    }
    switch (approvalStatus) {
      case ProcessStatusEnum.APPROVED:
      case ProcessStatusEnum.AUTO_APPROVED:
        return 'bg-[var(--success-green)]';
      case ProcessStatusEnum.UNAPPROVED:
      case ProcessStatusEnum.AUTO_UNAPPROVED:
        return 'bg-[var(--error-red)]';
      case ProcessStatusEnum.APPROVING:
        return 'bg-[var(--info-blue)]';
      default:
        return 'bg-[var(--text-n4)]';
    }
  }
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
