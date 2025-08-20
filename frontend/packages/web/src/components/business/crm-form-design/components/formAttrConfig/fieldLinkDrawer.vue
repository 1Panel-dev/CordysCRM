<template>
  <CrmDrawer
    v-model:show="visible"
    :width="800"
    :title="t('crmFormDesign.fieldLinkSetting')"
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
        :label="t('crmFormDesign.linkField')"
        path="targetField"
        label-align="left"
        :rule="[{ required: true, message: t('common.required') }]"
      >
        <n-select
          v-model:value="formModel.targetField"
          size="medium"
          :options="fieldOptions"
          @update-value="handleFieldChange"
        />
      </n-form-item>
      <div class="mt-[16px] flex flex-col gap-[12px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
        <div class="flex items-center justify-between">
          <div class="flex-1 text-[var(--text-n1)]">{{ t('crmFormDesign.currentFieldValue') }}</div>
          <div class="mx-[12px] w-[130px] text-[var(--text-n1)]">{{ t('crmFormDesign.linkMethod') }}</div>
          <div class="flex-1 text-[var(--text-n1)]">{{ t('crmFormDesign.linkFieldValue') }}</div>
          <div :class="formModel.linkOptions.length > 1 ? 'w-[46px]' : ''"></div>
        </div>
        <n-scrollbar ref="linkFieldsScrollbar" class="max-h-[40vh] pr-[6px]" content-class="flex flex-col gap-[12px]">
          <div v-for="(line, index) of formModel.linkOptions" :key="index" class="flex items-start justify-between">
            <n-form-item
              :path="`linkOptions.${index}.current`"
              class="flex-1"
              :rule="[
                {
                  required: true,
                  message: t('common.required'),
                  trigger: 'change',
                  type: fieldConfig.type === FieldTypeEnum.SELECT_MULTIPLE ? 'array' : 'string',
                },
              ]"
            >
              <n-select
                v-model:value="line.current"
                :options="fieldConfig.options"
                :fallback-option="
                  line.current !== null && line.current !== undefined && line.current.length > 0
                    ? fallbackOption
                    : false
                "
                :multiple="fieldConfig.type === FieldTypeEnum.SELECT_MULTIPLE"
                max-tag-count="responsive"
              />
            </n-form-item>
            <n-form-item
              :path="`linkOptions.${index}.method`"
              class="mx-[12px] w-[130px]"
              :rule="[{ required: true, message: t('common.required'), trigger: 'change' }]"
            >
              <n-select v-model:value="line.method" :options="methodOptions" />
            </n-form-item>
            <n-form-item
              :path="`linkOptions.${index}.target`"
              class="flex-1"
              :rule="[
                {
                  required: true,
                  message: t('common.required'),
                  trigger: 'change',
                  type: getTargetMultiple(line.method) ? 'array' : 'string',
                },
              ]"
            >
              <n-select
                v-model:value="line.target"
                :options="getLinkFieldOptions"
                :fallback-option="
                  line.target !== null && line.target !== undefined && line.target !== '' ? fallbackOption : false
                "
                :multiple="getTargetMultiple(line.method)"
                max-tag-count="responsive"
              />
            </n-form-item>
            <n-button
              v-if="formModel.linkOptions.length > 1"
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
        <n-button type="primary" text class="w-[fit-content]" @click="handleAddListItem">
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

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import { FieldLinkProp, FormCreateField } from '@/components/business/crm-form-create/types';

  const visible = defineModel<boolean>('visible', { required: true });

  const { t } = useI18n();

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formFields: FormCreateField[];
  }>();

  const emit = defineEmits<{
    (e: 'save', value: FieldLinkProp): void;
  }>();

  const defaultFormModel: FieldLinkProp = {
    targetField: '',
    linkOptions: [
      {
        current: [],
        method: 'AUTO',
        target: '',
      },
    ],
  };

  const linkFieldsScrollbar = ref<ScrollbarInst>();
  const formModel = ref<FieldLinkProp>(cloneDeep(props.fieldConfig.linkProp || defaultFormModel));

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
        formModel.value = cloneDeep(props.fieldConfig.linkProp || defaultFormModel);
        if (props.fieldConfig.linkProp?.targetField === null) {
          formModel.value.targetField = FormDesignKeyEnum.CLUE;
          formModel.value.linkOptions = [
            {
              current: [],
              method: 'AUTO',
              target: '',
            },
          ];
        }
      }
    },
    {
      immediate: true,
    }
  );

  const fieldOptions = computed(() => {
    return props.formFields
      .filter(
        (e) => [FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(e.type) && e.id !== props.fieldConfig.id
      )
      .map((f) => ({ label: f.name, value: f.id }));
  });

  const methodOptions = computed(() => {
    return [
      { label: t('crmFormDesign.autoSelect'), value: 'AUTO' },
      { label: t('crmFormDesign.rangeLimit'), value: 'HIDDEN' },
    ];
  });

  function getTargetMultiple(method: 'AUTO' | 'HIDDEN') {
    const targetField = props.formFields.find((f) => f.id === formModel.value.targetField);
    return targetField?.type === FieldTypeEnum.SELECT_MULTIPLE || method === 'HIDDEN';
  }

  const getLinkFieldOptions = computed(() => {
    const currentField = props.formFields.find((f) => f.id === formModel.value.targetField);
    if (!currentField) return [];
    return currentField.options;
  });

  function fallbackOption(val: string | number) {
    return {
      label: t('crmFormDesign.fieldNotExist'),
      value: val,
    };
  }

  function handleFieldChange(val: FormDesignKeyEnum) {
    if (val !== formModel.value.targetField) {
      formRef.value?.restoreValidation();
      formModel.value.linkOptions = [
        {
          current: [],
          method: 'AUTO',
          target: '',
        },
      ];
    }
  }

  function handleCancel() {
    formModel.value = cloneDeep(defaultFormModel);
  }

  function handleAddListItem() {
    formRef.value?.validate((errors) => {
      if (!errors) {
        formModel.value.linkOptions.push({
          current: [],
          method: 'AUTO',
          target: '',
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
    formModel.value.linkOptions.splice(index, 1);
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
