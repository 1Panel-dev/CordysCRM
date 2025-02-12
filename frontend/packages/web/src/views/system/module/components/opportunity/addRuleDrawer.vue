<template>
  <CrmDrawer
    v-model:show="visible"
    :width="800"
    :title="t('module.businessManage.businessCloseRule')"
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
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="ruleName"
          :label="t('opportunity.ruleName')"
        >
          <n-input v-model:value="form.ruleName" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
      </div>
      <div class="flex">
        <div class="flex-1">
          <n-form-item
            require-mark-placement="left"
            label-placement="left"
            path="adminId"
            :label="t('opportunity.admin')"
          >
            <n-select v-model:value="form.adminId" :placeholder="t('common.pleaseSelect')" :options="adminOptions" />
          </n-form-item>
        </div>
        <div class="flex-1">
          <n-form-item
            require-mark-placement="left"
            label-placement="left"
            path="userId"
            :label="t('opportunity.members')"
          >
            <CrmUserSelect
              v-model:value="form.userId"
              value-field="id"
              label-field="name"
              mode="remote"
              filterable
              :fetch-api="getUserOptions"
            />
          </n-form-item>
        </div>
      </div>
      <div class="crm-module-form-title"> {{ t('module.businessManage.businessCloseRule') }}</div>
      <n-form-item
        require-mark-placement="left"
        label-placement="left"
        path="autoClose"
        :label="t('opportunity.autoClose')"
      >
        <n-radio-group v-model:value="form.autoClose" name="radiogroup">
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
        require-mark-placement="left"
        label-placement="left"
        path="expirationReminder"
        :label="t('opportunity.expirationReminder')"
      >
        <n-radio-group v-model:value="form.expirationReminder" name="radiogroup">
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
        require-mark-placement="left"
        label-placement="left"
        path="reminderAdvance"
        :label="t('module.reminderAdvance')"
      >
        <n-input
          v-model:value="form.reminderAdvance"
          class="crm-reminder-advance-input"
          type="text"
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
    NRadio,
    NRadioGroup,
    NSelect,
    NSpace,
    useMessage,
  } from 'naive-ui';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmUserSelect from '@/components/business/crm-user-select/index.vue';

  import { getUserOptions } from '@/api/modules/system/org';
  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();
  const Message = useMessage();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const adminOptions = ref([]);

  const rules: FormRules = {
    ruleName: [{ required: true, message: t('common.notNull', { value: `${t('org.userName')}` }) }],
    adminId: [{ required: true, message: t('common.pleaseSelect') }],
    userId: [{ required: true, message: t('common.pleaseSelect') }],
    reminderAdvance: [{ required: true, message: t('common.pleaseInput') }],
  };

  const form = ref({
    id: '',
    ruleName: '',
    adminId: null,
    userId: null,
    autoClose: true,
    expirationReminder: true,
    reminderAdvance: '1',
  });

  function cancelHandler() {}

  const formRef = ref<FormInst | null>(null);
  const loading = ref<boolean>(false);
  function confirmHandler(isContinue: boolean) {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
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
