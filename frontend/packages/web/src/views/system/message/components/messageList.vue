<template>
  <CrmCard hide-footer :special-height="licenseStore.expiredDuring ? 128 : 64">
    <n-data-table
      :single-line="false"
      :columns="columns"
      :data="data"
      :paging="false"
      class="message-table"
      :pagination="false"
      :loading="loading"
      :max-height="licenseStore.expiredDuring ? 'calc(100vh - 306px)' : 'calc(100vh - 242px)'"
    />
    <expirationSettingDrawer v-model:visible="showExpirationSetting" :detail="activeDetail" @ok="initMessageList" />
  </CrmCard>
</template>

<script lang="ts" setup>
  import { DataTableColumn, NDataTable, useMessage } from 'naive-ui';

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { MessageConfigItem, MessageTaskDetailDTOItem } from '@lib/shared/models/system/message';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import expirationSettingDrawer from './expirationSettingDrawer.vue';
  import SwitchPopConfirm from './switchPopConfirm.vue';

  import { batchSaveMessageTask, getConfigSynchronization, getMessageTask, saveMessageTask } from '@/api/modules';
  import { platFormNameMap, platformType } from '@/config/business';
  import { useAppStore } from '@/store';
  import useLicenseStore from '@/store/modules/setting/license';
  import { hasAnyPermission } from '@/utils/permission';

  const Message = useMessage();

  const { t } = useI18n();
  const licenseStore = useLicenseStore();
  const appStore = useAppStore();

  const enableSystemMessage = ref(false);
  const enableEmailMessage = ref(false);
  const enableThirdPartyMessage = ref(false);
  const enableSystemLoading = ref(false);
  const noticeEnableMapKey: Record<string, keyof MessageTaskDetailDTOItem> = {
    [CompanyTypeEnum.WECOM]: 'weComEnable',
    [CompanyTypeEnum.DINGTALK]: 'dingTalkEnable',
    [CompanyTypeEnum.LARK]: 'larkEnable',
  };

  const thirdPartyEnableKey = computed<keyof MessageTaskDetailDTOItem>(() => {
    const syncPlatformType = appStore.activePlatformResource.syncResource;
    return noticeEnableMapKey[syncPlatformType];
  });
  const data = ref<MessageConfigItem[]>([]);
  const loading = ref(false);

  async function initMessageList() {
    try {
      loading.value = true;
      const result = await getMessageTask();

      enableSystemMessage.value = result.every((e) => e.messageTaskDetailDTOList.every((c) => c.sysEnable));
      enableEmailMessage.value = result.every((e) => e.messageTaskDetailDTOList.every((c) => c.emailEnable));
      enableThirdPartyMessage.value = result.every((e) => {
        return e.messageTaskDetailDTOList.every((c) => c[thirdPartyEnableKey.value]);
      });

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
        [thirdPartyEnableKey.value]:
          type === 'weChat' ? !row[thirdPartyEnableKey.value] : row[thirdPartyEnableKey.value],
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
        dingTalkEnable: boolean | undefined;
        larkEnable: boolean | undefined;
      } = {
        sysEnable: undefined,
        emailEnable: undefined,
        weComEnable: undefined,
        dingTalkEnable: undefined,
        larkEnable: undefined,
      };

      if (type === 'system') {
        params.sysEnable = !enableSystemMessage.value;
      } else if (type === 'email') {
        params.emailEnable = !enableEmailMessage.value;
      } else if (type === 'weChat') {
        params[thirdPartyEnableKey.value as keyof typeof params] = !enableThirdPartyMessage.value;
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

  const isEnableNoticeConfig = ref<boolean>(false);
  const platformName = computed(() => platFormNameMap[appStore.activePlatformResource.syncResource]);
  const isSyncFromThirdChecked = computed(() => appStore.activePlatformResource.sync);

  const showExpirationSetting = ref(false);
  const activeDetail = ref<MessageConfigItem | null>(null);
  function settingMessage(e: MouseEvent, row: MessageConfigItem) {
    showExpirationSetting.value = true;
    activeDetail.value = row;
  }
  const columns = computed<DataTableColumn[]>(() => [
    {
      title: t('system.message.Feature'),
      key: 'moduleName',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      className: 'message-table-module-name',
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
      render: (row) => {
        return h(
          'div',
          {
            class: 'one-line-text flex items-center gap-[8px]',
          },
          [
            h('span', row.eventName as string),
            hasAnyPermission(['SYSTEM_NOTICE:UPDATE'])
              ? h(CrmIcon, {
                  type: 'iconicon_set_up',
                  size: 16,
                  class: 'ml-2 text-[var(--primary-8)] cursor-pointer',
                  onClick: (e: MouseEvent) => settingMessage(e, row as unknown as MessageConfigItem),
                })
              : null,
          ]
        );
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
    ...(isEnableNoticeConfig.value
      ? [
          {
            title: () => {
              return h(SwitchPopConfirm, {
                title: t('system.message.confirmCloseWeChatNotice'),
                titleColumnText: t('system.message.platformNotice', { type: platformName.value }),
                value: enableThirdPartyMessage.value,
                loading: enableSystemLoading.value,
                content: t('system.message.confirmCloseSystemNotifyContent'),
                disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']) || !isSyncFromThirdChecked.value,
                toolTipContent: !isSyncFromThirdChecked.value
                  ? t('system.message.weComSwitchTip', { type: platformName.value })
                  : '',
                onChange: (cancel?: () => void) => {
                  if (!isSyncFromThirdChecked.value) return;
                  toggleGlobalMessage('weChat', cancel);
                },
              });
            },
            key: thirdPartyEnableKey.value,
            width: 200,
            ellipsis: {
              tooltip: true,
            },
            render: (row: any) => {
              return h(SwitchPopConfirm, {
                title: t('system.message.confirmCloseWeChatNotice'),
                value: row[thirdPartyEnableKey.value] as boolean,
                loading: enableSystemLoading.value,
                content: t('system.message.confirmCloseSystemNotifyContent'),
                disabled: !hasAnyPermission(['SYSTEM_NOTICE:UPDATE']) || !isSyncFromThirdChecked.value,
                toolTipContent: !isSyncFromThirdChecked.value
                  ? t('system.message.weComSwitchTip', { type: platformName.value })
                  : '',
                onChange: (cancel?: () => void) => {
                  if (!isSyncFromThirdChecked.value) return;
                  handleToggleSystemMessage(row as unknown as MessageConfigItem, 'weChat', cancel);
                },
              });
            },
          },
        ]
      : []),
  ]);

  async function initIntegration() {
    try {
      const res = await getConfigSynchronization();
      if (res) {
        const platFormConfig = res.find(
          (item) => platformType.includes(item.type) && item.type === appStore.activePlatformResource.syncResource
        );
        isEnableNoticeConfig.value =
          !!platFormConfig && !!platFormConfig.config && !!platFormConfig.config?.startEnable;
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(() => {
    // TODO license 先放开
    // if (licenseStore.hasLicense()) {
    //   initIntegration();
    // }
    initIntegration();
    initMessageList();
  });
</script>

<style lang="less" scoped>
  :deep(.message-table) {
    .n-data-table-thead {
      .n-data-table-tr {
        th {
          background: var(--text-n9);
        }
      }
    }
    .message-table-module-name {
      &.n-data-table-td {
        background: var(--text-n9);
      }
    }
  }
</style>
