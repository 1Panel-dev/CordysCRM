<template>
  <CrmDrawer
    v-model:show="visible"
    :width="800"
    :title="t('module.clue.addCluePool')"
    :show-continue="true"
    :loading="loading"
    @confirm="confirmHandler(false)"
    @continue="confirmHandler(true)"
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
        <n-form-item path="name" :label="t('module.clue.name')">
          <n-input v-model:value="form.name" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
      </div>
      <div class="flex">
        <div class="flex-1">
          <n-form-item path="adminId" :label="t('opportunity.admin')">
            <CrmUserTagSelector v-model:selected-list="form.adminId" />
          </n-form-item>
        </div>
        <div class="flex-1">
          <n-form-item path="userId" :label="t('role.member')">
            <CrmUserTagSelector v-model:selected-list="form.userId" />
          </n-form-item>
        </div>
      </div>
      <div class="crm-module-form-title"> {{ t('module.clue.clueCollectionRules') }}</div>
      <n-form-item path="dailyCollection" :label="t('module.clue.dailyCollection')">
        <n-radio-group v-model:value="form.dailyCollection" name="radiogroup">
          <n-space>
            <n-radio value="noLimit">
              {{ t('module.clue.noLimit') }}
            </n-radio>
            <n-radio value="limit">
              {{ t('module.clue.limit') }}
            </n-radio>
          </n-space>
        </n-radio-group>
      </n-form-item>
      <n-form-item v-if="form.dailyCollection === 'limit'" path="limitQuantity" :label="t('module.clue.limitQuantity')">
        <n-input-number
          v-model:value="form.limitQuantity"
          class="crm-reminder-advance-input"
          :placeholder="t('common.pleaseInput')"
        />
      </n-form-item>
      <n-form-item path="ownerCollection" :label="t('module.clue.ownerCollection')">
        <n-radio-group v-model:value="form.ownerCollection" name="radiogroup">
          <n-space>
            <n-radio value="noLimit">
              {{ t('module.clue.noLimit') }}
            </n-radio>
            <n-radio value="limit">
              {{ t('module.clue.limit') }}
            </n-radio>
          </n-space>
        </n-radio-group>
      </n-form-item>
      <n-form-item v-if="form.ownerCollection === 'limit'" path="formerOwner" :label="t('module.clue.formerOwner')">
        <n-input-number
          v-model:value="form.formerOwner"
          class="crm-reminder-advance-input"
          :placeholder="t('common.pleaseInput')"
        />
        <div class="flex flex-nowrap"> {{ t('module.clue.receiveDay') }}</div>
      </n-form-item>
      <div class="crm-module-form-title"> {{ t('module.clue.clueRecycleRule') }}</div>
      <n-form-item path="autoRecycle" :label="t('module.clue.autoRecycle')">
        <n-radio-group v-model:value="form.autoRecycle" name="radiogroup">
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
        v-if="form.autoRecycle"
        ref="batchFormRef"
        :models="formItemModel"
        :default-list="form.list"
        :add-text="t('module.clue.addConditions')"
        show-all-or
      ></CrmBatchForm>
      <n-form-item path="expirationReminder" :label="t('opportunity.expirationReminder')">
        <n-radio-group v-model:value="form.expirationReminder" name="radiogroup">
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
      <n-form-item v-if="form.expirationReminder" path="reminderAdvance" :label="t('module.reminderAdvance')">
        <n-input-number
          v-model:value="form.reminderAdvance"
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
    useMessage,
  } from 'naive-ui';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmBatchForm from '@/components/business/crm-batch-form/index.vue';
  import type { FormItemModel } from '@/components/business/crm-batch-form/types';
  import { FieldTypeEnum } from '@/components/business/crm-form-create/enum';
  import { SelectedUsersItem } from '@/components/business/crm-select-user-drawer/type';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();
  const Message = useMessage();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const rules: FormRules = {
    name: [{ required: true, message: t('common.notNull', { value: `${t('module.clue.name')}` }) }],
    adminId: [{ required: true, message: t('common.pleaseSelect') }],
    userId: [{ required: true, message: t('common.pleaseSelect') }],
    reminderAdvance: [{ required: true, message: t('common.pleaseInput') }],
    formerOwner: [{ required: true, message: t('common.pleaseInput') }],
    limitQuantity: [{ required: true, message: t('common.pleaseInput') }],
  };

  // TODO 类型和字段
  const form = ref<{
    adminId: SelectedUsersItem[];
    userId: SelectedUsersItem[];
    [key: string]: any;
  }>({
    id: '',
    name: '',
    adminId: [],
    userId: [],
    ownerCollection: 'noLimit',
    dailyCollection: 'noLimit',
    autoRecycle: false,
    expirationReminder: false,
    reminderAdvance: 1,
    formerOwner: 2,
    limitQuantity: 2,
    list: [],
  });

  // TODO 待补充
  const formItemModel: Ref<FormItemModel[]> = ref([
    {
      path: 'member',
      type: FieldTypeEnum.SELECT,
      selectProps: {
        options: [],
      },
    },
    {
      path: 'operator',
      type: FieldTypeEnum.SELECT,
      selectProps: {
        options: [],
      },
    },
    {
      path: 'value',
      type: FieldTypeEnum.INPUT,
    },
  ]);

  function cancelHandler() {
    visible.value = false;
  }

  const formRef = ref<FormInst | null>(null);
  const loading = ref<boolean>(false);
  function confirmHandler(isContinue: boolean) {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          // TODO 联调
          if (form.value.id) {
            Message.success(t('common.updateSuccess'));
          } else {
            Message.success(t('common.addSuccess'));
          }

          if (isContinue) {
            // TODO
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
    });
  }
</script>

<style scoped lang="less"></style>
