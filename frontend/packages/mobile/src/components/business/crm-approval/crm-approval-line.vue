<template>
  <div class="flex h-full min-h-0 w-full flex-col overflow-hidden">
    <CrmSegmentedControl
      v-if="props.resourceId"
      v-model="activeAnchor"
      class="mb-[12px] flex-none"
      :options="anchorOptions"
      @select="(value) => scrollToAnchor(value as 'record' | 'comment')"
    />
    <div ref="scrollContainerRef" class="min-h-0 flex-1 overflow-y-auto">
      <van-collapse
        v-if="historyInstances.length"
        v-model="expandedHistoryInstanceNames"
        class="mb-[16px] rounded-[var(--border-radius-small)]"
        :border="false"
      >
        <van-collapse-item v-for="instance in historyInstances" :key="instance.id" :name="instance.id" :border="false">
          <template #title>
            <div class="flex items-center gap-[8px]">
              <CrmApprovalStatus :status="instance.approvalStatus" scene="approvalRecord" />
              <div class="text-[14px] font-semibold text-[var(--text-n1)]">
                {{
                  t('crm.approval.previousRoundTitle', {
                    nodeName: getCurrentNode(instance)?.nodeName || instance.nodes.at(-1)?.nodeName || '',
                  })
                }}
              </div>
            </div>
          </template>
          <template #right-icon>
            <CrmIcon
              :name="
                expandedHistoryInstanceNames.includes(instance.id) ? 'iconicon_chevron_down' : 'iconicon_chevron_right'
              "
              width="16px"
              height="16px"
              color="var(--van-cell-right-icon-color)"
            />
          </template>
          <div class="bg-[var(--text-n10)] p-[8px]">
            <CrmApprovalLine
              :nodes="instance.nodes"
              :submitter="{
                submitAvatar: instance.submitAvatar,
                submitter: instance.submitter,
                submitTime: instance.submitTime,
                submitterId: instance.submitterId,
                comment: instance.comment,
              }"
              :current-approval-node="getCurrentNode(instance)"
              :current-approval-node-index="getCurrentNodeIndex(instance)"
              :finally-result="instance.approvalStatus"
              class="h-[55vh]"
            />
          </div>
        </van-collapse-item>
      </van-collapse>
      <van-steps
        active="none"
        :icon-size="20"
        class="w-full flex-none gap-[4px] !overflow-visible"
        direction="vertical"
      >
        <van-step>
          <template #inactive-icon>
            <div class="timeline-icon-wrapper bg-[var(--primary-8)]">
              <CrmIcon name="iconicon_add" :size="14" color="var(--text-n10)" />
            </div>
          </template>
          <div class="mb-[16px] flex items-center justify-between">
            <div class="font-semibold leading-[22px] text-[var(--text-n1)]">{{ t('crm.approval.submit') }}</div>
            <div class="text-[var(--text-n4)]">
              {{ dayjs(props.submitter.submitTime).format('YYYY-MM-DD HH:mm') }}
            </div>
          </div>
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
          <div v-if="props.submitter.comment" class="mt-[8px] bg-[var(--text-n9)] p-[8px]">
            <div class="break-words text-[var(--text-n4)]">{{ props.submitter.comment }}</div>
          </div>
        </van-step>
        <van-step v-for="(node, index) in props.nodes" :key="node.nodeId">
          <template #inactive-icon>
            <div v-if="node.endNode" class="timeline-icon-wrapper" :class="getIconClass(node)">
              <CrmIcon name="iconicon_end" color="var(--text-n10)" />
            </div>
            <div v-else class="timeline-icon-wrapper" :class="getIconClass(node)">
              <CrmIcon name="iconicon_contract" color="var(--text-n10)" />
            </div>
          </template>
          <van-collapse v-model="expandedNodes">
            <van-collapse-item
              :name="node.nodeId"
              :disabled="!node.taskNodes?.length || node.endNode"
              :is-link="!!node.taskNodes?.length || !node.endNode"
              class="node-collapse-item"
            >
              <template #title>
                <div class="mb-[8px] flex w-full items-center justify-between gap-[8px]">
                  <div class="flex flex-1 items-center gap-[8px] overflow-hidden">
                    <div class="one-line-text font-semibold !leading-[22px] text-[var(--text-n1)]">
                      {{ node.nodeName }}
                    </div>
                    <CrmTag
                      v-if="!node.endNode && node.taskNodes?.length > 1"
                      bgColor="var(--info-blue)"
                      textColor="var(--info-blue)"
                      plain
                      theme="outline"
                      :tag="MultiApproverModeMap[node.multiApproverMode]"
                    />
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
                    <CrmIcon
                      v-if="node.backNode"
                      name="iconicon_info_circle_filled"
                      color="var(--warning-yellow)"
                      width="16px"
                      height="16px"
                      @click.stop="handleBackNodeClick(node)"
                    />
                  </div>
                </div>
              </template>
              <div class="mt-[2px] py-[8px] pl-0">
                <van-collapse v-if="node.taskNodes?.length" v-model="expandedTaskNodes">
                  <van-collapse-item
                    v-for="task in node.taskNodes"
                    :name="task.taskId"
                    class="task-collapse-item"
                    :border="false"
                  >
                    <template #title>
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
                        <div class="one-line-text flex-1">{{ task.approver }}</div>
                        <CrmTag
                          v-if="task.sign"
                          :tag="t('common.COUNTERSIGNATURE')"
                          theme="outline"
                          bgColor="var(--info-blue)"
                          textColor="var(--info-blue)"
                          plain
                          tooltipDisabled
                          @click="handleSignNodeClick(task)"
                        />
                      </div>
                    </template>
                    <template #right-icon>
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
                      class="flex w-full gap-[8px] overflow-hidden bg-[var(--text-n9)] p-[8px]"
                    >
                      <div class="w-full break-words text-[var(--text-n4)]">{{ task.comment }}</div>
                    </div>
                    <CrmFileList
                      v-if="task.attachments?.length > 0"
                      :fileList="task.attachments"
                      class="mt-[8px] px-0 pb-0"
                      readonly
                    />
                  </van-collapse-item>
                </van-collapse>
              </div>
              <CrmFileList v-if="node.attachments?.length > 0" :fileList="node.attachments" class="mt-[8px]" readonly />
              <van-collapse v-if="node.ccNodes?.length" v-model="expandedCCNodes" :border="false">
                <van-collapse-item :title="t('common.copyTo')" name="copyTo" class="!ml-0">
                  <template #title>
                    <div class="flex items-center gap-[8px]">
                      <CrmIcon name="iconicon_send" color="var(--text-n4)" />
                      <div>{{ t('common.copyTo') }}</div>
                    </div>
                  </template>
                  <template #right-icon>
                    <div class="flex items-center gap-[16px]">
                      <div class="text-[var(--text-n4)]">
                        {{ t('crm.approval.copyToTip', { count: node.ccNodes.length }) }}
                      </div>
                      <CrmIcon
                        :name="expandedCCNodes.length > 0 ? 'iconicon_chevron_right' : 'iconicon_chevron_down'"
                        width="16px"
                        height="16px"
                        color="var(--van-cell-right-icon-color)"
                      />
                    </div>
                  </template>
                  <div class="mt-[12px] flex flex-wrap gap-[8px] bg-[var(--text-n9)] p-[8px]">
                    <div v-for="cc in node.ccNodes" :key="cc.ccUserId" class="flex items-center gap-[8px]">
                      <div class="h-[24px] w-[24px]">
                        <CrmAvatar :avatar="cc.ccUserAvatar" :word="cc.ccUserName" :is-user="false" :size="24" />
                      </div>
                      <div class="one-line-text font-normal">{{ cc.ccUserName }}</div>
                    </div>
                  </div>
                </van-collapse-item>
              </van-collapse>
            </van-collapse-item>
          </van-collapse>
        </van-step>
      </van-steps>
      <div v-if="props.resourceId" ref="commentAnchorRef" class="mt-[16px] rounded-[12px] bg-[var(--text-n10)]">
        <CrmComment
          v-model:count="commentCount"
          type="approval"
          :source-id="props.resourceId"
          :can-create="props.commentable"
          :can-reply="props.commentable"
          enable-attachment
          show-time-sort
          embedded
        />
      </div>
    </div>
  </div>
  <van-popup v-model:show="backNodePopupShow" position="bottom" class="rounded-[12px_12px_0_0]" closeable>
    <div class="flex flex-col items-center gap-[8px] px-[16px]">
      <div class="relative p-[16px] text-center">
        <div class="text-[16px] font-semibold">{{ t('crm.approval.fallbackReason') }}</div>
      </div>
      <div class="mr-auto flex items-center gap-[8px]">
        <CrmIcon name="iconicon_info_circle_filled" color="var(--warning-yellow)" width="16px" height="16px" />
        <div>{{ t('crm.approval.fallbackReason') }}</div>
      </div>
      <div class="w-full break-words text-[var(--text-n4)]">{{ activeBackNode.backReason }}</div>
      <div class="min-h-[200px] w-full overflow-y-auto">
        <CrmFileList
          v-if="activeBackNode.backAttachments?.length > 0"
          :fileList="activeBackNode.backAttachments"
          class="mt-[8px] px-0"
          readonly
        />
      </div>
    </div>
  </van-popup>
  <van-popup v-model:show="signNodePopupShow" position="bottom" class="rounded-[12px_12px_0_0]" closeable>
    <div class="flex flex-col items-center gap-[8px] px-[16px]">
      <div class="relative p-[16px] text-center">
        <div class="text-[16px] font-semibold">{{ t('crm.approval.addSign') }}</div>
      </div>
      <div class="mr-auto flex items-center gap-[8px]">
        <CrmIcon name="iconicon_info_circle_filled" color="var(--warning-yellow)" width="16px" height="16px" />
        <div>{{ t('crm.approval.addSign') }}</div>
      </div>
      <div class="w-full break-words text-[var(--text-n4)]">{{ activeSignNode.signComment }}</div>
      <div class="min-h-[200px] w-full overflow-y-auto">
        <CrmFileList
          v-if="activeSignNode.signAttachments?.length > 0"
          :fileList="activeSignNode.signAttachments"
          class="mt-[8px] px-0"
          readonly
        />
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
  import dayjs from 'dayjs';
  import { showFailToast } from 'vant';

  import { MultiApproverModeEnum, ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatBadgeCount } from '@lib/shared/method';
  import type { ApprovalDetail, ApprovalNode } from '@lib/shared/models/system/process';

  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmSegmentedControl from '@/components/pure/crm-segmented-control/index.vue';
  import CrmApprovalStatus from './crm-approval-status.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import CrmFileList from '@/components/business/crm-file-list-pop/fileList.vue';
  import CrmApprovalAvatar from './crm-approval-avatar.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmComment from '@/components/business/crm-comment/index.vue';

  import { getApprovalResourceInstanceDetail } from '@/api/modules';

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
    approvalId?: string;
    commentCount?: number;
    commentable?: boolean;
    resourceId?: string;
  }>();

  const { t } = useI18n();
  const commentCount = ref(props.commentCount || 0);
  const activeAnchor = ref<'record' | 'comment'>('record');
  const scrollContainerRef = ref<HTMLElement>();
  const commentAnchorRef = ref<HTMLElement>();
  const historyInstances = ref<ApprovalDetail[]>([]);
  const expandedHistoryInstanceNames = ref<Array<string | number>>([]);
  const anchorOptions = computed(() => [
    { value: 'record' as const, label: t('crm.approval.record') },
    { value: 'comment' as const, label: `${t('crmComment.title')} ${formatBadgeCount(commentCount.value)}` },
  ]);

  function scrollToAnchor(anchor: 'record' | 'comment') {
    const container = scrollContainerRef.value;
    if (!container) return;
    activeAnchor.value = anchor;
    const target = commentAnchorRef.value;
    const top =
      anchor === 'comment' && target
        ? target.getBoundingClientRect().top - container.getBoundingClientRect().top + container.scrollTop
        : 0;
    container.scrollTo({ top, behavior: 'smooth' });
  }

  watch(
    () => props.approvalId,
    () => {
      activeAnchor.value = 'record';
      scrollContainerRef.value?.scrollTo({ top: 0 });
    }
  );

  const MultiApproverModeMap = {
    [MultiApproverModeEnum.ALL]: t('crm.approval.multiApprovalType.all'),
    [MultiApproverModeEnum.ANY]: t('crm.approval.multiApprovalType.any'),
    [MultiApproverModeEnum.SEQUENTIAL]: t('crm.approval.multiApprovalType.sequential'),
  };

  function getCurrentNodeIndex(detail?: ApprovalDetail) {
    return detail?.nodes.findIndex((node) => node.nodeId === detail.currentNodeId) ?? -1;
  }

  function getCurrentNode(detail?: ApprovalDetail) {
    return detail?.nodes.find((node) => node.nodeId === detail.currentNodeId);
  }

  async function loadHistoryInstances(resourceId?: string) {
    if (!resourceId) {
      historyInstances.value = [];
      return;
    }
    try {
      const instances = await getApprovalResourceInstanceDetail(resourceId);
      if (props.resourceId !== resourceId) return;
      historyInstances.value = instances;
      expandedHistoryInstanceNames.value = [];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      showFailToast(t('crm.approval.historyLoadFailed'));
      historyInstances.value = [];
    }
  }

  watch(
    () => [props.resourceId, props.approvalId],
    () => loadHistoryInstances(props.resourceId),
    { immediate: true }
  );

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

  const backNodePopupShow = ref(false);
  const activeBackNode = ref<any>({});

  function handleBackNodeClick(node: any) {
    activeBackNode.value = node;
    backNodePopupShow.value = true;
  }

  const signNodePopupShow = ref(false);
  const activeSignNode = ref<any>({});

  function handleSignNodeClick(node: any) {
    activeSignNode.value = node;
    signNodePopupShow.value = true;
  }

  const expandedNodes = ref<string[]>([]);
  const expandedTaskNodes = ref<string[]>([]);
  const expandedCCNodes = ref<string[]>([]);

  watch(
    () => props.commentCount,
    (value) => {
      commentCount.value = value || 0;
    }
  );

  watch(
    () => props.nodes,
    (nodes) => {
      for (let i = 0; i < nodes.length; i++) {
        const node = nodes[i];
        if (node.taskNodes?.length && !node.endNode) {
          expandedNodes.value.push(node.nodeId);
          for (let j = 0; j < node.taskNodes.length; j++) {
            const taskNode = node.taskNodes[j];
            if (taskNode.comment || taskNode.attachments?.length) {
              expandedTaskNodes.value.push(taskNode.taskId);
            }
          }
        }
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped>
  .timeline-icon-wrapper {
    @apply flex items-center justify-center;

    width: 20px;
    height: 20px;
    border-radius: 4px;
  }
  :deep(.van-step__line) {
    margin-top: 20px;
    height: calc(100% - 34px);
  }
  .van-step--waiting {
    padding: 8px 8px 8px 4px;
  }
  .van-step--vertical::after {
    border-bottom: 0;
  }
  .van-hairline--top-bottom::after,
  .van-hairline-unset--top-bottom::after {
    border: 0;
  }
  :deep(.node-collapse-item) {
    .van-collapse-item__title,
    .van-collapse-item__content {
      padding: 0;
    }
  }
  :deep(.task-collapse-item) {
    &:not(:last-child) {
      .half-px-border-bottom();
    }
    .van-collapse-item__title,
    .van-collapse-item__content {
      padding: 8px;
    }
  }
  :deep(.van-cell__title) {
    width: 100%;
  }
</style>
