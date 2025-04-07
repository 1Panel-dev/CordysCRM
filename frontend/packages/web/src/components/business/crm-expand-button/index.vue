<template>
  <div>
    <div class="flex items-center gap-[8px] text-[var(--primary-8)]" @click="toggle">
      <CrmIcon
        class="cursor-pointer"
        :type="innerExpand ? 'iconicon_chevron_down' : 'iconicon_chevron_right'"
        :size="16"
      />
      <div class="cursor-pointer">{{ props.text || t('common.more') }}</div>
      <div class="flex-1">
        <n-divider dashed />
      </div>
    </div>

    <div v-show="expand" class="toggle-content">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NDivider } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  const { t } = useI18n();

  const props = defineProps<{
    text?: string;
  }>();

  const innerExpand = defineModel<boolean>('expand', {
    required: true,
    default: false,
  });

  function toggle() {
    innerExpand.value = !innerExpand.value;
  }
</script>

<style scoped lang="less"></style>
