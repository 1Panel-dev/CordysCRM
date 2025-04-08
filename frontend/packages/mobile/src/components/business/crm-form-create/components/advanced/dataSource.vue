<template>
  <van-field
    v-model:value="fieldValue"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rule="props.fieldConfig.rules"
    is-link
    readonly
    :placeholder="props.fieldConfig.placeholder"
    :disabled="props.fieldConfig.editable === false"
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
    <CrmPageWrapper :title="t('formCreate.pickResource')" hide-back>
      <div class="flex h-full flex-col">
        <van-search
          v-model="keyword"
          shape="round"
          :placeholder="t('customer.searchPlaceholder')"
          class="flex-1 !p-0"
        />
        <div class="flex-1 overflow-hidden">
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
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmSelectList from '@/components/business/crm-select-list/index.vue';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string | string[]): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<string | string[]>('value', {
    default: '',
  });
  const selectedRows = ref<Record<string, any>[]>(props.fieldConfig.initialOptions || []);
  const fieldValue = ref('');

  const showPicker = ref(false);
  const keyword = ref('');

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      value.value = val;
    },
    {
      immediate: true,
    }
  );

  function onConfirm() {
    showPicker.value = false;
    fieldValue.value = selectedRows.value.map((item) => item.name).join('；');
    value.value = selectedRows.value.map((item) => item.id);
    emit('change', value.value);
  }
</script>

<style lang="less" scoped></style>
