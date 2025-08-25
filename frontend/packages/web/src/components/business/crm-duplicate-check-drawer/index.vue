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
    <div class="p-[24px]">
      <div class="mb-[16px] flex items-center justify-between gap-[12px]">
        <CrmSearchInput
          v-model:value="keyword"
          class="!w-full"
          auto-search
          :debounce-time="500"
          :placeholder="t('workbench.duplicateCheck.inputPlaceholder')"
          @search="(val) => searchData(val)"
        />
        <searchSettingButton v-model:config-list="configList" @init="initAdvanceConfig" />
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
      <template v-if="keyword.length">
        <div class="flex gap-[8px]">
          <CrmTag
            v-for="(item, index) of configList"
            :key="`${item.value}-${index}`"
            :type="activeConfigValue === item.value ? 'primary' : 'default'"
            theme="light"
            class="!px-[12px]"
            size="large"
            @click="clickTag(item)"
          >
            <span>
              {{ item.label }}
              <span
                :class="`${activeConfigValue === item.value ? 'text-[var(--primary-8)]' : 'text-[var(--text-n4)]'}`"
              >
                ({{ useTableRes.propsRes.value.crmPagination?.itemCount }})</span
              >
            </span>
          </CrmTag>
        </div>

        <CrmTable
          v-bind="useTableRes.propsRes.value"
          @page-size-change="useTableRes.propsEvent.value.pageSizeChange"
          @sorter-change="useTableRes.propsEvent.value.sorterChange"
          @filter-change="useTableRes.propsEvent.value.filterChange"
          @page-change="useTableRes.propsEvent.value.pageChange"
        />
      </template>
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
  import { NButton } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import GlobalSearchDrawer from './components/globalSearchDrawer.vue';
  import RelatedTable from './components/relatedTable.vue';
  import searchSettingButton from './searchConfig/index.vue';

  import { advancedSearchOptDetail, getAdvancedSearchClueDetail } from '@/api/modules';

  import { DefaultSearchSetFormModel, defaultSearchSetFormModel, lastScopedOptions, ScopedOptions } from './config';
  import type { SearchTableKey } from './useSearchTable';
  import useSearchTable from './useSearchTable';

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();

  const keyword = ref('');

  const configList = ref<ScopedOptions[]>([]); // 横向标签列表
  const activeConfigValue = ref<SearchTableKey>(FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER); // 当前选中的标签
  function clickTag(config: ScopedOptions) {
    activeConfigValue.value = config.value as SearchTableKey;
  }

  const formModel = ref<DefaultSearchSetFormModel>(cloneDeep(defaultSearchSetFormModel)); // 设置里的值
  const searchFieldMap = ref<Record<string, FilterFormItem[]>>({}); // 所有可匹配的字段列表
  function initAdvanceConfig(val: Record<string, any>, form: DefaultSearchSetFormModel) {
    searchFieldMap.value = val.value;
    formModel.value = form;
  }

  const { useTableRes } = await useSearchTable({
    searchTableKey: activeConfigValue.value,
    fieldList: searchFieldMap.value[activeConfigValue.value] ?? [],
    selectedFieldKeyList: formModel.value.searchFields[activeConfigValue.value] ?? [],
  });

  const activeCustomer = ref();
  const showDetailDrawer = ref(false);
  const detailType = ref<'opportunity' | 'clue'>('clue');

  // TODO lmy
  const relatedColumns = ref([]);

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

  watch(
    () => visible.value,
    (val) => {
      if (!val) {
        keyword.value = '';
      }
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
