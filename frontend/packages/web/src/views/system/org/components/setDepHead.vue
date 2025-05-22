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
        <n-form-item require-mark-placement="left" label-placement="left" path="ownerIds" :label="t('common.head')">
          <CrmUserTagSelector
            v-model:selected-list="form.ownerIds"
            :member-types="[
              {
                label: t('menu.settings.org'),
                value: MemberSelectTypeEnum.ORG,
              },
            ]"
            :multiple="false"
            :api-type-key="MemberApiTypeEnum.SYSTEM_ORG_USER"
            :drawer-title="t('org.setDepartmentHead')"
            :disabled-node-types="[DeptNodeTypeEnum.ORG]"
            :base-params="{ id: props.departmentId }"
          />
        </n-form-item>
      </n-form>
    </div>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, FormRules, NForm, NFormItem, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { SetCommanderForm } from '@lib/shared/models/system/org';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { setCommander } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    departmentId: string;
  }>();

  const emit = defineEmits<{
    (e: 'close'): void;
    (e: 'loadList'): void;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const defaultForm: SetCommanderForm = {
    commanderId: null,
    departmentId: '',
    ownerIds: [],
  };

  const form = ref<SetCommanderForm>(cloneDeep(defaultForm));

  const rules: FormRules = {
    commanderId: [{ required: true, message: t('org.selectHeadPlaceholder') }],
  };

  const formRef = ref<FormInst | null>(null);

  function closeHandler() {
    form.value = cloneDeep(defaultForm);
    emit('close');
  }

  const loading = ref(false);

  function confirmHandler() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          const [commanderItem] = form.value.ownerIds;
          await setCommander({
            commanderId: commanderItem.id,
            departmentId: props.departmentId,
          });
          emit('loadList');
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
