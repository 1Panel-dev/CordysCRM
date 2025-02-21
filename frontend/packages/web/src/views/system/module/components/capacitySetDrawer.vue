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
  import { useMessage } from 'naive-ui';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmBatchForm from '@/components/business/crm-batch-form/index.vue';
  import type { FormItemModel } from '@/components/business/crm-batch-form/types';
  import { FieldTypeEnum } from '@/components/business/crm-form-create/enum';

  import { getCapacityPage, saveCapacity } from '@/api/modules/system/module';
  import { useI18n } from '@/hooks/useI18n';

  import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';

  const Message = useMessage();

  const { t } = useI18n();

  const props = defineProps<{
    title: string;
    type: ModuleConfigEnum;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const loading = ref(false);

  const batchFormRef = ref<InstanceType<typeof CrmBatchForm>>();

  const form = ref<any>({ list: [] });

  const formItemModel: Ref<FormItemModel[]> = ref([
    {
      path: 'members',
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
      path: 'capacity',
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
    try {
      const capacities = form.value.list.map((item: Record<string, any>) => {
        return {
          scopeIds: item.members.map((memberItem: SelectedUsersItem) => memberItem.id),
          capacity: item.capacity,
        };
      });
      await saveCapacity({ capacities }, props.type);
      Message.success(t('common.saveSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function confirm() {
    userFormValidate(capacitySet);
  }

  watch(
    () => visible.value,
    async (newVal) => {
      if (newVal) {
        try {
          loading.value = true;
          form.value.list = await getCapacityPage(props.type);
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        } finally {
          loading.value = false;
        }
      } else {
        form.value.list = [];
      }
    }
  );
</script>
