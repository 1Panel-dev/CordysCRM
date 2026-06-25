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
              filterable
              :render-option="renderOption"
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
              @change-options="(options: SelectedUsersItem[]) => handleFieldValueOptionsUpdate(line, options)"
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

    <div class="mt-[16px]">
      <div class="mb-[16px] flex items-center gap-[8px]">
        <n-switch
          v-model:value="activeWebhookConfig.webHookEnable"
          :rubber-band="false"
          :disabled="props.readonly"
          @update:value="handleWebhookEnableChange"
        />
        <div class="font-semibold text-[var(--text-n1)]">{{ t('process.process.flow.webhook') }}</div>
        <n-tooltip :delay="300" placement="top-start" :show-arrow="false">
          <template #trigger>
            <CrmIcon
              type="iconicon_help_circle"
              :size="16"
              class="text-[var(--text-n4)] hover:text-[var(--primary-8)]"
            />
          </template>
          {{ t('process.process.flow.webhookTip') }}
        </n-tooltip>
      </div>

      <div v-if="activeWebhookConfig.webHookEnable" class="flex flex-col gap-[12px]">
        <n-form
          ref="webHookFormRef"
          class="process-setting-form"
          require-mark-placement="right"
          :model="activeWebhookConfig"
          label-placement="top"
        >
          <n-form-item
            class="after-approval-webhook-form-item"
            :label="t('process.process.flow.webhookDescription')"
            path="webHookDescribe"
          >
            <n-input
              v-model:value="activeWebhookConfig.webHookDescribe"
              :disabled="props.readonly"
              type="textarea"
              :maxlength="1000"
              :autosize="{ minRows: 3, maxRows: 5 }"
              :placeholder="t('process.process.flow.webhookDescriptionPlaceholder')"
            />
          </n-form-item>

          <n-form-item
            class="after-approval-webhook-form-item"
            :label="t('process.process.flow.webhookUrl')"
            path="webHookUrl"
            :rule="[
              {
                required: activeWebhookConfig.webHookEnable,
                message: t('common.notNull', { value: `${t('process.process.flow.webhookUrl')}` }),
                trigger: ['blur', 'input'],
              },
              {
                validator: validateWebhookUrl,
                trigger: ['blur', 'input'],
              },
            ]"
          >
            <n-input
              v-model:value="activeWebhookConfig.webHookUrl"
              :disabled="props.readonly"
              :maxlength="1000"
              :placeholder="t('process.process.flow.webhookUrlPlaceholder')"
            />
          </n-form-item>

          <n-form-item
            class="after-approval-webhook-form-item"
            :label="t('process.process.flow.webhookMethod')"
            path="webHookMethod"
          >
            <n-radio-group v-model:value="activeWebhookConfig.webHookMethod" :disabled="props.readonly">
              <div class="flex flex-col gap-[8px]">
                <n-radio :value="RequestEnum.POST">POST</n-radio>
                <n-radio :value="RequestEnum.GET">GET</n-radio>
              </div>
            </n-radio-group>
          </n-form-item>

          <n-form-item
            class="after-approval-webhook-form-item"
            :label="t('process.process.flow.webhookHeaders')"
            path="webHookHeader"
          >
            <n-input
              v-model:value="activeWebhookConfig.webHookHeader"
              :disabled="props.readonly"
              type="textarea"
              :maxlength="1000"
              :autosize="{ minRows: 3, maxRows: 6 }"
              :placeholder="webhookHeadersPlaceholder"
            />
          </n-form-item>

          <n-form-item
            class="after-approval-webhook-form-item"
            :label="t('process.process.flow.webhookBody')"
            path="webHookBody"
            :rule="[
              {
                required: activeWebhookConfig.webHookMethod === RequestEnum.POST,
                message: t('common.notNull', { value: `${t('process.process.flow.webhookBody')}` }),
                trigger: ['blur', 'input'],
              },
            ]"
          >
            <n-input
              v-model:value="activeWebhookConfig.webHookBody"
              :disabled="props.readonly"
              type="textarea"
              :maxlength="1000"
              :autosize="{ minRows: 6, maxRows: 10 }"
              :placeholder="webhookBodyPlaceholder"
            />
          </n-form-item>

          <div class="flex items-center gap-[8px]">
            <n-button type="primary" ghost :loading="testLoading" @click="handleTestWebhook">
              {{ t('common.testLink') }}
            </n-button>
            <n-button :disabled="props.readonly" @click="handleCancelWebhook">
              {{ t('common.cancel') }}
            </n-button>
          </div>
        </n-form>
      </div>
    </div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { computed, ref, shallowRef, VNode, VNodeChild, watch } from 'vue';
  import {
    type FormInst,
    NButton,
    NEmpty,
    NForm,
    NFormItem,
    NInput,
    NRadio,
    NRadioGroup,
    NScrollbar,
    NSelect,
    NSwitch,
    NTooltip,
    SelectOption,
    useMessage,
  } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldRuleEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { RequestEnum } from '@lib/shared/enums/httpEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getRuleType } from '@lib/shared/method/formCreate';
  import { validateHttpUrl } from '@lib/shared/method/validate';
  import type { SelectedUsersItem } from '@lib/shared/models/system/module';
  import type {
    ApprovalActionNode,
    ApprovalFieldUpdateConfig,
    ApprovalPostConfig,
    ApprovalWebhookConfig,
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
  import type {
    FormCreateField,
    FormCreateFieldOption,
    FormCreateFieldRule,
  } from '@/components/business/crm-form-create/types';

  import { getContractStatusConfig, getOrderStatusConfig, testApprovalWebHook } from '@/api/modules';
  import { quotationStatus } from '@/config/opportunity';
  import { defaultWebHookConfig } from '@/config/process';

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
  const Message = useMessage();
  const activePostTab = ref<'pass' | 'reject'>('pass');
  const formFields = ref<FormCreateField[]>([]);
  const contractStageOptions = ref<{ label: string; value: string }[]>([]);
  const orderStageOptions = ref<{ label: string; value: string }[]>([]);
  const formRef = ref<FormInst | null>(null);
  const webHookFormRef = ref<FormInst | null>(null);

  // 用户修改成员/部门后还没保存，后端 optionMap 仍是旧回显；这里临时缓存当前行的新回显，避免切换 tab 后显示回旧值
  const fieldValueOptionMap = shallowRef(new Map<ApprovalFieldUpdateConfig, SelectedUsersItem[]>());

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

  function renderOption({ node, option }: { node: VNode; option: SelectOption }): VNodeChild {
    return h(
      NTooltip,
      {
        delay: 300,
      },
      {
        trigger: () => node,
        default: () => option.label,
      }
    );
  }

  function ensureActivePostConfig(): ApprovalPostConfig {
    if (activePostTab.value === 'pass') {
      nodeConfig.value.passPostConfig ??= { fieldUpdateConfigs: [], webHookConfig: { ...defaultWebHookConfig } };
      return nodeConfig.value.passPostConfig;
    }

    nodeConfig.value.rejectPostConfig ??= { fieldUpdateConfigs: [], webHookConfig: { ...defaultWebHookConfig } };
    return nodeConfig.value.rejectPostConfig;
  }

  const activeWebhookConfig = computed<ApprovalWebhookConfig>(() => {
    if (!props.readonly) {
      const currentConfig = ensureActivePostConfig();

      currentConfig.webHookConfig ??= { ...defaultWebHookConfig };
      return currentConfig.webHookConfig;
    }

    const currentConfig = getCurrentPostConfig();

    if (!currentConfig) {
      return { ...defaultWebHookConfig };
    }

    return currentConfig.webHookConfig ?? { ...defaultWebHookConfig };
  });

  const fieldUpdateRows = computed(() => getCurrentPostConfig()?.fieldUpdateConfigs ?? []);
  const postFormModel = computed(() => ({
    fieldUpdateConfigs: fieldUpdateRows.value,
    webHookConfig: activeWebhookConfig.value,
  }));

  const webhookHeadersPlaceholder = '{"Content-Type":"application/json"}';
  const webhookBodyTemplate = `{
  "orderNo": "\${报价.编号}",
  "title": "\${报价.名称}",
  "status": "approved"
}`;
  const webhookBodyPlaceholder = computed(
    () => `${t('process.process.flow.webhookBodyPlaceholderPrefix')}\n${webhookBodyTemplate}`
  );

  // 操作对象与批量更新字段保持一致，只保留当前表单里允许更新的字段
  const editableFields = computed(() => {
    const isPoolForm = [FormDesignKeyEnum.CLUE_POOL, FormDesignKeyEnum.CUSTOMER_OPEN_SEA].includes(
      props.formType as FormDesignKeyEnum
    );

    const customFields = formFields.value.filter((field) => {
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

    const systemFieldMap: Partial<Record<FormDesignKeyEnum, FormCreateField[]>> = {
      [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: [
        {
          id: 'invalid',
          businessKey: 'invalid',
          name: t('common.status'),
          type: FieldTypeEnum.SELECT,
          options: quotationStatus as unknown as FormCreateFieldOption[],
        } as FormCreateField,
      ],
      [FormDesignKeyEnum.CONTRACT]: [
        {
          id: 'stage',
          businessKey: 'stage',
          name: t('contract.status'),
          type: FieldTypeEnum.SELECT,
          options: contractStageOptions.value,
        } as FormCreateField,
      ],
      [FormDesignKeyEnum.ORDER]: [
        {
          id: 'stage',
          businessKey: 'stage',
          name: t('order.status'),
          type: FieldTypeEnum.SELECT,
          options: orderStageOptions.value,
        } as FormCreateField,
      ],
    };

    const systemFields = systemFieldMap[props.formType as FormDesignKeyEnum] || [];

    return [...customFields, ...systemFields];
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
    if (
      [FieldTypeEnum.SELECT, FieldTypeEnum.RADIO].includes(field.type) &&
      field.options?.some((item) => typeof item.value === 'boolean')
    ) {
      return 'boolean';
    }
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

    return currentField;
  }

  const fieldValueConfigMap = computed(
    () => new Map(editableFields.value.map((field) => [field.id, buildFieldValueConfig(field)]))
  );

  function isEmptyFieldValue(value: unknown) {
    if (Array.isArray(value)) {
      return !value.length;
    }

    return value === null || value === undefined || value === '';
  }

  function getFieldValueOptions(line: ApprovalFieldUpdateConfig) {
    // 用户在页面上改过成员/部门后，优先用当前未保存的选择；否则再用后端 optionMap 回显
    const cachedOptions = fieldValueOptionMap.value.get(line);
    if (cachedOptions) {
      return cachedOptions;
    }

    if (!line.fieldId) {
      return [];
    }

    const selectedIds = new Set(
      Array.isArray(line.fieldValue) ? line.fieldValue.map(String) : [String(line.fieldValue)]
    );
    // optionMap 是“字段 -> 全部回显项”的集合，同一字段可能同时包含通过/驳回 tab 的值，需要按当前行 fieldValue 过滤
    return (props.optionMap?.[line.fieldId] ?? []).filter((item) => selectedIds.has(String(item.id)));
  }

  function getFieldValueConfig(line: ApprovalFieldUpdateConfig) {
    if (!line.fieldId) {
      return null;
    }

    const fieldConfig = fieldValueConfigMap.value.get(line.fieldId);
    if (!fieldConfig) {
      return null;
    }

    const initialOptions = getFieldValueOptions(line);
    if (initialOptions.length) {
      return {
        ...fieldConfig,
        initialOptions: [...initialOptions],
      };
    }

    return fieldConfig;
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

  function handleFieldValueOptionsUpdate(line: ApprovalFieldUpdateConfig, options: SelectedUsersItem[]) {
    fieldValueOptionMap.value = new Map(fieldValueOptionMap.value).set(line, [...options]);
  }

  // 当前行切换字段时，旧字段的成员/部门回显不能继续沿用
  function clearFieldValueOptions(line: ApprovalFieldUpdateConfig) {
    const nextMap = new Map(fieldValueOptionMap.value);
    nextMap.delete(line);
    fieldValueOptionMap.value = nextMap;
  }

  function isFieldUpdateSwitchDisabled(line: ApprovalFieldUpdateConfig) {
    return props.readonly || !line.fieldId || isEmptyFieldValue(line.fieldValue);
  }

  function handleFieldUpdate(line: ApprovalFieldUpdateConfig, fieldId: string) {
    if (props.readonly) {
      return;
    }

    line.fieldId = fieldId;
    clearFieldValueOptions(line);
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

  function validateWebhookUrl(_rule: unknown, value: string) {
    if (!activeWebhookConfig.value.webHookEnable || !value?.trim()) {
      return true;
    }

    return validateHttpUrl(value) || new Error(t('process.process.flow.webhookUrlInvalid'));
  }

  function handleCancelWebhook() {
    ensureActivePostConfig().webHookConfig = { ...defaultWebHookConfig };
  }

  const testLoading = ref(false);
  function handleTestWebhook() {
    if (props.readonly || !activeWebhookConfig.value.webHookEnable) {
      return;
    }
    webHookFormRef.value?.validate(async (errors) => {
      if (!errors) {
        testLoading.value = true;
        try {
          await testApprovalWebHook(activeWebhookConfig.value);
          Message.success(t('org.testConnectionSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        } finally {
          testLoading.value = false;
        }
      }
    });
  }

  watch(
    () => activePostTab.value,
    (val) => {
      if (val) {
        webHookFormRef.value?.restoreValidation();
      }
    }
  );

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

    config.webHookConfig = {
      ...defaultWebHookConfig,
      ...config.webHookConfig,
    };
  }

  function handleWebhookEnableChange(val: boolean) {
    if (!val) {
      ensureActivePostConfig().webHookConfig = { ...defaultWebHookConfig };
    }
  }

  async function initStage() {
    contractStageOptions.value = [];
    orderStageOptions.value = [];
    try {
      if (props.formType === FormDesignKeyEnum.CONTRACT) {
        const contractStageConfig = await getContractStatusConfig();
        contractStageOptions.value = contractStageConfig.stageConfigList.map((item) => ({
          label: item.name,
          value: item.id,
        }));
      }

      if (props.formType === FormDesignKeyEnum.ORDER) {
        const orderStageConfig = await getOrderStatusConfig();
        orderStageOptions.value = orderStageConfig.stageConfigList.map((item) => ({
          label: item.name,
          value: item.id,
        }));
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function loadFormFields() {
    try {
      const api =
        getFormConfigApiMap[props.formType as FormDesignKeyEnum] ??
        getFormConfigApiMap[FormDesignKeyEnum.OPPORTUNITY_QUOTATION];
      const res = await api();
      formFields.value = res.fields;
      initStage();
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
