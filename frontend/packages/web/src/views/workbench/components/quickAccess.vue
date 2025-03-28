<template>
  <CrmCard hide-footer auto-height class="mb-[16px]">
    <div class="title">
      <div class="title-name">{{ t('workbench.quickAccess') }}</div>
    </div>
    <div class="flex justify-around gap-[16px]">
      <div
        v-for="item in quickAccessList"
        :key="item.key"
        class="flex w-[114px] cursor-pointer flex-col items-center gap-[8px] py-[8px]"
        @click="handleActionSelect(item.key)"
      >
        <CrmSvg width="40px" height="40px" :name="item.icon" />
        {{ item.label }}
      </div>
    </div>
  </CrmCard>
  <!-- TODO 保存后刷新 other-save-params参数 -->
  <CrmFormCreateDrawer v-model:visible="formCreateDrawerVisible" :form-key="activeFormKey" />
</template>

<script setup lang="ts">
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmSvg from '@/components/pure/crm-svg/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';

  import { quickAccessList } from '@/config/workbench';
  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const activeFormKey = ref(FormDesignKeyEnum.CUSTOMER);
  const formCreateDrawerVisible = ref(false);

  function handleActionSelect(actionKey: FormDesignKeyEnum) {
    activeFormKey.value = actionKey;
    switch (actionKey) {
      case FormDesignKeyEnum.CUSTOMER:
      case FormDesignKeyEnum.CLUE:
      case FormDesignKeyEnum.BUSINESS:
      case FormDesignKeyEnum.CONTACT:
        formCreateDrawerVisible.value = true;
        break;
      // TODO lmy 新建跟进计划/新建跟进记录
      default:
        break;
    }
  }
</script>

<style lang="less" scoped>
  .title {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
    .title-name {
      font-weight: 600;
    }
  }
</style>
