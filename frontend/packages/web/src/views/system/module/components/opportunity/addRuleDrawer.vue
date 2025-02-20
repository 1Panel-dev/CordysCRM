<template>
  <CrmDrawer
    v-model:show="visible"
    :width="800"
    :title="t('module.businessManage.businessCloseRule')"
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
            <CrmUserTagSelector v-model:selected-list="form.ownerIds" />
          </n-form-item>
        </div>
        <div class="flex-1">
          <n-form-item
            require-mark-placement="left"
            label-placement="left"
            path="userId"
            :label="t('opportunity.members')"
          >
            <CrmUserTagSelector v-model:selected-list="form.scopeIds" />
          </n-form-item>
        </div>
      </div>

      <div class="crm-module-form-title"> {{ t('module.businessManage.businessCloseRule') }}</div>
      <!-- 自动关闭 -->
      <n-form-item
        v-if="!form.id"
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

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmBatchForm from '@/components/business/crm-batch-form/index.vue';
  import type { FormItemModel } from '@/components/business/crm-batch-form/types';
  import { FieldTypeEnum } from '@/components/business/crm-form-create/enum';
  import { SelectedUsersItem } from '@/components/business/crm-select-user-drawer/type';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { addOpportunityRule, updateOpportunityRule } from '@/api/modules/system/module';
  import { EQUAL, GE, GT, LE, LT, NOT_EQUAL } from '@/config/operator';
  import { useI18n } from '@/hooks/useI18n';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import type { ModuleConditionsItem, OpportunityDetail, OpportunityParams } from '@lib/shared/models/system/module';

  const { t } = useI18n();
  const Message = useMessage();

  export type OpportunityDetailType = {
    ownerIds: SelectedUsersItem[];
    scopeIds: SelectedUsersItem[];
  } & OpportunityDetail;

  const props = defineProps<{
    rows?: OpportunityDetailType;
  }>();

  const emit = defineEmits<{
    (e: 'loadList'): void;
    (e: 'cancel'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const rules: FormRules = {
    name: [{ required: true, message: t('common.notNull', { value: `${t('org.userName')}` }) }],
    ownerIds: [{ required: true, message: t('common.pleaseSelect') }],
    scopeIds: [{ required: true, message: t('common.pleaseSelect') }],
    noticeDays: [{ required: true, message: t('common.pleaseInput') }],
  };

  const closeAttrsOptions = ref<SelectOption[]>([
    {
      value: 'belongDays',
      label: t('opportunity.belongDays'),
    },
    {
      value: 'remainingDays',
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
    column: 'belongDays',
    operator: OperatorEnum.EQ,
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
      const { ownerIds, scopeIds } = form.value;
      const params: OpportunityParams = {
        ...form.value,
        ownerIds: ownerIds.map((e) => e.id),
        scopeIds: scopeIds.map((e) => e.id),
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
        userFormValidate(handleSave, isContinue);
      }
    });
  }

  watch(
    () => props.rows,
    (val) => {
      if (val) {
        // TODO 回显待联调
        form.value = cloneDeep(val);
      }
    }
  );
</script>

<style scoped lang="less"></style>
