<template>
  <CrmCard hide-footer :special-height="props.specialHeight">
    <CrmTable
      ref="crmTableRef"
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    >
      <template #tableTop>
        <div
          :class="`flex items-center ${
            !props.readonly && hasAnyPermission(['CUSTOMER_MANAGEMENT_CONTACT:ADD']) ? 'justify-between' : 'justify-end'
          }`"
        >
          <n-button
            v-if="!props.readonly"
            v-permission="['CUSTOMER_MANAGEMENT_CONTACT:ADD']"
            type="primary"
            @click="handleCreate"
          >
            {{ t('overviewDrawer.addContract') }}
          </n-button>
          <CrmSearchInput
            v-model:value="keyword"
            class="!w-[240px]"
            :placeholder="t('common.searchByNamePhone')"
            @search="searchData"
          />
        </div>
      </template>
    </CrmTable>
    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.CONTACT"
      :source-id="activeContactId"
      :need-init-detail="needInitDetail"
      :initial-source-name="props.initialSourceName"
      @saved="loadList"
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
          <n-input
            v-model:value="form.reason"
            type="textarea"
            :placeholder="t('common.pleaseInput')"
            allow-clear
            maxlength="200"
            show-count
          />
        </n-form-item>
      </n-form>
    </CrmModal>
  </CrmCard>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { FormInst, FormRules, NButton, NForm, NFormItem, NInput, NSwitch, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { CustomerContractListItem } from '@lib/shared/models/customer';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';

  import {
    checkOpportunity,
    deleteCustomerContact,
    disableCustomerContact,
    enableCustomerContact,
  } from '@/api/modules';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  import { AppRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    sourceId?: string;
    refreshKey?: number;
    initialSourceName?: string;
    readonly?: boolean;
    formKey: FormDesignKeyEnum.CONTACT | FormDesignKeyEnum.CUSTOMER_CONTACT | FormDesignKeyEnum.BUSINESS_CONTACT;
    searchType?: CustomerSearchTypeEnum;
    specialHeight?: number;
  }>();

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();
  const router = useRouter();

  const keyword = ref('');
  const formCreateDrawerVisible = ref(false);
  const activeContactId = ref('');
  const activeContactName = ref('');
  const needInitDetail = ref(false);
  const tableRefreshId = ref(0);

  const operationGroupList: ActionsItem[] = [
    {
      label: t('common.edit'),
      key: 'edit',
      permission: ['CUSTOMER_MANAGEMENT_CONTACT:UPDATE'],
    },
    {
      label: t('common.delete'),
      key: 'delete',
      permission: ['CUSTOMER_MANAGEMENT_CONTACT:DELETE'],
    },
  ];

  function handleCreate() {
    if (props.sourceId) {
      activeContactId.value = props.sourceId;
    } else {
      activeContactId.value = '';
    }
    needInitDetail.value = false;
    formCreateDrawerVisible.value = true;
  }

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
      onNegativeClick: () => {
        if (hasData) {
          router.push({
            name: AppRouteEnum.OPPORTUNITY,
          });
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
        title: t('common.confirmEnableTitle', { name: characterLimit(row.name) }),
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
        needInitDetail.value = true;
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
    formKey: props.formKey,
    showPagination: !props.sourceId,
    readonly: props.readonly,
    operationColumn: {
      key: 'operation',
      width: 100,
      fixed: 'right',
      render: (row: CustomerContractListItem) =>
        h(CrmOperationButton, {
          groupList: props.readonly ? [] : operationGroupList,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
    specialRender: {
      status: (row: CustomerContractListItem) => {
        return h(NSwitch, {
          value: row.enable,
          disabled: !hasAnyPermission(['CUSTOMER_MANAGEMENT_CONTACT:UPDATE']) || props.readonly,
          onClick: () => {
            if (!hasAnyPermission(['CUSTOMER_MANAGEMENT_CONTACT:UPDATE']) || props.readonly) return;
            handleToggleStatus(row);
          },
        });
      },
      customerId: (row: CustomerContractListItem) => {
        return h(CrmNameTooltip, { text: row.customerName });
      },
      name: (row: CustomerContractListItem) => {
        return h(CrmNameTooltip, { text: row.name });
      },
    },
  });
  const { propsRes, propsEvent, loadList, setLoadListParams } = useTableRes;
  const backupData = ref<CustomerContractListItem[]>([]);

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  async function searchData(val?: string) {
    if (props.sourceId) {
      if (val) {
        const lowerCaseVal = val.toLowerCase();
        propsRes.value.data = backupData.value.filter((item: CustomerContractListItem) => {
          return item.name.toLowerCase().includes(lowerCaseVal) || item.phone.toLowerCase().includes(lowerCaseVal);
        });
      } else {
        setLoadListParams({ id: props.sourceId });
        await loadList();
        backupData.value = cloneDeep(propsRes.value.data);
      }
    } else {
      setLoadListParams({ keyword: val ?? keyword.value, searchType: props.searchType });
      await loadList();
      backupData.value = cloneDeep(propsRes.value.data);
    }
    crmTableRef.value?.scrollTo({ top: 0 });
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
    if (!props.sourceId) return;
    searchData();
  });

  watch(
    () => props.searchType,
    (val) => {
      if (val) {
        searchData();
      }
    },
    { immediate: true }
  );
</script>
