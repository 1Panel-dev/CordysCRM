<template>
  <CrmTag v-if="props.status !== CustomerFollowPlanStatusEnum.ALL" :type="statusMap[props.status].type" theme="light">
    <template #icon>
      <CrmIcon :type="statusMap[props.status].icon" :size="16" :class="`text-[${statusMap[props.status].color}]`" />
    </template>
    <div :class="`text-[${statusMap[props.status].color}] flex items-center gap-[8px]`">
      {{ t(statusMap[props.status].label) }}
      <CrmIcon v-if="!props.hiddenDownIcon" type="iconicon_chevron_down" :size="16" class="text-[var(--text-n4)]" />
    </div>
  </CrmTag>
</template>

<script lang="ts" setup>
  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { statusMap } from '@/config/follow';

  const { t } = useI18n();

  const props = defineProps<{
    status: CustomerFollowPlanStatusEnum;
    hiddenDownIcon?: boolean;
  }>();
</script>

<style lang="less" scoped></style>
