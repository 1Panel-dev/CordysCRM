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
    <n-scrollbar class="my-[4px] max-h-[416px] px-[4px]">
      <div class="mb-[4px] flex h-[24px] w-[175px] items-center justify-between text-[12px]">
        <div class="font-medium text-[var(--text-n1)]">
          {{ t('crmTable.columnSetting.tableHeaderDisplaySettings') }}
        </div>
        <n-button text type="primary" size="tiny" :disabled="!hasChange" @click="handleReset">
          {{ t('crmTable.columnSetting.resetDefault') }}
        </n-button>
      </div>
      <div v-for="element in notAllowSortCachedColumns" :key="element.key" class="crm-table-column-setting-item">
        <div class="flex flex-1 items-center overflow-hidden pl-[12px]">
          <CrmIcon
            :type="element.fixed ? 'iconicon_pin_filled' : 'iconicon_pin'"
            :class="`mx-[8px] ${element.columnSelectorDisabled ? 'cursor-not-allowed' : 'cursor-pointer'} ${
              element.fixed ? 'text-[var(--primary-8)]' : 'text-[var(--text-n1)]'
            }`"
            :size="12"
          />
          <span class="one-line-text ml-[8px] text-[12px]">
            {{ t(element.title as string) }}
          </span>
        </div>
        <n-switch
          v-model:value="element.showInTable"
          :disabled="element.columnSelectorDisabled"
          size="small"
          @update:value="handleChange"
        />
      </div>

      <VueDraggable v-model="allowSortCachedColumns" handle=".sort-handle" @change="handleChange">
        <div v-for="element in allowSortCachedColumns" :key="element.key" class="crm-table-column-setting-item">
          <div class="flex flex-1 items-center gap-[8px] overflow-hidden">
            <CrmIcon
              type="iconicon_move"
              :class="`sort-handle text-[var(--text-n4)] ${
                element.key !== SpecialColumnEnum.OPERATION ? 'cursor-move ' : 'cursor-not-allowed'
              }`"
              :size="12"
            />
            <CrmIcon
              :type="element.fixed ? 'iconicon_pin_filled' : 'iconicon_pin'"
              :class="`cursor-pointer ${element.fixed ? 'text-[var(--primary-8)]' : 'text-[var(--text-n1)]'}`"
              :size="12"
              @click="toggleFixedColumn(element)"
            />
            <span class="one-line-text ml-[8px] text-[12px]">
              {{ t(element.title as string) }}
            </span>
          </div>
          <n-switch
            v-model:value="element.showInTable"
            :disabled="element.key === SpecialColumnEnum.OPERATION"
            size="small"
            @update:value="handleChange"
          />
        </div>
      </VueDraggable>
    </n-scrollbar>
  </n-popover>
</template>

<script setup lang="ts">
  import { NButton, NPopover, NScrollbar, NSwitch } from 'naive-ui';
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

  async function getCachedColumns() {
    const columns = await tableStore.getCanSetColumns(props.tableKey);
    notAllowSortCachedColumns.value = columns.filter((e) => e.columnSelectorDisabled);
    allowSortCachedColumns.value = columns.filter((e) => !e.columnSelectorDisabled);
  }

  onBeforeMount(() => {
    if (props.tableKey) {
      getCachedColumns();
    }
  });

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
</script>

<style lang="less">
  .crm-table-column-setting-popover {
    padding: 0 !important;
    .crm-table-column-setting-item {
      padding: 5px 8px;
      width: 183px;
      border-radius: @border-radius-small;
      @apply flex items-center justify-between;
      &:hover {
        background: var(--text-n9);
      }
    }
  }
</style>
