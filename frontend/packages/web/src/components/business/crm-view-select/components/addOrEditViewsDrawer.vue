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
        v-model:form-model="formModel as FilterForm"
        keep-one-line
        :config-list="props.configList"
        :custom-list="props.customList"
        :readonly="props.readonly"
      />
    </n-form>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, FormRules, NForm, NFormItem, NInput, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ViewItem } from '@lib/shared/models/view';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import { FilterForm, FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import { multipleValueTypeList } from '@/components/business/crm-form-create/config';

  import { TabType } from '@/hooks/useHiddenTab';

  import { viewApiMap } from '../config';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    type: TabType;
    row?: ViewItem;
    readonly?: boolean;
    configList: FilterFormItem[];
    customList?: FilterFormItem[]; // 自定义字段
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'refresh', activeId?: string): void;
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
      },
    ],
  };

  const initForm: { name?: string; id?: string } = {
    name: '',
  };

  const form = ref(cloneDeep(initForm));

  const defaultFormModel: FilterForm = {
    searchMode: 'AND',
    list: [{ dataIndex: null, operator: undefined, value: null, type: FieldTypeEnum.INPUT }],
  };
  const formModel = ref<FilterForm>(cloneDeep(defaultFormModel));

  function cancelHandler() {
    form.value = cloneDeep(initForm);
    formModel.value = cloneDeep(defaultFormModel);
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
    }));

    return {
      searchMode: formModel.value.searchMode,
      conditions,
    };
  }

  async function handleSave(isContinue: boolean) {
    try {
      loading.value = true;
      const params = {
        ...form.value,
        ...getParams(),
      };
      let activeId;
      if (form.value?.id) {
        await viewApiMap.update[props.type](params);
        Message.success(t('common.updateSuccess'));
        activeId = form.value?.id;
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
      emit('refresh', activeId);
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
    () => props.row,
    (val?: ViewItem) => {
      if (val) {
        form.value = { ...val };
        formModel.value = {
          searchMode: val.searchMode,
          list: cloneDeep(val.list),
        };
      }
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
