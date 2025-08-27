<template>
  <CrmModal
    v-model:show="visible"
    :title="t('workbench.duplicateCheck.searchConfig')"
    :positive-text="t('common.save')"
    :negative-text="t('common.reset')"
    :width="816"
  >
    <searchSetting
      ref="searchSettingRef"
      v-model:form-model="formModel"
      is-not-required-validate
      :scoped-options="lastScopedOptions"
      :search-field-map="props.searchFieldMap"
    />
    <div class="mt-[16px] text-[var(--text-n1)]">{{ t('workbench.duplicateCheck.filterResultSort') }}</div>
    <div class="mt-[8px]">
      <VueDraggable
        v-model="configList"
        :animation="150"
        draggable=".draggable"
        handle=".handle"
        class="flex gap-[16px]"
      >
        <CrmTag
          v-for="(item, index) of configList"
          :key="`${item.value}-${index}`"
          class="draggable !px-[12px]"
          size="large"
        >
          <template #icon>
            <CrmIcon type="iconicon_move" :size="16" class="handle cursor-move text-[var(--text-n4)]" />
          </template>
          {{ item.label }}
        </CrmTag>
      </VueDraggable>
    </div>
    <template #footer>
      <div class="flex w-full items-center justify-between">
        <div class="ml-[4px] flex items-center gap-[8px]">
          <n-switch v-model:value="formModel.resultDisplay" :rubber-band="false" />
          {{ t('workbench.duplicateCheck.showHasResultTable') }}
        </div>
        <div class="flex items-center justify-end gap-[12px]">
          <n-button :disabled="loading" secondary @click="handleReset">
            {{ t('common.reset') }}
          </n-button>
          <n-button
            :loading="loading"
            type="primary"
            :disabled="
              !Object.values(formModel.searchFields).some(
                (e) => e !== null && e !== undefined && Array.isArray(e) && e.length >= 1
              )
            "
            @click="handleConfirm"
          >
            {{ t('common.save') }}
          </n-button>
        </div>
      </div>
    </template>
  </CrmModal>
</template>

<script setup lang="ts">
  import { NButton, NSwitch, useMessage } from 'naive-ui';
  import { VueDraggable } from 'vue-draggable-plus';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import searchSetting from './searchSetting.vue';

  import { resetSearchConfig, searchConfig } from '@/api/modules';

  import type { DefaultSearchSetFormModel, ScopedOptions } from '../config';
  import { defaultSearchSetFormModel, lastScopedOptions, scopedOptions } from '../config';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    searchFieldMap: Record<string, FilterFormItem[]>;
    init: (isInit?: boolean, isReset?: boolean) => Promise<void>;
  }>();

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const visible = defineModel<boolean>('visible', { required: true });

  const configList = defineModel<ScopedOptions[]>('configList', {
    default: () => scopedOptions,
  });

  const formModel = defineModel<DefaultSearchSetFormModel>('formModel', {
    required: true,
    default: () => defaultSearchSetFormModel,
  });

  const searchSettingRef = ref<InstanceType<typeof searchSetting>>();
  const loading = ref(false);
  function handleConfirm() {
    searchSettingRef.value?.formRef?.validate(async (errors) => {
      if (!errors) {
        loading.value = true;
        try {
          await searchConfig({
            ...formModel.value,
            sortSetting: configList.value.map((e) => e.value),
          });
          Message.success(t('common.saveSuccess'));
          emit('refresh');
          visible.value = false;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        } finally {
          loading.value = false;
        }
      }
    });
  }

  async function handleReset() {
    searchSettingRef.value?.formRef?.restoreValidation();
    try {
      await resetSearchConfig();
      props.init(true, true);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        props.init(false);
      }
    }
  );
</script>

<style scoped></style>
