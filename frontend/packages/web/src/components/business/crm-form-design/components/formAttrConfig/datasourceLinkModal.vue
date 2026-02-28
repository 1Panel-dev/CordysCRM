<template>
  <CrmModal
    v-model:show="visible"
    :title="t('crmFormDesign.fieldLinkSetting')"
    :positive-text="t('common.save')"
    @confirm="save"
    @cancel="emit('cancel')"
  >
    <n-radio-group
      v-if="props.formFields.some((e) => [FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(e.type))"
      v-model:value="linkMode"
      name="radiogroup"
      class="mb-[16px]"
    >
      <n-radio-button value="form" class="flex-1 text-center">{{ t('crmFormDesign.form') }}</n-radio-button>
      <n-radio-button value="subForm" class="flex-1 text-center">{{ t('crmFormDesign.subForm') }}</n-radio-button>
    </n-radio-group>
    <n-form
      v-show="linkMode === 'form'"
      ref="formRef"
      :model="formModel"
      label-width="90"
      label-placement="left"
      require-mark-placement="left"
      class="crm-form-design-link-modal"
    >
      <div class="flex flex-col gap-[12px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
        <div class="flex items-center justify-between">
          <div class="flex-1 text-[var(--text-n1)]">{{ t('crmFormDesign.currentFormField') }}</div>
          <div class="mx-[12px] text-[var(--text-n1)]"></div>
          <div class="flex-1 text-[var(--text-n1)]">{{ t('crmFormDesign.linkField') }}</div>
          <div class="w-[110px]"></div>
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
                :render-label="renderLinkOptionLabel"
                :render-tag="renderLinkOptionTag"
                max-tag-count="responsive"
                @update-value="() => (line.link = '')"
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
                :options="getLinkFieldOptions(line.current)"
                :fallback-option="
                  line.link !== null && line.link !== undefined && line.link !== '' ? fallbackOption : false
                "
                :render-label="renderLinkOptionLabel"
                :render-tag="renderLinkOptionTag"
                class="crm-form-design-link-select"
              />
            </n-form-item>
            <div class="mx-[8px] flex h-[32px] w-[35px] items-center">
              {{ t('crmFormDesign.fillValue') }}
            </div>
            <n-form-item :path="`${index}.enable`">
              <n-switch v-model:value="line.enable" />
            </n-form-item>
            <n-button ghost class="ml-[12px] px-[7px]" @click="handleDeleteListItem(index)">
              <template #icon>
                <CrmIcon type="iconicon_minus_circle" class="text-[var(--text-n4)]" :size="16" />
              </template>
            </n-button>
          </div>
        </n-scrollbar>
        <n-button type="primary" text class="h-[22px] w-[fit-content]" @click="handleAddListItem">
          <template #icon>
            <n-icon><Add /></n-icon>
          </template>
          {{ t('crmFormDesign.addLink') }}
        </n-button>
      </div>
    </n-form>
    <div
      v-show="linkMode === 'subForm'"
      class="flex flex-col gap-[12px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]"
    >
      <div class="flex items-center justify-between pl-[38px]">
        <div class="flex-1 text-[var(--text-n1)]">{{ t('crmFormDesign.currentSubFormField') }}</div>
        <div class="mx-[12px] text-[var(--text-n1)]"></div>
        <div class="flex-1 text-[var(--text-n1)]">{{ t('crmFormDesign.linkSubFormField') }}</div>
        <div class="w-[110px]"></div>
      </div>
      <n-collapse class="crm-form-design-field-link-collapse">
        <n-form
          ref="subFormRef"
          :model="subFormModel"
          label-width="90"
          label-placement="left"
          require-mark-placement="left"
          class="crm-form-design-link-modal"
        >
          <n-collapse-item v-for="(line, index) of subFormModel" :key="index" title="" :name="line.current">
            <div class="pl-[28px]">
              <n-scrollbar
                ref="linkSubFieldsScrollbar"
                class="max-h-[400px]"
                content-class="flex mt-[12px] flex-col gap-[12px]"
              >
                <div
                  v-for="(sub, subIndex) of line.childLinks"
                  :key="subIndex"
                  class="flex items-start justify-between"
                >
                  <n-form-item
                    :path="`${index}.childLinks.${subIndex}.current`"
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
                      v-model:value="sub.current"
                      :options="getSubFieldOptions(sub.current, line.current, line.childLinks)"
                      :fallback-option="
                        sub.current !== null && sub.current !== undefined && sub.current.length > 0
                          ? fallbackOption
                          : false
                      "
                      :render-label="renderLinkOptionLabel"
                      :render-tag="renderLinkOptionTag"
                      max-tag-count="responsive"
                      @update-value="() => (sub.link = '')"
                    />
                  </n-form-item>
                  <n-form-item class="mx-[12px]">
                    {{ t('crmFormDesign.fill') }}
                  </n-form-item>
                  <n-form-item
                    :path="`${index}.childLinks.${subIndex}.link`"
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
                      v-model:value="sub.link"
                      :options="getLinkSubFieldOptions(sub.current, line.current, line.link)"
                      :fallback-option="
                        sub.link !== null && sub.link !== undefined && sub.link !== '' ? fallbackOption : false
                      "
                      :render-label="renderLinkOptionLabel"
                      :render-tag="renderLinkOptionTag"
                      class="crm-form-design-link-select"
                    />
                  </n-form-item>
                  <div class="mx-[8px] flex h-[32px] w-[35px] items-center">
                    {{ t('crmFormDesign.fillValue') }}
                  </div>
                  <n-form-item :path="`${index}.childLinks.${subIndex}.enable`">
                    <n-switch v-model:value="sub.enable" />
                  </n-form-item>
                  <n-button ghost class="ml-[12px] px-[7px]" @click="handleDeleteSubListSubItem(line, subIndex)">
                    <template #icon>
                      <CrmIcon type="iconicon_minus_circle" class="text-[var(--text-n4)]" :size="16" />
                    </template>
                  </n-button>
                </div>
              </n-scrollbar>
              <n-button
                type="primary"
                text
                size="small"
                class="mt-[12px] w-[fit-content] text-[14px]"
                @click="handleAddSubListSubItem(line)"
              >
                <template #icon>
                  <n-icon><Add /></n-icon>
                </template>
                {{ t('crmFormDesign.addFieldLink') }}
              </n-button>
            </div>
            <template #header-extra>
              <div class="flex w-full items-start justify-between" @click.stop>
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
                    :options="getSubFieldParentOptions(line.current)"
                    :fallback-option="
                      line.current !== null && line.current !== undefined && line.current.length > 0
                        ? fallbackOption
                        : false
                    "
                    :render-label="renderLinkOptionLabel"
                    :render-tag="renderLinkOptionTag"
                    max-tag-count="responsive"
                    @update-value="() => (line.link = '')"
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
                    :options="linkSubFieldParentOptions"
                    :fallback-option="
                      line.link !== null && line.link !== undefined && line.link !== '' ? fallbackOption : false
                    "
                    :render-label="renderLinkOptionLabel"
                    :render-tag="renderLinkOptionTag"
                    class="crm-form-design-link-select"
                  />
                </n-form-item>
                <div class="mx-[8px] flex h-[32px] w-[35px] items-center">
                  {{ t('crmFormDesign.fillValue') }}
                </div>
                <n-form-item :path="`${index}.enable`">
                  <n-switch v-model:value="line.enable" />
                </n-form-item>
                <n-button ghost class="ml-[12px] px-[7px]" @click="handleDeleteSubListItem(index)">
                  <template #icon>
                    <CrmIcon type="iconicon_minus_circle" class="text-[var(--text-n4)]" :size="16" />
                  </template>
                </n-button>
              </div>
            </template>
          </n-collapse-item>
        </n-form>
      </n-collapse>
      <n-button type="primary" text size="small" class="w-[fit-content] text-[14px]" @click="handleAddSubListItem">
        <template #icon>
          <n-icon><Add /></n-icon>
        </template>
        {{ t('crmFormDesign.addLink') }}
      </n-button>
    </div>
    <template #footerLeft>
      <n-button secondary @click="handleClear">{{ t('common.clear') }}</n-button>
    </template>
  </CrmModal>
</template>

<script lang="ts" setup>
  import {
    FormInst,
    NButton,
    NCollapse,
    NCollapseItem,
    NDivider,
    NForm,
    NFormItem,
    NIcon,
    NRadioButton,
    NRadioGroup,
    NScrollbar,
    NSelect,
    NSwitch,
    ScrollbarInst,
    type SelectOption,
    useMessage,
  } from 'naive-ui';
  import { Add } from '@vicons/ionicons5';
  import { cloneDeep } from 'lodash-es';

  import { FieldDataSourceTypeEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import {
    dataSourceTypes,
    hiddenTypes,
    linkAllAcceptTypes,
    multipleTypes,
    needSameTypes,
    singleTypes,
  } from '@lib/shared/method/formCreate';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import {
    dataSourceFilterFormKeyMap,
    getFieldIcon,
    getFormConfigApiMap,
  } from '@/components/business/crm-form-create/config';
  import {
    type DataSourceLinkField,
    type DataSourceSubFieldLinkField,
    FormCreateField,
  } from '@/components/business/crm-form-create/types';

  import { getFieldDisplayList } from '@/api/modules';

  import { Option } from 'naive-ui/es/transfer/src/interface';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formFields: FormCreateField[];
  }>();

  const emit = defineEmits<{
    (e: 'save', value: DataSourceLinkField[], subFormValue: DataSourceSubFieldLinkField[]): void;
    (e: 'cancel'): void;
    (e: 'clear'): void;
  }>();

  const { t } = useI18n();
  const message = useMessage();

  const visible = defineModel<boolean>('visible', { required: true });
  const linkMode = ref<'form' | 'subForm'>('form');
  const linkFieldsScrollbar = ref<ScrollbarInst>();
  const formModel = ref<DataSourceLinkField[]>(cloneDeep(props.fieldConfig.linkFields || []));

  const formRef = ref<FormInst>();
  const formKey = computed<FormDesignKeyEnum>(() => {
    return dataSourceFilterFormKeyMap[
      props.fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER
    ] as FormDesignKeyEnum;
  });
  const linkFieldOptions = ref<any[]>([]);
  async function getDisplayList() {
    try {
      const res = await getFieldDisplayList(formKey.value);
      linkFieldOptions.value = res.fields
        .filter((e) => [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(e.type))
        .map((item) => {
          return {
            label: item.name,
            value: item.id,
            icon: getFieldIcon(item.type),
            type: item.type,
          };
        });
      linkFieldOptions.value.unshift({
        label: props.fieldConfig.name,
        value: props.fieldConfig.businessKey || props.fieldConfig.id,
        icon: getFieldIcon(props.fieldConfig.type),
        type: props.fieldConfig.type,
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function getLinkFieldOptions(currentFieldId: string) {
    const currentField = props.formFields.find((f) => f.id === currentFieldId);
    if (currentField?.type === FieldTypeEnum.DATA_SOURCE) {
      // 单选数据源不能填充多选数据源
      return linkFieldOptions.value.filter((f) => f.type !== FieldTypeEnum.DATA_SOURCE_MULTIPLE);
    }
    return linkFieldOptions.value;
  }

  function getFieldOptions(currentFieldId: string) {
    const alreadySelectedFields = formModel.value.map((f) => f.current);
    return props.formFields
      .filter(
        (f) =>
          [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(f.type) &&
          f.id !== props.fieldConfig.id &&
          (f.id === currentFieldId || !alreadySelectedFields.includes(f.id))
      )
      .map((f) => ({ label: f.name, value: f.id, icon: getFieldIcon(f.type) }));
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
          h(CrmIcon, { type: (option as any).icon, class: 'mr-[4px] text-[var(--text-n4)]', size: 14 }),
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
          h(CrmIcon, { type: (option.option as any).icon, class: 'mr-[4px] text-[var(--text-n4)]', size: 14 }),
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

  const linkSubFieldsScrollbar = ref<ScrollbarInst>();
  const subFormRef = ref<FormInst>();
  const subFormModel = ref<DataSourceSubFieldLinkField[]>(cloneDeep(props.fieldConfig.childLinkFields || []));

  const linkSubFieldParents = ref<FormCreateField[]>([]);
  const linkSubFieldParentOptions = computed(() => {
    return linkSubFieldParents.value.map((item) => {
      return {
        label: item.name,
        value: item.id,
        icon: getFieldIcon(item.type),
        type: item.type,
      };
    });
  });
  async function initSubTableFields() {
    try {
      const api = getFormConfigApiMap[formKey.value];
      const res = await api();
      linkSubFieldParents.value = res.fields.filter((e) =>
        [FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(e.type)
      );
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function getSubFieldParentOptions(currentFieldId: string) {
    const alreadySelectedFields = subFormModel.value.map((f) => f.current);
    return props.formFields
      .filter(
        (f) =>
          [FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(f.type) &&
          f.id !== props.fieldConfig.id &&
          (f.id === currentFieldId || !alreadySelectedFields.includes(f.id))
      )
      .map((f) => ({ label: f.name, value: f.id, icon: getFieldIcon(f.type) }));
  }

  function getSubFieldOptions(
    currentFieldId: string,
    parentFieldId: string,
    alreadySelectedFields: DataSourceSubFieldLinkField[]
  ) {
    return props.formFields
      .find((f) => f.id === parentFieldId)
      ?.subFields?.filter(
        (f) =>
          !hiddenTypes.includes(f.type) &&
          f.type !== FieldTypeEnum.FORMULA &&
          !f.resourceFieldId &&
          (f.id === currentFieldId || !alreadySelectedFields.some((field) => field.current === f.id))
      )
      .map((f) => ({ label: f.name, value: f.id, icon: getFieldIcon(f.type) }));
  }

  /**
   * 获取联动字段的选项，联动字段只能选择与当前字段类型匹配的字段
   */
  function getLinkSubFieldOptions(currentFieldId: string, parentFieldId: string, linkParentFieldId: string) {
    const currentParentField = props.formFields.find((f) => f.id === parentFieldId);
    const currentField = currentParentField?.subFields?.find((f) => f.id === currentFieldId);
    const linkParentField = linkSubFieldParents.value.find((f) => f.id === linkParentFieldId);
    if (!currentParentField || !currentParentField.subFields || !currentField || !linkParentField?.subFields) return [];
    const hiddenCondition = (f: FormCreateField) => hiddenTypes.includes(f.type) || f.resourceFieldId;
    if (dataSourceTypes.includes(currentField.type)) {
      // 左侧是数据源，右侧也只能选择数据源
      if (currentField.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE) {
        return linkParentField?.subFields
          ?.filter(
            (f) =>
              dataSourceTypes.includes(f.type) &&
              f.dataSourceType === currentField.dataSourceType &&
              !hiddenCondition(f)
          )
          .map((f) => ({ label: f.name, value: f.id, icon: getFieldIcon(f.type) }));
      }
      return linkParentField?.subFields
        ?.filter(
          (f) => f.type === currentField.type && f.dataSourceType === currentField.dataSourceType && !hiddenCondition(f)
        )
        .map((f) => ({ label: f.name, value: f.id, icon: getFieldIcon(f.type) }));
    }
    if (needSameTypes.includes(currentField.type)) {
      // 两侧需要保持一致的类型
      return linkParentField?.subFields
        ?.filter((f) => f.type === currentField.type && !hiddenCondition(f))
        .map((f) => ({ label: f.name, value: f.id, icon: getFieldIcon(f.type) }));
    }
    if (multipleTypes.includes(currentField.type)) {
      // 多选类型，也可接受单选类型值
      return linkParentField?.subFields
        ?.filter((f) => [...multipleTypes, ...singleTypes].includes(f.type) && !hiddenCondition(f))
        .map((f) => ({ label: f.name, value: f.id, icon: getFieldIcon(f.type) }));
    }
    if (singleTypes.includes(currentField.type)) {
      // 单选类型
      return linkParentField?.subFields
        ?.filter((f) => singleTypes.includes(f.type) && !hiddenCondition(f))
        .map((f) => ({ label: f.name, value: f.id, icon: getFieldIcon(f.type) }));
    }
    if (linkAllAcceptTypes.includes(currentField.type)) {
      return linkParentField?.subFields
        ?.filter((f) => !hiddenCondition(f))
        .map((f) => ({ label: f.name, value: f.id, icon: getFieldIcon(f.type) }));
    }
    return [];
  }

  function handleClear() {
    if (linkMode.value === 'form') {
      formModel.value = [];
    } else {
      subFormModel.value = [];
    }
    nextTick(() => {
      emit('clear');
    });
  }

  function handleAddSubListItem() {
    subFormRef.value?.validate((errors) => {
      if (!errors) {
        subFormModel.value.push({
          current: '',
          link: '',
          method: 'fill',
          enable: true,
          childLinks: [],
        });
        nextTick(() => {
          linkSubFieldsScrollbar.value?.scrollTo({
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

  function handleDeleteSubListItem(index: number) {
    subFormModel.value.splice(index, 1);
  }

  function handleAddSubListSubItem(line: DataSourceSubFieldLinkField) {
    subFormRef.value?.validate((errors) => {
      if (!errors) {
        line.childLinks.push({
          current: '',
          link: '',
          method: 'fill',
          enable: true,
          childLinks: [],
        });
        nextTick(() => {
          linkSubFieldsScrollbar.value?.scrollTo({
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

  function handleDeleteSubListSubItem(line: DataSourceSubFieldLinkField, subIndex: number) {
    line.childLinks.splice(subIndex, 1);
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        getDisplayList();
        formModel.value = cloneDeep(props.fieldConfig.linkFields || []);
        initSubTableFields();
        subFormModel.value = cloneDeep(props.fieldConfig.childLinkFields || []);
        linkMode.value = 'form';
      }
    },
    {
      immediate: true,
    }
  );

  function save() {
    formRef.value?.validate((errors) => {
      if (!errors) {
        subFormRef.value?.validate((subErrors) => {
          if (!subErrors) {
            const unConfigSubField = subFormModel.value.find((e) => e.childLinks?.length === 0);
            if (unConfigSubField) {
              message.warning(
                t('crmFormDesign.subFormUnConfig', {
                  name: props.formFields.find((f) => f.id === unConfigSubField.current)?.name || '',
                })
              );
            }
            emit('save', formModel.value, subFormModel.value);
            visible.value = false;
          }
        });
      } else {
        linkMode.value = 'form';
      }
    });
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
  .crm-form-design-field-link-collapse {
    padding: 8px 8px 8px 12px;
    border-radius: var(--border-radius-small);
    background-color: var(--text-n10);
    .n-collapse-item-arrow {
      font-size: 16px !important;
    }
    .n-collapse-item__header-main {
      flex: none !important;
    }
    .n-collapse-item__header-extra {
      padding-left: 8px;
      width: 100%;
    }
    .n-collapse-item__content-inner {
      padding-top: 0 !important;
    }
  }
</style>
