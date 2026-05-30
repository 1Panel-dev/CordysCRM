<template>
  <CrmDrawer
    v-model:show="show"
    width="100%"
    :ok-text="t('clue.relationCustomer')"
    :ok-disabled="checkedRowKeys.length === 0"
    :title="t('clue.linkAccount')"
    no-padding
    @cancel="handleCancel"
    @confirm="handleConfirm"
  >
    <div class="h-full px-[24px] pt-[24px]">
      <CrmTable
        ref="crmTableRef"
        v-model:checked-row-keys="checkedRowKeys"
        v-bind="propsRes"
        class="crm-clue-convert-table"
        :not-show-table-filter="isAdvancedSearchMode"
        @page-change="propsEvent.pageChange"
        @page-size-change="propsEvent.pageSizeChange"
        @sorter-change="propsEvent.sorterChange"
        @filter-change="propsEvent.filterChange"
        @refresh="searchData"
      >
        <template #tableTop>
          <div class="flex items-center justify-end gap-[12px]">
            <div class="flex gap-[12px]">
              <CrmAdvanceFilter
                ref="tableAdvanceFilterRef"
                v-model:keyword="keyword"
                :custom-fields-config-list="filterConfigList"
                :filter-config-list="customFieldsFilterConfig"
                @adv-search="handleAdvSearch"
                @keyword-search="searchData"
              />
            </div>
          </div>
        </template>
      </CrmTable>
    </div>
  </CrmDrawer>
  <customerOverviewDrawer v-model:show="showOverviewDrawer" :source-id="activeSourceId" @saved="searchData" />
</template>

<script setup lang="ts">
  import { DataTableRowKey, useMessage } from 'naive-ui';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import customerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';

  import { reTransitionCustomer } from '@/api/modules';
  import { baseFilterConfigList } from '@/config/clue';
  import useFormCreateTable from '@/hooks/useFormCreateTable';

  const props = defineProps<{
    clueIds: (string | number)[];
  }>();
  const emit = defineEmits<{
    (e: 'new', linkForm: Record<string, any>): void;
    (e: 'finish'): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const show = defineModel<boolean>('show', {
    default: false,
  });

  const showOverviewDrawer = ref(false);
  const activeSourceId = ref('');
  const tableAdvanceFilterRef = ref<InstanceType<typeof CrmAdvanceFilter>>();
  const isAdvancedSearchMode = computed(() => tableAdvanceFilterRef.value?.isAdvancedSearchMode);
  const keyword = ref('');
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const { useTableRes, customFieldsFilterConfig } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER,
    containerClass: '.crm-clue-convert-table',
    disabledSelection: (row: any) => {
      return row.collaborationType === 'READ_ONLY';
    },
    radio: true, // 单选模式
    specialRender: {
      name: (row: any) => {
        return h(CrmNameTooltip, {
          text: row.name,
        });
      },
    },
  });
  const { propsRes, propsEvent, loadList, setLoadListParams, setAdvanceFilter } = useTableRes;
  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  function searchData(val?: string) {
    setLoadListParams({ keyword: val ?? keyword.value });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function handleAdvSearch(filter: FilterResult) {
    keyword.value = '';
    setAdvanceFilter(filter);
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

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
        checkable: true,
        showContainChildModule: true,
        type: 'department',
        containChildIds: [],
      },
    },
    {
      title: t('customer.lastFollowUps'),
      dataIndex: 'follower',
      type: FieldTypeEnum.USER_SELECT,
    },
    {
      title: t('customer.lastFollowUpDate'),
      dataIndex: 'followTime',
      type: FieldTypeEnum.TIME_RANGE_PICKER,
    },
    ...baseFilterConfigList,
  ]);

  async function handleConfirm() {
    try {
      await reTransitionCustomer({
        clueIds: props.clueIds,
        customerId: checkedRowKeys.value[0] as string,
      });
      Message.success(t('clue.convertToCustomerSuccess'));
      emit('finish');
      show.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function handleCancel() {
    crmTableRef.value?.scrollTo({ top: 0 });
    checkedRowKeys.value = [];
    keyword.value = '';
    propsRes.value.data = []; // 手动重置数据，避免下次进入后导致表格虚拟滚动定位问题
  }

  watch(
    () => show.value,
    (newVal) => {
      if (newVal) {
        searchData();
      }
    },
    { immediate: true }
  );
</script>

<style lang="less" scoped></style>
