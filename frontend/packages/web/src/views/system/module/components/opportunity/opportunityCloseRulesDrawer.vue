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
      <div class="h-full bg-[var(--text-n10)] p-[16px]">
        <div class="mb-[16px] flex items-center justify-between">
          <n-button class="mr-[12px]" type="primary" @click="addRule">
            {{ t('module.businessManage.addRules') }}
          </n-button>
          <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
        </div>
        <CrmTable
          v-bind="propsRes"
          @page-change="propsEvent.pageChange"
          @page-size-change="propsEvent.pageSizeChange"
          @sorter-change="propsEvent.sorterChange"
          @filter-change="propsEvent.filterChange"
        />
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

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import AddRuleDrawer from './addRuleDrawer.vue';

  import { deleteOpportunity, getOpportunityList, switchOpportunityStatus } from '@/api/modules/system/module';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { characterLimit } from '@/utils';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import type { OpportunityItem } from '@lib/shared/models/system/module';

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
      case 'pop-edit':
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
      width: 200,
      sortOrder: false,
      sorter: true,
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
      showInTable: true,
      filterOptions: [
        {
          label: t('common.enable'),
          value: 1,
        },
        {
          label: t('common.disable'),
          value: 0,
        },
      ],
      filter: true,
      render: (row: OpportunityItem) => {
        return h(NSwitch, {
          value: row.enable,
          onClick: () => {
            handleToggleRuleStatus(row);
          },
        });
      },
    },
    {
      title: t('opportunity.admin'),
      key: 'owners',
      width: 150,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
    {
      title: t('role.member'),
      key: 'members',
      width: 150,
      sortOrder: false,
      sorter: true,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
    {
      title: t('opportunity.autoClose'),
      key: 'auto',
      ellipsis: {
        tooltip: true,
      },
      render: (row: OpportunityItem) => {
        return row.auto ? t('common.yes') : t('common.no');
      },
      width: 100,
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.creator'),
      key: 'createUser',
      width: 100,
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
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
    {
      key: 'operation',
      width: 100,
      fixed: 'right',
      render: (row: OpportunityItem) =>
        h(CrmOperationButton, {
          groupList: [
            {
              label: t('common.edit'),
              key: 'edit',
              popConfirmProps: {
                loading: false,
                title: t('common.updateConfirmTitle'),
                content: t('opportunity.updateRuleContentTip'),
                positiveText: t('common.updateConfirm'),
              },
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

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(getOpportunityList, {
    tableKey: TableKeyEnum.MODULE_OPPORTUNITY_RULE_TABLE,
    showSetting: true,
    columns,
    scrollX: 1600,
  });

  function handleCancel() {
    ruleRecord.value = undefined;
  }

  function initOpportunityList() {
    setLoadListParams({
      keyword: keyword.value,
    });
    loadList();
  }

  function searchData(val: string) {
    keyword.value = val;
    initOpportunityList();
  }

  onBeforeMount(() => {
    loadList();
  });

  watch(
    () => tableRefreshId.value,
    (val) => {
      if (val) {
        initOpportunityList();
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
