<template>
  <van-field
    v-model="displayValue"
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :name="props.fieldConfig.id"
    :rules="props.fieldConfig.rules as FieldRule[]"
    is-link
    readonly
    type="textarea"
    rows="1"
    autosize
    :placeholder="props.fieldConfig.placeholder || t('common.pleaseSelect')"
    :disabled="props.fieldConfig.editable === false"
    @click="handleClick"
  >
  </van-field>
  <van-popup v-model:show="showPicker" destroy-on-close round position="bottom">
    <div class="flex h-[var(--van-picker-toolbar-height)] items-center justify-between">
      <CrmTextButton
        color="var(--van-picker-cancel-action-color)"
        :text="t('common.cancel')"
        class="p-[var(--van-picker-action-padding)] !text-[14px]"
        @click="showPicker = false"
      />
      <CrmTextButton
        class="p-[var(--van-picker-action-padding)] !text-[14px]"
        :text="t('common.confirm')"
        @click="onConfirm"
      />
    </div>
    <div class="h-[260px] overflow-auto">
      <van-checkbox-group v-model="selectedValues">
        <van-cell-group inset>
          <van-cell
            v-for="item in props.fieldConfig.options"
            :key="item.value"
            clickable
            :title="item.label"
            @click="toggle(item.value as string)"
          >
            <template #right-icon>
              <van-checkbox :name="item.value" shape="square" @click.stop />
            </template>
          </van-cell>
        </van-cell-group>
      </van-checkbox-group>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
  import { FieldRule } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string[]): void;
  }>();

  const { t } = useI18n();

  const value = defineModel<string[]>('value', {
    default: [],
  });

  const displayValue = computed(() => {
    return props.fieldConfig.options
      ?.filter((opt) => value.value.includes(opt.value as string))
      .map((opt) => opt.label)
      .join('；');
  });

  const showPicker = ref(false);
  const selectedValues = ref<string[]>([]);

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (Array.isArray(val)) {
        value.value = val;
        selectedValues.value = val;
      }
    },
    { immediate: true }
  );

  function handleClick() {
    if (props.fieldConfig.editable) {
      selectedValues.value = [...value.value];
      showPicker.value = true;
    }
  }

  function toggle(val: string) {
    if (selectedValues.value.includes(val)) {
      selectedValues.value = selectedValues.value.filter((v) => v !== val);
    } else {
      selectedValues.value.push(val);
    }
  }

  function onConfirm() {
    value.value = [...selectedValues.value];
    emit('change', value.value);
    showPicker.value = false;
  }
</script>

<style lang="less" scoped></style>
