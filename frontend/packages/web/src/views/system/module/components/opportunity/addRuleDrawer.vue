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
          <n-input v-model:value="form.name" type="text" :placeholder="t('common.pleaseInput')" />
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
            path="userId"
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
      <CrmBatchForm
        v-if="form.auto"
        ref="batchFormRef"
        class="mt-[16px]"
        :models="formItemModel"
        :default-list="form.conditions"
        :add-text="t('module.clue.addConditions')"
        :validate-when-add="true"
        show-all-or
      />
      <n-form-item
        require-mark-placement="left"
        label-placement="left"
        class="mt-[16px]"
        path="expireNotice"
        :label="t('opportunity.expirationReminder')"
      >
        <n-radio-group v-model:value="form.expireNotice" name="radiogroup">
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
      <n-form-item
        v-if="form.expireNotice"
        require-mark-placement="left"
        label-placement="left"
        path="noticeDays"
        :label="t('module.reminderAdvance')"
      >
        <n-input-number
          v-model:value="form.noticeDays"
          class="crm-reminder-advance-input"
          :placeholder="t('common.pleaseInput')"
        />
        <div class="flex flex-nowrap"> {{ t('module.reminderDays') }}</div>
      </n-form-item>
    </n-form>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import {
    FormInst,
    FormRules,
    NForm,
    NFormItem,
    NInput,
    NInputNumber,
    NRadio,
    NRadioGroup,
    NSpace,
    SelectOption,
    useMessage,
  } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import type {
    ModuleConditionsItem,
    OpportunityDetail,
    OpportunityItem,
    OpportunityParams,
  } from '@lib/shared/models/system/module';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import { EQUAL, GE, GT, LE, LT, NOT_EQUAL } from '@/components/business/crm-batch-form/config';
  import CrmBatchForm from '@/components/business/crm-batch-form/index.vue';
  import type { FormItemModel } from '@/components/business/crm-batch-form/types';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { addOpportunityRule, updateOpportunityRule } from '@/api/modules/system/module';
  import { useI18n } from '@/hooks/useI18n';

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
    name: [{ required: true, message: t('common.notNull', { value: `${t('org.userName')}` }), trigger: ['blur'] }],
    ownerIds: [{ required: true, message: t('common.pleaseSelect') }],
    scopeIds: [{ required: true, message: t('common.pleaseSelect') }],
    noticeDays: [{ required: true, message: t('common.pleaseInput'), trigger: ['blur'] }],
  };

  const closeAttrsOptions = ref<SelectOption[]>([
    {
      value: 'keepDays',
      label: t('opportunity.belongDays'),
    },
    {
      value: 'remainKeepDays',
      label: t('module.remainingDays'),
    },
    {
      value: 'opportunityStage',
      label: t('opportunity.opportunityStage'),
    },
  ]);

  const formItemModel: Ref<FormItemModel[]> = ref([
    {
      path: 'column',
      type: FieldTypeEnum.SELECT,
      rule: [
        {
          required: true,
          message: t('common.pleaseSelect'),
        },
      ],
      selectProps: {
        options: closeAttrsOptions.value,
      },
    },
    {
      path: 'operator',
      type: FieldTypeEnum.SELECT,
      rule: [
        {
          required: true,
          message: t('common.pleaseSelect'),
        },
      ],
      selectProps: {
        options: [EQUAL, NOT_EQUAL, GT, GE, LT, LE],
      },
    },
    {
      path: 'value',
      type: FieldTypeEnum.INPUT,
    },
  ]);

  const initDefaultItem: ModuleConditionsItem = {
    column: 'keepDays',
    operator: OperatorEnum.EQUALS,
    value: '',
  };

  const initRuleForm: OpportunityDetailType = {
    name: '',
    auto: false,
    enable: true,
    expireNotice: false,
    noticeDays: 0,
    conditions: [initDefaultItem],
    operator: 'AND',
    ownerIds: [],
    scopeIds: [],
  };

  const form = ref<OpportunityDetailType>(cloneDeep(initRuleForm));

  function cancelHandler() {
    form.value = cloneDeep(initRuleForm);
    visible.value = false;
  }

  const formRef = ref<FormInst | null>(null);
  const loading = ref<boolean>(false);

  const batchFormRef = ref<InstanceType<typeof CrmBatchForm>>();

  function userFormValidate(cb: (_isContinue: boolean) => Promise<any>, isContinue: boolean) {
    batchFormRef.value?.formValidate(async (batchForm?: Record<string, any>) => {
      try {
        loading.value = true;
        form.value.conditions = batchForm?.list;
        form.value.operator = batchForm?.allOr;
        await cb(isContinue);
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      } finally {
        loading.value = false;
      }
    });
  }

  async function handleSave(isContinue: boolean) {
    try {
      loading.value = true;
      const { ownerIds, scopeIds, conditions, auto } = form.value;
      const params: OpportunityParams = {
        ...form.value,
        ownerIds: ownerIds.map((e) => e.id),
        scopeIds: scopeIds.map((e) => e.id),
        conditions: auto ? conditions : [],
      };

      if (form.value.id) {
        await updateOpportunityRule(params);
        Message.success(t('common.updateSuccess'));
      } else {
        await addOpportunityRule(params);
        Message.success(t('common.addSuccess'));
      }
      if (isContinue) {
        form.value = cloneDeep(initRuleForm);
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

  function confirmHandler(isContinue: boolean) {
    formRef.value?.validate(async (error) => {
      if (!error) {
        if (form.value.auto) {
          userFormValidate(handleSave, isContinue);
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
      }
    }
  );
</script>

<style scoped lang="less"></style>
