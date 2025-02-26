<template>
  <CrmDrawer
    v-model:show="showDetailDrawer"
    :width="480"
    :footer="false"
    :title="detail?.userName"
    @cancel="emit('cancel')"
  >
    <template #titleLeft>
      <CrmTag theme="light" :type="`${detail?.enable ? 'success' : 'error'}`">
        {{ detail?.enable ? t('common.opened') : t('common.disabled') }}
      </CrmTag>
    </template>
    <template #titleRight>
      <n-button type="primary" ghost @click="() => emit('edit', detail?.id)">
        {{ t('common.edit') }}
      </n-button>
    </template>
    <CrmDescription class="m-[8px]" label-width="90px" :descriptions="descriptions" label-align="end">
      <template #gender="{ item }">
        {{ item.value ? t('org.female') : t('org.male') }}
      </template>
    </CrmDescription>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import type { MemberParams } from '@lib/shared/models/system/org';

  import CrmDescription, { Description } from '@/components/pure/crm-description/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { getUserDetail } from '@/api/modules/system/org';
  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const props = defineProps<{
    userId: string;
  }>();

  const emit = defineEmits<{
    (e: 'edit', id?: string): void;
    (e: 'cancel'): void;
  }>();

  const showDetailDrawer = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const initDetail = {
    name: '',
    gender: false,
    phone: '',
    email: '',
    departmentId: '',
    employeeId: '',
    position: '',
    enable: true,
    employeeType: '',
    workCity: '',
    supervisorId: '',
    roleIds: [],
    userGroupIds: [],
    userName: '',
  };

  const detail = ref<MemberParams>(cloneDeep(initDetail));

  const initDescriptions = [
    {
      label: t('org.phoneNumber'),
      value: 'phone',
    },
    { label: t('org.gender'), value: 'gender', slotName: 'gender' },
    { label: t('org.userEmail'), value: 'email' },
    { label: t('org.department'), value: 'department' },
    { label: t('org.employeeNumber'), value: 'employeeId' },
    { label: t('org.employeeType'), value: 'employeeType' },
    { label: t('org.directSuperior'), value: 'supervisorId' },
    { label: t('org.workingCity'), value: 'workCity' },
    { label: t('org.role'), value: 'role' },
    { label: t('org.userGroup'), value: 'userGroup' },
  ];

  const descriptions = ref<Description[]>(cloneDeep(initDescriptions));

  async function getDetail() {
    descriptions.value = cloneDeep(initDescriptions);
    try {
      detail.value = await getUserDetail(props.userId);
      if (detail.value) {
        descriptions.value = descriptions.value.map((e) => {
          return {
            ...e,
            value: detail.value[e.value as keyof MemberParams] as string,
          };
        });
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => showDetailDrawer.value,
    (val) => {
      if (val) {
        getDetail();
      }
    }
  );
</script>

<style scoped></style>
