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
        {{ form.allOr === 'all' ? 'all' : 'or' }}
      </CrmTag>
    </div>
    <div class="flex-1">
      <n-form ref="formRef" :model="form">
        <n-scrollbar :style="{ 'max-height': props.maxHeight }">
          <div v-for="(element, index) in form.list" :key="`${element.path}${index}`" class="flex gap-[8px]">
            <n-form-item
              v-for="model of props.models"
              :key="`${model.path}${index}`"
              :label="index === 0 && model.label ? model.label : ''"
              :path="`list[${index}].${model.path}`"
              :rule="
              model.rule?.map((e) => {
                if (e.notRepeat) {
                  return {
                    validator: (rule: FormItemRule, value: string) => fieldNotRepeat(value,  index, model.path, e.message as string),
                  };
                }
                return e;
              })
            "
              class="block flex-1"
              :class="model.formItemClass"
            >
              <n-input
                v-if="model.type === FieldTypeEnum.INPUT"
                v-model:value="element[model.path]"
                allow-clear
                :max-length="255"
                :placeholder="t('common.pleaseInput')"
                v-bind="model.inputProps"
              />
              <n-input-number
                v-if="model.type === FieldTypeEnum.INPUT_NUMBER"
                v-model:value="element[model.path]"
                class="w-full"
                clearable
                :placeholder="t('common.pleaseInput')"
                v-bind="model.numberProps"
              />
            </n-form-item>
            <n-button
              :disabled="form.list.length === 1"
              ghost
              class="px-[7px]"
              :style="{ 'margin-top': index === 0 ? '26px' : '' }"
              @click="handleDeleteListItem(index)"
            >
              <template #icon>
                <CrmIcon type="iconicon_minus_circle1" :size="16" class="mr-[3px]" />
              </template>
            </n-button>
          </div>
        </n-scrollbar>
      </n-form>
      <n-button
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
  import { FormInst, FormItemRule, NButton, NForm, NFormItem, NIcon, NInput, NInputNumber, NScrollbar } from 'naive-ui';
  import { Add } from '@vicons/ionicons5';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum } from '@/components/pure/crm-form-design/enum';
  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  import { FormItemModel } from './types';
  import { scrollIntoView } from '@lib/shared/method/dom';

  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      models: FormItemModel[];
      addText: string;
      maxHeight?: string;
      defaultList?: any[]; // 当外层是编辑状态时，可传入已填充的数据
      disabledAdd?: boolean; // 是否禁用添加按钮
      validateWhenAdd?: boolean; // 增加一行的时候是否进行校验
      showAllOr?: boolean;
    }>(),
    {
      maxHeight: '100%',
      disabledAdd: false,
      showAllOr: false,
    }
  );

  const formRef = ref<FormInst | null>(null);
  const form = ref<Record<string, any>>({ list: [], allOr: 'all' });
  const formItem: Record<string, any> = {};

  // 初始化表单数据
  function initForm() {
    props.models.forEach((e) => {
      formItem[e.path] = e.defaultValue ?? (e.type === FieldTypeEnum.INPUT_NUMBER ? undefined : '');
    });
    form.value.list = props.defaultList?.length ? cloneDeep(props.defaultList) : [{ ...formItem }];
  }

  watchEffect(() => {
    initForm();
  });

  function changeAllOr() {
    form.value.allOr = form.value.allOr === 'all' ? 'or' : 'all';
  }

  function fieldNotRepeat(value: string | undefined, index: number, field: string, msg?: string) {
    if (value === '' || value === undefined) return;
    // 遍历其他同 path 名的输入框的值，检查是否与当前输入框的值重复
    for (let i = 0; i < form.value.list.length; i++) {
      if (i !== index && form.value.list[i][field].trim() === value) {
        return new Error(t(msg || ''));
      }
    }
  }

  function getFormResult() {
    return unref(form.value);
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

  // 删除一行
  function handleDeleteListItem(i: number) {
    form.value.list.splice(i, 1);
  }

  // 增加一行
  function handleAddListItem() {
    if (props.validateWhenAdd) {
      formValidate(() => {
        form.value.list.push({ ...formItem });
      }, false);
    } else {
      form.value.list.push({ ...formItem });
    }
  }

  defineExpose({
    formValidate,
  });
</script>

<style lang="scss" scoped>
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
  :deep(.n-input__suffix .n-button) {
    display: none;
  }
</style>
