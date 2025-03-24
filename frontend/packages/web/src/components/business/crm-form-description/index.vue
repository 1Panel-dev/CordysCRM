<template>
  <n-spin :show="loading">
    <CrmDescription :descriptions="descriptions" value-align="end">
      <template #divider="{ item }">
        <CrmFormCreateDivider :field-config="item.fieldInfo" class="!m-0 w-full" />
      </template>
    </CrmDescription>
  </n-spin>
</template>

<script setup lang="ts">
  import { NSpin } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmFormCreateDivider from '@/components/business/crm-form-create/components/basic/divider.vue';

  import useFormCreateApi from '@/hooks/useFormCreateApi';

  const props = defineProps<{
    sourceId: string;
    formKey: FormDesignKeyEnum;
    refreshKey?: number;
  }>();
  const emit = defineEmits<{
    (e: 'init', collaborationType?: CollaborationType): void;
  }>();

  const { descriptions, loading, collaborationType, initFormConfig, initFormDescription } = useFormCreateApi({
    formKey: toRefs(props).formKey,
    sourceId: toRefs(props).sourceId,
  });

  watch(
    () => props.refreshKey,
    async () => {
      await initFormDescription();
      emit('init', collaborationType.value);
    }
  );

  onBeforeMount(async () => {
    await initFormConfig();
    initFormDescription();
  });
</script>

<style lang="less" scoped></style>
