<template>
  <div class="p-[16px]">
    <div class="crm-form-design-config-item">
      <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.formLayout') }}</div>
      <n-radio-group v-model:value="formConfig.layout">
        <n-radio-button :value="1">
          {{ t('crmFormDesign.formLayout1') }}
        </n-radio-button>
        <n-radio-button :value="2">
          {{ t('crmFormDesign.formLayout2') }}
        </n-radio-button>
        <n-radio-button :value="3">
          {{ t('crmFormDesign.formLayout3') }}
        </n-radio-button>
        <n-radio-button :value="4">
          {{ t('crmFormDesign.formLayout4') }}
        </n-radio-button>
      </n-radio-group>
    </div>
    <div class="crm-form-design-config-item">
      <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.formLabelPosition') }}</div>
      <div class="flex gap-[8px]">
        <div
          class="crm-form-design-config-item-label-position"
          :class="formConfig.labelPos === 'top' ? 'crm-form-design-config-item-label-position--active' : ''"
          @click="() => (formConfig.labelPos = 'top')"
        >
          <div class="crm-form-design-config-item-label-position-card">
            <div class="h-[15px] w-[40px] rounded-[var(--border-radius-mini)] bg-[var(--text-n7)]"></div>
            <div class="h-[15px] w-full rounded-[var(--border-radius-mini)] bg-[var(--text-n8)]"></div>
          </div>
          {{ t('crmFormDesign.vertical') }}
        </div>
        <div
          class="crm-form-design-config-item-label-position"
          :class="formConfig.labelPos === 'left' ? 'crm-form-design-config-item-label-position--active' : ''"
          @click="() => (formConfig.labelPos = 'left')"
        >
          <div class="crm-form-design-config-item-label-position-card">
            <div class="flex gap-[4px]">
              <div class="h-[15px] w-[30px] rounded-[var(--border-radius-mini)] bg-[var(--text-n7)]"></div>
              <div class="h-[15px] w-[60px] rounded-[var(--border-radius-mini)] bg-[var(--text-n8)]"></div>
            </div>
            <div class="flex gap-[4px]">
              <div class="h-[15px] w-[30px] rounded-[var(--border-radius-mini)] bg-[var(--text-n7)]"></div>
              <div class="h-[15px] w-[60px] rounded-[var(--border-radius-mini)] bg-[var(--text-n8)]"></div>
            </div>
          </div>
          {{ t('crmFormDesign.horizontal') }}
        </div>
      </div>
    </div>
    <div class="crm-form-design-config-item">
      <div class="crm-form-design-config-item-title">
        {{ t('crmFormDesign.inputWidth') }}
        <n-tooltip trigger="hover">
          <template #trigger>
            <CrmIcon type="iconicon_help_circle" class="cursor-pointer hover:text-[var(--primary-1)]" />
          </template>
          {{ t('crmFormDesign.inputWidthTip') }}
        </n-tooltip>
      </div>
      <n-radio-group v-model:value="formConfig.inputWidth" name="radiogroup" class="flex">
        <n-radio-button value="custom" class="flex-1 text-center">
          {{ t('common.custom') }}
        </n-radio-button>
        <n-radio-button value="full" class="flex-1 text-center">
          {{ t('crmFormDesign.wholeLine') }}
        </n-radio-button>
      </n-radio-group>
    </div>
    <div class="crm-form-design-config-item">
      <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.formActionButton') }}</div>
      <div class="crm-form-design-config-item-input">
        <n-input
          v-model:value="formConfig.optBtnContent[0].text"
          type="text"
          :placeholder="t('common.placeholder')"
          clearable
        />
        <n-switch v-model:value="formConfig.optBtnContent[0].enable" />
      </div>
      <div class="crm-form-design-config-item-input">
        <n-input
          v-model:value="formConfig.optBtnContent[1].text"
          type="text"
          :placeholder="t('common.placeholder')"
          clearable
        />
        <n-switch v-model:value="formConfig.optBtnContent[1].enable" />
      </div>
      <div class="crm-form-design-config-item-input">
        <n-input
          v-model:value="formConfig.optBtnContent[2].text"
          type="text"
          :placeholder="t('common.placeholder')"
          clearable
        />
        <n-switch v-model:value="formConfig.optBtnContent[2].enable" />
      </div>
    </div>
    <div class="crm-form-design-config-item">
      <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.formActionButtonPosition') }}</div>
      <n-radio-group v-model:value="formConfig.optBtnPos" name="radiogroup" class="flex">
        <n-radio-button value="flex-row" class="flex-1 text-center">
          <CrmIcon type="iconicon_align_text_left" />
        </n-radio-button>
        <n-radio-button value="justify-center" class="flex-1 text-center">
          <CrmIcon type="iconicon_align_text_center" />
        </n-radio-button>
        <n-radio-button value="flex-row-reverse" class="flex-1 text-center">
          <CrmIcon type="iconicon_align_text_right" />
        </n-radio-button>
      </n-radio-group>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NInput, NRadioButton, NRadioGroup, NSwitch, NTooltip } from 'naive-ui';

  import { FormConfig } from '@lib/shared/models/system/module';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const formConfig = defineModel<FormConfig>('formConfig', {
    required: true,
  });
</script>

<style lang="less" scoped>
  .crm-form-design-config-item-label-position {
    @apply flex flex-1 cursor-pointer flex-col items-center;

    gap: 4px;
    .crm-form-design-config-item-label-position-card {
      @apply flex w-full flex-col;

      padding: 8px;
      border: 1px solid var(--text-n7);
      border-radius: var(--border-radius-small);
      gap: 4px;
    }
  }
  .crm-form-design-config-item-label-position--active {
    color: var(--primary-8);
    .crm-form-design-config-item-label-position-card {
      border-color: var(--primary-8);
    }
  }
</style>
