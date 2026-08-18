<template>
  <CrmDrawer
    v-model:show="showDrawer"
    :title="isEdit ? t('system.business.term.updateTerm') : t('system.business.term.addTerm')"
    :width="680"
    :ok-text="isEdit ? t('common.update') : t('common.add')"
    :show-continue="!isEdit"
    :loading="saving"
    @confirm="submit(false)"
    @continue="submit(true)"
    @cancel="resetFormState()"
  >
    <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" require-mark-placement="right">
      <n-form-item :label="t('system.business.term.category')" path="catalogId">
        <n-select
          v-model:value="form.catalogId"
          filterable
          tag
          clearable
          :options="categoryOptions"
          :placeholder="t('common.selectOrCreateOption')"
          :render-label="renderCategoryLabel"
        />
      </n-form-item>
      <n-form-item :label="t('system.business.term.standardTerm')" path="standardTerm">
        <n-input
          v-model:value="form.standardTerm"
          clearable
          :maxlength="255"
          :placeholder="t('system.business.term.standardTermPlaceholder')"
        />
      </n-form-item>
      <n-form-item :label="t('system.business.term.synonyms')" path="alsoCalled">
        <n-input
          v-model:value="form.alsoCalled"
          clearable
          :maxlength="255"
          :placeholder="t('system.business.term.synonymsPlaceholder')"
        />
      </n-form-item>
      <n-form-item :label="t('system.business.term.forbiddenWords')" path="avoidThese">
        <n-input
          v-model:value="form.avoidThese"
          clearable
          :maxlength="255"
          :placeholder="t('system.business.term.forbiddenWordsPlaceholder')"
        />
      </n-form-item>
      <n-form-item :label="t('system.business.term.scenes')" path="useCase">
        <n-input
          v-model:value="form.useCase"
          clearable
          :maxlength="255"
          :placeholder="t('system.business.term.scenesPlaceholder')"
        />
      </n-form-item>
      <n-form-item :label="t('system.business.term.systemMapping')" path="systemReference">
        <n-input
          v-model:value="form.systemReference"
          clearable
          :maxlength="255"
          :placeholder="t('common.pleaseInput')"
        />
      </n-form-item>
    </n-form>

    <template #footerLeft>
      <div class="flex items-center gap-[8px]">
        <n-switch v-model:value="form.enable" :rubber-band="false" />
        <span>{{ t('system.business.term.enableTerm') }}</span>
      </div>
    </template>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { computed, h, reactive, ref, watch } from 'vue';
  import { FormInst, NForm, NFormItem, NInput, NSelect, NSwitch, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { TermCategoryItem, TermItem, TermParams } from '@lib/shared/models/system/term';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  import { addTerm, addTermCategory, adoptTermDiscovery, getTermCategoryList, updateTerm } from '@/api/modules';

  import type { FormRules, SelectOption } from 'naive-ui';

  const props = defineProps<{
    term?: Partial<TermItem> | null;
    categories: TermCategoryItem[];
    activeCategoryId?: string;
    adoptDiscoveryId?: string;
  }>();

  const emit = defineEmits<{
    (e: 'saved', refreshId?: string, adoptDiscoveryId?: string): void;
  }>();

  const showDrawer = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const { t } = useI18n();
  const Message = useMessage();

  const formRef = ref<FormInst | null>(null);
  function getDefaultForm() {
    return {
      id: undefined,
      catalogId: props.activeCategoryId || '',
      standardTerm: '',
      alsoCalled: '',
      avoidThese: '',
      useCase: '',
      systemReference: '',
      enable: true,
    };
  }
  const form = reactive<TermParams>(getDefaultForm());

  const isEdit = computed(() => !!props.term?.id);

  const categoryOptions = computed<SelectOption[]>(() =>
    props.categories.map((category) => ({
      label: category.name,
      value: category.id,
    }))
  );

  function renderCategoryLabel(option: SelectOption) {
    const isExistingOption = props.categories.some((category) => category.id === option.value);

    if (isExistingOption) {
      return option.label as string;
    }

    return h('span', { class: 'inline-flex items-center gap-[8px]' }, [
      h('span', { class: 'text-[var(--text-n4)]' }, t('common.createOption')),
      h('span', option.label as string),
    ]);
  }

  const rules: FormRules = {
    catalogId: [
      {
        required: true,
        message: t('common.notNull', { value: t('system.business.term.category') }),
        trigger: ['blur', 'change'],
      },
      {
        validator: (_rule, value: string) => {
          const matchedCategory = props.categories.some((category) => category.id === value);
          return matchedCategory || !value || value.length <= 255;
        },
        message: t('system.business.term.categoryMaxLength'),
        trigger: ['blur', 'change'],
      },
    ],
    standardTerm: [
      {
        required: true,
        message: t('common.notNull', { value: t('system.business.term.standardTerm') }),
        trigger: ['blur', 'input'],
      },
    ],
  };

  function resetFormState(term?: Partial<TermItem>) {
    Object.assign(form, {
      ...getDefaultForm(),
      ...(term ?? {}),
    });
    formRef.value?.restoreValidation();
  }

  async function getPayload() {
    if (!form.catalogId) {
      return { ...form };
    }

    const matchedCategory = props.categories.find((category) => category.id === form.catalogId);
    let catalogId = matchedCategory?.id;
    if (!catalogId) {
      await addTermCategory({ name: form.catalogId });
      const categories = await getTermCategoryList();
      catalogId = categories.find((category) => category.name === form.catalogId)?.id;
    }

    return {
      ...form,
      catalogId: catalogId || '',
    };
  }

  watch(
    () => showDrawer.value,
    (visible) => {
      if (visible) {
        resetFormState(props.term ? props.term : undefined);
      }
    }
  );

  const saving = ref(false);
  async function submit(continueAdd: boolean) {
    await formRef.value?.validate();
    try {
      saving.value = true;
      let refreshId = form.id;
      if (form.id) {
        await updateTerm(await getPayload());
        Message.success(t('common.updateSuccess'));
      } else {
        const payload = await getPayload();
        if (props.adoptDiscoveryId) {
          await adoptTermDiscovery({
            ...payload,
            id: props.adoptDiscoveryId,
            catalogId: payload.catalogId || '',
          });
        } else {
          await addTerm(payload);
        }
        refreshId = undefined;
        Message.success(t('common.addSuccess'));
      }
      const nextCatalogId = form.catalogId;
      emit('saved', refreshId, props.adoptDiscoveryId);
      if (!continueAdd) {
        showDrawer.value = false;
      } else {
        resetFormState({ catalogId: nextCatalogId });
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      saving.value = false;
    }
  }
</script>
