<template>
  <CrmCard hide-footer>
    <div class="mb-[16px] flex justify-between">
      <div v-if="props.customerId" class="font-medium text-[var(--text-n1)]">
        {{ t('opportunity.contactInfo') }}
      </div>
      <n-button v-else type="primary" @click="formCreateDrawerVisible = true">
        {{ t('overviewDrawer.addContract') }}
      </n-button>
      <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
    </div>
    <CrmTable
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    />
    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.CONTACT"
      :source-id="activeContactId"
    />
    <!-- 停用 -->
    <CrmModal
      v-model:show="deactivateModalVisible"
      size="small"
      :ok-loading="deactivateLoading"
      :positive-text="t('common.deactivate')"
      @confirm="handleDeactivate"
    >
      <template #title>
        <div class="flex gap-[4px] overflow-hidden">
          <div class="text-[var(--text-n1)]">{{ t('common.deactivationReason') }}</div>
          <div class="flex text-[var(--text-n4)]">
            (
            <div class="one-line-text max-w-[300px]">{{ activeContactName }}</div>
            )
          </div>
        </div>
      </template>
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="left"
        label-width="auto"
        require-mark-placement="right"
      >
        <n-form-item path="reason" :label="t('common.deactivationReason')">
          <n-input v-model:value="form.reason" type="textarea" :placeholder="t('common.pleaseInput')" allow-clear />
        </n-form-item>
      </n-form>
    </CrmModal>
  </CrmCard>
</template>

<script setup lang="ts">
  import { FormInst, FormRules, NButton, NForm, NFormItem, NInput, NSwitch, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import type { CustomerContractListItem } from '@lib/shared/models/customer';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import {
    checkOpportunity,
    deleteCustomerContact,
    disableCustomerContact,
    enableCustomerContact,
  } from '@/api/modules/customer/index';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import { characterLimit } from '@/utils';

  const props = defineProps<{
    customerId?: string;
    refreshKey?: number;
  }>();

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();

  const keyword = ref('');
  const formCreateDrawerVisible = ref(false);
  const activeContactId = ref('');
  const activeContactName = ref('');
  const tableRefreshId = ref(0);

  const operationGroupList: ActionsItem[] = [
    {
      label: t('common.edit'),
      key: 'edit',
    },
    {
      label: t('common.delete'),
      key: 'delete',
    },
  ];

  async function bindOpportunity(id: string) {
    try {
      return await checkOpportunity(id);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  // 删除
  async function handleDelete(row: CustomerContractListItem) {
    const hasData = await bindOpportunity(row.id);
    const title = hasData
      ? t('customer.contact.deleteTitle')
      : t('common.deleteConfirmTitle', { name: characterLimit(row.name) });
    const content = hasData ? '' : t('customer.contact.deleteContentTip');
    const positiveText = t(hasData ? 'opportunity.gotIt' : 'common.confirm');
    const negativeText = t(hasData ? 'opportunity.goMove' : 'common.cancel');

    openModal({
      type: 'error',
      title,
      content,
      positiveText,
      negativeText,
      positiveButtonProps: {
        type: hasData ? 'primary' : 'error',
        size: 'medium',
      },
      onPositiveClick: async () => {
        if (!hasData) {
          try {
            await deleteCustomerContact(row.id);
            Message.success(t('common.deleteSuccess'));
            tableRefreshId.value += 1;
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
          }
        }
      },
    });
  }

  // 切换状态
  const deactivateModalVisible = ref(false);
  const deactivateLoading = ref(false);
  const formRef = ref<FormInst | null>(null);
  const form = ref<{ reason: string }>({ reason: '' });
  const rules: FormRules = {
    reason: [{ required: true, message: t('common.notNull', { value: t('common.deactivationReason') }) }],
  };

  async function handleToggleStatus(row: CustomerContractListItem) {
    if (row.enable) {
      deactivateModalVisible.value = true;
      form.value.reason = '';
      activeContactName.value = row.name;
      activeContactId.value = row.id;
    } else {
      openModal({
        type: 'default',
        title: t('common.confirmEnableTitle', { name: row.name }),
        positiveText: t('common.enable'),
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          try {
            await enableCustomerContact(row.id);
            Message.success(t('common.activated'));
            tableRefreshId.value += 1;
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
          }
        },
      });
    }
  }

  async function handleDeactivate() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          deactivateLoading.value = true;
          await disableCustomerContact(activeContactId.value, form.value.reason);
          Message.success(t('common.deactivated'));
          deactivateModalVisible.value = false;
          tableRefreshId.value += 1;
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          deactivateLoading.value = false;
        }
      }
    });
  }

  function handleActionSelect(row: CustomerContractListItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        activeContactId.value = row.id;
        activeContactName.value = row.name;
        formCreateDrawerVisible.value = true;
        break;
      case 'delete':
        handleDelete(row);
        break;
      default:
        break;
    }
  }

  const { useTableRes } = await useFormCreateTable({
    formKey: !props.customerId ? FormDesignKeyEnum.CONTACT : FormDesignKeyEnum.CUSTOMER_CONTACT,
    showPagination: !props.customerId,
    operationColumn: {
      key: 'operation',
      width: 100,
      fixed: 'right',
      render: (row: CustomerContractListItem) =>
        h(CrmOperationButton, {
          groupList: operationGroupList,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
    specialRender: {
      status: (row: CustomerContractListItem) => {
        return h(NSwitch, {
          value: row.enable,
          onClick: () => {
            handleToggleStatus(row);
          },
        });
      },
    },
  });
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTableRes;

  function searchData(val?: string) {
    if (props.customerId) {
      if (val) {
        const lowerCaseVal = val.toLowerCase();
        propsRes.value.data = propsRes.value.data.filter((item: CustomerContractListItem) => {
          return item.name.toLowerCase().includes(lowerCaseVal);
        });
      } else {
        setLoadListParams({ id: props.customerId });
        loadList();
      }
    } else {
      setLoadListParams({ keyword: val ?? keyword.value });
      loadList();
    }
  }

  watch(
    () => tableRefreshId.value,
    () => {
      loadList();
    }
  );

  watch(
    () => props.refreshKey,
    (val) => {
      if (val) {
        loadList();
      }
    }
  );

  onMounted(() => {
    searchData();
  });
</script>
