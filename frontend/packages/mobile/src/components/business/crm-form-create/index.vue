<template>
  <CrmPageWrapper :title="formCreateTitle">
    <van-form ref="formRef" class="border-b border-[var(--text-n8)]" required>
      <van-cell-group inset>
        <template v-for="item in fieldList" :key="item.id">
          <component
            :is="getItemComponent(item.type, item.multiple)"
            v-if="item.show !== false"
            v-model:value="formDetail[item.id]"
            :field-config="item"
            :path="item.id"
            @change="($event: any) => handleFieldChange($event, item)"
          />
        </template>
      </van-cell-group>
    </van-form>
    <template #footer>
      <div class="flex items-center gap-[16px]">
        <van-button
          type="default"
          class="crm-button-primary--secondary !rounded-[var(--border-radius-small)] !text-[16px]"
          block
          :disabled="loading"
          @click="router.back"
        >
          {{ t('common.cancel') }}
        </van-button>
        <van-button
          type="primary"
          class="!rounded-[var(--border-radius-small)] !text-[16px]"
          :loading="loading"
          block
          @click="saveForm"
        >
          {{ route.query.id ? t('common.update') : t('common.create') }}
        </van-button>
      </div>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { FormInstance } from 'vant';

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmFormCreateComponents from '@/components/business/crm-form-create/components';

  import useFormCreateApi from '@/hooks/useFormCreateApi';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const formRef = ref<FormInstance>();

  const lastPageParams = window.history.state.params ? JSON.parse(window.history.state.params) : null; // 获取上个页面带过来的表格查询参数
  const { fieldList, formDetail, loading, formCreateTitle, initFormConfig, initFormDetail, saveForm } =
    useFormCreateApi({
      formKey: route.query.formKey as FormDesignKeyEnum,
      sourceId: route.query.sourceId as string,
      needInitDetail: route.query.needInitDetail === 'Y',
      initialSourceName: route.query.initialSourceName as string,
      otherSaveParams: lastPageParams,
    });

  function getItemComponent(type: FieldTypeEnum, multiple?: boolean) {
    if (type === FieldTypeEnum.INPUT) {
      return CrmFormCreateComponents.basicComponents.singleText;
    }
    if (type === FieldTypeEnum.TEXTAREA) {
      return CrmFormCreateComponents.basicComponents.textarea;
    }
    if (type === FieldTypeEnum.DATE_TIME) {
      return CrmFormCreateComponents.basicComponents.datePicker;
    }
    if (type === FieldTypeEnum.SELECT) {
      return multiple
        ? CrmFormCreateComponents.basicComponents.multiplePick
        : CrmFormCreateComponents.basicComponents.pick;
    }
    if (type === FieldTypeEnum.PHONE) {
      return CrmFormCreateComponents.advancedComponents.phone;
    }
    if (type === FieldTypeEnum.DATA_SOURCE) {
      return CrmFormCreateComponents.advancedComponents.dataSource;
    }
  }

  function handleFieldChange(value: any, item: FormCreateField) {
    // 控制显示规则
    if (item.showControlRules?.length) {
      item.showControlRules.forEach((rule) => {
        fieldList.value.forEach((e) => {
          // 若配置了该值的显示规则，且该字段在显示规则中，则显示
          if (rule.value === value && rule.fieldIds.includes(e.id)) {
            e.show = true;
          } else if (rule.fieldIds.includes(e.id)) {
            // 若该字段在显示规则中，但值不符合，则隐藏该字段
            e.show = false;
          }
        });
      });
    }
  }

  onBeforeMount(async () => {
    await initFormConfig();
    if (route.query.id && route.query.needInitDetail === 'Y') {
      initFormDetail();
    }
  });
</script>

<style lang="less" scoped></style>
