<template>
  <CrmModal
    v-model:show="validateResultModal"
    size="small"
    :title="t('common.import')"
    :ok-loading="props.importLoading"
    @cancel="handleCancel"
  >
    <div class="text-center">
      <CrmIcon :size="32" :type="getTipType?.icon" :class="`${getTipType?.color}`" />
      <div class="my-2 text-[16px] font-medium text-[var(--text-n1)]">{{ t(getTipType?.message ?? '') }}</div>
      <div class="leading-8 text-[var(--text-n4)]">
        <span>
          {{ t('crmImportButton.successfulCheck') }}
          <span class="mx-1 text-[var(--success-green)]"> {{ validateResultInfo.successCount }} </span>
          {{ t('crmImportButton.countNumber') }}；
          <span v-if="validateResultInfo.successCount && !validateResultInfo.failCount">
            {{ t('crmImportButton.successImportActionTip') }}
          </span>
        </span>
        <span v-if="validateResultInfo.failCount">
          {{ t('crmImportButton.failCheck') }}
          <span class="mx-1 font-medium text-[var(--error-red)]">{{ validateResultInfo.failCount }}</span>
          {{ t('crmImportButton.countNumber') }}；

          <n-popover class="error-popover" trigger="hover" placement="bottom" :show-arrow="false">
            <template #trigger>
              <n-button strong quaternary type="primary" class="text-btn-primary !px-[4px]">
                {{ t('crmImportButton.viewErrorDetail') }}
              </n-button>
            </template>
            <div class="error-detail-wrapper">
              <div class="p-[16px]">
                <div class="mb-[8px] font-medium text-[var(--text-n1)]">
                  {{ t('crmImportButton.someUsersImportFailed') }}
                  <span class="text-[14px] font-medium text-[var(--text-n4)]">
                    ({{ validateResultInfo.failCount }})
                  </span>
                </div>
                <n-scrollbar style="max-height: 30vh">
                  <div
                    v-for="(item, index) of validateResultInfo.errorMessages"
                    :key="`${item.rowNum}-${index}`"
                    class="error-messages-item"
                  >
                    {{ item.errMsg }}
                  </div>
                </n-scrollbar>
              </div>
              <div class="detail-more-btn middle-box-shadow h-[40px] text-[14px]" type="text" long @click="showMore">
                {{ t('common.ViewMore') }}
              </div>
            </div>
          </n-popover>
        </span>
      </div>
      <div v-if="validateResultInfo.failCount > 0" class="text-[var(--text-n4)]">
        {{ t('crmImportButton.afterFailingToModify') }}
      </div>
    </div>
    <template #footer>
      <div class="flex justify-end">
        <n-button v-if="!validateResultInfo.successCount || !validateResultInfo.failCount" quaternary @click="backList">
          {{ t('crmImportButton.backList', { title: props.title }) }}
        </n-button>

        <n-button
          v-if="validateResultInfo.successCount && !validateResultInfo.failCount"
          :disabled="props.importLoading"
          :loading="props.importLoading"
          quaternary
          type="primary"
          class="text-btn-primary"
          @click="confirmImport"
        >
          {{ t('common.import') }}
        </n-button>

        <n-button
          v-if="validateResultInfo.failCount || (validateResultInfo.failCount && validateResultInfo.successCount)"
          quaternary
          type="primary"
          class="text-btn-primary"
          @click="handleCancel"
        >
          {{ t('crmImportButton.backToUploadPage') }}
        </n-button>

        <n-button
          v-if="validateResultInfo.failCount && validateResultInfo.successCount"
          :loading="props.importLoading"
          quaternary
          type="primary"
          @click="confirmImport"
        >
          {{ t('crmImportButton.ignoreErrorContinueImporting') }}
        </n-button>
      </div>
    </template>
  </CrmModal>
  <CrmDrawer v-model:show="showMoreFailureCase" :width="680" :footer="false">
    <template #header>
      <div>
        {{ t('crmImportButton.importErrorData') }}
        <span class="text-[var(--text-n4)]">（{{ validateResultInfo.failCount }}）</span>
      </div>
    </template>
    <CrmList
      v-model:data="validateResultInfo.errorMessages"
      :bordered="false"
      :item-border="false"
      virtual-scroll-height="calc(100vh - 82px)"
      key-field="num"
      no-hover
      :item-height="30"
    >
      <template #item="{ item }">
        <div class="flex p-[8px]">
          <div class="circle"></div>
          <div class="text-[var(--color-text-2)]">{{ item.errMsg }} </div>
        </div>
      </template>
    </CrmList>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NPopover, NScrollbar } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ValidateInfo } from '@lib/shared/models/system/org';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';

  const { t } = useI18n();

  const props = defineProps<{
    validateInfo: ValidateInfo;
    importLoading: boolean;
    title?: string;
  }>();

  const emit = defineEmits<{
    (e: 'close'): void;
    (e: 'save'): void;
  }>();

  const validateResultModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const validateResultInfo = ref<ValidateInfo>(props.validateInfo);
  const showMoreFailureCase = ref<boolean>(false);

  function handleCancel() {
    validateResultModal.value = false;
  }

  // 查看更多导入失败
  function showMore() {
    showMoreFailureCase.value = true;
  }

  // 返回列表
  function backList() {
    validateResultModal.value = false;
    showMoreFailureCase.value = false;
    emit('close');
  }

  // 确定继续导入
  function confirmImport() {
    emit('save');
  }

  const statusMap = ref<
    Record<
      'partial' | 'success' | 'error',
      {
        type: string;
        icon: string;
        color: string;
        message: string;
      }
    >
  >({
    partial: {
      type: 'warning',
      icon: 'iconicon_info_circle_filled',
      color: 'text-[var(--warning-yellow)]',
      message: 'crmImportButton.partialVerificationFailed',
    },
    success: {
      type: 'success',
      icon: 'iconicon_check_circle_filled',
      color: 'text-[var(--success-green)]',
      message: 'crmImportButton.successfulCheck',
    },
    error: {
      type: 'error',
      icon: 'iconicon_close_circle_filled',
      color: 'text-[var(--error-red)]',
      message: 'crmImportButton.failCheck',
    },
  });

  const getTipType = computed(() => {
    const { successCount, failCount } = validateResultInfo.value;
    if (failCount && successCount) {
      return statusMap.value.partial;
    }
    if (!failCount) {
      return statusMap.value.success;
    }
    if (!successCount) {
      return statusMap.value.error;
    }
  });

  watch(
    () => props.validateInfo,
    (val) => {
      validateResultInfo.value = { ...val };
    },
    { deep: true }
  );
</script>

<style scoped lang="less">
  .detail-more-btn {
    color: var(--primary-8);
    @apply mt-2 flex cursor-pointer items-center justify-center;
  }
  .error-messages-item {
    font-size: 14px;
    line-height: 21px;
    color: var(--text-n2);
    @apply my-4;
  }
  .error-detail-wrapper {
    .error-detail-list {
      max-height: 400px;
    }
  }
  .circle {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--text-n7);
    @apply mr-2 mt-2 flex-shrink-0;
  }
</style>

<style lang="less">
  .error-popover {
    &.n-popover {
      padding: 0 !important;
      min-width: 440px;
    }
  }
</style>
