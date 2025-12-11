<template>
  <n-popover
    v-model:show="popoverVisible"
    trigger="click"
    placement="bottom-end"
    class="crm-table-column-setting-popover"
    @update:show="handleUpdateShow"
  >
    <template #trigger>
      <n-button
        :ghost="popoverVisible"
        :type="popoverVisible ? 'primary' : 'default'"
        class="outline--secondary px-[8px]"
      >
        <CrmIcon
          type="iconicon_set_up"
          :class="`cursor-pointer ${popoverVisible ? 'text-[var(--primary-8)]' : ''}`"
          :size="16"
        />
      </n-button>
    </template>
    <div class="mb-[4px] p-[8px]">
      <n-radio-group v-model:value="layoutType" name="layoutType">
        <n-radio-button v-for="e in layoutTypeList" :key="e.value" :value="e.value" :label="e.label" />
      </n-radio-group>
    </div>
    <n-scrollbar class="my-[4px] max-h-[416px] max-w-[250px] px-[4px]">
      <div v-if="layoutType === 'columnHeaderSet'">
        <div class="mb-[4px] flex h-[24px] items-center justify-between pr-[8px] text-[12px]">
          <div class="one-line-text font-medium text-[var(--text-n1)]">
            {{ t('crmTable.columnSetting.tableHeaderDisplaySettings') }}
          </div>
          <n-button text type="primary" size="tiny" :disabled="!hasChange" @click="handleReset">
            {{ t('crmTable.columnSetting.resetDefault') }}
          </n-button>
        </div>
        <div
          v-for="element in notAllowSortCachedColumns"
          :key="element.key"
          class="crm-table-column-setting-item pr-[8px]"
        >
          <div class="flex flex-1 items-center gap-[8px] overflow-hidden">
            <CrmIcon type="iconicon_move" class="cursor-not-allowed text-[var(--text-n6)]" :size="12" />
            <CrmIcon
              :type="element.fixed ? 'iconicon_pin_filled' : 'iconicon_pin'"
              :class="`${element.columnSelectorDisabled ? 'cursor-not-allowed' : 'cursor-pointer'} ${
                element.fixed ? 'text-[var(--primary-8)]' : 'text-[var(--text-n1)]'
              }`"
              :size="12"
            />
            <n-tooltip trigger="hover" placement="top">
              <template #trigger>
                <span class="one-line-text ml-[8px] text-[12px]">
                  {{ t(element.title as string) }}
                </span>
              </template>
              {{ t(element.title as string) }}
            </n-tooltip>
          </div>
          <n-switch
            v-model:value="element.showInTable"
            :disabled="element.columnSelectorDisabled"
            size="small"
            :rubber-band="false"
            @update:value="handleChange"
          />
        </div>

        <VueDraggable v-model="allowSortCachedColumns" handle=".sort-handle" @change="handleChange">
          <div v-for="element in allowSortCachedColumns" :key="element.key" class="crm-table-column-setting-item">
            <div class="flex flex-1 items-center gap-[8px] overflow-hidden">
              <CrmIcon
                type="iconicon_move"
                :class="` ${
                  element.key !== SpecialColumnEnum.OPERATION
                    ? 'sort-handle cursor-move text-[var(--text-n4)]'
                    : 'cursor-not-allowed text-[var(--text-n6)]'
                }`"
                :size="12"
              />
              <CrmIcon
                :type="element.fixed ? 'iconicon_pin_filled' : 'iconicon_pin'"
                :class="`cursor-pointer ${element.fixed ? 'text-[var(--primary-8)]' : 'text-[var(--text-n1)]'}`"
                :size="12"
                @click="toggleFixedColumn(element)"
              />
              <n-tooltip trigger="hover" placement="top">
                <template #trigger>
                  <span class="one-line-text ml-[8px] text-[12px]">
                    {{ t(element.title as string) }}
                  </span>
                </template>
                {{ t(element.title as string) }}
              </n-tooltip>
            </div>
            <n-switch
              v-model:value="element.showInTable"
              :disabled="element.key === SpecialColumnEnum.OPERATION"
              size="small"
              :rubber-band="false"
              @update:value="handleChange"
            />
          </div>
        </VueDraggable>
      </div>
      <div v-else class="px-[8px]">
        <div class="h-[24px] font-medium text-[var(--text-n1)]">
          {{ t('crmTable.columnSetting.tableLineHeightSettings') }}
        </div>
        <div class="mt-[4px] flex w-full gap-[4px]">
          <div @click="changeActiveLayoutType('compact')">
            <div :class="`layout-type-item flex flex-col gap-[4px] ${activeLayoutType === 'compact' ? 'active' : ''}`">
              <n-skeleton
                v-for="(width, index) in ['60%', '100%', '100%', '100%', '100%']"
                :key="index"
                height="12px"
                :width="width"
                :sharp="false"
              />
            </div>
            <div :class="`layout-type-item-name text-center ${activeLayoutType === 'compact' ? 'active' : ''}`">
              {{ t('crmTable.columnSetting.compact') }}
            </div>
          </div>
          <div @click="changeActiveLayoutType('loose')">
            <div :class="`layout-type-item flex flex-col gap-[4px] ${activeLayoutType === 'loose' ? 'active' : ''}`">
              <n-skeleton
                v-for="(width, index) in ['60%', '100%', '100%', '100%']"
                :key="index"
                height="12px"
                :width="width"
                :sharp="false"
              />
            </div>
            <div :class="`layout-type-item-name text-center ${activeLayoutType === 'loose' ? 'active' : ''}`">
              {{ t('crmTable.columnSetting.loose') }}
            </div>
          </div>
        </div>
      </div>
    </n-scrollbar>
  </n-popover>
</template>

<script setup lang="ts">
  import { NButton, NPopover, NRadioButton, NRadioGroup, NScrollbar, NSkeleton, NSwitch, NTooltip } from 'naive-ui';
  import { VueDraggable } from 'vue-draggable-plus';

  import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';

  import useTableStore from '@/hooks/useTableStore';

  const props = defineProps<{
    tableKey: TableKeyEnum;
  }>();

  const emit = defineEmits<{
    (e: 'changeColumnsSetting'): void; //  数据发生变化
  }>();

  const { t } = useI18n();
  const tableStore = useTableStore();

  const popoverVisible = ref(false);
  const hasChange = ref(false); // 是否有改动

  const notAllowSortCachedColumns = ref<CrmDataTableColumn[]>([]);
  const allowSortCachedColumns = ref<CrmDataTableColumn[]>([]);
  const activeLayoutType = ref<string>('compact');

  async function getCachedColumns() {
    const columns = await tableStore.getCanSetColumns(props.tableKey);
    notAllowSortCachedColumns.value = columns.filter((e) => e.columnSelectorDisabled);
    allowSortCachedColumns.value = columns.filter((e) => !e.columnSelectorDisabled);
    activeLayoutType.value = (await tableStore.getTableLineHeight(props.tableKey)) as string;
  }

  const layoutTypeList = [
    {
      value: 'columnHeaderSet',
      label: t('crmTable.columnSetting.tableHeaderSettings'),
    },
    {
      value: 'lineHeightSet',
      label: t('crmTable.columnSetting.tableLineHeightSettings'),
    },
  ];

  function handleReset() {
    getCachedColumns();
    hasChange.value = false;
  }

  function handleChange() {
    hasChange.value = true;
  }

  async function handleUpdateShow(show: boolean) {
    if (!show) {
      if (hasChange.value) {
        await tableStore.setColumns(props.tableKey, [
          ...notAllowSortCachedColumns.value,
          ...allowSortCachedColumns.value,
        ]);
        await tableStore.setTableLineHeight(props.tableKey, activeLayoutType.value);
        emit('changeColumnsSetting');
        hasChange.value = false;
      }
    }
  }

  function toggleFixedColumn(ele: CrmDataTableColumn) {
    allowSortCachedColumns.value = allowSortCachedColumns.value.map((item) => {
      if (item.key === ele.key) {
        if (item.fixed) {
          item.fixed = undefined;
        } else {
          item.fixed = item.key === SpecialColumnEnum.OPERATION ? 'right' : 'left';
        }
        return item;
      }
      return item;
    });
    hasChange.value = true;
  }

  const layoutType = ref('columnHeaderSet');

  function changeActiveLayoutType(type: string) {
    activeLayoutType.value = type;
    hasChange.value = true;
  }

  watch(
    () => props.tableKey,
    () => {
      getCachedColumns();
    }
  );

  watch(
    () => popoverVisible.value,
    (val) => {
      if (val && props.tableKey) {
        getCachedColumns();
      }
    }
  );
</script>

<style lang="less">
  .crm-table-column-setting-popover {
    padding: 0 !important;
    .crm-table-column-setting-item {
      padding: 5px 8px;
      max-width: 300px;
      border-radius: @border-radius-small;
      @apply flex items-center justify-between;
      &:hover {
        background: var(--text-n9);
      }
    }
  }
</style>

<style lang="less" scoped>
  .layout-type-item {
    padding: 8px;
    width: 83px;
    height: 76px;
    border: 1px solid var(--text-n6);
    border-radius: 4px;
    &.active {
      border: 1px solid var(--primary-8);
    }
  }
  .layout-type-item-name {
    padding: 8px 0;
    color: var(--text-n1);
    &.active {
      color: var(--primary-8);
    }
  }
</style>
