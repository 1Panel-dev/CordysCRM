<template>
  <CrmDrawer
    v-model:show="show"
    :width="1000"
    :footer="false"
    :title="textConfig.title"
    body-content-class="bg-[var(--text-n9)]"
    :loading="drawerLoading"
    :mask-closable="false"
    @mask-click="handleMaskClick"
  >
    <div class="h-full">
      <n-tabs v-model:value="tabName" type="line" size="medium">
        <n-tab-pane name="statusConfig" :tab="textConfig.sectionTitle">
          <div class="bg-[var(--text-n10)] p-[24px]">
            <div class="mb-[16px] text-[16px] font-semibold">{{ t('crmStatusConfigDrawer.configCondition') }}</div>
            <div class="bg-[var(--text-n9)] p-[16px]">
              <div class="flex items-center gap-[8px]">
                <div class="w-[12px]"></div>
                <div v-for="(ele, index) of textConfig.columnTitles" :key="`ele-${index}`" class="w-full flex-1">
                  {{ ele }}
                </div>
                <div class="w-[68px]"></div>
              </div>

              <CrmBatchForm
                ref="batchFormRef"
                :models="formItemModel"
                :default-list="form.list"
                validate-when-add
                draggable
                class="!p-0"
                :move="handleMove"
                @save-row="handleSave"
                @drag="dragEnd"
                @cancel-row="handleCancelRow"
              >
                <template #extra="{ element }">
                  <CrmMoreAction
                    :options="getDropdownOptions(element)"
                    placement="bottom"
                    @select="handleActionSelect($event, element)"
                  >
                    <n-button ghost class="px-[7px]">
                      <template #icon>
                        <CrmIcon type="iconicon_ellipsis" :size="16" />
                      </template>
                    </n-button>
                  </CrmMoreAction>

                  <div v-if="getDropdownOptions(element).length === 0" class="w-[32px]"></div>
                </template>
              </CrmBatchForm>
            </div>
          </div>
          <div v-if="props.type === FormDesignKeyEnum.BUSINESS" class="bg-[var(--text-n10)] p-[24px]">
            <div class="mb-[16px] mt-[24px]">
              {{ textConfig.rollbackTitle }}
            </div>
            <div
              v-for="item in textConfig.switches"
              :key="item.key"
              :class="[item.key === 'completedStageRollback' ? 'mt-[16px]' : '', 'flex items-center gap-[8px]']"
            >
              <n-switch v-model:value="form[item.key]" @update-value="handleSwitchChange" />
              {{ item.label }}
              <n-tooltip trigger="hover" placement="right">
                <template #trigger>
                  <CrmIcon
                    type="iconicon_help_circle"
                    :size="16"
                    class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                  />
                </template>
                {{ item.tip }}
              </n-tooltip>
            </div>
          </div>
        </n-tab-pane>
        <n-tab-pane
          v-if="props.type !== FormDesignKeyEnum.BUSINESS"
          name="flowConfiguration"
          :tab="t('crmStatusConfigDrawer.flowConfiguration')"
        >
          <div class="bg-[var(--text-n10)] p-[24px]">
            <div class="flex items-center justify-between">
              <n-tabs
                v-model:value="flowConfigurationType"
                type="segment"
                pane-class="hidden"
                tab-class="h-[28px]"
                @before-leave="handleFlowConfigurationTypeBeforeChange"
              >
                <n-tab-pane :name="CirculationTypeEnum.NORMAL" :tab="t('crmStatusConfigDrawer.basicFlow')">
                </n-tab-pane>
                <n-tab-pane :name="CirculationTypeEnum.ADVANCED" :tab="t('crmStatusConfigDrawer.advanceFlow')">
                </n-tab-pane>
              </n-tabs>
              <div v-if="flowConfigurationType === CirculationTypeEnum.ADVANCED" class="flex items-center gap-[8px]">
                <n-button secondary :disabled="!unsaved" @click="handleCancelFlowConfiguration">
                  {{ t('common.cancel') }}
                </n-button>
                <n-button type="primary" :disabled="!unsaved" @click="handleConfirmFlowConfiguration">
                  {{ t('common.save') }}
                </n-button>
              </div>
            </div>
            <template v-if="flowConfigurationType === CirculationTypeEnum.NORMAL">
              <div class="mb-[16px] mt-[24px]">
                {{ textConfig.rollbackTitle }}
              </div>
              <div
                v-for="item in textConfig.switches"
                :key="item.key"
                :class="[item.key === 'completedStageRollback' ? 'mt-[16px]' : '', 'flex items-center gap-[8px]']"
              >
                <n-switch v-model:value="form[item.key]" @update-value="handleSwitchChange" />
                {{ item.label }}
                <n-tooltip trigger="hover" placement="right">
                  <template #trigger>
                    <CrmIcon
                      type="iconicon_help_circle"
                      :size="16"
                      class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                    />
                  </template>
                  {{ item.tip }}
                </n-tooltip>
              </div>
            </template>
            <template v-else>
              <n-data-table
                :columns="columns"
                :data="form.advancedConfigs"
                :paging="false"
                :loading="loading"
                :single-line="false"
                class="mt-[16px]"
              >
              </n-data-table>
            </template>
          </div>
        </n-tab-pane>
      </n-tabs>
    </div>
  </CrmDrawer>
  <CrmModal v-model:show="flowSettingVisible" :mask-closable="false" @cancel="handleCancel" @confirm="handleConfirm">
    <template #title>
      <div class="flex items-center gap-[8px]">
        {{ t('crmStatusConfigDrawer.flowSetting') }}
        <div class="text-[var(--text-n4)]">
          {{ t('crmStatusConfigDrawer.flowPath', { f: activeRow?.name, t: activeCol?.name }) }}
        </div>
      </div>
    </template>
    <div class="mb-[16px]">{{ t('crmStatusConfigDrawer.flowSettingCondition') }}</div>
    <div class="flex w-full rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
      <div class="min-w-0 flex-1">
        <n-form ref="formRef" :model="tempForm">
          <div
            v-for="(item, listIndex) in tempForm.circulationFieldValues"
            :key="item.fieldId"
            class="flex items-start gap-[8px]"
          >
            <n-form-item
              :label="listIndex === 0 ? t('crmStatusConfigDrawer.field') : ''"
              :path="`circulationFieldValues[${listIndex}].fieldId`"
              :rule="[{ required: true, message: t('common.notNull', { value: t('crmStatusConfigDrawer.field') }) }]"
              class="block flex-1 overflow-hidden"
            >
              <n-select
                v-model:value="item.fieldId"
                filterable
                :placeholder="t('common.pleaseSelect')"
                :options="getFieldOptions(item.fieldId)"
                :fallback-option="() => fallbackOption(item.fieldId)"
                :loading="fieldLoading"
                :render-option="renderOption"
                @update-value="() => leftFieldChange(item)"
              />
            </n-form-item>
            <n-form-item
              :label="listIndex === 0 ? t('crmStatusConfigDrawer.defaultValueType') : ''"
              :path="`circulationFieldValues[${listIndex}].valueType`"
              class="block w-[105px]"
            >
              <n-select
                v-model:value="item.valueType"
                :options="[
                  {
                    label: t('crmStatusConfigDrawer.fieldValue'),
                    value: CirculationValueTypeEnum.FIELD_VALUE,
                  },
                  {
                    label: t('crmStatusConfigDrawer.fixedValue'),
                    value: CirculationValueTypeEnum.FIXED_VALUE,
                  },
                ]"
                :disabled="!item.fieldId"
              />
            </n-form-item>
            <n-form-item
              :label="listIndex === 0 ? t('crmFormDesign.defaultValue') : ''"
              :path="`circulationFieldValues[${listIndex}].fieldValue`"
              class="block flex-1 overflow-hidden"
            >
              <template v-if="item.fieldProps">
                <n-date-picker
                  v-if="[FieldTypeEnum.TIME_RANGE_PICKER, FieldTypeEnum.DATE_TIME].includes(item.fieldProps.type)"
                  v-model:value="item.fieldValue"
                  type="datetime"
                  clearable
                  :disabled="item.valueType === CirculationValueTypeEnum.FIELD_VALUE"
                  class="w-full"
                  :default-time="undefined"
                  :placeholder="
                    item.valueType === CirculationValueTypeEnum.FIELD_VALUE
                      ? t('crmStatusConfigDrawer.fieldDefaultValueTip')
                      : t('common.pleaseInput')
                  "
                />
                <CrmInputNumber
                  v-else-if="[FieldTypeEnum.INPUT_NUMBER, FieldTypeEnum.FORMULA].includes(item.fieldProps.type)"
                  v-model:value="item.fieldValue"
                  clearable
                  :disabled="item.valueType === CirculationValueTypeEnum.FIELD_VALUE"
                  :placeholder="
                    item.valueType === CirculationValueTypeEnum.FIELD_VALUE
                      ? t('crmStatusConfigDrawer.fieldDefaultValueTip')
                      : t('common.pleaseInput')
                  "
                  class="w-full"
                />
                <CrmTagInput
                  v-else-if="item.fieldProps.type === FieldTypeEnum.INPUT_MULTIPLE"
                  v-model:value="item.fieldValue"
                  clearable
                  :disabled="item.valueType === CirculationValueTypeEnum.FIELD_VALUE"
                  class="w-full"
                  :placeholder="
                    item.valueType === CirculationValueTypeEnum.FIELD_VALUE
                      ? t('crmStatusConfigDrawer.fieldDefaultValueTip')
                      : t('common.pleaseInput')
                  "
                />
                <CrmDataSource
                  v-else-if="
                    [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(item.fieldProps.type) &&
                    item.fieldProps.dataSourceType
                  "
                  v-model:value="item.fieldValue"
                  v-model:rows="item.fieldProps.initialOptions"
                  :disabled="item.valueType === CirculationValueTypeEnum.FIELD_VALUE"
                  :data-source-type="item.fieldProps.dataSourceType"
                  :multiple="item.fieldProps.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE"
                  :placeholder="
                    item.valueType === CirculationValueTypeEnum.FIELD_VALUE
                      ? t('crmStatusConfigDrawer.fieldDefaultValueTip')
                      : t('common.pleaseSelect')
                  "
                />
                <n-select
                  v-else-if="
                    [
                      FieldTypeEnum.SELECT,
                      FieldTypeEnum.SELECT_MULTIPLE,
                      FieldTypeEnum.RADIO,
                      FieldTypeEnum.CHECKBOX,
                    ].includes(item.fieldProps.type)
                  "
                  v-model:value="item.fieldValue"
                  clearable
                  max-tag-count="responsive"
                  :disabled="item.valueType === CirculationValueTypeEnum.FIELD_VALUE"
                  :placeholder="
                    item.valueType === CirculationValueTypeEnum.FIELD_VALUE
                      ? t('crmStatusConfigDrawer.fieldDefaultValueTip')
                      : t('common.pleaseSelect')
                  "
                  :multiple="[FieldTypeEnum.SELECT_MULTIPLE, FieldTypeEnum.CHECKBOX].includes(item.fieldProps.type)"
                  :options="item.fieldProps.options"
                />
                <CrmCitySelect
                  v-else-if="item.fieldProps.type === FieldTypeEnum.LOCATION"
                  v-model:value="item.fieldValue"
                  :placeholder="
                    item.valueType === CirculationValueTypeEnum.FIELD_VALUE
                      ? t('crmStatusConfigDrawer.fieldDefaultValueTip')
                      : t('common.pleaseSelect')
                  "
                  :disabled="item.valueType === CirculationValueTypeEnum.FIELD_VALUE"
                  clearable
                />
                <CrmIndustrySelect
                  v-else-if="item.fieldProps.type === FieldTypeEnum.INDUSTRY"
                  v-model:value="item.fieldValue"
                  :placeholder="
                    item.valueType === CirculationValueTypeEnum.FIELD_VALUE
                      ? t('crmStatusConfigDrawer.fieldDefaultValueTip')
                      : t('common.pleaseSelect')
                  "
                  :disabled="item.valueType === CirculationValueTypeEnum.FIELD_VALUE"
                  clearable
                />
                <CrmUserTagSelector
                  v-else-if="
                    [
                      FieldTypeEnum.DEPARTMENT,
                      FieldTypeEnum.DEPARTMENT_MULTIPLE,
                      FieldTypeEnum.MEMBER,
                      FieldTypeEnum.MEMBER_MULTIPLE,
                    ].includes(item.fieldProps.type)
                  "
                  v-model:value="item.fieldValue"
                  v-model:selected-list="item.fieldProps.initialOptions"
                  :multiple="
                    [FieldTypeEnum.DEPARTMENT_MULTIPLE, FieldTypeEnum.MEMBER_MULTIPLE].includes(item.fieldProps.type)
                  "
                  :disabled="item.valueType === CirculationValueTypeEnum.FIELD_VALUE"
                  :drawer-title="t('crmFormDesign.selectDataSource')"
                  :api-type-key="MemberApiTypeEnum.FORM_FIELD"
                  :member-types="
                    [FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(item.fieldProps.type)
                      ? [
                          {
                            label: t('menu.settings.org'),
                            value: MemberSelectTypeEnum.ORG,
                          },
                        ]
                      : [
                          {
                            label: t('menu.settings.org'),
                            value: MemberSelectTypeEnum.ONLY_ORG,
                          },
                        ]
                  "
                  :disabled-node-types="
                    [FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(item.fieldProps.type)
                      ? [DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE]
                      : [DeptNodeTypeEnum.USER, DeptNodeTypeEnum.ROLE]
                  "
                  :placeholder="
                    item.valueType === CirculationValueTypeEnum.FIELD_VALUE
                      ? t('crmStatusConfigDrawer.fieldDefaultValueTip')
                      : t('common.pleaseSelect')
                  "
                />
                <n-input
                  v-else
                  v-model:value="item.fieldValue"
                  allow-clear
                  :disabled="item.valueType === CirculationValueTypeEnum.FIELD_VALUE"
                  :maxlength="255"
                  :placeholder="
                    item.valueType === CirculationValueTypeEnum.FIELD_VALUE
                      ? t('crmStatusConfigDrawer.fieldDefaultValueTip')
                      : t('common.pleaseInput')
                  "
                />
              </template>
              <n-input
                v-else
                v-model:value="item.fieldValue"
                allow-clear
                :disabled="item.valueType === CirculationValueTypeEnum.FIELD_VALUE"
                :maxlength="255"
                :placeholder="
                  item.valueType === CirculationValueTypeEnum.FIELD_VALUE
                    ? t('crmStatusConfigDrawer.fieldDefaultValueTip')
                    : t('common.pleaseInput')
                "
              />
            </n-form-item>
            <n-form-item
              :label="listIndex === 0 ? t('common.required') : ''"
              :path="`conditions[${listIndex}].required`"
              :show-label="listIndex === 0"
              class="w-[30px]"
            >
              <n-checkbox v-model:checked="item.required" />
            </n-form-item>
            <n-form-item :show-label="false" :class="listIndex === 0 ? 'self-center' : 'self-start'">
              <n-button ghost class="px-[8px]" @click="handleDeleteItem(listIndex)">
                <template #icon>
                  <CrmIcon type="iconicon_minus_circle1" :size="16" />
                </template>
              </n-button>
            </n-form-item>
          </div>
        </n-form>
        <n-button type="primary" text class="w-[fit-content]" @click="handleAddItem">
          <template #icon>
            <n-icon><Add /></n-icon>
          </template>
          {{ t('crmStatusConfigDrawer.addField') }}
        </n-button>
      </div>
    </div>
  </CrmModal>
</template>

<script setup lang="ts">
  import {
    FormInst,
    NButton,
    NCheckbox,
    NDataTable,
    NDatePicker,
    NForm,
    NFormItem,
    NIcon,
    NInput,
    NSelect,
    NSwitch,
    NTabPane,
    NTabs,
    NTooltip,
    type SelectOption,
    useMessage,
  } from 'naive-ui';
  import { Add } from '@vicons/ionicons5';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { CirculationTypeEnum, CirculationValueTypeEnum } from '@lib/shared/enums/opportunityEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { scrollIntoView } from '@lib/shared/method/dom';
  import { initFieldValue } from '@lib/shared/method/formCreate';
  import type { CirculationFieldValueItem, CirculationSetting } from '@lib/shared/models/opportunity';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmIndustrySelect from '@/components/pure/crm-industry-select/index.vue';
  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmTagInput from '@/components/pure/crm-tag-input/index.vue';
  import CrmBatchForm from '@/components/business/crm-batch-form/index.vue';
  import CrmCitySelect from '@/components/business/crm-city-select/index.vue';
  import CrmDataSource from '@/components/business/crm-data-source-select/index.vue';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useModal from '@/hooks/useModal';

  import { flowApiMap } from './config';
  import type { StatusBizType, StatusRowItem } from './types';
  import useStageConfig from './useStageConfig';
  import type { TableColumn } from 'naive-ui/es/data-table/src/interface';
  import type { VNodeChild } from 'vue';

  const props = defineProps<{
    type: StatusBizType;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const show = defineModel<boolean>('visible', {
    required: true,
  });

  const batchFormRef = ref<InstanceType<typeof CrmBatchForm>>();
  const unsaved = ref(false);

  const {
    textConfig,
    formItemModel,
    form,
    init,
    handleSave,
    handleCancelRow,
    handleSwitchChange,
    handleMoreSelect,
    dragEnd,
    handleMove,
    getDropdownOptions,
  } = useStageConfig(computed(() => props.type));

  const { fieldList, initFormConfig } = useFormCreateApi({
    formKey: computed(() => props.type),
  });

  async function handleActionSelect(action: ActionsItem, element: StatusRowItem) {
    await handleMoreSelect(action, element, batchFormRef.value);
  }

  const tabName = ref('statusConfig');
  const drawerLoading = ref(false);
  const flowConfigurationType = ref<CirculationTypeEnum>(CirculationTypeEnum.NORMAL);
  const formRef = ref<FormInst | null>(null);
  const tempForm = ref({
    circulationFieldValues: [] as CirculationFieldValueItem[],
  });

  function handleMaskClick() {
    if (unsaved.value) {
      openModal({
        type: 'warning',
        title: t('common.tip'),
        content: t('common.editUnsavedLeave'),
        positiveText: t('common.confirm'),
        negativeText: t('common.cancel'),
        onPositiveClick: () => {
          show.value = false;
        },
      });
    } else {
      show.value = false;
    }
  }

  function initAdvanceConfig() {
    flowConfigurationType.value = form.value.circulationType;
    if (!form.value.advancedConfigs?.length) {
      form.value.advancedConfigs = form.value.list.map((e) => ({
        ...e,
        originId: e.id!,
        targets: [
          {
            targetId: e.id!,
            circulationFieldValues: [],
            enable: true,
          },
        ],
        moduleType: '',
      }));
    } else {
      form.value.advancedConfigs = form.value.advancedConfigs.map((e) => {
        const stage = form.value.list.find((s) => s.id === e.originId);
        return {
          ...e,
          name: stage?.name,
          type: stage?.type,
        };
      });
    }
  }

  watch(
    () => show.value,
    async (val) => {
      if (val) {
        drawerLoading.value = true;
        await init();
        drawerLoading.value = false;
        initAdvanceConfig();
        unsaved.value = false;
      } else {
        tempForm.value = {
          circulationFieldValues: [] as CirculationFieldValueItem[],
        };
        tabName.value = 'statusConfig';
      }
    }
  );

  watch(
    () => form.value.list,
    (arr) => {
      form.value.advancedConfigs = arr.map((e) => ({
        ...e,
        originId: e.id!,
        targets: form.value.advancedConfigs.find((ac) => ac.originId === e.id)?.targets || [
          {
            targetId: e.id!,
            circulationFieldValues: [],
            enable: true,
          },
        ],
        moduleType: '',
      }));
    },
    { deep: true }
  );

  const loading = ref(false);

  function getStatusColor(status: 'AFOOT' | 'END' | string, index: number) {
    if (status === 'AFOOT' && index === 0) {
      return 'default';
    }
    if (status === 'AFOOT') {
      return 'info';
    }
    return 'success';
  }

  function handleCancelFlowConfiguration() {
    show.value = false;
  }

  async function handleConfirmFlowConfiguration() {
    try {
      drawerLoading.value = true;
      await flowApiMap[props.type].save({
        circulationType: flowConfigurationType.value,
        circulationSettings: form.value.advancedConfigs.map((e) => ({
          ...e,
          targets: e.targets.map((target) => ({
            targetId: target.targetId,
            enable: target.enable,
            circulationFieldValues: target.circulationFieldValues.map((tc) => ({
              fieldId: tc.fieldId,
              fieldValue:
                tc.fieldProps &&
                [
                  FieldTypeEnum.SELECT,
                  FieldTypeEnum.DEPARTMENT,
                  FieldTypeEnum.DATA_SOURCE,
                  FieldTypeEnum.MEMBER,
                ].includes(tc.fieldProps.type) &&
                Array.isArray(tc.fieldValue)
                  ? tc.fieldValue[0] || ''
                  : tc.fieldValue,
              required: tc.required,
              valueType: tc.valueType,
            })),
          })),
        })),
      });
      Message.success(t('common.saveSuccess'));
      unsaved.value = false;
      show.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      drawerLoading.value = false;
    }
  }

  async function handleFlowTypeChange(val: any) {
    try {
      await flowApiMap[props.type].switch(val);
      flowConfigurationType.value = val;
      Message.success(t('common.operationSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleFlowConfigurationTypeBeforeChange(val: any) {
    openModal({
      title: t('common.tip'),
      type: 'warning',
      content: t('crmStatusConfigDrawer.typeChangeTip'),
      negativeText: t('common.cancel'),
      positiveText: t('common.confirm'),
      onPositiveClick: async () => {
        handleFlowTypeChange(val);
      },
    });
    return false;
  }

  const flowSettingVisible = ref(false);
  const activeRow = ref<CirculationSetting & StatusRowItem>();
  const activeCol = ref<StatusRowItem>();
  const fieldLoading = ref(false);

  async function openFlowSetting(row: CirculationSetting & StatusRowItem, col: StatusRowItem) {
    try {
      fieldLoading.value = true;
      activeRow.value = row;
      activeCol.value = col;
      flowSettingVisible.value = true;
      await initFormConfig();
      // 初始化未初始化过的字段属性
      const currentFlow = row.targets.find((e) => e.targetId === col.id);
      if (currentFlow) {
        currentFlow.circulationFieldValues = currentFlow.circulationFieldValues.map((v) => {
          const field = fieldList.value.find((f) => f.id === v.fieldId);
          if (field) {
            const options = form.value.optionMap?.[field.id]?.map((e: Record<string, any>) => ({
              id: e.id,
              name: e.name || t('common.optionNotExist'),
            }));
            if (options && options.length > 0) {
              field.initialOptions = (v.fieldProps?.initialOptions || []).concat(
                options
                  ?.filter((e) => v.fieldValue?.includes(e.id))
                  .map((e) => ({
                    ...e,
                    name: e.name || t('common.optionNotExist'),
                  }))
              );
            } else {
              field.initialOptions = v.fieldProps?.initialOptions || field.initialOptions;
            }
            return {
              ...v,
              fieldValue: initFieldValue(field, v.fieldValue),
              fieldProps: field,
            };
          }
          return v;
        });
        tempForm.value = {
          circulationFieldValues: cloneDeep(currentFlow.circulationFieldValues),
        };
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      fieldLoading.value = false;
    }
  }

  function handleCancel() {
    flowSettingVisible.value = false;
  }

  function validateForm(cb: (res?: Record<string, any>) => void) {
    formRef.value?.validate(async (errors) => {
      if (errors) {
        scrollIntoView(document.querySelector('.n-form-item-blank--error'), { block: 'center' });
        return;
      }
      if (typeof cb === 'function') {
        cb();
      }
    });
  }

  function handleConfirm() {
    validateForm(() => {
      const currentFlow = activeRow.value?.targets.find((e) => e.targetId === activeCol.value?.id);
      if (currentFlow) {
        currentFlow.circulationFieldValues = cloneDeep(tempForm.value.circulationFieldValues);
      }
      unsaved.value = true;
      flowSettingVisible.value = false;
    });
  }

  const columns = computed<TableColumn<CirculationSetting & StatusRowItem>[]>(() => {
    const cols: TableColumn<CirculationSetting & StatusRowItem>[] = [
      {
        key: 'staticTitle',
        title: () =>
          h(
            'div',
            { class: 'w-[120px] relative h-[46px]' },
            {
              default: () => [
                h(
                  'div',
                  { class: 'startStatus font-normal' },
                  { default: () => t('crmStatusConfigDrawer.sourceStatus') }
                ),
                h('div', { class: 'line' }),
                h(
                  'div',
                  { class: 'endStatus font-normal' },
                  { default: () => t('crmStatusConfigDrawer.targetStatus') }
                ),
              ],
            }
          ),
        width: 120,
        render: (row, rowIndex) =>
          h(
            CrmTag,
            { theme: 'light', type: getStatusColor(row.type as string, rowIndex), size: 'large' },
            { default: () => row.name }
          ),
      },
    ];
    form.value.list.forEach((e, i) => {
      cols.push({
        key: e.id!,
        title: () =>
          h(
            CrmTag,
            { theme: 'light', type: getStatusColor(e.type, i), size: 'large', class: 'font-normal' },
            { default: () => e.name }
          ),
        width: 120,
        render: (row, rowIndex) =>
          h(
            'div',
            { class: 'crm-status-cell' },
            {
              default: () => [
                h(NCheckbox, {
                  defaultChecked: rowIndex === i || row.targets.find((r) => r.targetId === e.id)?.enable,
                  disabled: rowIndex === i,
                  onUpdateChecked: (val) => {
                    const currentCell = row.targets.find((r) => r.targetId === e.id);
                    if (currentCell) {
                      currentCell.enable = val;
                    } else {
                      row.targets.push({
                        targetId: e.id!,
                        enable: true,
                        circulationFieldValues: [],
                      });
                    }
                    unsaved.value = true;
                  },
                }),
                rowIndex === i
                  ? undefined
                  : h(CrmIcon, {
                      type: 'iconicon_set_up',
                      class: row.targets.find((r) => r.targetId === e.id)?.enable ? 'setting-icon' : 'hidden',
                      onClick: () => openFlowSetting(row, e),
                    }),
              ],
            }
          ),
      });
    });
    return cols;
  });

  const fieldOptions = computed(() =>
    fieldList.value
      .filter(
        (e) =>
          (![
            FieldTypeEnum.DIVIDER,
            FieldTypeEnum.SERIAL_NUMBER,
            FieldTypeEnum.SUB_PRICE,
            FieldTypeEnum.SUB_PRODUCT,
            FieldTypeEnum.ATTACHMENT,
            FieldTypeEnum.FORMULA,
            FieldTypeEnum.PICTURE,
            FieldTypeEnum.INPUT,
          ].includes(e.type) &&
            !e.resourceFieldId &&
            e.editable) ||
          (e.type === FieldTypeEnum.INPUT && e.defaultValueType === 'custom')
      )
      .map((e) => ({
        label: e.name,
        value: e.id,
      }))
  );

  function getFieldOptions(currentFieldId?: string) {
    const alreadySelectedFields = tempForm.value.circulationFieldValues.map((e) => e.fieldId);
    return fieldOptions.value.filter((e) => e.value === currentFieldId || !alreadySelectedFields.includes(e.value));
  }

  function fallbackOption(val?: string | number) {
    return {
      label: t('common.optionNotExist'),
      value: val,
    };
  }

  function renderOption({ node, option }: { node: VNode; option: SelectOption }): VNodeChild {
    return h(
      NTooltip,
      {
        delay: 300,
        placement: 'left',
      },
      {
        trigger: () => node,
        default: () => option.label,
      }
    );
  }

  function leftFieldChange(item: CirculationFieldValueItem) {
    nextTick(() => {
      const field = fieldList.value.find((e) => e.id === item.fieldId);
      if (field) {
        item.valueType = CirculationValueTypeEnum.FIELD_VALUE;
        item.required = false;
        item.fieldValue = initFieldValue(field, undefined);
        item.fieldProps = field;
      }
    });
  }

  function handleDeleteItem(index: number) {
    tempForm.value.circulationFieldValues.splice(index, 1);
  }

  function handleAddItem() {
    validateForm(() => {
      const item: CirculationFieldValueItem = {
        fieldId: undefined,
        fieldProps: undefined,
        fieldValue: undefined,
        valueType: CirculationValueTypeEnum.FIELD_VALUE,
        required: false,
      };
      tempForm.value.circulationFieldValues.push(item);
    });
  }
</script>

<style lang="less" scoped>
  :deep(.v-x-scroll) {
    padding: 0 16px;
    background-color: var(--text-n10);
  }
  :deep(.startStatus) {
    position: absolute;
    bottom: 0;
    left: -6px;
  }
  :deep(.endStatus) {
    position: absolute;
    top: 0;
    right: 0;
  }
  :deep(.line) {
    position: absolute;
    top: 22px;
    left: -22px;
    width: 130%;
    height: 1px;
    background: var(--text-n8);
    transform: rotateZ(17deg);
  }
  :deep(.crm-status-cell) {
    @apply flex items-center;

    gap: 8px;
    .setting-icon {
      color: var(--text-n4);
      cursor: pointer;
    }
  }
</style>
