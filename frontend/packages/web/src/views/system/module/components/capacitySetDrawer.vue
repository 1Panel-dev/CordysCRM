<template>
  <CrmDrawer
    v-model:show="visible"
    :width="800"
    :title="props.title"
    :ok-text="t('common.save')"
    :loading="loading"
    @confirm="confirm"
  >
    <CrmBatchForm
      ref="batchFormRef"
      :models="formItemModel"
      :default-list="form.list"
      :add-text="t('module.businessManage.addRules')"
      validate-when-add
    ></CrmBatchForm>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { cloneDeep } from 'lodash-es';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmBatchForm from '@/components/business/crm-batch-form/index.vue';
  import type { FormItemModel } from '@/components/business/crm-batch-form/types';
  import { FieldTypeEnum } from '@/components/business/crm-form-create/enum';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const props = defineProps<{
    title: string;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const loading = ref(false);

  const batchFormRef = ref<InstanceType<typeof CrmBatchForm>>();

  const defaultForm = {
    list: [
      {
        member: [],
        Maximum: 33,
      },
    ],
  };
  const form = ref<any>(cloneDeep(defaultForm));

  const formItemModel: Ref<FormItemModel[]> = ref([
    {
      path: 'member',
      type: FieldTypeEnum.USER_TAG_SELECTOR,
      label: t('module.capacitySet.departmentOrMember'),
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('module.capacitySet.departmentOrMember') }),
        },
        { notRepeat: true, message: t('module.capacitySet.repeatMsg') },
      ],
    },
    {
      path: 'Maximum',
      type: FieldTypeEnum.INPUT_NUMBER,
      label: t('module.capacitySet.Maximum'),
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('module.capacitySet.Maximum') }),
        },
      ],
      formItemClass: 'w-[120px] flex-initial',
      numberProps: {
        placeholder: t('module.capacitySet.MaximumPlaceholder'),
      },
    },
  ]);

  function userFormValidate(cb: () => Promise<any>) {
    batchFormRef.value?.formValidate(async (batchForm?: Record<string, any>) => {
      try {
        loading.value = true;
        form.value.list = batchForm?.list;
        await cb();
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      } finally {
        loading.value = false;
      }
    });
  }

  async function capacitySet() {
    // eslint-disable-next-line no-console
    console.log('🤔️ =>', form.value);
  }

  function confirm() {
    userFormValidate(capacitySet);
  }
</script>
