<template>
  <n-scrollbar
    :content-style="{ gridTemplateColumns: `repeat(${(stageConfig?.stageConfigList || []).length || 7}, 300px)` }"
    content-class="grid gap-[16px] h-full"
    class="mb-[16px] flex-1"
    x-scrollable
  >
    <list
      v-for="(item, index) in stageConfig?.stageConfigList"
      ref="listRef"
      :key="item.id"
      :index="index"
      :view-id="props.viewId"
      :stage-ids="stageConfig?.stageConfigList.map((i) => i.id) || []"
      :keyword="keyword"
      :field-list="fieldList"
      :stage-config="item"
      :refresh-time-stamp="refreshTimeStamp"
      :advance-filter="advanceFilter"
      :enable-reason="enableReason"
      :fail-id="stageConfig?.stageConfigList.slice(-1)[0].id"
      @fail="handleFailItem"
      @change="refreshList"
    />
  </n-scrollbar>
  <CrmModal
    v-model:show="updateStatusModal"
    :title="t('common.complete')"
    :ok-loading="updateStageLoading"
    size="small"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <n-form ref="formRef" :model="form" label-placement="left" require-mark-placement="left">
      <n-form-item
        require-mark-placement="left"
        label-placement="left"
        path="failureReason"
        :label="t('opportunity.failureReason')"
        :rule="[{ required: true, message: t('common.notNull', { value: t('opportunity.failureReason') }) }]"
      >
        <n-select v-model:value="form.failureReason" :options="reasonList" :placeholder="t('common.pleaseSelect')" />
      </n-form-item>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { NForm, NFormItem, NScrollbar, NSelect } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { OpportunityStageConfig } from '@lib/shared/models/opportunity';

  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import list from './list.vue';

  import { getOpportunityStageConfig, getReasonConfig, updateOptStage } from '@/api/modules';
  import useFormCreateApi from '@/hooks/useFormCreateApi';

  const props = defineProps<{
    advanceFilter?: FilterResult;
    viewId?: string;
    keyword?: string;
  }>();
  const emit = defineEmits<{
    (e: 'change'): void;
  }>();

  const { t } = useI18n();

  const { initFormConfig: initOptFormConfig, fieldList } = useFormCreateApi({
    formKey: computed(() => FormDesignKeyEnum.BUSINESS),
  });

  const stageConfig = ref<OpportunityStageConfig>();
  async function initStageConfig() {
    try {
      stageConfig.value = await getOpportunityStageConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const refreshTimeStamp = ref(0);
  function refresh() {
    nextTick(() => {
      refreshTimeStamp.value += 1;
    });
  }

  const listRef = ref<InstanceType<typeof list>[]>();
  function refreshList(stage: string) {
    const index = stageConfig.value?.stageConfigList.findIndex((item) => item.id === stage) || 0;
    listRef.value?.[index].refreshList();
    emit('change');
  }

  const form = ref({
    failureReason: null,
  });
  const updateStatusModal = ref<boolean>(false);
  const updateStageLoading = ref(false);
  const updateOptItem = ref<any>({});
  const enableReason = ref(false);
  const reasonList = ref<Option[]>([]);
  async function initReason() {
    try {
      const { dictList, enable } = await getReasonConfig(ReasonTypeEnum.OPPORTUNITY_FAIL_RS);
      enableReason.value = enable;
      reasonList.value = dictList.map((e) => ({ label: e.name, value: e.id }));
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  function handleFailItem(item: any) {
    updateOptItem.value = item;
    updateStatusModal.value = true;
    form.value.failureReason = null;
  }

  async function handleConfirm() {
    try {
      updateStageLoading.value = true;
      await listRef.value?.[(stageConfig.value?.stageConfigList.length || 1) - 1].sortItem(updateOptItem.value);
      await updateOptStage({
        id: updateOptItem.value.data.id,
        stage: stageConfig.value?.stageConfigList.slice(-1)[0].id || '',
        failureReason: form.value.failureReason || '',
      });
      updateStatusModal.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      updateStageLoading.value = false;
    }
  }

  function handleCancel() {
    updateOptItem.value = {};
    updateStatusModal.value = false;
    form.value.failureReason = null;
  }

  onBeforeMount(async () => {
    await initStageConfig();
    initOptFormConfig();
    initReason();
  });

  defineExpose({
    refresh,
  });
</script>

<style lang="less" scoped>
  .show-type-tabs {
    :deep(.n-tabs-tab) {
      padding: 6px;
    }
  }
  :deep(.n-scrollbar-rail--horizontal--bottom) {
    bottom: 0 !important;
  }
</style>
