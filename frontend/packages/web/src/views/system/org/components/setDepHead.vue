<template>
  <CrmModal
    v-model:show="showModal"
    size="small"
    :title="t('org.setDepartmentHead')"
    :ok-loading="loading"
    @confirm="confirmHandler"
    @cancel="closeHandler"
  >
    <div>
      <n-form ref="formRef" :model="form" :rules="rules">
        <n-form-item require-mark-placement="left" label-placement="left" path="commanderId" :label="t('org.head')">
          <CrmUserSelect
            v-model:value="form.commanderId"
            :placeholder="t('org.selectHeadPlaceholder')"
            value-field="id"
            label-field="name"
            mode="remote"
            :fetch-api="getUserOptions"
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
  import { FormInst, FormRules, NForm, NFormItem, NTooltip, SelectOption, useMessage } from 'naive-ui';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmUserSelect from '@/components/business/crm-user-select/index.vue';

  import { getUserOptions, setCommander } from '@/api/modules/system/org';
  import { useI18n } from '@/hooks/useI18n';

  import type { SetCommanderParams } from '@lib/shared/models/system/org';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    departmentId: string;
  }>();

  const emit = defineEmits<{
    (e: 'close'): void;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const form = ref<SetCommanderParams>({
    commanderId: null,
    departmentId: '',
  });

  const rules: FormRules = {
    commanderId: [{ required: true, message: t('org.selectHeadPlaceholder') }],
  };

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
              class: `option-content ${option.id === form.value.commanderId ? 'option-content-selected' : ''}`,
            },
            [
              h('div', { class: 'option-label' }, { default: () => option.name }),
              h('div', { class: 'option-email' }, { default: () => option.id }),
            ]
          );
        },
        default: () => option.name,
      }
    );
  }

  function renderTag(infoProps: { option: SelectOption; handleClose: () => void }) {
    const { option, handleClose } = infoProps;
    return h(
      CrmTag,
      {
        closable: true,
        onClose: () => handleClose(),
      },
      {
        default: () => option.name,
      }
    );
  }

  const formRef = ref<FormInst | null>(null);

  function closeHandler() {
    form.value.commanderId = null;
    emit('close');
  }

  const loading = ref(false);

  function confirmHandler() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          await setCommander({
            ...form.value,
            departmentId: props.departmentId,
          });
          closeHandler();
          Message.success(t('org.setupSuccess'));
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          loading.value = false;
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
  .option-content-selected {
    .option-label {
      color: var(--primary-8);
    }
    .option-email {
      color: var(--primary-8);
    }
  }
</style>
