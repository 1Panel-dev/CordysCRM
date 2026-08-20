<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div class="mb-[8px] flex gap-[8px] px-[24px] pt-[24px]">
      <CrmSearchInput v-model:value="categoryKeyword" :placeholder="t('common.searchByName')" class="flex-1" />
      <n-button
        v-permission="['SYSTEM_SETTING:ADD']"
        type="primary"
        ghost
        class="n-btn-outline-primary p-[8px]"
        @click="addCategoryRow"
      >
        <CrmIcon type="iconicon_add" :size="16" />
      </n-button>
    </div>
    <div class="flex-1 overflow-hidden px-[24px] pb-[24px]">
      <n-spin :show="categoryLoading" class="h-full" content-class="h-full">
        <n-empty
          v-if="filteredCategories.length === 0"
          :description="t('common.noData')"
          :show-icon="false"
          class="flex h-[38px] flex-col items-center justify-center bg-[var(--text-n9)]"
        />
        <CrmList
          v-show="filteredCategories.length > 0"
          ref="categoryListRef"
          v-model:active-item-key="selectedCategoryId"
          v-model:focus-item-key="focusCategoryId"
          :data="filteredCategories"
          class="term-category-list"
          virtual-scroll-height="100%"
          key-field="id"
          :item-more-actions="getCategoryMoreActions"
          item-class="gap-[8px] px-[4px]"
          activeItemClass="bg-[var(--text-n9)]"
          mode="static"
          @item-click="handleCategoryClick"
          @more-action-select="handleCategoryActionSelect"
        >
          <template #title="{ item }">
            <n-form
              v-if="editingCategoryId === item.id"
              ref="categoryFormRef"
              :model="categoryForm"
              class="term-category-edit-form"
              size="small"
              :rules="categoryRules"
            >
              <n-form-item label="" :show-label="false" :show-feedback="false" path="name">
                <n-input
                  ref="inputInstRef"
                  v-model:value="categoryForm.name"
                  :maxlength="255"
                  :placeholder="t('common.pleaseInputToEnter')"
                  :loading="savingCategoryId === item.id"
                  @blur="handleSaveCategory(item)"
                  @keydown="handleCategoryKeyDown(item, $event)"
                  @input="validateCategoryName"
                  @click.stop
                  @compositionstart="handleCompositionStart"
                  @compositionend="handleCompositionEnd"
                >
                  <template #suffix>
                    <CrmClearSuffix
                      :tooltip-content="tooltipContent"
                      :status="validateNameError"
                      @clear="clearCategoryName(item)"
                    />
                  </template>
                </n-input>
              </n-form-item>
            </n-form>
            <n-tooltip v-else trigger="hover">
              <template #trigger>
                <div class="one-line-text" :class="selectedCategoryId === item.id ? 'text-[var(--primary-8)]' : ''">
                  {{ item.name }}
                </div>
              </template>
              {{ item.name }}
            </n-tooltip>
          </template>
          <template #itemRight="{ item }">
            <span v-if="editingCategoryId !== item.id" class="term-category-count text-[var(--text-n4)]">
              {{ item.termCount }}
            </span>
          </template>
        </CrmList>
      </n-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onMounted, ref } from 'vue';
  import {
    FormInst,
    FormItemRule,
    FormRules,
    InputInst,
    NButton,
    NEmpty,
    NForm,
    NFormItem,
    NInput,
    NSpin,
    NTooltip,
    useMessage,
  } from 'naive-ui';
  import { debounce } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';
  import type { TermCategoryItem } from '@lib/shared/models/system/term';

  import type { ClearStatusType } from '@/components/pure/crm-clear-suffix/index.vue';
  import CrmClearSuffix from '@/components/pure/crm-clear-suffix/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';

  import { addTermCategory, deleteTermCategory, getTermCategoryList, updateTermCategory } from '@/api/modules';

  const emit = defineEmits<{
    (e: 'loaded', list: TermCategoryItem[]): void;
    (e: 'deleted'): void;
  }>();

  const selectedCategoryId = defineModel<string>('selectedId', {
    default: '',
  });

  const { t } = useI18n();
  const Message = useMessage();

  const categoryKeyword = ref('');
  const categories = ref<TermCategoryItem[]>([]);
  const categoryLoading = ref(false);
  const focusCategoryId = ref('');

  const filteredCategories = computed(() =>
    categoryKeyword.value
      ? categories.value.filter((category) => category.name.includes(categoryKeyword.value))
      : categories.value
  );

  async function initCategoryList(selectName?: string) {
    try {
      categoryLoading.value = true;
      categories.value = await getTermCategoryList();
      emit('loaded', categories.value);
      if (selectName) {
        // 新增分类后接口返回真实 id，通过名称回选刚新增的分类。
        const createdCategory = categories.value.find((category) => category.name === selectName);
        selectedCategoryId.value = createdCategory?.id || '';
      } else if (!selectedCategoryId.value && categories.value.length) {
        // 首次进入页面时默认选中第一项。
        selectedCategoryId.value = categories.value[0].id;
      } else if (!categories.value.some((category) => category.id === selectedCategoryId.value)) {
        // 当前选中的分类被删除或接口已不返回时，回退到第一项。
        selectedCategoryId.value = categories.value[0]?.id || '';
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      categoryLoading.value = false;
    }
  }

  const categoryMoreActions: ActionsItem[] = [
    {
      key: 'rename',
      label: t('common.rename'),
      permission: ['SYSTEM_SETTING:UPDATE'],
    },
    {
      key: 'delete',
      label: t('common.delete'),
      danger: true,
      permission: ['SYSTEM_SETTING:DELETE'],
    },
  ];

  const editingCategoryId = ref('');
  const savingCategoryId = ref('');
  const categoryForm = ref({ name: '' });
  const validateNameError = ref<ClearStatusType>('default');
  const tooltipContent = ref('');
  const categoryFormRef = ref<FormInst | null>(null);
  const inputInstRef = ref<InputInst | null>(null);
  const categoryListRef = ref<InstanceType<typeof CrmList> | null>(null);

  function getCategoryMoreActions(item: Record<string, any>) {
    return item.isNew || editingCategoryId.value === item.id ? [] : categoryMoreActions;
  }

  const categoryNames = computed(() =>
    categories.value
      .filter((category) => category.id !== editingCategoryId.value && !category.isNew)
      .map((category) => category.name)
  );

  const validateCategoryNameRule = (_rule: FormItemRule, value: string) => {
    if (value.trim().length === 0) {
      tooltipContent.value = t('common.nameNotNull');
      return new Error(t('common.nameNotNull'));
    }

    if (categoryNames.value.includes(value)) {
      tooltipContent.value = t('common.nameExists');
      return new Error(t('common.nameExists'));
    }
    tooltipContent.value = '';
  };

  const categoryRules: FormRules = {
    name: [
      { required: true, message: t('common.nameNotNull'), trigger: ['input'] },
      { validator: validateCategoryNameRule, trigger: ['input'] },
    ],
  };

  function resetValidateStatus() {
    validateNameError.value = 'default';
    tooltipContent.value = '';
  }

  async function createCategory(category: TermCategoryItem) {
    try {
      await addTermCategory({ name: category.name });
      await initCategoryList(category.name);
      editingCategoryId.value = '';
      categoryForm.value.name = '';
      resetValidateStatus();
      return true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      return false;
    }
  }

  async function renameCategory(category: TermCategoryItem) {
    try {
      await updateTermCategory({
        id: category.id,
        name: category.name,
      });
      await initCategoryList();
      editingCategoryId.value = '';
      categoryForm.value.name = '';
      resetValidateStatus();
      return true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      return false;
    }
  }

  function addCategoryRow() {
    const id = getGenerateId();
    categoryKeyword.value = '';
    categories.value.push({
      id,
      name: t('system.business.term.newCategory'),
      termCount: 0,
      isNew: true,
    });
    emit('loaded', categories.value);
    selectedCategoryId.value = id;
    editingCategoryId.value = id;
    categoryForm.value.name = '';
    resetValidateStatus();
    nextTick(() => {
      categoryListRef.value?.scrollTo({ top: Number.MAX_SAFE_INTEGER });
      inputInstRef.value?.focus();
    });
  }

  function handleCategoryClick(category: Record<string, any>) {
    selectedCategoryId.value = category.id;
  }

  function editCategory(category: TermCategoryItem) {
    editingCategoryId.value = category.id;
    categoryForm.value.name = category.name;
    resetValidateStatus();
    nextTick(() => {
      inputInstRef.value?.focus();
    });
  }

  function cancelEditCategory(item: Record<string, any>) {
    if (item.isNew) {
      categories.value = categories.value.filter((categoryItem) => categoryItem.id !== item.id);
      emit('loaded', categories.value);
      selectedCategoryId.value = categories.value[0]?.id || '';
    }
    editingCategoryId.value = '';
    categoryForm.value.name = '';
    resetValidateStatus();
  }

  async function saveCategory(item: Record<string, any>) {
    const category = item as TermCategoryItem;
    if (editingCategoryId.value !== category.id) return;
    if (savingCategoryId.value === category.id) return;
    const name = categoryForm.value.name.trim();
    if (!name) {
      cancelEditCategory(category);
      return;
    }

    const params = {
      ...category,
      name,
    };

    try {
      savingCategoryId.value = category.id;
      if (category.isNew) {
        await createCategory(params);
      } else {
        await renameCategory(params);
      }
    } finally {
      savingCategoryId.value = '';
    }
  }

  const debouncedSaveCategory = debounce((item: Record<string, any>) => {
    saveCategory(item);
  }, 100);

  function validateCategoryName() {
    categoryFormRef.value?.validate((errors) => {
      validateNameError.value = errors ? 'error' : 'default';
    });
  }

  function handleSaveCategory(item: Record<string, any>) {
    categoryFormRef.value?.validate((errors) => {
      if (!errors) {
        validateNameError.value = 'default';
        debouncedSaveCategory(item);
        return;
      }

      validateNameError.value = 'error';
      if (item.isNew && categoryForm.value.name.trim().length === 0) {
        cancelEditCategory(item);
      }
    });
  }

  const isComposing = ref(false);
  function handleCompositionStart() {
    isComposing.value = true;
  }

  function handleCompositionEnd() {
    isComposing.value = false;
  }

  function handleCategoryKeyDown(item: Record<string, any>, event: KeyboardEvent) {
    if (event.key === 'Enter') {
      if (isComposing.value) return;
      event.stopPropagation();
      event.preventDefault();
      handleSaveCategory(item);
    } else if (event.key === 'Escape') {
      event.stopPropagation();
      event.preventDefault();
      cancelEditCategory(item);
    }
  }

  function clearCategoryName(item: Record<string, any>) {
    if (item.isNew && categoryForm.value.name.trim().length === 0) {
      cancelEditCategory(item);
      return;
    }

    categoryForm.value.name = '';
    validateCategoryName();
  }

  async function handleDelete(category: TermCategoryItem) {
    try {
      await deleteTermCategory(category.id);
      Message.success(t('common.deleteSuccess'));
      await initCategoryList();
      emit('deleted');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleCategoryActionSelect(item: ActionsItem, node: Record<string, any>) {
    const category = node as TermCategoryItem;
    switch (item.key) {
      case 'rename':
        editCategory(category);
        focusCategoryId.value = '';
        break;
      case 'delete':
        handleDelete(category);
        break;
      default:
        break;
    }
  }

  onMounted(() => {
    initCategoryList();
  });

  defineExpose({
    initCategoryList,
  });
</script>

<style scoped lang="less">
  :deep(.term-category-list) {
    .crm-list-item:hover,
    .crm-list-item--focus {
      .term-category-count {
        display: none;
      }
    }
    .crm-list-item:has(.term-category-edit-form) > div:last-child {
      display: none;
    }
    .term-category-edit-form {
      width: 100%;
    }
  }
</style>
