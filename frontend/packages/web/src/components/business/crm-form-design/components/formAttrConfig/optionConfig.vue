<template>
  <n-radio-group
    v-model:value="fieldConfig.optionSource"
    name="radiogroup"
    class="flex"
    :disabled="props.disabled"
    size="small"
  >
    <n-radio-button value="ref" class="flex-1 text-center">
      {{ t('crmFormDesign.quotingData') }}
    </n-radio-button>
    <n-radio-button value="custom" class="flex-1 text-center">
      {{ t('crmFormDesign.custom') }}
    </n-radio-button>
  </n-radio-group>
  <n-cascader
    v-if="fieldConfig.optionSource === 'ref'"
    v-model:value="fieldConfig.refId"
    :disabled="props.disabled"
    :options="refOptions"
    check-strategy="child"
    :render-label="renderCascaderLabel"
    :menu-props="{ class: 'form-design-cascader' }"
    remote
    :on-load="handleLoadRef"
    @update:value="updateFormKeyFromCascader"
  />
  <component
    :is="getComponent"
    v-else-if="fieldConfig.optionSource === 'custom' && fieldConfig.customOptions"
    :value="fieldConfig.defaultValue"
    :disabled="props.disabled"
  >
    <!-- 通过draggable属性控制带.draggable类的元素可拖拽，实现部分元素不允许拖拽 -->
    <VueDraggable
      v-model="fieldConfig.customOptions"
      :animation="150"
      draggable=".draggable"
      handle=".handle"
      class="flex flex-col gap-[8px]"
    >
      <div
        v-for="(item, i) in fieldConfig.customOptions"
        :key="item.value"
        class="flex flex-wrap items-center gap-[8px]"
        :class="item.value === 'other' ? '' : 'draggable'"
      >
        <n-tooltip
          :delay="300"
          :show-arrow="false"
          :disabled="item.value === 'other'"
          class="crm-form-design--composition-item-tools-tip"
        >
          <template #trigger>
            <CrmIcon
              type="iconicon_move"
              class="handle cursor-move"
              :class="item.value === 'other' ? 'cursor-not-allowed text-[var(--text-n6)]' : ''"
            />
          </template>
          {{ t('common.sort') }}
        </n-tooltip>
        <n-checkbox v-if="isMultiple" :value="item.value" @click="handleCheckBoxOptionClick(item.value)" />
        <n-radio
          v-else
          :value="item.value"
          :default-checked="fieldConfig.defaultValue === item.value"
          class="flex items-center"
          :disabled="props.disabled"
          @click="handleRadioOptionClick(item.value)"
        />
        <n-input
          v-model:value="item.label"
          :maxlength="50"
          :disabled="props.disabled"
          :status="
            fieldConfig.customOptions.some((e) => e.value !== item.value && e.label === item.label)
              ? 'error'
              : undefined
          "
          class="flex-1"
          clearable
        ></n-input>
        <n-tooltip :delay="300" :show-arrow="false" class="crm-form-design--composition-item-tools-tip">
          <template #trigger>
            <n-button
              quaternary
              type="error"
              size="small"
              :disabled="
                props.disabled || fieldConfig.customOptions?.length === 1 || fieldConfig.defaultValue === item.value
              "
              class="text-btn-error p-[4px] text-[var(--text-n1)]"
              @click="handleOptionDelete(i)"
            >
              <CrmIcon type="iconicon_delete" :size="14" />
            </n-button>
          </template>
          {{ t('common.delete') }}
        </n-tooltip>
        <div
          v-if="fieldConfig.customOptions.some((e) => e.value !== item.value && e.label === item.label)"
          class="ml-[48px] w-full text-[12px] text-[var(--error-red)]"
        >
          {{ t('crmFormDesign.repeatOptionName') }}
        </div>
      </div>
    </VueDraggable>
  </component>
  <div v-if="fieldConfig.optionSource === 'custom'" class="flex items-center justify-center gap-[8px]">
    <div
      class="cursor-pointer text-[var(--primary-8)]"
      :class="props.disabled ? '!text-[var(--primary-4)]' : ''"
      @click="handleAddOption"
    >
      {{ t('crmFormDesign.addOption') }}
    </div>
    <n-divider vertical class="!m-0" />
    <div
      :class="
        props.disabled || fieldConfig.customOptions?.some((item) => item.value === 'other')
          ? 'cursor-not-allowed text-[var(--primary-4)]'
          : 'cursor-pointer text-[var(--primary-8)]'
      "
      @click="handleAddOtherOption"
    >
      {{ t('crmFormDesign.addOptionOther') }}
    </div>
    <n-divider vertical class="!m-0" />
    <div
      class="cursor-pointer text-[var(--primary-8)]"
      :class="props.disabled ? '!text-[var(--primary-4)]' : ''"
      @click="handleShowBatchEditModal"
    >
      {{ t('crmFormDesign.batchEdit') }}
    </div>
  </div>
  <CrmModal
    v-model:show="showModal"
    :title="t('crmFormDesign.batchEdit')"
    :positive-text="t('common.save')"
    @confirm="handleBatchEditConfirm"
  >
    <n-input
      v-model:value="batchEditValue"
      type="textarea"
      :autosize="{
        minRows: 3,
        maxRows: 10,
      }"
      clearable
    ></n-input>
    <div class="text-[12px] leading-[20px] text-[var(--text-n4)]">{{ t('crmFormDesign.batchEditTip') }}</div>
  </CrmModal>
</template>

<script setup lang="ts">
  import {
    CascaderOption,
    NButton,
    NCascader,
    NCheckbox,
    NCheckboxGroup,
    NDivider,
    NInput,
    NRadio,
    NRadioButton,
    NRadioGroup,
    NTooltip,
  } from 'naive-ui';
  import { cloneDeep, debounce } from 'lodash-es';
  import { VueDraggable } from 'vue-draggable-plus';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { findNodePathByKey, getGenerateId } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { fullFormSettingList } from '@/components/business/crm-form-create/config';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  import { getFormDesignConfig } from '@/api/modules';

  const props = defineProps<{
    disabled?: boolean;
  }>();

  const { t } = useI18n();

  const fieldConfig = defineModel<FormCreateField>('field', {
    default: null,
  });

  const isMultiple = computed(() =>
    [
      FieldTypeEnum.CHECKBOX,
      FieldTypeEnum.SELECT_MULTIPLE,
      FieldTypeEnum.MEMBER_MULTIPLE,
      FieldTypeEnum.DEPARTMENT_MULTIPLE,
      FieldTypeEnum.DATA_SOURCE_MULTIPLE,
    ].includes(fieldConfig.value.type)
  );

  const defaultRefOptions = fullFormSettingList
    .filter((item) => !!item.formKey)
    .map((i) => ({ ...i, value: i.formKey, isLeaf: false }));
  const refOptions = ref<CascaderOption[]>(cloneDeep(defaultRefOptions));

  // 获取第二层的数据
  const getRefData = async (key: string) => {
    const res = await getFormDesignConfig(key as FormDesignKeyEnum);
    return res.fields
      .filter((field) => field.optionSource === 'custom' && field.type === fieldConfig.value.type)
      .map((field) => ({ label: field.name, value: field.id, isLeaf: true, options: field.options }));
  };

  async function handleLoadRef(option: CascaderOption) {
    if (option.children) return;
    option.loading = true;
    let key = option.formKey as FormDesignKeyEnum;
    // 跟进记录和计划 key 特殊处理，因为各模块 key 不一致但是表单配置一致
    if (key.includes('record')) {
      key = 'record' as FormDesignKeyEnum;
    } else if (key.includes('plan')) {
      key = 'plan' as FormDesignKeyEnum;
    }
    option.children = await getRefData(key);
    // 没数据
    if (!option.children || option.children.length === 0) {
      option.children = [
        {
          label: t('common.noData'),
          value: 'no-data',
          disabled: true,
          isLeaf: true,
        },
      ];
    }
    option.loading = false;
  }

  function renderCascaderLabel(option: CascaderOption, checked: boolean) {
    if (option.value === 'no-data') {
      return h('span', { class: 'no-data' }, option.label);
    }
    return option.label;
  }

  // 更新第一层父节点的值，传入后端，方便后期回显
  async function updateFormKeyFromCascader(childValue: string) {
    const path = findNodePathByKey(refOptions.value, childValue, undefined, 'value');
    if (path) {
      fieldConfig.value.refFormKey = path.treePath[0].value;
      fieldConfig.value.options = path.options;
    } else {
      fieldConfig.value.refFormKey = '';
    }
  }

  // 初始化回显
  async function initEchoByPath(rootValue: string) {
    const rootOpt = refOptions.value.find((opt) => opt.value === rootValue);
    if (!rootOpt) return;
    rootOpt.children = await getRefData(rootValue);
  }

  watch(
    () => fieldConfig.value.id,
    async () => {
      refOptions.value = cloneDeep(defaultRefOptions);
      if (fieldConfig.value.optionSource === 'ref' && fieldConfig.value.refFormKey) {
        initEchoByPath(fieldConfig.value.refFormKey);
      }
    },
    { immediate: true }
  );

  watch(
    () => fieldConfig.value.optionSource,
    async (val) => {
      if (val === 'ref') {
        fieldConfig.value.defaultValue = isMultiple.value ? [] : '';
        if (fieldConfig.value.refFormKey && fieldConfig.value.refId) {
          await initEchoByPath(fieldConfig.value.refFormKey);
          updateFormKeyFromCascader(fieldConfig.value.refId);
        } else {
          fieldConfig.value.options = [];
        }
      } else {
        fieldConfig.value.options = fieldConfig.value.customOptions || fieldConfig.value.options;
      }
    }
  );

  const getComponent = computed(() => {
    if (isMultiple.value) {
      return NCheckboxGroup;
    }
    return NRadioGroup;
  });

  const handleRadioOptionClick = debounce((val: string | number) => {
    if (fieldConfig.value.defaultValue === val) {
      fieldConfig.value.defaultValue = '';
    } else {
      fieldConfig.value.defaultValue = val;
    }
  });

  const handleCheckBoxOptionClick = debounce((val: string | number) => {
    if (!Array.isArray(fieldConfig.value.defaultValue)) {
      fieldConfig.value.defaultValue = [];
    }
    const index = fieldConfig.value.defaultValue.indexOf(val);
    if (index > -1) {
      fieldConfig.value.defaultValue.splice(index, 1);
    } else {
      fieldConfig.value.defaultValue.push(val);
    }
  });

  function handleAddOption() {
    if (props.disabled) {
      return;
    }
    if (!fieldConfig.value.customOptions?.some((e) => e.value === 'other')) {
      fieldConfig.value.customOptions?.push({
        label: t('crmFormDesign.option', { i: fieldConfig.value.customOptions.length + 1 }),
        value: getGenerateId(),
      });
    } else {
      fieldConfig.value.customOptions?.splice(fieldConfig.value.customOptions.length - 1, 0, {
        label: t('crmFormDesign.option', { i: fieldConfig.value.customOptions.length }),
        value: getGenerateId(),
      });
    }
  }

  function handleAddOtherOption() {
    if (props.disabled || fieldConfig.value.customOptions?.some((e) => e.value === 'other')) {
      return;
    }
    fieldConfig.value.customOptions?.push({
      label: t('crmFormDesign.optionOther'),
      value: 'other',
    });
  }

  function setDefaultValue() {
    if (isMultiple.value) {
      fieldConfig.value.defaultValue = fieldConfig.value.defaultValue?.filter((e: any) =>
        fieldConfig.value.customOptions?.some((item) => item.value === e)
      );
    } else if (fieldConfig.value.customOptions?.every((e) => e.value !== fieldConfig.value.defaultValue)) {
      fieldConfig.value.defaultValue = '';
    }
  }

  function handleOptionDelete(i: number) {
    fieldConfig.value.customOptions?.splice(i, 1);
    setDefaultValue();
  }

  const showModal = ref(false);
  const batchEditValue = ref('');

  function handleShowBatchEditModal() {
    if (props.disabled) {
      return;
    }
    showModal.value = true;
    batchEditValue.value = fieldConfig.value.customOptions?.map((e) => e.label).join('\n') || '';
  }

  function handleBatchEditConfirm() {
    const resArr = Array.from(new Set(batchEditValue.value.split('\n')));
    if (resArr.length === 0) {
      fieldConfig.value.customOptions = [
        {
          label: t('crmFormDesign.option', { i: 1 }),
          value: getGenerateId(),
        },
        {
          label: t('crmFormDesign.option', { i: 2 }),
          value: getGenerateId(),
        },
        {
          label: t('crmFormDesign.option', { i: 3 }),
          value: getGenerateId(),
        },
      ];
    } else {
      const newOptions = resArr
        .map((e) => e.trim())
        .filter((e) => e)
        .map((e) => ({
          label: e.slice(0, 50),
          value: fieldConfig.value.customOptions?.find((item) => item.label === e)?.value || getGenerateId(),
        }));
      fieldConfig.value.customOptions = newOptions;
    }
    setDefaultValue();
    showModal.value = false;
  }
</script>

<style lang="less">
  // 没数据的样式
  .form-design-cascader .n-cascader-option.n-cascader-option--disabled:has(.no-data) {
    .n-cascader-option__label {
      text-align: center;
    }
    &:hover {
      background: transparent !important;
    }
  }
</style>
