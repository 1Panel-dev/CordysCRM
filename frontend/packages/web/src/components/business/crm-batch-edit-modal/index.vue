<template>
  <CrmModal
    v-model:show="visible"
    :title="t('common.batchEdit')"
    siz="small"
    :positive-text="t('common.update')"
    :negative-text="t('common.cancel')"
    :ok-loading="loading"
    @confirm="handleSave"
    @cancel="handleCancel"
  >
    <n-scrollbar class="max-h-[60vh] pr-[8px]">
      <n-form
        ref="formRef"
        require-mark-placement="left"
        :model="form"
        class="crm-batch-edit-form"
        label-placement="left"
        label-align="right"
        :label-width="100"
      >
        <n-form-item
          path="fieldId"
          :label="t('common.updateField')"
          :rule="[{ required: true, message: t('common.notNull', { value: `${t('org.attributes')}` }) }]"
        >
          <n-select
            v-model:value="form.fieldId"
            :options="fieldOptions"
            :placeholder="t('common.pleaseSelect')"
            label-field="name"
            filterable
            value-field="id"
            clearable
          />
        </n-form-item>
        <Select
          v-if="form.fieldId && [FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(currentForm.type)"
          v-model:value="form.fieldValue"
          path="fieldValue"
          :field-config="currentForm"
          :form-detail="form"
        />
        <InputNumber
          v-else-if="form.fieldId && [FieldTypeEnum.INPUT_NUMBER].includes(currentForm.type)"
          v-model:value="form.fieldValue"
          path="fieldValue"
          :field-config="currentForm"
          :form-detail="form"
        />
        <MemberSelect
          v-else-if="
            form.fieldId &&
            [
              FieldTypeEnum.MEMBER,
              FieldTypeEnum.MEMBER_MULTIPLE,
              FieldTypeEnum.DEPARTMENT,
              FieldTypeEnum.DEPARTMENT_MULTIPLE,
            ].includes(currentForm.type)
          "
          v-model:value="form.fieldValue"
          path="fieldValue"
          :form-detail="form"
          :field-config="currentForm"
        />
        <Radio
          v-else-if="form.fieldId && [FieldTypeEnum.RADIO].includes(currentForm.type)"
          v-model:value="form.fieldValue"
          path="fieldValue"
          class="crm-batch-edit-form-item"
          :form-detail="form"
          :field-config="currentForm"
        />
        <Checkbox
          v-else-if="form.fieldId && [FieldTypeEnum.CHECKBOX].includes(currentForm.type)"
          v-model:value="form.fieldValue"
          path="fieldValue"
          class="crm-batch-edit-form-item"
          :field-config="currentForm"
          :form-detail="form"
        />
        <Textarea
          v-else-if="form.fieldId && [FieldTypeEnum.TEXTAREA].includes(currentForm.type)"
          v-model:value="form.fieldValue"
          path="fieldValue"
          :field-config="currentForm"
          :form-detail="form"
        />
        <DateTime
          v-else-if="form.fieldId && [FieldTypeEnum.DATE_TIME].includes(currentForm.type)"
          v-model:value="form.fieldValue"
          path="fieldValue"
          :field-config="currentForm"
          :form-detail="form"
        />
        <SingleText
          v-else-if="form.fieldId && [FieldTypeEnum.INPUT].includes(currentForm.type)"
          v-model:value="form.fieldValue"
          path="fieldValue"
          :field-config="currentForm"
          :form-detail="form"
        />
        <TagInput
          v-else-if="form.fieldId && [FieldTypeEnum.INPUT_MULTIPLE].includes(currentForm.type)"
          v-model:value="form.fieldValue"
          path="fieldValue"
          :field-config="currentForm"
          :form-detail="form"
        />

        <dataSource
          v-else-if="
            form.fieldId && [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(currentForm.type)
          "
          v-model:value="form.fieldValue"
          path="fieldValue"
          :field-config="currentForm"
          :form-detail="form"
        />
        <Location
          v-else-if="form.fieldId && [FieldTypeEnum.LOCATION].includes(currentForm.type)"
          v-model:value="form.fieldValue"
          path="fieldValue"
          :field-config="currentForm"
          :form-detail="form"
        />
        <Phone
          v-else-if="form.fieldId && [FieldTypeEnum.PHONE].includes(currentForm.type)"
          :id="currentForm.id"
          v-model:value="form.fieldValue"
          path="fieldValue"
          :field-config="currentForm"
          :form-detail="form"
        />
        <n-form-item v-else path="fieldValue" :label="t('common.batchUpdate')">
          <n-input disabled type="text" :placeholder="t('common.pleaseInput')" :maxlength="255" />
        </n-form-item>
      </n-form>
    </n-scrollbar>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, NForm, NFormItem, NInput, NScrollbar, NSelect, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getNormalFieldValue, getRuleType } from '@lib/shared/method/formCreate';
  import { BatchUpdatePoolAccountParams } from '@lib/shared/models/customer';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  // 高级组件
  import dataSource from '@/components/business/crm-form-create/components/advanced/dataSource.vue';
  import Location from '@/components/business/crm-form-create/components/advanced/location.vue';
  import Phone from '@/components/business/crm-form-create/components/advanced/phone.vue';
  // 基础组件
  import Checkbox from '@/components/business/crm-form-create/components/basic/checkbox.vue';
  import DateTime from '@/components/business/crm-form-create/components/basic/dateTime.vue';
  import InputNumber from '@/components/business/crm-form-create/components/basic/inputNumber.vue';
  import MemberSelect from '@/components/business/crm-form-create/components/basic/memberSelect.vue';
  import Radio from '@/components/business/crm-form-create/components/basic/radio.vue';
  import Select from '@/components/business/crm-form-create/components/basic/select.vue';
  import SingleText from '@/components/business/crm-form-create/components/basic/singleText.vue';
  import TagInput from '@/components/business/crm-form-create/components/basic/tagInput.vue';
  import Textarea from '@/components/business/crm-form-create/components/basic/textarea.vue';
  import { FormCreateField, FormCreateFieldRule } from '@/components/business/crm-form-create/types';

  import {
    batchUpdateAccount,
    batchUpdateCluePool,
    batchUpdateContact,
    batchUpdateLead,
    batchUpdateOpenSeaCustomer,
    batchUpdateOpportunity,
    batchUpdateProduct,
    batchUpdateProductPrice,
  } from '@/api/modules';
  import { useUserStore } from '@/store';

  import { rules } from '../crm-form-create/config';
  import { SelectMixedOption } from 'naive-ui/es/select/src/interface';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    ids: (number | string)[];
    formKey:
      | FormDesignKeyEnum.CLUE_POOL
      | FormDesignKeyEnum.CUSTOMER_OPEN_SEA
      | FormDesignKeyEnum.BUSINESS
      | FormDesignKeyEnum.CLUE
      | FormDesignKeyEnum.CUSTOMER
      | FormDesignKeyEnum.PRODUCT
      | FormDesignKeyEnum.CUSTOMER_CONTACT
      | FormDesignKeyEnum.PRICE;
  }>();

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const visible = defineModel<boolean>('visible', { required: true });
  const userStore = useUserStore();

  const list = defineModel<FormCreateField[]>('fieldList', {
    required: true,
  });

  const saveApiMap: Record<string, (params: BatchUpdatePoolAccountParams) => Promise<any>> = {
    [FormDesignKeyEnum.CLUE_POOL]: batchUpdateCluePool,
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: batchUpdateOpenSeaCustomer,
    [FormDesignKeyEnum.BUSINESS]: batchUpdateOpportunity,
    [FormDesignKeyEnum.CLUE]: batchUpdateLead,
    [FormDesignKeyEnum.PRODUCT]: batchUpdateProduct,
    [FormDesignKeyEnum.CUSTOMER]: batchUpdateAccount,
    [FormDesignKeyEnum.CUSTOMER_CONTACT]: batchUpdateContact,
    [FormDesignKeyEnum.PRICE]: batchUpdateProductPrice,
  };

  const initForm = {
    fieldId: null,
    fieldValue: undefined,
  };

  const form = ref<{
    fieldId: string | null;
    fieldValue: any;
  }>({
    ...initForm,
  });

  const fieldOptions = computed(
    () =>
      list.value.filter((e) => {
        const baseCondition = ![
          FieldTypeEnum.DIVIDER,
          FieldTypeEnum.PICTURE,
          FieldTypeEnum.LINK,
          FieldTypeEnum.SERIAL_NUMBER,
          FieldTypeEnum.ATTACHMENT,
          FieldTypeEnum.SUB_PRICE,
          FieldTypeEnum.SUB_PRODUCT,
        ].includes(e.type);

        if (props.formKey === FormDesignKeyEnum.CLUE_POOL || props.formKey === FormDesignKeyEnum.CUSTOMER_OPEN_SEA) {
          return baseCondition && e.businessKey !== 'owner';
        }
        return baseCondition;
      }) as unknown as SelectMixedOption[]
  );

  const formRef = ref<FormInst>();
  const currentForm = ref();
  watch(
    () => form.value.fieldId,
    (val) => {
      if (val) {
        formRef.value?.restoreValidation();
        const currentFormVal = list.value.find((e) => e.id === val);

        if (currentFormVal) {
          let defaultValue = currentFormVal.defaultValue || '';
          if ([FieldTypeEnum.DATE_TIME, FieldTypeEnum.INPUT_NUMBER].includes(currentFormVal.type)) {
            defaultValue = Number.isNaN(Number(defaultValue)) || defaultValue === '' ? null : Number(defaultValue);
          } else if (getRuleType(currentFormVal) === 'array') {
            defaultValue =
              [FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.MEMBER].includes(
                currentFormVal.type
              ) && typeof currentFormVal.defaultValue === 'string'
                ? [defaultValue]
                : defaultValue || [];
          }

          form.value.fieldValue = defaultValue;
          currentForm.value = {
            ...currentFormVal,
            name: t('common.batchUpdate'),
            description: '',
          };

          const fullRules: FormCreateFieldRule[] = [];
          if (currentFormVal?.rules) {
            (currentFormVal.rules || []).forEach((rule) => {
              const staticRule = cloneDeep(rules.find((e) => e.key === rule.key));

              if (staticRule) {
                staticRule.message = t(staticRule.message as string, { value: t(currentFormVal.name) });
                staticRule.type = getRuleType(currentFormVal);
                staticRule.regex = rule.regex;

                if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(currentFormVal.type)) {
                  staticRule.trigger = 'none';
                }
                fullRules.push(staticRule);
              }
            });
            currentForm.value.rules = fullRules;
          }

          if (
            [FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(currentFormVal.type) &&
            currentFormVal.hasCurrentUser
          ) {
            currentForm.value.defaultValue = userStore.userInfo.id;
            currentForm.value.initialOptions = [
              ...(currentFormVal.initialOptions || []),
              {
                id: userStore.userInfo.id,
                name: userStore.userInfo.name,
              },
            ].filter((option, index, self) => self.findIndex((o) => o.id === option.id) === index);
          } else if (
            [FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(currentFormVal.type) &&
            currentFormVal.hasCurrentUserDept
          ) {
            currentForm.value.defaultValue = userStore.userInfo.departmentId;
            currentForm.value.initialOptions = [
              ...(currentFormVal.initialOptions || []),
              {
                id: userStore.userInfo.departmentId,
                name: userStore.userInfo.departmentName,
              },
            ].filter((option, index, self) => self.findIndex((o) => o.id === option.id) === index);
          }
        }
      }
    }
  );

  function handleCancel() {
    visible.value = false;
    form.value = { ...initForm };
  }

  const loading = ref(false);
  function handleSave() {
    formRef.value?.validate(async (errors) => {
      if (!errors) {
        try {
          loading.value = true;

          const result = { ...form.value };
          if (currentForm.value.type === FieldTypeEnum.DATA_SOURCE && Array.isArray(result.fieldValue)) {
            // 处理数据源字段，单选传单个值
            result.fieldValue = result.fieldValue?.[0];
          }
          if (currentForm.value.type === FieldTypeEnum.PHONE) {
            // 去空格
            result.fieldValue = result.fieldValue.replace(/[\s\uFEFF\xA0]+/g, '');
          }
          await saveApiMap[props.formKey]({
            ids: props.ids,
            ...result,
            fieldValue: !currentForm.value.businessKey
              ? getNormalFieldValue(currentForm.value, result.fieldValue)
              : result.fieldValue ?? '',
          });
          Message.success(t('common.updateSuccess'));
          handleCancel();
          emit('refresh');
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        } finally {
          loading.value = false;
        }
      }
    });
  }
</script>

<style scoped lang="less">
  .crm-batch-edit-form {
    :deep(.crm-batch-edit-form-item) {
      .n-checkbox-group,
      .n-radio-group {
        margin-top: 4px !important;
      }
    }
  }
</style>
