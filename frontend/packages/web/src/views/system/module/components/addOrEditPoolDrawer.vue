<template>
  <CrmDrawer
    v-model:show="visible"
    :width="800"
    :title="title"
    :show-continue="!form.id"
    :ok-text="form.id ? t('common.update') : undefined"
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
          path="name"
          :label="
            props.type === ModuleConfigEnum.CLUE_MANAGEMENT ? t('module.clue.name') : t('module.customer.openSeaName')
          "
        >
          <n-input v-model:value="form.name" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
      </div>
      <div class="flex">
        <div class="flex-1">
          <n-form-item path="adminIds" :label="t('opportunity.admin')">
            <CrmUserTagSelector v-model:selected-list="form.adminIds" />
          </n-form-item>
        </div>
        <div class="flex-1">
          <n-form-item path="userIds" :label="t('role.member')">
            <CrmUserTagSelector v-model:selected-list="form.userIds" />
          </n-form-item>
        </div>
      </div>
      <div class="crm-module-form-title"> {{ t('module.clue.clueCollectionRules') }}</div>
      <n-form-item path="pickRule.limitOnNumber" :label="t('module.clue.dailyCollection')">
        <n-radio-group v-model:value="form.pickRule.limitOnNumber" name="radiogroup">
          <n-space>
            <n-radio :value="false">
              {{ t('module.clue.noLimit') }}
            </n-radio>
            <n-radio :value="true">
              {{ t('module.clue.limit') }}
            </n-radio>
          </n-space>
        </n-radio-group>
      </n-form-item>
      <n-form-item
        v-if="form.pickRule.limitOnNumber"
        path="pickRule.pickNumber"
        :label="t('module.clue.limitQuantity')"
      >
        <n-input-number
          v-model:value="form.pickRule.pickNumber"
          class="crm-reminder-advance-input"
          :placeholder="t('common.pleaseInput')"
        />
      </n-form-item>
      <n-form-item path="pickRule.limitPreOwner" :label="t('module.clue.ownerCollection')">
        <n-radio-group v-model:value="form.pickRule.limitPreOwner" name="radiogroup">
          <n-space>
            <n-radio :value="false">
              {{ t('module.clue.noLimit') }}
            </n-radio>
            <n-radio :value="true">
              {{ t('module.clue.limit') }}
            </n-radio>
          </n-space>
        </n-radio-group>
      </n-form-item>
      <n-form-item
        v-if="form.pickRule.limitPreOwner"
        path="pickRule.pickIntervalDays"
        :label="t('module.clue.formerOwner')"
      >
        <n-input-number
          v-model:value="form.pickRule.pickIntervalDays"
          class="crm-reminder-advance-input"
          :placeholder="t('common.pleaseInput')"
        />
        <div class="flex flex-nowrap"> {{ t('module.clue.receiveDay') }}</div>
      </n-form-item>
      <div class="crm-module-form-title"> {{ t('module.clue.clueRecycleRule') }}</div>
      <n-form-item path="auto" :label="t('module.clue.autoRecycle')">
        <n-radio-group v-model:value="form.auto" name="radiogroup">
          <n-space>
            <n-radio :value="true">
              {{ t('common.yes') }}
            </n-radio>
            <n-radio :value="false">
              {{ t('common.no') }}
            </n-radio>
          </n-space>
        </n-radio-group>
      </n-form-item>
      <CrmBatchForm
        v-if="form.auto"
        ref="batchFormRef"
        :models="formItemModel"
        :default-list="form.recycleRule.conditions"
        :add-text="t('module.clue.addConditions')"
        :validate-when-add="true"
        show-all-or
      />
      <n-form-item path="recycleRule.expireNotice" :label="t('opportunity.expirationReminder')">
        <n-radio-group v-model:value="form.recycleRule.expireNotice" name="radiogroup">
          <n-space>
            <n-radio :value="true">
              {{ t('common.yes') }}
            </n-radio>
            <n-radio :value="false">
              {{ t('common.no') }}
            </n-radio>
          </n-space>
        </n-radio-group>
      </n-form-item>
      <n-form-item
        v-if="form.recycleRule.expireNotice"
        path="recycleRule.noticeDays"
        :label="t('module.reminderAdvance')"
      >
        <n-input-number
          v-model:value="form.recycleRule.noticeDays"
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
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { addCustomerPool, addLeadPool, updateCustomerPool, updateLeadPool } from '@/api/modules/system/module';
  import { EQUAL, GE, GT, LE, LT } from '@/config/operator';
  import { useI18n } from '@/hooks/useI18n';

  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import type { LeadPoolForm, LeadPoolItem, LeadPoolParams } from '@lib/shared/models/system/module';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    type: ModuleConfigEnum;
    closeAttrsOptions: SelectOption[];
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const row = defineModel<LeadPoolItem>('row', {
    required: false,
  });

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const rules: FormRules = {
    name: [{ required: true, message: t('common.notNull', { value: `${t('module.clue.name')}` }) }],
    adminIds: [{ required: true, message: t('common.pleaseSelect') }],
    userIds: [{ required: true, message: t('common.pleaseSelect') }],
    [`recycleRule.noticeDays`]: [{ required: true, message: t('common.pleaseInput') }],
    [`pickRule.pickIntervalDays`]: [{ required: true, message: t('common.pleaseInput') }],
    [`pickRule.pickNumber`]: [{ required: true, message: t('common.pleaseInput') }],
  };

  const initForm: LeadPoolForm = {
    name: '',
    adminIds: [],
    userIds: [],
    enable: false,
    auto: false,
    pickRule: {
      limitOnNumber: false,
      pickNumber: undefined,
      limitPreOwner: false,
      pickIntervalDays: undefined,
    },
    recycleRule: {
      expireNotice: false,
      noticeDays: undefined,
      operator: 'all',
      conditions: [],
    },
  };

  const form = ref<LeadPoolForm>(cloneDeep(initForm));

  const title = computed(() => {
    if (props.type === ModuleConfigEnum.CLUE_MANAGEMENT) {
      return !form.value.id ? t('module.clue.addCluePool') : t('module.clue.updateCluePool');
    }
    if (props.type === ModuleConfigEnum.CUSTOMER_MANAGEMENT) {
      return !form.value.id ? t('module.customer.addOpenSea') : t('module.customer.updateOpenSea');
    }
  });

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
        options: props.closeAttrsOptions,
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
        options: [EQUAL, GT, GE, LT, LE],
      },
    },
    {
      path: 'value',
      type: FieldTypeEnum.INPUT,
    },
  ]);

  function cancelHandler() {
    form.value = cloneDeep(initForm);
    row.value = undefined;
    visible.value = false;
  }

  const formRef = ref<FormInst | null>(null);
  const loading = ref<boolean>(false);

  const batchFormRef = ref<InstanceType<typeof CrmBatchForm>>();

  function userFormValidate(cb: (_isContinue: boolean) => Promise<any>, isContinue: boolean) {
    batchFormRef.value?.formValidate(async (batchForm?: Record<string, any>) => {
      try {
        loading.value = true;
        form.value.recycleRule.conditions = batchForm?.list;
        form.value.recycleRule.operator = batchForm?.allOr;
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
      const { userIds, adminIds, ...otherParams } = form.value;
      const params: LeadPoolParams = {
        ...otherParams,
        ownerIds: adminIds.map((e) => e.id),
        scopeIds: userIds.map((e) => e.id),
      };
      if (form.value.id) {
        await (props.type === ModuleConfigEnum.CUSTOMER_MANAGEMENT
          ? updateCustomerPool(params)
          : updateLeadPool(params));
        Message.success(t('common.updateSuccess'));
      } else {
        await (props.type === ModuleConfigEnum.CUSTOMER_MANAGEMENT ? addCustomerPool(params) : addLeadPool(params));
        Message.success(t('common.addSuccess'));
      }
      if (isContinue) {
        form.value = cloneDeep(initForm);
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

  function confirmHandler(isContinue: boolean) {
    formRef.value?.validate(async (error) => {
      if (!error) {
        if (batchFormRef.value) {
          userFormValidate(handleSave, isContinue);
        } else {
          handleSave(isContinue);
        }
      }
    });
  }

  watch(
    () => row.value,
    (val?: LeadPoolItem) => {
      if (val) {
        form.value = {
          id: val.id,
          name: val.name,
          enable: val.enable,
          auto: val.auto,
          pickRule: val.pickRule ?? cloneDeep(initForm).pickRule,
          recycleRule: val.recycleRule ?? cloneDeep(initForm).recycleRule,
          userIds: val.members,
          adminIds: val.owners,
        };
      }
    },
    { deep: true }
  );
</script>

<style scoped lang="less"></style>
