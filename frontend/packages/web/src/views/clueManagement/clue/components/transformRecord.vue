<template>
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
    @page-change="() => ({})"
    @page-size-change="() => ({})"
    @sorter-change="() => ({})"
  >
  </CrmTable>
  <customerOverviewDrawer
    v-model:show="showOverviewDrawer"
    :source-id="activeSourceId"
    @saved="init()"
    @deleted="init()"
    @transfer="init()"
    @refresh="init()"
  />
</template>

<script setup lang="ts">
  import { DataTableRowKey } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { transformData } from '@lib/shared/method/formCreate';

  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmApprovalPopover from '@/components/business/crm-approval/components/crm-approval-popover.vue';
  import { fixedFieldKeyListMap } from '@/components/business/crm-duplicate-check-drawer/useSearchTable';
  import customerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';

  import { getClueTransitionCustomerRecordList } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useFormCreateTable from '@/hooks/useFormCreateTable';

  const props = defineProps<{
    resourceId: string;
  }>();

  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const activeSourceId = ref('');

  const { initApprovalPermission, enableApproval, hasApprovalScopedPermission } = useApprovalOperation<
    Record<string, any>
  >({
    formType: FormDesignKeyEnum.CUSTOMER,
    dataActionMap: () => ({}),
  });

  // 概览
  const showOverviewDrawer = ref(false);

  await initApprovalPermission();

  const { columns, fieldList } = await useFormCreateTable({
    formKey: FormDesignKeyEnum.CUSTOMER,
    tableKey: FormDesignKeyEnum.CUSTOMER,
    specialRender: {
      name: (row: any) => {
        if (row.hasPermission === false) {
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
    readonly: true,
    containerClass: '.crm-customer-table',
    enableApproval,
  });

  const data = ref<any[]>([]);
  const loading = ref(false);
  const hasPermission = ref(false);
  const realColumns = computed(() =>
    columns.value.filter((item) =>
      hasPermission.value
        ? true
        : fixedFieldKeyListMap[FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER]?.includes(item.key as string) ||
          ['followTime', 'createTime'].includes(item.key as string)
    )
  );
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
              fields: fieldList.value,
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

  onBeforeMount(() => {
    init();
  });
</script>

<style lang="less" scoped>
  :deep(.n-tabs-scroll-padding) {
    width: 16px !important;
  }
</style>
