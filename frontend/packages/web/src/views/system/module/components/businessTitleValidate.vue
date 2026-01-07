<template>
  <n-popover
    v-model:show="popoverVisible"
    trigger="click"
    placement="left-start"
    class="!p-0"
    @update:show="handleUpdateShow"
  >
    <template #trigger>
      <div class="inline-flex cursor-pointer items-center" @click.stop>
        <div>{{ t('module.contract.businessNameValidateConfig') }}</div>
        <CrmIcon type="iconicon_chevron_right" class="ml-[4px]" :size="16" />
      </div>
    </template>
    <n-scrollbar class="my-[4px] max-h-[416px] max-w-[250px] px-[4px]">
      <div class="min-w-[176px]">
        <div v-for="element in formConfigList" :key="element.value" class="business-name-setting-item">
          <div class="flex flex-1 items-center gap-[8px] overflow-hidden">
            <n-tooltip trigger="hover" placement="top">
              <template #trigger>
                <span class="one-line-text text-[12px]">
                  {{ t(element.label) }}
                </span>
              </template>
              {{ t(element.label) }}
            </n-tooltip>
          </div>
          <n-switch
            v-model:value="element.enabled"
            :disabled="element.disabled"
            size="small"
            :rubber-band="false"
            @update:value="handleChange"
          />
        </div>
      </div>
    </n-scrollbar>
  </n-popover>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NPopover, NScrollbar, NSwitch, NTooltip, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { allBusinessTitleFormConfigList } from '@/config/contract';

  const { t } = useI18n();
  const Message = useMessage();

  const defaultEnabledKey = ['businessName', 'identificationNumber', 'registrationAddress'];
  const defaultDisabledKey = ['businessName'];

  const formConfigList = computed(() => {
    return allBusinessTitleFormConfigList.map((item) => {
      return {
        ...item,
        enabled: defaultEnabledKey.includes(item.value),
        disabled: defaultDisabledKey.includes(item.value),
      };
    });
  });

  const popoverVisible = ref(false);

  function handleChange() {
    // todo xinxinwu
  }

  async function handleUpdateShow(show: boolean) {
    // todo xinxinwu
  }
</script>

<style scoped lang="less">
  .business-name-setting-item {
    padding: 5px 8px;
    max-width: 300px;
    border-radius: @border-radius-small;
    @apply flex items-center justify-between;
    &:hover {
      background: var(--text-n9);
    }
  }
</style>
