<template>
  <div class="h-full bg-[var(--text-n9)] p-[16px]">
    <Suspense>
      <RecordTable v-if="props.type === 'followRecord'" />
      <PlanTable v-else />
    </Suspense>
  </div>
</template>

<script setup lang="ts">
  import PlanTable from '@/components/business/crm-follow-drawer/components/planTable.vue';
  import RecordTable from '@/components/business/crm-follow-drawer/components/recordTable.vue';

  const props = defineProps<{
    type: 'followRecord' | 'followPlan';
  }>();
</script>
