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
        v-if="customList.length"
        v-model:selected-ids="selectedCustomIds"
        :items="customList"
        class="px-0 pt-0"
        :title="t('common.formFields')"
        @select-part="(ids) => updateSelectedList(ids, customList)"
        @select-item="(meta) => selectItem(ColumnTypeEnum.CUSTOM, meta)"
      />
      <FieldSection
        v-if="subTableList.length"
        v-model:selected-ids="selectedSubTableIds"
        :items="subTableList"
        class="px-0 pt-0"
        :title="t('crmFormDesign.subTableField')"
        @select-part="(ids) => updateSelectedList(ids, subTableList)"
        @select-item="(meta) => selectItem(ColumnTypeEnum.SUB_TABLE, meta)"
      />
    </n-scrollbar>
  </CrmModal>
</template>

<script lang="ts" setup>
  import { NScrollbar } from 'naive-ui';

  import { ColumnTypeEnum } from '@lib/shared/enums/commonEnum';
  import { FieldDataSourceTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ExportTableColumnItem } from '@lib/shared/models/common';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { dataSourceFilterFormKeyMap } from '@/components/business/crm-form-create/config';
  import { FormCreateField } from '@/components/business/crm-form-create/types';
  import FieldSection from '@/components/business/crm-table-export-modal/components/fieldSection.vue';

  import { getFieldDisplayList } from '@/api/modules';

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const props = defineProps<{
    fieldConfig: FormCreateField;
    isSubTableField?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'save', selectedIdList: string[], selectedList: any[]): void;
  }>();

  const allColumns = ref<ExportTableColumnItem[]>([]);

  const formKey = computed<FormDesignKeyEnum>(() => {
    return dataSourceFilterFormKeyMap[
      props.fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER
    ] as FormDesignKeyEnum;
  });

  async function getDisplayList() {
    try {
      const res = await getFieldDisplayList(formKey.value);
      allColumns.value = res.fields.map((item) => {
        return {
          key: item.id,
          title: item.name,
          columnType: item.subTableFieldId ? ColumnTypeEnum.SUB_TABLE : ColumnTypeEnum.CUSTOM,
          ...item,
        };
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => show.value,
    async (val) => {
      if (val) {
        await getDisplayList();
      }
    },
    {
      immediate: true,
    }
  );

  const subTableList = computed(() =>
    !props.isSubTableField ? [] : allColumns.value.filter((item) => item.columnType === ColumnTypeEnum.SUB_TABLE)
  );
  const customList = computed(() => allColumns.value.filter((item) => item.columnType === ColumnTypeEnum.CUSTOM));

  const selectedList = ref<any[]>([]);

  const selectedSubTableIds = computed(() =>
    selectedList.value.filter((e) => e.columnType === ColumnTypeEnum.SUB_TABLE).map((e) => e.key)
  );

  const selectedCustomIds = computed(() =>
    selectedList.value.filter((e) => e.columnType === ColumnTypeEnum.CUSTOM).map((e) => e.key)
  );

  const updateSelectedList = (ids: string[], sourceList: any[]) => {
    const newItems = sourceList.filter((item) => ids.includes(item.key));
    const remainingItems = selectedList.value.filter((item) => !sourceList.some((src) => src.key === item.key));
    selectedList.value = [...remainingItems, ...newItems];
  };

  function selectItem(columnType: ColumnTypeEnum, meta: { actionType: 'check' | 'uncheck'; value: string | number }) {
    if (meta.actionType === 'check') {
      // 添加选中的项
      const itemToAdd = (columnType === ColumnTypeEnum.SUB_TABLE ? subTableList.value : customList.value).find(
        (i) => i.key === meta.value
      );
      if (itemToAdd) {
        selectedList.value.push(itemToAdd);
      }
    } else {
      // 移除取消选中的项
      selectedList.value = selectedList.value.filter((item) => item.key !== meta.value);
    }
  }

  function handleConfirm() {
    show.value = false;
    emit('save', [...selectedSubTableIds.value, ...selectedCustomIds.value], selectedList.value);
  }

  function handleCancel() {
    selectedList.value = props.fieldConfig?.showFields ? [...props.fieldConfig.showFields] : [];
    show.value = false;
  }

  watch([() => props.fieldConfig?.showFields, () => allColumns.value], () => {
    if (props.fieldConfig?.showFields && allColumns.value) {
      selectedList.value = allColumns.value.filter((item) => props.fieldConfig?.showFields?.includes(item.key));
    }
  });
</script>
