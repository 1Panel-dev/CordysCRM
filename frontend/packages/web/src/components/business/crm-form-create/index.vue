<template>
  <n-form
    ref="formRef"
    :model="form"
    :label-placement="props.formConfig.labelPos"
    :require-mark-placement="props.formConfig.labelPos === 'left' ? 'left' : 'right'"
    class="crm-form-create"
  >
    <n-scrollbar>
      <template v-for="item in list" :key="item.id">
        <div v-if="item.show !== false" class="crm-form-create-item" :style="{ width: `${item.fieldWidth * 100}%` }">
          <component
            :is="getItemComponent(item.type)"
            v-model:value="form[item.id]"
            :field-config="item"
            :path="item.id"
            @change="($event: any) => handleFieldChange($event, item)"
          />
        </div>
      </template>
    </n-scrollbar>
    <div class="crm-form-create-footer" :class="props.formConfig.optBtnPos">
      <n-button v-if="formConfig.optBtnContent[0].enable" type="primary" @click="handleSave(false)">
        {{ formConfig.optBtnContent[0].text }}
      </n-button>
      <n-button v-if="formConfig.optBtnContent[1].enable" type="primary" ghost @click="handleSave(true)">
        {{ formConfig.optBtnContent[1].text }}
      </n-button>
      <n-button v-if="formConfig.optBtnContent[2].enable" secondary @click="emit('cancel')">
        {{ formConfig.optBtnContent[2].text }}
      </n-button>
    </div>
  </n-form>
</template>

<script setup lang="ts">
  import { FormInst, NButton, NForm, NScrollbar } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { FormConfig } from '@lib/shared/models/system/module';

  import CrmFormCreateComponents from '@/components/business/crm-form-create/components';
  import { FormCreateField, FormCreateFieldRule } from '@/components/business/crm-form-create/types';

  import { useI18n } from '@/hooks/useI18n';

  import { rules } from './config';

  const props = defineProps<{
    formConfig: FormConfig;
    formDetail?: Record<string, any>;
  }>();
  const emit = defineEmits<{
    (e: 'cancel'): void;
    (e: 'save', form: Record<string, any>, isContinue: boolean): void;
  }>();

  const { t } = useI18n();

  const list = defineModel<FormCreateField[]>('list', {
    required: true,
  });

  const formRef = ref<FormInst>();
  const form = ref<Record<string, any>>(props.formDetail || {});

  function getItemComponent(type: FieldTypeEnum) {
    if (type === FieldTypeEnum.INPUT) {
      return CrmFormCreateComponents.basicComponents.singleText;
    }
    if (type === FieldTypeEnum.TEXTAREA) {
      return CrmFormCreateComponents.basicComponents.textarea;
    }
    if (type === FieldTypeEnum.INPUT_NUMBER) {
      return CrmFormCreateComponents.basicComponents.inputNumber;
    }
    if (type === FieldTypeEnum.DATE_TIME) {
      return CrmFormCreateComponents.basicComponents.dateTime;
    }
    if (type === FieldTypeEnum.RADIO) {
      return CrmFormCreateComponents.basicComponents.radio;
    }
    if (type === FieldTypeEnum.CHECKBOX) {
      return CrmFormCreateComponents.basicComponents.checkbox;
    }
    if (type === FieldTypeEnum.SELECT) {
      return CrmFormCreateComponents.basicComponents.select;
    }
    if (type === FieldTypeEnum.MEMBER) {
      return CrmFormCreateComponents.basicComponents.memberSelect;
    }
    if (type === FieldTypeEnum.DEPARTMENT) {
      return CrmFormCreateComponents.basicComponents.memberSelect;
    }
    if (type === FieldTypeEnum.DIVIDER) {
      return CrmFormCreateComponents.basicComponents.divider;
    }
    if (type === FieldTypeEnum.MULTIPLE_INPUT) {
      return CrmFormCreateComponents.basicComponents.tagInput;
    }
    if (type === FieldTypeEnum.PICTURE) {
      return CrmFormCreateComponents.advancedComponents.upload;
    }
    if (type === FieldTypeEnum.LOCATION) {
      return CrmFormCreateComponents.advancedComponents.location;
    }
    if (type === FieldTypeEnum.PHONE) {
      return CrmFormCreateComponents.advancedComponents.phone;
    }
    if (type === FieldTypeEnum.DATA_SOURCE) {
      return CrmFormCreateComponents.advancedComponents.dataSource;
    }
  }

  function handleFieldChange(value: any, item: FormCreateField) {
    // 控制显示规则
    if (item.showControlRules?.length) {
      item.showControlRules.forEach((rule) => {
        list.value.forEach((e) => {
          // 若配置了该值的显示规则，且该字段在显示规则中，则显示
          if (rule.value === value && rule.fieldIds.includes(e.id)) {
            e.show = true;
          } else if (rule.fieldIds.includes(e.id)) {
            // 若该字段在显示规则中，但值不符合，则隐藏该字段
            e.show = false;
          }
        });
      });
    }
  }

  function handleSave(isContinue = false) {
    formRef.value?.validate((errors) => {
      if (!errors) {
        const result = cloneDeep(form.value);
        list.value.forEach((item) => {
          if (item.type === FieldTypeEnum.DATA_SOURCE) {
            // 处理数据源字段，单选传单个值，多选传数组
            result[item.id] = item.multiple ? result[item.id] : result[item.id][0];
          }
        });
        emit('save', result, isContinue);
      }
    });
  }

  function getRuleType(item: FormCreateField) {
    if (
      (item.type === FieldTypeEnum.SELECT && item.multiple) ||
      item.type === FieldTypeEnum.CHECKBOX ||
      item.type === FieldTypeEnum.MULTIPLE_INPUT ||
      (item.type === FieldTypeEnum.MEMBER && item.multiple) ||
      (item.type === FieldTypeEnum.DEPARTMENT && item.multiple) ||
      item.type === FieldTypeEnum.DATA_SOURCE
    ) {
      return 'array';
    }
    if (item.type === FieldTypeEnum.DATE_TIME) {
      return 'date';
    }
    if (item.type === FieldTypeEnum.INPUT_NUMBER) {
      return 'number';
    }
    return 'string';
  }

  watch(
    () => list.value,
    () => {
      list.value.forEach((item) => {
        if (!form.value[item.id]) {
          let defaultValue = item.defaultValue || '';
          if ([FieldTypeEnum.DATE_TIME, FieldTypeEnum.INPUT_NUMBER].includes(item.type)) {
            defaultValue = defaultValue || null;
          }
          if (getRuleType(item) === 'array') {
            defaultValue = defaultValue || [];
          }
          form.value[item.id] = defaultValue;
        }
        handleFieldChange(form.value[item.id], item); // 初始化时，根据字段值控制显示
        const fullRules: FormCreateFieldRule[] = [];
        rules.forEach((rule) => {
          // 遍历规则集合，将全量的规则配置载入
          const staticRule = cloneDeep(rules.find((e) => e.key === rule.key));
          if (staticRule) {
            staticRule.regex = rule.regex; // 正则表达式(目前没有)是配置到后台存储的，需要读取
            staticRule.message = t(rule.message as string, { value: t(item.name) });
            staticRule.type = getRuleType(item);
            if (item.type === FieldTypeEnum.DATA_SOURCE) {
              staticRule.trigger = 'none';
            }
            fullRules.push(staticRule);
          }
        });
        item.rules = fullRules;
      });
    },
    { immediate: true }
  );
</script>

<style lang="less">
  .crm-form-create {
    @apply relative flex h-full flex-col;
    .crm-form-create-item {
      @apply relative self-start;

      padding: 0 16px;
      border-radius: var(--border-radius-small);
      .n-form-item-label {
        margin-bottom: 4px;
        padding-bottom: 0;
      }
    }
    .crm-form-create-footer {
      @apply relative flex w-full;

      padding: 12px 16px;
      border-top: 1px solid var(--text-n8);
      gap: 8px;
    }
  }
</style>
