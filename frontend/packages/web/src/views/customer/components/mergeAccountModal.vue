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
        path="toMergeId"
        :rule="[
          {
            required: true,
            message: t('common.notNull', { value: `${t('module.customerManagement')}` }),
          },
        ]"
      >
        <n-select
          v-model:value="form.toMergeId"
          :placeholder="t('common.pleaseSelect')"
          clearable
          filterable
          :options="accountList"
          @scroll="handleAccountScroll"
          @search="handleAccountSearch"
          @input="handleAccountInput"
          @clear="handleAccountSearch"
          @update-value="changeAccount"
        />
      </n-form-item>

      <n-form-item
        path="ownerId"
        :label="t('common.head')"
        :rule="[
          {
            required: true,
            message: t('common.notNull', { value: `${t('common.head')}` }),
          },
        ]"
      >
        <n-select
          v-model:value="form.ownerId"
          :disabled="form.selectedAccount !== 'selected'"
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
  import { debounce } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { MergeAccountParams } from '@lib/shared/models/customer';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { mergeAccount, mergeAccountPage } from '@/api/modules';

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

  const initForm: MergeAccountParams & {
    selectedAccount: string;
  } = {
    selectedAccount: 'selected',
    mergeIds: [],
    toMergeId: null,
    ownerId: null,
  };

  const form = ref({ ...initForm });

  function cancelHandler() {
    form.value = { ...initForm };
    showModal.value = false;
  }

  const loading = ref(false);
  const formRef = ref<FormInst | null>(null);
  function confirmHandler() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          const { toMergeId, ownerId } = form.value;
          await mergeAccount({
            mergeIds: props.selectedRows.map((e) => e.id) as string[],
            toMergeId,
            ownerId,
          });
          Message.success(t('common.transferSuccess'));
          emit('saved');
          cancelHandler();
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
      ? (props.selectedRows.map((e) => ({
          ownerId: e.owner,
          ownerName: e.ownerName,
          value: e.id,
          label: e.name,
        })) as SelectMixedOption[])
      : otherAccountList.value
  );

  const otherOwnerList = ref<SelectMixedOption[]>([]);
  const ownerList = computed<SelectMixedOption[]>(() => {
    const source =
      form.value.selectedAccount === 'selected'
        ? props.selectedRows
            .filter((e) => e.ownerName)
            .map((e) => ({
              value: e.owner,
              label: e.ownerName,
            }))
        : otherOwnerList.value;

    return Array.from(new Map(source.map((item) => [item.value, item])).values()) as SelectMixedOption[];
  });

  function handleChange() {
    form.value.toMergeId = null;
    form.value.ownerId = null;
  }

  const pagination = ref({
    total: 0,
    pageSize: 10,
    current: 1,
  });

  const accountLoading = ref(false);

  const accountKeyword = ref('');
  async function loadOtherAccount(keyword?: string, reset = false) {
    if (form.value.selectedAccount !== 'other') return;
    if (reset) {
      pagination.value.current = 1;
      otherAccountList.value = [];
    }

    try {
      accountLoading.value = true;
      const result = await mergeAccountPage({
        keyword,
        current: pagination.value.current,
        pageSize: pagination.value.pageSize,
      });

      const resultData = result.list.map((e: any) => ({
        ownerId: e.owner,
        ownerName: e.ownerName,
        value: e.id,
        label: e.name,
      }));

      if (reset) {
        otherAccountList.value = resultData;
      } else {
        otherAccountList.value = [...otherAccountList.value, ...resultData];
      }
      pagination.value.total = result.total;
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    } finally {
      accountLoading.value = false;
    }
  }

  function handleAccountScroll(e: Event) {
    const currentTarget = e.currentTarget as HTMLElement;
    if (currentTarget.scrollTop + currentTarget.offsetHeight >= currentTarget.scrollHeight) {
      if (!accountLoading.value && otherAccountList.value.length < pagination.value.total) {
        pagination.value.current += 1;
        loadOtherAccount(accountKeyword.value, false);
      }
    }
  }

  function handleAccountSearch(val?: string) {
    accountKeyword.value = val ?? '';
    loadOtherAccount(val, true);
  }

  function handleAccountInput(val: string) {
    accountKeyword.value = val;
    debounce(() => {
      handleAccountSearch(val);
    }, 200);
  }

  function changeAccount(value: Array<string | number | null>, option: SelectMixedOption | null | SelectMixedOption[]) {
    form.value.ownerId = null;

    if (form.value.selectedAccount === 'other') {
      const selectOption = option as SelectMixedOption;
      otherOwnerList.value = [
        {
          label: selectOption?.ownerName ?? t('common.optionNotExist'),
          value: selectOption?.ownerId,
        },
      ] as SelectMixedOption[];
    }

    if (option && 'ownerId' in option) {
      const ownerId = option.ownerId as string;
      form.value.ownerId = ownerId;
    }
  }

  watch(
    () => form.value.selectedAccount,
    (val) => {
      if (val === 'other') {
        loadOtherAccount('', true);
      }
    }
  );
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
