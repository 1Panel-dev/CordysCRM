<template>
  <CrmModal
    v-model:show="visible"
    :title="t('crmFormDesign.fieldLinkSetting')"
    :positive-text="t('common.save')"
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
      <div class="flex flex-col gap-[12px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
        <div class="flex items-center justify-between">
          <div class="flex-1 text-[var(--text-n1)]">{{ t('crmFormDesign.currentFieldValue') }}</div>
          <div class="mx-[12px] text-[var(--text-n1)]"></div>
          <div class="flex-1 text-[var(--text-n1)]">{{ t('crmFormDesign.linkFieldValue') }}</div>
          <div :class="formModel.length > 1 ? 'w-[110px]' : 'w-[64px] '"></div>
        </div>
        <n-scrollbar ref="linkFieldsScrollbar" class="max-h-[40vh] pr-[6px]" content-class="flex flex-col gap-[12px]">
          <div v-for="(line, index) of formModel" :key="index" class="flex items-start justify-between">
            <n-form-item
              :path="`${index}.current`"
              class="flex-1"
              :rule="[
                {
                  required: true,
                  message: t('common.required'),
                  trigger: 'change',
                  type: 'string',
                },
              ]"
            >
              <n-select
                v-model:value="line.current"
                :options="getFieldOptions(line.current)"
                :fallback-option="
                  line.current !== null && line.current !== undefined && line.current.length > 0
                    ? fallbackOption
                    : false
                "
                max-tag-count="responsive"
              />
            </n-form-item>
            <n-form-item class="mx-[12px]">
              {{ t('crmFormDesign.fill') }}
            </n-form-item>
            <n-form-item
              :path="`${index}.link`"
              class="flex-1"
              :rule="[
                {
                  required: true,
                  message: t('common.required'),
                  trigger: 'change',
                  type: 'string',
                },
              ]"
            >
              <n-select
                v-model:value="line.link"
                :options="linkFieldOptions"
                :fallback-option="
                  line.link !== null && line.link !== undefined && line.link !== '' ? fallbackOption : false
                "
                :render-label="renderLinkOptionLabel"
                :render-tag="renderLinkOptionTag"
              />
            </n-form-item>
            <div class="mx-[8px] flex h-[32px] w-[35px] items-center">
              {{ t('crmFormDesign.fillValue') }}
            </div>
            <n-form-item :path="`${index}.enable`">
              <n-switch v-model:value="line.enable" />
            </n-form-item>
            <n-button v-if="formModel.length > 1" ghost class="ml-[12px] px-[7px]" @click="handleDeleteListItem(index)">
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
  </CrmModal>
</template>

<script lang="ts" setup>
  import {
    FormInst,
    NButton,
    NDivider,
    NForm,
    NFormItem,
    NIcon,
    NScrollbar,
    NSelect,
    NSwitch,
    ScrollbarInst,
    type SelectOption,
  } from 'naive-ui';
  import { Add } from '@vicons/ionicons5';
  import { cloneDeep } from 'lodash-es';

  import { FieldDataSourceTypeEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { dataSourceFilterFormKeyMap } from '@/components/business/crm-form-create/config';
  import { type DataSourceLinkField, FormCreateField } from '@/components/business/crm-form-create/types';

  import { getFieldDisplayList } from '@/api/modules';

  import { Option } from 'naive-ui/es/transfer/src/interface';

  const visible = defineModel<boolean>('visible', { required: true });

  const { t } = useI18n();

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formFields: FormCreateField[];
  }>();

  const emit = defineEmits<{
    (e: 'save', value: DataSourceLinkField[]): void;
  }>();

  const linkFieldsScrollbar = ref<ScrollbarInst>();
  const formModel = ref<DataSourceLinkField[]>(
    cloneDeep(
      props.fieldConfig.linkFields || [
        {
          current: '',
          link: '',
          method: 'fill',
          enable: true,
        },
      ]
    )
  );

  const formRef = ref<FormInst>();
  function save() {
    formRef.value?.validate((errors) => {
      if (!errors) {
        visible.value = false;
        emit('save', cloneDeep(formModel.value));
      }
    });
  }

  const formKey = computed<FormDesignKeyEnum>(() => {
    return dataSourceFilterFormKeyMap[
      props.fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER
    ] as FormDesignKeyEnum;
  });
  const linkFieldOptions = ref<Option[]>([]);
  async function getDisplayList() {
    try {
      const res = await getFieldDisplayList(formKey.value);
      linkFieldOptions.value = res.fields
        .filter((e) => e.type === FieldTypeEnum.DATA_SOURCE)
        .map((item) => {
          return {
            label: item.name,
            value: item.id,
          };
        });
      linkFieldOptions.value.unshift({
        label: props.fieldConfig.name,
        value: props.fieldConfig.businessKey || props.fieldConfig.id,
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        getDisplayList();
        formModel.value = cloneDeep(
          props.fieldConfig.linkFields || [
            {
              current: '',
              link: '',
              method: 'fill',
              enable: true,
            },
          ]
        );
      }
    },
    {
      immediate: true,
    }
  );

  function getFieldOptions(currentFieldId: string) {
    const alreadySelectedFields = formModel.value.map((f) => f.current);
    return props.formFields
      .filter(
        (f) =>
          [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(f.type) &&
          f.id !== props.fieldConfig.id &&
          (f.id === currentFieldId || !alreadySelectedFields.includes(f.id))
      )
      .map((f) => ({ label: f.name, value: f.id }));
  }

  function renderLinkOptionLabel(option: Option) {
    if (option.value === props.fieldConfig.businessKey || option.value === props.fieldConfig.id) {
      return h('div', {}, { default: () => [option.label, h(NDivider, { class: '!mb-0 !mt-[6px]' })] });
    }
    return h(
      'div',
      { class: 'flex items-center gap-[4px]' },
      {
        default: () => [
          h(CrmIcon, { type: 'iconicon_correlation', class: 'mr-[4px] text-[var(--text-n4)]', size: 14 }),
          option.label,
        ],
      }
    );
  }

  function renderLinkOptionTag(option: SelectOption) {
    if (
      (option.option as Option).value === props.fieldConfig.businessKey ||
      (option.option as Option).value === props.fieldConfig.id
    ) {
      return h('div', {}, { default: () => (option.option as Option).label });
    }
    return h(
      'div',
      { class: 'flex items-center gap-[4px]' },
      {
        default: () => [
          h(CrmIcon, { type: 'iconicon_correlation', class: 'mr-[4px] text-[var(--text-n4)]', size: 14 }),
          (option.option as Option).label,
        ],
      }
    );
  }

  function fallbackOption(val: string | number) {
    return {
      label: t('crmFormDesign.fieldNotExist'),
      value: val,
    };
  }

  function handleCancel() {
    formModel.value = [];
  }

  function handleAddListItem() {
    formRef.value?.validate((errors) => {
      if (!errors) {
        formModel.value.push({
          current: '',
          link: '',
          method: 'fill',
          enable: true,
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
    formModel.value.splice(index, 1);
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
