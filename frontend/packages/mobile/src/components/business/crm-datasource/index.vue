<template>
  <van-field
    v-model="fieldValue"
    :label="props.label"
    :name="props.id"
    :rules="props.rules"
    is-link
    readonly
    :placeholder="props.placeholder || t('common.pleaseSelect')"
    :disabled="props.disabled"
    :class="props.class"
    type="textarea"
    rows="1"
    autosize
    @click="showPicker = true"
    @update:model-value="($event) => emit('change', $event)"
  >
  </van-field>
  <van-popup
    v-model:show="showPicker"
    destroy-on-close
    round
    position="bottom"
    safe-area-inset-top
    safe-area-inset-bottom
    class="h-[100vh]"
  >
    <CrmPageWrapper
      :title="
        t('datasource.pickResource', {
          name: props.dataSourceType ? t(typeLocaleMap[props.dataSourceType]) : '',
        })
      "
      hide-back
    >
      <div class="flex h-full flex-col overflow-hidden">
        <van-search
          v-model="keyword"
          class="crm-datasource-search"
          shape="round"
          :placeholder="
            t('datasource.searchPlaceholder', {
              name: props.dataSourceType ? t(typeLocaleMap[props.dataSourceType]) : '',
            })
          "
          @search="search"
        />
        <div class="flex-1 overflow-hidden px-[16px]">
          <CrmSelectList
            v-if="props.dataSourceType"
            ref="crmSelectListRef"
            v-model:value="pickerValue"
            v-model:selected-rows="pickerSelectedRows"
            :multiple="props.multiple"
            :keyword="keyword"
            :load-list-api="sourceApi[props.dataSourceType]"
            :transform="selectListTransform"
            :no-page-nation="props.noPageNation"
          ></CrmSelectList>
        </div>
      </div>
      <template #footer>
        <div class="flex items-center gap-[16px]">
          <van-button
            type="default"
            class="crm-button-primary--secondary !rounded-[var(--border-radius-small)] !text-[16px]"
            block
            @click="onCancel"
          >
            {{ t('common.cancel') }}
          </van-button>
          <van-button
            type="primary"
            class="!rounded-[var(--border-radius-small)] !text-[16px]"
            block
            :disabled="!pickerSelectedRows.length"
            @click="onConfirm"
          >
            {{ t('common.confirm') }}
          </van-button>
        </div>
      </template>
    </CrmPageWrapper>
  </van-popup>
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';

  import { FieldDataSourceTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CommonList } from '@lib/shared/models/common';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmSelectList from '@/components/business/crm-select-list/index.vue';

  import {
    getCustomerOptions,
    getFieldClueList,
    getFieldContactList,
    getFieldCustomerList,
    getFieldOpportunityList,
    getFieldProductList,
    getUserOptions,
  } from '@/api/modules';

  const props = defineProps<{
    dataSourceType?: FieldDataSourceTypeEnum;
    placeholder?: string;
    disabled?: boolean;
    rules?: FieldRule[];
    id?: string;
    label?: string;
    multiple?: boolean;
    noPageNation?: boolean;
    disabledSelection?: (item: Record<string, any>) => boolean;
    class?: string;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string | string[]): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<string | string[]>('value', {
    default: '',
  });
  const selectedRows = defineModel<Record<string, any>[]>('selectedRows', {
    default: [],
  });

  const fieldValue = ref('');
  const pickerValue = ref<string[]>([]);
  const pickerSelectedRows = ref<Record<string, any>[]>([]);
  const crmSelectListRef = ref<InstanceType<typeof CrmSelectList>>();
  const showPicker = ref(false);
  const keyword = ref('');

  const typeLocaleMap = {
    [FieldDataSourceTypeEnum.CUSTOMER]: 'formCreate.customer',
    [FieldDataSourceTypeEnum.CONTACT]: 'formCreate.contract',
    [FieldDataSourceTypeEnum.BUSINESS]: 'formCreate.business',
    [FieldDataSourceTypeEnum.PRODUCT]: 'formCreate.product',
    [FieldDataSourceTypeEnum.CLUE]: 'formCreate.clue',
    [FieldDataSourceTypeEnum.CUSTOMER_OPTIONS]: '',
    [FieldDataSourceTypeEnum.USER_OPTIONS]: '',
  };

  const sourceApi: Record<FieldDataSourceTypeEnum, (data: any) => Promise<CommonList<any>>> = {
    [FieldDataSourceTypeEnum.BUSINESS]: getFieldOpportunityList,
    [FieldDataSourceTypeEnum.CLUE]: getFieldClueList,
    [FieldDataSourceTypeEnum.CONTACT]: getFieldContactList,
    [FieldDataSourceTypeEnum.CUSTOMER]: getFieldCustomerList,
    [FieldDataSourceTypeEnum.PRODUCT]: getFieldProductList,
    [FieldDataSourceTypeEnum.CUSTOMER_OPTIONS]: getCustomerOptions,
    [FieldDataSourceTypeEnum.USER_OPTIONS]: getUserOptions,
  };

  function onConfirm() {
    showPicker.value = false;
    selectedRows.value = pickerSelectedRows.value;
    fieldValue.value = pickerSelectedRows.value.map((item) => item.name).join('；');
    value.value = props.multiple ? pickerSelectedRows.value.map((item) => item.id) : pickerSelectedRows.value[0].id;
    emit('change', value.value);
  }

  function onCancel() {
    pickerValue.value = Array.isArray(value.value) ? value.value : [value.value];
    showPicker.value = false;
  }

  function selectListTransform(item: Record<string, any>) {
    return {
      ...item,
      disabled: props.disabledSelection ? props.disabledSelection(item) : false,
      checked: pickerSelectedRows.value.some((row) => row.id === item.id),
    };
  }

  function search() {
    crmSelectListRef.value?.filterListByKeyword('name');
  }

  watch(
    () => selectedRows.value,
    (val) => {
      fieldValue.value = val
        .filter((e) => value.value.includes(e.id))
        .map((item) => item.name)
        .join('；');
      pickerSelectedRows.value = val;
    },
    {
      immediate: true,
    }
  );

  watch(
    () => value.value,
    (val) => {
      pickerValue.value = Array.isArray(val) ? val : [val];
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped>
  .crm-datasource-search {
    :deep(.van-cell) {
      &:last-child::before {
        @apply !hidden;
      }
    }
  }
</style>
