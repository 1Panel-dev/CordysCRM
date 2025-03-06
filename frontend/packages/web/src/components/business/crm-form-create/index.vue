<template>
  <n-form
    ref="formRef"
    :model="form"
    :label-placement="props.formConfig.labelPos"
    :require-mark-placement="props.formConfig.labelPos === 'left' ? 'left' : 'right'"
    class="crm-form-create"
  >
    <template v-for="item in list" :key="item.id">
      <div v-if="item.show !== false" class="crm-form-create-item" :style="{ width: `${item.fieldWidth * 100}%` }">
        <component
          :is="getItemComponent(item.type)"
          :field-config="item"
          :path="item.id"
          @change="($event: any) => handleFieldChange($event, item)"
        />
      </div>
    </template>
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
  import { FormInst, NButton, NForm } from 'naive-ui';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { FormConfig } from '@lib/shared/models/system/module';

  import CrmFormCreateComponents from '@/components/business/crm-form-create/components';
  import { FormCreateField, FormCreateFieldRule } from '@/components/business/crm-form-create/types';

  import { rules } from './config';

  const props = defineProps<{
    formConfig: FormConfig;
    formDetail?: Record<string, any>;
  }>();
  const emit = defineEmits<{
    (e: 'cancel'): void;
    (e: 'save', form: Record<string, any>, isContinue: boolean): void;
  }>();

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
        // 若配置了该值的显示规则
        if (rule.value === value) {
          list.value.forEach((e) => {
            // 若该字段在显示规则中，则根据规则显示或隐藏
            e.show = rule.fieldIds.includes(e.id);
          });
        }
      });
    }
  }

  function handleSave(isContinue = false) {
    formRef.value?.validate((errors) => {
      if (!errors) {
        emit('save', form.value, isContinue);
      }
    });
  }

  onBeforeMount(() => {
    list.value.forEach((item) => {
      if (!form.value[item.id]) {
        form.value[item.id] = item.defaultValue;
      }
      const fullRules: FormCreateFieldRule[] = [];
      rules.forEach((rule) => {
        // 遍历规则集合，将全量的规则配置载入
        const staticRule = item.rules.find((e) => e.key === rule.key);
        if (staticRule) {
          staticRule.regex = rule.regex; // 正则表达式(目前没有)是配置到后台存储的，需要读取
          fullRules.push(staticRule);
        }
      });
      item.rules = fullRules;
    });
  });
</script>

<style lang="less">
  .crm-form-create {
    @apply relative flex h-full flex-col;

    padding: 24px 24px 0;
    .crm-form-create-item {
      @apply relative cursor-move self-start;

      padding: 16px;
      border-radius: var(--border-radius-small);
      .n-form-item-label {
        margin-bottom: 4px;
        padding-bottom: 0;
      }
      .n-form-item-feedback-wrapper {
        @apply hidden;
      }
    }
    .crm-form-create-footer {
      @apply relative flex w-full;

      padding: 12px 0;
      border-top: 1px solid var(--text-n8);
      gap: 8px;
    }
  }
</style>
