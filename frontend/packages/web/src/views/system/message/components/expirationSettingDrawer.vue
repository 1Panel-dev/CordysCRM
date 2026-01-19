<template>
  <CrmDrawer
    v-model:show="visible"
    resizable
    no-padding
    :default-width="600"
    :ok-text="t('common.save')"
    :title="t('system.message.expirationTitle', { type: detail?.eventName })"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <n-scrollbar content-class="p-[24px]">
      <div v-if="props.showTimeSetting" class="mb-[16px] font-medium text-[var(--text-n1)]">
        {{ t('system.message.timeSetting') }}
      </div>
      <div v-if="props.showTimeSetting" class="bg-[var(--text-n9)] p-[16px]">
        <CrmBatchForm
          ref="batchFormRef"
          class="!p-0"
          :models="formItemModel"
          :default-list="form.list"
          :add-text="t('common.add')"
          validate-when-add
          :max-limit-length="10"
          :need-init-form-row="false"
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
          path="userIds"
          :label="t('system.message.noticePerson')"
        >
          <CrmUserTagSelector
            v-model:selected-list="form.userIds"
            :member-types="departmentTypes"
            :disabled-node-types="[DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE]"
          />
        </n-form-item>
        <div class="flex flex-col gap-[16px]">
          <div class="flex items-center gap-[8px]">
            <n-checkbox v-model:checked="form.ownerEnable">
              {{ t('system.message.toHeadAbove') }}
            </n-checkbox>
            <CrmInputNumber v-model:value="form.ownerLevel" max="10000" :precision="0" class="w-[80px]" :min="0" />
            {{ t('system.message.noticeOfDepartmentHead') }}
            <n-tooltip placement="bottom">
              <template #trigger>
                <CrmIcon
                  type="iconicon_help_circle"
                  :size="16"
                  class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                />
              </template>
              {{ t('system.message.departmentHeadTooltip') }}
            </n-tooltip>
          </div>
          <div class="flex flex-nowrap items-start gap-[8px]">
            <n-checkbox v-model:checked="form.roleEnable" class="mt-[4px]">
              <div>
                {{ t('org.role') }}
              </div>
            </n-checkbox>
            <n-form-item
              v-if="form.roleEnable"
              :rule="[{ required: true, message: t('common.notNull', { value: `${t('org.role')}` }) }]"
              require-mark-placement="left"
              label-placement="left"
              path="roleIds"
              class="flex-1"
            >
              <CrmUserTagSelector
                v-model:selected-list="form.roleIds"
                :disabledList="form.roleIds"
                class="flex-1"
                :member-types="memberTypes"
              />
            </n-form-item>
          </div>
        </div>
      </n-form>
    </n-scrollbar>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { FormInst, NCheckbox, NForm, NFormItem, NScrollbar, NTooltip, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { MessageConfigItem, SaveMessageConfigParams } from '@lib/shared/models/system/message';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';
  import CrmBatchForm from '@/components/business/crm-batch-form/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { getMessageTaskConfigDetail, saveMessageTask } from '@/api/modules';

  const props = defineProps<{
    detail?: MessageConfigItem;
    showTimeSetting?: boolean;
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
    ownerEnable: false,
    roleEnable: false,
    roleIds: [],
    ownerLevel: 1,
    userIds: [
      {
        id: 'OWNER',
        name: t('common.head'),
        disabled: true,
      },
    ],
  };

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

  function handleCancel() {
    form.value = cloneDeep(initForm);
    visible.value = false;
  }

  const formRef = ref<FormInst | null>(null);
  const batchFormRef = ref<InstanceType<typeof CrmBatchForm>>();

  async function saveHandler(res?: Record<string, any>) {
    try {
      const params: SaveMessageConfigParams = {
        config: {
          ...form.value,
          timeList: props.showTimeSetting
            ? res?.list?.map((e: any) => {
                const arr = e.endTime.split(',');
                return {
                  timeValue: arr[0],
                  timeUnit: arr[1],
                };
              })
            : [],
          userIds: form.value.userIds?.map((e: any) => e.id),
          roleIds: form.value.roleIds?.map((e: any) => e.id),
        },
        ...props.detail?.messageTaskDetailDTOList.find((e) => e.event === props.detail?.event),
        module: props.detail?.module ?? '',
        event: props.detail?.event ?? '',
      };
      await saveMessageTask(params);
      handleCancel();
      emit('ok');
      Message.success(t('common.saveSuccess'));
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  function handleConfirm() {
    formRef.value?.validate((error) => {
      if (!error) {
        if (props.showTimeSetting) {
          batchFormRef.value?.formValidate(async (res?: Record<string, any> | undefined) => {
            saveHandler(res);
          });
        } else {
          saveHandler();
        }
      }
    });
  }

  async function getConfigDetail() {
    try {
      const result = await getMessageTaskConfigDetail({
        event: props.detail?.event ?? '',
        module: props.detail?.module ?? '',
      });
      if (!result) {
        form.value = cloneDeep(initForm);
        return;
      }
      form.value = {
        ...result,
        list: result?.timeList?.map((e) => ({
          endTime: `${e.timeValue},${e.timeUnit}`,
        })),
        userIds: result.userIdNames?.map((e: { id: string; name: string }) => ({
          ...e,
          disabled: e.id === 'OWNER',
        })),
        roleIds: result.roleIdNames?.map((e: { id: string; name: string }) => ({
          ...e,
        })),
      };
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        getConfigDetail();
      }
    }
  );
</script>

<style scoped></style>
