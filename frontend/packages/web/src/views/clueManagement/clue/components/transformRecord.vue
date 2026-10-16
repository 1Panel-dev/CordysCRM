<template>
  <div ref="crmTableContainerRef" class="crm-transform-record bg-[var(--text-n10)]">
    <CrmTable
      ref="crmTableRef"
      v-model:checked-row-keys="checkedRowKeys"
      v-bind="{
        data: data,
        hiddenRefresh: true,
        hiddenTotal: true,
        bordered: false,
        loading: loading,
      }"
      class="crm-customer-table !h-[200px] p-[24px]"
      :columns="realColumns"
      :not-show-table-filter="true"
      :fullscreenTargetRef="crmTableContainerRef"
      @page-change="() => ({})"
      @page-size-change="() => ({})"
      @sorter-change="() => ({})"
    >
    </CrmTable>
  </div>
  <customerOverviewDrawer
    v-model:show="showOverviewDrawer"
    :source-id="activeSourceId"
    @saved="init()"
    @deleted="init()"
    @transfer="init()"
    @refresh="init()"
  />

  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="activeFormKey"
    :source-id="activeSourceId"
    :need-init-detail="needInitDetail"
    :initial-source-name="initialSourceName"
    :other-save-params="otherFollowRecordSaveParams"
    :link-form-info="linkFormFieldMap"
    :link-form-key="FormDesignKeyEnum.CUSTOMER"
    :link-scenario="
      activeFormKey === FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER ? FormLinkScenarioEnum.CUSTOMER_TO_RECORD : undefined
    "
    @saved="init()"
    @review="handleFormReview"
  />
  <CrmMoveModal
    v-model:show="showMoveModal"
    :reason-key="ReasonTypeEnum.CUSTOMER_POOL_RS"
    :source-id="moveIds"
    :name="initialSourceName"
    type="warning"
    @refresh="init()"
  />
</template>

<script setup lang="ts">
  import { DataTableRowKey, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum, FormLinkScenarioEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum, ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { characterLimit } from '@lib/shared/method';
  import { transformData } from '@lib/shared/method/formCreate';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import type { CrmPopConfirmIconType } from '@/components/pure/crm-pop-confirm/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmApprovalPopover from '@/components/business/crm-approval/components/crm-approval-popover.vue';
  import { fixedFieldKeyListMap } from '@/components/business/crm-duplicate-check-drawer/useSearchTable';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmMoveModal from '@/components/business/crm-move-modal/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import TransferForm from '@/components/business/crm-transfer-modal/transferForm.vue';
  import customerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';

  import { deleteCustomer, getClueTransitionCustomerRecordList, updateCustomer } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import useUpdateReviewChange from '@/hooks/useUpdateReviewChange';

  const props = defineProps<{
    resourceId: string;
  }>();

  const Message = useMessage();
  const { currentLocale } = useLocale(Message.loading);
  const { t } = useI18n();
  const { openModal } = useModal();

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const activeSourceId = ref('');
  const formCreateDrawerVisible = ref(false);
  const crmTableContainerRef = ref<HTMLElement | null>(null);
  const initialSourceName = ref('');
  const needInitDetail = ref(false);
  const activeFormKey = ref(FormDesignKeyEnum.CUSTOMER);
  const otherFollowRecordSaveParams = ref({
    type: 'CUSTOMER',
    customerId: '',
    id: '',
  });
  const transferFormRef = ref<InstanceType<typeof TransferForm>>();
  const transferLoading = ref(false);
  const transferForm = ref<any>({
    owner: null,
  });

  const customerDataActionMap = computed<Record<string, ActionsItem>>(() => {
    const actions: Record<string, ActionsItem> = {
      followUp: {
        label: t('opportunity.followUp'),
        key: 'followUp',
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
      edit: {
        label: t('common.edit'),
        key: 'edit',
        permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
      transfer: {
        label: t('common.transfer'),
        key: 'transfer',
        popConfirmProps: {
          loading: transferLoading.value,
          title: t('common.transfer'),
          positiveText: t('common.confirm'),
          iconType: 'primary' as CrmPopConfirmIconType,
        },
        popSlotName: 'transferPopTitle',
        popSlotContent: 'transferPopContent',
        permission: ['CUSTOMER_MANAGEMENT:TRANSFER'],
      },
      moveToOpenSea: {
        label: t('customer.moveToOpenSea'),
        key: 'moveToOpenSea',
        permission: ['CUSTOMER_MANAGEMENT:RECYCLE'],
      },
      delete: {
        label: t('common.delete'),
        key: 'delete',
        permission: ['CUSTOMER_MANAGEMENT:DELETE'],
      },
    };
    return actions;
  });
  const {
    initApprovalPermission,
    enableApproval,
    deleteExecute,
    updateExecute,
    resolveRowOperation,
    hasApprovalScopedPermission,
  } = useApprovalOperation<Record<string, any>>({
    formType: FormDesignKeyEnum.CUSTOMER,
    dataActionMap: () => customerDataActionMap.value,
  });

  const { reviewByFormResult, reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.CUSTOMER,
  });
  const { getUpdateReviewExtraParams } = useUpdateReviewChange();

  // 概览
  const showOverviewDrawer = ref(false);

  await initApprovalPermission();

  const {
    fieldList: customerFormFields,
    linkFormFieldMap,
    initFormConfig,
    initFormDetail,
  } = useFormCreateApi({
    formKey: computed(() => FormDesignKeyEnum.CUSTOMER),
    sourceId: activeSourceId,
    needInitDetail: computed(() => true),
  });

  const data = ref<any[]>([]);
  const loading = ref(false);
  const hasPermission = ref(false);
  const customerFormFieldList = ref<FormCreateField[]>([]);
  async function init() {
    try {
      loading.value = true;
      const res = await getClueTransitionCustomerRecordList(props.resourceId);
      hasPermission.value = res.hasPermission;
      data.value = res.list.length
        ? [
            transformData({
              item: res.list[0],
              originalData: res,
              fields: customerFormFieldList.value,
            }),
          ]
        : [];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  const showMoveModal = ref(false);
  const moveIds = ref<(string | number) | (string | number)[]>('');

  function handleMoveToOpenSea(row?: any) {
    initialSourceName.value = row?.name ?? '';
    moveIds.value = row?.id ? row.id : checkedRowKeys.value;
    showMoveModal.value = true;
  }

  // 删除
  function handleDelete(row: any) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('customer.batchDeleteContentTip'),
      positiveText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCustomer(row.id);
          Message.success(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
          init();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 转移
  function resetTransferForm() {
    transferForm.value.owner = null;
  }

  function handleTransferPopUpdate(key: string, show: boolean) {
    if (key === 'transfer' && show) {
      resetTransferForm();
    }
  }

  async function transferCustomer(row: Record<string, any>) {
    try {
      const extraParams = await getUpdateReviewExtraParams(Boolean(row.approved && updateExecute.value));
      if (extraParams === false) {
        return;
      }

      transferLoading.value = true;
      await updateCustomer({
        id: activeSourceId.value,
        owner: transferForm.value.owner,
        ...extraParams,
      });

      Message.success(t('common.transferSuccess'));
      resetTransferForm();
      init();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      transferLoading.value = false;
    }
  }

  async function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'review':
        reviewByResourceId(row.id, {
          onSuccess: (resourceId) => init(),
        });
        break;
      case 'revoke':
        revokeByResourceId(row.id, {
          onSuccess: (resourceId) => init(),
        });
        break;
      case 'edit':
        activeFormKey.value = FormDesignKeyEnum.CUSTOMER;
        activeSourceId.value = row.id;
        needInitDetail.value = true;
        otherFollowRecordSaveParams.value.id = row.id;
        linkFormFieldMap.value = {};
        formCreateDrawerVisible.value = true;
        break;
      case 'followUp':
        activeFormKey.value = FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER;
        activeSourceId.value = row.id;
        needInitDetail.value = false;
        initialSourceName.value = row.name;
        otherFollowRecordSaveParams.value.customerId = row.id;
        if (customerFormFields.value.length === 0) {
          await initFormConfig();
        }
        await initFormDetail(false, true);
        formCreateDrawerVisible.value = true;
        break;
      case 'pop-transfer':
        activeSourceId.value = row.id;
        transferCustomer(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      case 'moveToOpenSea':
        handleMoveToOpenSea(row);
        break;
      default:
        break;
    }
  }

  function handleFormReview(res: any) {
    reviewByFormResult(res, {
      onSuccess: () => {
        init();
      },
    });
  }

  const { columns, fieldList } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CUSTOMER,
    tableKey: FormDesignKeyEnum.CUSTOMER,
    operationColumn: {
      key: 'operation',
      width: currentLocale.value === 'en-US' ? 250 : 200,
      fixed: 'right',
      render: (row: any) => {
        if (row.collaborationType === 'READ_ONLY') {
          return '-';
        }
        const operation = resolveRowOperation(row);
        return operation.groupList.length
          ? h(
              CrmOperationButton,
              {
                groupList: operation.groupList,
                moreList: operation.moreList,
                onSelect: (key: string) => handleActionSelect(row, key),
                onCancel: resetTransferForm,
                onPopUpdate: handleTransferPopUpdate,
              },
              {
                transferPopContent: () =>
                  h(TransferForm, {
                    class: 'w-[320px] mt-[16px]',
                    form: transferForm.value,
                    ref: transferFormRef,
                    moduleType: ModuleConfigEnum.CLUE_MANAGEMENT,
                  }),
              }
            )
          : '-';
      },
    },
    specialRender: {
      name: (row: any) => {
        if (!hasPermission.value) {
          return h(CrmNameTooltip, { text: row.name });
        }
        return hasApprovalScopedPermission(row, ['CUSTOMER_MANAGEMENT:READ'])
          ? h(
              CrmTableButton,
              {
                onClick: () => {
                  activeSourceId.value = row.id;
                  showOverviewDrawer.value = true;
                },
              },
              { trigger: () => row.name, default: () => row.name }
            )
          : h(CrmNameTooltip, { text: row.name });
      },
      approvalStatus: (row: any) =>
        h(CrmApprovalPopover, {
          status: row.approvalStatus,
          formKey: FormDesignKeyEnum.CUSTOMER,
          sourceId: row.id,
          showMore: hasApprovalScopedPermission(row, ['CUSTOMER_MANAGEMENT:READ']),
          disabled: row.approvalStatus !== ProcessStatusEnum.UNAPPROVED,
          onMore: () => {
            activeSourceId.value = row.id;
            showOverviewDrawer.value = true;
          },
        }),
    },
    containerClass: '.crm-transform-record',
    enableApproval,
  });
  customerFormFieldList.value = fieldList.value;

  const realColumns = computed(() =>
    columns.value.filter(
      (item) =>
        item.type !== 'selection' &&
        (hasPermission.value
          ? true
          : fixedFieldKeyListMap[FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER]?.includes(item.key as string) ||
            ['followTime', 'createTime'].includes(item.key as string))
    )
  );

  onBeforeMount(() => {
    init();
  });
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
</style>
