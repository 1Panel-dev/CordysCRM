<template>
  <n-scrollbar class="p-[16px]">
    <CrmTab
      v-model:active-tab="activePostTab"
      no-content
      :tab-list="postTabList"
      type="segment"
      class="after-approval-tabs mb-[16px]"
    />

    <div class="mb-[8px] font-semibold text-[var(--text-n1)]">
      {{ t('process.process.flow.fieldUpdate') }}
    </div>

    <div class="rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[8px]">
      <n-empty v-if="!fieldUpdateRows.length && props.readonly" :description="t('common.noData')" size="small" />
      <n-form v-else ref="formRef" :model="postFormModel" :show-label="false" class="after-approval-update-form">
        <div v-for="(line, index) in fieldUpdateRows" :key="index" class="mb-[8px] flex items-start gap-[8px]">
          <n-form-item
            :path="`fieldUpdateConfigs.${index}.fieldId`"
            class="flex-1"
            :show-label="false"
            :rule="[{ required: true, message: t('common.required'), trigger: 'change' }]"
          >
            <n-select
              v-model:value="line.fieldId"
              :disabled="props.readonly"
              :options="getFieldOptions(line.fieldId)"
              :placeholder="t('common.pleaseSelect')"
              @update:value="(value) => handleFieldUpdate(line, value)"
            />
          </n-form-item>

          <n-form-item class="text-[12px] text-[var(--text-n2)]">
            {{ t('process.process.flow.updateTo') }}
          </n-form-item>

          <div class="flex-1">
            <component
              :is="getFieldValueComponent(line)"
              v-if="getFieldValueComponent(line) && getFieldValueConfig(line)"
              :value="getFieldValueModel(line)"
              :disabled="props.readonly"
              :path="`fieldUpdateConfigs.${index}.fieldValue`"
              :field-config="getFieldValueConfig(line)!"
              :form-detail="line"
              :need-init-detail="true"
              @update:value="(value: unknown) => handleFieldValueUpdate(line, value)"
            />
            <n-form-item
              v-else
              :show-label="false"
              :path="`fieldUpdateConfigs.${index}.fieldValue`"
              :rule="[{ required: true, message: t('common.required'), trigger: 'change' }]"
            >
              <n-input disabled :placeholder="t('common.pleaseSelect')" />
            </n-form-item>
          </div>
          <n-form-item :path="`${index}.enable`">
            <n-switch v-model:value="line.enable" :disabled="isFieldUpdateSwitchDisabled(line)" />
          </n-form-item>
          <n-button ghost class="px-[7px]" :disabled="props.readonly" @click="handleDeleteFieldUpdate(index)">
            <template #icon>
              <CrmIcon type="iconicon_minus_circle" :size="16" />
            </template>
          </n-button>
        </div>
      </n-form>

      <n-button
        v-if="!props.readonly"
        type="primary"
        text
        :disabled="isAddFieldUpdateDisabled"
        @click="handleAddFieldUpdate"
      >
        <template #icon>
          <CrmIcon type="iconicon_add" :size="16" />
        </template>
        {{ t('common.add') }}
      </n-button>
    </div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { type FormInst, NButton, NEmpty, NForm, NFormItem, NInput, NScrollbar, NSelect, NSwitch } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldRuleEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getRuleType } from '@lib/shared/method/formCreate';
  import type {
    ApprovalActionNode,
    ApprovalFieldUpdateConfig,
    ApprovalPostConfig,
  } from '@lib/shared/models/system/process';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import DataSource from '@/components/business/crm-form-create/components/advanced/dataSource.vue';
  import Industry from '@/components/business/crm-form-create/components/advanced/industry.vue';
  import Link from '@/components/business/crm-form-create/components/advanced/link.vue';
  import Location from '@/components/business/crm-form-create/components/advanced/location.vue';
  import Phone from '@/components/business/crm-form-create/components/advanced/phone.vue';
  import Checkbox from '@/components/business/crm-form-create/components/basic/checkbox.vue';
  import DateTime from '@/components/business/crm-form-create/components/basic/dateTime.vue';
  import InputNumber from '@/components/business/crm-form-create/components/basic/inputNumber.vue';
  import MemberSelect from '@/components/business/crm-form-create/components/basic/memberSelect.vue';
  import Radio from '@/components/business/crm-form-create/components/basic/radio.vue';
  import Select from '@/components/business/crm-form-create/components/basic/select.vue';
  import SingleText from '@/components/business/crm-form-create/components/basic/singleText.vue';
  import TagInput from '@/components/business/crm-form-create/components/basic/tagInput.vue';
  import Textarea from '@/components/business/crm-form-create/components/basic/textarea.vue';
  import { getFormConfigApiMap, rules } from '@/components/business/crm-form-create/config';
  import type { FormCreateField, FormCreateFieldRule } from '@/components/business/crm-form-create/types';

  defineOptions({
    name: 'AfterApprovalTab',
  });

  const props = defineProps<{
    formType: string;
    optionMap?: Record<string, any[]>;
    readonly?: boolean;
  }>();

  const nodeConfig = defineModel<ApprovalActionNode>('nodeConfig', {
    required: true,
  });

  const { t } = useI18n();
  const activePostTab = ref<'pass' | 'reject'>('pass');
  const formFields = ref<FormCreateField[]>([]);
  const formRef = ref<FormInst | null>(null);

  const postTabList = [
    {
      name: 'pass',
      tab: t('process.process.flow.passAction'),
    },
    {
      name: 'reject',
      tab: t('process.process.flow.rejectAction'),
    },
  ];

  // 通过后操作 / 驳回后操作共用同一套 UI，这里按当前 tab 取对应配置
  function getCurrentPostConfig(): ApprovalPostConfig | undefined {
    return activePostTab.value === 'pass' ? nodeConfig.value.passPostConfig : nodeConfig.value.rejectPostConfig;
  }

  function ensureActivePostConfig(): ApprovalPostConfig {
    if (activePostTab.value === 'pass') {
      nodeConfig.value.passPostConfig ??= { fieldUpdateConfigs: [] };
      return nodeConfig.value.passPostConfig;
    }

    nodeConfig.value.rejectPostConfig ??= { fieldUpdateConfigs: [] };
    return nodeConfig.value.rejectPostConfig;
  }

  const fieldUpdateRows = computed(() => getCurrentPostConfig()?.fieldUpdateConfigs ?? []);
  const postFormModel = computed(() => ({
    fieldUpdateConfigs: fieldUpdateRows.value,
  }));

  // 操作对象与批量更新字段保持一致，只保留当前表单里允许更新的字段
  const editableFields = computed(() => {
    const isPoolForm = [FormDesignKeyEnum.CLUE_POOL, FormDesignKeyEnum.CUSTOMER_OPEN_SEA].includes(
      props.formType as FormDesignKeyEnum
    );

    return formFields.value.filter((field) => {
      const baseCondition =
        ![
          FieldTypeEnum.DIVIDER,
          FieldTypeEnum.PICTURE,
          FieldTypeEnum.SERIAL_NUMBER,
          FieldTypeEnum.ATTACHMENT,
          FieldTypeEnum.SUB_PRICE,
          FieldTypeEnum.SUB_PRODUCT,
          FieldTypeEnum.FORMULA,
        ].includes(field.type) &&
        !field.resourceFieldId?.length &&
        field.defaultValueType !== 'formula';

      return isPoolForm ? baseCondition && field.businessKey !== 'owner' : baseCondition;
    });
  });

  const editableFieldMap = computed(() => new Map(editableFields.value.map((field) => [field.id, field])));
  const fieldValueComponentMap = {
    [FieldTypeEnum.SELECT]: Select,
    [FieldTypeEnum.SELECT_MULTIPLE]: Select,
    [FieldTypeEnum.INPUT_NUMBER]: InputNumber,
    [FieldTypeEnum.MEMBER]: MemberSelect,
    [FieldTypeEnum.MEMBER_MULTIPLE]: MemberSelect,
    [FieldTypeEnum.DEPARTMENT]: MemberSelect,
    [FieldTypeEnum.DEPARTMENT_MULTIPLE]: MemberSelect,
    [FieldTypeEnum.RADIO]: Radio,
    [FieldTypeEnum.CHECKBOX]: Checkbox,
    [FieldTypeEnum.TEXTAREA]: Textarea,
    [FieldTypeEnum.DATE_TIME]: DateTime,
    [FieldTypeEnum.INPUT]: SingleText,
    [FieldTypeEnum.INPUT_MULTIPLE]: TagInput,
    [FieldTypeEnum.DATA_SOURCE]: DataSource,
    [FieldTypeEnum.DATA_SOURCE_MULTIPLE]: DataSource,
    [FieldTypeEnum.LOCATION]: Location,
    [FieldTypeEnum.PHONE]: Phone,
    [FieldTypeEnum.INDUSTRY]: Industry,
    [FieldTypeEnum.LINK]: Link,
  } as const;
  type SupportedFieldValueType = keyof typeof fieldValueComponentMap;

  const selectedFieldIdSet = computed(() => new Set(fieldUpdateRows.value.map((item) => item.fieldId).filter(Boolean)));
  const isAddFieldUpdateDisabled = computed(
    () => editableFields.value.length > 0 && selectedFieldIdSet.value.size >= editableFields.value.length
  );

  function getFieldOptions(currentFieldId: string | null) {
    // 每个字段只能选择一次
    return editableFields.value
      .filter((field) => field.id === currentFieldId || !selectedFieldIdSet.value.has(field.id))
      .map((field) => ({
        label: field.name,
        value: field.id,
      }));
  }

  function getCurrentField(fieldId: string) {
    return editableFieldMap.value.get(fieldId) ?? null;
  }

  function getInitialFieldValue(field: FormCreateField) {
    if ([FieldTypeEnum.DATE_TIME, FieldTypeEnum.INPUT_NUMBER, FieldTypeEnum.DATA_SOURCE].includes(field.type)) {
      return null;
    }

    if (getRuleType(field) === 'array') {
      return [];
    }

    return '';
  }

  function getFieldValueRuleType(field: FormCreateField) {
    return field.type === FieldTypeEnum.DATA_SOURCE ? 'string' : getRuleType(field);
  }

  function buildFieldValueConfig(field: FormCreateField) {
    // 复用原表单字段配置来渲染“更新为”控件，同时隐藏标题并补齐校验
    const currentField = {
      ...cloneDeep(field),
      name: t('common.batchUpdate'),
      description: '',
      showLabel: false,
      editable: props.readonly ? false : field.editable,
    };

    const fullRules: FormCreateFieldRule[] = [];
    (field.rules || []).forEach((rule) => {
      const staticRule = cloneDeep(rules.find((item) => item.key === rule.key));

      if (!staticRule) {
        return;
      }

      staticRule.message = t(staticRule.message as string, { value: t(field.name) });
      staticRule.type = getFieldValueRuleType(field);
      staticRule.regex = rule.regex;

      if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(field.type)) {
        staticRule.trigger = 'none';
      }

      fullRules.push(staticRule);
    });

    // 如果原字段没配必填规则，这里补一个
    if (!fullRules.some((rule) => rule.key === 'required')) {
      fullRules.unshift({
        key: FieldRuleEnum.REQUIRED,
        required: true,
        message: t('common.required'),
        trigger: ['blur', 'change'],
        type: getFieldValueRuleType(field),
      });
    }

    currentField.rules = fullRules;

    const initialOptions = props.optionMap?.[field.id] ?? [];
    if (initialOptions.length) {
      currentField.initialOptions = [...initialOptions];
    }

    return currentField;
  }

  const fieldValueConfigMap = computed(
    () => new Map(editableFields.value.map((field) => [field.id, buildFieldValueConfig(field)]))
  );

  function getFieldValueConfig(line: ApprovalFieldUpdateConfig) {
    if (!line.fieldId) {
      return null;
    }

    return fieldValueConfigMap.value.get(line.fieldId) ?? null;
  }

  function isSupportedFieldValueType(fieldType: FieldTypeEnum): fieldType is SupportedFieldValueType {
    return fieldType in fieldValueComponentMap;
  }

  function getFieldValueComponent(line: ApprovalFieldUpdateConfig) {
    const fieldType = getFieldValueConfig(line)?.type;
    return fieldType && isSupportedFieldValueType(fieldType) ? fieldValueComponentMap[fieldType] : null;
  }

  function getFieldValueModel(line: ApprovalFieldUpdateConfig) {
    const fieldType = getFieldValueConfig(line)?.type;
    if (fieldType === FieldTypeEnum.DATA_SOURCE) {
      return line.fieldValue ? [line.fieldValue] : [];
    }

    return line.fieldValue;
  }

  function handleFieldValueUpdate(line: ApprovalFieldUpdateConfig, value: unknown) {
    const fieldType = getFieldValueConfig(line)?.type;
    if (fieldType === FieldTypeEnum.DATA_SOURCE) {
      line.fieldValue = Array.isArray(value) ? value[0] ?? null : value;
      return;
    }

    line.fieldValue = value;
  }

  function isEmptyFieldValue(value: unknown) {
    if (Array.isArray(value)) {
      return !value.length;
    }

    return value === null || value === undefined || value === '';
  }

  function isFieldUpdateSwitchDisabled(line: ApprovalFieldUpdateConfig) {
    return props.readonly || !line.fieldId || isEmptyFieldValue(line.fieldValue);
  }

  function handleFieldUpdate(line: ApprovalFieldUpdateConfig, fieldId: string) {
    if (props.readonly) {
      return;
    }

    line.fieldId = fieldId;
    const currentField = fieldId ? getCurrentField(fieldId) : null;
    line.fieldValue = currentField ? getInitialFieldValue(currentField) : '';
  }

  function handleDeleteFieldUpdate(index: number) {
    if (props.readonly) {
      return;
    }

    ensureActivePostConfig().fieldUpdateConfigs.splice(index, 1);
  }

  function handleAddFieldUpdate() {
    if (props.readonly || isAddFieldUpdateDisabled.value) {
      return;
    }

    formRef.value?.validate((errors) => {
      if (errors) {
        return;
      }

      // 只有当前规则都填完整了，才允许继续新增下一条
      ensureActivePostConfig().fieldUpdateConfigs.push({
        fieldId: null,
        fieldValue: '',
        enable: true,
      });
    });
  }

  function normalizeFieldUpdateConfigs(config?: ApprovalPostConfig) {
    if (props.readonly || !config) {
      return;
    }

    const selectedFieldIds = new Set<string>();

    config.fieldUpdateConfigs = config.fieldUpdateConfigs.filter((item) => {
      if (!item.fieldId) {
        return true;
      }

      if (!editableFieldMap.value.has(item.fieldId) || selectedFieldIds.has(item.fieldId)) {
        return false;
      }

      selectedFieldIds.add(item.fieldId);
      return true;
    });
  }

  async function loadFormFields() {
    try {
      const api =
        getFormConfigApiMap[props.formType as FormDesignKeyEnum] ??
        getFormConfigApiMap[FormDesignKeyEnum.OPPORTUNITY_QUOTATION];
      const res = await api();
      formFields.value = res.fields;
      if (!props.readonly) {
        normalizeFieldUpdateConfigs(nodeConfig.value.passPostConfig);
        normalizeFieldUpdateConfigs(nodeConfig.value.rejectPostConfig);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => props.formType,
    () => {
      loadFormFields();
    },
    {
      immediate: true,
    }
  );
</script>

<style scoped lang="less">
  .after-approval-tabs {
    :deep(.n-tabs-rail) {
      width: 100%;
    }
  }
  .after-approval-update-form {
    :deep(.n-form-item-label) {
      display: none;
    }
    :deep(.n-form-item.n-form-item--top-labelled) {
      grid-template-areas: 'blank' 'feedback';
    }
    :deep(.n-form-item-feedback-wrapper) {
      display: none;
    }
    :deep(.n-form-item-blank--error) + .n-form-item-feedback-wrapper {
      display: inline-block;
    }
  }
</style>
