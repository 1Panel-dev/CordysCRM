<template>
  <CrmDrawer
    v-model:show="visible"
    :width="800"
    :title="form.id ? t('opportunity.updateRules') : t('opportunity.addRules')"
    :ok-text="form.id ? t('common.update') : t('common.add')"
    :show-continue="!form.id"
    :loading="loading"
    @confirm="confirmHandler(false)"
    @continue="confirmHandler(true)"
    @cancel="cancelHandler"
  >
    <n-alert v-if="form.id" class="mb-[16px]" type="warning">
      {{ t('opportunity.updateRuleContentTip') }}
    </n-alert>
    <n-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-placement="left"
      :label-width="100"
      require-mark-placement="left"
    >
      <div class="crm-module-form-title">{{ t('common.baseInfo') }}</div>
      <div class="w-full">
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="name"
          :label="t('opportunity.ruleName')"
        >
          <n-input v-model:value="form.name" :maxlength="255" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
      </div>
      <div class="flex">
        <div class="flex-1">
          <n-form-item
            require-mark-placement="left"
            label-placement="left"
            path="ownerIds"
            :label="t('opportunity.admin')"
          >
            <CrmUserTagSelector v-model:selected-list="form.ownerIds" :member-types="memberTypes" />
          </n-form-item>
        </div>
        <div class="flex-1">
          <n-form-item
            require-mark-placement="left"
            label-placement="left"
            path="scopeIds"
            :label="t('opportunity.members')"
          >
            <CrmUserTagSelector v-model:selected-list="form.scopeIds" :member-types="memberTypes" />
          </n-form-item>
        </div>
      </div>

      <div class="crm-module-form-title"> {{ t('module.businessManage.businessCloseRule') }}</div>
      <!-- 自动关闭 -->
      <n-form-item
        require-mark-placement="left"
        label-placement="left"
        path="auto"
        :label="t('opportunity.autoClose')"
        :show-feedback="false"
      >
        <n-radio-group v-model:value="form.auto" name="radiogroup">
          <n-space>
            <n-radio key="yes" :value="true">
              {{ t('common.yes') }}
            </n-radio>
            <n-radio key="no" :value="false">
              {{ t('common.no') }}
            </n-radio>
          </n-space>
        </n-radio-group>
      </n-form-item>
      <FilterContent
        v-if="form.auto"
        ref="filterContentRef"
        v-model:form-model="recycleFormItemModel"
        keep-one-line
        :config-list="filterConfigList"
      />
    </n-form>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import {
    FormInst,
    FormRules,
    NAlert,
    NForm,
    NFormItem,
    NInput,
    NRadio,
    NRadioGroup,
    NSpace,
    useMessage,
  } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { OpportunityDetail, OpportunityItem, OpportunityParams } from '@lib/shared/models/system/module';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import { IN, NOT_IN } from '@/components/pure/crm-advance-filter/index';
  import { AccordBelowType, FilterForm, FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { addOpportunityRule, updateOpportunityRule } from '@/api/modules';
  import { lastOpportunitySteps } from '@/config/opportunity';

  const { t } = useI18n();
  const Message = useMessage();

  export type OpportunityDetailType = {
    ownerIds: SelectedUsersItem[];
    scopeIds: SelectedUsersItem[];
  } & OpportunityDetail;

  const props = defineProps<{
    rows?: OpportunityItem;
  }>();

  const emit = defineEmits<{
    (e: 'loadList'): void;
    (e: 'cancel'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const memberTypes: Option[] = [
    {
      label: t('menu.settings.org'),
      value: MemberSelectTypeEnum.ORG,
    },
    {
      label: t('role.role'),
      value: MemberSelectTypeEnum.ROLE,
    },
  ];

  const rules: FormRules = {
    name: [
      { required: true, message: t('common.notNull', { value: `${t('opportunity.ruleName')}` }), trigger: ['blur'] },
    ],
    ownerIds: [{ required: true, message: t('common.pleaseSelect') }],
    scopeIds: [{ required: true, message: t('common.pleaseSelect') }],
  };

  const filterConfigList = computed<FilterFormItem[]>(() => {
    return [
      {
        title: t('common.createTime'),
        dataIndex: 'createTime',
        type: FieldTypeEnum.TIME_RANGE_PICKER,
      },
      {
        title: t('opportunity.opportunityStage'),
        dataIndex: 'opportunityStage',
        type: FieldTypeEnum.SELECT_MULTIPLE,
        operatorOption: [IN, NOT_IN],
        selectProps: {
          options: lastOpportunitySteps,
        },
      },
    ];
  });

  const initRuleForm: OpportunityDetailType = {
    name: '',
    auto: false,
    enable: true,
    conditions: [],
    operator: 'AND',
    ownerIds: [],
    scopeIds: [],
  };

  const form = ref<OpportunityDetailType>(cloneDeep(initRuleForm));

  const defaultFormModel: FilterForm = {
    searchMode: 'AND',
    list: [
      {
        dataIndex: 'createTime',
        type: FieldTypeEnum.TIME_RANGE_PICKER,
        operator: OperatorEnum.DYNAMICS,
        value: '6,month',
      },
    ],
  };

  const recycleFormItemModel = ref<FilterForm>(cloneDeep(defaultFormModel));

  function cancelHandler() {
    form.value = cloneDeep(initRuleForm);
    recycleFormItemModel.value = cloneDeep(defaultFormModel);
    visible.value = false;
  }

  const formRef = ref<FormInst | null>(null);
  const loading = ref<boolean>(false);

  async function handleSave(isContinue: boolean) {
    try {
      loading.value = true;
      const { ownerIds, scopeIds, auto, ...otherParams } = form.value;
      const params: OpportunityParams = {
        ...otherParams,
        auto,
        ownerIds: ownerIds.map((e) => e.id),
        scopeIds: scopeIds.map((e) => e.id),
        operator: recycleFormItemModel.value.searchMode as string,
        conditions: [],
      };
      if (auto) {
        const list = recycleFormItemModel.value.list?.map((item) => ({
          column: item.dataIndex as string,
          operator: item.operator as string,
          value: item.value as string,
          scope: item.scope ?? [],
        }));
        params.conditions = list;
      }

      if (form.value.id) {
        await updateOpportunityRule(params);
        Message.success(t('common.updateSuccess'));
      } else {
        await addOpportunityRule(params);
        Message.success(t('common.addSuccess'));
      }
      if (isContinue) {
        form.value = cloneDeep(initRuleForm);
        recycleFormItemModel.value = cloneDeep(defaultFormModel);
      } else {
        cancelHandler();
      }
      emit('loadList');
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
    () => props.rows,
    (val?: OpportunityItem) => {
      if (val) {
        form.value = {
          ...val,
          ownerIds: val.owners,
          scopeIds: val.members,
          conditions: JSON.parse(val.condition),
        };
        if (val.auto) {
          recycleFormItemModel.value.list = JSON.parse(val.condition)?.map((item: any) => {
            const filterConfigItem = filterConfigList.value.find((listItem) => listItem.dataIndex === item.column);
            return {
              dataIndex: item.column,
              operator: item.operator,
              value: item.value,
              scope: item.scope,
              showScope: filterConfigItem?.showScope,
              type: filterConfigItem?.type,
              ...(item.column === 'opportunityStage'
                ? {
                    selectProps: {
                      options: lastOpportunitySteps,
                    },
                  }
                : {}),
            } as FilterFormItem;
          });
          recycleFormItemModel.value.searchMode = val.operator as AccordBelowType;
        } else {
          recycleFormItemModel.value = cloneDeep(defaultFormModel);
        }
      }
    }
  );
</script>

<style scoped lang="less">
  :deep(.dataIndex-col) {
    width: 100px;
    flex: initial;
  }
</style>
