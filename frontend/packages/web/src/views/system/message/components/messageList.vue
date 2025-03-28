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

  import { batchSaveMessageTask, getMessageTask, saveMessageTask } from '@/api/modules/system/message';
  import { useI18n } from '@/hooks/useI18n';

  const Message = useMessage();

  const { t } = useI18n();

  const enableSystemMessage = ref(false);
  const enableEmailMessage = ref(false);
  const enableSystemLoading = ref(false);

  const data = ref<MessageConfigItem[]>([]);
  const loading = ref(false);

  async function initMessageList() {
    try {
      loading.value = true;
      const result = await getMessageTask();

      enableSystemMessage.value = result.every((e) => e.messageTaskDetailDTOList.every((c) => c.sysEnable));
      enableEmailMessage.value = result.every((e) => e.messageTaskDetailDTOList.every((c) => c.emailEnable));

      data.value = result
        .map((item) =>
          item.messageTaskDetailDTOList.map((child) => ({
            ...child,
            ...item,
            moduleName: item.moduleName || item.module,
            eventName: child.eventName || child.event,
          }))
        )
        .flat();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  async function handleToggleSystemMessage(row: MessageConfigItem, type: string, cancel?: () => void) {
    try {
      enableSystemLoading.value = true;
      await saveMessageTask({
        module: row.module,
        event: row.event,
        emailEnable: type === 'email' ? !row.emailEnable : row.emailEnable,
        sysEnable: type === 'system' ? !row.sysEnable : row.sysEnable,
      });
      Message.success(t('common.saveSuccess'));
      cancel?.();
      initMessageList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      enableSystemLoading.value = false;
    }
  }

  async function toggleGlobalMessage(type: string, cancel?: () => void) {
    try {
      enableSystemLoading.value = true;
      await batchSaveMessageTask({
        sysEnable: type === 'system' ? !enableSystemMessage.value : enableSystemMessage.value,
        emailEnable: type === 'email' ? !enableEmailMessage.value : enableEmailMessage.value,
      });
      Message.success(t('common.saveSuccess'));
      cancel?.();
      initMessageList();
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
      key: 'moduleName',
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
          onChange: (cancel?: () => void) => toggleGlobalMessage('system', cancel),
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
          value: row.sysEnable as boolean,
          loading: enableSystemLoading.value,
          content: t('system.message.confirmCloseSystemNotifyContent'),
          onChange: (cancel?: () => void) =>
            handleToggleSystemMessage(row as unknown as MessageConfigItem, 'system', cancel),
        });
      },
    },
    {
      title: () => {
        return h(SwitchPopConfirm, {
          titleColumnText: t('system.message.emailReminder'),
          value: enableEmailMessage.value,
          loading: enableSystemLoading.value,
          onChange: (cancel?: () => void) => toggleGlobalMessage('email', cancel),
        });
      },
      key: 'emailReminder',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      render: (row) => {
        return h(SwitchPopConfirm, {
          value: row.emailEnable as boolean,
          loading: enableSystemLoading.value,
          onChange: (cancel?: () => void) =>
            handleToggleSystemMessage(row as unknown as MessageConfigItem, 'email', cancel),
        });
      },
    },
  ];

  onBeforeMount(() => {
    initMessageList();
  });
</script>

<style lang="less" scoped></style>
