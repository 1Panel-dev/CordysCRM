<template>
  <CrmDrawer
    v-model:show="visible"
    :width="800"
    :title="title"
    :show-continue="!form.id"
    :ok-text="form.id ? t('common.update') : undefined"
    :loading="loading"
    :footer="!props.readonly"
    @confirm="confirmHandler(false)"
    @continue="confirmHandler(true)"
    @cancel="cancelHandler"
  >
    <n-scrollbar>
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="left"
        :label-width="100"
        require-mark-placement="left"
        :class="props.readonly ? 'read-only' : ''"
      >
        <n-form-item path="name" :label="t('crmViewSelect.viewName')">
          <n-input
            v-model:value="form.name"
            :disabled="props.readonly"
            :maxlength="255"
            type="text"
            :placeholder="t('common.pleaseInput')"
          />
        </n-form-item>
        <FilterContent
          ref="filterContentRef"
          :key="filterContentKey"
          v-model:form-model="formModel as FilterForm"
          keep-one-line
          no-filter-option
          :config-list="props.configList"
          :custom-list="props.customList"
          :readonly="props.readonly"
        />
      </n-form>
    </n-scrollbar>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, FormRules, NForm, NFormItem, NInput, NScrollbar, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getCopiedName } from '@lib/shared/method';
  import type { CustomFormUserViewDetail } from '@lib/shared/models/customForm';
  import type { ViewItem } from '@lib/shared/models/view';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import { FilterForm, FilterFormItem, filterOptionKeyMap } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import { multipleValueTypeList } from '@/components/business/crm-form-create/config';

  import { TabType } from '@/hooks/useHiddenTab';

  import { viewApiMap } from '../config';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    type: TabType;
    customFormId?: string;
    row?: ViewItem;
    readonly?: boolean;
    copy?: boolean;
    configList: FilterFormItem[];
    customList?: FilterFormItem[]; // 自定义字段
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'refresh', activeId: string, refreshTable: boolean): void;
  }>();

  const title = computed(() => {
    if (props.readonly) {
      return t('crmViewSelect.readOnly');
    }
    return `${!props.row?.id ? t('common.newCreate') : t('common.update')}${t('crmViewSelect.view')}`;
  });

  const rules: FormRules = {
    name: [
      {
        required: true,
        message: t('common.notNull', {
          value: t('crmViewSelect.viewName'),
        }),
        trigger: ['blur'],
      },
    ],
  };

  const initForm: { name: string; id?: string } = {
    name: '',
  };

  const form = ref(cloneDeep(initForm));

  const defaultFormModel: FilterForm = {
    searchMode: 'AND',
    list: [{ dataIndex: null, operator: undefined, value: null, type: FieldTypeEnum.INPUT }],
  };
  const formModel = ref<FilterForm>(cloneDeep(defaultFormModel));
  const filterContentKey = ref(0);

  function cancelHandler() {
    form.value = cloneDeep(initForm);
    formModel.value = cloneDeep(defaultFormModel);
    filterContentKey.value += 1;
    visible.value = false;
  }

  const formRef = ref<FormInst | null>(null);
  const loading = ref<boolean>(false);

  function getParams() {
    const conditions = formModel.value.list.map((item: any) => ({
      value: item.value,
      operator: item.operator,
      name: item.dataIndex ?? '',
      multipleValue: multipleValueTypeList.includes(item.type),
      type: item.type,
      containChildIds: item.containChildIds || [],
    }));

    return {
      searchMode: formModel.value.searchMode,
      conditions,
    };
  }

  function toFilterForm(detail: CustomFormUserViewDetail): FilterForm {
    const configMap = new Map([...props.configList, ...(props.customList || [])].map((item) => [item.dataIndex, item]));

    return {
      searchMode: detail.searchMode || 'AND',
      list: (detail.conditions || []).map((condition) => {
        const key = filterOptionKeyMap[condition.type as FieldTypeEnum];
        const options = detail.optionMap?.[condition.name] || [];
        return {
          ...configMap.get(condition.name),
          ...condition,
          dataIndex: condition.name,
          ...(key
            ? {
                [key]: options.filter((option) =>
                  Array.isArray(condition.value) ? condition.value.includes(option.id) : option.id === condition.value
                ),
              }
            : {}),
        } as FilterFormItem;
      }),
    };
  }

  async function handleSave(isContinue: boolean) {
    try {
      loading.value = true;
      const params = {
        ...form.value,
        ...getParams(),
      };
      let activeId: string | undefined;
      const isEdit = !!form.value?.id;
      if (props.customFormId) {
        if (isEdit) {
          await viewApiMap.update[FormDesignKeyEnum.CUSTOM_FORM](props.customFormId, params);
          activeId = form.value.id;
        } else {
          const res = await viewApiMap.add[FormDesignKeyEnum.CUSTOM_FORM](props.customFormId, params);
          activeId = res.id;
        }
        Message.success(t(isEdit ? 'common.updateSuccess' : 'common.addSuccess'));
      } else if (isEdit) {
        await viewApiMap.update[props.type](params);
        Message.success(t('common.updateSuccess'));
        activeId = form.value.id;
      } else {
        const res = await viewApiMap.add[props.type](params);
        Message.success(t('common.addSuccess'));
        activeId = res.id;
      }
      if (isContinue) {
        form.value = cloneDeep(initForm);
        formModel.value = cloneDeep(defaultFormModel);
      } else {
        cancelHandler();
      }
      if (activeId) emit('refresh', activeId, props.customFormId ? true : isEdit);
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    } finally {
      loading.value = false;
    }
  }

  const filterContentRef = ref<InstanceType<typeof FilterContent>>();
  function confirmHandler(isContinue: boolean) {
    formRef.value?.validate(async (error) => {
      if (!error) {
        if (filterContentRef.value) {
          filterContentRef.value?.formRef?.validate((errors) => {
            if (!errors) {
              handleSave(isContinue);
            }
          });
        } else {
          handleSave(isContinue);
        }
      }
    });
  }

  watch(
    () => [visible.value, props.row] as const,
    async ([isVisible, row]) => {
      if (!isVisible) return;
      form.value = cloneDeep(initForm);
      formModel.value = cloneDeep(defaultFormModel);
      if (!row) return;

      if (props.customFormId) {
        // 自定义表单的系统视图只在本地维护、没有后端视图记录，不能请求详情接口
        if (row.type === 'internal') {
          form.value = { name: props.copy ? getCopiedName(row.name) : row.name };
          formModel.value = {
            searchMode: row.searchMode,
            list: cloneDeep(row.list),
          };
          return;
        }

        try {
          loading.value = true;
          const detail = await viewApiMap.detail[FormDesignKeyEnum.CUSTOM_FORM](props.customFormId, row.id);
          form.value = props.copy ? { name: getCopiedName(detail.name) } : { id: detail.id, name: detail.name };
          formModel.value = toFilterForm(detail);
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
          visible.value = false;
        } finally {
          loading.value = false;
        }
        return;
      }

      form.value = { id: props.copy ? undefined : row.id, name: row.name };
      formModel.value = {
        searchMode: row.searchMode,
        list: cloneDeep(row.list),
      };
    }
  );
</script>

<style lang="less" scoped>
  .read-only {
    :deep(.n-form-item) {
      .n-base-selection.n-base-selection--disabled .n-base-selection-label .n-base-selection-input,
      .n-input.n-input--disabled .n-input__input-el,
      .n-input.n-input--disabled .n-input__placeholder,
      .n-base-selection.n-base-selection--disabled .n-base-selection-placeholder,
      .n-base-selection-overlay__wrapper {
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
</style>

<style lang="less">
  .n-scrollbar-rail--vertical {
    @apply !right-0;
  }
</style>
