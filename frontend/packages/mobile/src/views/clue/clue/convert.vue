<template>
  <CrmPageWrapper :title="t('clue.convertTo')">
    <div class="px-[16px]">
      <div class="mb-[24px]">
        <div class="my-[14px] text-[16px] font-semibold">
          {{ t('clue.convertCluesTo') }}
        </div>
        <div class="flex gap-[12px]">
          <div
            v-for="item in convertTypeOptions"
            :key="item.value"
            class="option-card"
            :class="{
              'is-active': selectedType.includes(item.value),
              'is-disabled': item.disabled,
            }"
            @click="!item.disabled && toggleSelect(item.value)"
          >
            <!-- 左上角三角 + 对勾 -->
            <div v-if="selectedType.includes(item.value)" class="corner">
              <div class="triangle"></div>
              <CrmIcon class="checkmark" name="iconicon_check" color="var(--text-n10)" />
            </div>

            <div class="label">{{ item.label }}</div>
          </div>
        </div>
        <van-form v-if="selectedType.includes(FormDesignKeyEnum.BUSINESS)" ref="formRef" required class="crm-form">
          <van-field
            v-model="oppName"
            class="!pb-0 !pl-0"
            :label="oppLabel"
            required
            :maxlength="255"
            :rules="[{ required: true, message: t('common.notNull', { value: oppLabel }) }]"
            name="oppName"
            :placeholder="t('common.pleaseInput')"
          />
        </van-form>
      </div>

      <div class="rounded-md bg-[var(--text-n9)] px-[20px] py-[16px]">
        <div class="mb-[4px] font-medium text-[var(--text-n1)]">{{ t('clue.remarks') }}</div>
        <div class="remarks text-[12px] text-[var(--text-n4)]">
          <div>
            {{ isSelectOpportunity ? t('clue.sameNameOppConvertTip') : t('clue.sameNameConvertTip') }}
          </div>
          <div>{{ isSelectOpportunity ? t('clue.notSameNameOppConvertTip') : t('clue.notSameNameConvertTip') }}</div>
          <div>{{ isSelectOpportunity ? t('clue.linkFormConfigOppTip') : t('clue.linkFormConfigTip') }}</div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex items-center gap-[16px]">
        <van-button
          type="default"
          class="crm-button-primary--secondary !rounded-[var(--border-radius-small)] !text-[16px]"
          :disabled="loading"
          block
          @click="router.back"
        >
          {{ t('common.cancel') }}
        </van-button>
        <van-button
          v-permission="['CUSTOMER_MANAGEMENT:UPDATE']"
          type="primary"
          :loading="loading"
          class="!rounded-[var(--border-radius-small)] !text-[16px]"
          block
          :disabled="isNoPermissionDisabled"
          @click="onConfirm"
        >
          {{ t('clue.convert') }}
        </van-button>
      </div>
    </template>

    <van-dialog
      v-model:show="showConvertedSuccess"
      :title="t('clue.conversionSuccessful')"
      show-cancel-button
      close-on-click-overlay
      close-on-popstate
      class="convertedSuccess"
    >
      <div class="mt-[8px] px-[24px]">
        <van-count-down
          class="inline-flex items-center whitespace-nowrap"
          millisecond
          time="5000"
          format="ss"
          auto-start
          @finish="back"
        >
          <template #default="timeData">
            <span class="text-[var(--primary-8)]">{{ timeData.seconds }}</span>
          </template>
        </van-count-down>
        <span class="text-[var(--text-n2)]">
          {{ t('clue.countDownTip') }}
        </span>
      </div>
      <template #footer>
        <div class="flex items-center gap-[16px] p-[24px]">
          <van-button
            type="default"
            class="crm-button-primary--secondary h-[40px] !rounded-[var(--border-radius-small)] !text-[16px]"
            block
            @click="back"
          >
            {{ t('clue.backClueList') }}
          </van-button>
          <van-button
            v-permission="['CUSTOMER_MANAGEMENT:UPDATE']"
            type="primary"
            class="h-[40px] !rounded-[var(--border-radius-small)] !text-[16px]"
            block
            @click="goDetail"
          >
            {{
              t('clue.afterConvertGoDetailText', {
                name: type === FormDesignKeyEnum.BUSINESS ? t('common.opportunity') : t('common.customer'),
              })
            }}
          </van-button>
        </div>
      </template>
    </van-dialog>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { FormInstance, showLoadingToast } from 'vant';
  import { cloneDeep } from 'lodash-es';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';

  import { getOptFormConfig, transformClue } from '@/api/modules';
  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  import { ClueRouteEnum, CustomerRouteEnum, OpportunityRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();
  const route = useRoute();

  const clueId = ref(route.query.clueId as string);
  const initSelectedType = [FormDesignKeyEnum.CONTACT, FormDesignKeyEnum.CUSTOMER];
  const selectedType = ref(cloneDeep(initSelectedType));
  const isSelectOpportunity = computed(() => selectedType.value.includes(FormDesignKeyEnum.BUSINESS));

  const convertTypeOptions = computed(() => [
    {
      value: FormDesignKeyEnum.CONTACT,
      label: t('common.contact'),
      disabled: true,
    },
    {
      value: FormDesignKeyEnum.CUSTOMER,
      label: t('common.customer'),
      disabled: true,
    },
    {
      value: FormDesignKeyEnum.BUSINESS,
      label: t('common.opportunity'),
      disabled: !hasAllPermission(['OPPORTUNITY_MANAGEMENT:ADD', 'CUSTOMER_MANAGEMENT:ADD']),
    },
  ]);

  const isNoPermissionDisabled = computed(() =>
    selectedType.value.includes(FormDesignKeyEnum.BUSINESS)
      ? !hasAllPermission(['CUSTOMER_MANAGEMENT:ADD', 'OPPORTUNITY_MANAGEMENT:ADD'])
      : !hasAllPermission(['CUSTOMER_MANAGEMENT:ADD'])
  );

  function toggleSelect(value: FormDesignKeyEnum) {
    const index = selectedType.value.indexOf(value);
    if (index > -1) {
      selectedType.value.splice(index, 1);
    } else {
      selectedType.value.push(value);
    }
  }

  const oppLabel = ref(t('opportunity.name'));
  async function initOppName() {
    if (!hasAnyPermission(['OPPORTUNITY_MANAGEMENT:READ'])) return;
    try {
      const result = await getOptFormConfig();
      oppLabel.value = result.fields.find((e) => e.businessKey === 'name')?.name ?? t('opportunity.name');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
  onMounted(() => {
    initOppName();
  });

  const loading = ref(false);
  const oppName = ref('');
  const formRef = ref<FormInstance>();

  const type = ref(FormDesignKeyEnum.CUSTOMER);
  const successDataId = ref('');
  const showConvertedSuccess = ref(false);
  function showConvertedSuccessDialog(res: string) {
    type.value = selectedType.value.includes(FormDesignKeyEnum.BUSINESS)
      ? FormDesignKeyEnum.BUSINESS
      : FormDesignKeyEnum.CUSTOMER;
    showConvertedSuccess.value = true;
    successDataId.value = res;
  }

  async function onConfirm() {
    try {
      await formRef.value?.validate();
      loading.value = true;
      showLoadingToast(t('common.transferring'));
      const res = await transformClue({
        oppName: selectedType.value.includes(FormDesignKeyEnum.BUSINESS) ? oppName.value : '',
        oppCreated: selectedType.value.includes(FormDesignKeyEnum.BUSINESS),
        clueId: clueId.value,
      });
      showConvertedSuccessDialog(res);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  function back() {
    router.push({
      name: ClueRouteEnum.CLUE_INDEX,
    });
  }

  function goDetail() {
    if (type.value === FormDesignKeyEnum.BUSINESS) {
      router.push({
        name: OpportunityRouteEnum.OPPORTUNITY_DETAIL,
        query: {
          id: successDataId.value,
          name: oppName.value,
        },
      });
    } else {
      router.push({
        name: CustomerRouteEnum.CUSTOMER_DETAIL,
        query: {
          id: successDataId.value,
          name: route.query.clueName as string,
          needInitDetail: 'Y',
        },
      });
    }
  }
</script>

<style lang="less" scoped>
  .remarks {
    text-align: justify;
    line-height: 20px;
  }
  .option-card {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    height: 56px;
    font-size: 16px;
    border: 1.5px solid var(--text-n6);
    border-radius: 6px;
    flex: 1;
    & .corner {
      position: absolute;
      top: 0;
      left: 0;
    }
    & .triangle {
      width: 0;
      height: 0;
      border-style: solid;
      border-width: 0 0 28px 28px;
      border-color: transparent transparent transparent var(--primary-8);
    }
    & .checkmark {
      position: absolute;
      top: 3px;
      left: 1px;
    }
    &.is-active {
      border: 1.5px solid var(--primary-8);
      .triangle {
        border-color: transparent transparent transparent var(--primary-8);
      }
    }
    &.is-disabled {
      border: 1.5px solid var(--primary-4);
      color: var(--text-n4);
      .triangle {
        border-color: transparent transparent transparent var(--primary-4);
      }
    }
  }
</style>

<style lang="less">
  .convertedSuccess {
    .van-dialog__header {
      font-size: 18px;
    }
  }
</style>
