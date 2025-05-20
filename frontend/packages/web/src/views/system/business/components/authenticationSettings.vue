<template>
  <!-- special-height 64是tab的高度和margin -->
  <CrmCard hide-footer :special-height="64">
    <div class="mb-[16px] flex items-center justify-between">
      <n-button v-permission="['SYSTEM_SETTING:ADD']" type="primary" @click="handleAdd">
        {{ t('system.business.authenticationSettings.add') }}
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
      <n-button
        v-permission="['SYSTEM_SETTING:UPDATE']"
        type="primary"
        ghost
        @click="handleEdit(activeAuthDetail, true)"
      >
        {{ t('common.edit') }}
      </n-button>
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

  <AddOrEditAuthDrawer v-model:show="showAddOrEditAuthDrawer" :edit-auth-info="editAuthInfo" @refresh="loadList" />
</template>

<script setup lang="ts">
  import { useClipboard } from '@vueuse/core';
  import { NButton, NSwitch, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit, desensitize } from '@lib/shared/method';
  import type { AuthForm, AuthItem } from '@lib/shared/models/system/business';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDescription, { Description } from '@/components/pure/crm-description/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmEditableText from '@/components/business/crm-editable-text/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import AddOrEditAuthDrawer from './addOrEditAuthDrawer.vue';

  import { deleteAuth, getAuthDetail, getAuthList, updateAuthName, updateAuthStatus } from '@/api/modules';
  import { authTypeFieldMap, defaultAuthForm } from '@/config/business';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const { openModal } = useModal();
  const Message = useMessage();
  const { copy, isSupported } = useClipboard({ legacy: true });

  // 详情
  const showDetailDrawer = ref(false);
  const activeAuthDetail = ref<AuthForm>(cloneDeep(defaultAuthForm));
  const descriptions = ref<Description[]>([]);

  async function getDetail(id: string) {
    try {
      const res = await getAuthDetail(id);
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
      { label: t('common.desc'), value: activeAuthDetail.value.description || '' },
      ...(authTypeFieldMap[activeAuthDetail.value?.type as string]?.map(({ label, key }) => ({
        label,
        value: activeAuthDetail.value.configuration[key as string],
        valueSlotName: ['password', 'appSecret'].includes(key) ? 'password' : undefined,
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
  const editAuthInfo = ref<AuthForm>(cloneDeep(defaultAuthForm));

  function handleAdd() {
    editAuthInfo.value = cloneDeep(defaultAuthForm);
    showAddOrEditAuthDrawer.value = true;
  }
  async function handleEdit(record: AuthForm | AuthItem, isFromDetail = false) {
    if (isFromDetail) {
      editAuthInfo.value = { ...(record as AuthForm) };
      showAddOrEditAuthDrawer.value = true;
    } else {
      await getDetail((record as AuthItem).id);
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
      permission: ['SYSTEM_SETTING:UPDATE'],
    },
    {
      label: t('common.delete'),
      key: 'delete',
      permission: ['SYSTEM_SETTING:DELETE'],
    },
  ]);

  const tableRefreshId = ref(0);
  function handleDelete(row: AuthItem) {
    openModal({
      type: 'error',
      title: t('system.business.authenticationSettings.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteAuth(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleActionSelect(row: AuthItem, actionKey: string) {
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

  function handleEnable(row: AuthItem) {
    openModal({
      type: 'default',
      title: t('system.business.authenticationSettings.enableConfirmTitle', { name: characterLimit(row.name) }),
      content: t('system.business.authenticationSettings.enableConfirmContent'),
      positiveText: t('common.confirmStart'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await updateAuthStatus(row.id, true);
          Message.success(t('common.opened'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleDisable(row: AuthItem) {
    openModal({
      type: 'default',
      title: t('system.business.authenticationSettings.disableConfirmTitle', { name: characterLimit(row.name) }),
      content: t('system.business.authenticationSettings.disableConfirmContent'),
      positiveText: t('common.confirmDisable'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await updateAuthStatus(row.id, false);
          Message.success(t('common.disabled'));
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  async function handleChangeName(id: string, name: string) {
    try {
      await updateAuthName(id, name);
      Message.success(t('common.updateSuccess'));
      return Promise.resolve(true);
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
      return Promise.resolve(false);
    }
  }

  const columns: CrmDataTableColumn[] = [
    {
      title: t('common.name'),
      key: 'name',
      width: 200,
      columnSelectorDisabled: true,
      render: (row: AuthItem) => {
        return h(
          CrmEditableText,
          {
            value: row.name,
            permission: ['SYSTEM_SETTING:UPDATE'],
            onHandleEdit: async (val: string, done?: () => void) => {
              const res = await handleChangeName(row.id, val);
              if (res) {
                done?.();
                tableRefreshId.value += 1;
              }
            },
          },
          {
            default: () =>
              h(
                'div',
                {
                  class: 'max-w-[calc(100%-24px)] w-[fit-content]',
                },
                h(
                  CrmTableButton,
                  {
                    onClick: () => openAuthDetail(row.id as string),
                  },
                  { default: () => row.name, trigger: () => row.name }
                )
              ),
          }
        );
      },
    },
    {
      title: t('common.status'),
      key: 'enable',
      width: 60,
      render: (row: AuthItem) => {
        return h(NSwitch, {
          value: row.enable,
          disabled: !hasAnyPermission(['SYSTEM_SETTING:UPDATE']),
          onClick: () => {
            if (!hasAnyPermission(['SYSTEM_SETTING:UPDATE'])) return;
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
      render: (row: AuthItem) =>
        h(CrmOperationButton, {
          groupList: operationGroupList.value,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(getAuthList, {
    tableKey: TableKeyEnum.AUTH,
    showSetting: true,
    columns,
  });

  function searchData() {
    setLoadListParams({ keyword: keyword.value });
    loadList();
  }

  watch(
    () => tableRefreshId.value,
    () => {
      loadList();
    }
  );

  onMounted(() => {
    searchData();
  });
</script>
