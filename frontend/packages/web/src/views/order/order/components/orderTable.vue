<template>
  <CrmTable
    ref="crmTableRef"
    v-model:checked-row-keys="checkedRowKeys"
    v-bind="propsRes"
    :class="`crm-order-table-${props.formKey}`"
    :not-show-table-filter="isAdvancedSearchMode"
    :fullscreen-target-ref="props.fullscreenTargetRef"
    @page-change="propsEvent.pageChange"
    @page-size-change="propsEvent.pageSizeChange"
    @sorter-change="propsEvent.sorterChange"
    @filter-change="propsEvent.filterChange"
    @refresh="searchData"
  >
    <template #actionLeft>
      <!-- TODO lmy permission  -->
      <n-button v-if="!props.readonly" v-permission="['CLUE_MANAGEMENT:ADD']" type="primary" @click="handleNewClick">
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
        v-if="!props.hiddenAdvanceFilter"
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
    @saved="() => searchData()"
  />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { DataTableRowKey, NButton, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import useLocale from '@lib/shared/locale/useLocale';
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

  import { getOpportunityStageConfig } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const route = useRoute();

  const { t } = useI18n();
  const Message = useMessage();
  const { currentLocale } = useLocale(Message.loading);
  const { openModal } = useModal();

  const props = defineProps<{
    fullscreenTargetRef?: HTMLElement | null;
    hiddenAdvanceFilter?: boolean;
    isContractTab?: boolean;
    sourceId?: string; // 合同详情下
    sourceName?: string;
    readonly?: boolean;
    formKey: FormDesignKeyEnum.ORDER | FormDesignKeyEnum.CONTRACT_ORDER;
  }>();
  const emit = defineEmits<{
    (e: 'openContractDrawer', params: { id: string }): void;
  }>();

  const activeTab = ref();
  const keyword = ref('');

  const statusConfig = ref<any>();
  async function initStageConfig() {
    try {
      // TODO lmy 接口
      statusConfig.value = await getOpportunityStageConfig();
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
      dataIndex: 'status',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      selectProps: {
        // TODO lmy 状态
        options:
          statusConfig.value?.stageConfigList.map((e: any) => ({
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
              permission: ['CONTRACT_PAYMENT_PLAN:UPDATE'], // TODO lmy permission
            },
          ]
        : []),
      {
        label: t('common.download'),
        key: 'download',
        permission: ['CONTRACT_PAYMENT_PLAN:DELETE'], // TODO lmy permission
      },
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

  async function handleActionSelect(row: OrderItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row.id);
        break;
      case 'download':
        // TODO lmy 下载
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
        return props.isContractTab || !hasAnyPermission(['CONTRACT:READ'])
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
    },
    containerClass: `.crm-order-table-${props.formKey}`,
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
    setLoadListParams({ keyword: val ?? keyword.value, viewId: activeTab.value, contractId: props.sourceId });
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
    if (props.isContractTab) {
      searchData();
    }
  });

  watch(
    () => activeTab.value,
    (val) => {
      if (val) {
        checkedRowKeys.value = [];
        setLoadListParams({ keyword: keyword.value, viewId: activeTab.value, contractId: props.sourceId });
        crmTableRef.value?.setColumnSort(val);
      }
    }
  );

  onMounted(async () => {
    if (route.query.id && !props.isContractTab) {
      activeSourceId.value = route.query.id as string;
      showDetailDrawer.value = true;
    }
  });
</script>
