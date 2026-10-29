<template>
  <CrmCard hide-footer no-content-padding>
    <div
      v-if="props.approvalStatus && ![ProcessStatusEnum.PENDING, ProcessStatusEnum.NONE].includes(props.approvalStatus)"
      ref="splitContainerRef"
      class="h-full w-full"
    >
      <CrmSplitPanel
        size="calc(100% - 422px)"
        :max="maxSplitSize"
        :min="0.2"
        default-size="calc(100% - 422px)"
        collapse-side="right"
      >
        <template #1>
          <div class="relative flex h-full w-full bg-[var(--text-n10)] p-[24px_8px_0_24px]">
            <Transition name="approval-left-skeleton">
              <div
                v-if="leftSlotLoading"
                class="absolute inset-0 z-10 bg-[var(--text-n10)] p-[24px_16px_24px_24px]"
                aria-hidden="true"
              >
                <div class="mb-[20px] flex items-center gap-[12px]">
                  <n-skeleton circle :width="36" :height="36" />
                  <div class="flex flex-1 flex-col gap-[8px]">
                    <n-skeleton :width="160" />
                    <n-skeleton :width="96" />
                  </div>
                </div>
                <div class="grid grid-cols-2 gap-x-[24px] gap-y-[28px]">
                  <div v-for="item in 8" :key="item" class="flex flex-col gap-[10px]">
                    <n-skeleton :width="88" />
                    <n-skeleton class="w-full" :height="34" />
                  </div>
                </div>
              </div>
            </Transition>
            <div class="h-full min-h-0 w-full" :class="{ 'opacity-0': leftSlotLoading }">
              <n-scrollbar :content-style="{ paddingRight: '8px' }" x-scrollable>
                <slot
                  name="left"
                  :fieldPermissions="filedPermission"
                  :taskNode="currentTaskNode"
                  :setLoading="setLeftSlotLoading"
                  :setEditing="setLeftEditing"
                ></slot>
              </n-scrollbar>
            </div>
          </div>
        </template>
        <template #2>
          <div v-if="noApproval" class="h-full w-full flex-1 bg-[var(--text-n10)] px-[16px] py-[24px] pb-[32px]">
            <div class="mb-[8px] text-[16px] font-semibold">{{ t('crm.approval.record') }}</div>
            {{ t('crm.approval.historyTip') }}
          </div>
          <div
            v-else
            ref="approvalHistoryPanelRef"
            class="approval-history-panel flex h-full w-full flex-col overflow-hidden bg-[var(--text-n10)]"
          >
            <div class="shrink-0 px-[24px] pt-[24px]">
              <CrmTab
                v-model:active-tab="activeAnchor"
                no-content
                type="segment"
                class="approval-anchor-tab"
                :tab-list="approvalAnchorTabList"
                @change="handleAnchorTabChange"
                @click="handleAnchorTabClick"
              />
            </div>
            <n-scrollbar class="min-h-0 flex-1" :content-style="{ padding: '16px 24px 32px 24px' }">
              <div ref="approvalRecordRef">
                <n-collapse
                  v-if="latestHistoricalApproval"
                  v-model:expanded-names="expandedHistoryInstanceNames"
                  class="mb-[16px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[8px]"
                  @update:expanded-names="handleHistoryExpandedNames"
                >
                  <template #arrow><div></div></template>
                  <n-collapse-item name="history">
                    <template #header>
                      <div class="flex items-center gap-[8px]">
                        <CrmApprovalStatus
                          :status="latestHistoricalApproval.approvalStatus"
                          isTag
                          scene="approvalRecord"
                          class="font-normal"
                        />
                        <div class="text-[14px] font-semibold text-[var(--text-n1)]">
                          <template
                            v-if="
                              [ProcessStatusEnum.APPROVED, ProcessStatusEnum.AUTO_APPROVED].includes(
                                latestHistoricalApproval.approvalStatus
                              )
                            "
                          >
                            {{ formatApprovalTime(getHistoricalApprovalTime(latestHistoricalApproval)) }}
                          </template>
                          <template v-else>
                            {{
                              t('crm.approval.previousRoundTitle', {
                                nodeName: getCurrentNode(latestHistoricalApproval)?.nodeName || '',
                              })
                            }}
                          </template>
                        </div>
                      </div>
                    </template>
                    <template #header-extra="{ collapsed }">
                      <CrmIcon :type="collapsed ? 'iconicon_chevron_right' : 'iconicon_chevron_down'" :size="16" />
                    </template>
                    <div
                      v-for="(detail, index) in historicalApprovalDetails"
                      :key="detail.id"
                      class="mb-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n10)] p-[8px] last:mb-0"
                    >
                      <CrmApprovalLine
                        :nodes="detail.nodes || []"
                        :submitter="{
                          submitAvatar: detail.submitAvatar,
                          submitter: detail.submitter,
                          submitTime: detail.submitTime,
                          submitterId: detail.submitterId,
                          comment: detail.comment,
                        }"
                        :currentApprovalNode="getCurrentNode(detail)"
                        :currentApprovalNodeIndex="getCurrentNodeIndex(detail)"
                        :finally-result="detail.approvalStatus"
                        :isLatest="index === historicalApprovalDetails.length - 1"
                        is-historical
                      />
                    </div>
                  </n-collapse-item>
                </n-collapse>
                <CrmApprovalLine
                  :nodes="approvalInfo?.nodes || []"
                  :submitter="{
                    submitAvatar: approvalInfo?.submitAvatar,
                    submitter: approvalInfo?.submitter,
                    submitTime: approvalInfo?.submitTime,
                    submitterId: approvalInfo?.submitterId,
                    comment: approvalInfo?.comment,
                  }"
                  :currentApprovalNode="currentApprovalNode"
                  :currentApprovalNodeIndex="currentApprovalNodeIndex"
                  :current-round-start-index="currentRoundStartIndex"
                  :finally-result="approvalInfo?.approvalStatus"
                  class="pr-[8px]"
                />
              </div>
              <div ref="approvalCommentRef">
                <CrmComment
                  v-if="approvalInfo?.id"
                  v-model:expanded="commentExpanded"
                  v-model:count="approvalInfo.commentCount"
                  type="approval"
                  :can-create="approvalInfo.commentable"
                  :can-reply="approvalInfo.commentable"
                  :collapsible="false"
                  actions-in-more
                  enable-attachment
                  show-time-sort
                  :source-id="props.sourceId"
                  class="mt-[24px]"
                />
              </div>
            </n-scrollbar>
            <div
              v-if="
                !leftSlotEditing &&
                approvalInfo?.approvalStatus === ProcessStatusEnum.APPROVING &&
                (isApprover || canCancelApply || canCancelApproval)
              "
              class="sticky bottom-0 flex max-h-full flex-col border-t border-[var(--text-n8)] bg-[var(--text-n10)] p-[16px]"
            >
              <div v-if="isApprover" class="flex shrink-0 justify-between gap-[12px]">
                <div v-if="isApprover">
                  <n-button type="primary" :loading="approvalLoading" @click="openApprovalOpinion('approve')">
                    {{ t('common.approve') }}
                  </n-button>
                  <n-button type="error" class="ml-[8px]" ghost @click="openApprovalOpinion('reject')">{{
                    t('common.reject')
                  }}</n-button>
                </div>
                <CrmMoreAction
                  v-if="moreActions.length"
                  :class="!isApprover ? 'ml-auto' : ''"
                  :options="moreActions"
                  trigger="click"
                  size="medium"
                  @select="handleMoreActionSelect"
                />
              </div>
              <div v-if="!isApprover" class="flex gap-[8px]">
                <n-button v-if="canCancelApply" type="primary" ghost class="flex-1" @click="cancelApproval('apply')">
                  <template #icon>
                    <CrmIcon type="iconicon_rollfront" :size="16" />
                  </template>
                  {{ t('common.revoke') }}
                </n-button>
                <n-button v-if="canCancelApproval" type="primary" ghost class="flex-1" @click="cancelApproval()">
                  <template #icon>
                    <CrmIcon type="iconicon_rollfront" :size="16" />
                  </template>
                  {{ t('crm.approval.cancelApproval') }}
                </n-button>
              </div>
            </div>
          </div>
        </template>
      </CrmSplitPanel>
    </div>
    <div v-else class="flex h-full w-full p-[24px_16px_24px_24px]">
      <n-scrollbar x-scrollable>
        <slot
          name="left"
          :fieldPermissions="[]"
          :taskNode="currentTaskNode"
          :setLoading="setLeftSlotLoading"
          :setEditing="setLeftEditing"
        ></slot>
      </n-scrollbar>
    </div>
  </CrmCard>
  <ApprovalOpinionModal
    v-model:show="approvalOpinionModalVisible"
    v-model:opinion="approvalOpinion"
    v-model:file-list="fileList"
    :title="t('crm.approval.suggestion')"
    :approval-type="approvalAction"
    :loading="approvalLoading"
    :required="isCommentRequired"
    @confirm="submitApprovalOpinion"
  />
  <CrmModal
    v-model:show="addSignModalVisible"
    :title="t('common.COUNTERSIGNATURE')"
    :ok-loading="addSignLoading"
    :positive-text="addSignForm.type === 'AFTER' ? t('crm.approval.agreeAndAddSign') : t('crm.approval.confirmAddSign')"
    @confirm="handleAddSign"
  >
    <n-form
      ref="addSignFormRef"
      :model="addSignForm"
      label-placement="left"
      label-width="auto"
      require-mark-placement="right"
    >
      <n-form-item path="type" :label="t('crm.approval.addSignMethod')">
        <n-radio-group v-model:value="addSignForm.type" name="radiogroup">
          <n-radio key="BEFORE" value="BEFORE">
            {{ t('crm.approval.beforeMethod') }}
          </n-radio>
          <n-radio key="AFTER" value="AFTER">
            {{ t('crm.approval.afterMethod') }}
          </n-radio>
        </n-radio-group>
      </n-form-item>
      <n-form-item
        path="reviewer"
        :label="t('crm.approval.addSignApprover')"
        :rule="[
          {
            required: true,
            type: 'array',
            message: t('common.notNull', {
              value: t('crm.approval.addSignApprover'),
            }),
            trigger: 'change',
          },
        ]"
      >
        <CrmMemberSelect
          v-model:value="addSignForm.reviewer"
          :multiple="false"
          :apiTypeKey="MemberApiTypeEnum.FORM_FIELD"
          :member-types="[
            {
              label: t('menu.settings.org'),
              value: MemberSelectTypeEnum.ORG,
            },
          ]"
        />
      </n-form-item>
      <n-form-item path="reason" :label="t('crm.approval.addSignOpinion')">
        <CrmFileInput v-model:value="addSignForm.reason" v-model:file-list="addSignForm.fileList" />
      </n-form-item>
    </n-form>
  </CrmModal>
  <CrmModal
    v-model:show="fallbackModalVisible"
    :title="t('taskDrawer.operation.BACK')"
    :ok-loading="fallbackLoading"
    :positive-text="t('crm.approval.confirmFallback')"
    @confirm="handleFallback"
  >
    <n-form
      ref="fallbackFormRef"
      :model="fallbackForm"
      label-placement="left"
      label-width="auto"
      require-mark-placement="right"
    >
      <n-form-item
        path="node"
        :label="t('crm.approval.fallbackTo')"
        :rule="[
          {
            required: true,
            message: t('common.notNull', {
              value: t('crm.approval.fallbackTo'),
            }),
            trigger: 'change',
          },
        ]"
      >
        <n-select v-model:value="fallbackForm.node" :options="fallbackOptions" clearable />
      </n-form-item>
      <n-form-item path="reason" :label="t('crm.approval.fallbackReason')">
        <CrmFileInput v-model:value="fallbackForm.reason" v-model:file-list="fallbackForm.fileList" />
      </n-form-item>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { nextTick, onBeforeUnmount, onMounted } from 'vue';
  import {
    type FormInst,
    NButton,
    NCollapse,
    NCollapseItem,
    NForm,
    NFormItem,
    NRadio,
    NRadioGroup,
    NScrollbar,
    NSelect,
    NSkeleton,
    NSpin,
    type UploadFileInfo,
    useMessage,
  } from 'naive-ui';
  import dayjs from 'dayjs';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { ApprovalTaskExecuteTimeEnum, MultiApproverModeEnum, ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatBadgeCount } from '@lib/shared/method';
  import type { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig } from '@lib/shared/models/system/module';
  import type {
    ApprovalDetail,
    ApprovalFieldPermission,
    ApprovalNode,
    ApprovalNodeTaskNode,
    ApprovalProcessDetail,
  } from '@lib/shared/models/system/process';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmComment from '@/components/business/crm-comment/index.vue';
  import CrmFileInput from '@/components/business/crm-file-input/index.vue';
  import CrmMemberSelect from '@/components/business/crm-user-tag-selector/index.vue';
  import ApprovalOpinionModal from './approvalOpinionModal.vue';
  import CrmApprovalLine from './crm-approval-line.vue';
  import CrmApprovalStatus from './crm-approval-status.vue';

  import {
    addSignApproval,
    agreeApproval,
    backApproval,
    getApprovalConfigDetail,
    getApprovalResourceDetail,
    getApprovalResourceInstanceDetailList,
    rejectApproval,
    revokeApproval,
    revokeResource,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useUserStore from '@/store/modules/user';

  import type { SelectMixedOption } from 'naive-ui/es/select/src/interface';

  const props = defineProps<{
    sourceId: string;
    formKey: FormDesignKeyEnum | string;
    refreshKey?: number;
    approvalTaskId?: string;
    commentMentionId?: string;
    layout?: 'horizontal' | 'vertical';
    approvalStatus: ProcessStatusEnum;
    leftLoading?: boolean;
  }>();
  const splitContainerRef = ref<HTMLElement>();
  const maxSplitSize = ref<number | string>(0.63);
  let splitResizeObserver: ResizeObserver | undefined;

  onMounted(() => {
    if (!splitContainerRef.value) return;
    splitResizeObserver = new ResizeObserver(([entry]) => {
      maxSplitSize.value = `${Math.max(0, entry.contentRect.width - 422)}px`;
    });
    splitResizeObserver.observe(splitContainerRef.value);
  });

  onBeforeUnmount(() => splitResizeObserver?.disconnect());

  defineSlots<{
    left(props: {
      fieldPermissions: ApprovalFieldPermission[];
      taskNode?: ApprovalNodeTaskNode;
      setLoading: (loading: boolean) => void;
      setEditing: (editing: boolean) => void;
    }): any;
  }>();
  const approvalStandardFormKeys = new Set<string>([
    FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
    FormDesignKeyEnum.CONTRACT,
    FormDesignKeyEnum.ORDER,
    FormDesignKeyEnum.INVOICE,
  ]);
  const emit = defineEmits<{
    (
      e: 'descriptionInit',
      collaborationType?: CollaborationType,
      sourceName?: string,
      detail?: Record<string, any>,
      config?: FormConfig
    ): void;
    (e: 'saveApproval', callback: () => Promise<any>, hasFieldPermission: boolean): void;
    (e: 'deleteApproved'): void;
    (e: 'refresh'): void;
  }>();

  const { t } = useI18n();
  const { openModal } = useModal();
  const message = useMessage();
  const userStore = useUserStore();

  const approvalInfo = ref<ApprovalDetail>();
  const historicalApprovalDetails = ref<ApprovalDetail[]>([]);
  const expandedHistoryInstanceNames = ref<Array<string | number>>([]);
  const latestHistoricalApproval = computed(() => historicalApprovalDetails.value.at(-1));

  const approvalConfig = ref<ApprovalProcessDetail>(); // 审批配置详情
  const leftSlotLoading = ref(props.leftLoading ?? false);
  const leftSlotEditing = ref(false);

  function setLeftSlotLoading(loading: boolean) {
    leftSlotLoading.value = loading;
  }

  function setLeftEditing(editing: boolean) {
    leftSlotEditing.value = editing;
  }

  const activeAnchor = ref<'record' | 'comment'>('record');
  const approvalAnchorTabList = computed(() => [
    { name: 'record', tab: t('crm.approval.record') },
    { name: 'comment', tab: `${t('crmComment.title')} ${formatBadgeCount(approvalInfo.value?.commentCount || 0)}` },
  ]);
  const approvalHistoryPanelRef = ref<HTMLElement>();
  const approvalRecordRef = ref<HTMLElement>();
  const approvalCommentRef = ref<HTMLElement>();

  function scrollToAnchor(anchor: 'record' | 'comment') {
    activeAnchor.value = anchor;
    const target = anchor === 'record' ? approvalRecordRef.value : approvalCommentRef.value;
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  function focusCurrentRoundStart() {
    expandedHistoryInstanceNames.value = [];
    nextTick(() => {
      const target = approvalRecordRef.value?.querySelector('.approval-current-round-start-node');
      (target || approvalRecordRef.value)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  }

  function handleAnchorTabChange(anchor: string | number) {
    if (anchor === 'record' || anchor === 'comment') {
      if (anchor === 'record') {
        focusCurrentRoundStart();
      } else {
        scrollToAnchor(anchor);
      }
    }
  }

  function handleAnchorTabClick(event: MouseEvent) {
    if (
      activeAnchor.value === 'record' &&
      event.target instanceof Element &&
      event.target.closest('[data-name="record"]')
    ) {
      focusCurrentRoundStart();
    }
  }

  const commentExpanded = ref(true);

  async function initApprovalConfig() {
    try {
      if (props.formKey) {
        approvalConfig.value = await getApprovalConfigDetail(props.formKey);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const approvalOpinion = ref('');
  const fileList = ref<UploadFileInfo[]>([]);
  const approvalOpinionModalVisible = ref(false);
  const approvalAction = ref<'approve' | 'reject'>('approve');
  const isCommentRequired = computed(() =>
    approvalAction.value === 'approve'
      ? approvalConfig.value?.requireApproveComment
      : approvalConfig.value?.requireRejectComment
  );
  const currentApprovalNode = ref<ApprovalNode>();
  const currentApprovalNodeIndex = ref(0);
  const currentRoundStartIndex = computed(() => {
    if (!currentApprovalNode.value || !approvalInfo.value) return -1;
    return approvalInfo.value.nodes.findIndex((node) => node.nodeRound === currentApprovalNode.value?.nodeRound);
  });
  const hasApprovalStatus = [ProcessStatusEnum.APPROVED, ProcessStatusEnum.UNAPPROVED, ProcessStatusEnum.NONE];
  // 我审批的上一个节点
  const prevMineApprovalNode = computed(() => {
    if (!currentApprovalNode.value) {
      return undefined;
    }
    const prevMineApprovalIndex = currentApprovalNode.value.taskNodes?.findIndex(
      (e) => hasApprovalStatus.includes(e.approvalStatus) && e.approverId === userStore.userInfo.id
    );
    if (prevMineApprovalIndex !== -1) {
      return currentApprovalNode.value?.taskNodes?.[prevMineApprovalIndex];
    }
    const prevApprovalNode = approvalInfo.value?.nodes[currentApprovalNodeIndex.value - 1];
    if (prevApprovalNode) {
      return prevApprovalNode.taskNodes.find(
        (e) => hasApprovalStatus.includes(e.approvalStatus) && e.approverId === userStore.userInfo.id
      );
    }
  });
  const currentTaskNode = computed(() => {
    if (!currentApprovalNode.value) {
      return undefined;
    }
    return currentApprovalNode.value.taskNodes?.find(
      (e) => e.approvalStatus === ProcessStatusEnum.APPROVING && e.approverId === userStore.userInfo.id
    );
  });
  // 只有当前审批中的人才展示编辑权限，其他节点展示只读权限
  const filedPermission = computed(() => {
    if (currentTaskNode.value && approvalInfo.value?.currentNodeFieldPermissions) {
      return (JSON.parse(approvalInfo.value?.currentNodeFieldPermissions || '[]') as ApprovalFieldPermission[]) || [];
    }
    return [];
  });

  // 是否是审批人
  const isApprover = computed(() => {
    if (currentApprovalNode.value?.multiApproverMode === MultiApproverModeEnum.SEQUENTIAL) {
      // 顺序审批，只有当前审批人可以操作
      return (
        currentApprovalNode.value?.taskNodes?.find((e) => e.approvalStatus === ProcessStatusEnum.APPROVING)
          ?.approverId === userStore.userInfo.id
      );
    }
    return currentApprovalNode.value?.taskNodes?.some(
      (taskNode) =>
        taskNode.approvalStatus === ProcessStatusEnum.APPROVING && taskNode.approverId === userStore.userInfo.id
    ); // 会签/或签/单人审批
  });
  // 是否可以撤销审批申请
  const canCancelApply = computed(() => {
    // 未配置撤销申请
    if (
      !approvalConfig.value?.submitterCanRevoke &&
      approvalInfo.value?.nodes[0].approvalStatus !== ProcessStatusEnum.APPROVING
    ) {
      return false;
    }
    // 流程结束不允许撤销
    if (currentApprovalNode.value?.endNode) {
      return false;
    }
    // 只有提交人可以撤销审批申请，已撤销状态不显示撤销按钮
    if (
      approvalInfo.value?.submitterId !== userStore.userInfo.id ||
      props.approvalStatus === ProcessStatusEnum.REVOKED
    ) {
      return false;
    }
    // 当前没有审批完成节点时可撤销
    if (
      !approvalInfo.value?.nodes.some(
        (node) =>
          node.approvalStatus === ProcessStatusEnum.APPROVED || node.approvalStatus === ProcessStatusEnum.UNAPPROVED
      )
    ) {
      return true;
    }
    // 第一个节点完成审批，且配置了可撤销配置，允许撤销
    if (
      approvalInfo.value?.nodes[0].approvalStatus === ProcessStatusEnum.APPROVED &&
      approvalConfig.value?.submitterCanRevoke
    ) {
      return true;
    }
  });
  // 是否可以撤销审批
  const canCancelApproval = computed(() => {
    // 未配置撤销
    if (!approvalConfig.value?.allowWithdraw) {
      return false;
    }
    // 流程结束不允许撤销
    if (currentApprovalNode.value?.endNode) {
      return false;
    }
    // 当前节点包含自己审批的任务节点，则说明是多人审批节点且节点未结束，判断当前节点情况即可
    if (
      currentApprovalNode.value &&
      currentApprovalNode.value.taskNodes?.findIndex(
        (e) => hasApprovalStatus.includes(e.approvalStatus) && e.approverId === userStore.userInfo.id
      ) !== -1
    ) {
      // 当前是顺序审批节点，上一个审批节点是自己，并且下一个节点未审批，允许撤销自己的审批
      if (currentApprovalNode.value?.multiApproverMode === MultiApproverModeEnum.SEQUENTIAL) {
        const currentTaskNodeIndex = currentApprovalNode.value?.taskNodes?.findIndex(
          (e) => e.approvalStatus === ProcessStatusEnum.APPROVING
        );
        return currentApprovalNode.value?.taskNodes[currentTaskNodeIndex - 1]?.approverId === userStore.userInfo.id;
      }
      // 当前是会签节点，且自己已经审批时允许撤回
      if (
        currentApprovalNode.value.taskNodes?.findIndex(
          (e) => hasApprovalStatus.includes(e.approvalStatus) && e.approverId === userStore.userInfo.id
        ) !== -1
      ) {
        return true;
      }
    }
    // 当前节点未包含自己审批的任务节点，则判断上一个节点情况
    // 上一个节点非多人审批，是自己审批的，且当前节点下所有人都未审批
    if (approvalInfo.value?.nodes[currentApprovalNodeIndex.value - 1]?.taskNodes?.length === 1) {
      return (
        approvalInfo.value?.nodes[currentApprovalNodeIndex.value - 1].taskNodes[0].approverId ===
          userStore.userInfo.id &&
        currentApprovalNode.value?.taskNodes?.every((e) =>
          [ProcessStatusEnum.APPROVING, ProcessStatusEnum.PENDING].includes(e.approvalStatus)
        )
      );
    }
    // 上一个节点是多人审批且是或签，且是自己审批通过的，且当前节点下所有人都未审批
    if (
      approvalInfo.value?.nodes[currentApprovalNodeIndex.value - 1]?.multiApproverMode === MultiApproverModeEnum.ANY
    ) {
      return (
        approvalInfo.value?.nodes[currentApprovalNodeIndex.value - 1].taskNodes.some(
          (e) => hasApprovalStatus.includes(e.approvalStatus) && e.approverId === userStore.userInfo.id
        ) &&
        currentApprovalNode.value?.taskNodes?.every((e) =>
          [ProcessStatusEnum.APPROVING, ProcessStatusEnum.PENDING].includes(e.approvalStatus)
        )
      );
    }
  });
  const noApproval = ref(false);

  function getCurrentNodeIndex(detail?: ApprovalDetail) {
    return detail?.nodes.findIndex((node) => node.nodeId === detail.currentNodeId) ?? -1;
  }

  function getCurrentNode(detail?: ApprovalDetail) {
    return detail?.nodes.find((node) => node.nodeId === detail.currentNodeId);
  }

  function getHistoricalApprovalTime(detail?: ApprovalDetail) {
    const approvalTimes = detail?.nodes.flatMap((node) => [
      node.approvalTime,
      ...(node.taskNodes || []).map((task) => task.approvalTime),
    ]);
    return approvalTimes?.filter((time): time is number => Number.isFinite(time) && time > 0).sort((a, b) => b - a)[0];
  }

  function formatApprovalTime(time?: number) {
    return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-';
  }

  function scrollToLatestHistoricalApproval() {
    const target = approvalHistoryPanelRef.value?.querySelector('.approval-history-latest-node');
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  async function handleHistoryExpandedNames(names: Array<string | number>) {
    expandedHistoryInstanceNames.value = names;
    if (!names.includes('history')) return;
    setTimeout(() => {
      scrollToLatestHistoricalApproval();
    }, 300);
  }

  async function initApprovalDetail() {
    try {
      approvalInfo.value = await getApprovalResourceDetail(props.sourceId, props.commentMentionId);
      if (!approvalInfo.value) {
        noApproval.value = true;
      }
      currentApprovalNodeIndex.value = approvalInfo.value?.nodes.findIndex(
        (node) => node.nodeId === approvalInfo.value?.currentNodeId
      );
      currentApprovalNode.value = approvalInfo.value?.nodes[currentApprovalNodeIndex.value];
      historicalApprovalDetails.value = await getApprovalResourceInstanceDetailList(
        props.sourceId,
        props.commentMentionId
      );
      await nextTick();
      scrollToLatestHistoricalApproval();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      historicalApprovalDetails.value = [];
    }
  }

  const approvalLoading = ref(false);
  async function approvalAgree() {
    if (!currentTaskNode.value || !currentApprovalNode.value) {
      return;
    }
    try {
      approvalLoading.value = true;
      await agreeApproval({
        id: currentTaskNode.value.taskId,
        nodeId: currentApprovalNode.value.nodeId,
        instanceId: approvalConfig.value?.id || '',
        attachmentIds: fileList.value.map((e) => e.id),
        approverId: currentTaskNode.value.approverId,
        comment: approvalOpinion.value,
      });
      message.success(t('common.approved'));
      approvalOpinionModalVisible.value = false;
      if (approvalInfo.value?.executeTime === ApprovalTaskExecuteTimeEnum.DELETE) {
        emit('deleteApproved');
        return;
      }
      if (!props.approvalTaskId) {
        initApprovalDetail();
        emit('refresh');
      }
      approvalOpinion.value = '';
      fileList.value = [];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      approvalLoading.value = false;
    }
  }

  function openApprovalOpinion(action: 'approve' | 'reject') {
    approvalAction.value = action;
    approvalOpinion.value = '';
    fileList.value = [];
    approvalOpinionModalVisible.value = true;
  }

  async function handleApprove() {
    if (!currentTaskNode.value || !currentApprovalNode.value) {
      return;
    }
    emit(
      'saveApproval',
      approvalAgree,
      filedPermission.value.some((e) => e.permissionType === 'EDIT')
    );
  }

  function handleReject() {
    if (!currentApprovalNode.value || !currentTaskNode.value) {
      return;
    }
    emit(
      'saveApproval',
      async () => {
        if (!currentApprovalNode.value || !currentTaskNode.value) {
          return;
        }
        try {
          approvalLoading.value = true;
          await rejectApproval({
            id: currentTaskNode.value.taskId,
            nodeId: currentApprovalNode.value.nodeId,
            instanceId: approvalConfig.value?.id || '',
            attachmentIds: fileList.value.map((e) => e.id),
            approverId: currentTaskNode.value.approverId,
            comment: approvalOpinion.value,
          });
          message.success(t('common.rejected'));
          approvalOpinionModalVisible.value = false;
          if (!props.approvalTaskId) {
            initApprovalDetail();
            emit('refresh');
          }
          approvalOpinion.value = '';
          fileList.value = [];
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        } finally {
          approvalLoading.value = false;
        }
      },
      filedPermission.value.some((e) => e.permissionType === 'EDIT')
    );
  }

  function submitApprovalOpinion() {
    if (approvalAction.value === 'approve') {
      handleApprove();
      return;
    }
    handleReject();
  }

  const addSignModalVisible = ref(false);
  const addSignLoading = ref(false);
  const addSignForm = ref({
    type: 'BEFORE',
    reviewer: undefined,
    reason: '',
    fileList: [] as UploadFileInfo[],
  });
  const addSignFormRef = ref<FormInst>();

  function resetAddSignForm() {
    addSignForm.value = {
      type: 'BEFORE',
      reviewer: undefined,
      reason: '',
      fileList: [],
    };
    addSignFormRef.value?.restoreValidation();
  }

  watch(addSignModalVisible, (visible) => {
    if (!visible) {
      resetAddSignForm();
    }
  });

  function handleAddSign() {
    addSignFormRef.value?.validate(async (errors) => {
      if (!errors && currentApprovalNode.value && currentTaskNode.value) {
        emit(
          'saveApproval',
          async () => {
            if (!errors && currentApprovalNode.value && currentTaskNode.value) {
              try {
                addSignLoading.value = true;
                await addSignApproval({
                  id: currentTaskNode.value.taskId,
                  nodeId: currentApprovalNode.value.nodeId,
                  instanceId: approvalInfo.value?.id || '',
                  approverId: currentTaskNode.value?.approverId || '',
                  customFormId: approvalStandardFormKeys.has(props.formKey) ? undefined : props.formKey,
                  comment: addSignForm.value.reason,
                  attachmentIds: addSignForm.value.fileList.map((e) => e.id),
                  type: addSignForm.value.type,
                  signApprover: addSignForm.value.reviewer?.[0] || '',
                });
                addSignModalVisible.value = false;
                message.success(t('crm.approval.addSignSuccess'));
                initApprovalDetail();
                emit('refresh');
              } catch (error) {
                // eslint-disable-next-line no-console
                console.log(error);
              } finally {
                addSignLoading.value = false;
              }
            }
          },
          filedPermission.value.some((e) => e.permissionType === 'EDIT')
        );
      }
    });
  }

  const fallbackModalVisible = ref(false);
  const fallbackLoading = ref(false);
  const fallbackForm = ref({
    node: undefined,
    reason: '',
    fileList: [] as UploadFileInfo[],
  });
  const fallbackOptions = computed(() => {
    const options: SelectMixedOption[] = [];
    for (let i = currentApprovalNodeIndex.value - 1; i >= 0; i--) {
      options.push({
        label: t('crm.approval.preNode', { index: currentApprovalNodeIndex.value - i }),
        value: approvalInfo.value?.nodes[i].nodeId || '',
      });
    }
    return options;
  });
  const fallbackFormRef = ref<FormInst>();

  function resetFallbackForm() {
    fallbackForm.value = {
      node: undefined,
      reason: '',
      fileList: [],
    };
    fallbackFormRef.value?.restoreValidation();
  }

  watch(fallbackModalVisible, (visible) => {
    if (!visible) {
      resetFallbackForm();
    }
  });

  function handleFallback() {
    fallbackFormRef.value?.validate(async (errors) => {
      if (!errors && currentApprovalNode.value && currentTaskNode.value) {
        emit(
          'saveApproval',
          async () => {
            if (!errors && currentApprovalNode.value && currentTaskNode.value) {
              try {
                fallbackLoading.value = true;
                const returnToNode = approvalInfo.value?.nodes.find((node) => node.nodeId === fallbackForm.value.node);
                await backApproval({
                  id: currentTaskNode.value.taskId,
                  nodeId: currentApprovalNode.value.nodeId,
                  instanceId: approvalInfo.value?.id || '',
                  approverId: currentTaskNode.value?.approverId || '',
                  comment: fallbackForm.value.reason,
                  attachmentIds: fallbackForm.value.fileList.map((e) => e.id),
                  returnToNodeId: fallbackForm.value.node || '',
                  returnToTaskId: returnToNode?.nodeId.includes('-SN') ? returnToNode.taskNodes[0]?.taskId : undefined,
                });
                fallbackModalVisible.value = false;
                message.success(t('crm.approval.fallbackSuccess'));
                initApprovalDetail();
                emit('refresh');
              } catch (error) {
                // eslint-disable-next-line no-console
                console.log(error);
              } finally {
                fallbackLoading.value = false;
              }
            }
          },
          filedPermission.value.some((e) => e.permissionType === 'EDIT')
        );
      }
    });
  }

  const moreActions = computed(() => {
    const fullActions: ActionsItem[] = [];
    if (isApprover.value && approvalConfig.value?.allowAddSign) {
      fullActions.push({
        key: 'addSign',
        label: t('common.COUNTERSIGNATURE'),
      });
    }
    if (isApprover.value && fallbackOptions.value.length) {
      fullActions.push({
        key: 'fallback',
        label: t('taskDrawer.operation.BACK'),
      });
    }
    if (canCancelApply.value) {
      fullActions.push({
        key: 'cancelApply',
        label: t('common.revoke'),
      });
    }
    if (canCancelApproval.value) {
      fullActions.push({
        key: 'cancelApproval',
        label: t('crm.approval.cancelApproval'),
      });
    }
    return fullActions;
  });

  function cancelApproval(type?: 'apply' | 'approval') {
    if (type === 'apply') {
      openModal({
        title: t('crm.approval.cancelApprovalApplyConfirm'),
        content: t('crm.approval.cancelApprovalApplyTip'),
        type: 'error',
        positiveText: t('crm.approval.confirmCancelApprovalApply'),
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          emit(
            'saveApproval',
            async () => {
              await revokeResource({
                resourceId: props.sourceId,
                formKey: props.formKey,
              });
              message.success(t('common.revokeSuccess'));
              initApprovalDetail();
              emit('refresh');
            },
            filedPermission.value.some((e) => e.permissionType === 'EDIT')
          );
        },
      });
    } else {
      openModal({
        title: t('crm.approval.cancelApprovalConfirm'),
        content: t('crm.approval.cancelApprovalTip'),
        type: 'error',
        positiveText: t('crm.approval.confirmCancelApproval'),
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          emit(
            'saveApproval',
            async () => {
              await revokeApproval({
                id: prevMineApprovalNode.value?.taskId || '',
              });
              message.success(t('crm.approval.cancelApprovalSuccess'));
              initApprovalDetail();
              emit('refresh');
            },
            filedPermission.value.some((e) => e.permissionType === 'EDIT')
          );
        },
      });
    }
  }

  function handleMoreActionSelect(item: ActionsItem) {
    if (item.key === 'addSign') {
      // 添加会签
      addSignModalVisible.value = true;
    } else if (item.key === 'fallback') {
      // 退回
      fallbackModalVisible.value = true;
    } else if (item.key === 'cancelApply') {
      cancelApproval('apply');
    } else if (item.key === 'cancelApproval') {
      cancelApproval();
    }
  }

  watch(
    () => props.approvalStatus,
    () => {
      if (props.approvalStatus && ![ProcessStatusEnum.PENDING, ProcessStatusEnum.NONE].includes(props.approvalStatus)) {
        initApprovalDetail();
        initApprovalConfig();
      }
    },
    {
      immediate: true,
    }
  );

  watch(
    () => props.refreshKey,
    () => {
      if (props.approvalStatus && ![ProcessStatusEnum.PENDING, ProcessStatusEnum.NONE].includes(props.approvalStatus)) {
        initApprovalDetail();
      }
    }
  );
</script>

<style lang="less" scoped>
  .approval-left-skeleton-enter-active,
  .approval-left-skeleton-leave-active {
    transition: opacity 180ms ease;
  }
  .approval-left-skeleton-enter-from,
  .approval-left-skeleton-leave-to {
    opacity: 0;
  }
  .approval-anchor-tab {
    :deep(.n-tabs-rail) {
      width: 100%;
    }
  }
  :deep(.n-split__resize-trigger-wrapper) {
    transform: translateX(8px);
    .n-split__resize-trigger {
      border: none;
      background-color: transparent;
      &:hover,
      &.n-split__resize-trigger--hover {
        background-color: var(--primary-8);
      }
    }
  }
  :deep(.n-split--horizontal) {
    gap: 8px;
    background-color: var(--text-n9);
  }
  .approval-history-panel {
    container-type: size;
  }
</style>
