<template>
  <div class="p-[16px]">
    <div v-if="fieldConfig">
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.fieldTitle') }}</div>
        <n-input v-model:value="fieldConfig.name" :maxlength="255" :placeholder="t('common.placeholder')" clearable />
        <n-checkbox v-model:checked="fieldConfig.showLabel">
          {{ t('crmFormDesign.showTitle') }}
        </n-checkbox>
      </div>
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.desc') }}</div>
        <n-input v-model:value="fieldConfig.description" type="textarea" :maxlength="1000" clearable />
      </div>
      <div
        v-if="
          ![
            FieldTypeEnum.MEMBER_SINGLE,
            FieldTypeEnum.MEMBER_MULTIPLE,
            FieldTypeEnum.DEPARTMENT_SINGLE,
            FieldTypeEnum.DEPARTMENT_MULTIPLE,
          ].includes(fieldConfig.type)
        "
        class="crm-form-design-config-item"
      >
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.placeholder') }}
          <n-tooltip trigger="hover">
            <template #trigger>
              <CrmIcon type="iconicon_help_circle" class="cursor-pointer hover:text-[var(--primary-1)]" />
            </template>
            {{ t('crmFormDesign.placeholderTip') }}
          </n-tooltip>
        </div>
        <n-input v-model:value="fieldConfig.placeholder" :maxlength="56" clearable />
      </div>
      <!-- inputNumber数字输入属性 -->
      <div v-if="fieldConfig.type === FieldTypeEnum.INPUT_NUMBER" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.format') }}</div>
        <n-radio-group v-model:value="fieldConfig.numberFormat" name="radiogroup" class="flex">
          <n-radio-button value="number" class="flex-1 text-center">{{ t('crmFormDesign.number') }}</n-radio-button>
          <n-radio-button value="percent" class="flex-1 text-center">{{ t('crmFormDesign.percent') }}</n-radio-button>
        </n-radio-group>
        <n-checkbox v-model:checked="fieldConfig.decimalPlaces" @update-checked="() => (fieldConfig.precision = 0)">
          {{ t('crmFormDesign.saveFloat') }}
        </n-checkbox>
        <n-checkbox v-if="fieldConfig.numberFormat === 'number'" v-model:checked="fieldConfig.showThousandsSeparator">
          {{ t('crmFormDesign.showThousandSeparator') }}
        </n-checkbox>
        <div v-if="fieldConfig.decimalPlaces || fieldConfig.showThousandsSeparator" class="flex items-center gap-[8px]">
          <n-input-number
            v-if="fieldConfig.decimalPlaces"
            v-model:value="fieldConfig.precision"
            :min="0"
            class="flex-1"
          />
          <div
            class="flex flex-1 items-center gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] px-[8px] py-[4px]"
          >
            <div class="text-[var(--text-n4)]">{{ t('common.preview') }}</div>
            {{ numberPreview }}
          </div>
        </div>
      </div>
      <!-- inputNumber End -->
      <!-- date 日期输入属性 -->
      <div v-if="fieldConfig.type === FieldTypeEnum.DATE_TIME" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('common.type') }}
        </div>
        <n-select v-model:value="fieldConfig.datetype" :options="dateTypeOptions" />
      </div>
      <!-- date End -->
      <!-- 选项属性 -->
      <div v-if="fieldConfig.options" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.option') }}
        </div>
        <optionConfig v-model:field="fieldConfig" />
      </div>
      <!-- options End -->
      <div
        v-if="[FieldTypeEnum.RADIO, FieldTypeEnum.CHECKBOX].includes(fieldConfig.type)"
        class="crm-form-design-config-item"
      >
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.direction') }}
        </div>
        <n-radio-group v-model:value="fieldConfig.direction" name="radiogroup" class="flex">
          <n-radio-button value="vertical" class="flex-1 text-center">
            {{ t('crmFormDesign.verticalSort') }}
          </n-radio-button>
          <n-radio-button value="horizontal" class="flex-1 text-center">
            {{ t('crmFormDesign.horizontalSort') }}
          </n-radio-button>
        </n-radio-group>
      </div>
      <div v-if="!fieldConfig.options || fieldConfig.options.length === 0" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.defaultValue') }}</div>
        <n-input-number
          v-if="fieldConfig.type === FieldTypeEnum.INPUT_NUMBER"
          v-model:value="fieldConfig.defaultValue"
          :show-button="false"
          :min="0"
        />
        <n-date-picker
          v-else-if="fieldConfig.type === FieldTypeEnum.DATE_TIME"
          v-model:value="fieldConfig.defaultValue"
          :type="fieldConfig.datetype"
          class="w-full"
        ></n-date-picker>
        <CrmUserTagSelector
          v-else-if="[FieldTypeEnum.MEMBER_SINGLE, FieldTypeEnum.MEMBER_MULTIPLE].includes(fieldConfig.type)"
          v-model:selected-list="fieldConfig.defaultValue"
          :multiple="fieldConfig.type === FieldTypeEnum.MEMBER_MULTIPLE"
          :drawer-title="t('crmFormDesign.selectMember')"
          :ok-text="t('common.confirm')"
          :member-types="[]"
        />
        <CrmUserTagSelector
          v-else-if="[FieldTypeEnum.DEPARTMENT_SINGLE, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(fieldConfig.type)"
          v-model:selected-list="fieldConfig.defaultValue"
          :multiple="fieldConfig.type === FieldTypeEnum.DEPARTMENT_MULTIPLE"
          :drawer-title="t('crmFormDesign.selectMember')"
          :ok-text="t('common.confirm')"
          :member-types="[]"
        />
        <n-input v-else v-model:value="fieldConfig.defaultValue" :maxlength="255" clearable />
      </div>
      <div v-if="showRules.length > 0" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.validator') }}</div>
        <n-checkbox-group v-model:value="checkedRules">
          <n-space item-class="w-full">
            <n-checkbox v-for="rule of showRules" :key="rule.key" :value="rule.key">
              {{ t(rule.label, { value: t(fieldConfig.name) }) }}
            </n-checkbox>
          </n-space>
        </n-checkbox-group>
      </div>
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.fieldPermission') }}
        </div>
        <n-checkbox v-model:checked="fieldConfig.readable">
          {{ t('crmFormDesign.readable') }}
        </n-checkbox>
        <n-checkbox v-model:checked="fieldConfig.editable">
          {{ t('crmFormDesign.editable') }}
        </n-checkbox>
      </div>
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.fieldWidth') }}
        </div>
        <n-radio-group v-model:value="fieldConfig.fieldWidth" name="radiogroup" class="flex">
          <n-radio-button :value="1 / 4" class="!px-[8px]"> 1/4 </n-radio-button>
          <n-radio-button :value="1 / 3" class="!px-[8px]"> 1/3 </n-radio-button>
          <n-radio-button :value="1 / 2" class="!px-[8px]"> 1/2 </n-radio-button>
          <n-radio-button :value="2 / 3" class="!px-[8px]"> 2/3 </n-radio-button>
          <n-radio-button :value="3 / 4" class="!px-[8px]"> 3/4 </n-radio-button>
          <n-radio-button :value="1" class="!px-[8px]">
            {{ t('crmFormDesign.wholeLine') }}
          </n-radio-button>
        </n-radio-group>
      </div>
    </div>
    <div v-else class="flex justify-center py-[44px] text-[var(--text-n4)]">
      {{ t('crmFormDesign.fieldConfigEmptyTip') }}
    </div>
  </div>
</template>

<script setup lang="ts">
  import {
    NCheckbox,
    NCheckboxGroup,
    NDatePicker,
    NInput,
    NInputNumber,
    NRadioButton,
    NRadioGroup,
    NSelect,
    NSpace,
    NTooltip,
  } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { rules } from '@/components/business/crm-form-create/config';
  import { FieldTypeEnum } from '@/components/business/crm-form-create/enum';
  import { FormCreateField } from '@/components/business/crm-form-create/types';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';
  import optionConfig from './optionConfig.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const fieldConfig = defineModel<FormCreateField>('field', {
    default: null,
  });

  const showRules = computed(() => {
    return rules.filter((rule) => rule.key && fieldConfig.value.showRules?.includes(rule.key));
  });

  const checkedRules = ref<string[]>([]);

  watch(
    () => checkedRules.value,
    (value) => {
      fieldConfig.value.rules = showRules.value
        .filter((rule) => rule.key && value.includes(rule.key))
        .map((rule) => ({
          ...rule,
          label: t(rule.label, { value: t(fieldConfig.value.name) }),
        }));
    }
  );

  const numberPreview = computed(() => {
    const tempVal = 9999;
    if (fieldConfig.value.numberFormat === 'percent') {
      if (fieldConfig.value.decimalPlaces && fieldConfig.value.precision) {
        return `99.${'0'.repeat(fieldConfig.value.precision)}%`;
      }
      return '99%';
    }
    if (fieldConfig.value.showThousandsSeparator && fieldConfig.value.decimalPlaces && fieldConfig.value.precision) {
      return `${tempVal.toLocaleString('en-US')}.${'0'.repeat(fieldConfig.value.precision)}`;
    }
    if (fieldConfig.value.showThousandsSeparator) {
      return tempVal.toLocaleString('en-US');
    }
    if (fieldConfig.value.decimalPlaces && fieldConfig.value.precision) {
      return `9999.${'0'.repeat(fieldConfig.value.precision)}`;
    }
    return 9999;
  });

  const dateTypeOptions = [
    {
      label: t('crmFormDesign.monthType'),
      value: 'month',
    },
    {
      label: t('crmFormDesign.dayType'),
      value: 'date',
    },
    {
      label: t('crmFormDesign.timeType'),
      value: 'datetime',
    },
  ];
</script>

<style lang="less" scoped>
  .crm-form-design-config-item {
    @apply flex flex-col;

    gap: 8px;
    &:not(:first-child) {
      margin-top: 24px;
    }
    .crm-form-design-config-item-title {
      @apply flex items-center font-semibold;

      gap: 4px;
    }
  }
</style>
