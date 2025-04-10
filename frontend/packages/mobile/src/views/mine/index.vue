<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div class="personal-title p-[11px_16px] text-[18px] text-[var(--text-n1)]">
      {{ t('menu.mine') }}
    </div>
    <div class="flex flex-1 flex-col gap-[16px] overflow-auto p-[16px]">
      <div class="personal-header-info info-item gap-[16px] p-[16px]">
        <van-image round width="64px" height="64px" src="https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg" />
        <div class="flex w-[calc(100%-80px)] flex-1 flex-col justify-evenly">
          <div class="one-line-text text-[16px] font-semibold text-[var(--text-n1)]"> {{ detail.name }}</div>
          <div>
            <van-tag color="var(--text-n9)" text-color="var(--text-n1)" size="medium">
              {{ detail.departmentName }}
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
          :value="detail.phone"
          @click="handleEditInfo('phone')"
        />
        <van-cell center class="!p-[16px]" :title="t('mine.email')" is-link @click="handleEditInfo('email')">
          <template #value>
            <div class="one-line-text text-[var(--text-n4)]">
              {{ detail.email }}
            </div>
          </template>
        </van-cell>
        <div class="person-bottom-border"></div>
      </van-cell-group>
      <van-cell-group inset class="info-item">
        <van-cell :title="t('common.message')" is-link class="!p-[16px]" @click="handleEditInfo('message')">
          <template #value>
            <div class="absolute right-[16px] top-[8px]">
              <van-badge :content="100" color="var(--error-red)" max="99" />
            </div>
          </template>
        </van-cell>
        <div class="person-bottom-border"></div>
      </van-cell-group>
      <van-cell-group inset class="info-item">
        <van-cell
          class="!p-[16px]"
          center
          :title="t('mine.resetPassWord')"
          is-link
          @click="handleEditInfo('resetPassWord')"
        />
        <div class="person-bottom-border"></div>
      </van-cell-group>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { MineRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();

  const detail = ref({
    name: '迪丽热巴迪丽热巴迪',
    phone: '13333336676',
    email: 'zhaopang@fit2cou.com',
    departmentName: '部门名称',
    id: '8889',
  });

  const routeKey = ref('');

  function handleEditInfo(type: string) {
    routeKey.value = type !== 'message' ? MineRouteEnum.MINE_DETAIL : MineRouteEnum.MINE_MESSAGE;
    router.push({
      name: routeKey.value,
      query: {
        id: detail.value.id,
        type,
      },
    });
  }
</script>

<style lang="less" scoped>
  .personal-title {
    @apply flex items-center justify-center font-semibold;

    background-color: var(--text-n10);
    .half-px-border-bottom();
  }
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
