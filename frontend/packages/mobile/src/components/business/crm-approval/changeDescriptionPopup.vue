<template>
  <van-popup
    v-model:show="show"
    position="bottom"
    :style="{ height: '100vh' }"
    safe-area-inset-top
    safe-area-inset-bottom
  >
    <div class="relative p-[16px] text-center">
      <CrmTextButton
        icon="iconicon_chevron_left"
        icon-size="24px"
        color="var(--text-n1)"
        class="absolute left-[12px] top-[16px]"
        @click="show = false"
      />
      <div class="text-[16px] font-semibold">{{ t('crm.approval.change') }}</div>
    </div>
    <van-form ref="formRef" required>
      <van-field
        v-model="comment"
        name="comment"
        :label="t('crm.approval.changeDescription')"
        :placeholder="t('common.pleaseInput')"
        type="textarea"
        autosize
        maxlength="300"
        show-word-limit
        required
        :rules="[{ required: true, message: t('common.notNull', { value: t('crm.approval.changeDescription') }) }]"
      />
    </van-form>
    <div class="fixed bottom-0 left-0 right-0 flex items-center gap-[16px] p-[16px]">
      <van-button
        type="default"
        class="flex-1 !border-none !bg-[var(--text-n8)] !text-[var(--text-n1)]"
        @click="show = false"
      >
        {{ t('common.cancel') }}
      </van-button>
      <van-button type="primary" class="flex-1" @click="handleConfirm">
        {{ t('crm.approval.confirmChange') }}
      </van-button>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
  import { showToast, type FormInstance } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  const emit = defineEmits<{
    (e: 'confirm', comment: string): void;
  }>();

  const { t } = useI18n();
  const show = defineModel<boolean>('show', { default: false });
  const formRef = ref<FormInstance>();
  const comment = ref('');

  async function handleConfirm() {
    try {
      await formRef.value?.validate();
      const value = comment.value.trim();
      if (!value) {
        showToast(t('common.notNull', { value: t('crm.approval.changeDescription') }));
        return;
      }
      emit('confirm', value);
      show.value = false;
      comment.value = '';
    } catch {
      return;
    }
  }

  watch(show, (visible) => {
    if (!visible) {
      comment.value = '';
    }
  });
</script>
