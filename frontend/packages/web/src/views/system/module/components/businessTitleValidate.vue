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
        <div v-for="element in formConfigList" :key="element.id" class="business-name-setting-item">
          <div class="flex flex-1 items-center gap-[8px] overflow-hidden">
            <n-tooltip trigger="hover" placement="top">
              <template #trigger>
                <span class="one-line-text text-[12px]">
                  {{ t(element.title) }}
                </span>
              </template>
              {{ t(element.title) }}
            </n-tooltip>
          </div>
          <n-switch
            v-model:value="element.required"
            :disabled="element.disabled"
            size="small"
            :rubber-band="false"
            @update:value="(val:boolean)=>handleChange(val, element.id)"
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
  import { BusinessTitleValidateConfig } from '@lib/shared/models/contract';

  import { getBusinessTitleConfig, switchBusinessTitleFormConfig } from '@/api/modules';
  import { allBusinessTitleFormConfigList } from '@/config/contract';

  const { t } = useI18n();
  const Message = useMessage();

  const defaultEnabledKey = ['businessName', 'identificationNumber', 'registrationAddress'];
  const defaultDisabledKey = ['businessName'];

  const formConfigList = ref<BusinessTitleValidateConfig[]>([]);

  const popoverVisible = ref(false);

  async function initValidateConfig() {
    try {
      const result = await getBusinessTitleConfig();
      formConfigList.value = result.map((e) => {
        const item = allBusinessTitleFormConfigList.find((c) => c.value === e.field);
        return {
          ...e,
          title: item?.label ?? '',
          disabled: defaultDisabledKey.includes(e.field),
        };
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleChange(value: boolean, id: string) {
    try {
      await switchBusinessTitleFormConfig(id);
      initValidateConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleUpdateShow(show: boolean) {
    if (show) {
      initValidateConfig();
    }
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
