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
    >
      <n-form-item path="name" :label="t('crmViewSelect.viewName')">
        <n-input v-model:value="form.name" :maxlength="255" type="text" :placeholder="t('common.pleaseInput')" />
      </n-form-item>
      <FilterContent
        ref="filterContentRef"
        v-model:form-model="formModel as FilterForm"
        keep-one-line
        :config-list="props.configList"
        :custom-list="props.customList"
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

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import { FilterForm, FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    row?: any;
    readonly?: boolean;
    configList: FilterFormItem[];
    customList?: FilterFormItem[]; // 自定义字段
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'refresh'): void;
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

  async function handleSave(isContinue: boolean) {
    try {
      loading.value = true;
      // TODO lmy
      const params = {
        ...form,
      };
      if (form.value?.id) {
        // await updateOpportunityRule(params);
        Message.success(t('common.updateSuccess'));
      } else {
        // await addOpportunityRule(params);
        Message.success(t('common.addSuccess'));
      }
      if (isContinue) {
        form.value = cloneDeep(initForm);
        formModel.value = cloneDeep(defaultFormModel);
      } else {
        cancelHandler();
      }
      emit('refresh');
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
    (val: any) => {
      // TODO lmy
      form.value = { ...val };
    }
  );
</script>
