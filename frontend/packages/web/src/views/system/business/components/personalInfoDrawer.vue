<template>
  <CrmDrawer
    v-model:show="visible"
    width="100%"
    :title="t('system.personal.info.title')"
    :footer="false"
    :show-back="true"
    :closable="false"
    :body-content-class="bodyClass"
  >
    <n-scrollbar>
      <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" @change="searchData()" />
      </CrmCard>
      <CrmCard v-if="activeTab === PersonalEnum.INFO" hide-footer :special-height="64">
        <div class="flex font-medium text-[var(--text-n1)]">
          <n-p>{{ t('common.baseInfo') }}</n-p>
        </div>
        <div class="flex w-full items-center gap-[8px] py-[16px]">
          <CrmAvatar />
          <div class="flex-1">
            <div class="text-[var(--text-n1)]">{{ personalInfo.userName }}</div>
            <div class="flex items-center gap-[4px]">
              <n-tag
                v-if="personalInfo.userId === 'admin'"
                :bordered="false"
                size="small"
                :color="{
                  color: 'var(--primary-6)',
                  textColor: 'var(--primary-8)',
                }"
              >
                {{ t('common.admin') }}
              </n-tag>
              <template v-else>
                <CrmTag
                  v-for="role in personalInfo.roles"
                  :key="role.id"
                  :bordered="false"
                  size="small"
                  :color="{
                    color: 'var(--primary-6)',
                    textColor: 'var(--primary-8)',
                  }"
                >
                  {{ role.name }}
                </CrmTag>
              </template>
            </div>
          </div>
        </div>
        <div
          class="grid w-full grid-cols-3 gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[24px]"
        >
          <div class="flex">
            <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.phone') }}</n-p>
            <n-p class="mx-[8px] my-[0] text-[var(--text-n1)]">{{ personalInfo.phone }}</n-p>
          </div>
          <div class="flex">
            <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.email') }}</n-p>
            <n-p class="mx-[8px] my-[0] text-[var(--text-n1)]">{{ personalInfo.email }}</n-p>
          </div>
          <div v-if="!userStore.isAdmin" class="flex">
            <n-p class="m-[0] text-[var(--text-n4)]">{{ t('system.personal.department') }}</n-p>
            <n-p class="mx-[8px] my-[0] text-[var(--text-n1)]">{{ personalInfo.departmentName }}</n-p>
          </div>
        </div>
        <div class="py-[24px]">
          <n-button type="primary" ghost class="mx-[8px]" @click="edit">
            {{ t('common.edit') }}
          </n-button>
          <n-button @click="changePassword">
            {{ t('system.personal.changePassword') }}
          </n-button>
        </div>
      </CrmCard>
      <CrmCard v-if="activeTab === PersonalEnum.MY_PLAN" no-content-padding hide-footer :special-height="64">
        <FollowDetail
          :show-add="
            hasAnyPermission(['CUSTOMER_MANAGEMENT:UPDATE', 'CLUE_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:UPDATE'])
          "
          :refresh-key="refreshKey"
          active-type="followPlan"
          wrapper-class="h-[calc(100vh-155px)]"
          virtual-scroll-height="calc(100vh - 252px)"
          follow-api-key="myPlan"
          source-id="NULL"
          :any-permission="['CUSTOMER_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ']"
        />
      </CrmCard>
      <apiKey v-if="activeTab === PersonalEnum.API_KEY" />
    </n-scrollbar>
  </CrmDrawer>
  <EditPersonalInfoModal v-model:show="showEditPersonalModal" :integration="currentInfo" @init-sync="searchData()" />
  <EditPasswordModal v-model:show="showEditPasswordModal" @init-sync="searchData()" />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NP, NScrollbar, NTag, TabPaneProps } from 'naive-ui';

  import { PersonalEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { PersonalInfoRequest } from '@lib/shared/models/system/business';
  import { OrgUserInfo } from '@lib/shared/models/system/org';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import FollowDetail from '@/components/business/crm-follow-detail/index.vue';
  import apiKey from './apiKey.vue';
  import EditPasswordModal from '@/views/system/business/components/editPasswordModal.vue';
  import EditPersonalInfoModal from '@/views/system/business/components/editPersonalInfoModal.vue';

  import { getPersonalInfo } from '@/api/modules';
  import { defaultUserInfo } from '@/config/business';
  import { useUserStore } from '@/store';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const userStore = useUserStore();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const activeTab = defineModel<PersonalEnum>('activeTabValue', {
    required: false,
    default: PersonalEnum.INFO,
  });

  const personalInfo = ref<OrgUserInfo>({
    ...defaultUserInfo,
  });

  const currentInfo = ref<PersonalInfoRequest>({
    phone: '',
    email: '',
  });

  const bodyClass = ref<string>('crm-drawer-content');

  const showEditPersonalModal = ref<boolean>(false); // 已配置
  const showEditPasswordModal = ref<boolean>(false); // 已配置

  const tabList = computed<TabPaneProps[]>(() => {
    return [
      {
        name: PersonalEnum.INFO,
        tab: t('system.personal.info'),
      },
      {
        name: PersonalEnum.MY_PLAN,
        tab: t('system.personal.plan'),
      },
      ...(hasAnyPermission(['PERSONAL_API_KEY:READ'])
        ? [
            {
              name: PersonalEnum.API_KEY,
              tab: t('system.personal.apiKey'),
            },
          ]
        : []),
    ];
  });

  async function searchData() {
    if (activeTab.value === PersonalEnum.INFO) {
      personalInfo.value = await getPersonalInfo();
    }
  }
  function edit() {
    currentInfo.value.email = personalInfo.value.email;
    currentInfo.value.phone = personalInfo.value.phone;
    showEditPersonalModal.value = true;
  }

  function changePassword() {
    showEditPasswordModal.value = true;
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        searchData();
      }
    }
  );

  watch(
    () => activeTab.value,
    (val) => {
      if (val === PersonalEnum.INFO) {
        searchData();
      }
    }
  );

  const refreshKey = ref(0);
</script>

<style scoped lang="less"></style>
