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
        <n-button class="mb-[16px] mr-[12px]" type="primary" @click="addRule">
          {{ t('module.businessManage.addRules') }}
        </n-button>
        <CrmTable
          v-bind="propsRes"
          @page-change="propsEvent.pageChange"
          @page-size-change="propsEvent.pageSizeChange"
          @sorter-change="propsEvent.sorterChange"
          @filter-change="propsEvent.filterChange"
        />
      </div>
      <AddRuleDrawer v-model:visible="showAddRuleDrawer" />
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NSwitch, useMessage } from 'naive-ui';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmTableDataItem } from '@/components/pure/crm-table/type';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import AddRuleDrawer from './addRuleDrawer.vue';

  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { characterLimit } from '@/utils';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import type { CommonList } from '@lib/shared/models/common';

  const { openModal } = useModal();
  const Message = useMessage();

  const { t } = useI18n();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const groupList = ref([
    {
      label: t('common.edit'),
      key: 'edit',
    },
    {
      label: 'more',
      key: 'more',
      slotName: 'more',
    },
  ]);

  const moreOperationList = ref<ActionsItem[]>([
    {
      label: t('common.delete'),
      key: 'delete',
      danger: true,
    },
  ]);

  function addOrEditRule(row: any) {}
  // 删除规则
  function deleteRule(row: any) {
    const hasData = false;

    const title = hasData
      ? t('opportunity.deleteRulesTitle')
      : t('common.deleteConfirmTitle', { name: characterLimit(row.userName) });

    const content = hasData ? '' : t('org.deleteMemberTipContent');

    const positiveText = t(hasData ? 'opportunity.gotIt' : 'common.confirm');

    const negativeText = t(hasData ? 'opportunity.goMove' : 'common.cancel');

    openModal({
      type: 'error',
      title,
      content,
      positiveText,
      negativeText,
      onPositiveClick: async () => {
        try {
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        addOrEditRule(row);
        break;
      case 'delete':
        deleteRule(row);
        break;
      default:
        break;
    }
  }

  // 切换规则状态
  function handleToggleRuleStatus(row: any) {
    const isEnabling = !row.enable;
    const title = t(isEnabling ? 'common.confirmEnableTitle' : 'common.confirmDisabledTitle', {
      name: characterLimit(row.userName),
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
      key: 'userName',
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
      // TODO 类型
      render: (row: any) => {
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
      key: 'admin',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('opportunity.autoClose'),
      key: 'autoClose',
      ellipsis: {
        tooltip: true,
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
      render: (row: any) => {
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
      // TODO 类型
      render: (row: any) =>
        h(CrmOperationButton, {
          groupList: groupList.value,
          moreList: moreOperationList.value,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  function initData() {
    const data: CommonList<CrmTableDataItem<any>> = {
      total: 11,
      pageSize: 10,
      current: 1,
      list: [
        {
          id: '11',
          num: 'string',
          title: 'string',
          enable: false,
          updateTime: null,
          createTime: null,
        },
        {
          id: '22',
          num: '232324323',
          title: '222',
          enable: false,
          updateTime: null,
          createTime: null,
        },
      ],
    };
    return new Promise<CommonList<CrmTableDataItem<any>>>((resolve) => {
      setTimeout(() => {
        resolve(data);
      }, 200);
    });
  }

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(initData, {
    tableKey: TableKeyEnum.MODULE_OPPORTUNITY_RULE_TABLE,
    showSetting: true,
    columns,
    scrollX: 1600,
  });

  const showAddRuleDrawer = ref<boolean>(false);
  function addRule() {
    showAddRuleDrawer.value = true;
  }

  onBeforeMount(() => {
    loadList();
  });
</script>

<style scoped lang="less">
  .business-close-rule {
    padding: 16px;
    background: var(--text-n9);
    @apply h-full w-full;
  }
</style>
