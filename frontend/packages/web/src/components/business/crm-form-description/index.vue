<template>
  <n-spin :show="loading" class="h-full" :description="props.loadingDescription">
    <CrmDescription
      :descriptions="realDescriptions"
      :value-align="props.valueAlign ?? 'end'"
      :class="[`value-align-${props.valueAlign ?? 'end'}`, props.class]"
      :column="props.column"
      :label-width="props.labelWidth"
      :tooltip-position="props.tooltipPosition"
    >
      <template #divider="{ item }">
        <CrmFormCreateDivider :field-config="item.fieldInfo" class="!m-0 w-full" />
      </template>
      <template #image="{ item }">
        <n-image-group>
          <n-space :class="`${props.valueAlign ?? '!justify-end'}`">
            <n-image v-for="img in item.value" :key="img" :src="`${PreviewPictureUrl}/${img}`" width="40" height="40" />
          </n-space>
        </n-image-group>
      </template>
      <template #[FieldTypeEnum.TEXTAREA]="{ item }">
        <div
          class="field-line flex w-full"
          :class="props.column && props.column > 1 ? 'items-baseline' : 'items-center'"
        >
          <div
            class="mr-[16px] whitespace-nowrap text-[var(--text-n2)]"
            :style="{ width: props.labelWidth || '120px' }"
          >
            {{ item.label }}
          </div>
          <div v-html="item.value?.toString().replace(/\n/g, '<br />')"></div>
        </div>
      </template>
      <!-- 链接字段 -->
      <template #[FieldTypeEnum.LINK]="{ item }">
        <div class="field-line flex w-full items-center">
          <div
            class="mr-[16px] whitespace-nowrap text-[var(--text-n2)]"
            :style="{ width: props.labelWidth || '120px' }"
          >
            {{ item.label }}
          </div>
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
        <div class="field-line flex w-full items-center">
          <div class="mr-[16px] text-[var(--text-n2)]" :style="{ width: props.labelWidth || '120px' }">
            {{ item.label }}
          </div>
          <CrmTableButton @click="openCustomerDetail(formDetail[item.fieldInfo.id])">
            <template #trigger>
              {{ item.value }}
            </template>
            {{ item.value }}
          </CrmTableButton>
        </div>
      </template>
      <template #[FieldDataSourceTypeEnum.CONTRACT]="{ item }">
        <div class="field-line flex w-full items-center">
          <div class="mr-[16px] text-[var(--text-n2)]" :style="{ width: props.labelWidth || '120px' }">
            {{ item.label }}
          </div>
          <CrmTableButton @click="openContractDetail(formDetail[item.fieldInfo.id])">
            <template #trigger>
              {{ item.value }}
            </template>
            {{ item.value }}
          </CrmTableButton>
        </div>
      </template>
      <template #[FieldTypeEnum.DATE_TIME]="{ item }">
        <div class="field-line flex w-full items-center">
          <div class="mr-[16px] text-[var(--text-n2)]" :style="{ width: props.labelWidth || '120px' }">
            {{ item.label }}
          </div>
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
        <div class="field-line flex w-full items-center">
          <div class="mr-[16px] text-[var(--text-n2)]" :style="{ width: props.labelWidth || '120px' }">
            {{ item.label }}
          </div>
          <n-button v-if="item.value.length > 0" type="primary" text @click="openFileListModal(item)">
            {{ t('crm.formDescription.attachmentTip', { count: item.value.length }) }}
          </n-button>
          <div v-else>-</div>
        </div>
      </template>
      <template #[FieldTypeEnum.SUB_PRICE]="{ item }">
        <CrmSubTable
          :parent-id="item.key || ''"
          :value="item.value as Record<string, any>[] || []"
          :sub-fields="item.fieldInfo.subFields"
          :fixed-column="item.fieldInfo.fixedColumn"
          :sum-columns="item.fieldInfo.sumColumns"
          :optionMap="item.optionMap"
          readonly
        />
      </template>
      <template #[FieldTypeEnum.SUB_PRODUCT]="{ item }">
        <CrmSubTable
          :parent-id="item.key || ''"
          :value="item.value as Record<string, any>[] || []"
          :sub-fields="item.fieldInfo.subFields"
          :fixed-column="item.fieldInfo.fixedColumn"
          :sum-columns="item.fieldInfo.sumColumns"
          :optionMap="item.optionMap"
          readonly
        />
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
  import CrmSubTable from '@/components/business/crm-sub-table/index.vue';
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
    column?: number;
    valueAlign?: 'center' | 'start' | 'end';
    labelWidth?: string;
    tooltipPosition?:
      | 'top-start'
      | 'top'
      | 'top-end'
      | 'right-start'
      | 'right'
      | 'right-end'
      | 'bottom-start'
      | 'bottom'
      | 'bottom-end'
      | 'left-start'
      | 'left'
      | 'left-end'
      | undefined;
    loadingDescription?: string;
  }>();
  const emit = defineEmits<{
    (e: 'init', collaborationType?: CollaborationType, sourceName?: string, detail?: Record<string, any>): void;
    (e: 'openCustomerDetail', params: { customerId: string; inCustomerPool: boolean; poolId: string }): void;
    (e: 'openContractDetail', params: { id: string }): void;
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
    moduleFormConfig,
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
    return descriptions.value
      .filter((item) => !props.hiddenFields?.includes(item.fieldInfo.id))
      .map((item) => {
        // 独占一行
        if (
          [FieldTypeEnum.TEXTAREA, FieldTypeEnum.DIVIDER, FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(
            item.fieldInfo.type
          )
        ) {
          const extraClass = props.column && props.column > 1 ? '!w-full' : '';
          return {
            ...item,
            class: [item.class, extraClass].filter(Boolean).join(' '), // 合并 class
          };
        }
        return item;
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

  function openContractDetail(id: string | string[]) {
    emit('openContractDetail', {
      id: Array.isArray(id) ? id[0] : id,
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
    moduleFormConfig,
  });
</script>

<style lang="less" scoped>
  :deep(.n-form-item-feedback-wrapper) {
    display: none;
  }
  .value-align-start .field-line {
    justify-content: flex-start;
  }
  .value-align-center .field-line {
    justify-content: center;
  }
  .value-align-end .field-line {
    justify-content: space-between;
  }
</style>
