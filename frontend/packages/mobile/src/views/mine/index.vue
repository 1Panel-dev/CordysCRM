<template>
  <div class="flex h-full flex-col overflow-hidden">
    <CrmPageHeader :title="t('menu.mine')" hide-back />
    <div class="mt-[48px] flex flex-1 flex-col gap-[16px] overflow-auto p-[16px]">
      <div class="personal-header-info info-item gap-[16px] p-[16px]">
        <CrmAvatar :size="64" :is-word="false" />
        <div class="flex w-[calc(100%-80px)] flex-1 flex-col justify-evenly">
          <div class="one-line-text text-[16px] font-semibold text-[var(--text-n1)]" @click="handleUserNameClick">
            {{ personalInfo?.userName }}
          </div>
          <div class="max-w-full">
            <CrmTag
              :tag="personalInfo?.departmentName ?? ''"
              color="var(--text-n9)"
              text-color="var(--text-n1)"
              class="one-line-text max-w-full rounded-[var(--border-radius-small)] !p-[2px_6px]"
            />
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
        <van-cell
          center
          class="email-cell !p-[16px]"
          :title="t('mine.email')"
          is-link
          title-class="email-title-class"
          @click="handleEditInfo('email')"
        >
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

      <van-button block type="primary" :loading="loading" native-type="submit" @click="handleLogout">
        {{ t('login.form.logout') }}
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { OrgUserInfo } from '@lib/shared/models/system/org';

  import CrmPageHeader from '@/components/pure/crm-page-header/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  import { getNotificationCount, getPersonalInfo } from '@/api/modules';
  import { defaultUserInfo } from '@/config/mine';
  import useUser from '@/hooks/useUser';
  import useUserStore from '@/store/modules/user';
  import { hasAnyPermission } from '@/utils/permission';

  import { MineRouteEnum } from '@/enums/routeEnum';

  const { logout } = useUser();

  const userStore = useUserStore();

  const { t } = useI18n();
  const router = useRouter();

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
      personalInfo.value = await getPersonalInfo();
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

  const loading = ref(false);
  async function handleLogout() {
    loading.value = true;
    try {
      await userStore.logout();
      logout();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
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

<style lang="less">
  .email-cell {
    &.van-cell {
      display: flex;
      justify-content: space-between;
      .email-title-class {
        min-width: 60px;
        @apply flex-grow-0;
      }
    }
  }
</style>
