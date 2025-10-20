<template>
  <CrmModal
    v-model:show="showModal"
    :title="t('customer.mergeAccount')"
    :ok-loading="loading"
    :positive-text="t('customer.merge')"
    class="crm-form-modal"
    @confirm="confirmHandler"
    @cancel="cancelHandler"
  >
    <n-form ref="formRef" :label-width="70" :model="form" label-placement="left" require-mark-placement="left">
      <n-form-item path="selectedAccount" :label="t('customer.mergeTo')">
        <n-radio-group v-model:value="form.selectedAccount" name="radiogroup" @change="handleChange">
          <n-space>
            <n-radio key="selected" value="selected">
              {{ t('customer.selectedAccount') }}
            </n-radio>
            <n-radio key="other" value="other">
              <div class="flex items-center gap-[8px]">
                {{ t('customer.otherAccount') }}
                <n-tooltip trigger="hover" placement="right">
                  <template #trigger>
                    <CrmIcon
                      type="iconicon_help_circle"
                      :size="16"
                      class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                    />
                  </template>
                  {{ t('customer.selectedMergeAccountTooltip') }}
                </n-tooltip>
              </div>
            </n-radio>
          </n-space>
        </n-radio-group>
      </n-form-item>
      <n-form-item
        class="ml-[70px] w-[calc(100%-70px)]"
        path="account"
        :rule="[
          {
            required: true,
            message: t('common.notNull', { value: `${t('module.customerManagement')}` }),
          },
        ]"
      >
        <n-select
          v-model:value="form.account"
          :placeholder="t('common.pleaseSelect')"
          clearable
          filterable
          :options="accountList"
        />
      </n-form-item>

      <n-form-item
        path="owner"
        :label="t('common.head')"
        :rule="[
          {
            required: true,
            message: t('common.notNull', { value: `${t('common.head')}` }),
          },
        ]"
      >
        <n-select
          v-model:value="form.owner"
          :disabled="!form.account"
          :placeholder="t('common.pleaseSelect')"
          clearable
          filterable
          :options="ownerList"
        />
      </n-form-item>

      <div class="merge-rule">
        <div class="mb-[4px] text-[var(--text-n1)]">{{ t('customer.mergeRules') }}</div>
        <div>{{ t('customer.selectedAccountMergeTip') }}</div>
        <div>{{ t('customer.afterMergeDeleteAccountBaseInfoTip') }}</div>
        <div>{{ t('customer.afterMergeInfoTip') }}</div>
      </div>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, NForm, NFormItem, NRadio, NRadioGroup, NSelect, NSpace, NTooltip, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { InternalRowData } from 'naive-ui/es/data-table/src/interface';
  import { SelectMixedOption } from 'naive-ui/es/select/src/interface';

  const Message = useMessage();

  const { t } = useI18n();

  const props = defineProps<{
    selectedRows: InternalRowData[];
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'saved'): void;
  }>();

  const form = ref({
    selectedAccount: 'selected',
    account: null,
    owner: null,
  });

  const loading = ref(false);
  const formRef = ref<FormInst | null>(null);
  function confirmHandler() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          //  TODO xinxinwu
          showModal.value = false;
          emit('saved');
          Message.success(t('common.transferSuccess'));
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          loading.value = false;
        }
      }
    });
  }

  const otherAccountList = ref<SelectMixedOption[]>([]);
  const accountList = computed<SelectMixedOption[]>(() =>
    form.value.selectedAccount === 'selected'
      ? (props.selectedRows.map((e) => ({ value: e.id, label: e.name })) as SelectMixedOption[])
      : otherAccountList.value
  );

  const otherOwnerList = ref<SelectMixedOption[]>([]);
  const ownerList = computed<SelectMixedOption[]>(() =>
    form.value.selectedAccount === 'selected'
      ? (props.selectedRows.map((e) => ({ value: e.owner, label: e.ownerName })) as SelectMixedOption[])
      : otherOwnerList.value
  );

  function handleChange() {
    form.value.account = null;
    form.value.owner = null;
  }
  function cancelHandler() {
    showModal.value = false;
  }
</script>

<style scoped lang="less">
  .merge-rule {
    margin: 0 0 16px;
    padding: 16px 24px;
    border: 1px solid var(--primary-8);
    border-radius: 6px;
    background: var(--primary-7);
  }
</style>
