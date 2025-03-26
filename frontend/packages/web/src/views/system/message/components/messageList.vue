<template>
  <CrmCard hide-footer :special-height="64">
    <n-data-table
      :single-line="false"
      :columns="columns"
      :data="data"
      :paging="false"
      :pagination="false"
      :loading="loading"
      max-height="calc(100vh - 242px)"
    />
  </CrmCard>
</template>

<script lang="ts" setup>
  import { DataTableColumn, NDataTable, useMessage } from 'naive-ui';

  import type { MessageConfigItem } from '@lib/shared/models/system/message';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import SwitchPopConfirm from './switchPopConfirm.vue';

  import { getMessageTask, saveMessageTask } from '@/api/modules/system/message';
  import { useI18n } from '@/hooks/useI18n';

  const Message = useMessage();

  const { t } = useI18n();

  const enableSystemMessage = ref(true);
  const enableEmailMessage = ref(false);
  const enableSystemLoading = ref(false);
  async function handleToggleSystemMessage(val: boolean, type: string, cancel?: () => void) {
    try {
      enableSystemLoading.value = true;
      // TODO 等待联调
      // await saveMessageTask();
      Message.success(t('common.saveSuccess'));
      cancel?.();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      enableSystemLoading.value = false;
    }
  }

  const columns: DataTableColumn[] = [
    {
      title: t('system.message.Feature'),
      key: 'name',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      rowSpan: (rowData: { messageTaskDetailDTOList?: MessageConfigItem[] }) => {
        return rowData?.messageTaskDetailDTOList?.length ?? 0;
      },
    },
    {
      title: t('system.message.notificationScenario'),
      key: 'eventName',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: () => {
        return h(SwitchPopConfirm, {
          title: t('system.message.confirmCloseSystemNotify'),
          titleColumnText: t('system.message.systemMessage'),
          value: enableSystemMessage.value,
          loading: enableSystemLoading.value,
          content: t('system.message.confirmCloseSystemNotifyContent'),
          onChange: (val: boolean, cancel?: () => void) => handleToggleSystemMessage(val, 'system', cancel),
        });
      },
      key: 'systemMessage',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      render: (row) => {
        return h(SwitchPopConfirm, {
          title: t('system.message.confirmCloseSystemNotify'),
          value: row.enable as boolean,
          loading: enableSystemLoading.value,
          content: t('system.message.confirmCloseSystemNotifyContent'),
          onChange: (val: boolean, cancel?: () => void) => handleToggleSystemMessage(val, 'system', cancel),
        });
      },
    },
    {
      title: () => {
        return h(SwitchPopConfirm, {
          titleColumnText: t('system.message.emailReminder'),
          value: enableEmailMessage.value,
          loading: enableSystemLoading.value,
          onChange: (val: boolean, cancel?: () => void) => handleToggleSystemMessage(val, 'email', cancel),
        });
      },
      key: 'emailReminder',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      render: (row) => {
        return h(SwitchPopConfirm, {
          value: row.enable as boolean,
          loading: enableSystemLoading.value,
          onChange: (val: boolean, cancel?: () => void) => handleToggleSystemMessage(val, 'email', cancel),
        });
      },
    },
  ];

  const data = ref<MessageConfigItem[]>([]);
  const loading = ref(false);

  // 类型 (type)  TODO 暂用
  const typeMapping: Record<string, string> = {
    CUSTOMER: '客户',
    CLUE: '线索',
    BUSINESS: '商机',
    CLUE_POOL: '线索池',
  };

  // 事件 (event)  TODO 暂用
  const eventMapping: Record<string, string> = {
    CUSTOMER_TRANSFERRED_CUSTOMER: '客户转移',
    CUSTOMER_AUTO_MOVED_HIGH_SEAS: '客户自动进入公海',
    CUSTOMER_MOVED_HIGH_SEAS: '客户进入公海',
    CUSTOMER_DELETED: '客户删除',
    CLUE_DISTRIBUTED: '线索分配',
    CLUE_MOVED_POOL: '线索回收',
    CLUE_TRANSFER: '线索转移',
    BUSINESS_TRANSFER: '负责人变更',
  };

  async function initMessageList() {
    try {
      loading.value = true;
      const result = await getMessageTask();
      const lastData: MessageConfigItem[] = result.flatMap((item) =>
        item.messageTaskDetailDTOList.map((child) => ({
          ...child,
          ...item,
          name: typeMapping[item.type],
          eventName: eventMapping[child.event],
        }))
      );
      data.value = lastData;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  onBeforeMount(() => {
    initMessageList();
  });
</script>

<style lang="less" scoped></style>
