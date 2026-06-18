<template>
  <CrmModal
    v-model:show="show"
    :title="t('crmStatusFlow.flowPath', { f: props.from.name, t: props.to.name })"
    :positive-text="t('crmStatusFlow.flow')"
    @cancel="handleCancel"
    @confirm="handleConfirm"
  >
    <n-spin :show="loading" class="block">
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
                />
              </div>
            </template>
          </div>
        </n-scrollbar>
      </n-form>
    </n-spin>
  </CrmModal>
</template>

<script setup lang="ts">
  import { FormInst, NForm, NScrollbar, NSpin } from 'naive-ui';

  import { FieldRuleEnum, FieldTypeEnum, type FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { CirculationValueTypeEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getRuleType } from '@lib/shared/method/formCreate';
  import type { CirculationFieldValueItem } from '@lib/shared/models/opportunity';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmFormCreateComponents from '@/components/business/crm-form-create/components';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';

  import useFormCreateApi from '@/hooks/useFormCreateApi';

  const props = defineProps<{
    from: { id?: string; name?: string };
    to: { id?: string; name?: string };
    formKey: FormDesignKeyEnum;
    circulationFieldValues: CirculationFieldValueItem[];
  }>();

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const formRef = ref<FormInst>();
  const { formKey } = toRefs(props);

  const { fieldList, formConfig, formDetail, originFormDetail, loading, initFormConfig, initForm } = useFormCreateApi({
    formKey,
  });

  const realFields = ref<FormCreateField[]>([]);
  function initRealFields() {
    realFields.value = props.circulationFieldValues
      .map((cf) => {
        const field = fieldList.value.find((f) => f.id === cf.fieldId);
        if (field) {
          return {
            ...field,
            defaultValue: cf.valueType === CirculationValueTypeEnum.FIXED_VALUE ? cf.fieldValue : field.defaultValue,
            fieldWidth: 1,
            rules:
              cf.required && field.rules.some((e) => e.required)
                ? field.rules
                : {
                    key: FieldRuleEnum.REQUIRED,
                    required: true,
                    message: 'common.notNull',
                    label: 'common.required',
                    trigger: ['change', 'blur'],
                    type: getRuleType(field),
                  },
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
        await initFormConfig();
        initForm();
        initRealFields();
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

  function handleConfirm() {
    formRef.value?.validate(async (errors) => {
      if (!errors) {
        try {
          //
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      }
    });
  }
</script>

<style lang="less" scoped></style>
