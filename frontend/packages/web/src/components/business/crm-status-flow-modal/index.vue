<template>
  <CrmModal
    v-model:show="show"
    :title="t('crmStatusFlow.flowPath', { f: props.from.name, t: props.to.name })"
    :positive-text="t('crmStatusFlow.flow')"
    :ok-loading="okLoading"
    @cancel="handleCancel"
    @confirm="handleConfirm"
  >
    <n-spin :show="loading" class="block" :class="loading ? 'min-h-[150px]' : ''">
      <n-form ref="formRef" :model="formDetail" label-placement="top" label-width="auto" class="crm-form-create">
        <n-scrollbar>
          <div class="crm-form-create">
            <template v-for="item in realFields" :key="item.id">
              <div v-if="item.show !== false && item.readable" class="crm-form-create-item">
                <component
                  :is="getItemComponent(item)"
                  :id="item.id"
                  v-model:value="formDetail[item.id]"
                  :field-config="{
                    ...item,
                    show: true,
                    showLabel: true,
                  }"
                  :form-detail="formDetail"
                  :origin-form-detail="originFormDetail"
                  :path="item.id"
                  :form-config="{
                    ...formConfig,
                    layout: 1,
                    labelPos: 'top',
                  }"
                  needInitDetail
                />
              </div>
            </template>
            <div
              v-if="props.formKey === FormDesignKeyEnum.CONTRACT && props.to.id === 'VOID'"
              class="crm-form-create-item"
            >
              <n-form-item path="reason" :label="t('contract.voidReason')">
                <n-input
                  v-model:value="formDetail.reason"
                  type="textarea"
                  :placeholder="t('common.pleaseInput')"
                  allow-clear
                  maxlength="200"
                  show-count
                />
              </n-form-item>
            </div>
          </div>
        </n-scrollbar>
      </n-form>
    </n-spin>
  </CrmModal>
</template>

<script setup lang="ts">
  import { FormInst, NForm, NFormItem, NInput, NScrollbar, NSpin, useMessage } from 'naive-ui';

  import { FieldRuleEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { CirculationValueTypeEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getNormalFieldValue, getRuleType, initFieldValue, transformFieldValue } from '@lib/shared/method/formCreate';
  import type {
    CirculationFieldValueItem,
    OpportunityStageConfig,
    UpdateStageParams,
  } from '@lib/shared/models/opportunity';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmFormCreateComponents from '@/components/business/crm-form-create/components';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';

  import { changeContractStatus, updateOrderStage } from '@/api/modules';
  import useFormCreateApi from '@/hooks/useFormCreateApi';

  import cloneDeep from 'lodash-es/cloneDeep';

  const props = defineProps<{
    from: { id?: string; name?: string };
    to: { id?: string; name?: string };
    formKey: FormDesignKeyEnum;
    circulationFieldValues: CirculationFieldValueItem[];
    sourceId: string;
    stageConfig?: OpportunityStageConfig; // 阶段配置
  }>();
  const emit = defineEmits<{
    (e: 'success'): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const formRef = ref<FormInst>();
  const { formKey, sourceId } = toRefs(props);

  const {
    fieldList,
    formConfig,
    formDetail,
    originFormDetail,
    loading,
    initFormConfig,
    initFormDetail,
    initForm,
    resetForm,
  } = useFormCreateApi({
    formKey,
    sourceId,
  });

  const realFields = ref<FormCreateField[]>([]);
  function initRealFields() {
    realFields.value = props.circulationFieldValues
      .map((cf) => {
        const field = fieldList.value.find((f) => f.id === cf.fieldId);
        if (field) {
          let rules = field.rules || [];
          if (cf.required && !field.rules.some((e) => e.required)) {
            rules.push({
              key: FieldRuleEnum.REQUIRED,
              required: true,
              message: t('common.notNull', { value: field.name }),
              label: t('common.required'),
              trigger: ['change', 'blur'],
              type: getRuleType(field),
            });
          } else if (!cf.required && field.rules.some((e) => e.key === 'required')) {
            rules = rules.filter((e) => e.key !== 'required');
          }
          if (cf.valueType === CirculationValueTypeEnum.FIXED_VALUE) {
            formDetail.value[field.id] = initFieldValue(field, cf.fieldValue);
            const options = props.stageConfig?.optionMap?.[field.id]?.map((e: Record<string, any>) => ({
              id: e.id,
              name: e.name || t('common.optionNotExist'),
            }));
            if (options && options.length > 0) {
              field.initialOptions = options
                ?.filter((e) => cf.fieldValue?.includes(e.id))
                .map((e) => ({
                  ...e,
                  name: e.name || t('common.optionNotExist'),
                }));
            } else {
              field.initialOptions = [];
            }
          }
          return {
            ...field,
            defaultValue:
              cf.valueType === CirculationValueTypeEnum.FIXED_VALUE
                ? initFieldValue(field, cf.fieldValue)
                : formDetail.value[field.id],
            fieldWidth: 1,
            rules,
          };
        }
        return null;
      })
      .filter((e) => e !== null) as FormCreateField[];
  }

  watch(
    () => show.value,
    async (val) => {
      if (val) {
        loading.value = true;
        await initFormConfig();
        await initFormDetail();
        initRealFields();
        initForm();
        if (props.formKey === FormDesignKeyEnum.CONTRACT && props.to.id === 'VOID') {
          formDetail.value.voidReason = '';
        }
        loading.value = false;
      } else {
        realFields.value = [];
        resetForm();
      }
    }
  );

  function getItemComponent(item: FormCreateField) {
    if (item.type === FieldTypeEnum.INPUT || item.resourceFieldId) {
      return CrmFormCreateComponents.basicComponents.singleText;
    }
    if (item.type === FieldTypeEnum.TEXTAREA) {
      return CrmFormCreateComponents.basicComponents.textarea;
    }
    if (item.type === FieldTypeEnum.INPUT_NUMBER) {
      return CrmFormCreateComponents.basicComponents.inputNumber;
    }
    if (item.type === FieldTypeEnum.DATE_TIME) {
      return CrmFormCreateComponents.basicComponents.dateTime;
    }
    if (item.type === FieldTypeEnum.RADIO) {
      return CrmFormCreateComponents.basicComponents.radio;
    }
    if (item.type === FieldTypeEnum.CHECKBOX) {
      return CrmFormCreateComponents.basicComponents.checkbox;
    }
    if ([FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(item.type)) {
      return CrmFormCreateComponents.basicComponents.select;
    }
    if ([FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(item.type)) {
      return CrmFormCreateComponents.basicComponents.memberSelect;
    }
    if ([FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(item.type)) {
      return CrmFormCreateComponents.basicComponents.memberSelect;
    }
    if (item.type === FieldTypeEnum.INPUT_MULTIPLE) {
      return CrmFormCreateComponents.basicComponents.tagInput;
    }
    if (item.type === FieldTypeEnum.PICTURE) {
      return CrmFormCreateComponents.advancedComponents.upload;
    }
    if (item.type === FieldTypeEnum.LOCATION) {
      return CrmFormCreateComponents.advancedComponents.location;
    }
    if (item.type === FieldTypeEnum.PHONE) {
      return CrmFormCreateComponents.advancedComponents.phone;
    }
    if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(item.type)) {
      return CrmFormCreateComponents.advancedComponents.dataSource;
    }
    if (item.type === FieldTypeEnum.LINK) {
      return CrmFormCreateComponents.advancedComponents.link;
    }
    if (item.type === FieldTypeEnum.ATTACHMENT) {
      return CrmFormCreateComponents.advancedComponents.file;
    }
    if (item.type === FieldTypeEnum.INDUSTRY) {
      return CrmFormCreateComponents.advancedComponents.industry;
    }
  }

  function handleCancel() {
    show.value = false;
  }

  const updateApiMap: Partial<Record<FormDesignKeyEnum, (data: UpdateStageParams) => Promise<any>>> = {
    [FormDesignKeyEnum.CONTRACT]: changeContractStatus,
    [FormDesignKeyEnum.ORDER]: updateOrderStage,
  };

  const okLoading = ref(false);
  function handleConfirm() {
    formRef.value?.validate(async (errors) => {
      if (!errors) {
        try {
          okLoading.value = true;
          const result = cloneDeep(formDetail.value);
          await updateApiMap[props.formKey]?.({
            id: props.sourceId,
            stage: props.to.id || '',
            fields: realFields.value.map((e) => {
              transformFieldValue(e, result, e.id);
              return {
                fieldId: e.id,
                fieldValue: getNormalFieldValue(e, result[e.id]),
              };
            }),
            voidReason: formDetail.value.voidReason,
          });
          Message.success(t('crmStatusFlow.flowSuccess'));
          emit('success');
          show.value = false;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        } finally {
          okLoading.value = false;
        }
      }
    });
  }
</script>

<style lang="less" scoped>
  .crm-form-create-item {
    @apply w-full;

    margin-bottom: 16px;
  }
</style>
