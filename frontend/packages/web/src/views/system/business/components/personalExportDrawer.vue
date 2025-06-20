<template>
  <CrmDrawer v-model:show="visible" :width="800" :title="t('common.export')" :footer="false">
    <n-alert type="default" class="mb-[16px]" closable>
      <template #icon>
        <CrmIcon type="iconicon_info_circle_filled" :size="20" />
      </template>
      {{ t('system.personal.exportTip') }}
    </n-alert>
    <div class="mb-[16px] flex items-center justify-between">
      <div class="flex items-center gap-[12px]">
        <div>
          <CrmTab v-model:active-tab="activeTab" class="inline-block" type="segment" no-content :tab-list="tabList" />
        </div>
        <n-select
          v-model:value="exportStatus"
          :options="exportStatusOptions"
          class="w-[120px]"
          @update-value="changeExportStatus"
        />
      </div>

      <div class="flex items-center gap-[12px]">
        <CrmSearchInput
          v-model:value="keyword"
          :placeholder="t('common.searchByName')"
          class="!w-[200px]"
          @search="searchData"
        />
        <n-button class="!px-[7px]">
          <template #icon>
            <CrmIcon type="iconicon_refresh" class="text-[var(--text-n1)]" :size="16" />
          </template>
        </n-button>
      </div>
    </div>
    <div>
      <n-spin :show="loading" class="min-h-[300px]">
        <CrmList
          v-if="list.length"
          v-model:data="list"
          virtual-scroll-height="calc(100vh - 188px)"
          key-field="id"
          :item-height="82"
        >
          <template #item="{ item }">
            <div class="export-item mb-[16px]">
              <div class="mb-[8px] flex items-center justify-between">
                <exportStatusTag :status="item.status" />
                <CrmTag type="info" theme="light">
                  {{ getItemType(item.type) }}
                </CrmTag>
              </div>
              <div class="flex flex-nowrap items-center justify-between">
                <div class="flex items-center gap-[8px] overflow-hidden">
                  <n-tooltip :delay="300">
                    <template #trigger>
                      <div class="one-line-text text-[var(--text-n2)]">{{ item.name }}</div>
                    </template>
                    {{ item.name }}
                  </n-tooltip>
                  <div class="flex-shrink-0 text-[var(--text-n4)]">
                    {{ dayjs(item.exportTime).format('YYYY-MM-DD HH:mm:ss') }}
                  </div>
                </div>
                <div
                  v-if="[PersonalExportStatusEnum.SUCCESS, PersonalExportStatusEnum.EXPORTING].includes(item.status)"
                  class="ml-[24px] flex items-center"
                >
                  <n-button
                    v-if="item.status === PersonalExportStatusEnum.SUCCESS"
                    text
                    type="primary"
                    @click="handleDownload(item.id)"
                  >
                    {{ t('common.downloadFile') }}
                  </n-button>
                  <n-button
                    v-if="item.status === PersonalExportStatusEnum.EXPORTING"
                    text
                    type="primary"
                    @click="handleCancelExport(item.id)"
                  >
                    {{ t('common.cancelExport') }}
                  </n-button>
                </div>
              </div>
            </div>
          </template>
        </CrmList>
        <div v-else class="w-full p-[16px] text-center text-[var(--text-n4)]">
          {{ t('system.personal.noExportTask') }}
        </div>
      </n-spin>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NAlert, NButton, NSelect, NSpin, NTooltip } from 'naive-ui';
  import dayjs from 'dayjs';

  import { PersonalExportStatusEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import exportStatusTag from './exportStatusTag.vue';

  const { t } = useI18n();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const activeTab = ref('');
  const loading = ref(false);

  const keyword = ref('');

  const tabList = ref([
    { name: '', tab: t('common.all') },
    { name: 'customer', tab: t('menu.customer') },
    { name: 'clue', tab: t('menu.clue') },
    { name: 'opportunity', tab: t('menu.opportunity') },
  ]);

  const exportStatus = ref('');
  const exportStatusOptions = ref([
    {
      value: '',
      label: t('common.all'),
    },
    {
      value: PersonalExportStatusEnum.EXPORTING,
      label: t('system.personal.exporting'),
    },
    {
      value: PersonalExportStatusEnum.ERROR,
      label: t('common.exportFailed'),
    },
    {
      value: PersonalExportStatusEnum.SUCCESS,
      label: t('common.exportSuccessful'),
    },
  ]);

  function changeExportStatus() {
    // TODO
  }
  // TODO
  const list = ref<any[]>([
    {
      id: '1001',
      status: PersonalExportStatusEnum.EXPORTING,
      name: '导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称',
      exportTime: 10080809809,
      type: 'customer',
    },
    {
      id: '1002',
      status: PersonalExportStatusEnum.CANCELED,
      name: '导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称',
      exportTime: 10080809809,
      type: 'customer',
    },
    {
      id: '1003',
      status: PersonalExportStatusEnum.ERROR,
      name: '导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称',
      exportTime: 10080809809,
      type: 'customer',
    },
    {
      id: '1004',
      status: PersonalExportStatusEnum.SUCCESS,
      name: '导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称导出名称',
      exportTime: 10080809809,
      type: 'customer',
    },
  ]);

  function searchData(val?: string) {}

  function getItemType(type: string) {
    switch (type) {
      case 'customer':
        return t('menu.customer');
      case 'clue':
        return t('menu.clue');
      default:
        return t('menu.opportunity');
    }
  }

  // 取消导出
  function handleCancelExport(id: string) {}

  // 下载
  function handleDownload(id: string) {}
</script>

<style scoped lang="less">
  .export-item {
    padding: 16px;
    border: 1px solid var(--text-n8);
    border-radius: var(--border-radius-medium);
  }
</style>
