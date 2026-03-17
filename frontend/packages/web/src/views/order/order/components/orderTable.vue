<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    :class="`crm-order-table-${props.formKey}`"
    :not-show-table-filter="isAdvancedSearchMode"
    :fullscreen-target-ref="props.fullscreenTargetRef"
    :action-config="{ baseAction: [] }"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="propsEvent.filterChange"
    @refresh="searchData"
  >
    <template #actionLeft>
      <n-button
        v-if="!props.readonly && !props.isCustomerTab"
        v-permission="['ORDER:ADD']"
        type="primary"
        @click="handleNewClick"
      >
        {{ t('order.new') }}
      </n-button>
    </template>
    <template #actionRight>
      <CrmAdvanceFilter
        v-if="!props.hiddenAdvanceFilter"
        ref="tableAdvanceFilterRef"
        v-model:keyword="keyword"
        :search-placeholder="t('order.searchPlaceholder')"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        @adv-search="handleAdvSearch"
        @keyword-search="searchData"
      />
    </template>
    <template #view>
      <CrmViewSelect
        v-if="!props.isContractTab && !props.isCustomerTab"
        v-model:active-tab="activeTab"
        :type="FormDesignKeyEnum.ORDER"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        @refresh-table-data="searchData"
      />
    </template>
  </CrmTable>

  <DetailDrawer
    v-model:visible="showDetailDrawer"
    :sourceId="activeSourceId"
    :readonly="props.readonly"
    @refresh="searchData"
    @open-contract-drawer="showContractDrawer"
  />
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="activeFormKey"
    :source-id="activeSourceId"
    :need-init-detail="needInitDetail"
    :initial-source-name="initialSourceName"
    :link-form-key="FormDesignKeyEnum.CONTRACT"
    :link-form-info="linkFormInfo"
    :link-scenario="FormLinkScenarioEnum.CONTRACT_TO_ORDER"
    @saved="() => searchData()"
  />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum, FormLinkScenarioEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
  import { characterLimit } from '@lib/shared/method';
  import { OpportunityStageConfig } from '@lib/shared/models/opportunity';
  import { OrderItem } from '@lib/shared/models/order';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';
  import DetailDrawer from './detail.vue';

  import { deleteOrder, getOrderStatusConfig } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import { hasAnyPermission } from '@/utils/permission';

  import { FullPageEnum } from '@/enums/routeEnum';

  const route = useRoute();

  const { t } = useI18n();
  const Message = useMessage();
  const { currentLocale } = useLocale(Message.loading);
  const { openModal } = useModal();
  const { openNewPage } = useOpenNewPage();

  const props = defineProps<{
    fullscreenTargetRef?: HTMLElement | null;
    hiddenAdvanceFilter?: boolean;
    isContractTab?: boolean;
    isCustomerTab?: boolean;
    sourceId?: string; // 合同详情下
    sourceName?: string;
    readonly?: boolean;
    formKey: FormDesignKeyEnum.ORDER | FormDesignKeyEnum.CONTRACT_ORDER | FormDesignKeyEnum.CUSTOMER_ORDER;
  }>();
  const emit = defineEmits<{
    (e: 'openContractDrawer', params: { id: string }): void;
    (e: 'openCustomerDrawer', params: { customerId: string; inCustomerPool: boolean; poolId: string }): void;
  }>();

  const activeTab = ref();
  const keyword = ref('');

  const stageConfig = ref<OpportunityStageConfig>();
  async function initStageConfig() {
    try {
      stageConfig.value = await getOrderStatusConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
  await initStageConfig();

  // 操作
  const checkedRowKeys = ref<DataTableRowKey[]>([]);

  const formCreateDrawerVisible = ref(false);
  const activeSourceId = ref(props.sourceId || '');
  const initialSourceName = ref('');
  const needInitDetail = ref(false);
  const activeFormKey = ref(FormDesignKeyEnum.ORDER);

  const createLoading = ref(false);
  const linkFormKey = ref(FormDesignKeyEnum.CONTRACT);
  const linkFormInfo = ref();
  const { initFormDetail, initFormConfig, linkFormFieldMap } = useFormCreateApi({
    formKey: linkFormKey,
    sourceId: activeSourceId,
  });

  async function handleNewClick() {
    try {
      createLoading.value = true;
      activeSourceId.value = props.isContractTab ? props.sourceId || '' : '';
      initialSourceName.value = props.isContractTab ? props.sourceName || '' : '';
      needInitDetail.value = false;
      activeFormKey.value = FormDesignKeyEnum.ORDER;
      if (props.isContractTab) {
        linkFormKey.value = FormDesignKeyEnum.CONTRACT;
        await initFormConfig();
        await initFormDetail(false, true);
      }
      linkFormInfo.value = linkFormFieldMap.value;
      formCreateDrawerVisible.value = true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      createLoading.value = false;
    }
  }

  // 表格
  const filterConfigList = computed<FilterFormItem[]>(() => [
    {
      title: t('opportunity.department'),
      dataIndex: 'departmentId',
      type: FieldTypeEnum.TREE_SELECT,
      treeSelectProps: {
        labelField: 'name',
        keyField: 'id',
        multiple: true,
        clearFilterAfterSelect: false,
        type: 'department',
        checkable: true,
        showContainChildModule: true,
        containChildIds: [],
      },
    },
    {
      title: t('order.status'),
      dataIndex: 'stage',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      selectProps: {
        options:
          stageConfig.value?.stageConfigList.map((e: any) => ({
            label: e.name,
            value: e.id,
          })) || [],
      },
    },
    ...baseFilterConfigList,
  ]);

  const operationGroupList = computed<ActionsItem[]>(() => {
    return [
      ...(!props.readonly
        ? [
            {
              label: t('common.edit'),
              key: 'edit',
              permission: ['ORDER:UPDATE'],
            },
          ]
        : []),
      {
        label: t('common.download'),
        key: 'download',
        permission: ['ORDER:DOWNLOAD'],
      },
      ...(!props.readonly
        ? [
            {
              label: 'more',
              key: 'more',
              slotName: 'more',
            },
          ]
        : []),
    ];
  });

  const tableRefreshId = ref(0);
  const showDetailDrawer = ref(false);

  function handleEdit(id: string) {
    activeFormKey.value = FormDesignKeyEnum.ORDER;
    activeSourceId.value = id;
    needInitDetail.value = true;
    formCreateDrawerVisible.value = true;
  }

  function showDetail(id: string) {
    activeSourceId.value = id;
    showDetailDrawer.value = true;
  }

  function handleDownload(id: string) {
    openNewPage(FullPageEnum.FULL_PAGE_EXPORT_ORDER, { id });
  }

  async function handleDelete(row: OrderItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteOrder(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function handleActionSelect(row: OrderItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'download':
        handleDownload(row.id);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  function showContractDrawer(params: { id: string }) {
    if (props.isContractTab) {
      showDetailDrawer.value = false;
    } else {
      emit('openContractDrawer', {
        id: params.id,
      });
    }
  }

  function showCustomerDrawer(params: { customerId: string; inCustomerPool: boolean; poolId: string }) {
    emit('openCustomerDrawer', {
      customerId: params.customerId,
      inCustomerPool: params.inCustomerPool,
      poolId: params.poolId || '',
    });
  }

  const { useTableRes, customFieldsFilterConfig } = await useFormCreateTable({
    formKey: props.formKey,
    excludeFieldIds: ['contractId'],
    operationColumn: {
      key: 'operation',
      width: currentLocale.value === 'en-US' ? 180 : 150,
      fixed: 'right',
      render: (row: OrderItem) =>
        h(CrmOperationButton, {
          groupList: operationGroupList.value,
          onSelect: (key: string) => handleActionSelect(row, key),
          moreList: [
            {
              label: t('common.delete'),
              key: 'delete',
              danger: true,
              permission: ['ORDER:DELETE'],
            },
          ],
        }),
    },
    specialRender: {
      name: (row: OrderItem) => {
        return h(
          CrmTableButton,
          {
            onClick: () => {
              showDetail(row.id);
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
      contractId: (row: OrderItem) => {
        return props.isContractTab || props.readonly || !hasAnyPermission(['CONTRACT:READ'])
          ? h(
              CrmNameTooltip,
              { text: row.contractName },
              {
                default: () => row.contractName,
              }
            )
          : h(
              CrmTableButton,
              {
                onClick: () => {
                  showContractDrawer({ id: row.contractId });
                },
              },
              { default: () => row.contractName, trigger: () => row.contractName }
            );
      },
      customerId: (row: OrderItem) => {
        return props.isCustomerTab ||
          (!row.inCustomerPool && !hasAnyPermission(['CUSTOMER_MANAGEMENT:READ'])) ||
          (row.inCustomerPool && !hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:READ']))
          ? h(
              CrmNameTooltip,
              { text: row.customerName },
              {
                default: () => row.customerName,
              }
            )
          : h(
              CrmTableButton,
              {
                onClick: () => {
                  showCustomerDrawer(row);
                },
              },
              { default: () => row.customerName, trigger: () => row.customerName }
            );
      },
      stage: (row: OrderItem) => {
        return row.stageName || '-';
      },
    },
    containerClass: `.crm-order-table-${props.formKey}`,
    orderStage: stageConfig.value?.stageConfigList || [],
  });
  const { propsRes, propsEvent, tableQueryParams, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();

  const isAdvancedSearchMode = ref(false);
  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchData(val?: string) {
    setLoadListParams({
      keyword: val ?? keyword.value,
      viewId: activeTab.value,
      ...(props.formKey === FormDesignKeyEnum.CONTRACT_ORDER ? { contractId: props.sourceId } : {}),
      ...(props.formKey === FormDesignKeyEnum.CUSTOMER_ORDER ? { customerId: props.sourceId } : {}),
    });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  watch(
    () => tableRefreshId.value,
    () => {
      checkedRowKeys.value = [];
      searchData();
    }
  );

  onBeforeMount(() => {
    if (props.isContractTab || props.isCustomerTab) {
      searchData();
    }
  });

  watch(
    () => activeTab.value,
    (val) => {
      if (val) {
        checkedRowKeys.value = [];
        setLoadListParams({
          keyword: keyword.value,
          viewId: activeTab.value,
          ...(props.formKey === FormDesignKeyEnum.CONTRACT_ORDER ? { contractId: props.sourceId } : {}),
          ...(props.formKey === FormDesignKeyEnum.CUSTOMER_ORDER ? { customerId: props.sourceId } : {}),
        });
        crmTableRef.value?.setColumnSort(val);
      }
    }
  );

  onMounted(async () => {
    if (route.query.id && !(props.isContractTab || props.isCustomerTab)) {
      activeSourceId.value = route.query.id as string;
      showDetailDrawer.value = true;
    }
  });
</script>
