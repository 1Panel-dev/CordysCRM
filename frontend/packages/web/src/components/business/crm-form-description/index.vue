<template>
  <n-spin :show="loading" class="h-full" :description="props.loadingDescription">
    <CrmDescription
      :descriptions="realDescriptions"
      :value-align="props.valueAlign ?? 'end'"
      :class="[`value-align-${props.valueAlign ?? 'end'}`, props.class]"
      :column="props.column"
      :label-width="props.labelWidth"
      :one-line-label="props.oneLineLabel"
      :one-line-value="props.oneLineValue"
      :tooltip-position="props.tooltipPosition"
    >
      <template #divider="{ item }">
        <CrmFormCreateDivider v-if="item.fieldInfo" :field-config="item.fieldInfo" class="!m-0 w-full" />
      </template>
      <template #image="{ item }">
        <n-image-group v-if="item.value?.length">
          <n-space :class="`${props.valueAlign ?? '!justify-end'}`">
            <n-image
              v-for="img in item.value"
              :key="img"
              :src="`${PreviewPictureUrl}/${img}?userId=${userStore.userInfo.id}`"
              width="40"
              height="40"
            />
          </n-space>
        </n-image-group>
        <data v-else>-</data>
      </template>
      <template #[FieldTypeEnum.INPUT]="{ item }">
        <div class="field-line flex w-full items-center">
          <div
            class="mr-[16px] whitespace-nowrap text-[var(--text-n2)]"
            :style="{ width: props.labelWidth || '120px' }"
          >
            {{ item.label }}
          </div>
          <div>{{ item.value || '-' }}</div>
        </div>
      </template>
      <template #[FieldTypeEnum.TEXTAREA]="{ item }">
        <div class="field-line flex w-full items-start">
          <div
            class="mr-[16px] whitespace-nowrap text-[var(--text-n2)]"
            :style="{ width: props.labelWidth || '120px' }"
          >
            {{ item.label }}
          </div>
          <div v-if="item.value" class="break-all" v-html="item.value?.toString().replace(/\n/g, '<br />')"></div>
          <div v-else>-</div>
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
          <n-tooltip v-if="item.value && item.value !== '-'" :delay="300">
            <template #trigger>
              <div class="one-line-text cursor-pointer text-[var(--primary-8)]" @click="openLink(item)">
                {{ item.value }}
              </div>
            </template>
            {{ item.value }}
          </n-tooltip>
          <div v-else>-</div>
        </div>
      </template>

      <!-- 单选 DataSource -->
      <template #dataSource="{ item }">
        <div class="field-line flex w-full items-center overflow-hidden">
          <div class="mr-[16px] text-[var(--text-n2)]" :style="{ width: props.labelWidth || '120px' }">
            {{ item.label }}
          </div>
          <CrmTableButton
            v-if="canOpenDataSource(item) && item.value !== '-' && item.value"
            class="crm-form-description-link-button flex-1 overflow-hidden"
            :class="`justify-${props.valueAlign ?? 'end'}`"
            @click="openDataSource(item)"
          >
            <template #trigger>
              {{ item.value }}
            </template>
            {{ item.value }}
          </CrmTableButton>
          <n-tooltip
            v-else-if="
              item.value !== undefined && item.value !== null && item.value?.toString() !== '' && item.value !== '-'
            "
            :delay="300"
            :placement="(props.tooltipPosition || item.tooltipPosition) ?? 'top-start'"
            :disabled="item.value === undefined || item.value === null || item.value?.toString() === ''"
          >
            <template #trigger>
              <div class="one-line-text">
                {{ item.value }}
              </div>
            </template>
            {{ item.value }}
          </n-tooltip>
          <div v-else>-</div>
        </div>
      </template>

      <!-- 多选 DataSource -->
      <template #dataSourceMultiple="{ item }">
        <div class="field-line flex w-full overflow-hidden">
          <div class="mr-[16px] text-[var(--text-n2)]" :style="{ width: props.labelWidth || '120px' }">
            {{ item.label }}
          </div>
          <div
            v-if="canOpenDataSource(item) && item.value && item.value.length"
            class="flex flex-1 flex-col items-start gap-[12px] overflow-hidden"
          >
            <CrmTableButton
              v-for="v in item.value"
              :key="v.id || v"
              class="crm-form-description-link-button"
              @click="openDataSource(item, v)"
            >
              <template #trigger>
                {{ v }}
              </template>
              {{ v }}
            </CrmTableButton>
          </div>
          <CrmTagGroup
            v-else-if="Array.isArray(item.value) && item.value.length"
            :tags="item.value"
            :label-key="item.tagProps?.labelKey"
            :class="`justify-${props.valueAlign ?? 'end'}`"
          />
          <div v-else>-</div>
        </div>
      </template>

      <template #[FieldTypeEnum.DATE_TIME]="{ item }">
        <div class="field-line flex w-full items-center">
          <div class="mr-[16px] text-[var(--text-n2)]" :style="{ width: props.labelWidth || '120px' }">
            {{ item.label }}
          </div>
          <CrmDateTime
            v-if="isOpportunityEndDate(item.fieldInfo)"
            v-model:value="formDetail[item.fieldInfo?.id]"
            :field-config="{
              ...item.fieldInfo,
              showLabel: false,
            }"
            :path="item.fieldInfo?.id"
            :disabled="!item.fieldInfo?.editable"
            isDescriptionRender
            :feedback="feedbackMap[item.fieldInfo?.id]"
            needInitDetail
            @change="handleFormChange()"
          />
          <div v-else>{{ item.value || '-' }}</div>
        </div>
      </template>
      <template #[FieldTypeEnum.SELECT]="{ item }">
        <div class="field-line flex w-full items-center">
          <div class="mr-[16px] text-nowrap text-[var(--text-n2)]" :style="{ width: props.labelWidth || '120px' }">
            {{ item.label }}
          </div>
          <CrmTagGroup
            v-if="Array.isArray(item.value) && item.value.length"
            :tags="item.value"
            :label-key="item.tagProps?.labelKey"
            :class="`justify-${props.valueAlign ?? 'end'}`"
          />
          <div v-else>{{ typeof item.value === 'string' ? item.value || '-' : '-' }}</div>
        </div>
      </template>
      <template #[FieldTypeEnum.INPUT_NUMBER]="{ item }">
        <div class="field-line flex w-full items-center">
          <div class="mr-[16px] text-[var(--text-n2)]" :style="{ width: props.labelWidth || '120px' }">
            {{ item.label }}
          </div>
          <div>{{ isNotEmpty(item.value) ? item.value : '-' }}</div>
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
        <div class="field-line flex w-full flex-wrap items-center">
          <div class="w-full text-[var(--text-n2)]">
            {{ item.label }}
          </div>
          <CrmSubTable
            :parent-id="item.key || ''"
            :value="item.value as Record<string, any>[] || []"
            :sub-fields="item.fieldInfo?.subFields || []"
            :fixed-column="item.fieldInfo?.fixedColumn"
            :sum-columns="item.fieldInfo?.sumColumns"
            :optionMap="item.optionMap"
            readonly
            isFormDescription
          />
        </div>
      </template>
      <template #[FieldTypeEnum.SUB_PRODUCT]="{ item }">
        <div class="field-line flex w-full flex-wrap items-center">
          <div class="w-full text-[var(--text-n2)]">
            {{ item.label }}
          </div>
          <CrmSubTable
            :parent-id="item.key || ''"
            :value="item.value as Record<string, any>[] || []"
            :sub-fields="item.fieldInfo?.subFields || []"
            :fixed-column="item.fieldInfo?.fixedColumn"
            :sum-columns="item.fieldInfo?.sumColumns"
            :optionMap="item.optionMap"
            readonly
            isFormDescription
          />
        </div>
      </template>
      <template #[FieldTypeEnum.STATISTIC]="{ item }">
        <div class="field-line flex w-full flex-wrap items-center">
          <div class="pr-[16px] text-[var(--text-n2)]">
            {{ item.label }}
          </div>
          <div class="flex items-center gap-[8px]">
            {{ item.value === null || item.value === undefined ? '-' : item.value }}
            <CrmPopConfirm
              v-if="item.fieldInfo && !item.fieldInfo.resourceFieldId"
              v-model:show="popShow[item.fieldInfo?.id]"
              :title="t('crmFormCreate.reCalculation')"
              icon-type="warning"
              :content="t('crmFormCreate.reCalculationTip')"
              :positive-text="t('common.confirm')"
              trigger="click"
              :negative-text="t('common.cancel')"
              placement="bottom-end"
              @confirm="handleReCalculation(item.fieldInfo, item)"
            >
              <n-button type="warning" quaternary>
                <template #icon><CrmIcon type="iconicon_error_circle_filled" /></template>
                {{ t('crmFormCreate.reCalculation') }}
              </n-button>
            </CrmPopConfirm>
          </div>
        </div>
      </template>
    </CrmDescription>
  </n-spin>
  <CrmFileListModal
    v-model:show="showFileListModal"
    :readonly="props.readonly"
    :files="activeFileList"
    @delete-file="handleDeleteFile"
  />
  <businessTitleDrawer
    v-if="isInitBusinessTitleDetail"
    v-model:visible="showBusinessTitleDetail"
    :source-id="activeBusinessTitleId"
  />
</template>

<script setup lang="ts">
  import { NButton, NImage, NImageGroup, NSpace, NSpin, NTooltip, useMessage } from 'naive-ui';

  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { FieldDataSourceTypeEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ApprovalFieldPermissionModeEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatNumberValueToString } from '@lib/shared/method/formCreate.js';
  import { isNotEmpty } from '@lib/shared/method/is.js';
  import { CollaborationType } from '@lib/shared/models/customer';
  import type { FormConfig } from '@lib/shared/models/system/module';
  import type { ApprovalFieldPermission } from '@lib/shared/models/system/process';

  import CrmDescription, { Description } from '@/components/pure/crm-description/index.vue';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmTagGroup from '@/components/pure/crm-tag-group/index.vue';
  import CrmFileListModal from '@/components/business/crm-file-list-modal/index.vue';
  import CrmDateTime from '@/components/business/crm-form-create/components/basic/dateTime.vue';
  import CrmFormCreateDivider from '@/components/business/crm-form-create/components/basic/divider.vue';
  import { AttachmentInfo, type FormCreateField } from '@/components/business/crm-form-create/types';
  import type { FormulaDataSourceMap } from '@/components/business/crm-formula/formula-runtime/types.js';
  import { safeParseFormula } from '@/components/business/crm-formula-editor/utils.js';
  import CrmSubTable from '@/components/business/crm-sub-table/index.vue';

  import { refreshStatistic } from '@/api/modules/index.js';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useUserStore from '@/store/modules/user';
  import { hasAnyPermission } from '@/utils/permission';

  const businessTitleDrawer = defineAsyncComponent(
    () => import('@/views/contract/businessTitle/components/detail.vue')
  );

  const props = withDefaults(
    defineProps<{
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
      readonly?: boolean;
      loadingDescription?: string;
      oneLineValue?: boolean; // value 是否单行显示
      oneLineLabel?: boolean; // label 是否单行显示
      isContractTableDetail?: boolean; // 只支持合同列表打开的合同详情抽屉高亮跳转
      fieldPermissions?: ApprovalFieldPermission[]; // 字段权限控制
      otherSaveParams?: Record<string, any>;
      customFormId?: string;
      refreshFormConfig?: boolean; // 用于更新后刷新表单(快照)配置
    }>(),
    {
      oneLineLabel: true,
      oneLineValue: true,
    }
  );
  const emit = defineEmits<{
    (
      e: 'init',
      collaborationType?: CollaborationType,
      sourceName?: string,
      detail?: Record<string, any>,
      config?: FormConfig
    ): void;
    (e: 'openCustomerDetail', params: { customerId: string; inCustomerPool: boolean; poolId: string }): void;
    (e: 'openContractDetail', params: { id: string }): void;
    (e: 'openContractPaymentPlanDetail', params: { id: string }): void;
    (e: 'openOpportunityDetail', params: { id: string }): void;
    (e: 'openQuotationDetail', params: { id: string }): void;
    (e: 'refresh', params: { id: string }): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();
  const userStore = useUserStore();

  const needInitDetail = computed(() => true);
  const hiddenFieldByPermission = computed(
    () =>
      props.fieldPermissions
        ?.filter((e) => e.permissionType === ApprovalFieldPermissionModeEnum.HIDDEN)
        .map((e) => e.fieldId) || []
  );
  const { formKey, sourceId, otherSaveParams, customFormId } = toRefs(props);
  const {
    fieldList,
    descriptions,
    loading,
    collaborationType,
    sourceName,
    detail,
    formDetail,
    moduleFormConfig,
    formConfig,
    initFormDetail,
    initFormConfig,
    initFormDescription,
    saveForm,
  } = useFormCreateApi({
    formKey,
    sourceId,
    needInitDetail,
    isContractTableDetail: props.isContractTableDetail,
    otherSaveParams,
    customFormId,
  });

  const realDescriptions = computed(() => {
    return descriptions.value
      .filter(
        (item) =>
          item.fieldInfo &&
          !props.hiddenFields?.includes(item.fieldInfo?.id) &&
          !hiddenFieldByPermission.value?.includes(item.fieldInfo?.id) &&
          item.fieldInfo?.show !== false
      )
      .map((item) => {
        // 独占一行
        if (
          item.fieldInfo &&
          [FieldTypeEnum.TEXTAREA, FieldTypeEnum.DIVIDER, FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(
            item.fieldInfo?.type
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

  const feedbackMap = ref<Record<string, string>>({});

  function isOpportunityEndDate(field?: FormCreateField): field is FormCreateField {
    return (
      props.formKey === FormDesignKeyEnum.BUSINESS && field?.businessKey === 'expectedEndTime' && !field.resourceFieldId
    );
  }

  function validateField(field: FormCreateField) {
    if (isOpportunityEndDate(field)) {
      // 商机结束日期字段仍需校验
      if (field.rules.some((rule) => rule.key === 'required')) {
        const currentValue = formDetail.value[field.id];
        if (
          currentValue === null ||
          currentValue === undefined ||
          (Array.isArray(currentValue) && currentValue.length === 0) ||
          (typeof currentValue === 'string' && currentValue.trim() === '')
        ) {
          Message.warning(t('common.notNull', { value: field.name }));
          feedbackMap.value[field.id] = t('common.notNull', { value: field.name });
          return false;
        }
        feedbackMap.value[field.id] = '';
        return true;
      }
    }
    feedbackMap.value[field.id] = '';
    return true;
  }

  function handleFormChange(callback?: () => void) {
    nextTick(async () => {
      try {
        if (!isInit.value) return;
        let hasErrorField = false;
        for (let i = 0; i < fieldList.value.length; i++) {
          const item = fieldList.value[i];
          if (
            [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.MEMBER, FieldTypeEnum.DEPARTMENT].includes(item.type) &&
            Array.isArray(formDetail.value[item.id])
          ) {
            // 处理数据源字段，单选传单个值
            formDetail.value[item.id] = formDetail.value[item.id]?.[0];
          }
          if (item.subFields?.length) {
            const parentFieldDetail = formDetail.value[item.id];
            if (parentFieldDetail) {
              parentFieldDetail.forEach((subItem: Record<string, any>) => {
                item.subFields?.forEach((subField) => {
                  if ([FieldTypeEnum.DATA_SOURCE].includes(subField.type) && Array.isArray(subItem[subField.id])) {
                    // 处理数据源字段，单选传单个值
                    subItem[subField.id] = subItem[subField.id]?.[0];
                  }
                });
              });
            }
          }
          if (item.show !== false && !validateField(item)) {
            hasErrorField = true;
            break;
          }
        }
        if (!hasErrorField) {
          await saveForm(formDetail.value, false, callback, true);
        }
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

  function openContractPaymentPlanDetail(id: string | string[]) {
    emit('openContractPaymentPlanDetail', {
      id: Array.isArray(id) ? id[0] : id,
    });
  }

  const isInitBusinessTitleDetail = ref(false);
  const showBusinessTitleDetail = ref(false);
  const activeBusinessTitleId = ref<string>('');
  function openContractBusinessTitleDetail(id: string | string[]) {
    activeBusinessTitleId.value = Array.isArray(id) ? id[0] : id;
    isInitBusinessTitleDetail.value = true;
    showBusinessTitleDetail.value = true;
  }

  function openOpportunityDetail(id: string | string[]) {
    emit('openOpportunityDetail', {
      id: Array.isArray(id) ? id[0] : id,
    });
  }

  function openQuotationDetail(id: string | string[]) {
    emit('openQuotationDetail', {
      id: Array.isArray(id) ? id[0] : id,
    });
  }

  type DataSourceConfig = {
    canOpen: (item: Description) => boolean;
    open: (id: string | string[]) => void;
  };
  const dataSourceConfig: Partial<Record<FieldDataSourceTypeEnum, DataSourceConfig>> = {
    [FieldDataSourceTypeEnum.CUSTOMER]: {
      canOpen: (item: Description) => {
        const { value } = item;

        const valid = value !== t('common.optionNotExist') && value !== '-';

        if (!valid) return false;

        if (detail.value.inCustomerPool) {
          return hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:READ']);
        }

        return hasAnyPermission(['CUSTOMER_MANAGEMENT:READ']);
      },
      open: openCustomerDetail,
    },

    [FieldDataSourceTypeEnum.CONTRACT]: {
      canOpen: () => hasAnyPermission(['CONTRACT:READ']),
      open: openContractDetail,
    },

    [FieldDataSourceTypeEnum.CONTRACT_PAYMENT]: {
      canOpen: () => hasAnyPermission(['CONTRACT_PAYMENT_PLAN:READ']),
      open: openContractPaymentPlanDetail,
    },

    [FieldDataSourceTypeEnum.BUSINESS_TITLE]: {
      canOpen: () => hasAnyPermission(['CONTRACT_BUSINESS_TITLE:READ']),
      open: openContractBusinessTitleDetail,
    },

    [FieldDataSourceTypeEnum.QUOTATION]: {
      canOpen: () => hasAnyPermission(['OPPORTUNITY_QUOTATION:READ']),
      open: openQuotationDetail,
    },

    [FieldDataSourceTypeEnum.BUSINESS]: {
      canOpen: () => hasAnyPermission(['OPPORTUNITY_MANAGEMENT:READ']),
      open: openOpportunityDetail,
    },
  };

  function canOpenDataSource(item: Description) {
    const config = dataSourceConfig[item.fieldInfo?.dataSourceType as FieldDataSourceTypeEnum];

    if (!config) return false;

    return config.canOpen?.(item);
  }

  function openDataSource(item: Description, value?: string) {
    const config = dataSourceConfig[item.fieldInfo?.dataSourceType as FieldDataSourceTypeEnum];

    if (!config) return;
    const option = item.fieldInfo?.initialOptions?.find((i: { id: string; name: string }) => i.name === value);

    const id = option?.id ?? formDetail.value[item.fieldInfo?.id || ''];
    config.open(id);
  }

  // 打开链接
  function openLink(item: any) {
    if (item.fieldInfo?.openMode === 'openInCurrent') {
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
    formDetail.value[activeDescItem.value?.fieldInfo?.id || ''] = formDetail.value[
      activeDescItem.value?.fieldInfo?.id || ''
    ].filter((e: string) => e !== id);
    handleFormChange();
  }

  const popShow = ref<Record<string, boolean>>({});
  async function handleReCalculation(field: FormCreateField, value: Description) {
    try {
      const res = await refreshStatistic(props.sourceId, field.id);
      if (res === null) {
        value.value = '-';
      } else {
        value.value = formatNumberValueToString(res, field);
      }
      popShow.value[field.id] = false;
      Message.success(t('common.refreshSuccess'));
      emit('refresh', { id: field.id });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function refreshFormDescription() {
    if (props.refreshFormConfig) {
      await initFormConfig();
    }
    await initFormDetail(true);
    emit('init', collaborationType.value, sourceName.value, detail.value, formConfig.value);
  }

  watch(
    () => props.refreshKey,
    () => {
      refreshFormDescription();
    }
  );

  const formulaDataSource = ref<FormulaDataSourceMap>({});

  const evaluationNow = ref<Date | null>(new Date());

  const formulaFormContext = computed(() => ({
    fields: fieldList.value,
    formulaDataSource: formulaDataSource.value,
    evaluationNow,
  }));

  provide('formFieldsProvider', readonly(formulaFormContext));

  function initFormulaDataSourceRemark() {
    formulaDataSource.value = {};
    const fieldMap = new Map(fieldList.value.map((item) => [item.id, item]));
    fieldList.value.forEach((item) => {
      if ([FieldTypeEnum.FORMULA, FieldTypeEnum.INPUT, FieldTypeEnum.SERIAL_NUMBER].includes(item.type)) {
        const { fields } = safeParseFormula(item.formula ?? '');
        fields?.forEach((e: any) => {
          let options = [];
          const targetField = fieldMap.get(e.fieldId);

          if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(e.fieldType)) {
            options = targetField?.initialOptions ?? [];
          } else if (e.fieldType === FieldTypeEnum.SELECT) {
            options = targetField?.options ?? [];
          } else {
            return;
          }

          formulaDataSource.value[e.fieldId] = {
            parserName: true,
            options,
          };
        });
      }
    });
  }

  onBeforeMount(async () => {
    await initFormConfig();
    await initFormDetail(true);
    descriptions.value.forEach((e) => {
      if (e.fieldInfo?.type === FieldTypeEnum.STATISTIC) {
        popShow.value[e.fieldInfo?.id || ''] = false;
      }
    });
    initFormulaDataSourceRemark();
    emit('init', collaborationType.value, sourceName.value, detail.value, formConfig.value);
    isInit.value = true;
  });

  defineExpose({
    initFormDescription,
    handleFormChange,
    moduleFormConfig,
    refreshFormDescription,
  });
</script>

<style lang="less">
  .crm-description {
    .crm-form-description-link-button {
      height: 20px !important;
      font-size: 14px !important;
    }
  }
</style>

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
