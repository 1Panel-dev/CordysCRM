<template>
  <n-spin :show="loading" class="h-full">
    <CrmDescription :descriptions="realDescriptions" value-align="end" :class="props.class">
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
      <template #[FieldTypeEnum.TEXTAREA]="{ item }">
        <div class="flex w-full items-center justify-between">
          <div class="mr-[4px] whitespace-nowrap text-[var(--text-n2)]">{{ item.label }}</div>
          <div v-html="item.value?.toString().replace(/\n/g, '<br />')"></div>
        </div>
      </template>
      <!-- 链接字段 -->
      <template #[FieldTypeEnum.LINK]="{ item }">
        <div class="flex w-full items-center justify-between">
          <div class="mr-[4px] whitespace-nowrap text-[var(--text-n2)]">{{ item.label }}</div>
          <n-tooltip :delay="300">
            <template #trigger>
              <div class="one-line-text cursor-pointer text-[var(--primary-8)]" @click="openLink(item)">
                {{ item.value }}
              </div>
            </template>
            {{ item.value }}
          </n-tooltip>
        </div>
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
      <template #[FieldTypeEnum.ATTACHMENT]="{ item }">
        <div class="flex w-full items-center justify-between">
          <div class="text-[var(--text-n2)]">{{ item.label }}</div>
          <n-button v-if="item.value.length > 0" type="primary" text @click="openFileListModal(item)">
            {{ t('crm.formDescription.attachmentTip', { count: item.value.length }) }}
          </n-button>
          <div v-else>-</div>
        </div>
      </template>
    </CrmDescription>
  </n-spin>
  <CrmFileListModal v-model:show="showFileListModal" :files="activeFileList" @delete-file="handleDeleteFile" />
</template>

<script setup lang="ts">
  import { NButton, NImage, NImageGroup, NSpace, NSpin, NTooltip } from 'naive-ui';

  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { FieldDataSourceTypeEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmDescription, { Description } from '@/components/pure/crm-description/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmFileListModal from '@/components/business/crm-file-list-modal/index.vue';
  import CrmFormCreateDivider from '@/components/business/crm-form-create/components/basic/divider.vue';
  import dateTime from '../crm-form-create/components/basic/dateTime.vue';

  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAnyPermission } from '@/utils/permission';

  import { AttachmentInfo } from '../crm-form-create/types';

  const props = defineProps<{
    sourceId: string;
    formKey: FormDesignKeyEnum;
    refreshKey?: number;
    class?: string;
    hiddenFields?: string[];
  }>();
  const emit = defineEmits<{
    (e: 'init', collaborationType?: CollaborationType, sourceName?: string, detail?: Record<string, any>): void;
    (e: 'openCustomerDetail', params: { customerId: string; inCustomerPool: boolean; poolId: string }): void;
  }>();

  const { t } = useI18n();

  const needInitDetail = computed(() => true);
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

  const realDescriptions = computed(() => {
    return descriptions.value.filter((item) => {
      return !props.hiddenFields?.includes(item.fieldInfo.id);
    });
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
    emit('openCustomerDetail', {
      customerId: Array.isArray(customerId) ? customerId[0] : customerId,
      inCustomerPool: detail.value.inCustomerPool,
      poolId: detail.value.poolId,
    });
  }

  // 打开链接
  function openLink(item: any) {
    if (item.fieldInfo.openMode === 'openInCurrent') {
      window.location.href = item.value;
    } else {
      window.open(item.value, '_blank');
    }
  }

  const showFileListModal = ref(false);
  const activeFileList = ref<AttachmentInfo[]>([]);
  const activeDescItem = ref<Description>();
  function openFileListModal(item: Description) {
    showFileListModal.value = true;
    activeFileList.value = (item.value as AttachmentInfo[]) || [];
    activeDescItem.value = item;
  }

  function handleDeleteFile(id: string) {
    activeFileList.value = activeFileList.value.filter((file) => file.id !== id);
    if (activeDescItem.value) {
      activeDescItem.value.value = (activeDescItem.value?.value as AttachmentInfo[])?.filter(
        (file: AttachmentInfo) => file.id !== id
      );
    }
    formDetail.value[activeDescItem.value?.fieldInfo.id] = formDetail.value[activeDescItem.value?.fieldInfo.id].filter(
      (e: string) => e !== id
    );
    handleFormChange();
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
