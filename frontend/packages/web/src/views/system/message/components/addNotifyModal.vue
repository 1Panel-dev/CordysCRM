<template>
  <CrmModal
    v-model:show="showModal"
    :title="form.id ? t('system.message.updateAnnouncement') : t('system.message.newAnnouncement')"
    :ok-loading="loading"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <div>
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="left"
        :label-width="100"
        require-mark-placement="left"
      >
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="subject"
          :label="t('system.message.announcementTitle')"
        >
          <n-input v-model:value="form.subject" type="text" :maxlength="255" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item require-mark-placement="left" label-placement="left" path="url" :label="t('system.message.href')">
          <n-input v-model:value="form.url" :maxlength="255" type="text" :placeholder="t('common.pleaseInput')" />
          <div class="text-[var(--text-n4)]">{{ t('system.message.descContent') }}</div>
        </n-form-item>
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="rename"
          :label="t('system.message.hrefName')"
        >
          <n-input
            v-model:value="form.renameUrl"
            :disabled="!form.url"
            type="text"
            :maxlength="255"
            :placeholder="t('system.message.hrefRename')"
          />
        </n-form-item>
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="content"
          :label="t('system.message.announcementContent')"
        >
          <n-input
            v-model:value="form.content"
            :maxlength="1000"
            type="textarea"
            :placeholder="t('common.pleaseInput')"
          />
          <CrmPopConfirm
            v-model:show="showPopModal"
            :title="form.subject"
            icon-type="warning"
            :show-arrow="false"
            :positive-text="null"
            :negative-text="null"
            placement="right"
            :show-close="true"
          >
            <n-button type="primary" text @click="() => (showPopModal = true)">
              {{ t('system.message.preview') }}
            </n-button>
            <template #content>
              <div class="p-[16px]">
                {{ form.content }}
                <n-tooltip trigger="hover" :delay="300" :disabled="!(form.renameUrl || form.url)?.length">
                  <template #trigger>
                    <n-button class="inline-block" text type="primary" @click="goUrl">
                      <div class="one-line-text max-w-[300px]">
                        {{ form.renameUrl || form.url }}
                      </div>
                    </n-button>
                  </template>
                  {{ form.renameUrl ?? form.url }}
                </n-tooltip>
              </div>
            </template>
          </CrmPopConfirm>
        </n-form-item>
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="range"
          :label="t('system.message.timeOfPublication')"
        >
          <n-date-picker
            v-model:value="form.range"
            :is-date-disabled="isRangeDateDisabled"
            :is-time-disabled="isRangeTimeDisabled"
            class="w-[340px]"
            type="datetimerange"
            clearable
          >
            <template #date-icon>
              <CrmIcon class="text-[var(--text-n4)]" type="iconicon_time" :size="16" />
            </template>
            <template #separator>
              <div class="text-[var(--text-n4)]">{{ t('common.to') }}</div>
            </template>
          </n-date-picker>
        </n-form-item>
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="ownerIds"
          :label="t('system.message.receiver')"
        >
          <CrmUserTagSelector
            v-model:selected-list="form.ownerIds"
            :api-type-key="MemberApiTypeEnum.SYSTEM_ROLE"
            :member-types="memberTypes"
          />
        </n-form-item>
      </n-form>
    </div>
  </CrmModal>
</template>

<script lang="ts" setup>
  import { FormInst, FormRules, NButton, NDatePicker, NForm, NFormItem, NInput, NTooltip, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AnnouncementSaveParams } from '@lib/shared/models/system/message';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { addAnnouncement, getAnnouncementDetail, updateAnnouncement } from '@/api/modules';
  import useAppStore from '@/store/modules/app';

  const appStore = useAppStore();

  const Message = useMessage();

  const { t } = useI18n();

  const props = defineProps<{
    id?: string;
  }>();

  const emit = defineEmits<{
    (e: 'saved'): void;
    (e: 'cancel'): void;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
  });

  const memberTypes: Option[] = [
    {
      label: t('menu.settings.org'),
      value: MemberSelectTypeEnum.ORG,
    },
  ];

  // 禁用当前时间之前的日期
  const isRangeDateDisabled = (currentTimestamp: number, _type: 'start' | 'end', _range: [number, number] | null) => {
    const now = new Date();
    const target = new Date(currentTimestamp);

    // 禁用今天之前的所有日期
    return target < new Date(now.getFullYear(), now.getMonth(), now.getDate());
  };

  // 禁用当前时间之前的小时/分钟/秒
  const isRangeTimeDisabled = (current: number, type: 'start' | 'end', range: [number, number]) => {
    const now = new Date();
    const currentDate = new Date(current);

    const isToday =
      currentDate.getFullYear() === now.getFullYear() &&
      currentDate.getMonth() === now.getMonth() &&
      currentDate.getDate() === now.getDate();

    if (!isToday) {
      // 今天以外的时间不限制
      return {
        isHourDisabled: () => false,
        isMinuteDisabled: () => false,
        isSecondDisabled: () => false,
      };
    }

    const nowHour = now.getHours();
    const nowMinute = now.getMinutes();
    const nowSecond = now.getSeconds();

    return {
      isHourDisabled: (hour: number) => hour < nowHour,
      isMinuteDisabled: (minute: number) => {
        return currentDate.getHours() === nowHour && minute < nowMinute;
      },
      isSecondDisabled: (second: number) => {
        return currentDate.getHours() === nowHour && currentDate.getMinutes() === nowMinute && second < nowSecond;
      },
    };
  };

  const rules: FormRules = {
    subject: [{ required: true, message: t('common.notNull', { value: `${t('system.message.announcementTitle')}` }) }],
    content: [
      { required: true, message: t('common.notNull', { value: `${t('system.message.announcementContent')}` }) },
    ],
    range: [{ required: true, message: t('common.pleaseSelect') }],
    ownerIds: [{ required: true, message: t('common.pleaseSelect') }],
  };

  const initForm: AnnouncementSaveParams = {
    id: '',
    subject: '',
    content: '',
    startTime: 0,
    endTime: 0,
    url: '',
    organizationId: '',
    deptIds: [],
    userIds: [],
    range: undefined,
    renameUrl: '',
    ownerIds: [],
  };

  const form = ref<AnnouncementSaveParams>(cloneDeep(initForm));

  const loading = ref<boolean>(false);
  const formRef = ref<FormInst | null>(null);

  function handleCancel() {
    showModal.value = false;
    form.value = cloneDeep(initForm);
    emit('cancel');
  }

  async function handleConfirm() {
    formRef.value?.validate(async (errors) => {
      if (!errors) {
        try {
          loading.value = true;
          const { ownerIds, range } = form.value;
          const deptIds = ownerIds.filter((item: any) => item.nodeType === DeptNodeTypeEnum.ORG).map((item) => item.id);
          const userIds = ownerIds.filter((item: any) => item.nodeType !== DeptNodeTypeEnum.ORG).map((item) => item.id);
          const [startTime, endTime] = range || [0, 0];
          const params = {
            ...form.value,
            deptIds,
            userIds,
            startTime,
            endTime,
            organizationId: appStore.orgId,
          };
          if (form.value.id) {
            await updateAnnouncement(params);
            Message.success(t('common.updateSuccess'));
          } else {
            await addAnnouncement(params);
            Message.success(t('common.addSuccess'));
          }
          loading.value = false;
          emit('saved');
          handleCancel();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      }
    });
  }

  async function getDetail() {
    if (props.id) {
      try {
        const result = await getAnnouncementDetail(props.id);
        const { subject, startTime, endTime, url, renameUrl, userIdName, deptIdName, organizationId, id, contentText } =
          result;

        form.value = {
          id,
          subject,
          startTime,
          endTime,
          url,
          renameUrl,
          organizationId,
          userIds: userIdName?.map((e) => e.id),
          deptIds: deptIdName?.map((e) => e.id),
          content: contentText,
          range: [startTime, endTime],
          ownerIds: [...(deptIdName || []), ...(userIdName || [])],
        };
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    }
  }

  const showPopModal = ref(false);

  function goUrl() {
    window.open(form.value.url, '_blank');
  }

  watch(
    () => showModal.value,
    (val) => {
      if (val) {
        getDetail();
      }
    }
  );
</script>

<style lang="less" scoped></style>
