<template>
  <component :is="getComponent" v-if="fieldConfig.options" :value="fieldConfig.defaultValue" :disabled="props.disabled">
    <!-- 通过draggable属性控制带.draggable类的元素可拖拽，实现部分元素不允许拖拽 -->
    <VueDraggable
      v-model="fieldConfig.options"
      :animation="150"
      draggable=".draggable"
      handle=".handle"
      class="flex flex-col gap-[8px]"
    >
      <div
        v-for="(item, i) in fieldConfig.options"
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
            fieldConfig.options.some((e) => e.value !== item.value && e.label === item.label) ? 'error' : undefined
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
              :disabled="props.disabled || fieldConfig.options?.length === 1 || fieldConfig.defaultValue === item.value"
              class="text-btn-error p-[4px] text-[var(--text-n1)]"
              @click="handleOptionDelete(i)"
            >
              <CrmIcon type="iconicon_delete" :size="14" />
            </n-button>
          </template>
          {{ t('common.delete') }}
        </n-tooltip>
        <div
          v-if="fieldConfig.options.some((e) => e.value !== item.value && e.label === item.label)"
          class="ml-[48px] w-full text-[12px] text-[var(--error-red)]"
        >
          {{ t('crmFormDesign.repeatOptionName') }}
        </div>
      </div>
    </VueDraggable>
  </component>
  <div class="flex items-center justify-center gap-[8px]">
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
        props.disabled || fieldConfig.options?.some((item) => item.value === 'other')
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
  import { NButton, NCheckbox, NCheckboxGroup, NDivider, NInput, NRadio, NRadioGroup, NTooltip } from 'naive-ui';
  import { debounce } from 'lodash-es';
  import { VueDraggable } from 'vue-draggable-plus';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

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
    if (!fieldConfig.value.options?.some((e) => e.value === 'other')) {
      fieldConfig.value.options?.push({
        label: t('crmFormDesign.option', { i: fieldConfig.value.options.length + 1 }),
        value: getGenerateId(),
      });
    } else {
      fieldConfig.value.options?.splice(fieldConfig.value.options.length - 1, 0, {
        label: t('crmFormDesign.option', { i: fieldConfig.value.options.length }),
        value: getGenerateId(),
      });
    }
  }

  function handleAddOtherOption() {
    if (props.disabled || fieldConfig.value.options?.some((e) => e.value === 'other')) {
      return;
    }
    fieldConfig.value.options?.push({
      label: t('crmFormDesign.optionOther'),
      value: 'other',
    });
  }

  function setDefaultValue() {
    if (isMultiple.value) {
      fieldConfig.value.defaultValue = fieldConfig.value.defaultValue?.filter((e: any) =>
        fieldConfig.value.options?.some((item) => item.value === e)
      );
    } else if (fieldConfig.value.options?.every((e) => e.value !== fieldConfig.value.defaultValue)) {
      fieldConfig.value.defaultValue = '';
    }
  }

  function handleOptionDelete(i: number) {
    fieldConfig.value.options?.splice(i, 1);
    setDefaultValue();
  }

  const showModal = ref(false);
  const batchEditValue = ref('');

  function handleShowBatchEditModal() {
    if (props.disabled) {
      return;
    }
    showModal.value = true;
    batchEditValue.value = fieldConfig.value.options?.map((e) => e.label).join('\n') || '';
  }

  function handleBatchEditConfirm() {
    const resArr = Array.from(new Set(batchEditValue.value.split('\n')));
    if (resArr.length === 0) {
      fieldConfig.value.options = [
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
          value: fieldConfig.value.options?.find((item) => item.label === e)?.value || getGenerateId(),
        }));
      fieldConfig.value.options = newOptions;
    }
    setDefaultValue();
    showModal.value = false;
  }
</script>

<style lang="less" scoped></style>
