<template>
  <CrmDrawer
    v-model:show="visible"
    resizable
    no-padding
    :default-width="1000"
    :footer="false"
    class="min-w-[1000px]"
    :title="t('common.search')"
  >
    <div class="flex h-full flex-col overflow-hidden px-[24px] pt-[24px]">
      <div class="mb-[16px] flex items-center justify-between gap-[12px]">
        <CrmSearchInput
          v-model:value="keyword"
          class="crm-duplicateCheck-search !w-full"
          auto-search
          :debounce-time="500"
          :placeholder="t('workbench.duplicateCheck.inputPlaceholder')"
          @search="(val) => refresh(val)"
        />
        <searchSettingButton
          v-model:config-list="configList"
          @init="initAdvanceConfig"
          @init-config-list="initConfigList"
        />
        <n-button
          v-if="lastScopedOptions.length > 0"
          class="n-btn-outline-primary"
          type="primary"
          ghost
          @click="() => openGlobalSearch()"
        >
          {{ t('workbench.duplicateCheck.advanced') }}
        </n-button>
      </div>
      <!-- 查询结果 -->
      <n-spin v-if="keyword.length" :show="loading" class="flex-1 overflow-hidden" content-class="flex flex-col">
        <div class="flex gap-[8px]">
          <CrmTag
            v-for="(item, index) of displayConfigList"
            :key="`${item.value}-${index}`"
            :type="activeConfigValue === item.value ? 'primary' : 'default'"
            theme="light"
            class="cursor-pointer !px-[12px]"
            size="large"
            tooltip-disabled
            @click="clickTag(item)"
          >
            <span>
              {{ item.label }}
              <span
                :class="`${activeConfigValue === item.value ? 'text-[var(--primary-8)]' : 'text-[var(--text-n4)]'}`"
              >
                ({{ moduleCount?.[item.value] }})
              </span>
            </span>
          </CrmTag>
        </div>

        <CrmTable
          v-if="displayConfigList.length"
          ref="crmTableRef"
          class="crm-search-table"
          v-bind="useTableRes.propsRes.value"
          :columns="columns"
          @page-size-change="useTableRes.propsEvent.value.pageSizeChange"
          @sorter-change="useTableRes.propsEvent.value.sorterChange"
          @filter-change="useTableRes.propsEvent.value.filterChange"
          @page-change="useTableRes.propsEvent.value.pageChange"
        />
      </n-spin>
    </div>
  </CrmDrawer>

  <CrmDrawer v-model:show="showDetailDrawer" :width="800" :footer="false" :title="activeCustomer?.name">
    <RelatedTable
      ref="detailTableRef"
      :api="detailType === 'opportunity' ? advancedSearchOptDetail : getAdvancedSearchClueDetail"
      :columns="relatedColumns"
      :title="
        detailType === 'opportunity'
          ? t('workbench.duplicateCheck.relatedOpportunity')
          : t('workbench.duplicateCheck.relatedClue')
      "
      class="crm-detail-related-table"
    />
  </CrmDrawer>
  <GlobalSearchDrawer
    v-model:visible="showGlobalSearchDrawer"
    :keyword="keyword"
    :form-key="globalSearchFormKey"
    @close="handleClose"
    @show-count-detail="(row:any,type:'opportunity' | 'clue')=>showDetail(row,type)"
  />
</template>

<script setup lang="ts">
  import { NButton, NSpin } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { DefaultSearchSetFormModel, ModuleNavBaseInfoItem } from '@lib/shared/models/system/module';

  import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import GlobalSearchDrawer from './components/globalSearchDrawer.vue';
  import RelatedTable from './components/relatedTable.vue';
  import searchSettingButton from './searchConfig/index.vue';

  import { advancedSearchOptDetail, getAdvancedSearchClueDetail, getGlobalModuleCount } from '@/api/modules';
  import { lastOpportunitySteps } from '@/config/opportunity';
  import useAppStore from '@/store/modules/app';
  import { hasAnyPermission } from '@/utils/permission';

  import { defaultSearchSetFormModel, lastScopedOptions, ScopedOptions } from './config';
  import type { SearchTableKey } from './useSearchTable';
  import useSearchTable from './useSearchTable';

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();
  const appStore = useAppStore();

  const keyword = ref('');

  const loading = ref(false);
  const moduleCount = ref<Record<string, number>>();
  async function getCountList(val: string) {
    if (!val) return;
    try {
      loading.value = true;
      const searchTerm = val.replace(/[\s\uFEFF\xA0]+/g, '');
      const res = await getGlobalModuleCount(searchTerm);
      moduleCount.value = res.reduce<Record<string, number>>((acc, item) => {
        acc[item.key] = item.count;
        return acc;
      }, {});
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  const tableRefreshId = ref(0);

  const formModel = ref<DefaultSearchSetFormModel>(cloneDeep(defaultSearchSetFormModel)); // 设置里的值
  const allFieldMap = ref<Record<string, FilterFormItem[]>>({}); // 所有可匹配的字段列表
  function initAdvanceConfig(val: Record<string, any>, form: DefaultSearchSetFormModel) {
    allFieldMap.value = val.value;
    formModel.value = form;
    tableRefreshId.value += 1;
  }

  const configList = ref<ScopedOptions[]>([]); // 横向标签列表
  const displayConfigList = computed(() => {
    const enableList = configList.value.filter((e: ScopedOptions) =>
      appStore.moduleConfigList.find(
        (m: ModuleNavBaseInfoItem) => m.moduleKey === e.moduleKey && m.enable && hasAnyPermission(e.permission)
      )
    );
    if (!formModel.value.resultDisplay) {
      return enableList;
    }
    return enableList.filter((item) => moduleCount.value?.[item.value]);
  });
  const activeConfigValue = ref<SearchTableKey>(lastScopedOptions.value[0].value as SearchTableKey); // 当前选中的标签

  function initConfigList() {
    activeConfigValue.value = displayConfigList.value[0]?.value as SearchTableKey;
  }
  function clickTag(config: ScopedOptions) {
    activeConfigValue.value = config.value as SearchTableKey;
  }

  const { useTableRes, columns, openNewPageOpportunity, openNewPageClue } = await useSearchTable({
    searchTableKey: activeConfigValue,
    fieldList: computed(() => allFieldMap.value[activeConfigValue.value] ?? []),
    selectedFieldIdList: computed(() => formModel.value.searchFields[activeConfigValue.value] ?? []),
  });

  const activeCustomer = ref();
  const showDetailDrawer = ref(false);
  const detailType = ref<'opportunity' | 'clue'>('clue');

  const opportunityColumns: CrmDataTableColumn[] = [
    {
      title: t('opportunity.name'),
      key: 'name',
      width: 100,
      fieldId: 'name',
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => {
        if (!row.hasPermission) return row.name;
        return h(
          CrmTableButton,
          {
            onClick: () => {
              openNewPageOpportunity(row);
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
    },
    {
      title: t('opportunity.customerName'),
      key: 'customerName',
      fieldId: 'customerId',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('opportunity.intendedProducts'),
      key: 'productNames',
      fieldId: 'products',
      width: 100,
      isTag: true,
    },
    {
      title: t('opportunity.stage'),
      width: 100,
      key: 'stage',
      render: (row) => {
        const step = lastOpportunitySteps.find((e: any) => e.value === row.stage);
        return step ? step.label : '-';
      },
    },
    {
      title: t('common.head'),
      key: 'ownerName',
      fieldId: 'owner',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
  ];

  const clueColumns: CrmDataTableColumn[] = [
    {
      title: t('crmFollowRecord.companyName'),
      key: 'name',
      fieldId: 'name',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => {
        if (!row.hasPermission) return row.name;
        return h(
          CrmTableButton,
          {
            onClick: () => {
              openNewPageClue(row);
            },
          },
          { default: () => row.name, trigger: () => row.name }
        );
      },
    },
    {
      title: t('common.head'),
      key: 'ownerName',
      fieldId: 'owner',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('opportunity.intendedProducts'),
      key: 'productNameList',
      fieldId: 'products',
      width: 100,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
  ];
  const relatedColumns = computed(() => {
    const fieldList =
      allFieldMap.value[
        detailType.value === 'clue'
          ? FormDesignKeyEnum.SEARCH_ADVANCED_CLUE
          : FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY
      ];

    // title替换
    const resultColumns = (detailType.value === 'clue' ? clueColumns : opportunityColumns).map((item) => {
      const title = fieldList.find((i) => i.dataIndex === item.fieldId)?.title;
      return {
        ...item,
        title: title ?? item.title,
      };
    });
    return resultColumns;
  });

  const detailTableRef = ref<InstanceType<typeof RelatedTable>>();
  function showDetail(row: any, type: 'opportunity' | 'clue') {
    activeCustomer.value = row;
    detailType.value = type;
    showDetailDrawer.value = true;
    nextTick(() => {
      detailTableRef.value?.searchData(row.name, row.id);
    });
  }

  const searchData = async (val: string) => {
    if (!val) return;
    const searchTerm = val.replace(/[\s\uFEFF\xA0]+/g, '');
    useTableRes.setLoadListParams({ keyword: searchTerm });
    await useTableRes.loadList();
  };

  async function refresh(val: string) {
    await getCountList(val);
    if (!displayConfigList.value.some((item) => item.value === activeConfigValue.value)) {
      initConfigList();
    }
    searchData(val);
  }

  watch(
    () => tableRefreshId.value,
    async () => {
      await refresh(keyword.value);
    }
  );

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  watch(
    () => activeConfigValue.value,
    () => {
      nextTick(() => {
        searchData(keyword.value);
        crmTableRef.value?.scrollTo({ top: 0 });
      });
    }
  );

  watch(
    () => visible.value,
    (val) => {
      if (!val) {
        keyword.value = '';
      }
      nextTick(() => {
        (document.querySelector('.crm-duplicateCheck-search input') as HTMLInputElement)?.focus();
      });
    }
  );

  const showGlobalSearchDrawer = ref(false);

  const globalSearchFormKey = ref();
  function openGlobalSearch(value?: FormDesignKeyEnum) {
    globalSearchFormKey.value = value;
    showGlobalSearchDrawer.value = true;
  }

  function handleClose() {
    globalSearchFormKey.value = undefined;
  }
</script>
