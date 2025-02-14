<template>
  <CrmDrawer
    v-model:show="visible"
    width="100%"
    :footer="false"
    :closable="false"
    header-class="crm-form-drawer-header"
    body-content-class="!p-0"
  >
    <template #header>
      <div class="flex items-center justify-between">
        <div class="flex items-center">
          <n-button text class="w-[32px]" @click="handleBack">
            <n-icon size="16">
              <ChevronBackOutline />
            </n-icon>
          </n-button>
          <n-input
            v-model:value="name"
            type="text"
            :placeholder="t('common.pleaseInput')"
            size="medium"
            clearable
            class="crm-form-drawer-title"
            autosize
            :status="name.trim() === '' ? 'error' : undefined"
            :maxlength="255"
          ></n-input>
        </div>
        <n-button type="primary" @click="handleSave">{{ t('common.save') }}</n-button>
      </div>
    </template>
    <CrmFormDesign v-if="visible" />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton, NIcon, NInput } from 'naive-ui';
  import { ChevronBackOutline } from '@vicons/ionicons5';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const CrmFormDesign = defineAsyncComponent(() => import('@/components/business/crm-form-design/index.vue'));

  const props = defineProps<{
    title: string;
  }>();

  const { t } = useI18n();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const name = ref(props.title);

  function handleBack() {
    visible.value = false;
  }

  async function handleSave() {
    console.log(name.value);
  }
</script>

<style lang="less">
  .crm-form-drawer-header {
    padding: 12px 16px !important;
    .n-drawer-header__main {
      max-width: 100%;
      .crm-form-drawer-title {
        --n-border: none !important;
        --n-border-hover: none !important;
        --n-border-focus: none !important;
        --n-box-shadow-focus: none !important;

        min-width: 80px;
        border-bottom: 2px solid var(--text-n8);
      }
    }
  }
</style>
