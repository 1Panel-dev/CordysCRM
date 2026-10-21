<template>
  <CrmDrawer v-model:show="show" resizable no-padding :footer="false" :title="sourceName" :view-size="formViewSize">
    <template #titleLeft>
      <CrmApprovalStatus :status="detailInfo?.approvalStatus || ProcessStatusEnum.NONE" />
    </template>
    <template v-if="!props.commentMentionId" #titleRight>
      <CrmOperationButton
        class="gap-[12px]"
        :group-list="detailActions.groupList"
        :more-list="detailActions.moreList"
        :not-show-divider="true"
        @pop-update="handleButtonPopUpdate"
        @select="handleButtonSelect"
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
              :setting-key="`${FormDesignKeyEnum.CUSTOMER_OPEN_SEA}-settingKey`"
              @init="initTabList"
            />
          </template>
        </CrmTab>
      </CrmCard>
      <CrmCard contentHeight="100%" hide-footer :special-height="64" no-content-padding>
        <div v-show="activeTab === 'openSea'" class="h-full overflow-hidden">
          <CrmApprovalDetail
            v-if="show"
            :comment-mention-id="props.commentMentionId"
            :form-key="FormDesignKeyEnum.CUSTOMER"
            :source-id="props.sourceId"
            :refresh-key="refreshKey"
            :approval-status="detailInfo?.approvalStatus || ProcessStatusEnum.NONE"
            @saveApproval="handleSaveApproval"
            @delete-approved="handleDeleteApproved"
          >
            <template #left="{ fieldPermissions, taskNode }">
              <CrmFormDescription
                ref="formDescription"
                :form-key="FormDesignKeyEnum.CUSTOMER_OPEN_SEA"
                :source-id="props.sourceId"
                :refresh-key="refreshKey"
                :hidden-fields="hiddenColumns"
                :column="2"
                label-width="auto"
                value-align="start"
                tooltip-position="top-start"
                :readonly="true"
                :fieldPermissions="fieldPermissions"
                :otherSaveParams="{
                  approvalTaskId: props.approvalTaskId || taskNode?.taskId,
                  commentMentionId: props.commentMentionId,
                }"
                @init="handleDescriptionInit"
                @refresh="emit('change')"
              />
            </template>
          </CrmApprovalDetail>
        </div>
        <FollowDetail
          v-if="activeTab === 'followRecord'"
          active-type="followRecord"
          wrapper-class="h-full"
          virtual-scroll-height="calc(100vh - 254px)"
          :follow-api-key="FormDesignKeyEnum.CUSTOMER_OPEN_SEA"
          :source-id="sourceId"
          :show-action="false"
          :refresh-key="refreshKey"
        />
        <div v-if="activeTab === 'headRecord'" class="h-full px-[24px] pt-[24px]">
          <CrmHeaderTable
            :form-key="FormDesignKeyEnum.CUSTOMER_OPEN_SEA"
            :source-id="props.sourceId"
            :load-list-api="getCustomerHeaderList"
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
  import { CollaborationType } from '@lib/shared/models/customer';
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

  import {
    assignOpenSeaCustomer,
    deleteOpenSeaCustomer,
    getCustomerHeaderList,
    pickOpenSeaCustomer,
  } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useModal from '@/hooks/useModal';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import { hasAnyPermission } from '@/utils/permission';

  import { CustomerRouteEnum } from '@/enums/routeEnum';

  const refreshKey = ref(0);

  const props = defineProps<{
    sourceId: string;
    poolId: string | number;
    readonly?: boolean;
    hiddenColumns: string[];
    refreshDetailKey?: number;
    approvalTaskId?: string;
    commentMentionId?: string;
  }>();
  const emit = defineEmits<{
    (e: 'change'): void;
    (e: 'approvalSuccess'): void;
    (e: 'delete'): void;
    (e: 'freeze', name: string): void;
    (e: 'unfreeze', name: string): void;
  }>();

  const { t } = useI18n();
  const { openModal } = useModal();
  const Message = useMessage();
  const { openNewPage } = useOpenNewPage();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const sourceId = computed(() => props.sourceId);
  const claimLoading = ref(false);
  const distributeLoading = ref(false);
  const frozen = ref(false);
  const sourceName = ref('');
  const detailInfo = ref<Record<string, any>>({
    approvalStatus: ProcessStatusEnum.NONE,
  });
  const openSeaDeleteRolePermissions = ['CUSTOMER_MANAGEMENT_POOL:DELETE'];
  const openSeaActions = computed<ActionsItem[]>(() => {
    if (props.readonly) {
      return [];
    }
    return (
      [
        {
          label: t('common.claim'),
          key: 'claim',
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
          permission: ['CUSTOMER_MANAGEMENT_POOL:PICK'],
          popConfirmProps: {
            loading: claimLoading.value,
            title: t('customer.claimTip'),
            content: t('customer.claimTipContent'),
            positiveText: t('common.claim'),
            iconType: 'primary',
          },
        },
        {
          label: t('common.distribute'),
          key: 'distribute',
          text: false,
          ghost: true,
          permission: ['CUSTOMER_MANAGEMENT_POOL:ASSIGN'],
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
          permission: ['CUSTOMER_MANAGEMENT_POOL:FREEZE'],
        },
        {
          label: t('common.unfreeze'),
          key: 'unfreeze',
          text: false,
          ghost: true,
          permission: ['CUSTOMER_MANAGEMENT_POOL:FREEZE'],
        },
      ] as ActionsItem[]
    ).filter((item) =>
      frozen.value ? !['claim', 'distribute', 'freeze'].includes(item.key || '') : item.key !== 'unfreeze'
    );
  });

  const { initApprovalPermission, resolveRowActions, deleteExecute } = useApprovalOperation<Record<string, any>>({
    formType: FormDesignKeyEnum.CUSTOMER,
    dataActionMap: {
      delete: {
        label: t('common.delete'),
        key: 'delete',
        text: false,
        ghost: true,
        danger: true,
        class: 'n-btn-outline-primary',
        permission: ['CUSTOMER_MANAGEMENT:DELETE'],
      },
    },
    isDetail: true,
    ignoreRolePermissionCheck: true,
  });

  const { reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.CUSTOMER,
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
      ...openSeaActions.value,
      ...(deleteAction && hasAnyPermission(openSeaDeleteRolePermissions)
        ? [{ ...deleteAction, permission: openSeaDeleteRolePermissions }]
        : []),
    ].map((item) => ({
      ...item,
      text: false,
      ghost: true,
      class: item.class || 'n-btn-outline-primary',
    }));

    return splitDetailActions(actions);
  });

  const activeTab = ref('openSea');
  const settingTabOptions: TabContentItem[] = [
    {
      name: 'followRecord',
      tab: t('crmFollowRecord.followRecord'),
      enable: true,
    },
    {
      name: 'headRecord',
      tab: t('common.headRecord'),
      enable: true,
    },
  ];
  const settingTabList = ref<TabContentItem[]>(settingTabOptions);
  const tabList = computed<TabContentItem[]>(() => [
    {
      name: 'openSea',
      tab: t('crmFormDesign.customer'),
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

  const distributeFormRef = ref<InstanceType<typeof TransferForm>>();
  const distributeForm = ref<any>({
    owner: null,
  });

  function resetDistributeForm() {
    distributeForm.value = {
      owner: null,
    };
    distributeFormRef.value?.formRef?.restoreValidation();
  }

  function handleButtonPopUpdate(key: string, visible: boolean) {
    if (key === 'distribute' && visible) {
      resetDistributeForm();
    }
  }

  // 删除
  function handleDelete() {
    openModal({
      type: 'error',
      title: t('customer.deleteTitleTip'),
      content: t('customer.batchDeleteContentTip'),
      positiveText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteOpenSeaCustomer(props.sourceId);
          Message.success(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
          emit('delete');
          show.value = false;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function handleClaim(id: string) {
    try {
      claimLoading.value = true;
      await pickOpenSeaCustomer({
        customerId: id,
        poolId: props.poolId,
      });
      Message.success(t('common.claimSuccess'));
      emit('delete');
      show.value = false;
      openNewPage(CustomerRouteEnum.CUSTOMER_INDEX, {
        id,
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      claimLoading.value = false;
    }
  }

  async function handleDistribute(id: string) {
    try {
      distributeLoading.value = true;
      await assignOpenSeaCustomer({
        customerId: id,
        assignUserId: distributeForm.value.owner || '',
      });
      Message.success(t('common.distributeSuccess'));
      resetDistributeForm();
      emit('delete');
      show.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      distributeLoading.value = false;
    }
  }

  function handleSaved() {
    refreshKey.value += 1;
    emit('change');
  }

  async function handleSaveApproval(callback: () => Promise<any>, _hasFieldPermission: boolean) {
    await callback();
    emit('approvalSuccess');
    handleSaved();
  }

  function handleDeleteApproved() {
    show.value = false;
    emit('change');
  }

  function handleButtonSelect(key: string) {
    switch (key) {
      case 'review':
        reviewByResourceId(props.sourceId, {
          onSuccess: handleSaved,
        });
        break;
      case 'revoke':
        revokeByResourceId(props.sourceId, {
          onSuccess: handleSaved,
        });
        break;
      case 'delete':
        handleDelete();
        break;
      case 'pop-claim':
        handleClaim(props.sourceId);
        break;
      case 'pop-distribute':
        handleDistribute(props.sourceId);
        break;
      case 'freeze':
        emit('freeze', sourceName.value);
        break;
      case 'unfreeze':
        emit('unfreeze', sourceName.value);
        break;
      default:
        break;
    }
  }

  const formDescription = ref<InstanceType<typeof CrmFormDescription>>();

  watch(
    () => props.refreshDetailKey,
    () => {
      refreshKey.value += 1;
      formDescription.value?.refreshFormDescription();
    }
  );

  const formViewSize = ref<FormViewSize>('large');
  function handleDescriptionInit(
    _collaborationType?: CollaborationType,
    _sourceName?: string,
    detail?: Record<string, any>,
    config?: FormConfig
  ) {
    sourceName.value = _sourceName || '';
    frozen.value = Boolean(detail?.frozen);
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
