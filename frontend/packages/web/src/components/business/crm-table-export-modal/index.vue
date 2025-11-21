<template>
  <CrmDrawer
    v-model:show="show"
    :title="t('common.export')"
    :width="800"
    :auto-focus="false"
    :show-back="false"
    closable
    :ok-text="t('common.export')"
    :ok-disabled="!form.fileName.trim().length || !selectedList.length"
    @confirm="confirmHandler"
    @cancel="closeHandler"
  >
    <n-form
      ref="formRef"
      :model="form"
      :rules="{
        fileName: [
          {
            required: true,
            message: t('common.nameNotNull'),
          },
          {
            validator,
          },
        ],
      }"
      label-placement="left"
      label-width="auto"
      require-mark-placement="left"
      class="hidden-feedback min-w-[350px]"
    >
      <n-form-item path="fileName" :label="t('common.name')" required>
        <n-input-group>
          <n-input v-model:value="form.fileName" type="text" :maxlength="50" :placeholder="t('common.pleaseInput')" />
          <n-input-group-label>.xlsx</n-input-group-label>
        </n-input-group>
        <div class="mt-[2px] text-[12px] text-[var(--text-n4)]"> {{ t('common.exportTaskTip') }} </div>
      </n-form-item>
    </n-form>

    <div class="flex h-[calc(100%-78px)] overflow-hidden">
      <div class="flex flex-[1.5] flex-col overflow-hidden border border-[var(--text-n8)]">
        <div class="flex items-center bg-[var(--text-n9)] px-[16px] py-[8px]">
          <n-checkbox :checked="isCheckedAll" :indeterminate="indeterminate" @update:checked="handleChangeAll">
            <span class="font-semibold">
              {{ t('common.optionalFields') }}
              <span class="text-[var(--text-n4)]">({{ allList.length }})</span>
            </span>
          </n-checkbox>
        </div>

        <n-scrollbar>
          <FieldSection
            v-if="systemList.length"
            v-model:selected-ids="selectedSystemIds"
            :items="systemList"
            :title="t('common.systemFields')"
            @select-part="(ids) => updateSelectedList(ids, systemList)"
            @select-item="(meta) => selectItem(ColumnTypeEnum.SYSTEM, meta)"
          />

          <FieldSection
            v-if="customList.length"
            v-model:selected-ids="selectedCustomIds"
            :items="customList"
            :title="t('common.formFields')"
            @select-part="(ids) => updateSelectedList(ids, customList)"
            @select-item="(meta) => selectItem(ColumnTypeEnum.CUSTOM, meta)"
          />
        </n-scrollbar>
      </div>

      <div class="flex flex-1 flex-col overflow-hidden border border-l-0 border-[var(--text-n8)]">
        <div class="flex items-center justify-between bg-[var(--text-n9)] px-[16px]">
          <span class="py-[8px] font-semibold">
            {{ t('common.selectedFields') }}
            <span class="text-[var(--text-n4)]">({{ selectedList.length }})</span>
          </span>
          <n-button text type="primary" @click="handleReset">
            {{ t('common.clear') }}
          </n-button>
        </div>

        <n-scrollbar>
          <VueDraggable v-model="selectedList" ghost-class="ghost" handle=".select-item" class="m-[16px]">
            <div
              v-for="element in selectedList"
              :key="element.key"
              class="select-item mb-[8px] flex items-center justify-between rounded bg-[var(--text-n9)] px-[8px] py-[6px]"
            >
              <div class="flex items-center">
                <CrmIcon type="iconicon_move" class="mr-[4px] cursor-move text-[var(--text-n4)]" :size="12" />
                <n-tooltip>
                  <template #trigger>
                    {{ element.title }}
                  </template>
                  {{ element.title }}
                </n-tooltip>
              </div>

              <n-button text size="small" @click="removeSelectedField(element.key)">
                <template #icon>
                  <CrmIcon type="iconicon_close" class="text-[var(--text-n4)]" />
                </template>
              </n-button>
            </div>
          </VueDraggable>
        </n-scrollbar>
      </div>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import {
    FormInst,
    FormItemRule,
    NButton,
    NCheckbox,
    NForm,
    NFormItem,
    NInput,
    NInputGroup,
    NInputGroupLabel,
    NScrollbar,
    NTooltip,
    useMessage,
  } from 'naive-ui';
  import dayjs from 'dayjs';
  import { VueDraggable } from 'vue-draggable-plus';

  import { ColumnTypeEnum } from '@lib/shared/enums/commonEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ExportTableColumnItem } from '@lib/shared/models/common';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import FieldSection from './components/fieldSection.vue';

  import {
    exportClueAll,
    exportCluePoolAll,
    exportCluePoolSelected,
    exportClueSelected,
    exportContactAll,
    exportContactSelected,
    exportContractAll,
    exportContractSelected,
    exportCustomerAll,
    exportCustomerOpenSeaAll,
    exportCustomerOpenSeaSelected,
    exportCustomerSelected,
    exportOpportunityAll,
    exportOpportunitySelected,
    exportPaymentPlanAll,
    exportPaymentPlanSelected,
  } from '@/api/modules';

  const props = defineProps<{
    params: Record<string, any>;
    type: 'customer' | 'clue' | 'opportunity' | 'cluePool' | 'openSea' | 'contact' | 'contract' | 'contractPaymentPlan';
    exportColumns: ExportTableColumnItem[];
    isExportAll?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'createSuccess'): void;
  }>();

  const { t } = useI18n();
  const message = useMessage();

  const show = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const typeStringMap = {
    customer: t('menu.customer'),
    clue: t('menu.clue'),
    opportunity: t('menu.opportunity'),
    cluePool: t('module.cluePool'),
    openSea: t('module.openSea'),
    contact: t('menu.contact'),
    contract: t('module.contract'),
    contractPaymentPlan: t('module.paymentPlan'),
  };

  const loading = ref<boolean>(false);
  const formRef = ref<FormInst>();
  const form = ref<Record<string, any>>({
    fileName: '',
  });

  watch(
    () => show.value,
    (newVal) => {
      if (newVal) {
        form.value.fileName = `${dayjs().format('YYYYMMDD-HHmmss')}-${typeStringMap[props.type]}`;
      }
    }
  );

  function validator(rule: FormItemRule, value: string) {
    if (/\//g.test(value)) {
      return Promise.reject(new Error(t('common.notAllowForwardSlash')));
    }
    return Promise.resolve();
  }

  const systemList = computed(() => props.exportColumns.filter((item) => item.columnType === ColumnTypeEnum.SYSTEM));
  const customList = computed(() => props.exportColumns.filter((item) => item.columnType === ColumnTypeEnum.CUSTOM));
  const allList = computed(() => [...systemList.value, ...customList.value]);

  // 已选
  const selectedList = ref<any[]>([]);

  const updateSelectedList = (ids: string[], sourceList: any[]) => {
    const newItems = sourceList.filter((item) => ids.includes(item.key));
    const remainingItems = selectedList.value.filter((item) => !sourceList.some((src) => src.key === item.key));
    selectedList.value = [...remainingItems, ...newItems];
  };

  const selectedSystemIds = computed(() =>
    selectedList.value.filter((e) => e.columnType === ColumnTypeEnum.SYSTEM).map((e) => e.key)
  );

  const selectedCustomIds = computed(() =>
    selectedList.value.filter((e) => e.columnType === ColumnTypeEnum.CUSTOM).map((e) => e.key)
  );

  function selectItem(columnType: ColumnTypeEnum, meta: { actionType: 'check' | 'uncheck'; value: string | number }) {
    if (meta.actionType === 'check') {
      // 添加选中的项
      const itemToAdd = (columnType === ColumnTypeEnum.SYSTEM ? systemList.value : customList.value).find(
        (i) => i.key === meta.value
      );
      if (itemToAdd) {
        selectedList.value.push(itemToAdd);
      }
    } else {
      // 移除取消选中的项
      selectedList.value = selectedList.value.filter((item) => item.key !== meta.value);
    }
  }

  // 全选
  const isCheckedAll = computed(() => {
    return selectedList.value.length === allList.value.length;
  });

  const indeterminate = computed(() => {
    return selectedList.value.length > 0 && selectedList.value.length < allList.value.length;
  });

  const handleReset = () => {
    selectedList.value = [];
  };

  const removeSelectedField = (id: string) => {
    selectedList.value = selectedList.value.filter((item) => item.key !== id);
  };

  const handleChangeAll = (value: boolean | (string | number | boolean)[]) => {
    if (value) {
      selectedList.value = allList.value;
    } else {
      selectedList.value = [];
    }
  };

  function closeHandler() {
    formRef.value?.restoreValidation();
    handleReset();
  }

  const exportAllApiMap = {
    customer: exportCustomerAll,
    contact: exportContactAll,
    clue: exportClueAll,
    opportunity: exportOpportunityAll,
    cluePool: exportCluePoolAll,
    openSea: exportCustomerOpenSeaAll,
    contract: exportContractAll,
    contractPaymentPlan: exportPaymentPlanAll,
  };

  const exportSelectedApiMap = {
    customer: exportCustomerSelected,
    contact: exportContactSelected,
    clue: exportClueSelected,
    opportunity: exportOpportunitySelected,
    cluePool: exportCluePoolSelected,
    openSea: exportCustomerOpenSeaSelected,
    contract: exportContractSelected,
    contractPaymentPlan: exportPaymentPlanSelected,
  };

  function confirmHandler() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          const exportApi = props.isExportAll ? exportAllApiMap[props.type] : exportSelectedApiMap[props.type];
          await exportApi({
            ...props.params,
            ids: props.params.ids || [],
            fileName: form.value.fileName.trim(),
            headList: selectedList.value,
          });
          form.value.fileName = '';
          show.value = false;
          message.success(t('common.exportTaskCreate'));
          emit('createSuccess');
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
