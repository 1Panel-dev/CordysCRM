<template>
  <CrmCard hide-footer :special-height="64">
    <div class="mb-[16px] flex items-center justify-between">
      <n-button v-permission="['SYSTEM_NOTICE:ADD']" type="primary" @click="handleAdd">
        {{ t('system.message.newAnnouncement') }}
      </n-button>
      <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
    </div>
    <CrmTable
      v-bind="propsRes"
      class="!h-[calc(100%-48px)]"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    />
    <AddNotifyModal :id="activeId" v-model:show="showModal" @cancel="cancelHandler" @saved="() => initData()" />
  </CrmCard>
</template>

<script lang="ts" setup>
  import { NButton, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { AnnouncementItemDetail } from '@lib/shared/models/system/message';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import AddNotifyModal from './addNotifyModal.vue';

  import { deleteAnnouncement, getAnnouncementList } from '@/api/modules';
  import useAppStore from '@/store/modules/app';

  const appStore = useAppStore();

  const { t } = useI18n();
  const Message = useMessage();

  const keyword = ref('');
  const tableRefreshId = ref(0);

  const showModal = ref(false);

  // 添加
  function handleAdd() {
    showModal.value = true;
  }

  const activeId = ref('');
  function cancelHandler() {
    activeId.value = '';
  }

  // 编辑
  function handleEdit(row: AnnouncementItemDetail) {
    activeId.value = row.id;
    showModal.value = true;
  }

  // 删除
  const deleteLoading = ref(false);
  async function deleteHandler(row: AnnouncementItemDetail, done?: () => void) {
    try {
      deleteLoading.value = true;
      await deleteAnnouncement(row.id);
      Message.success(t('common.deleteSuccess'));
      tableRefreshId.value += 1;
      done?.();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      deleteLoading.value = false;
    }
  }

  function handleActionSelect(row: AnnouncementItemDetail, actionKey: string, done?: () => void) {
    switch (actionKey) {
      case 'edit':
        handleEdit(row);
        break;
      case 'pop-delete':
        deleteHandler(row, done);
        break;
      default:
        break;
    }
  }

  const columns: CrmDataTableColumn[] = [
    {
      title: t('system.message.announcementTitle'),
      key: 'subject',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      columnSelectorDisabled: true,
    },
    {
      title: t('system.message.content'),
      key: 'contentText',
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },
    {
      title: t('system.message.timeOfPublication'),
      key: 'startTime',
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
      width: 200,
    },

    {
      title: t('system.message.receiver'),
      key: 'receiver',
      isTag: true,
      width: 200,
      tagGroupProps: {
        labelKey: 'name',
      },
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
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      width: 200,
      render: (row: AnnouncementItemDetail) => {
        return h(CrmNameTooltip, { text: row.createUserName });
      },
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 150,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      render: (row: AnnouncementItemDetail) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
    {
      key: 'operation',
      width: 120,
      fixed: 'right',
      render: (row: AnnouncementItemDetail) =>
        h(CrmOperationButton, {
          groupList: [
            {
              label: t('common.edit'),
              key: 'edit',
              permission: ['SYSTEM_NOTICE:UPDATE'],
            },
            {
              label: t('common.delete'),
              key: 'delete',
              popConfirmProps: {
                loading: deleteLoading.value,
                title: t('system.message.deleteAnnouncementTitle', { name: characterLimit(row.subject) }),
                content: t('system.message.deleteAnnouncementContent'),
                positiveText: t('common.confirm'),
                iconType: 'error',
              },
              permission: ['SYSTEM_NOTICE:DELETE'],
            },
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(
    getAnnouncementList,
    {
      tableKey: TableKeyEnum.SYSTEM_ANNOUNCEMENT_TABLE,
      showSetting: true,
      columns,
    },
    (row: AnnouncementItemDetail) => {
      return {
        ...row,
        startTime: `${dayjs(row.startTime).format('YYYY-MM-DD HH:mm:ss')} ${t('common.to')} ${dayjs(row.endTime).format(
          'YYYY-MM-DD HH:mm:ss'
        )}`,
        receiver: [...(row.deptIdName || []), ...(row.userIdName || [])],
      };
    }
  );

  function initData() {
    setLoadListParams({
      keyword: keyword.value,
      organizationId: appStore.orgId,
    });
    loadList();
  }

  function searchData(val: string) {
    keyword.value = val;
    initData();
  }

  watch(
    () => tableRefreshId.value,
    () => {
      initData();
    }
  );

  onBeforeMount(() => {
    initData();
  });
</script>

<style lang="less" scoped></style>
