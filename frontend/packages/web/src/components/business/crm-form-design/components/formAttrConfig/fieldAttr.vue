<template>
  <div class="p-[16px]">
    <div v-if="fieldConfig">
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.fieldTitle') }}</div>
        <n-input v-model:value="fieldConfig.name" :maxlength="255" :placeholder="t('common.placeholder')" clearable />
        <n-checkbox v-model:checked="fieldConfig.showLabel">
          {{ t('crmFormDesign.showTitle') }}
        </n-checkbox>
      </div>
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.desc') }}</div>
        <n-input v-model:value="fieldConfig.description" type="textarea" :maxlength="1000" clearable />
      </div>
      <div
        v-if="
          ![
            FieldTypeEnum.MEMBER,
            FieldTypeEnum.DEPARTMENT,
            FieldTypeEnum.DIVIDER,
            FieldTypeEnum.PICTURE,
            FieldTypeEnum.LOCATION,
            FieldTypeEnum.PHONE,
            FieldTypeEnum.DATA_SOURCE,
          ].includes(fieldConfig.type)
        "
        class="crm-form-design-config-item"
      >
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.placeholder') }}
          <n-tooltip trigger="hover">
            <template #trigger>
              <CrmIcon type="iconicon_help_circle" class="cursor-pointer hover:text-[var(--primary-1)]" />
            </template>
            {{ t('crmFormDesign.placeholderTip') }}
          </n-tooltip>
        </div>
        <n-input v-model:value="fieldConfig.placeholder" :maxlength="56" clearable />
      </div>
      <!-- inputNumber数字输入属性 -->
      <div v-if="fieldConfig.type === FieldTypeEnum.INPUT_NUMBER" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.format') }}</div>
        <n-radio-group v-model:value="fieldConfig.numberFormat" name="radiogroup" class="flex">
          <n-radio-button value="number" class="flex-1 text-center">{{ t('crmFormDesign.number') }}</n-radio-button>
          <n-radio-button value="percent" class="flex-1 text-center">{{ t('crmFormDesign.percent') }}</n-radio-button>
        </n-radio-group>
        <n-checkbox v-model:checked="fieldConfig.decimalPlaces" @update-checked="() => (fieldConfig.precision = 0)">
          {{ t('crmFormDesign.saveFloat') }}
        </n-checkbox>
        <n-checkbox v-if="fieldConfig.numberFormat === 'number'" v-model:checked="fieldConfig.showThousandsSeparator">
          {{ t('crmFormDesign.showThousandSeparator') }}
        </n-checkbox>
        <div v-if="fieldConfig.decimalPlaces || fieldConfig.showThousandsSeparator" class="flex items-center gap-[8px]">
          <n-input-number
            v-if="fieldConfig.decimalPlaces"
            v-model:value="fieldConfig.precision"
            :min="0"
            class="flex-1"
          />
          <div
            class="flex flex-1 items-center gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] px-[8px] py-[4px]"
          >
            <div class="text-[var(--text-n4)]">{{ t('common.preview') }}</div>
            {{ numberPreview }}
          </div>
        </div>
      </div>
      <!-- inputNumber End -->
      <!-- date 日期输入属性 -->
      <div v-if="fieldConfig.type === FieldTypeEnum.DATE_TIME" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('common.type') }}
        </div>
        <n-select v-model:value="fieldConfig.datetype" :options="dateTypeOptions" />
      </div>
      <!-- date End -->
      <!-- 数据源属性 -->
      <div v-if="fieldConfig.type === FieldTypeEnum.DATA_SOURCE" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('common.type') }}
        </div>
        <n-select v-model:value="fieldConfig.dataSourceType" :options="dataSourceOptions" />
      </div>
      <!-- 数据源属性 End -->
      <!-- 选项属性 -->
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.selectType') }}
        </div>
        <n-radio-group
          v-model:value="fieldConfig.multiple"
          name="radiogroup"
          class="flex"
          @update-value="handleMultipleChange"
        >
          <n-radio-button :value="false" class="flex-1 text-center">
            {{ t('crmFormDesign.single') }}
          </n-radio-button>
          <n-radio-button :value="true" class="flex-1 text-center">
            {{ t('crmFormDesign.multiple') }}
          </n-radio-button>
        </n-radio-group>
      </div>
      <div v-if="fieldConfig.options" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.option') }}
        </div>
        <optionConfig v-model:field="fieldConfig" />
      </div>
      <!-- 选项属性 End -->
      <!-- 分割线属性 -->
      <div v-if="fieldConfig.type === FieldTypeEnum.DIVIDER" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.style') }}
        </div>
        <n-popover
          v-model:show="dividerStyleShow"
          trigger="click"
          placement="bottom"
          width="trigger"
          :show-arrow="false"
          style="max-height: 360px"
          scrollable
        >
          <template #trigger>
            <div class="crm-form-design-divider-wrapper crm-form-design-divider-wrapper--active">
              <Divider
                :field-config="{
                  ...fieldConfig,
                  description: t('crmFormDesign.dividerDescDemo'),
                }"
              />
            </div>
          </template>
          <div
            v-for="option in dividerOptions"
            :key="option.value"
            class="crm-form-design-divider-wrapper mb-[6px]"
            :class="fieldConfig.dividerClass === option.value ? 'crm-form-design-divider-wrapper--active' : ''"
            @click="handleDividerStyleClick(option.value as string)"
          >
            <Divider
              :field-config="{
                  ...fieldConfig,
                  dividerClass: option.value as string,
                  description: t('crmFormDesign.dividerDescDemo'),
                }"
            />
          </div>
        </n-popover>
      </div>
      <!-- 分割线属性 End -->
      <!-- 单选/复选框排列属性 -->
      <div
        v-if="[FieldTypeEnum.RADIO, FieldTypeEnum.CHECKBOX].includes(fieldConfig.type)"
        class="crm-form-design-config-item"
      >
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.direction') }}
        </div>
        <n-radio-group v-model:value="fieldConfig.direction" name="radiogroup" class="flex">
          <n-radio-button value="vertical" class="flex-1 text-center">
            {{ t('crmFormDesign.verticalSort') }}
          </n-radio-button>
          <n-radio-button value="horizontal" class="flex-1 text-center">
            {{ t('crmFormDesign.horizontalSort') }}
          </n-radio-button>
        </n-radio-group>
      </div>
      <!-- 单选/复选框排列属性 End -->
      <!-- 图片属性 -->
      <template v-if="fieldConfig.type === FieldTypeEnum.PICTURE">
        <div class="crm-form-design-config-item">
          <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.pictureShowType') }}</div>
          <div class="flex items-center gap-[8px]">
            <div
              class="crm-form-design-config-item-label-picture"
              :class="fieldConfig.pictureShowType === 'card' ? 'crm-form-design-config-item-label-picture--active' : ''"
              @click="() => (fieldConfig.pictureShowType = 'card')"
            >
              <div class="crm-form-design-config-item-label-picture-card !flex-row justify-between">
                <div class="crm-form-design-config-item-label-picture-card-first flex flex-col">
                  <div class="crm-form-design-config-item-label-picture-card-heavy mb-[4px] h-[16px] w-[21px]"></div>
                  <div class="crm-form-design-config-item-label-picture-card-heavy mb-[2px] h-[3px] w-[21px]"></div>
                  <div class="crm-form-design-config-item-label-picture-card-light mb-[2px] h-[3px] w-[21px]"></div>
                  <div class="crm-form-design-config-item-label-picture-card-light h-[3px] w-[11px]"></div>
                </div>
                <div class="flex flex-col">
                  <div class="crm-form-design-config-item-label-picture-card-light mb-[4px] h-[16px] w-[21px]"></div>
                  <div class="crm-form-design-config-item-label-picture-card-light mb-[2px] h-[3px] w-[21px]"></div>
                  <div class="crm-form-design-config-item-label-picture-card-light mb-[2px] h-[3px] w-[21px]"></div>
                  <div class="crm-form-design-config-item-label-picture-card-light h-[3px] w-[11px]"></div>
                </div>
                <div class="flex flex-col">
                  <div class="crm-form-design-config-item-label-picture-card-light mb-[4px] h-[16px] w-[21px]"></div>
                  <div class="crm-form-design-config-item-label-picture-card-light mb-[2px] h-[3px] w-[21px]"></div>
                  <div class="crm-form-design-config-item-label-picture-card-light mb-[2px] h-[3px] w-[21px]"></div>
                  <div class="crm-form-design-config-item-label-picture-card-light h-[3px] w-[11px]"></div>
                </div>
              </div>
              {{ t('crmFormDesign.card') }}
            </div>
            <div
              class="crm-form-design-config-item-label-picture"
              :class="fieldConfig.pictureShowType === 'list' ? 'crm-form-design-config-item-label-picture--active' : ''"
              @click="() => (fieldConfig.pictureShowType = 'list')"
            >
              <div class="crm-form-design-config-item-label-picture-card">
                <div class="crm-form-design-config-item-label-picture-card-first flex items-center gap-[4px]">
                  <div class="crm-form-design-config-item-label-picture-card-heavy h-[16px] w-[21px]"></div>
                  <div class="flex flex-1 flex-col gap-[4px]">
                    <div class="crm-form-design-config-item-label-picture-card-light h-[3px]"></div>
                    <div class="crm-form-design-config-item-label-picture-card-light h-[3px] w-[50%]"></div>
                  </div>
                </div>
                <div class="flex items-center gap-[4px]">
                  <div class="crm-form-design-config-item-label-picture-card-heavy h-[16px] w-[21px]"></div>
                  <div class="flex flex-1 flex-col gap-[4px]">
                    <div class="crm-form-design-config-item-label-picture-card-light h-[3px]"></div>
                    <div class="crm-form-design-config-item-label-picture-card-light h-[3px] w-[50%]"></div>
                  </div>
                </div>
              </div>
              {{ t('crmFormDesign.list') }}
            </div>
          </div>
        </div>
        <div class="crm-form-design-config-item">
          <div class="crm-form-design-config-item-title">
            {{ t('crmFormDesign.pictureLimit') }}
            <n-tooltip trigger="hover">
              <template #trigger>
                <CrmIcon type="iconicon_help_circle" class="cursor-pointer hover:text-[var(--primary-1)]" />
              </template>
              {{ t('crmFormDesign.pictureLimitDesc') }}
            </n-tooltip>
          </div>
          <div class="flex flex-col gap-[8px]">
            <n-checkbox
              v-model:checked="fieldConfig.uploadLimitEnable"
              @update-checked="() => (fieldConfig.uploadLimit = 10)"
            >
              {{ t('crmFormDesign.pictureNumLimit') }}
            </n-checkbox>
            <n-input-number
              v-if="fieldConfig.uploadLimitEnable"
              v-model:value="fieldConfig.uploadLimit"
              :step="1"
              :min="1"
              :max="10"
              class="w-[130px]"
              button-placement="both"
            >
              <template #suffix>
                <div class="text-[var(--text-n4)]">{{ t('crmFormDesign.pictureNumUnit') }}</div>
              </template>
            </n-input-number>
          </div>
          <div class="flex flex-col gap-[8px]">
            <n-checkbox
              v-model:checked="fieldConfig.uploadSizeLimitEnable"
              @update-checked="() => (fieldConfig.uploadSizeLimit = 20)"
            >
              <div class="flex items-center gap-[4px]">
                {{ t('crmFormDesign.pictureSizeLimit') }}
                <n-tooltip trigger="hover">
                  <template #trigger>
                    <CrmIcon type="iconicon_help_circle" class="cursor-pointer hover:text-[var(--primary-1)]" />
                  </template>
                  {{ t('crmFormDesign.pictureSizeLimitTip') }}
                </n-tooltip>
              </div>
            </n-checkbox>
            <n-input-number
              v-if="fieldConfig.uploadSizeLimitEnable"
              v-model:value="fieldConfig.uploadSizeLimit"
              :step="1"
              :min="0"
              :max="20"
              class="w-[130px]"
              button-placement="both"
            >
              <template #suffix>
                <div class="text-[var(--text-n4)]">MB</div>
              </template>
            </n-input-number>
          </div>
        </div>
      </template>
      <!-- 图片属性 End -->
      <!-- 地址属性 -->
      <div v-if="fieldConfig.type === FieldTypeEnum.LOCATION" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('common.type') }}
        </div>
        <n-select
          v-model:value="fieldConfig.locationType"
          :options="[
            {
              label: t('crmFormDesign.PCD'),
              value: 'PCD',
            },
            {
              label: t('crmFormDesign.PCDDetail'),
              value: 'detail',
            },
          ]"
        />
      </div>
      <!-- 地址属性 End -->
      <div
        v-if="
          (!fieldConfig.options || fieldConfig.options.length === 0) &&
          ![FieldTypeEnum.DIVIDER, FieldTypeEnum.PICTURE, FieldTypeEnum.LOCATION, FieldTypeEnum.PHONE].includes(
            fieldConfig.type
          )
        "
        class="crm-form-design-config-item"
      >
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.defaultValue') }}</div>
        <div v-if="fieldConfig.type === FieldTypeEnum.MEMBER" class="flex items-center gap-[8px]">
          <n-switch v-model:value="fieldConfig.hasCurrentUser" @update-value="handleHasCurrentChange" />
          {{ t('crmFormDesign.loginUser') }}
        </div>
        <div v-else-if="fieldConfig.type === FieldTypeEnum.DEPARTMENT" class="flex items-center gap-[8px]">
          <n-switch v-model:value="fieldConfig.hasCurrentUserDept" @update-value="handleHasCurrentChange" />
          {{ t('crmFormDesign.loginUserDept') }}
        </div>
        <n-input-number
          v-if="fieldConfig.type === FieldTypeEnum.INPUT_NUMBER"
          v-model:value="fieldConfig.defaultValue"
          :show-button="false"
          :min="0"
        />
        <n-date-picker
          v-else-if="fieldConfig.type === FieldTypeEnum.DATE_TIME"
          v-model:value="fieldConfig.defaultValue"
          :type="fieldConfig.datetype"
          class="w-full"
        ></n-date-picker>
        <CrmUserTagSelector
          v-else-if="fieldConfig.type === FieldTypeEnum.MEMBER"
          v-show="fieldConfig.multiple || !fieldConfig.hasCurrentUser"
          v-model:selected-list="fieldConfig.defaultValue"
          :multiple="fieldConfig.multiple"
          :drawer-title="t('crmFormDesign.selectMember')"
          :ok-text="t('common.confirm')"
          :member-types="[]"
        />
        <CrmUserTagSelector
          v-else-if="fieldConfig.type === FieldTypeEnum.DEPARTMENT"
          v-show="fieldConfig.multiple || !fieldConfig.hasCurrentUserDept"
          v-model:selected-list="fieldConfig.defaultValue"
          :multiple="fieldConfig.multiple"
          :drawer-title="t('crmFormDesign.selectMember')"
          :ok-text="t('common.confirm')"
          :member-types="[]"
        />
        <CrmDataSource
          v-else-if="fieldConfig.type === FieldTypeEnum.DATA_SOURCE"
          v-model:value="fieldConfig.defaultValue"
          v-model:rows="fieldConfig.dataSourceSelectedRows"
          :multiple="fieldConfig.multiple"
          :data-source-type="fieldConfig.dataSourceType"
        />
        <n-input v-else v-model:value="fieldConfig.defaultValue" :maxlength="255" clearable />
      </div>
      <div v-if="showRules.length > 0" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.validator') }}</div>

        <n-checkbox-group v-model:value="checkedRules">
          <n-space item-class="w-full">
            <n-checkbox v-for="rule of showRules" :key="rule.key" :value="rule.key">
              {{ t(rule.label, { value: t(fieldConfig.name) }) }}
            </n-checkbox>
          </n-space>
        </n-checkbox-group>
      </div>
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.fieldPermission') }}
        </div>

        <n-checkbox v-model:checked="fieldConfig.readable">
          {{ t('crmFormDesign.readable') }}
        </n-checkbox>

        <n-checkbox v-if="![FieldTypeEnum.DIVIDER].includes(fieldConfig.type)" v-model:checked="fieldConfig.editable">
          {{ t('crmFormDesign.editable') }}
        </n-checkbox>
      </div>
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.fieldWidth') }}
        </div>

        <n-radio-group v-model:value="fieldConfig.fieldWidth" name="radiogroup" class="flex">
          <n-radio-button :value="1 / 4" class="!px-[8px]"> 1/4 </n-radio-button>
          <n-radio-button :value="1 / 3" class="!px-[8px]"> 1/3 </n-radio-button>
          <n-radio-button :value="1 / 2" class="!px-[8px]"> 1/2 </n-radio-button>
          <n-radio-button :value="2 / 3" class="!px-[8px]"> 2/3 </n-radio-button>
          <n-radio-button :value="3 / 4" class="!px-[8px]"> 3/4 </n-radio-button>
          <n-radio-button :value="1" class="!px-[8px]">
            {{ t('crmFormDesign.wholeLine') }}
          </n-radio-button>
        </n-radio-group>
      </div>
    </div>
    <div v-else class="flex justify-center py-[44px] text-[var(--text-n4)]">
      {{ t('crmFormDesign.fieldConfigEmptyTip') }}
    </div>
  </div>
</template>

<script setup lang="ts">
  import {
    NCheckbox,
    NCheckboxGroup,
    NDatePicker,
    NInput,
    NInputNumber,
    NPopover,
    NRadioButton,
    NRadioGroup,
    NSelect,
    NSpace,
    NSwitch,
    NTooltip,
  } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmDataSource from '@/components/business/crm-data-source-select/index.vue';
  import Divider from '@/components/business/crm-form-create/components/basic/divider.vue';
  import { rules } from '@/components/business/crm-form-create/config';
  import { FieldDataSourceTypeEnum, FieldRuleEnum, FieldTypeEnum } from '@/components/business/crm-form-create/enum';
  import { FormCreateField } from '@/components/business/crm-form-create/types';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';
  import optionConfig from './optionConfig.vue';

  import { useI18n } from '@/hooks/useI18n';

  import { SelectOption } from 'naive-ui/es/select/src/interface';

  const { t } = useI18n();

  const fieldConfig = defineModel<FormCreateField>('field', {
    default: null,
  });

  const showRules = computed(() => {
    return rules.filter((rule) => {
      if (fieldConfig.value.multiple) {
        // 多选时不显示唯一性校验
        return rule.key && fieldConfig.value.showRules?.includes(rule.key) && rule.key !== FieldRuleEnum.UNIQUE;
      }
      return rule.key && fieldConfig.value.showRules?.includes(rule.key);
    });
  });

  const checkedRules = ref<string[]>([]);

  watch(
    () => checkedRules.value,
    (value) => {
      fieldConfig.value.rules = showRules.value
        .filter((rule) => rule.key && value.includes(rule.key))
        .map((rule) => ({
          ...rule,
          label: t(rule.label, { value: t(fieldConfig.value.name) }),
        }));
    }
  );

  function handleMultipleChange(val: boolean) {
    if (val || [FieldTypeEnum.MEMBER, FieldTypeEnum.DEPARTMENT].includes(fieldConfig.value.type)) {
      fieldConfig.value.defaultValue = [];
    } else {
      fieldConfig.value.defaultValue = null;
    }
  }

  function handleHasCurrentChange(val: boolean) {
    if (val && !fieldConfig.value.multiple) {
      fieldConfig.value.defaultValue = [];
    }
  }

  const numberPreview = computed(() => {
    const tempVal = 9999;
    if (fieldConfig.value.numberFormat === 'percent') {
      if (fieldConfig.value.decimalPlaces && fieldConfig.value.precision) {
        return `99.${'0'.repeat(fieldConfig.value.precision)}%`;
      }
      return '99%';
    }
    if (fieldConfig.value.showThousandsSeparator && fieldConfig.value.decimalPlaces && fieldConfig.value.precision) {
      return `${tempVal.toLocaleString('en-US')}.${'0'.repeat(fieldConfig.value.precision)}`;
    }
    if (fieldConfig.value.showThousandsSeparator) {
      return tempVal.toLocaleString('en-US');
    }
    if (fieldConfig.value.decimalPlaces && fieldConfig.value.precision) {
      return `9999.${'0'.repeat(fieldConfig.value.precision)}`;
    }
    return 9999;
  });

  const dateTypeOptions = [
    {
      label: t('crmFormDesign.monthType'),
      value: 'month',
    },
    {
      label: t('crmFormDesign.dayType'),
      value: 'date',
    },
    {
      label: t('crmFormDesign.timeType'),
      value: 'datetime',
    },
  ];

  const dividerOptions: SelectOption[] = [
    {
      value: 'divider--hidden',
    },
    {
      value: 'divider--dashed',
    },
    {
      value: 'divider--normal',
    },
    {
      value: 'divider--double',
    },
  ];

  const dividerStyleShow = ref(false);
  function handleDividerStyleClick(value: string) {
    fieldConfig.value.dividerClass = value;
    dividerStyleShow.value = false;
  }

  const dataSourceOptions: SelectOption[] = [
    {
      label: t('crmFormDesign.customer'),
      value: FieldDataSourceTypeEnum.CUSTOMER,
    },
    {
      label: t('crmFormDesign.contract'),
      value: FieldDataSourceTypeEnum.CONTACT,
    },
    {
      label: t('crmFormDesign.business'),
      value: FieldDataSourceTypeEnum.BUSINESS,
    },
    {
      label: t('crmFormDesign.product'),
      value: FieldDataSourceTypeEnum.PRODUCT,
    },
  ];
</script>

<style lang="less" scoped>
  .crm-form-design-divider-wrapper {
    padding: 16px;
    border: 1px solid var(--text-n7);
    cursor: pointer;
    border-radius: 4px;
    &--active {
      border-color: var(--primary-8);
      background-color: var(--primary-7);
    }
  }
  .crm-form-design-config-item-label-picture {
    @apply flex flex-1 cursor-pointer flex-col items-center;

    gap: 4px;
    .crm-form-design-config-item-label-picture-card {
      @apply flex w-full flex-col;

      padding: 8px;
      border: 1px solid var(--text-n7);
      border-radius: var(--border-radius-small);
      gap: 4px;
      .crm-form-design-config-item-label-picture-card-heavy {
        border-radius: var(--border-radius-mini);
        background-color: var(--text-n7);
      }
      .crm-form-design-config-item-label-picture-card-light {
        border-radius: var(--border-radius-mini);
        background-color: var(--text-n8);
      }
    }
    &--active {
      color: var(--primary-8);
      .crm-form-design-config-item-label-picture-card {
        border-color: var(--primary-8);
        .crm-form-design-config-item-label-picture-card-first {
          .crm-form-design-config-item-label-picture-card-heavy {
            background-color: var(--primary-4);
          }
          .crm-form-design-config-item-label-picture-card-light {
            background-color: var(--primary-6);
          }
        }
      }
    }
  }
</style>
