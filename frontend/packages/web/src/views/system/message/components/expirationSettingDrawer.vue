<template>
  <CrmDrawer
    v-model:show="visible"
    resizable
    no-padding
    :default-width="600"
    :ok-text="t('common.save')"
    :title="t('system.message.expirationTitle', { type: detail?.eventName })"
    @confirm="handleConfirm"
  >
    <n-scrollbar content-class="p-[24px]">
      <div class="bg-[var(--text-n9)] p-[16px]">
        <div class="text-[var(--text-n1)]">
          {{ formTitleName }}
        </div>
        <CrmBatchForm
          ref="batchFormRef"
          class="!p-0"
          :models="formItemModel"
          :default-list="form.list"
          :add-text="t('common.add')"
          validate-when-add
          :max-limit-length="10"
          :disabled-add-tooltip="t('system.message.maxLimit', { count: 10 })"
          @delete-row="handleDelete"
          @save-row="handleSave"
        />
      </div>
      <div class="my-[16px] font-medium text-[var(--text-n1)]">{{ t('system.message.scopedSettings') }}</div>
      <n-form ref="formRef" :model="form" label-placement="left" label-width="auto">
        <n-form-item
          :rule="[{ required: true, message: t('common.notNull', { value: `${t('system.message.noticePerson')}` }) }]"
          require-mark-placement="left"
          label-placement="left"
          path="departmentOwnerIds"
          :label="t('system.message.noticePerson')"
        >
          <CrmUserTagSelector
            v-model:selected-list="form.departmentOwnerIds"
            :member-types="departmentTypes"
            :disabled-node-types="[DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE]"
          />
        </n-form-item>
        <div class="flex flex-col gap-[16px]">
          <n-checkbox v-model:checked="form.supervisorChecked">
            {{ t('system.message.supervisorOfOwner') }}
          </n-checkbox>
          <div class="flex items-center gap-[8px]">
            <n-checkbox v-model:checked="form.departmentOwnerChecked">
              {{ t('system.message.toHeadAbove') }}
            </n-checkbox>
            <CrmInputNumber v-model:value="form.level" max="10000" :precision="0" class="w-[80px]" :min="1" />
            {{ t('system.message.noticeOfDepartmentHead') }}
          </div>
          <div class="flex flex-nowrap items-start gap-[8px]">
            <n-checkbox v-model:checked="form.roleChecked" class="mt-[4px]">
              <div>
                {{ t('org.role') }}
              </div>
            </n-checkbox>
            <n-form-item
              v-if="form.roleChecked"
              :rule="[{ required: true, message: t('common.notNull', { value: `${t('org.role')}` }) }]"
              require-mark-placement="left"
              label-placement="left"
              path="roleIds"
              class="flex-1"
            >
              <CrmUserTagSelector v-model:selected-list="form.roleIds" class="flex-1" :member-types="memberTypes" />
            </n-form-item>
          </div>
        </div>
      </n-form>
    </n-scrollbar>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, NCheckbox, NForm, NFormItem, NScrollbar, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';
  import CrmBatchForm from '@/components/business/crm-batch-form/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  const props = defineProps<{
    detail: any; // TODO: define type xinxinwu
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'ok'): void;
  }>();

  const departmentTypes = [
    {
      label: t('menu.settings.org'),
      value: MemberSelectTypeEnum.ORG,
    },
  ];

  const memberTypes: Option[] = [
    {
      label: t('role.role'),
      value: MemberSelectTypeEnum.ROLE,
    },
  ];

  const initForm = {
    list: [
      {
        endTime: '3,DAY',
      },
    ],
    supervisorChecked: false,
    departmentOwnerChecked: false,
    roleChecked: false,
    roleIds: [],
    level: 1,
    departmentOwnerIds: [
      {
        id: 'OWNER',
        name: t('common.head'),
        disabled: true,
      },
    ],
  };
  // TODO: define type xinxinwu
  const form = ref<any>(cloneDeep(initForm));
  const formTitleName = computed(() =>
    t('system.message.endOfType', {
      type: props.detail?.moduleName,
    })
  );

  const formItemModel = computed(() => [
    {
      path: 'endTime',
      type: FieldTypeEnum.INPUT_NUMBER_WITH_UNIT,
      formItemClass: 'w-full flex-initial',
      inputProps: {
        maxlength: 255,
      },
      rule: [
        {
          required: true,
          message: t('common.notNull', {
            value: formTitleName.value,
          }),
        },
        { notRepeat: true, message: t('system.message.timeNumberRepeatMsg') },
      ],
    },
  ]);

  function handleDelete(_i: number, id: string, done: () => void) {
    done();
  }

  function handleSave(element: Record<string, any>, done: () => void, _i: number, _list?: Record<string, any>[]) {
    done();
  }

  const formRef = ref<FormInst | null>(null);
  const batchFormRef = ref<InstanceType<typeof CrmBatchForm>>();

  function handleConfirm() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        batchFormRef.value?.formValidate((res?: Record<string, any> | undefined) => {
          // TODO: xinxinwu
          try {
            Message.success(t('common.saveSuccess'));
          } catch (e) {
            // eslint-disable-next-line no-console
            console.log(e);
          }
        });
      }
    });
  }
</script>

<style scoped></style>
