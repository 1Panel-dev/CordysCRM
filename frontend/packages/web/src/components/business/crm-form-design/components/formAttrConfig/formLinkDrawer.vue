<template>
  <CrmDrawer
    v-model:show="visible"
    :width="800"
    :title="t('crmFormDesign.formLinkSetting')"
    :ok-text="t('common.save')"
    footer
    @confirm="save"
    @cancel="handleCancel"
  >
    <n-form
      ref="formRef"
      :model="formModel"
      label-width="90"
      label-placement="left"
      require-mark-placement="left"
      class="crm-form-design-link-modal"
    >
      <n-form-item
        :label="t('crmFormDesign.linkForm')"
        path="formKey"
        label-align="left"
        :rule="[{ required: true, message: t('common.required') }]"
      >
        <n-select
          v-model:value="formModel.formKey"
          size="medium"
          :options="formKeyOptions"
          @update-value="handleFormKeyChange"
        />
      </n-form-item>
      <div class="mt-[16px] flex flex-col gap-[12px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
        <div class="flex items-center justify-between">
          <div class="flex-1 text-[var(--text-n1)]">{{ t('crmFormDesign.currentForm') }}</div>
          <div class="w-[80px]"></div>
          <div class="flex-1 text-[var(--text-n1)]">{{ t('crmFormDesign.linkForm') }}</div>
          <div :class="formModel.linkFields.length > 1 ? 'w-[110px]' : 'w-[64px]'"></div>
        </div>
        <n-scrollbar ref="linkFieldsScrollbar" class="max-h-[40vh] pr-[6px]" content-class="flex flex-col gap-[12px]">
          <div v-for="(line, index) of formModel.linkFields" :key="index" class="flex items-start justify-between">
            <n-form-item
              :path="`linkFields.${index}.current`"
              class="flex-1"
              :rule="[{ required: true, message: t('common.required'), trigger: 'change' }]"
            >
              <n-select
                v-model:value="line.current"
                :options="getCurrentFieldOptions(line.current)"
                :fallback-option="
                  line.current !== null && line.current !== undefined && line.current !== '' ? fallbackOption : false
                "
                @update-value="line.link = ''"
              />
            </n-form-item>
            <div class="flex h-[32px] w-[60px] items-center justify-center text-[var(--text-n1)]">
              {{ t('crmFormDesign.fill') }}
            </div>
            <n-form-item
              :path="`linkFields.${index}.link`"
              class="flex-1"
              :rule="[{ required: true, message: t('common.required'), trigger: 'change' }]"
            >
              <n-select
                v-model:value="line.link"
                :options="getLinkFieldOptions(line.current)"
                :fallback-option="
                  line.link !== null && line.link !== undefined && line.link !== '' ? fallbackOption : false
                "
              />
            </n-form-item>
            <div class="ml-[12px] flex h-[32px] w-[30px] items-center text-[var(--text-n1)]">
              {{ t('crmFormDesign.fillValue') }}
            </div>
            <n-button
              v-if="formModel.linkFields.length > 1"
              ghost
              class="ml-[12px] px-[7px]"
              @click="handleDeleteListItem(index)"
            >
              <template #icon>
                <CrmIcon type="iconicon_minus_circle" class="text-[var(--text-n4)]" :size="16" />
              </template>
            </n-button>
          </div>
        </n-scrollbar>
        <n-button
          type="primary"
          text
          class="w-[fit-content]"
          :disabled="currentFieldOptions.length === formModel.linkFields.length"
          @click="handleAddListItem"
        >
          <template #icon>
            <n-icon><Add /></n-icon>
          </template>
          {{ t('crmFormDesign.addLink') }}
        </n-button>
      </div>
    </n-form>
    <template #footerLeft>
      <n-button secondary @click="handleCancel">{{ t('common.clear') }}</n-button>
    </template>
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { FormInst, NButton, NForm, NFormItem, NIcon, NScrollbar, NSelect, ScrollbarInst } from 'naive-ui';
  import { Add } from '@vicons/ionicons5';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { FormConfigLinkProp } from '@lib/shared/models/system/module';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  import useFormCreateApi from '@/hooks/useFormCreateApi';

  import {
    dataSourceTypes,
    departmentTypes,
    hiddenTypes,
    linkAllAcceptTypes,
    memberTypes,
    multipleTypes,
    needSameTypes,
    singleTypes,
  } from '../../linkFormConfig';

  const visible = defineModel<boolean>('visible', { required: true });

  const { t } = useI18n();

  const props = defineProps<{
    linkProp?: FormConfigLinkProp;
    formFields: FormCreateField[];
    formKey: FormDesignKeyEnum;
  }>();

  const emit = defineEmits<{
    (e: 'save', value: FormConfigLinkProp): void;
  }>();

  const formKeyOptions = computed(() => {
    if (props.formKey === FormDesignKeyEnum.CUSTOMER) {
      return [
        {
          label: t('crmFormDesign.clue'),
          value: FormDesignKeyEnum.CLUE,
        },
      ];
    }
    return [
      {
        label: t('common.plan'),
        value: FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER,
      },
    ];
  });
  const defaultFormModel: FormConfigLinkProp = {
    formKey: formKeyOptions.value[0].value,
    linkFields: [
      {
        current: '',
        link: '',
      },
    ],
  };

  const linkFieldsScrollbar = ref<ScrollbarInst>();
  const formModel = ref<FormConfigLinkProp>(cloneDeep(props.linkProp || defaultFormModel));
  const _formKey = computed(() => formModel.value.formKey);

  const { fieldList, initFormConfig } = useFormCreateApi({
    formKey: _formKey,
  });

  const formRef = ref<FormInst>();
  function save() {
    formRef.value?.validate((errors) => {
      if (!errors) {
        visible.value = false;
        emit('save', cloneDeep(formModel.value));
      }
    });
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        formModel.value = cloneDeep(props.linkProp || defaultFormModel);
        if (props.linkProp?.formKey === null) {
          formModel.value.formKey = formKeyOptions.value[0].value;
          formModel.value.linkFields = [
            {
              current: '',
              link: '',
            },
          ];
        }
        initFormConfig();
      }
    },
    {
      immediate: true,
    }
  );

  const currentFieldOptions = computed(() => {
    return props.formFields
      .filter((e) => !hiddenTypes.includes(e.type) && e.type !== FieldTypeEnum.SERIAL_NUMBER)
      .map((f) => ({ label: f.name, value: f.id }));
  });

  function getCurrentFieldOptions(currentFieldId: string) {
    const alreadySelectedFields = formModel.value.linkFields.map((f) => f.current);
    return currentFieldOptions.value.filter(
      (f) => f.value === currentFieldId || !alreadySelectedFields.includes(f.value)
    );
  }

  function getLinkFieldOptions(currentFieldId: string) {
    const currentField = props.formFields.find((f) => f.id === currentFieldId);
    if (!currentField) return [];
    if (dataSourceTypes.includes(currentField.type)) {
      // 左侧是数据源，右侧也只能选择数据源
      if (currentField.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE) {
        return fieldList.value
          .filter(
            (f) =>
              dataSourceTypes.includes(f.type) &&
              f.dataSourceType === currentField.dataSourceType &&
              !hiddenTypes.includes(f.type)
          )
          .map((f) => ({ label: f.name, value: f.id }));
      }
      return fieldList.value
        .filter(
          (f) =>
            f.type === currentField.type &&
            f.dataSourceType === currentField.dataSourceType &&
            !hiddenTypes.includes(f.type)
        )
        .map((f) => ({ label: f.name, value: f.id }));
    }
    if (needSameTypes.includes(currentField.type)) {
      // 两侧需要保持一致的类型
      return fieldList.value
        .filter((f) => f.type === currentField.type && !hiddenTypes.includes(f.type))
        .map((f) => ({ label: f.name, value: f.id }));
    }
    if (multipleTypes.includes(currentField.type)) {
      // 多选类型，也可接受单选类型值
      return fieldList.value
        .filter((f) => [...multipleTypes, ...singleTypes].includes(f.type) && !hiddenTypes.includes(f.type))
        .map((f) => ({ label: f.name, value: f.id }));
    }
    if (singleTypes.includes(currentField.type)) {
      // 单选类型
      return fieldList.value
        .filter((f) => singleTypes.includes(f.type) && !hiddenTypes.includes(f.type))
        .map((f) => ({ label: f.name, value: f.id }));
    }
    if (memberTypes.includes(currentField.type)) {
      // 成员类型
      if (currentField.type === FieldTypeEnum.MEMBER_MULTIPLE) {
        return fieldList.value
          .filter((f) => memberTypes.includes(f.type) && !hiddenTypes.includes(f.type))
          .map((f) => ({ label: f.name, value: f.id }));
      }
      return fieldList.value
        .filter((f) => f.type === currentField.type && !hiddenTypes.includes(f.type))
        .map((f) => ({ label: f.name, value: f.id }));
    }
    if (departmentTypes.includes(currentField.type)) {
      // 部门类型
      if (currentField.type === FieldTypeEnum.DEPARTMENT_MULTIPLE) {
        return fieldList.value
          .filter((f) => departmentTypes.includes(f.type) && !hiddenTypes.includes(f.type))
          .map((f) => ({ label: f.name, value: f.id }));
      }
      return fieldList.value
        .filter((f) => f.type === currentField.type && !hiddenTypes.includes(f.type))
        .map((f) => ({ label: f.name, value: f.id }));
    }
    if (linkAllAcceptTypes.includes(currentField.type)) {
      return fieldList.value.filter((f) => !hiddenTypes.includes(f.type)).map((f) => ({ label: f.name, value: f.id }));
    }
    return [];
  }

  function fallbackOption(val: string | number) {
    return {
      label: t('crmFormDesign.fieldNotExist'),
      value: val,
    };
  }

  function handleFormKeyChange(val: FormDesignKeyEnum) {
    if (val !== formModel.value.formKey) {
      formRef.value?.restoreValidation();
      formModel.value.linkFields = [
        {
          current: '',
          link: '',
        },
      ];
      nextTick(() => {
        initFormConfig();
      });
    }
  }

  function handleCancel() {
    formModel.value = cloneDeep(defaultFormModel);
  }

  function handleAddListItem() {
    formRef.value?.validate((errors) => {
      if (!errors) {
        formModel.value.linkFields.push({
          current: '',
          link: '',
        });
        nextTick(() => {
          linkFieldsScrollbar.value?.scrollTo({
            top: 99999,
            behavior: 'smooth',
          });
        });
      } else {
        document.querySelector('.n-form-item-blank--error')?.scrollIntoView({
          behavior: 'smooth',
        });
      }
    });
  }

  function handleDeleteListItem(index: number) {
    formModel.value.linkFields.splice(index, 1);
  }
</script>

<style lang="less">
  .crm-form-design-link-modal {
    .n-form-item-feedback-wrapper {
      display: none;
    }
    .n-form-item-blank--error + .n-form-item-feedback-wrapper {
      display: inline-block;
    }
    .n-scrollbar-rail--vertical {
      @apply !right-0;
    }
  }
</style>
