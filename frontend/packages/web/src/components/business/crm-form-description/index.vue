<template>
  <n-spin :show="loading" class="h-full">
    <CrmDescription :descriptions="descriptions" value-align="end" :class="props.class">
      <template #divider="{ item }">
        <CrmFormCreateDivider :field-config="item.fieldInfo" class="!m-0 w-full" />
      </template>
      <template #image="{ item }">
        <n-image-group>
          <n-space class="!justify-end">
            <n-image v-for="img in item.value" :key="img" :src="`${PreviewPictureUrl}/${img}`" width="40" height="40" />
          </n-space>
        </n-image-group>
      </template>
    </CrmDescription>
  </n-spin>
</template>

<script setup lang="ts">
  import { NImage, NImageGroup, NSpace, NSpin } from 'naive-ui';

  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmFormCreateDivider from '@/components/business/crm-form-create/components/basic/divider.vue';

  import useFormCreateApi from '@/hooks/useFormCreateApi';

  const props = defineProps<{
    sourceId: string;
    formKey: FormDesignKeyEnum;
    refreshKey?: number;
    class?: string;
  }>();
  const emit = defineEmits<{
    (e: 'init', collaborationType?: CollaborationType, sourceName?: string): void;
  }>();

  const { descriptions, loading, collaborationType, sourceName, initFormConfig, initFormDescription } =
    useFormCreateApi({
      formKey: toRefs(props).formKey,
      sourceId: toRefs(props).sourceId,
    });

  watch(
    () => props.refreshKey,
    async () => {
      await initFormDescription();
      emit('init', collaborationType.value, sourceName.value);
    }
  );

  onBeforeMount(async () => {
    await initFormConfig();
    await initFormDescription();
    emit('init', collaborationType.value, sourceName.value);
  });

  defineExpose({
    initFormDescription,
  });
</script>

<style lang="less" scoped></style>
