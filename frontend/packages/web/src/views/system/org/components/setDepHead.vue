<template>
  <CrmModal
    v-model:show="showModal"
    size="small"
    :title="t('org.setDepartmentHead')"
    :ok-loading="loading"
    @confirm="confirmHandler"
  >
    <div>
      <n-form ref="formRef" :model="form" :rules="rules">
        <n-form-item require-mark-placement="left" label-placement="left" path="users" :label="t('org.head')">
          <n-select
            v-model:value="form.users"
            :placeholder="t('org.selectHeadPlaceholder')"
            multiple
            :options="options"
            :render-label="renderLabel"
            :render-tag="renderTag"
            max-tag-count="responsive"
          />
        </n-form-item>
      </n-form>
    </div>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, FormRules, NForm, NFormItem, NSelect, NTooltip, SelectOption, useMessage } from 'naive-ui';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();
  const Message = useMessage();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const form = ref({
    users: [],
  });

  const rules: FormRules = {
    users: [{ required: true, message: t('org.selectHeadPlaceholder') }],
  };

  const options = ref<SelectOption[]>([
    {
      label: "Everybody's Got ",
      value: 'song0',
    },
    {
      label: 'Drive',
      value: 'song1',
    },
    {
      label: 'Norwegian ',
      value: 'song2',
    },
    {
      label: 'Norwegian ',
      value: 'song22',
    },
    {
      label: 'Norwegian ',
      value: 'song23',
    },
  ]);

  function renderLabel(option: SelectOption) {
    return h(
      NTooltip,
      {
        delay: 300,
        flip: true,
      },
      {
        trigger: () => {
          return h(
            'div',
            {
              class: 'option-content',
            },
            [
              h('div', { class: 'option-label' }, option.label as string),
              h('div', { class: 'option-email' }, option.value),
            ]
          );
        },
        default: () => option.label,
      }
    );
  }

  function renderTag(props: { option: SelectOption; handleClose: () => void }) {
    const { option, handleClose } = props;
    return h(
      CrmTag,
      {
        closable: true,
        onClose: () => handleClose(),
      },
      {
        default: () => option.label,
      }
    );
  }

  const formRef = ref<FormInst | null>(null);
  const loading = ref(false);

  function confirmHandler() {
    formRef.value?.validate((error) => {
      if (!error) {
        try {
          loading.value = true;
          Message.success(t('org.setupSuccess'));
        } catch (e) {
          console.log(e);
        }
      }
    });
  }
</script>

<style lang="less">
  .n-base-select-option__content {
    width: 100%;
  }
  .option-content {
    @apply flex w-full items-center justify-between;
    .option-label {
      color: var(--text-n1);
    }
    .option-email {
      color: var(--text-n4);
    }
  }
</style>
