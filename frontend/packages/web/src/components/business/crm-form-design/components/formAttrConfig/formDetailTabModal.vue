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
              :path="`${index}.relatedForm`"
              class="mb-0"
              :rule="[{ validator: () => validateRelatedForm(item), trigger: 'change' }]"
            >
              <n-select
                :value="item.relatedForm?.id"
                :options="relatedFormOptions"
                filterable
                :loading="detailTabOptionsLoading"
                :fallback-option="item.relatedForm ? () => getRelatedFormFallbackOption(item) : false"
                :placeholder="item.internalKey ? '-' : t('common.pleaseSelect')"
                :disabled="!!item.internalKey || !currentFormId || detailTabOptionsLoading"
                @update-value="(value) => handleRelatedFormChange(item, index, value)"
              />
            </n-form-item>
            <n-form-item
              ref="relatedFieldFormItemRefs"
              :path="`${index}.relatedField`"
              class="mb-0"
              :rule="[{ validator: () => validateRelatedField(item), trigger: 'change' }]"
            >
              <n-select
                :value="item.relatedField?.id"
                :options="getRelatedFieldOptions(item)"
                :fallback-option="
                  item.relatedField && detailTabOptionsLoaded ? () => getRelatedFieldFallbackOption(item) : false
                "
                filterable
                :loading="detailTabOptionsLoading"
                :placeholder="item.internalKey ? '-' : t('common.pleaseSelect')"
                :disabled="!!item.internalKey || !item.relatedForm?.id || detailTabOptionsLoading"
                @update-value="(value) => handleRelatedFieldChange(item, value)"
              />
            </n-form-item>
            <div class="flex h-[36px] items-center justify-between">
              <n-switch v-model:value="item.enable" :rubber-band="false" />
              <n-button v-if="!item.internalKey" ghost class="h-[36px] px-[7px]" @click="handleDelete(index)">
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

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';
  import type { FormDetailTabConfig, FormDetailTabOption } from '@lib/shared/models/system/module';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { getFormDetailTabOptions } from '@/api/modules';

  const visible = defineModel<boolean>('visible', { required: true });

  const props = defineProps<{
    tabs?: FormDetailTabConfig[];
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
  const detailTabOptions = ref<FormDetailTabOption[]>([]);
  const detailTabOptionsLoading = ref(false);
  const detailTabOptionsLoaded = ref(false);

  type RelatedFormOption = SelectOption;

  const relatedFormOptions = computed<RelatedFormOption[]>(() => {
    return detailTabOptions.value
      .filter((item) => item.id !== props.currentFormId)
      .map((item) => ({ label: item.name, value: item.id }));
  });

  function resetDraftTabs() {
    draftTabs.value = cloneDeep(props.tabs || []);
  }

  async function initDetailTabOptions() {
    if (!props.currentFormId) {
      detailTabOptions.value = [];
      detailTabOptionsLoaded.value = true;
      return;
    }

    detailTabOptionsLoading.value = true;
    detailTabOptionsLoaded.value = false;
    try {
      detailTabOptions.value = (await getFormDetailTabOptions(props.currentFormId)) || [];
    } catch (error) {
      detailTabOptions.value = [];
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      detailTabOptionsLoading.value = false;
      detailTabOptionsLoaded.value = true;
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

  function getRelatedFormFallbackOption(item: FormDetailTabConfig): SelectOption {
    const relatedForm = item.relatedForm!;
    return getFallbackOption(
      relatedForm.id,
      item.internalKey ? relatedForm.name : t('crmFormDesign.detailTabRelatedFormInvalid')
    );
  }

  function getRelatedFieldFallbackOption(item: FormDetailTabConfig): SelectOption {
    const relatedField = item.relatedField!;
    return getFallbackOption(
      relatedField.id,
      item.internalKey ? relatedField.name : t('crmFormDesign.detailTabRelatedFieldInvalid')
    );
  }

  function getRelatedFieldOptions(item: FormDetailTabConfig) {
    const options =
      detailTabOptions.value
        .find((option) => option.id === item.relatedForm?.id)
        ?.sourceTypeFields.map((field) => ({ label: field.name, value: field.id })) || [];
    const selectedFieldIds = new Set(
      draftTabs.value
        .filter((tab) => tab.id !== item.id && tab.relatedForm?.id === item.relatedForm?.id && tab.relatedField?.id)
        .map((tab) => tab.relatedField!.id)
    );
    const availableOptions = options.filter((option) => !selectedFieldIds.has(option.value as string));
    const currentOption = options.find((option) => option.value === item.relatedField?.id);

    if (currentOption) {
      return availableOptions.some((option) => option.value === currentOption.value)
        ? availableOptions
        : [...availableOptions, currentOption];
    }
    if (item.relatedField) {
      return [...availableOptions, getRelatedFieldFallbackOption(item)];
    }
    return availableOptions;
  }

  function isRelatedFormInvalid(item: FormDetailTabConfig) {
    return !!(
      detailTabOptionsLoaded.value &&
      item.relatedForm?.id &&
      !relatedFormOptions.value.some((option) => option.value === item.relatedForm?.id)
    );
  }

  function isRelatedFieldInvalid(item: FormDetailTabConfig) {
    const sourceTypeFields = detailTabOptions.value.find(
      (option) => option.id === item.relatedForm?.id
    )?.sourceTypeFields;
    return !!(
      detailTabOptionsLoaded.value &&
      item.relatedField?.id &&
      !sourceTypeFields?.some((field) => field.id === item.relatedField?.id)
    );
  }

  function getRelationKey(item: FormDetailTabConfig) {
    return item.relatedForm?.id && item.relatedField?.id ? `${item.relatedForm.id}:${item.relatedField.id}` : '';
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
    if (item.internalKey) {
      return true;
    }
    if (!item.relatedForm?.id) {
      return new Error(t('common.required'));
    }
    if (isRelatedFormInvalid(item)) {
      return new Error(t('crmFormDesign.detailTabRelatedFormInvalid'));
    }
    return true;
  }

  function validateRelatedField(item: FormDetailTabConfig) {
    if (item.internalKey) {
      return true;
    }
    if (!item.relatedField?.id) {
      return new Error(t('common.required'));
    }
    if (detailTabOptionsLoading.value) {
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

  function handleRelatedFormChange(item: FormDetailTabConfig, index: number, value: string | number | null) {
    const relatedFormOption = relatedFormOptions.value.find((option) => option.value === value);
    item.relatedForm = relatedFormOption
      ? { id: String(relatedFormOption.value), name: String(relatedFormOption.label) }
      : undefined;
    item.relatedField = undefined;
    relatedFieldFormItemRefs.value[index]?.restoreValidation();
  }

  function handleRelatedFieldChange(item: FormDetailTabConfig, value: string | number | null) {
    const relatedFieldOption = getRelatedFieldOptions(item).find((option) => option.value === value);
    item.relatedField = relatedFieldOption
      ? { id: String(relatedFieldOption.value), name: String(relatedFieldOption.label) }
      : undefined;
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
    if (normalizedTabs.some((item) => !item.internalKey && (!item.relatedForm?.id || !item.relatedField?.id))) {
      message.error(t('crmFormDesign.detailTabIncomplete'));
      return;
    }
    // 系统历史标签中允许没有关联字段，空关联键不参与“关联表单 + 字段”重复校验。
    const relationKeys = normalizedTabs.map(getRelationKey).filter(Boolean);
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

  watch(
    () => visible.value,
    async (isVisible) => {
      if (isVisible) {
        resetDraftTabs();
        await initDetailTabOptions();
      }
    }
  );
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
