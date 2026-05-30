<template>
  <div> workflow </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();
  const Message = useMessage();
</script>

<style scoped></style>
