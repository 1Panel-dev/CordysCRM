<template>
  <CrmDrawer
    v-model:show="showDrawer"
    :title="
      !form.id ? t('system.business.authenticationSettings.add') : t('system.business.authenticationSettings.edit')
    "
    :width="680"
    :show-continue="!form.id"
    :ok-text="form.id ? t('common.update') : undefined"
    @confirm="confirm"
    @cancel="cancel"
    @continue="confirm(true)"
  >
    <n-form ref="formRef" :model="form" require-mark-placement="left" label-placement="left" label-width="100px">
      <n-form-item
        path="name"
        :label="t('common.name')"
        :rule="[{ required: true, message: t('common.notNull', { value: t('common.name') }) }]"
      >
        <n-input
          v-model:value="form.name"
          class="!w-[80%]"
          allow-clear
          :max-length="255"
          :placeholder="t('common.pleaseInput')"
        />
      </n-form-item>
      <n-form-item path="description" :label="t('common.desc')">
        <n-input
          v-model:value="form.description"
          :max-length="1000"
          class="!w-[80%]"
          type="textarea"
          :placeholder="t('common.pleaseInput')"
          allow-clear
        />
      </n-form-item>
      <!-- 先不上 -->
      <!-- <n-form-item path="type" :label="t('system.business.authenticationSettings.addResource')">
        <CrmTab v-model:active-tab="form.type" :tab-list="tabList" type="segment" />
      </n-form-item> -->

      <!-- 根据类型展示不同的表单 目前只有 OAUTH2 -->
      <template v-for="item of authTypeFieldMap['OAUTH2']" :key="item.key">
        <n-form-item :label="item.label" :path="`configuration.${item.key}`" :rule="item.rule">
          <n-input
            v-model:value="form.configuration[item.key]"
            allow-clear
            :max-length="255"
            :placeholder="item.placeholder ?? t('common.pleaseInput')"
          />
          <div v-if="item.subTip" class="text-[12px] text-[var(--text-n4)]">{{ item.subTip }}</div>
        </n-form-item>
      </template>

      <n-form-item v-if="form.type === 'LDAP'" label=" ">
        <div class="flex items-center gap-[12px]">
          <n-button type="primary" ghost> {{ t('system.business.mailSettings.testLink') }} </n-button>
          <n-button type="primary" ghost> {{ t('system.business.authenticationSettings.testLogin') }} </n-button>
        </div>
      </n-form-item>
    </n-form>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { FormInst, NButton, NForm, NFormItem, NInput, useMessage } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  // import CrmTab from '@/components/pure/crm-tab/index.vue';
  import { createAuth, updateAuth } from '@/api/modules/system/business';
  import { authTypeFieldMap, defaultAuthForm } from '@/config/business';
  import { useI18n } from '@/hooks/useI18n';

  import { AuthForm, AuthUpdateParams } from '@lib/shared/models/system/business';

  const { t } = useI18n();
  const Message = useMessage();

  const showDrawer = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const form = defineModel<AuthForm>('editAuthInfo', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  // 先不上
  // const tabList = computed(() => {
  //   return Object.keys(authTypeFieldMap).map((item) => ({
  //     name: item,
  //     tab: item,
  //   }));
  // });

  const formRef = ref<FormInst | null>(null);

  // 新增和编辑操作
  async function confirm(isContinue?: boolean) {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          const formData: AuthUpdateParams = {
            ...form.value,
            configuration: JSON.stringify(form.value.configuration),
          };

          if (form.value.id) {
            await updateAuth(formData);
            Message.success(t('common.updateSuccess'));
          } else {
            await createAuth(formData);
            Message.success(t('common.addSuccess'));
          }
          if (isContinue) {
            form.value.name = '';
            form.value.description = '';
            Object.keys(form.value.configuration).forEach((key) => {
              form.value.configuration[key] = '';
            });
          } else {
            showDrawer.value = false;
          }
          emit('refresh');
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        }
      }
    });
  }

  function cancel() {
    form.value = cloneDeep(defaultAuthForm);
  }
</script>

<style scoped lang="less">
  :deep(.n-form-item .n-form-item-blank) {
    flex-direction: column;
    align-items: flex-start;
  }
</style>
