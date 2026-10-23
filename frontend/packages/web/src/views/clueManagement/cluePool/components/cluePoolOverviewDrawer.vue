<template>
  <CrmDrawer
    v-model:show="show"
    resizable
    no-padding
    :footer="false"
    :title="props.detail?.name"
    :view-size="formViewSize"
  >
    <template #titleLeft>
      <CrmApprovalStatus :status="detailInfo?.approvalStatus || ProcessStatusEnum.NONE" />
    </template>
    <template #titleRight>
      <CrmOperationButton
        class="gap-[12px]"
        :group-list="detailActions.groupList"
        :more-list="detailActions.moreList"
        :not-show-divider="true"
        @pop-update="handleButtonPopUpdate"
        @select="handleSelect"
      >
        <template #more>
          <n-button type="primary" ghost class="n-btn-outline-primary">
            {{ t('common.more') }}
            <CrmIcon class="ml-[8px]" type="iconicon_chevron_down" :size="16" />
          </n-button>
        </template>
        <template #distributePopContent>
          <TransferForm ref="distributeFormRef" v-model:form="distributeForm" class="mt-[16px] w-[320px]" />
        </template>
      </CrmOperationButton>
    </template>
    <div class="h-full bg-[var(--text-n9)] p-[16px]">
      <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line">
          <template #suffix>
            <CrmTabSetting
              :tab-list="settingTabOptions"
              :setting-key="`${FormDesignKeyEnum.CLUE_POOL}-settingKey`"
              @init="initTabList"
            />
          </template>
        </CrmTab>
      </CrmCard>
      <CrmCard contentHeight="100%" hide-footer :special-height="64" no-content-padding>
        <div v-show="activeTab === 'cluePool'" class="h-full overflow-hidden">
          <CrmApprovalDetail
            v-if="show"
            :refresh-key="refreshKey"
            :form-key="FormDesignKeyEnum.CLUE"
            :source-id="sourceId"
            :approval-status="detailInfo?.approvalStatus || ProcessStatusEnum.NONE"
            @saveApproval="handleSaveApproval"
            @delete-approved="handleDeleteApproved"
          >
            <template #left="{ fieldPermissions, taskNode }">
              <CrmFormDescription
                :refresh-key="refreshKey"
                :form-key="FormDesignKeyEnum.CLUE_POOL"
                :source-id="sourceId"
                :hidden-fields="hiddenColumns"
                :column="2"
                label-width="auto"
                value-align="start"
                tooltip-position="top-start"
                :readonly="true"
                :fieldPermissions="fieldPermissions"
                :otherSaveParams="{
                  approvalTaskId: props.approvalTaskId || taskNode?.taskId,
                }"
                @init="handleDescriptionInit"
                @refresh="emit('refresh')"
              />
            </template>
          </CrmApprovalDetail>
        </div>
        <FollowDetail
          v-if="activeTab === 'followRecord'"
          active-type="followRecord"
          wrapper-class="h-full"
          virtual-scroll-height="calc(100vh - 254px)"
          :follow-api-key="FormDesignKeyEnum.CLUE_POOL"
          :source-id="sourceId"
          :show-action="false"
          :refresh-key="refreshKey"
        />
        <div v-if="activeTab === 'headRecord'" class="h-full px-[24px] pt-[24px]">
          <CrmHeaderTable
            :form-key="FormDesignKeyEnum.CLUE_POOL"
            :source-id="sourceId"
            :load-list-api="getClueHeaderList"
          />
        </div>
      </CrmCard>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { CluePoolListItem } from '@lib/shared/models/clue';
  import type { CollaborationType, TransferParams } from '@lib/shared/models/customer';
  import type { FormConfig, FormViewSize } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmApprovalDetail from '@/components/business/crm-approval/components/crm-approval-detail.vue';
  import CrmApprovalStatus from '@/components/business/crm-approval/components/crm-approval-status.vue';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmHeaderTable from '@/components/business/crm-header-table/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmTabSetting from '@/components/business/crm-tab-setting/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';

  import { assignClue, deleteCluePool, getClueHeaderList, pickClue } from '@/api/modules';
  import { defaultTransferForm } from '@/config/opportunity';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useModal from '@/hooks/useModal';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import { hasAnyPermission } from '@/utils/permission';

  import { ClueRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    detail?: Partial<CluePoolListItem>;
    poolId: string;
    hiddenColumns: string[];
    approvalTaskId?: string;
  }>();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'remove'): void;
    (e: 'refresh'): void;
    (e: 'approvalSuccess'): void;
    (e: 'freeze'): void;
    (e: 'unfreeze'): void;
  }>();

  const { openModal } = useModal();
  const { t } = useI18n();
  const Message = useMessage();
  const { openNewPage } = useOpenNewPage();

  const sourceId = computed(() => props.detail?.id ?? '');

  const refreshKey = ref(0);
  const detailInfo = ref<Record<string, any>>({
    ...props.detail,
    approvalStatus: props.detail?.approvalStatus ?? ProcessStatusEnum.NONE,
  });

  const claimLoading = ref(false);
  const distributeLoading = ref(false);

  const cluePoolDeleteRolePermissions = ['CLUE_MANAGEMENT_POOL:DELETE'];
  const cluePoolActions = computed<ActionsItem[]>(() =>
    (
      [
        {
          label: t('common.claim'),
          key: 'claim',
          permission: ['CLUE_MANAGEMENT_POOL:PICK'],
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
          popConfirmProps: {
            loading: claimLoading.value,
            title: t('clue.claimOverviewTip'),
            positiveText: t('common.claim'),
            iconType: 'primary',
          },
        },
        {
          label: t('common.distribute'),
          key: 'distribute',
          permission: ['CLUE_MANAGEMENT_POOL:ASSIGN'],
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
          popConfirmProps: {
            loading: distributeLoading.value,
            title: t('common.distribute'),
            positiveText: t('common.confirm'),
            iconType: 'primary',
          },
          popSlotContent: 'distributePopContent',
        },
        {
          label: t('common.freeze'),
          key: 'freeze',
          text: false,
          ghost: true,
          permission: ['CLUE_MANAGEMENT_POOL:FREEZE'],
        },
        {
          label: t('common.unfreeze'),
          key: 'unfreeze',
          text: false,
          ghost: true,
          permission: ['CLUE_MANAGEMENT_POOL:FREEZE'],
        },
      ] as ActionsItem[]
    ).filter((item) =>
      props.detail?.frozen ? !['claim', 'distribute', 'freeze'].includes(item.key || '') : item.key !== 'unfreeze'
    )
  );

  const { initApprovalPermission, resolveRowActions, deleteExecute } = useApprovalOperation<Record<string, any>>({
    formType: FormDesignKeyEnum.CLUE,
    dataActionMap: {
      delete: {
        label: t('common.delete'),
        key: 'delete',
        permission: ['CLUE_MANAGEMENT:DELETE'],
        text: false,
        ghost: true,
        danger: true,
        class: 'n-btn-outline-primary',
      },
    },
    isDetail: true,
    ignoreRolePermissionCheck: true,
  });

  const { reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.CLUE,
  });

  function splitDetailActions(actions: ActionsItem[]) {
    const realVisibleNumber = 2;

    if (actions.length <= realVisibleNumber) {
      return {
        groupList: actions,
        moreList: [] as ActionsItem[],
      };
    }

    return {
      groupList: [
        ...actions.slice(0, realVisibleNumber),
        {
          label: t('common.more'),
          key: 'more',
          slotName: 'more',
        },
      ],
      moreList: actions.slice(realVisibleNumber),
    };
  }

  const detailActions = computed(() => {
    const approvalActions = resolveRowActions(detailInfo.value);
    const reviewActions = approvalActions.filter((item) => ['review', 'revoke'].includes(item.key || ''));
    const deleteAction = approvalActions.find((item) => item.key === 'delete');
    const actions = [
      ...reviewActions,
      ...cluePoolActions.value,
      ...(deleteAction && hasAnyPermission(cluePoolDeleteRolePermissions)
        ? [{ ...deleteAction, permission: cluePoolDeleteRolePermissions }]
        : []),
    ].map((item) => ({
      ...item,
      text: false,
      ghost: true,
      class: item.class || 'n-btn-outline-primary',
    }));

    return splitDetailActions(actions);
  });

  const distributeFormRef = ref<InstanceType<typeof TransferForm>>();
  const distributeForm = ref<TransferParams>({
    ...defaultTransferForm,
  });

  function resetDistributeForm() {
    distributeForm.value = { ...defaultTransferForm };
    distributeFormRef.value?.formRef?.restoreValidation();
  }

  function handleButtonPopUpdate(key: string, visible: boolean) {
    if (key === 'distribute' && visible) {
      resetDistributeForm();
    }
  }

  function handleDistribute() {
    distributeFormRef.value?.formRef?.validate(async (error) => {
      if (!error) {
        try {
          distributeLoading.value = true;
          await assignClue({
            assignUserId: distributeForm.value.owner || '',
            clueId: sourceId.value,
          });
          Message.success(t('common.distributeSuccess'));
          resetDistributeForm();
          emit('remove');
          show.value = false;
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          distributeLoading.value = false;
        }
      }
    });
  }

  // 删除
  function handleDelete() {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(props.detail?.name) }),
      content: t('clue.batchDeleteContentTip'),
      positiveText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCluePool(sourceId.value);
          Message.success(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
          show.value = false;
          emit('remove');
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  // 领取
  async function handleClaim() {
    try {
      claimLoading.value = true;
      await pickClue({
        clueId: sourceId.value,
        poolId: props.poolId,
      });
      Message.success(t('common.claimSuccess'));
      emit('remove');
      show.value = false;
      openNewPage(ClueRouteEnum.CLUE_MANAGEMENT, {
        id: sourceId.value,
        transitionType: props.detail?.transitionType,
        name: props.detail?.name,
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      claimLoading.value = false;
    }
  }

  function handleSaved() {
    refreshKey.value += 1;
    emit('refresh');
  }

  async function handleSaveApproval(callback: () => Promise<any>, _hasFieldPermission: boolean) {
    await callback();
    emit('approvalSuccess');
    handleSaved();
  }

  function handleDeleteApproved() {
    show.value = false;
    emit('refresh');
  }

  function handleSelect(key: string) {
    switch (key) {
      case 'review':
        reviewByResourceId(sourceId.value, {
          onSuccess: handleSaved,
        });
        break;
      case 'revoke':
        revokeByResourceId(sourceId.value, {
          onSuccess: handleSaved,
        });
        break;
      case 'pop-claim':
        handleClaim();
        break;
      case 'pop-distribute':
        handleDistribute();
        break;
      case 'delete':
        handleDelete();
        break;
      case 'freeze':
        emit('freeze');
        break;
      case 'unfreeze':
        emit('unfreeze');
        break;
      default:
        break;
    }
  }

  const activeTab = ref('cluePool');
  const settingTabOptions: TabContentItem[] = [
    {
      name: 'followRecord',
      tab: t('crmFollowRecord.followRecord'),
      enable: true,
    },
    {
      name: 'headRecord',
      tab: t('common.previousOwnerRecord'),
      enable: true,
    },
  ];
  const settingTabList = ref<TabContentItem[]>(settingTabOptions);
  const tabList = computed<TabContentItem[]>(() => [
    {
      name: 'cluePool',
      tab: t('crmFormDesign.clue'),
      enable: true,
    },
    ...settingTabList.value,
  ]);

  function initTabList(list: TabContentItem[]) {
    settingTabList.value = list;
  }

  watch(
    () => tabList.value,
    (list) => {
      if (!list.some((item) => item.name === activeTab.value)) {
        activeTab.value = list[0]?.name as string;
      }
    }
  );

  const formViewSize = ref<FormViewSize>('large');
  function handleDescriptionInit(
    _collaborationType?: CollaborationType,
    _sourceName?: string,
    detail?: Record<string, any>,
    config?: FormConfig
  ) {
    detailInfo.value = {
      ...detail,
      approvalStatus: detail?.approvalStatus ?? ProcessStatusEnum.NONE,
    };
    formViewSize.value = config?.viewSize || 'large';
  }

  onBeforeMount(() => {
    initApprovalPermission();
  });
</script>
