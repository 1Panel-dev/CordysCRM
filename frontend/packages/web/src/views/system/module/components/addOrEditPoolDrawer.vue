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
    <n-alert v-if="form.id" class="mb-[16px]" type="warning">
      {{ t('module.clue.updateConfirmContent') }}
    </n-alert>
    <n-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-placement="left"
      :label-width="110"
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
          <n-input v-model:value="form.name" :maxlength="255" type="text" :placeholder="t('common.pleaseInput')" />
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
      <div class="crm-module-form-title">
        {{
          props.type === ModuleConfigEnum.CLUE_MANAGEMENT
            ? t('module.clue.clueCollectionRules')
            : t('module.customer.customerCollectionRules')
        }}
      </div>
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
        <CrmInputNumber
          v-model:value="form.pickRule.pickNumber"
          class="crm-reminder-advance-input"
          :placeholder="t('common.pleaseInput')"
          min="1"
          max="10000"
          :precision="0"
        />
      </n-form-item>
      <n-form-item path="pickRule.limitNew">
        <template #label>
          <div class="flex items-center gap-[8px]">
            {{ t('module.clue.newDataPick') }}
            <n-tooltip trigger="hover" placement="right">
              <template #trigger>
                <CrmIcon
                  type="iconicon_help_circle"
                  :size="16"
                  class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                />
              </template>
              {{
                props.type === ModuleConfigEnum.CLUE_MANAGEMENT
                  ? t('module.clue.newPoolDataTip')
                  : t('module.clue.newOpenSeaDataTip')
              }}
            </n-tooltip>
          </div>
        </template>
        <n-radio-group v-model:value="form.pickRule.limitNew" name="radiogroup">
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
      <n-form-item v-if="form.pickRule.limitNew" path="pickRule.newPickInterval" :label="t('module.clue.newData')">
        <CrmInputNumber
          v-model:value="form.pickRule.newPickInterval"
          class="crm-reminder-advance-input"
          :placeholder="t('common.pleaseInput')"
          min="1"
          max="10000"
          :precision="0"
        />
        <div class="flex flex-nowrap"> {{ t('module.clue.receiveDay') }}</div>
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
        <CrmInputNumber
          v-model:value="form.pickRule.pickIntervalDays"
          class="crm-reminder-advance-input"
          :placeholder="t('common.pleaseInput')"
          min="1"
          max="10000"
          :precision="0"
        />
        <div class="flex flex-nowrap"> {{ t('module.clue.receiveDay') }}</div>
      </n-form-item>
      <div class="crm-module-form-title">
        {{
          props.type === ModuleConfigEnum.CLUE_MANAGEMENT
            ? t('module.clue.clueRecycleRule')
            : t('module.customer.customerRecycleRule')
        }}
      </div>
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
      <FilterContent
        v-if="form.auto"
        ref="filterContentRef"
        v-model:form-model="recycleFormItemModel as FilterForm"
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
    NTooltip,
    useMessage,
  } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    CluePoolForm,
    CluePoolItem,
    CluePoolParams,
    ModuleConditionsItem,
  } from '@lib/shared/models/system/module';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import { AccordBelowType, FilterForm, FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { addCluePool, addCustomerPool, updateCluePool, updateCustomerPool } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    type: ModuleConfigEnum;
    quick?: boolean;
    row?: CluePoolItem;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const rules: FormRules = {
    name: [
      {
        required: true,
        message: t('common.notNull', {
          value: `${
            props.type === ModuleConfigEnum.CLUE_MANAGEMENT ? t('module.clue.name') : t('module.customer.openSeaName')
          }`,
        }),
        trigger: ['input', 'blur'],
      },
    ],
    adminIds: [{ required: true, message: t('common.pleaseSelect') }],
    userIds: [{ required: true, message: t('common.pleaseSelect') }],
    [`pickRule.pickIntervalDays`]: [
      { required: true, type: 'number', message: t('common.pleaseInput'), trigger: ['input', 'blur'] },
    ],
    [`pickRule.pickNumber`]: [
      { required: true, type: 'number', message: t('common.pleaseInput'), trigger: ['input', 'blur'] },
    ],
  };

  const initForm: CluePoolForm = {
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
      limitNew: false,
      newPickInterval: undefined,
    },
    recycleRule: {
      operator: 'all',
      conditions: [],
    },
  };

  const form = ref<CluePoolForm>(cloneDeep(initForm));

  const defaultFormModel: FilterForm = {
    searchMode: 'AND',
    list: [
      {
        dataIndex: 'storageTime',
        type: FieldTypeEnum.TIME_RANGE_PICKER,
        operator: OperatorEnum.DYNAMICS,
        value: '6,month',
        showScope: true,
        scope: ['Created', 'Picked'],
      },
    ],
  };
  const recycleFormItemModel = ref<FilterForm>(cloneDeep(defaultFormModel));

  const title = computed(() => {
    if (props.type === ModuleConfigEnum.CLUE_MANAGEMENT) {
      return !form.value.id ? t('module.clue.addCluePool') : t('module.clue.updateCluePool');
    }
    if (props.type === ModuleConfigEnum.CUSTOMER_MANAGEMENT) {
      return !form.value.id ? t('module.customer.addOpenSea') : t('module.customer.updateOpenSea');
    }
  });

  const filterConfigList = computed<FilterFormItem[]>(() => {
    return [
      {
        title: t('module.clue.storageTime'),
        dataIndex: 'storageTime',
        type: FieldTypeEnum.TIME_RANGE_PICKER,
        showScope: true,
        scope: ['Created', 'Picked'],
      },
      {
        title: t('module.clue.followUpTime'),
        dataIndex: 'followUpTime',
        type: FieldTypeEnum.TIME_RANGE_PICKER,
      },
    ];
  });

  function cancelHandler() {
    form.value = cloneDeep(initForm);
    recycleFormItemModel.value = cloneDeep(defaultFormModel);
    visible.value = false;
  }

  const formRef = ref<FormInst | null>(null);
  const loading = ref<boolean>(false);

  async function handleSave(isContinue: boolean) {
    try {
      loading.value = true;
      const { userIds, auto, adminIds, ...otherParams } = form.value;

      const params: CluePoolParams = {
        ...otherParams,
        ownerIds: adminIds.map((e) => e.id),
        scopeIds: userIds.map((e) => e.id),
        auto,
        recycleRule: {
          operator: recycleFormItemModel.value.searchMode as string,
          conditions: [],
        },
      };
      if (auto) {
        const conditions: ModuleConditionsItem[] = [];
        recycleFormItemModel.value.list?.forEach((item) => {
          conditions.push({
            column: item.dataIndex || '',
            operator: item.operator || '',
            value: item.value,
            scope: item.scope,
          });
        });
        params.recycleRule.conditions = form.value.auto ? conditions : [];
      }
      if (form.value.id) {
        await (props.type === ModuleConfigEnum.CUSTOMER_MANAGEMENT
          ? updateCustomerPool(params, props.quick)
          : updateCluePool(params, props.quick));
        Message.success(t('common.updateSuccess'));
      } else {
        await (props.type === ModuleConfigEnum.CUSTOMER_MANAGEMENT ? addCustomerPool(params) : addCluePool(params));
        Message.success(t('common.addSuccess'));
      }
      if (isContinue) {
        form.value = cloneDeep(initForm);
        recycleFormItemModel.value = cloneDeep(defaultFormModel);
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
    (val?: CluePoolItem) => {
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
        if (val.auto) {
          recycleFormItemModel.value = {
            list: val.recycleRule.conditions?.map((item) => ({
              dataIndex: item.column,
              operator: item.operator,
              showScope: !!item.scope?.length,
              value: item.value,
              scope: item.scope,
              type: FieldTypeEnum.TIME_RANGE_PICKER,
            })) as FilterFormItem[],
            searchMode: val.recycleRule.operator as AccordBelowType,
          };
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
