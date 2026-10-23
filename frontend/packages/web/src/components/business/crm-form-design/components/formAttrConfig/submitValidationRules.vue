<template>
  <div class="crm-form-design-config-item">
    <div class="flex items-center justify-between">
      <div class="crm-form-design-config-item-title mb-0 flex items-center gap-[4px]">
        {{ t('crmFormDesign.submitValidation') }}
        <n-tooltip trigger="hover">
          <template #trigger>
            <CrmIcon type="iconicon_help_circle" class="cursor-pointer text-[var(--text-n4)]" />
          </template>
          {{ t('crmFormDesign.submitValidationDescription') }}
        </n-tooltip>
      </div>
      <div class="flex items-center gap-[12px]">
        <CrmPopConfirm
          :title="t('crmFormDesign.submitValidationClearTip')"
          :content="t('common.deleteConfirmContent')"
          icon-type="warning"
          :positive-text="t('common.confirm')"
          :negative-text="t('common.cancel')"
          trigger="click"
          :disabled="!rules.length"
          @confirm="rules = []"
        >
          <n-button text type="primary" :disabled="!rules.length">
            {{ t('common.clear') }}
          </n-button>
        </CrmPopConfirm>
        <n-button text type="primary" @click="openCreateRule">
          {{ t('common.add') }}
        </n-button>
      </div>
    </div>

    <div
      v-if="!rules.length"
      class="mt-[8px] flex min-h-[88px] items-center justify-center rounded-[var(--border-radius-small)] bg-[var(--text-n9)]"
    >
      <n-empty :description="t('crmFormDesign.submitValidationEmpty')" />
    </div>
    <VueDraggable
      v-else
      v-model="rules"
      handle=".submit-validation-rule-handle"
      :animation="150"
      class="mt-[8px] flex flex-col gap-[8px]"
    >
      <div
        v-for="(rule, index) in rules"
        :key="getRuleKey(rule)"
        class="flex items-center gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] px-[12px] py-[10px]"
      >
        <CrmIcon
          type="iconicon_move"
          :size="16"
          class="submit-validation-rule-handle shrink-0 cursor-move text-[var(--text-n4)]"
        />
        <n-tooltip trigger="hover">
          <template #trigger>
            <div class="min-w-0 flex-1 truncate">
              {{ rule.message || t('crmFormDesign.submitValidationUnnamed') }}
            </div>
          </template>
          {{ rule.message || t('crmFormDesign.submitValidationUnnamed') }}
        </n-tooltip>
        <n-switch v-model:value="rule.enable" :rubber-band="false" size="small" />
        <n-button quaternary circle size="small" @click="openEditRule">
          <template #icon>
            <CrmIcon type="iconicon_edit" :size="16" />
          </template>
        </n-button>
        <CrmPopConfirm
          :title="t('common.deleteConfirmTitle', { name: rule.message || t('crmFormDesign.submitValidationUnnamed') })"
          :content="t('common.deleteConfirmContent')"
          icon-type="warning"
          :positive-text="t('common.confirmDelete')"
          :negative-text="t('common.cancel')"
          trigger="click"
          @confirm="rules.splice(index, 1)"
        >
          <n-button quaternary circle size="small">
            <template #icon>
              <CrmIcon type="iconicon_delete" :size="16" />
            </template>
          </n-button>
        </CrmPopConfirm>
      </div>
    </VueDraggable>
  </div>

  <CrmModal
    v-model:show="ruleModalVisible"
    :title="t('crmFormDesign.addSubmitValidationRule')"
    :positive-text="t('common.save')"
    :width="960"
    footer
    class="crm-submit-rule-modal"
    @confirm="saveRules"
  >
    <n-form ref="ruleFormRef" :model="draftRules">
      <div class="rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]">
        <div class="grid grid-cols-[16px_minmax(0,1fr)_minmax(0,1fr)_180px_32px_32px] items-center gap-x-[12px]">
          <div></div>
          <div class="text-[var(--text-n1)]">
            <span class="text-[var(--error-red)]">*</span>
            {{ t('crmFormDesign.submitValidationCondition') }}
          </div>
          <div class="text-[var(--text-n1)]">
            <span class="text-[var(--error-red)]">*</span>
            {{ t('crmFormDesign.submitValidationMessage') }}
          </div>
          <div class="text-[var(--text-n1)]">{{ t('crmFormDesign.submitValidationFailureAction') }}</div>
          <div class="text-[var(--text-n1)]">{{ t('common.enable') }}</div>
          <div></div>
        </div>
        <n-scrollbar ref="ruleScrollbarRef" class="mt-[12px] max-h-[50vh]">
          <VueDraggable
            v-model="draftRules"
            handle=".submit-validation-draft-handle"
            :animation="150"
            class="flex flex-col gap-[12px]"
          >
            <div
              v-for="(rule, index) in draftRules"
              :key="rule.key"
              class="grid grid-cols-[16px_minmax(0,1fr)_minmax(0,1fr)_180px_32px_32px] items-start gap-x-[12px]"
            >
              <CrmIcon
                type="iconicon_move"
                :size="16"
                class="submit-validation-draft-handle mt-[8px] cursor-move text-[var(--text-n4)]"
              />
              <n-form-item
                :path="`${index}.formula`"
                class="flex-1"
                :show-label="false"
                :rule="[
                {
                  required: true,
                  validator: (_rule: unknown, value: any) => {
                    try {
                      return !!JSON.parse(value).source
                    } catch (error) {
                      return false
                    }
                  },
                  message: t('common.required'),
                  trigger: ['change', 'blur'],
                },
              ]"
              >
                <n-input
                  :value="getFormulaDisplay(rule.formula)"
                  readonly
                  :placeholder="t('common.setting')"
                  class="cursor-pointer"
                  @click="openRowFormula(index)"
                />
              </n-form-item>
              <n-form-item
                :path="`${index}.message`"
                class="flex-1"
                :show-label="false"
                :rule="[
                {
                  required: true,
                  validator: (_rule: unknown, value: string) => !!value?.trim(),
                  message: t('common.required'),
                  trigger: ['input', 'blur'],
                },
              ]"
              >
                <n-input v-model:value="rule.message" :maxlength="200" :placeholder="t('common.pleaseInput')" />
              </n-form-item>
              <n-select
                :value="rule.allowIgnore ? 'IGNORE' : 'BLOCK'"
                :options="failureActionOptions"
                @update:value="rule.allowIgnore = $event === 'IGNORE'"
              />
              <n-switch v-model:value="rule.enable" :rubber-band="false" class="mt-[6px]" />
              <CrmPopConfirm
                :title="
                  t('common.deleteConfirmTitle', {
                    name: rule.message || t('crmFormDesign.submitValidationUnnamed'),
                  })
                "
                :content="t('common.deleteConfirmContent')"
                icon-type="warning"
                :positive-text="t('common.confirmDelete')"
                :negative-text="t('common.cancel')"
                trigger="click"
                :disabled="draftRules.length <= 1"
                @confirm="draftRules.splice(index, 1)"
              >
                <n-button ghost :disabled="draftRules.length <= 1" class="px-[8px]">
                  <template #icon>
                    <CrmIcon type="iconicon_minus_circle" class="text-[var(--text-n4)]" :size="16" />
                  </template>
                </n-button>
              </CrmPopConfirm>
            </div>
          </VueDraggable>
        </n-scrollbar>
        <n-button type="primary" text class="mt-[12px] h-[22px] w-fit" @click="addDraftRule">
          <template #icon>
            <CrmIcon type="iconicon_add" :size="16" />
          </template>
          {{ t('common.add') }}
        </n-button>
      </div>
    </n-form>
  </CrmModal>

  <formulaModal
    v-model:visible="formulaModalVisible"
    :field-config="formulaFieldConfig"
    :form-fields="props.formFields"
    @save="handleFormulaSave"
  >
    <template #prefix>
      <div class="flex items-center text-[var(--text-n4)]">
        {{ t('crmFormDesign.submitValidationFormulaTip1') }}
        <div class="text-[var(--primary-8)]">TRUE/FALSE:</div>
        {{ t('crmFormDesign.submitValidationFormulaTip2') }}
      </div>
    </template>
  </formulaModal>
</template>

<script setup lang="ts">
  import { computed, nextTick, ref } from 'vue';
  import {
    type FormInst,
    NButton,
    NEmpty,
    NForm,
    NFormItem,
    NInput,
    NScrollbar,
    NSelect,
    NSwitch,
    NTooltip,
    type ScrollbarInst,
  } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';
  import { VueDraggable } from 'vue-draggable-plus';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';
  import type { FormSubmitValidationRule } from '@lib/shared/models/system/module';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';
  import { inputDefaultFieldConfig } from '@/components/business/crm-form-create/config';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';
  import { safeParseFormula } from '@/components/business/crm-formula-editor/utils';
  import formulaModal from './formulaModal.vue';

  const props = defineProps<{
    formFields: FormCreateField[];
  }>();

  const { t } = useI18n();
  const rules = defineModel<FormSubmitValidationRule[]>('rules', { required: true });
  const ruleFormRef = ref<FormInst>();
  const ruleScrollbarRef = ref<ScrollbarInst>();
  const ruleModalVisible = ref(false);
  const formulaModalVisible = ref(false);
  const editingRowIndex = ref(-1);
  const ruleKeys = new WeakMap<FormSubmitValidationRule, string>();

  interface DraftFormSubmitValidationRule extends FormSubmitValidationRule {
    key: string;
  }

  function createEmptyRule(): DraftFormSubmitValidationRule {
    return {
      key: getGenerateId(),
      enable: true,
      formula: '',
      message: '',
      allowIgnore: false,
    };
  }

  const draftRules = ref<DraftFormSubmitValidationRule[]>([createEmptyRule()]);
  const failureActionOptions = computed(() => [
    { label: t('crmFormDesign.submitValidationBlock'), value: 'BLOCK' },
    { label: t('crmFormDesign.submitValidationIgnore'), value: 'IGNORE' },
  ]);
  const formulaFieldConfig = ref<FormCreateField>({
    ...cloneDeep(inputDefaultFieldConfig),
    name: t('crmFormDesign.submitValidationCondition'),
  });

  function getFormulaDisplay(formula?: string) {
    if (!formula) return '';
    const safeFormula = safeParseFormula(formula);
    return safeFormula.display || safeFormula.source || '';
  }

  function getRuleKey(rule: FormSubmitValidationRule) {
    let key = ruleKeys.get(rule);
    if (!key) {
      key = getGenerateId();
      ruleKeys.set(rule, key);
    }
    return key;
  }

  function normalizeRules(source: FormSubmitValidationRule[]) {
    return source
      .filter((rule) => (rule as FormSubmitValidationRule & { enabled?: boolean }).enabled !== false)
      .map((rule) => {
        const legacyRule = rule as FormSubmitValidationRule & {
          condition?: string;
          enabled?: boolean;
          failureAction?: 'BLOCK' | 'IGNORE';
        };
        return {
          key: getGenerateId(),
          enable: typeof rule.enable === 'boolean' ? rule.enable : legacyRule.enabled !== false,
          formula: rule.formula || legacyRule.condition || '',
          message: rule.message || '',
          allowIgnore: typeof rule.allowIgnore === 'boolean' ? rule.allowIgnore : legacyRule.failureAction === 'IGNORE',
        };
      });
  }

  function openCreateRule() {
    draftRules.value = rules.value.length ? normalizeRules(cloneDeep(rules.value)) : [createEmptyRule()];
    ruleModalVisible.value = true;
    nextTick(() => ruleFormRef.value?.restoreValidation());
  }

  function openEditRule() {
    draftRules.value = rules.value.length ? normalizeRules(cloneDeep(rules.value)) : [createEmptyRule()];
    ruleModalVisible.value = true;
    nextTick(() => ruleFormRef.value?.restoreValidation());
  }

  function addDraftRule() {
    ruleFormRef.value?.validate((errors) => {
      if (errors) {
        document.querySelector('.n-form-item-blank--error')?.scrollIntoView({ behavior: 'smooth' });
        return;
      }
      draftRules.value.push(createEmptyRule());
      nextTick(() => {
        ruleScrollbarRef.value?.scrollTo({ top: 99999, behavior: 'smooth' });
      });
    });
  }

  function openRowFormula(index: number) {
    editingRowIndex.value = index;
    const rule = draftRules.value[index];
    formulaFieldConfig.value = {
      ...cloneDeep(inputDefaultFieldConfig),
      name: t('crmFormDesign.submitValidationCondition'),
      formula: rule.formula,
    };
    formulaModalVisible.value = true;
  }

  function handleFormulaSave(formula?: string) {
    if (editingRowIndex.value >= 0 && draftRules.value[editingRowIndex.value]) {
      draftRules.value[editingRowIndex.value].formula = formula || '';
    }
    formulaFieldConfig.value.formula = formula;
    formulaModalVisible.value = false;
    ruleFormRef.value?.restoreValidation();
  }

  function saveRules() {
    ruleFormRef.value?.validate((errors) => {
      if (errors) {
        document.querySelector('.n-form-item-blank--error')?.scrollIntoView({ behavior: 'smooth' });
        return;
      }
      rules.value = draftRules.value.map(({ enable, formula, message, allowIgnore }) => ({
        enable,
        formula,
        message,
        allowIgnore,
      }));
      ruleModalVisible.value = false;
    });
  }
</script>

<style lang="less">
  .crm-submit-rule-modal {
    .n-form-item-feedback-wrapper {
      display: none;
    }
    .n-form-item-blank--error + .n-form-item-feedback-wrapper {
      display: inline-block;
    }
  }
</style>
