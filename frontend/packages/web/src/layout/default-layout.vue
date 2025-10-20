<template>
  <n-layout class="default-layout">
    <LayoutHeader
      v-if="!route.name?.toString().includes(DashboardRouteEnum.DASHBOARD)"
      :is-preview="innerProps.isPreview"
      :logo="innerLogo"
      @open-personal-info="handleOpenPersonalInfo"
    />
    <n-layout class="flex-1" has-sider>
      <LayoutSider @open-personal-info="handleOpenPersonalInfo" />
      <PageContent />
    </n-layout>
  </n-layout>
  <PersonalInfoDrawer v-model:visible="showPersonalInfo" :active-tab-value="personalTab" />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { NLayout } from 'naive-ui';

  import { PersonalEnum } from '@lib/shared/enums/systemEnum';

  import LayoutHeader from './components/layout-header.vue';
  import LayoutSider from './components/layout-sider.vue';
  import PageContent from './page-content.vue';
  import PersonalInfoDrawer from '@/views/system/business/components/personalInfoDrawer.vue';

  import { defaultPlatformLogo } from '@/config/business';

  import { DashboardRouteEnum } from '@/enums/routeEnum';

  const route = useRoute();

  interface Props {
    isPreview?: boolean;
    logo?: string;
  }

  const props = defineProps<Props>();

  const innerProps = ref<Props>(props);
  const personalTab = ref(PersonalEnum.INFO);
  const showPersonalInfo = ref<boolean>(false);

  function handleOpenPersonalInfo(tab: PersonalEnum) {
    personalTab.value = tab;
    showPersonalInfo.value = true;
  }

  watch(
    () => props.logo,
    () => {
      innerProps.value = { ...props };
    }
  );
  const innerLogo = computed(() =>
    props.isPreview && innerProps.value.logo ? innerProps.value.logo : defaultPlatformLogo
  );
</script>

<style lang="less">
  .default-layout {
    @apply flex;

    height: 100vh;
    .n-layout-scroll-container {
      @apply flex w-full flex-col overflow-hidden;
    }
  }
</style>
