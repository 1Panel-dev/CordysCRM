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
    clearable
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
  >
    <CrmPageWrapper
      :title="
        t('formCreate.pickResource', {
          name: props.dataSourceType ? t(typeLocaleMap[props.dataSourceType]) : '',
        })
      "
      hide-back
    >
      <div class="flex h-full flex-col">
        <van-search v-model="keyword" shape="round" :placeholder="t('customer.searchPlaceholder')" />
        <div class="flex-1 overflow-hidden px-[16px]">
          <CrmSelectList v-model:value="value" v-model:selected-rows="selectedRows"></CrmSelectList>
        </div>
      </div>
      <template #footer>
        <div class="flex items-center gap-[16px]">
          <van-button
            type="default"
            class="crm-button-primary--secondary !rounded-[var(--border-radius-small)] !text-[16px]"
            block
            @click="showPicker = false"
          >
            {{ t('common.cancel') }}
          </van-button>
          <van-button
            type="primary"
            class="!rounded-[var(--border-radius-small)] !text-[16px]"
            block
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

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmSelectList from '@/components/business/crm-select-list/index.vue';

  const props = defineProps<{
    dataSourceType?: FieldDataSourceTypeEnum;
    placeholder?: string;
    disabled?: boolean;
    rules?: FieldRule[];
    id?: string;
    label?: string;
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

  const showPicker = ref(false);
  const keyword = ref('');

  const typeLocaleMap = {
    [FieldDataSourceTypeEnum.CUSTOMER]: 'formCreate .customer',
    [FieldDataSourceTypeEnum.CONTACT]: 'formCreate .contract',
    [FieldDataSourceTypeEnum.BUSINESS]: 'formCreate .business',
    [FieldDataSourceTypeEnum.PRODUCT]: 'formCreate .product',
    [FieldDataSourceTypeEnum.CLUE]: 'formCreate .clue',
    [FieldDataSourceTypeEnum.CUSTOMER_OPTIONS]: '',
  };

  function onConfirm() {
    showPicker.value = false;
    fieldValue.value = selectedRows.value.map((item) => item.name).join('；');
    value.value = selectedRows.value.map((item) => item.id);
    emit('change', value.value);
  }

  onBeforeMount(() => {
    fieldValue.value = selectedRows.value.map((item) => item.name).join('；');
  });
</script>

<style lang="less" scoped></style>
