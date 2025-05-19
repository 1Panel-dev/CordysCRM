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
            :style="{ width: `${item.fieldWidth * 100}%` }"
          >
            <component
              :is="getItemComponent(item.type)"
              :id="item.id"
              v-model:value="formDetail[item.id]"
              :field-config="item"
              :path="item.id"
              :need-init-detail="needInitDetail"
              @change="($event: any) => handleFieldChange($event, item)"
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
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

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
  }>();
  const emit = defineEmits<{
    (e: 'cancel'): void;
    (e: 'init', title: string): void;
    (e: 'saved', isContinue: boolean): void;
  }>();

  const { t } = useI18n();

  const formLoading = defineModel<boolean>('loading', {
    default: false,
  });
  const formUnsaved = defineModel<boolean>('unsaved', {
    default: false,
  });

  const formRef = ref<FormInst>();
  const { needInitDetail, formKey, sourceId, initialSourceName, otherSaveParams } = toRefs(props);

  const {
    fieldList,
    formConfig,
    formDetail,
    unsaved,
    loading,
    formCreateTitle,
    initFormConfig,
    initFormDetail,
    saveForm,
    initForm,
  } = useFormCreateApi({
    formKey,
    sourceId,
    needInitDetail,
    initialSourceName,
    otherSaveParams,
  });

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
    if ([FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(type)) {
      return CrmFormCreateComponents.basicComponents.select;
    }
    if ([FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(type)) {
      return CrmFormCreateComponents.basicComponents.memberSelect;
    }
    if ([FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(type)) {
      return CrmFormCreateComponents.basicComponents.memberSelect;
    }
    if (type === FieldTypeEnum.DIVIDER) {
      return CrmFormCreateComponents.basicComponents.divider;
    }
    if (type === FieldTypeEnum.INPUT_MULTIPLE) {
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
    if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(type)) {
      return CrmFormCreateComponents.advancedComponents.dataSource;
    }
  }

  function handleFieldChange(value: any, item: FormCreateField) {
    // 控制显示规则
    if (item.showControlRules?.length) {
      item.showControlRules.forEach((rule) => {
        fieldList.value.forEach((e) => {
          // 若配置了该值的显示规则，且该字段在显示规则中，则显示
          if (rule.value === value && rule.fieldIds.includes(e.id)) {
            e.show = true;
          } else if (rule.fieldIds.includes(e.id)) {
            // 若该字段在显示规则中，但值不符合，则隐藏该字段
            e.show = false;
          }
        });
      });
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
    unsaved.value = true;
  }

  function handleSave(isContinue = false) {
    formRef.value?.validate((errors) => {
      if (!errors) {
        const result = cloneDeep(formDetail.value);
        fieldList.value.forEach((item) => {
          if (
            [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.MEMBER, FieldTypeEnum.DEPARTMENT].includes(item.type) &&
            Array.isArray(result[item.id])
          ) {
            // 处理数据源字段，单选传单个值
            result[item.id] = result[item.id]?.[0];
          }
        });
        saveForm(result, isContinue, () => {
          emit('saved', isContinue);
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
    emit('init', formCreateTitle.value);
    if (props.sourceId && props.needInitDetail) {
      await initFormDetail();
    }
    initForm();
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
