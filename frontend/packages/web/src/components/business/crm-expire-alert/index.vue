<template>
  <div v-expire class="mb-[16px]" :class="props.styleClass">
    <n-alert :type="licenseStore.expiredDays > 0 && licenseStore.expiredDays <= 30 ? 'warning' : 'error'">
      {{
        licenseStore.expiredDays > 0 && licenseStore.expiredDays <= 30
          ? t('system.license.LicenseExpirationPromptLessThanThirty', { day: licenseStore.expiredDays })
          : t('system.license.LicenseExpirationPromptGreaterThanThirty')
      }}
    </n-alert>
  </div>
</template>

<script setup lang="ts">
  import { NAlert } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import useLicenseStore from '@/store/modules/setting/license';

  const licenseStore = useLicenseStore();
  const { t } = useI18n();

  const props = defineProps<{
    styleClass?: string[];
  }>();
</script>

<style scoped></style>
