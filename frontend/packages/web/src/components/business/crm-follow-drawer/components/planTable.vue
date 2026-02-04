<template>
  <CrmCard hide-footer no-content-bottom-padding>
    <CrmTable
      ref="crmTableRef"
      v-bind="propsRes"
      class="crm-plan-table"
      :not-show-table="activeShowType === 'timeline'"
      :action-config="{ baseAction: [] }"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    >
      <template #actionLeft>
        <div class="flex gap-[12px]">
          <CrmTab v-model:active-tab="activeStatus" no-content :tab-list="statusTabList" type="segment" />
        </div>
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
      <template #view>
        <CrmViewSelect
          v-model:active-tab="activeTab"
          :type="FormDesignKeyEnum.FOLLOW_PLAN"
          :custom-fields-config-list="filterConfigList"
          :filter-config-list="customFieldsFilterConfig"
          :advanced-original-form="advancedOriginalForm"
          @refresh-table-data="searchData"
          @generated-chart="handleGeneratedChart"
        />
      </template>
      <template v-if="activeShowType === 'timeline'" #other>
        <div class="flex-1">
          <FollowRecord
            v-model:data="propsRes.data"
            :loading="propsRes.loading"
            :virtual-scroll-height="isFullScreen ? 'calc(100vh - 143px)' : 'calc(100vh - 289px)'"
            :get-description-fun="getDescriptionFun"
            key-field="id"
            :disabled-open-detail="false"
            type="followPlan"
            :get-disabled-fun="() => true"
            :empty-text="t('crmFollowRecord.noFollowPlan')"
            @reach-bottom="handleReachBottom"
            @change="changePlanStatus"
          >
            <template #titleLeft="{ item }">
              <CrmTag type="primary" theme="light"> {{ item.type }} </CrmTag>
            </template>
            <template #headerAction="{ item }">
              <div class="flex items-center gap-[4px]">
                <n-button type="primary" class="text-btn-primary" quaternary @click="handleDetail(item)">
                  {{ t('common.detail') }}
                </n-button>
                <n-button type="primary" class="text-btn-primary" quaternary @click="handleEdit(item)">
                  {{ t('common.edit') }}
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
        </div>
      </template>
    </CrmTable>

    <CrmFormCreateDrawer
      v-model:visible="formDrawerVisible"
      :form-key="FormDesignKeyEnum.FOLLOW_PLAN"
      :source-id="realFollowSourceId"
      need-init-detail
      :other-save-params="otherFollowRecordSaveParams"
      @saved="handleAfterSave"
    />
    <DetailDrawer
      v-model:show="showDetailDrawer"
      :form-key="FormDesignKeyEnum.FOLLOW_PLAN"
      :source-id="sourceId"
      :source-name="sourceName"
      :refresh-key="refreshKey"
      @delete="handleDelete(sourceId)"
      @edit="handleEdit(activeItem)"
    />
  </CrmCard>
</template>

<script setup lang="ts">
  import { NButton, NTabPane, NTabs, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { EQUAL, NOT_EQUAL } from '@/components/pure/crm-advance-filter/index';
  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { Description } from '@/components/pure/crm-detail-card/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import { descriptionList, statusTabList } from '@/components/business/crm-follow-detail/config';
  import FollowRecord from '@/components/business/crm-follow-detail/followRecord.vue';
  import StatusTagSelect from '@/components/business/crm-follow-detail/statusTagSelect.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import CrmViewSelect from '@/components/business/crm-view-select/index.vue';
  import DetailDrawer from './detailDrawer.vue';

  import { deleteFollowPlan, updateFollowPlanStatus } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useLocalForage from '@/hooks/useLocalForage';
  import useModal from '@/hooks/useModal';
  import useOpenDetailPage from '@/hooks/useOpenDetailPage';

  const { t } = useI18n();
  const Message = useMessage();
  const { setItem, getItem } = useLocalForage();
  const { goDetail } = useOpenDetailPage();
  const { openModal } = useModal();

  const activeTab = ref('');

  const keyword = ref('');
  const activeStatus = ref<CustomerFollowPlanStatusEnum>(CustomerFollowPlanStatusEnum.ALL);

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
      {
        title: t('eventDrawer.record.converted'),
        type: FieldTypeEnum.SELECT,
        dataIndex: 'converted',
        operatorOption: [EQUAL, NOT_EQUAL],
        selectProps: {
          options: [
            { value: true, label: t('common.yes') },
            { value: 'false', label: t('common.no') },
          ],
        },
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
      label: t('common.edit'),
      key: 'edit',
    },
    {
      label: t('common.delete'),
      key: 'delete',
    },
  ];

  const tableRefreshId = ref(0);

  async function changePlanStatus(item: any) {
    try {
      await updateFollowPlanStatus({
        id: item.id,
        status: item.status,
      });
      Message.success(t('common.operationSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const refreshKey = ref(0);
  const sourceId = ref('');
  const sourceName = ref('');
  const showDetailDrawer = ref(false);
  const activeItem = ref<any>();

  function handleDetail(row: any) {
    sourceId.value = row.id;
    activeItem.value = row;
    sourceName.value = row.resourceType === 'CLUE' ? row.clueName : row.customerName;
    showDetailDrawer.value = true;
  }

  // 删除
  async function handleDelete(id: string) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirm'),
      positiveText: t('common.confirmDelete'),
      content: t('common.deleteConfirmContent'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteFollowPlan(id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
          if (showDetailDrawer.value) {
            showDetailDrawer.value = false;
          }
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  const formDrawerVisible = ref(false);
  const otherFollowRecordSaveParams = ref({
    converted: false,
  });
  const realFollowSourceId = ref<string | undefined>('');
  function handleEdit(item: any) {
    realFollowSourceId.value = item.id;
    formDrawerVisible.value = true;
    otherFollowRecordSaveParams.value.converted = item.converted;
  }

  function handleAfterSave() {
    if (showDetailDrawer.value) {
      refreshKey.value += 1;
    }
    tableRefreshId.value += 1;
  }

  function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'detail':
        handleDetail(row);
        break;
      case 'delete':
        handleDelete(row.id);
        break;
      case 'edit':
        handleEdit(row);
        break;
      default:
        break;
    }
  }

  const { useTableRes, customFieldsFilterConfig } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.FOLLOW_PLAN,
    containerClass: '.crm-plan-table',
    hiddenRefresh: true,
    operationColumn: {
      key: 'operation',
      width: 140,
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
      status: (row: any) => {
        return h(StatusTagSelect, {
          'status': row.status,
          'disabled': !!row.converted,
          'onUpdate:status': (val) => {
            row.status = val;
          },
          'onChange': () => {
            changePlanStatus(row);
          },
        });
      },
    },
  });
  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const isFullScreen = computed(() => crmTableRef.value?.isFullScreen);
  const isAdvancedSearchMode = ref(false);
  const advancedOriginalForm = ref<FilterForm | undefined>();
  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    keyword.value = '';
    advancedOriginalForm.value = originalForm;
    isAdvancedSearchMode.value = isAdvancedMode;
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchData(_keyword?: string) {
    setLoadListParams({
      keyword: _keyword ?? keyword.value,
      viewId: activeTab.value,
      status: activeStatus.value,
    });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  const tableAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();
  function handleGeneratedChart(res: FilterResult, form: FilterForm) {
    advancedOriginalForm.value = form;
    setAdvanceFilter(res);
    tableAdvanceFilterRef.value?.setAdvancedFilter(res, true);
    searchData();
  }

  function searchByKeyword(val: string) {
    keyword.value = val;
    nextTick(() => {
      searchData();
    });
  }

  watch([() => activeTab.value, () => tableRefreshId.value, () => activeStatus.value], () => {
    searchData();
  });

  const activeShowType = ref<'table' | 'timeline'>();
  watch(
    () => activeShowType.value,
    async (val) => {
      if (val) {
        await setItem(`plan-active-show-type`, activeShowType.value as 'table' | 'timeline');
      }
    }
  );
  onMounted(async () => {
    activeShowType.value = (await getItem<'timeline' | 'table'>(`plan-active-show-type`)) ?? 'table';
  });

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
