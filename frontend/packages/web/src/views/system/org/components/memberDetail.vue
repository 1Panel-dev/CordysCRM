<template>
  <CrmDrawer v-model:show="showDetailDrawer" :width="480" :footer="false" :title="props.rowsData.name">
    <template #titleLeft>
      <CrmTag theme="light" :type="`${props.rowsData.enabled ? 'success' : 'error'}`">
        {{ props.rowsData.enabled ? t('common.opened') : t('common.disabled') }}
      </CrmTag>
    </template>
    <template #titleRight>
      <n-button type="primary" ghost @click="() => emit('edit', props.rowsData.id)"> {{ t('common.edit') }} </n-button>
    </template>
    <CrmDescription class="m-[8px]" label-width="90px" :descriptions="descriptions" label-align="end"> </CrmDescription>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton } from 'naive-ui';

  import CrmDescription, { Description } from '@/components/pure/crm-description/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const props = defineProps<{
    // TODO xxw 类型
    rowsData: Record<string, any>;
  }>();

  const emit = defineEmits<{
    (e: 'edit', id: string): void;
  }>();

  const showDetailDrawer = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const descriptions = ref<Description[]>([
    {
      label: t('org.phoneNumber'),
      value: 'phoneNumber',
    },
    { label: t('org.gender'), value: 'gender' },
    { label: t('org.userEmail'), value: 'userEmail' },
    { label: t('org.department'), value: 'department' },
    { label: t('org.employeeNumber'), value: 'employeeNumber' },
    { label: t('org.employeeType'), value: 'employeeType' },
    { label: t('org.directSuperior'), value: 'directSuperior' },
    { label: t('org.workingCity'), value: 'workingCity' },
    { label: t('org.role'), value: 'role' },
    { label: t('org.userGroup'), value: 'userGroup' },
  ]);
</script>

<style scoped></style>
