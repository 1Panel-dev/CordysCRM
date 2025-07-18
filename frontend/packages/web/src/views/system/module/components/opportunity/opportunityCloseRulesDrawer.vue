<template>
  <CrmDrawer
    v-model:show="visible"
    width="100%"
    :title="t('module.businessManage.businessCloseRule')"
    :footer="false"
    no-padding
    :show-back="true"
  >
    <div class="business-close-rule">
      <div class="h-full bg-[var(--text-n10)] p-[16px] pb-0">
        <CrmTable
          ref="crmTableRef"
          v-bind="propsRes"
          @page-change="propsEvent.pageChange"
          @page-size-change="propsEvent.pageSizeChange"
          @sorter-change="propsEvent.sorterChange"
          @filter-change="propsEvent.filterChange"
        >
          <template #tableTop>
            <div class="flex items-center justify-between">
              <n-button class="mr-[12px]" type="primary" @click="addRule">
                {{ t('module.businessManage.addRules') }}
              </n-button>

              <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
            </div>
          </template>
        </CrmTable>
      </div>
      <AddRuleDrawer
        v-model:visible="showAddRuleDrawer"
        :rows="ruleRecord"
        @load-list="initOpportunityList()"
        @cancel="handleCancel"
      />
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NSwitch, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { OpportunityItem } from '@lib/shared/models/system/module';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import AddRuleDrawer from './addRuleDrawer.vue';

  import { deleteOpportunity, getOpportunityRuleList, switchOpportunityStatus } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const { openModal } = useModal();
  const Message = useMessage();

  const { t } = useI18n();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const keyword = ref<string>('');

  const tableRefreshId = ref(0);

  // 删除规则
  function deleteRule(row: OpportunityItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('opportunity.deleteRuleContent'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteOpportunity(row.id);
          tableRefreshId.value += 1;
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  const showAddRuleDrawer = ref<boolean>(false);
  const ruleRecord = ref<OpportunityItem>();
  function addRule() {
    showAddRuleDrawer.value = true;
  }

  function handleEdit(row: OpportunityItem) {
    ruleRecord.value = cloneDeep(row);
    showAddRuleDrawer.value = true;
  }

  function handleActionSelect(row: OpportunityItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row);
        break;
      case 'delete':
        deleteRule(row);
        break;
      default:
        break;
    }
  }

  // 切换规则状态
  function handleToggleRuleStatus(row: OpportunityItem) {
    const isEnabling = !row.enable;
    const title = t(isEnabling ? 'common.confirmEnableTitle' : 'common.confirmDisabledTitle', {
      name: characterLimit(row.name),
    });

    const content = t(isEnabling ? 'opportunity.enabledRuleTipContent' : 'opportunity.disabledRuleTipContent');

    const positiveText = t(isEnabling ? 'common.confirmStart' : 'common.confirmDisable');

    openModal({
      type: isEnabling ? 'default' : 'error',
      title,
      content,
      positiveText,
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await switchOpportunityStatus(row.id);
          Message.success(t(isEnabling ? 'common.opened' : 'common.disabled'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const columns: CrmDataTableColumn[] = [
    {
      title: t('opportunity.ruleName'),
      key: 'name',
      width: 250,
      sortOrder: false,
      sorter: true,
      fixed: 'left',
      ellipsis: {
        tooltip: true,
      },
      columnSelectorDisabled: true,
    },
    {
      title: t('common.status'),
      key: 'enable',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      filterOptions: [
        {
          label: t('common.enable'),
          value: true,
        },
        {
          label: t('common.disable'),
          value: false,
        },
      ],
      filter: true,
      render: (row: OpportunityItem) => {
        return h(NSwitch, {
          value: row.enable,
          onClick: () => {
            if (!hasAnyPermission(['MODULE_SETTING:UPDATE'])) return;
            handleToggleRuleStatus(row);
          },
        });
      },
    },
    {
      title: t('opportunity.admin'),
      key: 'owners',
      width: 200,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
    {
      title: t('role.member'),
      key: 'members',
      width: 200,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
    {
      title: t('opportunity.autoClose'),
      key: 'auto',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      filterOptions: [
        {
          label: t('common.yes'),
          value: true,
        },
        {
          label: t('common.no'),
          value: false,
        },
      ],
      filter: true,
      render: (row: OpportunityItem) => {
        return row.auto ? t('common.yes') : t('common.no');
      },
      width: 150,
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 200,
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.creator'),
      key: 'createUser',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      render: (row: OpportunityItem) => {
        return h(CrmNameTooltip, { text: row.createUserName });
      },
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 200,
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
    {
      key: 'operation',
      width: 150,
      fixed: 'right',
      render: (row: OpportunityItem) =>
        h(CrmOperationButton, {
          groupList: [
            {
              label: t('common.edit'),
              key: 'edit',
            },
            {
              label: t('common.delete'),
              key: 'delete',
            },
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(getOpportunityRuleList, {
    tableKey: TableKeyEnum.MODULE_OPPORTUNITY_RULE_TABLE,
    showSetting: true,
    columns,
  });

  function handleCancel() {
    ruleRecord.value = undefined;
  }

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  function initOpportunityList() {
    setLoadListParams({
      keyword: keyword.value,
    });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function searchData(val: string) {
    keyword.value = val;
    initOpportunityList();
  }

  watch(
    () => tableRefreshId.value,
    (val) => {
      if (val) {
        initOpportunityList();
      }
    }
  );

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        loadList();
      }
    }
  );
</script>

<style scoped lang="less">
  .business-close-rule {
    padding: 16px;
    background: var(--text-n9);
    @apply h-full w-full;
  }
</style>
