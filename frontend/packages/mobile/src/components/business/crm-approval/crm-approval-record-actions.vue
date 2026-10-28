<template>
  <slot v-if="props.hideActions" :actions="approvalActions" :handle-select="handleApprovalAction" />
  <div v-else-if="showActions" class="flex justify-between gap-[16px]">
    <van-button v-if="directMoreAction" plain type="primary" block @click="directMoreAction.action()">
      {{ directMoreAction.text }}
    </van-button>
    <van-button v-else-if="showMoreButton" plain type="primary" block @click="showMore = true">
      {{ t('common.more') }}
    </van-button>
    <van-button v-if="actionPermission.isApprover" plain type="danger" block @click="openApprovalPopup(true)">
      {{ t('workbench.operation.REJECT') }}
    </van-button>
    <van-button v-if="actionPermission.isApprover" type="primary" block @click="openApprovalPopup(false)">
      {{ t('workbench.operation.APPROVE') }}
    </van-button>
  </div>
  <ApprovalPopup
    v-model:show="showApprovalPopup"
    :approving-item="approvingItem"
    :is-rejecting="isRejecting"
    :resource-type="props.formKey"
    @refresh="handleApprovalSuccess"
  />
  <FallbackPopup
    v-model:show="showFallbackPopup"
    :approving-item="approvingItem"
    :is-rejecting="isRejecting"
    :resource-type="props.formKey"
    :approval-config="effectiveApprovalConfig"
    :fallbackOptions="fallbackOptions"
    @refresh="refresh"
  />
  <AddSignPopup
    v-model:show="showAddSignPopup"
    :approving-item="approvingItem"
    :is-rejecting="isRejecting"
    :resource-type="props.formKey"
    :approval-config="effectiveApprovalConfig"
    :fallbackOptions="fallbackOptions"
    @refresh="refresh"
  />
  <van-popup v-model:show="showMore" position="bottom">
    <template v-for="(action, index) in moreActionList" :key="action.key">
      <van-button class="rounded-none border-none" block @click="action.action">
        {{ action.text }}
      </van-button>
      <van-divider v-if="index < moreActionList.length - 1" class="!m-0" />
    </template>
  </van-popup>
</template>

<script setup lang="ts">
  import { showConfirmDialog, showSuccessToast, type PickerOption } from 'vant';

  import { getApprovalConfigDetail, revokeApproval, revokeResource } from '@/api/modules';
  import useUserStore from '@/store/modules/user';
  import { ApprovalTaskExecuteTimeEnum, ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { sleep } from '@lib/shared/method';
  import type {
    ApprovalDetail,
    ApprovalNode,
    ApprovalProcessDetail,
    ApprovalTodoItem,
  } from '@lib/shared/models/system/process';

  import AddSignPopup from '@/views/workbench/approval/addSignPopup.vue';
  import ApprovalPopup from '@/views/workbench/approval/approvalPopup.vue';
  import FallbackPopup from '@/views/workbench/approval/fallbackPopup.vue';

  import resolveApprovalActionPermission from './approvalActionPermission';

  const props = defineProps<{
    sourceId: string;
    formKey: string;
    approvalInfo?: ApprovalDetail;
    approvalConfig?: ApprovalProcessDetail;
    currentApprovalNode?: ApprovalNode;
    currentApprovalNodeIndex: number;
    hideActions?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'approvalSuccess'): void;
    (e: 'deleteApproved'): void;
    (e: 'cancelSuccess'): void;
  }>();

  const { t } = useI18n();
  const userStore = useUserStore();

  const innerApprovalConfig = ref<ApprovalProcessDetail>();
  const effectiveApprovalConfig = computed(() => props.approvalConfig || innerApprovalConfig.value);
  const showApprovalPopup = ref(false);
  const isRejecting = ref(false);
  const showMore = ref(false);
  const showFallbackPopup = ref(false);
  const showAddSignPopup = ref(false);

  const currentTaskNode = computed(() => {
    if (!props.currentApprovalNode) {
      return undefined;
    }
    return props.currentApprovalNode.taskNodes?.find(
      (taskNode) =>
        taskNode.approvalStatus === ProcessStatusEnum.APPROVING && taskNode.approverId === userStore.userInfo.id
    );
  });

  const prevMineApprovalNode = computed(() => {
    if (!props.currentApprovalNode) {
      return undefined;
    }
    const prevMineApprovalIndex = props.currentApprovalNode.taskNodes?.findIndex(
      (taskNode) =>
        [ProcessStatusEnum.APPROVED, ProcessStatusEnum.UNAPPROVED, ProcessStatusEnum.NONE].includes(
          taskNode.approvalStatus
        ) && taskNode.approverId === userStore.userInfo.id
    );
    if (prevMineApprovalIndex !== -1) {
      return props.currentApprovalNode.taskNodes?.[prevMineApprovalIndex];
    }
    const prevApprovalNode = props.approvalInfo?.nodes[props.currentApprovalNodeIndex - 1];
    return prevApprovalNode?.taskNodes.find(
      (taskNode) =>
        [ProcessStatusEnum.APPROVED, ProcessStatusEnum.UNAPPROVED, ProcessStatusEnum.NONE].includes(
          taskNode.approvalStatus
        ) && taskNode.approverId === userStore.userInfo.id
    );
  });

  const actionPermission = computed(() =>
    resolveApprovalActionPermission({
      approvalInfo: props.approvalInfo,
      approvalConfig: effectiveApprovalConfig.value,
      currentApprovalNode: props.currentApprovalNode,
      currentApprovalNodeIndex: props.currentApprovalNodeIndex,
      userId: userStore.userInfo.id,
    })
  );

  const approvingItem = computed<Partial<ApprovalTodoItem> | undefined>(() => {
    if (!props.currentApprovalNode || !currentTaskNode.value) {
      return undefined;
    }
    return {
      approvalTaskId: currentTaskNode.value.taskId,
      approvalNodeId: props.currentApprovalNode.nodeId,
      approvalInstanceId: props.approvalInfo?.id,
      approvalId: currentTaskNode.value.approverId,
    };
  });

  const fallbackOptions = computed(() => {
    const options: (PickerOption & { taskId?: string })[] = [];
    for (let i = props.currentApprovalNodeIndex - 1; i >= 0; i--) {
      options.push({
        text: t('workbench.preNode', { index: props.currentApprovalNodeIndex - i }),
        value: props.approvalInfo?.nodes[i].nodeId || '',
        taskId: props.approvalInfo?.nodes[i].nodeId.includes('-SN')
          ? props.approvalInfo?.nodes[i].taskNodes[0].taskId
          : undefined,
      });
    }
    return options;
  });

  const moreActionList = computed(() => {
    const actions: { key: string; text: string; action: () => void }[] = [];
    if (actionPermission.value.canFallback && fallbackOptions.value.length) {
      actions.push({
        key: 'fallback',
        text: t('workbench.operation.BACK'),
        action: openFallbackPopup,
      });
    }
    if (actionPermission.value.canAddSign) {
      actions.push({
        key: 'addSign',
        text: t('workbench.operation.SIGN'),
        action: openAddSignPopup,
      });
    }
    if (actionPermission.value.canCancelApproval) {
      actions.push({
        key: 'cancelApproval',
        text: t('workbench.cancelApproval'),
        action: () => cancelApproval(),
      });
    }
    if (actionPermission.value.canCancelApply) {
      actions.push({
        key: 'cancelApply',
        text: t('workbench.canApply'),
        action: () => cancelApproval('apply'),
      });
    }
    return actions;
  });
  const directMoreAction = computed(() =>
    moreActionList.value.length === 1 && moreActionList.value[0].key !== 'addSign' ? moreActionList.value[0] : undefined
  );
  const showMoreButton = computed(
    () =>
      moreActionList.value.length > 1 ||
      (moreActionList.value.length === 1 && moreActionList.value[0].key === 'addSign')
  );
  const showActions = computed(() => actionPermission.value.showActions);
  const approvalActions = computed(() => {
    if (!showActions.value) {
      return [];
    }

    return [
      ...(actionPermission.value.isApprover
        ? [
            {
              key: 'approve',
              text: t('workbench.operation.APPROVE'),
              action: () => openApprovalPopup(false),
            },
            {
              key: 'reject',
              text: t('workbench.operation.REJECT'),
              color: 'var(--error-red)',
              action: () => openApprovalPopup(true),
            },
          ]
        : []),
      ...moreActionList.value,
    ];
  });

  function handleApprovalAction(key: string) {
    approvalActions.value.find((action) => action.key === key)?.action();
  }

  async function initApprovalConfig() {
    try {
      if (props.formKey && !props.approvalConfig) {
        innerApprovalConfig.value = await getApprovalConfigDetail(props.formKey);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function openApprovalPopup(rejecting: boolean) {
    isRejecting.value = rejecting;
    showApprovalPopup.value = true;
  }

  function openFallbackPopup() {
    showMore.value = false;
    showFallbackPopup.value = true;
  }

  function openAddSignPopup() {
    showMore.value = false;
    showAddSignPopup.value = true;
  }

  function refresh() {
    initApprovalConfig();
    emit('refresh');
  }

  function handleApprovalSuccess() {
    if (!isRejecting.value && props.approvalInfo?.executeTime === ApprovalTaskExecuteTimeEnum.DELETE) {
      emit('deleteApproved');
      return;
    }

    refresh();
    emit('approvalSuccess');
  }

  function cancelApproval(type?: 'apply' | 'approval') {
    showMore.value = false;
    if (type === 'apply') {
      showConfirmDialog({
        title: t('workbench.cancelApprovalApplyConfirm'),
        message: t('workbench.cancelApprovalApplyTip'),
        confirmButtonText: t('workbench.confirmCancelApprovalApply'),
        confirmButtonColor: 'var(--error-red)',
        beforeClose: async (action) => {
          if (action !== 'confirm') {
            return Promise.resolve(true);
          }
          try {
            await revokeResource({
              resourceId: props.sourceId,
              formKey: props.formKey,
            });
            showSuccessToast(t('workbench.cancelApprovalApplySuccess'));
            await sleep(300);
            refresh();
            emit('cancelSuccess');
            return Promise.resolve(true);
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
            return Promise.resolve(false);
          }
        },
      });
    } else {
      showConfirmDialog({
        title: t('workbench.cancelApprovalConfirm'),
        message: t('workbench.cancelApprovalTip'),
        confirmButtonText: t('workbench.confirmCancelApproval'),
        confirmButtonColor: 'var(--error-red)',
        beforeClose: async (action) => {
          if (action !== 'confirm') {
            return Promise.resolve(true);
          }
          try {
            await revokeApproval({
              id: prevMineApprovalNode.value?.taskId || '',
            });
            showSuccessToast(t('workbench.cancelApprovalSuccess'));
            await sleep(300);
            refresh();
            emit('cancelSuccess');
            return Promise.resolve(true);
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
            return Promise.resolve(false);
          }
        },
      });
    }
  }

  onBeforeMount(() => {
    initApprovalConfig();
  });
</script>
