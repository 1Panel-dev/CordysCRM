<template>
  <n-form
    ref="formRef"
    :model="formDetail"
    :label-placement="formConfig.labelPos"
    :require-mark-placement="formConfig.labelPos === 'left' ? 'left' : 'right'"
    label-width="auto"
    class="crm-form-create"
  >
    <n-scrollbar>
      <div class="flex h-full w-full flex-wrap content-start">
        <template v-for="item in fieldList" :key="item.id">
          <div
            v-if="item.show !== false && item.readable"
            class="crm-form-create-item"
            :style="{ width: item.type === FieldTypeEnum.ATTACHMENT ? '100%' : `${item.fieldWidth * 100}%` }"
          >
            <component
              :is="getItemComponent(item)"
              :id="item.id"
              v-model:value="formDetail[item.id]"
              :field-config="item.resourceFieldId ? { ...item, rules: [] } : item"
              :form-detail="formDetail"
              :origin-form-detail="originFormDetail"
              :path="item.id"
              :need-init-detail="needInitDetail"
              @change="(value: any, source: Record<string, any>[]) => handleFieldChange(value, source, item)"
            />
          </div>
        </template>
      </div>
    </n-scrollbar>
    <div class="crm-form-create-footer" :class="formConfig.optBtnPos">
      <n-button v-if="props.isEdit" type="primary" @click="handleSave(false)">
        {{ t('common.update') }}
      </n-button>
      <template v-else>
        <n-button v-if="formConfig.optBtnContent[0].enable" type="primary" @click="handleSave(false)">
          {{ formConfig.optBtnContent[0].text }}
        </n-button>
        <n-button v-if="formConfig.optBtnContent[1].enable" type="primary" ghost @click="handleSave(true)">
          {{ formConfig.optBtnContent[1].text }}
        </n-button>
      </template>
      <n-button v-if="formConfig.optBtnContent[2].enable" secondary @click="emit('cancel')">
        {{ formConfig.optBtnContent[2].text }}
      </n-button>
    </div>
  </n-form>
</template>

<script setup lang="ts">
  import { FormInst, NButton, NForm, NScrollbar } from 'naive-ui';
  import { cloneDeep, isEqual } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum, FormLinkScenarioEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { specialBusinessKeyMap } from '@lib/shared/method/formCreate';
  import { FormViewSize } from '@lib/shared/models/system/module';

  import CrmFormCreateComponents from '@/components/business/crm-form-create/components';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  import useFormCreateApi from '@/hooks/useFormCreateApi';

  const props = defineProps<{
    isEdit?: boolean;
    sourceId?: string;
    formKey: FormDesignKeyEnum;
    needInitDetail?: boolean; // 是否需要初始化详情
    initialSourceName?: string; // 初始化详情时的名称
    otherSaveParams?: Record<string, any>;
    linkFormInfo?: Record<string, any>; // 关联表单信息
    linkFormKey?: FormDesignKeyEnum;
    linkScenario?: FormLinkScenarioEnum; // 关联表单场景
  }>();
  const emit = defineEmits<{
    (e: 'cancel'): void;
    (e: 'init', title: string, formViewSize?: FormViewSize): void;
    (e: 'saved', isContinue: boolean, res: any): void;
  }>();

  const { t } = useI18n();

  const formLoading = defineModel<boolean>('loading', {
    default: false,
  });
  const formUnsaved = defineModel<boolean>('unsaved', {
    default: false,
  });

  const formRef = ref<FormInst>();
  const {
    needInitDetail,
    formKey,
    sourceId,
    initialSourceName,
    otherSaveParams,
    linkFormInfo,
    linkFormKey,
    linkScenario,
  } = toRefs(props);

  const {
    fieldList,
    formConfig,
    formDetail,
    originFormDetail,
    unsaved,
    loading,
    formCreateTitle,
    initFormConfig,
    initFormDetail,
    saveForm,
    initForm,
    initFormShowControl,
  } = useFormCreateApi({
    formKey,
    sourceId,
    needInitDetail,
    initialSourceName,
    otherSaveParams,
    linkFormInfo,
    linkFormKey,
    linkScenario,
  });

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
    if (item.type === FieldTypeEnum.DIVIDER) {
      return CrmFormCreateComponents.basicComponents.divider;
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
    if (item.type === FieldTypeEnum.SERIAL_NUMBER) {
      return CrmFormCreateComponents.advancedComponents.serialNumber;
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
    if (item.type === FieldTypeEnum.FORMULA) {
      return CrmFormCreateComponents.advancedComponents.formula;
    }
    if ([FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(item.type)) {
      return CrmFormCreateComponents.advancedComponents.dataTable;
    }
  }

  function applyFieldLink(item: FormCreateField) {
    const currentFieldValue = formDetail.value[item.id];
    const linkField = fieldList.value.find((f) => f.id === item.linkProp?.targetField);
    if (item.linkProp?.linkOptions) {
      for (let i = 0; i < item.linkProp?.linkOptions.length; i++) {
        const option = item.linkProp?.linkOptions[i];
        if (isEqual(currentFieldValue, option.current)) {
          if (linkField) {
            if (option.method === 'HIDDEN') {
              linkField.linkRange = Array.isArray(option.target) ? option.target : [option.target];
            } else {
              linkField.linkRange = undefined;
              formDetail.value[linkField.id] = option.target;
            }
            return;
          }
        }
      }
    }
  }

  function handleFieldChange(value: any, source: Record<string, any>[], item: FormCreateField) {
    // 控制显示规则
    if (item.showControlRules?.length) {
      initFormShowControl();
      nextTick(() => {
        const labelNodes = Array.from(document.querySelectorAll('.n-form-item-label'));
        const noWidthLabelNodes = labelNodes.filter((e) => (e as HTMLElement).style.width === '');
        const hasWidthLabelNode = labelNodes.filter((e) => (e as HTMLElement).style.width !== '')[0];
        if (noWidthLabelNodes.length > 0) {
          noWidthLabelNodes.forEach((e) => {
            (e as HTMLElement).style.width = `${hasWidthLabelNode?.clientWidth}px`;
          });
        }
      });
    }
    // 字段联动
    if (item.linkProp?.targetField && item.linkProp?.linkOptions.length) {
      applyFieldLink(item);
    }
    if (item.type === FieldTypeEnum.ATTACHMENT) {
      formRef.value?.validate();
    }
    if (item.type === FieldTypeEnum.DATA_SOURCE && item.showFields?.length) {
      // 数据源显示字段联动
      const showFields = fieldList.value.filter((f) => f.resourceFieldId === item.id);
      showFields.forEach((field) => {
        const target = source.find((s) => s.id === value[0]);
        formDetail.value[field.id] =
          field.businessKey && specialBusinessKeyMap[field.businessKey]
            ? target?.[specialBusinessKeyMap[field.businessKey]]
            : target?.[field.businessKey || field.id];
      });
    }
    unsaved.value = true;
  }

  function transformFieldValue(item: FormCreateField, result: Record<string, any>, key: string) {
    if (
      [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.MEMBER, FieldTypeEnum.DEPARTMENT].includes(item.type) &&
      Array.isArray(result[key])
    ) {
      // 处理数据源字段，单选传单个值
      result[key] = result[key]?.[0];
    }
    if (item.type === FieldTypeEnum.PHONE) {
      // 去空格
      result[key] = result[key]?.replace(/[\s\uFEFF\xA0]+/g, '');
    }
  }

  function transformSubFieldsValue(item: FormCreateField, result: Record<string, any>[]) {
    const currentFieldValues = result.map((res) => res[item.businessKey || item.id]);
    currentFieldValues.forEach((fieldValue, index) => {
      if ([FieldTypeEnum.DATA_SOURCE].includes(item.type) && Array.isArray(fieldValue)) {
        // 处理数据源字段，单选传单个值
        result[index][item.businessKey || item.id] = fieldValue?.[0];
      }
      if (item.type === FieldTypeEnum.PHONE) {
        // 去空格
        result[index][item.businessKey || item.id] = fieldValue?.replace(/[\s\uFEFF\xA0]+/g, '');
      }
    });
  }

  function handleSave(isContinue = false) {
    formRef.value?.validate((errors) => {
      if (!errors) {
        const result = cloneDeep(formDetail.value);
        fieldList.value.forEach((item) => {
          if ([FieldTypeEnum.SUB_PRODUCT, FieldTypeEnum.SUB_PRICE].includes(item.type) && item.subFields?.length) {
            item.subFields.forEach((subField) => {
              transformSubFieldsValue(subField, result[item.id]);
            });
          } else {
            transformFieldValue(item, result, item.id);
          }
        });
        saveForm(result, isContinue, (_isContinue, res) => {
          emit('saved', isContinue, res);
        });
      } else {
        // 滚动到报错的位置
        const firstErrorId = errors[0]?.[0]?.field;
        if (firstErrorId) {
          const fieldElement = document.getElementById(firstErrorId);
          fieldElement?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      }
    });
  }

  watch(
    () => loading.value,
    (val) => {
      formLoading.value = val;
    }
  );

  watch(
    () => unsaved.value,
    (val) => {
      formUnsaved.value = val;
    }
  );

  onBeforeMount(async () => {
    await initFormConfig();
    emit('init', formCreateTitle.value, formConfig.value.viewSize);
    if (props.sourceId && props.needInitDetail) {
      await initFormDetail();
    }
    initForm(props.linkScenario);
  });
</script>

<style lang="less">
  .crm-form-create {
    @apply relative flex h-full flex-col;
    .crm-form-create-item {
      @apply relative self-start;

      padding: 0 16px;
      border-radius: var(--border-radius-small);
      .n-form-item-label {
        @apply w-full items-center;

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
