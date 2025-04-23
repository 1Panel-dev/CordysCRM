<template>
  <n-form
    :label-placement="props.formConfig.labelPos"
    :require-mark-placement="props.formConfig.labelPos === 'left' ? 'left' : 'right'"
    class="crm-form-design--composition"
  >
    <n-scrollbar>
      <VueDraggable
        v-model="list"
        :animation="150"
        ghost-class="crm-form-design--composition-item-ghost"
        group="crmFormDesign"
        class="crm-form-design--composition-drag-wrapper"
        @start="onStart"
      >
        <template v-for="item in list" :key="item.id">
          <div
            v-if="item.show !== false"
            class="crm-form-design--composition-item"
            :class="activeItem?.id === item.id ? 'crm-form-design--composition-item--active' : ''"
            :style="{ width: `${item.fieldWidth * 100}%` }"
            @click="() => handleItemClick(item)"
          >
            <div class="crm-form-design--composition-item-tools">
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
            <component :is="getItemComponent(item.type)" :field-config="item" :path="item.id" />
            <div class="crm-form-design--composition-item-mask"></div>
          </div>
        </template>
        <div
          v-if="list.length === 0"
          class="absolute col-span-4 flex h-full w-full items-center justify-center border border-dashed text-[var(--text-n4)]"
          draggable="false"
        >
          {{ t('crmFormDesign.emptyTip') }}
        </div>
      </VueDraggable>
    </n-scrollbar>
    <!-- <div class="crm-form-design--composition-footer" :class="props.formConfig.optBtnPos">
      <n-button v-if="formConfig.optBtnContent[0].enable" type="primary">
        {{ formConfig.optBtnContent[0].text }}
      </n-button>
      <n-button v-if="formConfig.optBtnContent[1].enable" type="primary" ghost>
        {{ formConfig.optBtnContent[1].text }}
      </n-button>
      <n-button v-if="formConfig.optBtnContent[2].enable" secondary>
        {{ formConfig.optBtnContent[2].text }}
      </n-button>
      <div class="crm-form-design--composition-footer-mask"></div>
    </div> -->
  </n-form>
</template>

<script setup lang="ts">
  import { NForm, NScrollbar, NTooltip } from 'naive-ui';
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
  }>();

  const { t } = useI18n();

  const list = defineModel<FormCreateField[]>('list', {
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
    if (type === FieldTypeEnum.TEXTAREA) {
      return CrmFormCreateComponents.basicComponents.textarea;
    }
    if (type === FieldTypeEnum.INPUT_NUMBER) {
      return CrmFormCreateComponents.basicComponents.inputNumber;
    }
    if (type === FieldTypeEnum.DATE_TIME) {
      return CrmFormCreateComponents.basicComponents.dateTime;
    }
    if (type === FieldTypeEnum.RADIO) {
      return CrmFormCreateComponents.basicComponents.radio;
    }
    if (type === FieldTypeEnum.CHECKBOX) {
      return CrmFormCreateComponents.basicComponents.checkbox;
    }
    if ([FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(type)) {
      return CrmFormCreateComponents.basicComponents.select;
    }
    if (
      [
        FieldTypeEnum.MEMBER,
        FieldTypeEnum.MEMBER_MULTIPLE,
        FieldTypeEnum.DEPARTMENT,
        FieldTypeEnum.DEPARTMENT_MULTIPLE,
      ].includes(type)
    ) {
      return CrmFormCreateComponents.basicComponents.memberSelect;
    }
    if (type === FieldTypeEnum.DIVIDER) {
      return CrmFormCreateComponents.basicComponents.divider;
    }
    if (type === FieldTypeEnum.MULTIPLE_INPUT) {
      return CrmFormCreateComponents.basicComponents.tagInput;
    }
    if (type === FieldTypeEnum.PICTURE) {
      return CrmFormCreateComponents.advancedComponents.upload;
    }
    if (type === FieldTypeEnum.LOCATION) {
      return CrmFormCreateComponents.advancedComponents.location;
    }
    if (type === FieldTypeEnum.PHONE) {
      return CrmFormCreateComponents.advancedComponents.phone;
    }
    if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(type)) {
      return CrmFormCreateComponents.advancedComponents.dataSource;
    }
  }

  function addItem(item: FormCreateField) {
    const res: FormCreateField = {
      ...item,
      id: getGenerateId(),
      name: t(item.name),
    };
    if (
      [FieldTypeEnum.CHECKBOX, FieldTypeEnum.RADIO, FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(
        item.type
      )
    ) {
      res.options = [
        {
          label: t('crmFormDesign.option', { i: 1 }),
          value: getGenerateId(),
        },
        {
          label: t('crmFormDesign.option', { i: 2 }),
          value: getGenerateId(),
        },
        {
          label: t('crmFormDesign.option', { i: 3 }),
          value: getGenerateId(),
        },
      ];
    }

    list.value.push(res);
  }

  function copyItem(item: FormCreateField) {
    const res: FormCreateField = {
      ...item,
      id: getGenerateId(),
      internalKey: undefined,
      businessKey: undefined,
    };
    if (
      [FieldTypeEnum.CHECKBOX, FieldTypeEnum.RADIO, FieldTypeEnum.SELECT].includes(item.type) &&
      item.options?.length === 0
    ) {
      res.options = [
        {
          label: t('crmFormDesign.option', { i: 1 }),
          value: getGenerateId(),
        },
        {
          label: t('crmFormDesign.option', { i: 2 }),
          value: getGenerateId(),
        },
        {
          label: t('crmFormDesign.option', { i: 3 }),
          value: getGenerateId(),
        },
      ];
    }

    list.value.push(cloneDeep(res));
  }

  function deleteItem(item: FormCreateField) {
    list.value = list.value.filter((e) => e.id !== item.id);
    if (activeItem.value?.id === item.id) {
      activeItem.value = null;
    }
  }

  defineExpose({
    addItem,
  });
</script>

<style lang="less">
  .crm-form-design--composition {
    @apply relative flex h-full flex-col;

    padding: 24px 24px 0;
    .crm-form-design--composition-drag-wrapper {
      @apply flex h-full w-full flex-wrap content-start;

      border-radius: var(--border-radius-small);
      .crm-form-design--composition-item {
        @apply relative cursor-move self-start;

        padding: 16px;
        border: 1px solid transparent;
        border-radius: var(--border-radius-small);
        transition: width 0.3s;
        &:hover {
          background-color: var(--text-n9);
          .crm-form-design--composition-item-tools {
            @apply visible;
          }
        }
        .crm-form-design--composition-item-tools {
          @apply invisible absolute z-20 flex items-center;

          top: 16px;
          right: 16px;
          padding: 4px;
          border: 1px solid var(--text-n7);
          border-radius: var(--border-radius-small);
          background-color: var(--text-n10);
          gap: 8px;
        }
        .n-form-item-label {
          margin-bottom: 4px;
          padding-bottom: 0;
        }
        .n-form-item-feedback-wrapper {
          @apply hidden;
        }
        .crm-form-design--composition-item-mask {
          @apply absolute bottom-0 left-0 right-0 top-0 z-10 cursor-move bg-transparent;
        }
      }
      .crm-form-design--composition-item--active {
        border: 1px solid var(--primary-8);
        background-color: var(--primary-7);
        .crm-form-design--composition-item-tools {
          @apply visible;
        }
      }
      .crm-form-design--composition-item-ghost {
        @apply flex items-center;

        padding: 16px;
        width: 100%;
        border: 1px solid var(--primary-8);
        border-radius: var(--border-radius-small);
        background-color: var(--primary-7);
        gap: 8px;
        line-height: 22px;
      }
    }
    // .crm-form-design--composition-footer {
    //   @apply relative flex w-full;

    //   padding: 12px 0;
    //   border-top: 1px solid var(--text-n8);
    //   gap: 8px;
    //   .crm-form-design--composition-footer-mask {
    //     @apply absolute bottom-0 left-0 right-0 top-0 z-10 bg-transparent;
    //   }
    // }
  }
  .crm-form-design--composition-item-tools-tip {
    padding: 0 4px !important;
    font-size: 12px;
    line-height: 20px;
  }
</style>
