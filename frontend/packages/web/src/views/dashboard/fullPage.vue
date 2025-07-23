<template>
  <div class="h-full">
    <dashboard
      :title="route.query.title as string"
      :dashboard-id="route.query.resourceId as string"
      :is-favorite="isFavorite"
      is-full-page
      @toggle-favorite="favoriteToggle()"
    />
  </div>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import dashboard from './components/dashboard.vue';

  import { dashboardCollect, dashboardUnCollect } from '@/api/modules';

  import { FullPageEnum } from '@/enums/routeEnum';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();
  const Message = useMessage();

  const isFavorite = ref(route.query.isFavorite === 'Y');

  async function favoriteToggle() {
    try {
      if (isFavorite.value) {
        await dashboardUnCollect(route.query.id as string);
      } else {
        await dashboardCollect(route.query.id as string);
      }
      isFavorite.value = !isFavorite.value;
      Message.success(isFavorite.value ? t('dashboard.favoriteSuccess') : t('dashboard.unFavoriteSuccess'));
      router.replace({
        name: FullPageEnum.FULL_PAGE_DASHBOARD,
        query: {
          ...route.query,
          isFavorite: isFavorite.value ? 'Y' : 'N',
        },
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
</script>

<style lang="less" scoped></style>
