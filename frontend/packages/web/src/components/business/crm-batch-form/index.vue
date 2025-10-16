<template>
  <div class="flex rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
    <div v-if="showAllOr" class="all-or">
      <CrmTag
        type="primary"
        theme="light"
        :color="{ color: 'var(--primary-6)' }"
        class="z-[1] w-[34px]"
        @click="changeAllOr"
      >
        {{ form.allOr === 'AND' ? 'all' : 'or' }}
      </CrmTag>
    </div>
    <div class="flex-1 overflow-hidden">
      <n-form ref="formRef" :model="form">
        <n-scrollbar :style="{ 'max-height': props.maxHeight }">
          <VueDraggable
            v-model="form.list"
            ghost-class="ghost"
            drag-class="drag-item-class"
            :disabled="disabledDraggable"
            :animation="150"
            handle=".handle"
            @move="handleMove"
            @end="(e) => emit('drag', e)"
          >
            <div
              v-for="(element, index) in form.list"
              :key="element.id ?? element._key"
              :class="`${!element.editing ? 'read-only-row' : ''} flex gap-[8px]`"
              :draggable="props.draggable && element.draggable !== false"
            >
              <CrmIcon
                v-if="props.draggable && element.draggable !== false"
                type="iconicon_move"
                :size="12"
                :class="`handle text-[var(--text-n4)] ${disabledDraggable ? 'cursor-not-allowed' : 'cursor-move'}`"
                :style="{ 'margin-top': index === 0 && props.models.some((item) => item.label) ? '40px' : '14px' }"
              />
              <div v-if="element.draggable === false" class="w-[42px]"></div>
              <n-form-item
                v-for="model of props.models"
                :key="`${model.path}${index}`"
                :ref="(el) => handleFormItemRef(el, model.path, index)"
                :label="index === 0 && model.label ? model.label : ''"
                :path="`list[${index}].${model.path}`"
                :rule="
                model.rule?.map((e) => { 
                  if (e.notRepeat) {
                    return {
                    validator: (rule: FormItemRule, value: string) => fieldNotRepeat(value, index, model.path, e.message as string),
                    };
                  }
                  if (typeof e.validator === 'function') {
                    return {
                      validator: (rule, value, callback) =>
                        (e.validator as any)?.(rule, value, callback, element),
                    };
                  }
                  return e;
                })
              "
                class="block flex-1 overflow-hidden py-[4px]"
                :class="model.formItemClass"
                @mousedown.stop
              >
                <template v-if="index === 0 && model.label && model.labelTooltip" #label>
                  <span class="inline-flex items-center gap-[8px]">
                    {{ model.label }}
                    <n-tooltip trigger="hover" placement="right">
                      <template #trigger>
                        <CrmIcon
                          type="iconicon_help_circle"
                          :size="16"
                          class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                        />
                      </template>
                      {{ model.labelTooltip }}
                    </n-tooltip>
                  </span>
                </template>

                <n-input
                  v-if="model.type === FieldTypeEnum.INPUT"
                  v-model:value="element[model.path]"
                  allow-clear
                  :maxlength="255"
                  :placeholder="t('common.pleaseInput')"
                  :disabled="!element.editing"
                  v-bind="model.inputProps"
                />
                <CrmInputNumber
                  v-if="model.type === FieldTypeEnum.INPUT_NUMBER"
                  v-model:value="element[model.path]"
                  class="w-full"
                  clearable
                  :disabled="!element.editing || model.numberProps?.disabledFunction?.(element)"
                  :placeholder="model.numberProps?.placeholder ?? t('common.pleaseInput')"
                  v-bind="{
                    min: model.numberProps?.min,
                    max: model.numberProps?.max,
                    precision: model.numberProps?.precision,
                  }"
                />
                <n-tooltip
                  v-if="[FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(model.type)"
                  :disabled="!model.selectProps?.disabledFunction?.(element)"
                  trigger="hover"
                  placement="bottom"
                >
                  <template #trigger>
                    <n-select
                      v-model:value="element[model.path]"
                      clearable
                      :placeholder="!element.editing ? '-' : t('common.pleaseSelect')"
                      :disabled="!element.editing || model.selectProps?.disabledFunction?.(element)"
                      :show-arrow="element.editing"
                      v-bind="model.selectProps"
                      :options="getSelectOptions(element, model)"
                      :multiple="model.type === FieldTypeEnum.SELECT_MULTIPLE"
                    />
                  </template>
                  {{ model.selectProps?.disabledTooltipFunction?.(element) }}
                </n-tooltip>
                <CrmUserTagSelector
                  v-if="model.type === FieldTypeEnum.USER_TAG_SELECTOR"
                  v-model:selected-list="element[model.path]"
                  :user-error-tag-ids="userErrorTagIds"
                  :disabled="!element.editing"
                  v-bind="model.userTagSelectorProps"
                  @delete-tag="handleUserTagSelectValidate"
                />
              </n-form-item>
              <div
                class="flex gap-[8px]"
                :style="{ 'margin-top': index === 0 && props.models.some((item) => item.label) ? '30px' : '4px' }"
              >
                <template v-if="element.editing">
                  <n-button type="success" ghost class="px-[7px]" @click="handleSaveRow(element, index)">
                    <template #icon>
                      <CrmIcon type="iconicon_check" :size="16" />
                    </template>
                  </n-button>
                  <n-button ghost type="error" class="px-[7px]" @click="handleCancelEdit(index)">
                    <template #icon>
                      <CrmIcon type="iconicon_close" :size="16" />
                    </template>
                  </n-button>
                </template>
                <template v-else>
                  <n-button ghost class="px-[7px]" @click="handleEditRow(index)">
                    <template #icon>
                      <CrmIcon type="iconicon_edit" :size="16" />
                    </template>
                  </n-button>
                  <CrmPopConfirm
                    v-if="props.popConfirmProps"
                    :show="popShow[element.id]"
                    :disabled="!props.popConfirmProps || !element.id"
                    placement="bottom-end"
                    class="w-[260px]"
                    v-bind="getPopConfirmProps(element,index) as CrmPopConfirmProps"
                    @confirm="handlePopDeleteListItem(index, element.id)"
                    @cancel="popShow[element.id] = false"
                  >
                    <n-button
                      v-if="!getPopConfirmProps(element, index)?.disabled"
                      ghost
                      class="px-[7px]"
                      @click.stop="handleDeleteListItem(index, element.id)"
                    >
                      <template #icon>
                        <CrmIcon type="iconicon_delete" :size="16" />
                      </template>
                    </n-button>
                    <span v-else></span>
                  </CrmPopConfirm>
                  <slot v-else-if="$slots.extra" name="extra" :element="element"></slot>
                  <n-button v-else ghost class="px-[7px]" @click="handleDeleteListItem(index, element.id)">
                    <template #icon>
                      <CrmIcon type="iconicon_delete" :size="16" />
                    </template>
                  </n-button>
                </template>
              </div>
            </div>
          </VueDraggable>
        </n-scrollbar>
      </n-form>
      <n-button
        v-if="props.addText"
        type="primary"
        :disabled="props.disabledAdd"
        text
        class="mt-[5px] w-[fit-content]"
        @click="handleAddListItem"
      >
        <template #icon>
          <n-icon><Add /></n-icon>
        </template>
        {{ props.addText }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import {
    FormInst,
    FormItemRule,
    NButton,
    NForm,
    NFormItem,
    NIcon,
    NInput,
    NScrollbar,
    NSelect,
    NTooltip,
  } from 'naive-ui';
  import { Add } from '@vicons/ionicons5';
  import { cloneDeep } from 'lodash-es';
  import { VueDraggable } from 'vue-draggable-plus';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';
  import { scrollIntoView } from '@lib/shared/method/dom';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';
  import CrmPopConfirm, { CrmPopConfirmProps } from '@/components/pure/crm-pop-confirm/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { FormItemModel } from './types';

  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      models: FormItemModel[];
      addText?: string;
      maxHeight?: string;
      defaultList?: any[]; // 当外层是编辑状态时，可传入已填充的数据
      disabledAdd?: boolean; // 是否禁用添加按钮
      validateWhenAdd?: boolean; // 增加一行的时候是否进行校验
      showAllOr?: boolean;
      draggable?: boolean;
      popConfirmProps?: (ele: Record<string, any>, i: number) => CrmPopConfirmProps | CrmPopConfirmProps;
    }>(),
    {
      maxHeight: '100%',
      disabledAdd: false,
      showAllOr: false,
      draggable: false,
    }
  );

  const emit = defineEmits<{
    (e: 'deleteRow', index: number, id: string, done: () => void): void;
    (e: 'saveRow', element: Record<string, any>, done: () => void, index: number): void;
    (e: 'drag', event: any): void;
    (e: 'cancelRow', index: number): void;
  }>();

  const formRef = ref<FormInst | null>(null);
  const form = ref<Record<string, any>>({ list: [], allOr: 'AND' });
  const formItemRefs = ref<Record<string, Map<string, any>>>({});
  const formItem: Record<string, any> = {};
  const popShow = ref<Record<string, boolean>>({});

  const handleFormItemRef = (el: Element | ComponentPublicInstance | null, path: string, index: number) => {
    if (!formItemRefs.value[path]) {
      formItemRefs.value[path] = new Map();
    }
    if (el) {
      formItemRefs.value[path].set(`${index}`, el);
    } else {
      formItemRefs.value[path].delete(`${index}`);
    }
  };

  const userErrorTagIds = ref<string[]>([]); // 对于CrmUserTagSelector列，上下行里重复的id

  function valueIsArray(listItem: FormItemModel) {
    return listItem.selectProps?.multiple || listItem.type === FieldTypeEnum.USER_TAG_SELECTOR;
  }

  // 初始化表单数据
  function initForm() {
    props.models.forEach((e) => {
      if (e.defaultValue) {
        formItem[e.path] = e.defaultValue;
      } else {
        formItem[e.path] = valueIsArray(e) ? [] : null;
      }
    });
    form.value.list = props.defaultList?.length
      ? cloneDeep(props.defaultList).map((item) => ({
          editing: false,
          ...item,
        }))
      : [{ ...formItem, editing: true }];
  }

  watchEffect(() => {
    initForm();
  });

  function changeAllOr() {
    form.value.allOr = form.value.allOr === 'AND' ? 'OR' : 'AND';
  }

  function fieldNotRepeat(value: any[] | string | undefined, index: number, field: string, msg?: string) {
    if (!value || value === '') return;

    const fieldConfig = props.models.find((model) => model.path === field);
    if (!fieldConfig) return;

    const otherItems = form.value.list.filter((_: Record<string, any>, i: number) => i !== index);

    // 非数组类型的重复检查
    if (!valueIsArray(fieldConfig)) {
      if (otherItems.some((item: Record<string, any>) => item[field] === value)) {
        return new Error(t(msg || ''));
      }
      return;
    }

    // USER_TAG_SELECTOR 类型的重复检查
    if (fieldConfig.type === FieldTypeEnum.USER_TAG_SELECTOR) {
      const currentIds = (value as SelectedUsersItem[]).map((item) => item.id);

      const duplicateIds: string[] = otherItems.reduce((duplicates: string[], item: Record<string, any>) => {
        const compareIds = item[field].map((tagItem: any) => tagItem.id);
        const newDuplicates = currentIds.filter((id) => compareIds.includes(id));
        return [...duplicates, ...newDuplicates];
      }, []);

      if (duplicateIds.length > 0) {
        userErrorTagIds.value = [...new Set(duplicateIds)]; // 去重
        return new Error(t(msg || ''));
      }
      userErrorTagIds.value = [];
    }
  }

  // 重新校验所有 USER_TAG_SELECTOR 类型的表单项
  function handleUserTagSelectValidate() {
    const userTagSelectorPath = props.models.find((item) => item.type === FieldTypeEnum.USER_TAG_SELECTOR)?.path ?? '';
    form.value.list.forEach((_: Record<string, any>, index: number) => {
      const userTagSelectItem = formItemRefs.value[userTagSelectorPath].get(`${index}`);
      if (userTagSelectItem) {
        userTagSelectItem.validate();
      }
    });
  }

  // 排除已选择的
  function getSelectOptions(element: Record<string, any>, model: FormItemModel) {
    if (model.selectProps?.filterRepeat) {
      const selectedValues = new Set<string>();

      form.value.list.forEach((item: any) => {
        if (item[model.path] && item !== element) {
          selectedValues.add(item[model.path]);
        }
      });

      const valueField = model.selectProps?.valueField || 'value';
      return (model.selectProps?.options || []).filter((item: any) => !selectedValues.has(item[valueField]));
    }

    return model.selectProps?.options || [];
  }

  function getFormResult() {
    return unref(form.value);
  }

  function getPopConfirmProps(element: Record<string, any>, i: number) {
    return props.popConfirmProps && typeof props.popConfirmProps === 'function'
      ? props.popConfirmProps(element, i)
      : props.popConfirmProps;
  }

  /**
   * 触发表单校验
   * @param cb 校验通过后执行回调
   * @param isSubmit 是否需要将表单值拼接后传入回调函数
   */
  function formValidate(cb: (res?: Record<string, any>) => void, isSubmit = true) {
    formRef.value?.validate(async (errors) => {
      if (errors) {
        scrollIntoView(document.querySelector('.n-form-item-blank--error'), { block: 'center' });
        return;
      }
      if (typeof cb === 'function') {
        if (isSubmit) {
          cb(getFormResult());
        } else {
          cb();
        }
      }
    });
  }

  function handleDelete(i: number, id?: string) {
    if (id) {
      emit('deleteRow', i, id, () => {
        form.value.list.splice(i, 1);
      });
    } else {
      form.value.list.splice(i, 1);
    }
  }

  // 删除一行
  async function handleDeleteListItem(i: number, id?: string) {
    if (id && props.popConfirmProps) {
      popShow.value[id] = true;
    } else {
      handleDelete(i, id);
    }
  }

  function handlePopDeleteListItem(i: number, id?: string) {
    if (props.popConfirmProps) {
      handleDelete(i, id);
    }
  }

  const rowBackups = ref<Record<number, any>>({});
  // 编辑一行
  function handleEditRow(index: number) {
    rowBackups.value[index] = cloneDeep(form.value.list[index]);
    form.value.list[index].editing = true;
  }

  // 单行校验
  async function validateRowFields(index: number): Promise<boolean> {
    const fieldRefs = props.models.map((model) => formItemRefs.value[model.path]?.get(`${index}`)).filter(Boolean);

    const results = await Promise.allSettled(fieldRefs.map((ref) => ref?.validate?.()));

    const hasError = results.some((r) => r.status === 'rejected');
    if (hasError) {
      scrollIntoView(document.querySelector('.n-form-item-blank--error'), { block: 'center' });
      return false;
    }
    return true;
  }

  // 单行还原到未校验的状态
  async function restoreValidationRowFields(index: number) {
    const fieldRefs = props.models.map((model) => formItemRefs.value[model.path]?.get(`${index}`)).filter(Boolean);
    await Promise.allSettled(fieldRefs.map((ref) => ref?.restoreValidation?.()));
  }

  function handleMove(evt: any) {
    if (evt.related.draggable === false) {
      return false;
    }
    return true;
  }

  // 取消编辑
  function handleCancelEdit(index: number) {
    if (rowBackups.value[index]) {
      form.value.list[index] = cloneDeep(rowBackups.value[index]);
      restoreValidationRowFields(index);
      form.value.list[index].editing = false;
      delete rowBackups.value[index];
    } else {
      form.value.list.splice(index, 1);
      emit('cancelRow', index);
    }
  }

  // 保存编辑
  async function handleSaveRow(element: Record<string, any>, index: number) {
    const isValid = await validateRowFields(index);
    formValidate(() => {
      if (!isValid) return;
      emit(
        'saveRow',
        element,
        () => {
          form.value.list[index].editing = false;
          delete rowBackups.value[index];
        },
        index
      );
    }, false);
  }

  const disabledDraggable = computed(() =>
    props.validateWhenAdd ? !props.draggable || form.value.list.some((e: any) => e.editing) : !props.draggable
  );

  // 增加一行
  function handleAddListItem() {
    const item = {
      ...formItem,
      editing: true,
      _key: getGenerateId(),
    };
    if (props.validateWhenAdd) {
      formValidate(() => {
        form.value.list.push(item);
      }, false);
    } else {
      form.value.list.push(item);
    }
  }

  // 初始化
  watch(
    () => form.value.list,
    (newList) => {
      if (props.popConfirmProps) {
        newList.forEach((item: any) => {
          if (item.popConfirmProps) {
            popShow.value[item.id as string] = false;
          }
        });
      }
    },
    { immediate: true }
  );

  defineExpose({
    formValidate,
  });
</script>

<style lang="less" scoped>
  .all-or {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-right: 16px;
    height: auto;
    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 50%;
      width: 1px;
      height: 100%;
      background-color: var(--text-n8);
      transform: translateX(-50%);
    }
    :deep(.n-tag__content) {
      margin: 0 auto;
    }
  }
  :deep(.n-form-item-feedback-wrapper) {
    min-height: 0;
  }
  .read-only-row {
    :deep(.n-form-item) {
      .n-base-selection.n-base-selection--disabled .n-base-selection-label .n-base-selection-input,
      .n-input.n-input--disabled .n-input__input-el,
      .n-input.n-input--disabled .n-input__placeholder,
      .n-base-selection.n-base-selection--disabled .n-base-selection-placeholder {
        color: var(--text-n1);
      }
      .n-tag {
        &.n-tag--disabled {
          opacity: 1;
        }
        .n-tag__close {
          display: none;
        }
      }
    }
  }
  .ghost {
    border: 1px dashed var(--primary-4);
    background-color: var(--primary-7);
    @apply rounded;
  }
  .drag-item-class {
    background: var(--text-n10);
    opacity: 1 !important;
    @apply rounded;
  }
</style>
