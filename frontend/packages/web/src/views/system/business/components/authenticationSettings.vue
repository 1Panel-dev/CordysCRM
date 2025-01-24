<template>
  <CrmCard hide-footer>
    <div class="mb-[16px] flex items-center justify-between">
      <n-button type="primary">{{ t('system.business.authenticationSettings.add') }}</n-button>
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
      <n-button type="primary" ghost> {{ t('common.edit') }} </n-button>
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
</template>

<script setup lang="ts">
  import { useClipboard } from '@vueuse/core';
  import { NButton, NInput, useMessage } from 'naive-ui';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDescription, { Description } from '@/components/pure/crm-description/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { authTypeFieldMap } from '@/config/business';
  import { useI18n } from '@/hooks/useI18n';
  import { desensitize } from '@/utils';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';

  const { t } = useI18n();
  const Message = useMessage();
  const { copy, isSupported } = useClipboard({ legacy: true });

  // 详情
  const showDetailDrawer = ref(false);
  const activeAuthDetail = ref<any>({
    id: '',
    enable: true,
    description: '',
    name: '',
    type: 'LDAP',
    updateTime: 0,
    createTime: 0,
    configuration: {},
  });
  const descriptions = ref<Description[]>([]);

  async function openAuthDetail(id: string) {
    try {
      showDetailDrawer.value = true;
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
      descriptions.value = [
        { label: t('common.desc'), value: res.description },
        ...(authTypeFieldMap[res.type]?.map(({ label, key }) => ({
          label,
          value: configuration[key],
          slotName: key === 'password' ? 'password' : undefined,
        })) || []),
      ];
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error(e);
    }
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

  // 表格
  const keyword = ref('');
  const columns: CrmDataTableColumn[] = [
    {
      title: t('common.creator'),
      key: 'num',
      width: 60,
      sortOrder: false,
      sorter: true,
      render: (row: any) => {
        return h(
          NButton,
          {
            text: true,
            type: 'primary',
            onClick: () => openAuthDetail(row.id as string),
          },
          { default: () => row.num }
        );
      },
    },
    {
      title: t('common.execute'),
      key: 'title',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      filterOptions: [
        {
          label: 'London',
          value: 'London',
        },
        {
          label: 'New York',
          value: 'New York',
        },
      ],
      filter: 'default',
    },
    {
      title: t('common.text'),
      key: 'status',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      filterOptions: [
        {
          label: 'London',
          value: 'London',
        },
        {
          label: 'New York',
          value: 'New York',
        },
      ],
      filter: 'default',
    },
    { key: 'operation', width: 80 },
  ];

  function initData() {
    const data: any = {
      total: 11,
      pageSize: 10,
      current: 1,
      list: [
        {
          id: '11',
          num: 'string',
          title: 'string',
          status: 'string',
          updateTime: null,
          createTime: null,
        },
        {
          id: '22',
          num: '232324323',
          title: '222',
          status: 'aaaa',
          updateTime: null,
          createTime: null,
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
