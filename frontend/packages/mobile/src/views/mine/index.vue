<template>
  <div class="flex h-full flex-col overflow-hidden">
    <CrmPageHeader :title="t('menu.mine')" hide-back />
    <div class="mt-[48px] flex flex-1 flex-col gap-[16px] overflow-auto p-[16px]">
      <div class="personal-header-info info-item gap-[16px] p-[16px]">
        <van-image round width="64px" height="64px" :src="userStore.userInfo?.avatar" />
        <div class="flex w-[calc(100%-80px)] flex-1 flex-col justify-evenly">
          <div class="one-line-text text-[16px] font-semibold text-[var(--text-n1)]" @click="handleUserNameClick">
            {{ personalInfo?.userName }}
          </div>
          <div>
            <van-tag color="var(--text-n9)" text-color="var(--text-n1)" size="medium">
              {{ personalInfo?.departmentName }}
            </van-tag>
          </div>
        </div>
      </div>
      <van-cell-group inset class="info-item">
        <van-cell
          center
          class="!p-[16px]"
          :title="t('common.phoneNumber')"
          is-link
          inset
          value-class="!text-[var(--text-n4)]"
          :value="personalInfo?.phone"
          @click="handleEditInfo('phone')"
        />
        <van-cell center class="!p-[16px]" :title="t('mine.email')" is-link @click="handleEditInfo('email')">
          <template #value>
            <div class="one-line-text text-[var(--text-n4)]">
              {{ personalInfo?.email }}
            </div>
          </template>
        </van-cell>
      </van-cell-group>
      <van-cell-group v-permission="['SYSTEM_NOTICE:READ']" inset class="info-item">
        <van-cell :title="t('common.message')" is-link class="!p-[16px]" @click="handleEditInfo('message')">
          <template #value>
            <div v-if="messageTotal > 0" class="absolute right-[16px] top-[8px]">
              <van-badge :content="messageTotal" color="var(--error-red)" max="99" />
            </div>
          </template>
        </van-cell>
      </van-cell-group>
      <van-cell-group inset class="info-item">
        <van-cell
          class="!p-[16px]"
          center
          :title="t('mine.resetPassWord')"
          is-link
          @click="handleEditInfo('resetPassWord')"
        />
      </van-cell-group>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { OrgUserInfo } from '@lib/shared/models/system/org';

  import CrmPageHeader from '@/components/pure/crm-page-header/index.vue';

  import { getNotificationCount, getPersonalUrl } from '@/api/modules';
  import { defaultUserInfo } from '@/config/mine';
  import useUserStore from '@/store/modules/user';
  import { hasAnyPermission } from '@/utils/permission';

  import { MineRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();
  const userStore = useUserStore();

  const routeKey = ref('');

  function handleEditInfo(type: string) {
    routeKey.value = type !== 'message' ? MineRouteEnum.MINE_DETAIL : MineRouteEnum.MINE_MESSAGE;
    router.push({
      name: routeKey.value,
      query: {
        type,
      },
    });
  }

  const personalInfo = ref<OrgUserInfo>({
    ...defaultUserInfo,
  });

  async function initPersonInfo() {
    try {
      personalInfo.value = await getPersonalUrl();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const messageTotal = ref(0);
  async function initMessageCount() {
    if (!hasAnyPermission(['SYSTEM_NOTICE:READ'])) {
      return;
    }
    try {
      const result = await getNotificationCount({
        type: '',
        status: '',
        resourceType: '',
        createTime: null,
        endTime: null,
      });

      messageTotal.value = result.find((e) => e.key === 'total')?.count || 0;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const clickCount = ref(0); // 记录点击次数
  let clickTimer: NodeJS.Timeout | null = null; // 定时器

  function handleUserNameClick() {
    clickCount.value++;

    // 如果 1 秒内没有连续点击，重置计数
    if (clickTimer) {
      clearTimeout(clickTimer);
    }
    clickTimer = setTimeout(() => {
      clickCount.value = 0;
    }, 1000);

    // 如果点击次数达到 10 次，加载 eruda 调试工具
    if (clickCount.value >= 10) {
      import('eruda').then((eruda) => eruda.default.init());
      clickCount.value = 0; // 重置计数
    }
  }

  onBeforeMount(() => {
    initPersonInfo();
    initMessageCount();
  });
</script>

<style lang="less" scoped>
  .personal-header-info {
    @apply flex;

    border-radius: @border-radius-large;
    background-color: var(--text-n10);
  }
  .info-item {
    @apply !mx-0 flex-shrink-0;
  }
  .person-bottom-border {
    margin: 0 16px;
    .half-px-border-bottom();
  }
</style>
