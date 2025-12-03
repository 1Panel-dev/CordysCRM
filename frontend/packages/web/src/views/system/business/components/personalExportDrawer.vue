<template>
  <CrmDrawer
    v-model:show="visible"
    resizable
    :default-width="800"
    :title="t('common.export')"
    :footer="false"
    class="min-w-[800px]"
    @cancel="handleCancel"
  >
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
        <n-button class="!px-[7px]" @click="() => searchData()">
          <template #icon>
            <CrmIcon type="iconicon_refresh" class="text-[var(--text-n1)]" :size="16" />
          </template>
        </n-button>
      </div>
    </div>
    <div>
      <n-spin :show="loading">
        <n-scrollbar class="max-h-[calc(100vh-204px)]">
          <div v-if="list.length" class="grid h-full grid-cols-2 gap-[16px]">
            <div v-for="item of list" :key="item.id" class="export-item">
              <div class="mb-[8px] flex items-center justify-between">
                <exportStatusTag :status="item.status" />
                <CrmTag type="info" theme="light">
                  {{ getItemType(item.resourceType) }}
                </CrmTag>
              </div>
              <div class="flex flex-nowrap items-center justify-between">
                <div class="flex items-center gap-[8px] overflow-hidden">
                  <n-tooltip :delay="300">
                    <template #trigger>
                      <div class="one-line-text text-[var(--text-n2)]">{{ item.fileName }}</div>
                    </template>
                    {{ item.fileName }}
                  </n-tooltip>
                  <div class="flex-shrink-0 text-[var(--text-n4)]">
                    {{ dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                  </div>
                </div>
                <div
                  v-if="[PersonalExportStatusEnum.SUCCESS, PersonalExportStatusEnum.PREPARED].includes(item.status)"
                  class="ml-[24px] flex items-center"
                >
                  <n-button
                    v-if="item.status === PersonalExportStatusEnum.SUCCESS"
                    text
                    type="primary"
                    @click="handleDownload(item)"
                  >
                    {{ t('common.downloadFile') }}
                  </n-button>
                  <n-button
                    v-if="item.status === PersonalExportStatusEnum.PREPARED"
                    text
                    type="primary"
                    @click="handleCancelExport(item.id)"
                  >
                    {{ t('common.cancelExport') }}
                  </n-button>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="w-full p-[16px] text-center text-[var(--text-n4)]">
            {{ t('system.personal.noExportTask') }}
          </div>
        </n-scrollbar>
      </n-spin>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NAlert, NButton, NScrollbar, NSelect, NSpin, NTooltip, useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { PersonalExportStatusEnum, SystemResourceMessageTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { downloadByteFile } from '@lib/shared/method';
  import { ExportCenterItem } from '@lib/shared/models/system/business';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import exportStatusTag from './exportStatusTag.vue';

  import { cancelCenterExport, exportCenterDownload, getExportCenterList } from '@/api/modules';

  const Message = useMessage();

  const { t } = useI18n();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const activeTab = ref('');
  const loading = ref(false);

  const keyword = ref('');

  const tabList = ref([
    { name: '', tab: t('common.all') },
    { name: SystemResourceMessageTypeEnum.CUSTOMER, tab: t('menu.customer') },
    { name: SystemResourceMessageTypeEnum.CLUE, tab: t('menu.clue') },
    { name: SystemResourceMessageTypeEnum.OPPORTUNITY, tab: t('menu.opportunity') },
    { name: SystemResourceMessageTypeEnum.CONTRACT, tab: t('module.contract') },
  ]);

  const exportStatus = ref('');
  const exportStatusOptions = ref([
    {
      value: '',
      label: t('common.all'),
    },
    {
      value: PersonalExportStatusEnum.PREPARED,
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

  const list = ref<ExportCenterItem[]>([]);

  async function searchData(val?: string) {
    try {
      loading.value = true;
      list.value = await getExportCenterList({
        keyword: val ?? keyword.value,
        exportType: activeTab.value,
        exportStatus: exportStatus.value,
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function changeExportStatus() {
    nextTick(() => {
      searchData();
    });
  }

  function getItemType(type: string) {
    switch (type) {
      case SystemResourceMessageTypeEnum.CUSTOMER:
        return t('menu.customer');
      case SystemResourceMessageTypeEnum.CUSTOMER_POOL:
        return t('module.openSea');
      case SystemResourceMessageTypeEnum.CLUE:
        return t('menu.clue');
      case SystemResourceMessageTypeEnum.CLUE_POOL:
        return t('module.cluePool');
      case SystemResourceMessageTypeEnum.CUSTOMER_CONTACT:
        return t('menu.contact');
      case SystemResourceMessageTypeEnum.CONTRACT:
        return t('module.contract');
      case SystemResourceMessageTypeEnum.CONTRACT_PAYMENT_PLAN:
        return t('module.paymentPlan');
      default:
        return t('menu.opportunity');
    }
  }

  // 取消导出
  async function handleCancelExport(id: string) {
    try {
      loading.value = true;
      await cancelCenterExport(id);
      Message.success(t('common.cancelSuccess'));
      searchData();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  // 下载
  async function handleDownload(item: ExportCenterItem) {
    try {
      loading.value = true;
      const res = await exportCenterDownload(item.id);
      downloadByteFile(res, `${item.fileName}.xlsx`);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handleCancel() {
    visible.value = false;
    exportStatus.value = '';
    activeTab.value = '';
  }

  watch(
    () => activeTab.value,
    () => {
      searchData();
    }
  );

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        searchData();
      }
    }
  );
</script>

<style scoped lang="less">
  .export-item {
    padding: 16px;
    border: 1px solid var(--text-n8);
    border-radius: var(--border-radius-medium);
  }
  :deep(.n-scrollbar-rail.n-scrollbar-rail--vertical--right) {
    right: 0;
  }
</style>
