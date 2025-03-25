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
          <n-input v-model:value="form.subject" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item require-mark-placement="left" label-placement="left" path="url" :label="t('system.message.href')">
          <n-input v-model:value="form.url" type="text" :placeholder="t('common.pleaseInput')" />
          <div class="text-[var(--text-n4)]">{{ t('system.message.descContent') }}</div>
        </n-form-item>
        <!-- TODO 加字段 -->
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
            :placeholder="t('system.message.hrefRename')"
          />
        </n-form-item>
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="content"
          :label="t('system.message.announcementContent')"
        >
          <n-input v-model:value="form.content" type="textarea" :placeholder="t('common.pleaseInput')" />
          <CrmPopConfirm
            v-model:show="showPopModal"
            :title="form.subject"
            icon-type="warning"
            :positive-text="null"
            :negative-text="null"
            placement="bottom-end"
          >
            <n-button type="primary" text @click="() => (showPopModal = true)">
              {{ t('system.message.preview') }}
            </n-button>
            <template #content>
              <div class="p-[16px]">
                <div>{{ form.content }}</div>
                <div class="cursor-pointer text-[var(--primary-8)]" @click="goUrl">
                  {{ form.renameUrl ?? form.url }}
                </div>
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
          <n-date-picker v-model:value="form.range" class="w-[240px]" type="daterange" clearable>
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
          path="userIds"
          :label="t('system.message.receiver')"
        >
          <CrmUserSelect
            v-model:value="form.userIds"
            value-field="id"
            label-field="name"
            mode="remote"
            :multiple="true"
            filterable
            :fetch-api="getUserOptions"
          />
        </n-form-item>
      </n-form>
    </div>
  </CrmModal>
</template>

<script lang="ts" setup>
  import { FormInst, FormRules, NButton, NDatePicker, NForm, NFormItem, NInput, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import type { AnnouncementSaveParams } from '@lib/shared/models/system/message';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';
  import CrmUserSelect from '@/components/business/crm-user-select/index.vue';

  import { addAnnouncement, getAnnouncementDetail, updateAnnouncement } from '@/api/modules/system/message';
  import { getUserOptions } from '@/api/modules/system/org';
  import { useI18n } from '@/hooks/useI18n';
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

  const rules: FormRules = {
    subject: [{ required: true, message: t('common.notNull', { value: `${t('system.message.announcementTitle')}` }) }],
    content: [
      { required: true, message: t('common.notNull', { value: `${t('system.message.announcementContent')}` }) },
    ],
    range: [{ required: true, message: t('common.pleaseSelect') }],
    userIds: [{ required: true, message: t('common.pleaseSelect') }],
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
    roleIds: [],
    userIds: [],
    range: undefined,
    renameUrl: '', // TODO 加字段
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
          const params = {
            ...form.value,
            organizationId: appStore.orgId,
            startTime: form.value.range ? form.value.range[0] : 0,
            endTime: form.value.range ? form.value.range[1] : 0,
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
        const {
          subject,
          startTime,
          endTime,
          url,
          renameUrl,
          userIdName,
          roleIdName,
          deptIdName,
          organizationId,
          id,
          contentText,
        } = result;

        form.value = {
          id,
          subject,
          startTime,
          endTime,
          url,
          renameUrl,
          organizationId,
          userIds: userIdName?.map((e) => e.id),
          roleIds: roleIdName?.map((e) => e.id),
          deptIds: deptIdName?.map((e) => e.id),
          content: contentText,
          range: [startTime, endTime],
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
