<template>
  <CrmModal
    v-model:show="show"
    :title="t('crmFormDesign.dataSourceDisplayField')"
    footer
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <n-scrollbar class="max-h-[60vh]">
      <FieldSection
        v-if="systemList.length"
        v-model:selected-ids="selectedSystemIds"
        :items="systemList"
        class="px-0 pt-0"
        :title="t('common.systemFields')"
        @select-part="(ids) => updateSelectedList(ids, systemList)"
        @select-item="(meta) => selectItem(ColumnTypeEnum.SYSTEM, meta)"
      />

      <FieldSection
        v-if="customList.length"
        v-model:selected-ids="selectedCustomIds"
        :items="customList"
        class="p-0"
        :title="t('common.formFields')"
        @select-part="(ids) => updateSelectedList(ids, customList)"
        @select-item="(meta) => selectItem(ColumnTypeEnum.CUSTOM, meta)"
      />
    </n-scrollbar>
  </CrmModal>
</template>

<script lang="ts" setup>
  import { NScrollbar } from 'naive-ui';

  import { ColumnTypeEnum } from '@lib/shared/enums/commonEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ExportTableColumnItem } from '@lib/shared/models/common';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { FormCreateField } from '@/components/business/crm-form-create/types';
  import FieldSection from '@/components/business/crm-table-export-modal/components/fieldSection.vue';

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();

  const emit = defineEmits<{
    (e: 'save', selectedList: any[]): void;
  }>();

  // TODO lmy 等后端 掉接口获取数据
  const allColumns = ref<ExportTableColumnItem[]>([
    {
      key: '176162279849400000',
      title: '省',
      columnType: ColumnTypeEnum.SYSTEM,
    },
    {
      key: '1763000260068100000',
      title: '部门多选',
      columnType: ColumnTypeEnum.CUSTOM,
    },
    {
      key: '1761622279849400000',
      title: '省',
      columnType: ColumnTypeEnum.CUSTOM,
    },
    {
      key: '176300060068100000',
      title: '部门多选',
      columnType: ColumnTypeEnum.SYSTEM,
    },
  ]);
  const systemList = computed(() => allColumns.value.filter((item) => item.columnType === ColumnTypeEnum.SYSTEM));
  const customList = computed(() => allColumns.value.filter((item) => item.columnType === ColumnTypeEnum.CUSTOM));

  const selectedList = ref<string[]>([]);

  const selectedSystemIds = computed(() =>
    systemList.value.filter((item) => selectedList.value.includes(item.key)).map((item) => item.key)
  );

  const selectedCustomIds = computed(() =>
    customList.value.filter((item) => selectedList.value.includes(item.key)).map((item) => item.key)
  );

  const updateSelectedList = (ids: string[], sourceList: any[]) => {
    const newItems = sourceList.filter((item) => ids.includes(item.key)).map((item) => item.key);
    const remainingItems = selectedList.value.filter((item) => !sourceList.some((src) => src.key === item));
    selectedList.value = [...remainingItems, ...newItems];
  };

  function selectItem(columnType: ColumnTypeEnum, meta: { actionType: 'check' | 'uncheck'; value: string | number }) {
    if (meta.actionType === 'check') {
      // 添加选中的项
      const itemToAdd = (columnType === ColumnTypeEnum.SYSTEM ? systemList.value : customList.value).find(
        (i) => i.key === meta.value
      );
      if (itemToAdd) {
        selectedList.value.push(itemToAdd.key);
      }
    } else {
      // 移除取消选中的项
      selectedList.value = selectedList.value.filter((item) => item !== meta.value);
    }
  }

  function handleConfirm() {
    show.value = false;
    emit('save', selectedList.value);
  }

  function handleCancel() {
    selectedList.value = props.fieldConfig?.showFields ? [...props.fieldConfig.showFields] : [];
    show.value = false;
  }

  watch(
    () => props.fieldConfig?.showFields,
    (val?: string[]) => {
      if (val) {
        selectedList.value = [...val];
      }
    }
  );
</script>
