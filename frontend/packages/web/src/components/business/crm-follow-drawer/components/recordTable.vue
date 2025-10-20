<template>
  <CrmCard hide-footer no-content-bottom-padding :special-height="64">
    <CrmTable
      ref="crmTableRef"
      v-bind="propsRes"
      class="crm-record-table"
      :not-show-table="activeShowType === 'timeline'"
      :action-config="{ baseAction: [] }"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    >
      <template #actionLeft>
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="segment" />
      </template>
      <template #actionRight>
        <CrmAdvanceFilter
          ref="tableAdvanceFilterRef"
          v-model:keyword="keyword"
          :search-placeholder="t('common.searchByName')"
          :custom-fields-config-list="filterConfigList"
          :filter-config-list="customFieldsFilterConfig"
          @adv-search="handleAdvSearch"
          @keyword-search="searchByKeyword"
        />
        <n-tabs v-model:value="activeShowType" type="segment" size="large" class="show-type-tabs">
          <n-tab-pane name="table" class="hidden">
            <template #tab><CrmIcon type="iconicon_list" /></template>
          </n-tab-pane>
          <n-tab-pane name="timeline" class="hidden">
            <template #tab><CrmIcon type="iconicon_timeline" /></template>
          </n-tab-pane>
        </n-tabs>
      </template>
      <template v-if="activeShowType === 'timeline'" #other>
        <FollowRecord
          v-model:data="propsRes.data"
          :loading="propsRes.loading"
          virtual-scroll-height="calc(100vh - 239px)"
          :get-description-fun="getDescriptionFun"
          key-field="id"
          :disabled-open-detail="false"
          type="followRecord"
          :empty-text="t('crmFollowRecord.noFollowRecord')"
          @reach-bottom="handleReachBottom"
        >
          <template #titleLeft="{ item }">
            <CrmTag type="primary" theme="light"> {{ item.type }} </CrmTag>
          </template>
          <template #headerAction="{ item }">
            <div class="flex items-center gap-[4px]">
              <n-button type="primary" class="text-btn-primary" quaternary @click="handleDetail(item)">
                {{ t('common.detail') }}
              </n-button>
              <n-button type="error" class="text-btn-error" quaternary @click="handleDelete(item.id)">
                {{ t('common.delete') }}
              </n-button>
            </div>
          </template>
          <template #createTime="{ descItem }">
            <div class="flex items-center gap-[8px]">
              {{ dayjs(descItem.value).format('YYYY-MM-DD HH:mm:ss') }}
            </div>
          </template>
          <template #updateTime="{ descItem }">
            <div class="flex items-center gap-[8px]">
              {{ dayjs(descItem.value).format('YYYY-MM-DD HH:mm:ss') }}
            </div>
          </template>
        </FollowRecord>
      </template>
    </CrmTable>

    <DetailDrawer
      v-model:show="showDetailDrawer"
      :form-key="FormDesignKeyEnum.FOLLOW_RECORD"
      :source-id="sourceId"
      :source-name="sourceName"
      @delete="handleDelete(sourceId)"
    />
  </CrmCard>
</template>

<script setup lang="ts">
  import { NButton, NTabPane, NTabs, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { Description } from '@/components/pure/crm-detail-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import { descriptionList } from '@/components/business/crm-follow-detail/config';
  import FollowRecord from '@/components/business/crm-follow-detail/followRecord.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import DetailDrawer from './detailDrawer.vue';

  import { deleteFollowRecord } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useHiddenTab from '@/hooks/useHiddenTab';
  import useOpenDetailPage from '@/hooks/useOpenDetailPage';

  const { t } = useI18n();
  const Message = useMessage();
  const { goDetail } = useOpenDetailPage();

  const activeTab = ref('');
  const { tabList, initTab } = useHiddenTab(FormDesignKeyEnum.FOLLOW_RECORD);

  onBeforeMount(async () => {
    await initTab();
    nextTick(() => {
      activeTab.value = tabList.value[0].name as string;
    });
  });

  const keyword = ref('');

  const filterConfigList = computed<FilterFormItem[]>(() => {
    return [
      {
        title: t('opportunity.department'),
        dataIndex: 'departmentId',
        type: FieldTypeEnum.TREE_SELECT,
        treeSelectProps: {
          labelField: 'name',
          keyField: 'id',
          multiple: true,
          clearFilterAfterSelect: false,
          checkable: true,
          showContainChildModule: true,
          type: 'department',
        },
      },
      {
        dataIndex: 'phone',
        title: t('common.phoneNumber'),
        type: FieldTypeEnum.PHONE,
      },
      ...baseFilterConfigList,
    ] as FilterFormItem[];
  });

  const operationGroupList: ActionsItem[] = [
    {
      label: t('common.detail'),
      key: 'detail',
    },
    {
      label: t('common.delete'),
      key: 'delete',
    },
  ];

  const tableRefreshId = ref(0);

  // 删除
  async function handleDelete(id: string) {
    try {
      await deleteFollowRecord(id);
      Message.success(t('common.deleteSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const sourceId = ref('');
  const sourceName = ref('');
  const showDetailDrawer = ref(false);
  function handleDetail(row: any) {
    sourceId.value = row.id;
    sourceName.value = row.resourceType === 'CLUE' ? row.clueName : row.customerName;
    showDetailDrawer.value = true;
  }

  function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'detail':
        handleDetail(row);
        break;
      case 'delete':
        handleDelete(row.id);
        break;
      default:
        break;
    }
  }

  const { useTableRes, customFieldsFilterConfig } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.FOLLOW_RECORD,
    containerClass: '.crm-record-table',
    hiddenAllScreen: true,
    hiddenRefresh: true,
    operationColumn: {
      key: 'operation',
      width: 100,
      fixed: 'right',
      render: (row: any) =>
        h(CrmOperationButton, {
          groupList: operationGroupList,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
    specialRender: {
      name: (row: any) => {
        return h(
          CrmTableButton,
          {
            onClick: () => {
              goDetail(row);
            },
          },
          {
            default: () => (row.resourceType === 'CLUE' ? row.clueName : row.customerName),
            trigger: () => (row.resourceType === 'CLUE' ? row.clueName : row.customerName),
          }
        );
      },
    },
  });
  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const isAdvancedSearchMode = ref(false);
  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean) {
    keyword.value = '';
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchData(_keyword?: string) {
    setLoadListParams({
      keyword: _keyword ?? keyword.value,
      viewId: activeTab.value,
    });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchByKeyword(val: string) {
    keyword.value = val;
    nextTick(() => {
      searchData();
    });
  }

  watch([() => activeTab.value, () => tableRefreshId.value], () => {
    searchData();
  });

  const activeShowType = ref<'table' | 'timeline'>('table');

  function getDescriptionFun(item: any) {
    const isClue = item.resourceType === 'CLUE' && item.clueId?.length;
    const customerNameKey = isClue ? 'clueName' : 'customerName';
    let lastDescriptionList = [
      ...[
        {
          key: customerNameKey,
          label: isClue ? t('crmFollowRecord.companyName') : t('opportunity.customerName'),
          value: customerNameKey,
        },
      ],
      ...descriptionList,
    ];

    if (isClue) {
      lastDescriptionList = lastDescriptionList.filter((e) => !['contactName', 'phone'].includes(e.key));
    }

    return (lastDescriptionList.map((desc: Description) => ({
      ...desc,
      value: item[desc.key as keyof any],
    })) || []) as Description[];
  }

  function handleReachBottom() {
    if (
      propsRes.value.crmPagination?.itemCount &&
      propsRes.value.crmPagination?.page &&
      propsRes.value.crmPagination?.pageSize &&
      propsRes.value.crmPagination.itemCount > propsRes.value.crmPagination.page * propsRes.value.crmPagination.pageSize
    ) {
      propsEvent.value.pageChange(propsRes.value.crmPagination.page + 1);
    }
  }
</script>

<style lang="less" scoped>
  .show-type-tabs {
    :deep(.n-tabs-tab) {
      padding: 6px;
    }
  }
</style>
