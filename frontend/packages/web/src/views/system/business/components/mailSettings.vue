<template>
  <CrmCard no-content-padding hide-footer>
    <CrmDescription class="m-[24px]" :descriptions="descriptions" :column="2">
      <template #password="{ item }">
        <div class="flex items-center gap-[8px]">
          <div v-show="showPassword">{{ item.value }}</div>
          <div v-show="!showPassword">{{ desensitize(item.value as string) }}</div>
          <CrmIcon
            :type="showPassword ? 'iconicon_browse' : 'iconicon_browse_off'"
            :size="16"
            class="cursor-pointer text-[var(--text-n4)]"
            @click="changeShowVisible"
          />
        </div>
      </template>
      <template #ssl="{ item }">
        <div class="flex items-center gap-[8px]">
          <CrmIcon
            v-if="item.value === 'true'"
            type="iconicon_check_circle_filled"
            :size="16"
            class="text-[var(--success-green)]"
          />
          <CrmIcon v-else type="iconicon_disable" :size="16" class="text-[var(--text-n4)]" />
          <div>
            {{
              item.value === 'false'
                ? t('system.business.mailSettings.closed')
                : t('system.business.mailSettings.opened')
            }}
          </div>
        </div>
      </template>
    </CrmDescription>
    <n-divider class="!m-0" />
    <div class="my-[12px] mr-[24px] flex justify-end">
      <n-button type="primary" ghost> {{ t('system.business.mailSettings.testLink') }} </n-button>
      <n-button type="primary" class="ml-[8px]">{{ t('common.edit') }}</n-button>
    </div>
  </CrmCard>
</template>

<script setup lang="ts">
  import { NButton, NDivider } from 'naive-ui';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDescription, { Description } from '@/components/pure/crm-description/index.vue';

  import { useI18n } from '@/hooks/useI18n';
  import { desensitize } from '@/utils';

  const { t } = useI18n();

  const showPassword = ref(false);
  function changeShowVisible() {
    showPassword.value = !showPassword.value;
  }

  const descriptions = ref<Description[]>([
    {
      label: t('system.business.mailSettings.smtpHost'),
      value: 'xxxxxxxx',
    },
    { label: t('system.business.mailSettings.smtpPort'), value: 'xxx' },
    { label: t('system.business.mailSettings.smtpAccount'), value: 'xxx' },
    { label: t('system.business.mailSettings.smtpPassword'), value: 'xxx', slotName: 'password' },
    { label: t('system.business.mailSettings.from'), value: 'xxx' },
    { label: t('system.business.mailSettings.recipient'), value: 'xxx' },
    { label: t('system.business.mailSettings.ssl'), value: 'true', slotName: 'ssl' },
    { label: t('system.business.mailSettings.tsl'), value: 'false', slotName: 'ssl' },
  ]);
</script>
