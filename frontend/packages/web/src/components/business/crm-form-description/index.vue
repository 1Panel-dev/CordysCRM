<template>
  <n-spin :show="loading">
    <CrmDescription :descriptions="descriptions">
      <template #divider="{ item }">
        <CrmFormCreateDivider :field-config="item.fieldInfo" />
      </template>
    </CrmDescription>
  </n-spin>
</template>

<script setup lang="ts">
  import { NSpin } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmFormCreateDivider from '@/components/business/crm-form-create/components/basic/divider.vue';

  import useFormCreateApi from '@/hooks/useFormCreateApi';

  const props = defineProps<{
    sourceId: string;
    formKey: FormDesignKeyEnum;
  }>();

  const { descriptions, loading, initFormConfig, initFormDescription } = useFormCreateApi({
    formKey: toRefs(props).formKey,
    sourceId: toRefs(props).sourceId,
  });

  onBeforeMount(async () => {
    await initFormConfig();
    initFormDescription();
  });
</script>

<style lang="less" scoped></style>
