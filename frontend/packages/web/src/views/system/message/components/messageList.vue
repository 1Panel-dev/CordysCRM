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

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { MessageConfigItem } from '@lib/shared/models/system/message';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import SwitchPopConfirm from './switchPopConfirm.vue';

  import { batchSaveMessageTask, checkSyncUserFromThird, getMessageTask, saveMessageTask } from '@/api/modules';
  import { hasAnyPermission } from '@/utils/permission';

  const Message = useMessage();

  const { t } = useI18n();

  const enableSystemMessage = ref(false);
  const enableEmailMessage = ref(false);
  const enableWeChatMessage = ref(false);
  const enableSystemLoading = ref(false);

  const data = ref<MessageConfigItem[]>([]);
  const loading = ref(false);

  async function initMessageList() {
    try {
      loading.value = true;
      const result = await getMessageTask();

      enableSystemMessage.value = result.every((e) => e.messageTaskDetailDTOList.every((c) => c.sysEnable));
      enableEmailMessage.value = result.every((e) => e.messageTaskDetailDTOList.every((c) => c.emailEnable));
      enableWeChatMessage.value = result.every((e) => e.messageTaskDetailDTOList.every((c) => c.weComEnable));

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
        weComEnable: type === 'weChat' ? !row.weComEnable : row.weComEnable,
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

      const params: {
        sysEnable: boolean | undefined;
        emailEnable: boolean | undefined;
        weComEnable: boolean | undefined;
      } = {
        sysEnable: undefined,
        emailEnable: undefined,
        weComEnable: undefined,
      };

      if (type === 'system') {
        params.sysEnable = !enableSystemMessage.value;
      } else if (type === 'email') {
        params.emailEnable = !enableEmailMessage.value;
      } else if (type === 'weChat') {
        params.weComEnable = !enableWeChatMessage.value;
      }

      await batchSaveMessageTask(params);
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

  const isSyncFromThirdChecked = ref(false);
  const columns = computed<DataTableColumn[]>(() => [
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
          disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']),
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
          disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']),
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
          disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']),
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
          disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']),
          onChange: (cancel?: () => void) =>
            handleToggleSystemMessage(row as unknown as MessageConfigItem, 'email', cancel),
        });
      },
    },
    {
      title: () => {
        return h(SwitchPopConfirm, {
          title: t('system.message.confirmCloseWeChatNotice'),
          titleColumnText: t('system.message.enterpriseWeChatNotice'),
          value: enableWeChatMessage.value,
          loading: enableSystemLoading.value,
          content: t('system.message.confirmCloseSystemNotifyContent'),
          disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']) || !isSyncFromThirdChecked.value,
          toolTipContent: !isSyncFromThirdChecked.value ? t('system.message.weComSwitchTip') : '',
          onChange: (cancel?: () => void) => toggleGlobalMessage('weChat', cancel),
        });
      },
      key: 'weComEnable',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      render: (row) => {
        return h(SwitchPopConfirm, {
          title: t('system.message.confirmCloseWeChatNotice'),
          value: row.weComEnable as boolean,
          loading: enableSystemLoading.value,
          content: t('system.message.confirmCloseSystemNotifyContent'),
          disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']) || !isSyncFromThirdChecked.value,
          toolTipContent: !isSyncFromThirdChecked.value ? t('system.message.weComSwitchTip') : '',
          onChange: (cancel?: () => void) =>
            handleToggleSystemMessage(row as unknown as MessageConfigItem, 'weChat', cancel),
        });
      },
    },
  ]);

  async function initCheckSyncType() {
    try {
      isSyncFromThirdChecked.value = await checkSyncUserFromThird();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(() => {
    initCheckSyncType();
    initMessageList();
  });
</script>

<style lang="less" scoped></style>
