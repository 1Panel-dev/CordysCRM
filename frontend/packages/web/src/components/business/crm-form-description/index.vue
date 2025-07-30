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
      <template #[FieldDataSourceTypeEnum.CUSTOMER]="{ item }">
        <div class="flex w-full items-center justify-between">
          <div class="text-[var(--text-n2)]">{{ item.label }}</div>
          <CrmTableButton @click="openCustomerDetail(formDetail[item.fieldInfo.id])">
            <template #trigger>
              {{ item.value }}
            </template>
            {{ item.value }}
          </CrmTableButton>
        </div>
      </template>
      <template #[FieldTypeEnum.DATE_TIME]="{ item }">
        <div class="flex w-full items-center justify-between">
          <div class="text-[var(--text-n2)]">{{ item.label }}</div>
          <dateTime
            v-model:value="formDetail[item.fieldInfo.id]"
            :field-config="{
              ...item.fieldInfo,
              showLabel: false,
            }"
            :path="item.fieldInfo.id"
            :disabled="!hasAnyPermission(['OPPORTUNITY_MANAGEMENT:UPDATE'])"
            @change="handleFormChange"
          />
        </div>
      </template>
    </CrmDescription>
  </n-spin>
</template>

<script setup lang="ts">
  import { NImage, NImageGroup, NSpace, NSpin } from 'naive-ui';

  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { FieldDataSourceTypeEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmFormCreateDivider from '@/components/business/crm-form-create/components/basic/divider.vue';
  import dateTime from '../crm-form-create/components/basic/dateTime.vue';

  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    sourceId: string;
    formKey: FormDesignKeyEnum;
    refreshKey?: number;
    class?: string;
  }>();
  const emit = defineEmits<{
    (e: 'init', collaborationType?: CollaborationType, sourceName?: string, detail?: Record<string, any>): void;
    (e: 'openCustomerDetail', customerId: string): void;
  }>();

  const needInitDetail = computed(() => props.formKey === FormDesignKeyEnum.BUSINESS); // TODO:商机需要编辑日期
  const {
    fieldList,
    descriptions,
    loading,
    collaborationType,
    sourceName,
    detail,
    formDetail,
    initFormDetail,
    initFormConfig,
    initFormDescription,
    saveForm,
  } = useFormCreateApi({
    formKey: toRefs(props).formKey,
    sourceId: toRefs(props).sourceId,
    needInitDetail,
  });

  const isInit = ref(false);

  function handleFormChange() {
    nextTick(async () => {
      try {
        if (!isInit.value) return;
        fieldList.value.forEach((item) => {
          if (
            [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.MEMBER, FieldTypeEnum.DEPARTMENT].includes(item.type) &&
            Array.isArray(formDetail.value[item.id])
          ) {
            // 处理数据源字段，单选传单个值
            formDetail.value[item.id] = formDetail.value[item.id]?.[0];
          }
        });
        await saveForm(formDetail.value, false, () => ({}), true);
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    });
  }

  function openCustomerDetail(customerId: string | string[]) {
    emit('openCustomerDetail', Array.isArray(customerId) ? customerId[0] : customerId);
  }

  watch(
    () => props.refreshKey,
    async () => {
      await initFormDetail(true);
      emit('init', collaborationType.value, sourceName.value, detail.value);
    }
  );

  onBeforeMount(async () => {
    await initFormConfig();
    await initFormDetail(true);
    emit('init', collaborationType.value, sourceName.value, detail.value);
    isInit.value = true;
  });

  defineExpose({
    initFormDescription,
  });
</script>

<style lang="less" scoped>
  :deep(.n-form-item-feedback-wrapper) {
    display: none;
  }
</style>
