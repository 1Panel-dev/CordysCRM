<template>
  <n-form-item :label="fieldConfig.name" :show-label="fieldConfig.showLabel" :path="props.path">
    <div v-if="fieldConfig.description" class="crm-form-create-item-desc" v-html="fieldConfig.description"></div>
    <div class="flex w-full border border-[var(--text-n8)]">
      <span class="inline-flex h-[100px] w-[40px] flex-col items-center">
        <div class="h-[42.5px]"></div>
        <n-divider class="!my-0" />
        <div class="px-[16px] py-[12px]">1</div>
      </span>
      <VueDraggable
        v-if="fieldConfig.subFields"
        v-model="fieldConfig.subFields"
        :animation="150"
        ghost-class="crm-form-design--composition-item-ghost"
        :group="{
          name: 'crmFormDesignSubTable',
          put(to, from) {
            return typeof from.options.group !== 'string' ? from.options.group?.name === 'crmFormDesign' : false;
          },
        }"
        class="crm-form-design--composition-drag-wrapper crm-form-design-subtable-wrapper"
        @start="onStart"
      >
        <template v-for="(item, index) in fieldConfig.subFields" :key="item.id">
          <div
            v-if="item.show !== false"
            :id="item.id"
            class="crm-form-design--composition-item--subtable"
            :class="[
              activeItem?.id === item.id ? 'crm-form-design--composition-item--subtable--active' : '',
              fieldConfig.fixedColumn && fieldConfig.fixedColumn >= index + 1
                ? 'crm-form-design--composition-item--subtable--shadow !sticky z-10'
                : '',
            ]"
            :style="{
              left: fieldConfig.fixedColumn && fieldConfig.fixedColumn >= index + 1 ? `${index * 160}px` : '',
              boxShadow:
                fieldConfig.fixedColumn && fieldConfig.fixedColumn >= index + 1
                  ? '3px 0 6px -3px rgba(0,0,0,0.12)'
                  : '',
            }"
            @click.stop="() => handleItemClick(item)"
          >
            <div class="crm-form-design--composition-item-tools--subtable">
              <n-tooltip :delay="300" :show-arrow="false" class="crm-form-design--composition-item-tools-tip">
                <template #trigger>
                  <CrmIcon
                    type="iconicon_file_copy"
                    :size="16"
                    class="cursor-pointer hover:text-[var(--primary-8)]"
                    @click.stop="copyItem(item)"
                  />
                </template>
                {{ t('common.copy') }}
              </n-tooltip>
              <n-tooltip
                v-if="!item.businessKey"
                :delay="300"
                :show-arrow="false"
                class="crm-form-design--composition-item-tools-tip"
              >
                <template #trigger>
                  <CrmIcon
                    type="iconicon_delete"
                    :size="16"
                    class="cursor-pointer hover:text-[var(--error-red)]"
                    @click.stop="deleteItem(item)"
                  />
                </template>
                {{ t('common.delete') }}
              </n-tooltip>
            </div>
            <div v-if="props.formConfig.labelPos === 'left'" class="h-[30px]"></div>
            <component :is="getItemComponent(item.type)" :field-config="item" :path="item.id" isSubTableField />
            <div class="crm-form-design--composition-item-mask"></div>
          </div>
        </template>
      </VueDraggable>
    </div>
    <div
      v-if="fieldConfig.sumColumns?.length"
      class="flex w-full items-center border border-t-0 border-[var(--text-n8)]"
    >
      <div class="flex items-center px-[16px] py-[12px]">
        {{ t('crmFormDesign.sum') }}
      </div>
    </div>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDivider, NFormItem, NTooltip } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';
  import { VueDraggable } from 'vue-draggable-plus';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';
  import { FormConfig } from '@lib/shared/models/system/module';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmFormCreateComponents from '@/components/business/crm-form-create/components';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  const props = defineProps<{
    formConfig: FormConfig;
    path: string;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string): void;
  }>();

  const { t } = useI18n();

  const fieldConfig = defineModel<FormCreateField>('fieldConfig', {
    required: true,
  });
  const activeItem = defineModel<FormCreateField | null>('field', {
    default: null,
  });

  function onStart(e: any) {
    activeItem.value = e.data as FormCreateField;
  }

  function handleItemClick(item: FormCreateField) {
    activeItem.value = item;
  }

  function getItemComponent(type: FieldTypeEnum) {
    if (type === FieldTypeEnum.INPUT) {
      return CrmFormCreateComponents.basicComponents.singleText;
    }
    if (type === FieldTypeEnum.SELECT || type === FieldTypeEnum.SELECT_MULTIPLE) {
      return CrmFormCreateComponents.basicComponents.select;
    }
    if (type === FieldTypeEnum.INPUT_NUMBER) {
      return CrmFormCreateComponents.basicComponents.inputNumber;
    }
    if (type === FieldTypeEnum.FORMULA) {
      return CrmFormCreateComponents.advancedComponents.formula;
    }
    if (type === FieldTypeEnum.DATA_SOURCE) {
      return CrmFormCreateComponents.advancedComponents.dataSource;
    }
  }

  function copyItem(item: FormCreateField) {
    const res: FormCreateField = {
      ...item,
      id: getGenerateId(),
      internalKey: undefined,
      businessKey: undefined,
    };
    // if (
    //   [FieldTypeEnum.CHECKBOX, FieldTypeEnum.RADIO, FieldTypeEnum.SELECT].includes(item.type) &&
    //   item.options?.length === 0
    // ) {
    //   res.options = [
    //     {
    //       label: t('crmFormDesign.option', { i: 1 }),
    //       value: getGenerateId(),
    //     },
    //     {
    //       label: t('crmFormDesign.option', { i: 2 }),
    //       value: getGenerateId(),
    //     },
    //     {
    //       label: t('crmFormDesign.option', { i: 3 }),
    //       value: getGenerateId(),
    //     },
    //   ];
    // }
    if (!fieldConfig.value.subFields) {
      fieldConfig.value.subFields = [];
    }
    fieldConfig.value.subFields.push(cloneDeep(res));
  }

  function deleteItem(item: FormCreateField) {
    fieldConfig.value.subFields = fieldConfig.value.subFields?.filter((e) => e.id !== item.id);
    if (activeItem.value?.id === item.id) {
      activeItem.value = null;
    }
  }
</script>

<style lang="less">
  .crm-form-design--composition-item--active {
    .crm-form-design--composition-item--subtable {
      background-color: var(--primary-7) !important;
    }
  }
  .crm-form-design--composition-item:hover {
    .crm-form-design--composition-item--subtable {
      background-color: var(--text-n9) !important;
    }
  }
  .crm-form-design-subtable-wrapper {
    .crm-scroll-bar();
    @apply w-full !flex-nowrap overflow-x-auto overflow-y-hidden;

    height: 100px !important;
    .crm-form-design--composition-item--subtable {
      @apply relative;

      padding: 0 !important;
      flex: 0 0 160px;
      width: 160px !important;
      height: 94px !important;
      border: 1px solid transparent;
      background-color: var(--text-n10);
      .crm-form-design--composition-item-tools--subtable {
        @apply invisible absolute z-20 flex items-center;

        top: 8px !important;
        right: 4px !important;
        padding: 3px 4px;
        border: 1px solid var(--text-n7);
        border-radius: var(--border-radius-small);
        background-color: var(--text-n10);
        gap: 8px;
      }
      .n-form-item {
        height: 96px;
        .n-form-item-label {
          margin-bottom: 0 !important;
          padding: 8px 4px;
          padding: 12px 4px !important;
        }
        .n-form-item-blank {
          height: 46px;
          .n-input,
          .n-select {
            margin: 8px 4px;
          }
        }
      }
      &:not(.crm-form-design--composition-item--active):hover {
        border: 1px dashed var(--primary-8) !important;
      }
    }
    .crm-form-design--composition-item--subtable--active {
      border: 1px solid var(--primary-8);
      background-color: var(--primary-7);
      .crm-form-design--composition-item-tools--subtable {
        @apply visible;
      }
    }
    .crm-form-design--composition-item--subtable:hover > .crm-form-design--composition-item-tools--subtable {
      @apply visible;
    }
    .crm-form-design--composition-item-ghost {
      width: 160px !important;
      flex: 0 0 160px;
      height: 94px !important;
    }
    .crm-form-design--composition-item--subtable--shadow::after {
      position: absolute;
      top: 0;
      right: -3px; /* 在元素右侧外露 3px */
      bottom: 0;
      width: 3px; /* 精确 3px 宽度 */
      background: linear-gradient(to right, rgb(0 0 0 / 12%), rgb(0 0 0 / 0%));
      content: '';
      pointer-events: none;
    }
    .n-input {
      width: 150px;
    }
  }
</style>
