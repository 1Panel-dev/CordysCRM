<template>
  <CrmCard hide-footer no-content-padding :special-height="licenseStore.expiredDuring ? 128 : 64">
    <CrmSplitPanel class="h-full" :max="0.5" :min="0.2" :default-size="0.2">
      <template #1>
        <TermCategoryList
          ref="termCategoryListRef"
          v-model:selected-id="selectedCategoryId"
          @loaded="handleCategoryLoaded"
          @deleted="handleCategoryDeleted"
        />
      </template>

      <template #2>
        <div class="h-full px-[24px] pt-[24px]">
          <CrmTable
            ref="crmTableRef"
            v-bind="propsRes"
            class="term-settings-table"
            @page-change="propsEvent.pageChange"
            @page-size-change="propsEvent.pageSizeChange"
            @sorter-change="propsEvent.sorterChange"
            @filter-change="propsEvent.filterChange"
            @refresh="searchData"
          >
            <template #tableTop>
              <div class="flex items-center gap-[12px]">
                <n-button v-permission="['SYSTEM_SETTING:ADD']" type="primary" @click="openAddDrawer">
                  {{ t('system.business.term.addTerm') }}
                </n-button>
                <CrmImportButton
                  v-if="hasAnyPermission(['SYSTEM_SETTING:ADD'])"
                  :api-type="ImportTypeExcludeFormDesignEnum.TERM_IMPORT"
                  :catalog-id="selectedCategoryId"
                  :title="t('system.business.term.term')"
                  hide-import-updates
                  hide-import-updates-tooltip
                  @import-success="handleImportSuccess"
                />
              </div>
            </template>
            <template #actionRight>
              <n-button
                v-permission="['SYSTEM_SETTING:ADD']"
                class="ai-discovery-button"
                ghost
                type="primary"
                @click="openDiscoveryDrawer"
              >
                <svg class="ai-discovery-gradient-defs" aria-hidden="true">
                  <defs>
                    <linearGradient id="aiDiscoveryIconGradient" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%" stop-color="#3370ff" />
                      <stop offset="47.65%" stop-color="#e22e23" />
                      <stop offset="100%" stop-color="#00c261" />
                    </linearGradient>
                  </defs>
                </svg>
                <template #icon>
                  <CrmIcon class="ai-discovery-icon" type="iconicon_star1" :size="18" />
                </template>
                <span class="ai-discovery-text">{{ t('system.business.term.aiDiscovery') }}</span>
              </n-button>
              <CrmSearchInput v-model:value="keyword" :placeholder="t('common.searchByName')" @search="searchData" />
            </template>
          </CrmTable>
        </div>
      </template>
    </CrmSplitPanel>
  </CrmCard>

  <TermDrawer
    v-model:show="drawerVisible"
    :term="editingTerm"
    :categories="categories"
    :active-category-id="selectedCategoryId"
    :adopt-discovery-id="adoptDiscoveryId"
    @saved="handleTermSaved"
  />
  <TermDiscoveryDrawer
    ref="termDiscoveryDrawerRef"
    v-model:show="discoveryDrawerVisible"
    @adopt="handleAdoptDiscovery"
  />
</template>

<script setup lang="ts">
  import { h, ref, watch } from 'vue';
  import { NButton, NSwitch, useMessage } from 'naive-ui';

  import { ImportTypeExcludeFormDesignEnum } from '@lib/shared/enums/commonEnum';
  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { TermCategoryItem, TermDiscoveryItem, TermItem } from '@lib/shared/models/system/term';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmImportButton from '@/components/business/crm-import-button/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import TermCategoryList from './components/termCategoryList.vue';
  import TermDiscoveryDrawer from './components/termDiscoveryDrawer.vue';
  import TermDrawer from './components/termDrawer.vue';

  import { deleteTerm, getTermDetail, getTermList, switchTerm } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useLicenseStore from '@/store/modules/setting/license';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const selectedCategoryId = ref('');
  const termCategoryListRef = ref<InstanceType<typeof TermCategoryList>>();
  const licenseStore = useLicenseStore();

  const tableRefreshId = ref(0);

  const categories = ref<TermCategoryItem[]>([]);
  function handleCategoryLoaded(list: TermCategoryItem[]) {
    categories.value = list;
  }

  function handleCategoryDeleted() {
    if (selectedCategoryId.value) {
      tableRefreshId.value += 1;
    }
  }

  async function toggleTermStatus(row: TermItem) {
    try {
      await switchTerm(row.id);
      Message.success(row.enable ? t('common.disableSuccess') : t('common.enableSuccess'));
      tableRefreshId.value += 1;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const drawerVisible = ref(false);
  const editingTerm = ref<Partial<TermItem> | null>(null);
  const adoptDiscoveryId = ref('');
  function openAddDrawer() {
    editingTerm.value = null;
    adoptDiscoveryId.value = '';
    drawerVisible.value = true;
  }

  async function handleEdit(row: TermItem) {
    try {
      editingTerm.value = await getTermDetail(row.id);
      drawerVisible.value = true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const discoveryDrawerVisible = ref(false);
  const termDiscoveryDrawerRef = ref<InstanceType<typeof TermDiscoveryDrawer>>();
  function openDiscoveryDrawer() {
    discoveryDrawerVisible.value = true;
  }

  function handleAdoptDiscovery(row: TermDiscoveryItem) {
    adoptDiscoveryId.value = row.id;
    editingTerm.value = {
      catalogId: selectedCategoryId.value,
      standardTerm: row.freeTerm,
      systemReference: row.reference,
    };
    drawerVisible.value = true;
  }

  const tableRemoveRefreshId = ref('');
  function handleDelete(row: TermItem) {
    openModal({
      type: 'error',
      title: t('system.business.term.deleteTermConfirmTitle', { name: characterLimit(row.standardTerm) }),
      content: t('system.business.term.deleteTermConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteTerm(row.id);
          Message.success(t('common.deleteSuccess'));
          tableRemoveRefreshId.value = row.id;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleActionSelect(row: TermItem, actionKey: string) {
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

  const columns: CrmDataTableColumn<TermItem>[] = [
    {
      title: t('crmTable.order'),
      width: 70,
      key: SpecialColumnEnum.ORDER,
      resizable: false,
      columnSelectorDisabled: true,
      render: (_row, index) => index + 1,
    },
    {
      title: t('system.business.term.standardTerm'),
      key: 'standardTerm',
      width: 160,
      columnSelectorDisabled: true,
      fixed: 'left',
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('system.business.term.synonyms'),
      key: 'alsoCalled',
      width: 220,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('system.business.term.forbiddenWords'),
      key: 'avoidThese',
      width: 220,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.status'),
      key: 'enable',
      width: 120,
      filter: true,
      filterOptions: [
        {
          value: true,
          label: t('common.enable'),
        },
        {
          value: false,
          label: t('common.disable'),
        },
      ],
      render: (row) =>
        h(NSwitch, {
          'value': row.enable,
          'disabled': !hasAnyPermission(['SYSTEM_SETTING:UPDATE']),
          'rubberBand': false,
          'onUpdate:value': () => {
            if (!hasAnyPermission(['SYSTEM_SETTING:UPDATE'])) return;
            toggleTermStatus(row);
          },
        }),
    },
    {
      title: t('common.creator'),
      key: 'createUserName',
      width: 120,
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 150,
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUserName',
      width: 120,
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 150,
      sortOrder: false,
      sorter: true,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.operation'),
      key: 'operation',
      width: 110,
      fixed: 'right',
      render: (row) =>
        h(CrmOperationButton, {
          groupList: [
            { label: t('common.edit'), key: 'edit', permission: ['SYSTEM_SETTING:UPDATE'] },
            { label: t('common.delete'), key: 'delete', permission: ['SYSTEM_SETTING:DELETE'] },
          ],
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable<TermItem>(getTermList, {
    columns,
    tableKey: TableKeyEnum.SYSTEM_TERM,
    permission: ['SYSTEM_SETTING:UPDATE', 'SYSTEM_SETTING:DELETE'],
    showSetting: true,
    containerClass: '.term-settings-table',
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const keyword = ref('');
  async function searchData(val?: string, refreshId?: string) {
    try {
      setLoadListParams({ keyword: val ?? keyword.value, catalogId: selectedCategoryId.value });
      await loadList(false, refreshId);
      if (!refreshId) {
        crmTableRef.value?.scrollTo({ top: 0 });
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function clearList() {
    propsRes.value.data = [];
    propsRes.value.crmPagination = {
      ...propsRes.value.crmPagination,
      itemCount: 0,
    };
  }

  function removeItemFromList(id: string) {
    propsRes.value.data = propsRes.value.data.filter((item) => item.id !== id) as typeof propsRes.value.data;
    propsRes.value.crmPagination = {
      ...propsRes.value.crmPagination,
      itemCount: (propsRes.value.crmPagination?.itemCount ?? 1) - 1,
    };
  }

  async function handleTermSaved(refreshId?: string, savedAdoptDiscoveryId?: string) {
    await termCategoryListRef.value?.initCategoryList();
    if (savedAdoptDiscoveryId) {
      termDiscoveryDrawerRef.value?.removeItemFromList(savedAdoptDiscoveryId);
      adoptDiscoveryId.value = '';
    }
    searchData(keyword.value, refreshId);
  }

  async function handleImportSuccess() {
    await termCategoryListRef.value?.initCategoryList();
    searchData();
  }

  watch(
    () => tableRefreshId.value,
    () => {
      searchData(keyword.value);
    }
  );

  watch(
    () => tableRemoveRefreshId.value,
    (val) => {
      if (val) {
        removeItemFromList(val);
      }
    }
  );

  watch(
    () => selectedCategoryId.value,
    (categoryId) => {
      if (categoryId) {
        searchData();
      } else {
        clearList();
      }
    }
  );
</script>

<style lang="less" scoped>
  .ai-discovery-button {
    --ai-discovery-gradient: linear-gradient(96.9deg, #3370ff 0%, #e22e23 47.65%, #00c261 100%);

    position: relative;
    border-color: transparent;
    background-color: var(--primary-7);
    &::before {
      position: absolute;
      padding: 1px;
      border-radius: inherit;
      background: var(--ai-discovery-gradient);
      inset: 0;
      pointer-events: none;
      content: '';
      mask: linear-gradient(#ffffff 0 0) content-box, linear-gradient(#ffffff 0 0);
      mask-composite: exclude;
    }
    :deep(.n-button__border),
    :deep(.n-button__state-border) {
      display: none;
    }
    .ai-discovery-gradient-defs {
      position: absolute;
      overflow: hidden;
      width: 0;
      height: 0;
    }
    :deep(.ai-discovery-icon svg) {
      color: #e22e23;
      fill: url('#aiDiscoveryIconGradient');
    }
    .ai-discovery-text {
      color: transparent;
      background: var(--ai-discovery-gradient);
      background-clip: text;
    }
  }
</style>
