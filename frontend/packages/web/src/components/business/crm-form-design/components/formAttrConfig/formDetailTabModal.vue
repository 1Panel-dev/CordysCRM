<template>
  <CrmModal
    v-model:show="visible"
    :title="t('crmFormDesign.detailTabsSetting')"
    :positive-text="t('common.save')"
    :width="900"
    footer
    @confirm="handleSave"
    @cancel="resetDraftTabs"
  >
    <n-form
      ref="formRef"
      :model="draftTabs"
      label-placement="left"
      :label-width="0"
      class="crm-form-design-detail-tab-modal"
    >
      <div class="flex flex-col gap-[12px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
        <div class="grid grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_68px] gap-[12px] text-[var(--text-n1)]">
          <div><span class="mr-[4px] text-[var(--error-red)]">*</span>{{ t('crmFormDesign.detailTabName') }}</div>
          <div><span class="mr-[4px] text-[var(--error-red)]">*</span>{{ t('crmFormDesign.relatedForm') }}</div>
          <div><span class="mr-[4px] text-[var(--error-red)]">*</span>{{ t('crmFormDesign.relatedField') }}</div>
          <div></div>
        </div>
        <div class="flex flex-col gap-[12px]">
          <div
            v-for="(item, index) in draftTabs"
            :key="item.id"
            class="grid grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_68px] items-start gap-[12px]"
          >
            <n-form-item
              :path="`${index}.name`"
              class="mb-0"
              :rule="[{ validator: () => validateTabName(item), trigger: ['input', 'blur'] }]"
            >
              <n-input v-model:value="item.name" :maxlength="16" :placeholder="t('common.pleaseInput')" />
            </n-form-item>
            <n-form-item
              :path="`${index}.relatedFormId`"
              class="mb-0"
              :rule="[
                { required: true, message: t('common.required'), trigger: 'change' },
                { validator: () => validateRelatedForm(item), trigger: 'change' },
              ]"
            >
              <n-select
                v-model:value="item.relatedFormId"
                :options="relatedFormOptions"
                filterable
                :fallback-option="item.relatedFormId ? getRelatedFormFallbackOption : false"
                :placeholder="t('common.pleaseSelect')"
                @update-value="(value) => handleRelatedFormChange(item, index, value)"
              />
            </n-form-item>
            <n-form-item
              ref="relatedFieldFormItemRefs"
              :path="`${index}.relatedFieldId`"
              class="mb-0"
              :rule="[
                { required: true, message: t('common.required'), trigger: 'change' },
                { validator: () => validateRelatedField(item), trigger: 'change' },
              ]"
            >
              <n-select
                v-model:value="item.relatedFieldId"
                :options="getRelatedFieldOptions(item)"
                :fallback-option="
                  item.relatedFieldId && hasRelatedFieldOptionsLoaded(item) ? getRelatedFieldFallbackOption : false
                "
                filterable
                :loading="isRelatedFieldLoading(item)"
                :placeholder="t('common.pleaseSelect')"
                :disabled="!item.relatedFormId || isRelatedFieldLoading(item)"
              />
            </n-form-item>
            <div class="flex h-[36px] items-center justify-between">
              <n-switch v-model:value="item.enable" :rubber-band="false" />
              <n-button ghost class="h-[36px] px-[7px]" @click="handleDelete(index)">
                <template #icon>
                  <CrmIcon type="iconicon_minus_circle" class="text-[var(--text-n4)]" :size="16" />
                </template>
              </n-button>
            </div>
          </div>
        </div>
        <n-button type="primary" text class="h-[22px] w-fit" @click="handleAdd">
          <template #icon>
            <CrmIcon type="iconicon_add" :size="16" />
          </template>
          {{ t('common.add') }}
        </n-button>
      </div>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import {
    FormInst,
    FormItemInst,
    NButton,
    NForm,
    NFormItem,
    NInput,
    NSelect,
    NSwitch,
    SelectOption,
    useMessage,
  } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';
  import { CustomFormItem } from '@lib/shared/models/customForm';
  import { FormDetailTabConfig, FormDetailTabRelatedFormType } from '@lib/shared/models/system/module';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { fullFormSettingList } from '@/components/business/crm-form-create/config';

  import { getCustomFormOptions, getFormDesignConfig } from '@/api/modules';

  const visible = defineModel<boolean>('visible', { required: true });

  const props = defineProps<{
    tabs?: FormDetailTabConfig[];
    currentFormType: FormDetailTabRelatedFormType;
    currentFormId?: string;
  }>();

  const emit = defineEmits<{
    (e: 'save', value: FormDetailTabConfig[]): void;
  }>();

  const { t } = useI18n();
  const message = useMessage();
  const formRef = ref<FormInst>();
  const relatedFieldFormItemRefs = ref<FormItemInst[]>([]);
  const draftTabs = ref<FormDetailTabConfig[]>([]);
  const customFormOptions = ref<CustomFormItem[]>([]);
  const relatedFieldOptionMap = ref<Record<string, SelectOption[]>>({});
  const relatedFieldLoadingMap = ref<Record<string, boolean>>({});

  type RelatedFormOption = SelectOption & {
    relatedFormType: FormDetailTabRelatedFormType;
  };

  const relatedFormOptions = computed<RelatedFormOption[]>(() => {
    const systemOptions = fullFormSettingList
      .filter((item) => item.formKey && !(props.currentFormType === 'SYSTEM' && item.formKey === props.currentFormId))
      .map((item) => ({
        label: item.label,
        value: item.formKey!,
        relatedFormType: 'SYSTEM' as const,
      }));
    const customOptions = customFormOptions.value
      .filter((item) => !(props.currentFormType === 'CUSTOM' && item.id === props.currentFormId))
      .map((item) => ({
        label: item.name,
        value: item.id,
        relatedFormType: 'CUSTOM' as const,
      }));

    return [...systemOptions, ...customOptions];
  });

  const currentDataSourceType = computed(() => {
    if (props.currentFormType === 'CUSTOM') {
      return props.currentFormId;
    }
    return fullFormSettingList.find((item) => item.formKey === props.currentFormId)?.dataSource;
  });

  function resetDraftTabs() {
    draftTabs.value = cloneDeep(props.tabs || []);
  }

  function resetRelatedFieldOptions() {
    relatedFieldOptionMap.value = {};
    relatedFieldLoadingMap.value = {};
  }

  async function initRelatedFormOptions() {
    try {
      customFormOptions.value = (await getCustomFormOptions()) || [];
    } catch (error) {
      customFormOptions.value = [];
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function scrollToFirstValidationError() {
    document.querySelector('.n-form-item-blank--error')?.scrollIntoView({
      behavior: 'smooth',
    });
  }

  function handleAdd() {
    formRef.value?.validate((errors) => {
      if (errors) {
        scrollToFirstValidationError();
        return;
      }
      draftTabs.value.push({
        id: getGenerateId(),
        origin: 'CUSTOM',
        name: '',
        enable: true,
      });
    });
  }

  function handleDelete(index: number) {
    draftTabs.value.splice(index, 1);
  }

  function getFallbackOption(value: string | number, label: string): SelectOption {
    return {
      label,
      value,
    };
  }

  function getRelatedFormFallbackOption(value: string | number): SelectOption {
    return getFallbackOption(value, t('crmFormDesign.detailTabRelatedFormInvalid'));
  }

  function getRelatedFieldFallbackOption(value: string | number): SelectOption {
    return getFallbackOption(value, t('crmFormDesign.detailTabRelatedFieldInvalid'));
  }

  function getRelatedFieldCacheKey(relatedFormType?: FormDetailTabRelatedFormType, relatedFormId?: string) {
    return relatedFormType && relatedFormId ? `${relatedFormType}:${relatedFormId}` : '';
  }

  function hasRelatedFieldOptionsLoaded(item: FormDetailTabConfig) {
    const cacheKey = getRelatedFieldCacheKey(item.relatedFormType, item.relatedFormId);
    return cacheKey ? relatedFieldOptionMap.value[cacheKey] !== undefined : false;
  }

  function getRelatedFieldOptions(item: FormDetailTabConfig) {
    const cacheKey = getRelatedFieldCacheKey(item.relatedFormType, item.relatedFormId);
    if (!hasRelatedFieldOptionsLoaded(item)) {
      return [];
    }
    const options = cacheKey ? relatedFieldOptionMap.value[cacheKey] || [] : [];
    const selectedFieldIds = new Set(
      draftTabs.value
        .filter(
          (tab) =>
            tab.id !== item.id &&
            tab.relatedFormType === item.relatedFormType &&
            tab.relatedFormId === item.relatedFormId &&
            tab.relatedFieldId
        )
        .map((tab) => tab.relatedFieldId)
    );
    const availableOptions = options.filter((option) => !selectedFieldIds.has(option.value as string));
    const currentOption = options.find((option) => option.value === item.relatedFieldId);

    if (currentOption) {
      return availableOptions.some((option) => option.value === currentOption.value)
        ? availableOptions
        : [...availableOptions, currentOption];
    }
    if (item.relatedFieldId) {
      return [...availableOptions, getRelatedFieldFallbackOption(item.relatedFieldId)];
    }
    return availableOptions;
  }

  function isRelatedFieldLoading(item: FormDetailTabConfig) {
    const cacheKey = getRelatedFieldCacheKey(item.relatedFormType, item.relatedFormId);
    return cacheKey ? !hasRelatedFieldOptionsLoaded(item) || !!relatedFieldLoadingMap.value[cacheKey] : false;
  }

  function isRelatedFormInvalid(item: FormDetailTabConfig) {
    return !!(
      item.relatedFormId &&
      !relatedFormOptions.value.some(
        (option) => option.value === item.relatedFormId && option.relatedFormType === item.relatedFormType
      )
    );
  }

  function isRelatedFieldInvalid(item: FormDetailTabConfig) {
    const cacheKey = getRelatedFieldCacheKey(item.relatedFormType, item.relatedFormId);
    return !!(
      item.relatedFieldId &&
      cacheKey &&
      relatedFieldOptionMap.value[cacheKey] &&
      !relatedFieldOptionMap.value[cacheKey].some((option) => option.value === item.relatedFieldId)
    );
  }

  function getRelationKey(item: FormDetailTabConfig) {
    return item.relatedFormType && item.relatedFormId && item.relatedFieldId
      ? `${item.relatedFormType}:${item.relatedFormId}:${item.relatedFieldId}`
      : '';
  }

  function isRelatedFieldDuplicate(item: FormDetailTabConfig) {
    const relationKey = getRelationKey(item);
    return !!relationKey && draftTabs.value.filter((tab) => getRelationKey(tab) === relationKey).length > 1;
  }

  function validateTabName(item: FormDetailTabConfig) {
    const tabName = item.name.trim();
    if (!tabName) {
      return new Error(t('crmFormDesign.detailTabNameRequired'));
    }
    if (tabName && draftTabs.value.filter((tab) => tab.name.trim() === tabName).length > 1) {
      return new Error(t('crmFormDesign.detailTabNameDuplicate'));
    }
    return true;
  }

  function validateRelatedForm(item: FormDetailTabConfig) {
    if (isRelatedFormInvalid(item)) {
      return new Error(t('crmFormDesign.detailTabRelatedFormInvalid'));
    }
    return true;
  }

  function validateRelatedField(item: FormDetailTabConfig) {
    if (isRelatedFieldLoading(item)) {
      return new Error(t('crmFormDesign.detailTabRelatedFieldLoading'));
    }
    if (isRelatedFieldInvalid(item)) {
      return new Error(t('crmFormDesign.detailTabRelatedFieldInvalid'));
    }
    if (isRelatedFieldDuplicate(item)) {
      return new Error(t('crmFormDesign.detailTabRelationDuplicate'));
    }
    return true;
  }

  async function loadRelatedFieldOptions(relatedFormType: FormDetailTabRelatedFormType, relatedFormId: string) {
    const cacheKey = getRelatedFieldCacheKey(relatedFormType, relatedFormId);
    if (!cacheKey || relatedFieldOptionMap.value[cacheKey] || relatedFieldLoadingMap.value[cacheKey]) {
      return;
    }

    relatedFieldLoadingMap.value[cacheKey] = true;
    try {
      const result = await getFormDesignConfig(relatedFormId);
      relatedFieldOptionMap.value[cacheKey] = result.fields
        .filter(
          (field) =>
            [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(field.type) &&
            field.dataSourceType === currentDataSourceType.value
        )
        .map((field) => ({
          label: field.name,
          value: field.id,
        }));
    } catch (error) {
      relatedFieldOptionMap.value[cacheKey] = [];
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      relatedFieldLoadingMap.value[cacheKey] = false;
    }
  }

  async function initRelatedFieldOptions(item: FormDetailTabConfig) {
    if (!item.relatedFormId) {
      return;
    }
    if (!item.relatedFormType) {
      const relatedFormOption = relatedFormOptions.value.find((option) => option.value === item.relatedFormId);
      item.relatedFormType = relatedFormOption?.relatedFormType;
    }
    if (item.relatedFormType) {
      await loadRelatedFieldOptions(item.relatedFormType, item.relatedFormId);
    }
  }

  async function handleRelatedFormChange(item: FormDetailTabConfig, index: number, value: string | number | null) {
    const relatedFormOption = relatedFormOptions.value.find((option) => option.value === value);
    item.relatedFormType = relatedFormOption?.relatedFormType;
    item.relatedFormId = typeof value === 'string' ? value : undefined;
    item.relatedFieldId = undefined;
    relatedFieldFormItemRefs.value[index]?.restoreValidation();
    if (item.relatedFormType && item.relatedFormId) {
      await loadRelatedFieldOptions(item.relatedFormType, item.relatedFormId);
    }
  }

  function saveTabs() {
    const normalizedTabs = draftTabs.value.map((item) => ({
      ...item,
      name: item.name.trim(),
    }));
    if (normalizedTabs.some((item) => !item.name)) {
      message.error(t('crmFormDesign.detailTabNameRequired'));
      return;
    }
    const tabNames = normalizedTabs.map((item) => item.name);
    if (new Set(tabNames).size !== tabNames.length) {
      message.error(t('crmFormDesign.detailTabNameDuplicate'));
      return;
    }
    if (normalizedTabs.some((item) => !item.relatedFormType || !item.relatedFormId || !item.relatedFieldId)) {
      message.error(t('crmFormDesign.detailTabIncomplete'));
      return;
    }
    const relationKeys = normalizedTabs.map(getRelationKey);
    if (new Set(relationKeys).size !== relationKeys.length) {
      message.error(t('crmFormDesign.detailTabRelationDuplicate'));
      return;
    }

    emit('save', normalizedTabs);
    visible.value = false;
  }

  function handleSave() {
    formRef.value?.validate((errors) => {
      if (errors) {
        scrollToFirstValidationError();
        return;
      }
      saveTabs();
    });
  }

  watch(visible, async (isVisible) => {
    if (isVisible) {
      resetDraftTabs();
      resetRelatedFieldOptions();
      await initRelatedFormOptions();
      await Promise.all(draftTabs.value.map((item) => initRelatedFieldOptions(item)));
    }
  });
</script>

<style lang="less" scoped>
  .crm-form-design-detail-tab-modal {
    :deep(.n-form-item-feedback-wrapper) {
      display: none;
    }
    :deep(.n-form-item-blank--error + .n-form-item-feedback-wrapper) {
      display: inline-block;
    }
  }
</style>
