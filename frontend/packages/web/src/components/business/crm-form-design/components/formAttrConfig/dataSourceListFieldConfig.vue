<template>
  <div ref="wrapperRef" class="crm-form-design-field-source-select">
    <n-popover
      v-model:show="popoverVisible"
      trigger="click"
      placement="bottom-start"
      :show-arrow="false"
      class="crm-form-design-field-source-select-popover"
      :disabled="props.disabled"
    >
      <template #trigger>
        <n-button
          :type="popoverVisible ? 'primary' : 'default'"
          ghost
          class="flex w-full justify-between px-[8px]"
          icon-placement="right"
          :disabled="props.disabled"
        >
          <div class="flex items-center gap-[4px]">
            <div class="text-[var(--text-n2)]">
              {{ t('crmFormDesign.dataSourceTableDisplayFieldShow') }}
            </div>
            <div class="text-[var(--primary-8)]">
              {{ displayCount }}
            </div>
            <div class="text-[var(--text-n2)]">
              {{ t('crmFormDesign.dataSourceTableDisplayFieldCount') }}
            </div>
          </div>
          <template #icon>
            <n-icon
              size="16"
              class="crm-form-design-field-source-select__arrow"
              :class="{ 'is-active': popoverVisible }"
            >
              <ChevronDownOutline />
            </n-icon>
          </template>
        </n-button>
      </template>

      <div class="crm-form-design-field-source-select__panel" :style="panelStyle">
        <CrmSearchInput v-model:value="keyword" :placeholder="t('common.search')" @search="handleSearch" />

        <template v-if="hasOptions">
          <div class="crm-form-design-field-source-select__list-wrap">
            <div
              class="crm-form-design-field-source-select__item crm-form-design-field-source-select__item--all"
              :class="{ 'is-disabled': enabledFilteredValues.length === 0, 'selected': allChecked }"
            >
              <n-checkbox
                :checked="allChecked"
                :indeterminate="indeterminate"
                :disabled="enabledFilteredValues.length === 0"
                @update:checked="handleToggleAll"
              >
                {{ t('common.allSelect') }}
              </n-checkbox>
            </div>

            <n-scrollbar :style="{ maxHeight: normalizedMaxHeight }">
              <n-checkbox-group
                :value="innerValue"
                class="crm-form-design-field-source-select__list"
                @update:value="handleGroupChange"
              >
                <div
                  v-for="item in filteredOptions"
                  :key="item.value"
                  class="crm-form-design-field-source-select__item"
                  :class="{ 'is-disabled': item.disabled, 'selected': innerValue.includes(item.value) }"
                >
                  <n-checkbox :value="item.value" :disabled="item.disabled">
                    <n-tooltip placement="top" :delay="300">
                      <template #trigger>
                        <div class="one-line-text max-w-[200px] !leading-[22px]">{{ item.label }}</div>
                      </template>
                      {{ item.label }}
                    </n-tooltip>
                  </n-checkbox>
                </div>
              </n-checkbox-group>
            </n-scrollbar>
          </div>

          <div v-if="filteredOptions.length === 0" class="crm-form-design-field-source-select__empty">
            <n-empty :description="t('common.noMatchData')" size="small" />
          </div>
        </template>

        <div v-else class="crm-form-design-field-source-select__empty">
          <n-empty :description="t('common.noData')" size="small" />
        </div>
      </div>
    </n-popover>
  </div>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import { NButton, NCheckbox, NCheckboxGroup, NEmpty, NIcon, NPopover, NScrollbar, NTooltip } from 'naive-ui';
  import { ChevronDownOutline } from '@vicons/ionicons5';

  import { FieldDataSourceTypeEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import { dataSourceFilterFormKeyMap } from '@/components/business/crm-form-create/config';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';

  import { getBusinessTitleModuleForm, getFormDesignConfig } from '@/api/modules';
  import useFormCreateSystemColumns from '@/hooks/useFormCreateSystemColumns';
  import { FormKey } from '@/hooks/useFormCreateTable';

  import type { CSSProperties } from 'vue';

  interface FieldOption {
    label: string;
    value: string;
    disabled?: boolean;
  }

  interface CrmFieldMultiSelectProps {
    fieldConfig: FormCreateField;
    isSubTableField?: boolean;
    value?: string[];
    options?: FieldOption[];
    width?: string | number;
    maxHeight?: string | number;
    disabled?: boolean;
  }

  const props = withDefaults(defineProps<CrmFieldMultiSelectProps>(), {
    value: () => [],
    options: () => [],
    maxHeight: 224,
    disabled: false,
  });

  const emit = defineEmits<{
    (e: 'update:value', value: string[]): void;
    (e: 'change', value: string[], selectedOptions: FieldOption[]): void;
  }>();

  const { t } = useI18n();

  function toCssSize(value: string | number) {
    return typeof value === 'number' ? `${value}px` : value;
  }

  function normalizeSelectedValue(value: string[] | undefined, options: FieldOption[]) {
    const optionValueSet = new Set(options.map((item) => String(item.value)));
    const dedupedValue = Array.from(new Set((value || []).map((item) => String(item))));
    return dedupedValue.filter((item) => optionValueSet.has(item));
  }

  function normalizeRawValue(value: string[] | undefined) {
    return Array.from(new Set((value || []).map((item) => String(item))));
  }

  function isSameValue(source: string[], target: string[]) {
    if (source.length !== target.length) return false;
    return source.every((item, index) => item === target[index]);
  }

  const keyword = ref('');
  const popoverVisible = ref(false);
  const innerValue = ref<string[]>([]);
  const normalizedOptions = ref<FieldOption[]>([...props.options]);

  const wrapperRef = ref<HTMLElement>();
  const triggerWidth = ref(0);

  function updateTriggerWidth() {
    triggerWidth.value = wrapperRef.value?.getBoundingClientRect().width || 0;
  }

  function handleSearch(value: string) {
    keyword.value = value;
  }

  const normalizedMaxHeight = computed(() => toCssSize(props.maxHeight));

  const panelStyle = computed<CSSProperties>(() => ({
    width: `${triggerWidth.value || 0}px`,
    minWidth: `${triggerWidth.value || 0}px`,
  }));

  const hasOptions = computed(() => normalizedOptions.value.length > 0);

  const filteredOptions = computed(() => {
    const currentKeyword = keyword.value.trim().toLowerCase();
    if (!currentKeyword) return normalizedOptions.value;
    return normalizedOptions.value.filter((item) => item.label.toLowerCase().includes(currentKeyword));
  });

  const enabledFilteredOptions = computed(() => filteredOptions.value.filter((item) => !item.disabled));
  const enabledFilteredValues = computed(() => enabledFilteredOptions.value.map((item) => item.value));

  const checkedCountInFiltered = computed(() => {
    const selectedSet = new Set(innerValue.value);
    return enabledFilteredValues.value.filter((value) => selectedSet.has(value)).length;
  });

  const allChecked = computed(() => {
    return (
      enabledFilteredValues.value.length > 0 && checkedCountInFiltered.value === enabledFilteredValues.value.length
    );
  });

  const disabledValue = computed(() => normalizedOptions.value.find((item) => item.disabled)?.value || '');

  const indeterminate = computed(() => {
    return innerValue.value.length > 0 && !allChecked.value && normalizedOptions.value.length > 0;
  });

  function getDefaultValue(options: FieldOption[]) {
    const defaultOption = options.find((item) => item.disabled);
    return defaultOption ? [defaultOption.value] : [];
  }

  function getInitialSelectedValue(options: FieldOption[]) {
    const savedValue = normalizeSelectedValue(props.value, options);
    const defaultValue = getDefaultValue(options);

    if (savedValue.length > 0) {
      return Array.from(new Set([...defaultValue, ...savedValue]));
    }
    return defaultValue;
  }

  const displayCount = computed(() => {
    if (normalizedOptions.value.length > 0) {
      return getInitialSelectedValue(normalizedOptions.value).length || 1;
    }

    return normalizeRawValue(props.value).length || 1;
  });

  function commitValueChange() {
    const nextValue = normalizeSelectedValue(innerValue.value, normalizedOptions.value);
    const propValue = normalizeSelectedValue(props.value, normalizedOptions.value);

    if (!isSameValue(nextValue, innerValue.value)) {
      innerValue.value = nextValue;
    }

    if (isSameValue(nextValue, propValue)) {
      return;
    }

    emit('update:value', nextValue);
    emit(
      'change',
      nextValue,
      normalizedOptions.value.filter((item) => nextValue.includes(item.value))
    );
  }

  watch(
    () => props.value,
    () => {
      if (!popoverVisible.value) {
        innerValue.value = getInitialSelectedValue(normalizedOptions.value);
      }
    },
    { immediate: true, deep: true }
  );

  watch(
    () => props.options,
    (val) => {
      normalizedOptions.value = [...(val || [])];
      if (!popoverVisible.value) {
        innerValue.value = getInitialSelectedValue(normalizedOptions.value);
      }
    },
    { immediate: true, deep: true }
  );

  onMounted(() => {
    updateTriggerWidth();
    window.addEventListener('resize', updateTriggerWidth);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', updateTriggerWidth);
  });

  function handleGroupChange(value: Array<string | number>) {
    innerValue.value = value.map((item) => String(item));
  }

  function handleToggleAll(checked: boolean) {
    const selectedSet = new Set(innerValue.value);
    if (checked) {
      enabledFilteredValues.value.forEach((value) => selectedSet.add(value));
    } else {
      enabledFilteredValues.value.forEach((value) => {
        if (disabledValue.value !== value) {
          selectedSet.delete(value);
        }
      });
    }
    const nextValue = normalizedOptions.value.map((item) => item.value).filter((value) => selectedSet.has(value));
    innerValue.value = nextValue;
  }

  const formKey = computed<FormDesignKeyEnum>(() => {
    return dataSourceFilterFormKeyMap[
      props.fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER
    ] as FormDesignKeyEnum;
  });

  const defaultInternalNameKeyMap: Record<string, string> = {
    [FormDesignKeyEnum.CUSTOMER]: 'customerName',
    [FormDesignKeyEnum.CLUE]: 'clueName',
    [FormDesignKeyEnum.BUSINESS]: 'opportunityName',
    [FormDesignKeyEnum.CONTACT]: 'contactName',
    [FormDesignKeyEnum.PRODUCT]: 'productName',
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: 'quotationName',
    [FormDesignKeyEnum.ORDER]: 'orderName',
    [FormDesignKeyEnum.CONTRACT]: 'contractName',
    [FormDesignKeyEnum.CONTRACT_PAYMENT]: 'contractPaymentPlanName',
    [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: 'contractPaymentRecordName',
    [FormDesignKeyEnum.PRICE]: 'priceName',
    [FormDesignKeyEnum.BUSINESS_TITLE]: 'name',
  };

  async function initFieldList() {
    try {
      let nextOptions: FieldOption[] = [];
      const { internalColumnMap, staticColumns } = await useFormCreateSystemColumns({
        formKey: formKey.value as FormKey,
        containerClass: '',
      });

      if (props.fieldConfig.dataSourceType === FieldDataSourceTypeEnum.BUSINESS_TITLE) {
        const res = await getBusinessTitleModuleForm();
        nextOptions = res.fields.map((item) => {
          return {
            label: t(item.name),
            value: item.id,
            disabled: item.businessKey === defaultInternalNameKeyMap[FormDesignKeyEnum.BUSINESS_TITLE],
          };
        });
      } else {
        const res = await getFormDesignConfig(formKey.value);
        nextOptions = res.fields.reduce((acc: FieldOption[], field) => {
          if (
            ![
              FieldTypeEnum.DIVIDER,
              FieldTypeEnum.TEXTAREA,
              FieldTypeEnum.ATTACHMENT,
              FieldTypeEnum.SUB_PRICE,
              FieldTypeEnum.SUB_PRODUCT,
            ].includes(field.type)
          ) {
            acc.push({
              label: field.name,
              value: field.id,
              disabled: field.internalKey === defaultInternalNameKeyMap[formKey.value],
            });
          }
          return acc;
        }, []);
      }
      const systemColumns = internalColumnMap[formKey.value]?.map((e) => {
        return {
          label: e.title,
          value: e.key,
        } as FieldOption;
      });

      const staticOptions = staticColumns?.map((e) => {
        return {
          label: e.title,
          value: e.key,
        } as FieldOption;
      });

      normalizedOptions.value = [...nextOptions, ...(systemColumns || []), ...(staticOptions || [])];

      const nextInnerValue = getInitialSelectedValue(normalizedOptions.value);
      innerValue.value = nextInnerValue;

      const rawValue = normalizeRawValue(props.value);
      if (!isSameValue(rawValue, nextInnerValue)) {
        emit('update:value', nextInnerValue);
        emit(
          'change',
          nextInnerValue,
          nextOptions.filter((item) => nextInnerValue.includes(item.value))
        );
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => props.fieldConfig.dataSourceType,
    () => {
      initFieldList();
    },
    { immediate: true }
  );

  watch(
    () => popoverVisible.value,
    (val) => {
      if (!val) {
        keyword.value = '';
        commitValueChange();
      } else {
        updateTriggerWidth();
      }
    }
  );
</script>

<style lang="less">
  .crm-form-design-field-source-select {
    @apply w-full;
  }
  .crm-form-design-field-source-select-popover {
    padding: 0 !important;
    .n-popover__content {
      padding: 0 !important;
    }
  }
  .crm-form-design-field-source-select__arrow {
    color: var(--text-n4);
    transition: transform 0.2s ease;
    &.is-active {
      transform: rotate(180deg);
    }
  }
  .crm-form-design-field-source-select__panel {
    padding: 8px;
    box-sizing: border-box;
  }
  .crm-form-design-field-source-select__list-wrap,
  .crm-form-design-field-source-select__list {
    gap: 2px;
    @apply flex flex-col;
  }
  .crm-form-design-field-source-select__item {
    padding: 0 4px;
    height: 28px;
    border-radius: 4px;
    box-sizing: border-box;
    @apply flex items-center;
    &.selected {
      background: var(--primary-7);
    }
    &.is-disabled {
      cursor: not-allowed;
      opacity: 0.6;
    }
  }
  .crm-form-design-field-source-select__item--all {
    margin-top: 8px;
    background: var(--text-10);
  }
  .crm-form-design-field-source-select__empty {
    padding: 12px 0 4px;
  }
</style>
