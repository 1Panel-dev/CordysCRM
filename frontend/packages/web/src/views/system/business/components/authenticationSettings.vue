<template>
  <CrmCard hide-footer>
    <div class="mb-[16px] flex items-center justify-between">
      <n-button type="primary" @click="handleAdd">
        {{ t('system.business.authenticationSettings.add') }}
      </n-button>
      <n-input v-model:value="keyword" :placeholder="t('common.searchByName')" clearable class="!w-[240px]" />
    </div>
    <CrmTable
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    />
  </CrmCard>

  <!-- 详情 -->
  <CrmDrawer
    v-model:show="showDetailDrawer"
    :footer="false"
    :show-mask="false"
    :title="activeAuthDetail.name"
    :width="680"
  >
    <template #titleLeft>
      <CrmTag class="font-normal" theme="light" :type="`${activeAuthDetail.enable ? 'success' : 'default'}`">
        {{ activeAuthDetail.enable ? t('common.opened') : t('common.disabled') }}
      </CrmTag>
    </template>
    <template #titleRight>
      <n-button type="primary" ghost @click="handleEdit(activeAuthDetail, true)"> {{ t('common.edit') }} </n-button>
    </template>
    <CrmDescription
      :one-line-label="false"
      class="p-[8px]"
      :descriptions="descriptions"
      :column="1"
      label-align="end"
      label-width="100px"
    >
      <template #password="{ item }">
        <div class="flex items-center gap-[8px]">
          <div v-show="showPassword">{{ item.value }}</div>
          <div v-show="!showPassword">{{ desensitize(item.value as string) }}</div>
          <CrmIcon
            :type="showPassword ? 'iconicon_browse' : 'iconicon_browse_off'"
            :size="16"
            class="cursor-pointer text-[var(--text-n4)]"
            @click="changeShowVisible"
          />
          <CrmIcon
            type="iconicon_file_copy"
            :size="16"
            class="cursor-pointer text-[var(--text-n4)] active:text-[var(--primary-8)]"
            @click="handleCopy(item.value as string)"
          />
        </div>
      </template>
    </CrmDescription>
  </CrmDrawer>

  <AddOrEditAuthDrawer v-model:show="showAddOrEditAuthDrawer" :edit-auth-info="editAuthInfo" />
</template>

<script setup lang="ts">
  import { useClipboard } from '@vueuse/core';
  import { NButton, NInput, NSwitch, useMessage } from 'naive-ui';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDescription, { Description } from '@/components/pure/crm-description/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmEditableText from '@/components/business/crm-editable-text/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import AddOrEditAuthDrawer from './addOrEditAuthDrawer.vue';

  import { authTypeFieldMap, defaultAuthForm } from '@/config/business';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { desensitize } from '@/utils';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';

  const { t } = useI18n();
  const { openModal } = useModal();
  const Message = useMessage();
  const { copy, isSupported } = useClipboard({ legacy: true });

  // 详情
  const showDetailDrawer = ref(false);
  const activeAuthDetail = ref<any>({ ...defaultAuthForm });
  const descriptions = ref<Description[]>([]);

  async function getDetail(id: string) {
    try {
      // const res = await getAuthDetail(id);
      const res = {
        id: '1029847274971136',
        description: '描述',
        name: '测测试测测试测试测试测试测试测试测试测试测试试测测试测试测试测试测试测试测试测试测试试测测试测试测试测试测试测试测试测试测试试测试测试测试测试测试测试测试测试试',
        type: 'LDAP',
        configuration:
          '{"url":"192.168.12.11","dn":"ghjkkj","password":"sdsadsa","ou":"wq","filter":"ds","mapping":"we"}',
        enable: true,
      };
      const configuration = JSON.parse(res.configuration || '{}');
      activeAuthDetail.value = { ...res, configuration };
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error(e);
    }
  }

  async function openAuthDetail(id: string) {
    showDetailDrawer.value = true;
    await getDetail(id);
    descriptions.value = [
      { label: t('common.desc'), value: activeAuthDetail.value.description },
      ...(authTypeFieldMap[activeAuthDetail.value.type]?.map(({ label, key }) => ({
        label,
        value: activeAuthDetail.value.configuration[key],
        slotName: key === 'password' ? 'password' : undefined,
      })) || []),
    ];
  }

  const showPassword = ref(false);
  function changeShowVisible() {
    showPassword.value = !showPassword.value;
  }
  function handleCopy(value: string) {
    if (isSupported) {
      copy(value);
      Message.success(t('common.copySuccess'));
    } else {
      Message.error(t('common.copyNotSupport'));
    }
  }

  // 新增和编辑
  const showAddOrEditAuthDrawer = ref(false);
  const editAuthInfo = ref<any>({});

  function handleAdd() {
    editAuthInfo.value = { ...defaultAuthForm };
    showAddOrEditAuthDrawer.value = true;
  }
  async function handleEdit(record: any, isFromDetail = false) {
    if (isFromDetail) {
      editAuthInfo.value = { ...record };
      showAddOrEditAuthDrawer.value = true;
    } else {
      await getDetail(record.id);
      editAuthInfo.value = { ...activeAuthDetail.value };
      showAddOrEditAuthDrawer.value = true;
    }
  }

  // 表格
  const keyword = ref('');

  // 操作列
  const operationGroupList = ref([
    {
      label: t('common.edit'),
      key: 'edit',
    },
    {
      label: t('common.delete'),
      key: 'delete',
    },
  ]);

  function handleDelete(row: any) {
    openModal({
      type: 'error',
      title: t('system.business.authenticationSettings.deleteConfirmTitle', { name: row.name }),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
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
        handleEdit(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  function handleEnable(row: any) {
    openModal({
      type: 'default',
      title: t('system.business.authenticationSettings.enableConfirmTitle', { name: row.name }),
      content: t('system.business.authenticationSettings.enableConfirmContent'),
      positiveText: t('common.confirmStart'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          Message.success(t('common.opened'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleDisable(row: any) {
    openModal({
      type: 'default',
      title: t('system.business.authenticationSettings.disableConfirmTitle', { name: row.name }),
      content: t('system.business.authenticationSettings.disableConfirmContent'),
      positiveText: t('common.confirmDisable'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          Message.success(t('common.disabled'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  const columns: CrmDataTableColumn[] = [
    {
      title: t('common.name'),
      key: 'name',
      width: 200,
      render: (row: any) => {
        return h(
          CrmEditableText,
          {
            value: row.name,
            onHandleEdit: (val: string) => {
              // TODO 调接口
              row.name = val;
            },
          },
          {
            default: h(
              NButton,
              {
                text: true,
                type: 'primary',
                onClick: () => openAuthDetail(row.id as string),
              },
              { default: () => row.name }
            ),
          }
        );
      },
    },
    {
      title: t('common.status'),
      key: 'enable',
      width: 60,
      render: (row: any) => {
        return h(NSwitch, {
          value: row.enable,
          onClick: () => {
            if (row.enable) {
              handleDisable(row);
            } else {
              handleEnable(row);
            }
          },
        });
      },
    },
    {
      title: t('common.desc'),
      key: 'description',
      width: 200,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      key: 'operation',
      width: 100,
      render: (row: any) =>
        h(CrmOperationButton, {
          groupList: operationGroupList.value,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  function initData() {
    const data: any = {
      total: 11,
      pageSize: 10,
      current: 1,
      list: [
        {
          id: '702175637397504',
          enable: false,
          createTime: 1722568872431,
          updateTime: 1722568872431,
          description: '',
          name: '1',
          type: 'OAUTH2',
          configuration: null,
        },
        {
          id: '693860580712448',
          enable: true,
          createTime: 1722568388178,
          updateTime: 1722568388178,
          description: '',
          name: '1213',
          type: 'OAuth 2.0',
          configuration: null,
        },
      ],
    };
    return new Promise<any>((resolve) => {
      setTimeout(() => {
        resolve(data);
      }, 200);
    });
  }

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(initData, {
    tableKey: TableKeyEnum.SYSTEM_USER,
    showSetting: true,
    columns,
  });

  function searchData() {
    setLoadListParams({ keyword: '' });
    loadList();
  }

  onMounted(() => {
    searchData();
  });
</script>
