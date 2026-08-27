<template>
  <div class="hidden" />
</template>

<script setup lang="ts">
  import useModal from '@/hooks/useModal';
  import useLicenseStore from '@/store/modules/setting/license';

  const { openModal } = useModal();
  const licenseStore = useLicenseStore();

  watch(
    () => licenseStore.noLicenseFeature,
    (feature) => {
      if (!feature) {
        return;
      }

      licenseStore.clearNoLicenseFeature();
      openModal(licenseStore.getNoLicenseModalConfig());
    },
    { immediate: true, flush: 'post' }
  );
</script>
