<template>
  <div class="flex gap-[24px]">
    <div
      v-for="item of props.list"
      :key="item.id"
      :class="`flex flex-col gap-[8px]`"
      :style="{ width: `calc(100% / ${props.list.length})` }"
    >
      <div class="font-medium text-[var(--text-n4)]">{{ item.name }}</div>
      <div v-for="ele of item.children" :key="ele.id" class="flex flex-nowrap items-center text-[var(--text-n1)]">
        <n-tooltip :delay="300">
          <template #trigger>
            <div class="one-line-text"> {{ ele.name }}</div>
          </template>
          {{ ele.name }}
        </n-tooltip>
        <CrmTag v-if="ele?.current" type="primary" theme="light">{{ t('opportunity.current') }}</CrmTag>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { NTooltip } from 'naive-ui';

  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();
  const props = defineProps<{
    // TODO 类型
    list: any[];
  }>();
</script>

<style lang="less" scoped></style>
