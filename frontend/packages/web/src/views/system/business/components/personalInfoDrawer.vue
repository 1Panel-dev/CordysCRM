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
    <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
      <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" @change="searchData()" />
    </CrmCard>
    <CrmCard v-if="activeTab === PersonalEnum.INFO" hide-footer :special-height="64">
      <div class="flex font-medium text-[var(--text-n1)]"
        ><n-spin>{{ t('common.baseInfo') }}</n-spin></div
      >
      <div class="flex w-full items-center gap-[8px] py-[16px]">
        <CrmAvatar />
        <div>
          <div>{{ personalInfo.userName }}</div>
          <n-tag
            v-if="showRoleTag"
            :bordered="false"
            size="small"
            :color="{
              color: 'var(--primary-6)',
              textColor: 'var(--primary-8)',
            }"
          >
            {{ personalInfo.userId === 'admin' ? t('common.admin') : t('opportunity.admin') }}
          </n-tag>
        </div>
      </div>
      <div
        class="flex w-full items-center justify-between gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[24px]"
      >
        <div>
          <n-p class="text-[var(--text-n4)]">{{ t('system.personal.phone') }}</n-p>
          <n-p class="mx-[8px] text-[var(--text-n1)]">{{ personalInfo.phone }}</n-p>
        </div>
        <div>
          <n-p class="text-[var(--text-n4)]">{{ t('system.personal.email') }}</n-p>
          <n-p class="mx-[8px] text-[var(--text-n1)]">{{ personalInfo.email }}</n-p>
        </div>
        <div>
          <n-p class="text-[var(--text-n4)]">{{ t('system.personal.department') }}</n-p>
          <n-p class="mx-[8px] text-[var(--text-n1)]">{{ personalInfo.departmentName }}</n-p>
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
    <CrmCard v-if="activeTab === PersonalEnum.MY_PLAN" hide-footer :special-height="64"> </CrmCard>
  </CrmDrawer>
  <EditPersonalInfoModal v-model:show="showEditPersonalModal" :integration="currentInfo" @init-sync="searchData()" />
  <EditPasswordModal v-model:show="showEditPasswordModal" :integration="currentPassword" @init-sync="searchData()" />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NTag, TabPaneProps, useMessage } from 'naive-ui';

  import { PersonalEnum } from '@lib/shared/enums/systemEnum';
  import { PersonalInfoRequest, PersonalPassword } from '@lib/shared/models/system/business';
  import { OrgUserInfo } from '@lib/shared/models/system/org';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';
  import EditPasswordModal from '@/views/system/business/components/editPasswordModal.vue';
  import EditPersonalInfoModal from '@/views/system/business/components/editPersonalInfoModal.vue';

  import { getPersonalUrl } from '@/api/modules/system/business';
  import { defaultUserInfo } from '@/config/business';
  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();
  const Message = useMessage();

  const props = withDefaults(
    defineProps<{
      activeTabValue?: PersonalEnum;
    }>(),
    {
      activeTabValue: PersonalEnum.INFO,
    }
  );

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const personalInfo = ref<OrgUserInfo>({
    ...defaultUserInfo,
  });

  const currentInfo = ref<PersonalInfoRequest>({
    phone: '',
    email: '',
  });

  const currentPassword = ref<PersonalPassword>({
    email: '',
    code: '',
    password: '',
    confirmPassword: '',
  });

  const bodyClass = ref<string>('crm-drawer-content');

  const showEditPersonalModal = ref<boolean>(false); // 已配置
  const showEditPasswordModal = ref<boolean>(false); // 已配置
  const showRoleTag = ref<boolean>(false);

  const activeTab = ref(PersonalEnum.INFO);
  watch(
    () => props.activeTabValue,
    () => {
      if (props.activeTabValue) {
        activeTab.value = props.activeTabValue;
      }
    }
  );

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
    ];
  });


  async function searchData() {
    if (activeTab.value === PersonalEnum.INFO) {
      personalInfo.value = await getPersonalUrl();
      showRoleTag.value = personalInfo.value.roles.some((role) => role.id === 'org_admin') || personalInfo.value.userId === 'admin';
    }
  }
  function edit() {
    currentInfo.value.email = personalInfo.value.email;
    currentInfo.value.phone = personalInfo.value.phone;
    showEditPersonalModal.value = true;
  }

  function changePassword() {
    currentPassword.value.email = personalInfo.value.email;
    showEditPasswordModal.value = true;
  }

  onMounted(() => {
    searchData();
  });
</script>

<style scoped lang="less"></style>
