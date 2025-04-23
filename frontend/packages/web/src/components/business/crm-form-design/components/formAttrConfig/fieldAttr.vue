<template>
  <div class="p-[16px]">
    <div v-if="fieldConfig">
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.fieldTitle') }}</div>
        <n-input
          v-model:value="fieldConfig.name"
          :disabled="fieldConfig.disabledProps?.includes('name')"
          :maxlength="255"
          :placeholder="t('common.placeholder')"
          clearable
        />
        <n-checkbox
          v-model:checked="fieldConfig.showLabel"
          :disabled="fieldConfig.disabledProps?.includes('showLabel')"
        >
          {{ t('crmFormDesign.showTitle') }}
        </n-checkbox>
      </div>
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.desc') }}</div>
        <n-input
          v-model:value="fieldConfig.description"
          :disabled="fieldConfig.disabledProps?.includes('description')"
          type="textarea"
          :maxlength="1000"
          clearable
        />
      </div>
      <div
        v-if="
          ![
            FieldTypeEnum.MEMBER,
            FieldTypeEnum.MEMBER_MULTIPLE,
            FieldTypeEnum.DEPARTMENT,
            FieldTypeEnum.DEPARTMENT_MULTIPLE,
            FieldTypeEnum.DIVIDER,
            FieldTypeEnum.PICTURE,
            FieldTypeEnum.LOCATION,
            FieldTypeEnum.PHONE,
            FieldTypeEnum.DATA_SOURCE,
            FieldTypeEnum.DATA_SOURCE_MULTIPLE,
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
        <n-input
          v-model:value="fieldConfig.placeholder"
          :disabled="fieldConfig.disabledProps?.includes('placeholder')"
          :maxlength="56"
          clearable
        />
      </div>
      <!-- inputNumber数字输入属性 -->
      <div v-if="fieldConfig.type === FieldTypeEnum.INPUT_NUMBER" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.format') }}</div>
        <n-radio-group
          v-model:value="fieldConfig.numberFormat"
          :disabled="fieldConfig.disabledProps?.includes('numberFormat')"
          name="radiogroup"
          class="flex"
        >
          <n-radio-button value="number" class="flex-1 text-center">{{ t('crmFormDesign.number') }}</n-radio-button>
          <n-radio-button value="percent" class="flex-1 text-center">{{ t('crmFormDesign.percent') }}</n-radio-button>
        </n-radio-group>
        <n-checkbox
          v-model:checked="fieldConfig.decimalPlaces"
          :disabled="fieldConfig.disabledProps?.includes('decimalPlaces')"
          @update-checked="() => (fieldConfig.precision = 0)"
        >
          {{ t('crmFormDesign.saveFloat') }}
        </n-checkbox>
        <n-checkbox
          v-if="fieldConfig.numberFormat === 'number'"
          v-model:checked="fieldConfig.showThousandsSeparator"
          :disabled="fieldConfig.disabledProps?.includes('showThousandsSeparator')"
        >
          {{ t('crmFormDesign.showThousandSeparator') }}
        </n-checkbox>
        <div v-if="fieldConfig.decimalPlaces || fieldConfig.showThousandsSeparator" class="flex items-center gap-[8px]">
          <n-input-number
            v-if="fieldConfig.decimalPlaces"
            v-model:value="fieldConfig.precision"
            :disabled="fieldConfig.disabledProps?.includes('precision')"
            :min="0"
            :max="10"
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
        <n-select
          v-model:value="fieldConfig.dateType"
          :options="dateTypeOptions"
          :disabled="fieldConfig.disabledProps?.includes('dateType')"
        />
      </div>
      <!-- date End -->
      <!-- 数据源属性 -->
      <div
        v-if="[FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(fieldConfig.type)"
        class="crm-form-design-config-item"
      >
        <div class="crm-form-design-config-item-title">
          {{ t('common.type') }}
        </div>
        <n-select
          v-model:value="fieldConfig.dataSourceType"
          :options="dataSourceOptions"
          :disabled="fieldConfig.disabledProps?.includes('dataSourceType')"
        />
      </div>
      <!-- 数据源属性 End -->
      <!-- 选项属性 -->
      <!-- <div
        v-if="
          [FieldTypeEnum.SELECT, FieldTypeEnum.MEMBER, FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DATA_SOURCE].includes(
            fieldConfig.type
          )
        "
        class="crm-form-design-config-item"
      >
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.selectType') }}
        </div>
        <n-radio-group
          v-model:value="fieldConfig.multiple"
          name="radiogroup"
          class="flex"
          :disabled="fieldConfig.disabledProps?.includes('multiple')"
          @update-value="handleMultipleChange"
        >
          <n-radio-button :value="false" class="flex-1 text-center">
            {{ t('crmFormDesign.single') }}
          </n-radio-button>
          <n-radio-button :value="true" class="flex-1 text-center">
            {{ t('crmFormDesign.multiple') }}
          </n-radio-button>
        </n-radio-group>
      </div> -->
      <div v-if="fieldConfig.options" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.option') }}
        </div>
        <optionConfig v-model:field="fieldConfig" :disabled="fieldConfig.disabledProps?.includes('options')" />
      </div>
      <!-- 选项属性 End -->
      <!-- 分割线属性 -->
      <!-- 显隐规则 -->
      <div v-if="isShowRuleField" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.showRule') }}
        </div>
        <n-button :disabled="fieldConfig.disabledProps?.includes('showControlRules')" @click="showRuleConfig">
          {{ t('common.setting') }}
        </n-button>
      </div>
      <!-- 显隐规则 End -->
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
          :disabled="fieldConfig.disabledProps?.includes('dividerClass')"
          scrollable
        >
          <template #trigger>
            <div class="crm-form-design-divider-wrapper">
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
        <div class="flex items-center justify-end gap-[8px]">
          <div class="flex-1 text-left">{{ t('crmFormDesign.dividerColor') }}</div>
          <n-popover
            trigger="click"
            placement="bottom"
            class="!p-0"
            :show-arrow="false"
            :disabled="fieldConfig.disabledProps?.includes('dividerColor')"
          >
            <template #trigger>
              <div class="crm-form-design-color-select-wrapper">
                <div class="crm-form-design-color-select" :style="{ backgroundColor: fieldConfig.dividerColor }"></div>
              </div>
            </template>
            <CrmColorSelect
              v-model:pure-color="fieldConfig.dividerColor"
              :disabled="fieldConfig.disabledProps?.includes('dividerColor')"
            />
          </n-popover>
          <n-button
            class="px-[9px]"
            :disabled="fieldConfig.disabledProps?.includes('dividerColor')"
            @click="
              () => {
                if (!fieldConfig.disabledProps?.includes('dividerColor')) {
                  fieldConfig.dividerColor = '#edf0f1';
                }
              }
            "
          >
            <CrmIcon type="iconicon_refresh" />
          </n-button>
        </div>
        <div class="flex items-center justify-end gap-[8px]">
          <div class="flex-1 text-left">{{ t('crmFormDesign.titleColor') }}</div>
          <n-popover
            trigger="click"
            placement="bottom"
            class="!p-0"
            :show-arrow="false"
            :disabled="fieldConfig.disabledProps?.includes('titleColor')"
          >
            <template #trigger>
              <div class="crm-form-design-color-select-wrapper">
                <div class="crm-form-design-color-select" :style="{ backgroundColor: fieldConfig.titleColor }"></div>
              </div>
            </template>
            <CrmColorSelect
              v-model:pure-color="fieldConfig.titleColor"
              :disabled="fieldConfig.disabledProps?.includes('titleColor')"
            />
          </n-popover>
          <n-button
            class="px-[9px]"
            @click="
              () => {
                if (!fieldConfig.disabledProps?.includes('titleColor')) {
                  fieldConfig.titleColor = '#323535';
                }
              }
            "
          >
            <CrmIcon type="iconicon_refresh" />
          </n-button>
        </div>
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
        <n-radio-group
          v-model:value="fieldConfig.direction"
          name="radiogroup"
          class="flex"
          :disabled="fieldConfig.disabledProps?.includes('direction')"
        >
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
              @click="
                () => {
                  if (!fieldConfig.disabledProps?.includes('pictureShowType')) {
                    fieldConfig.pictureShowType = 'card';
                  }
                }
              "
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
              @click="
                () => {
                  if (!fieldConfig.disabledProps?.includes('pictureShowType')) {
                    fieldConfig.pictureShowType = 'list';
                  }
                }
              "
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
          :disabled="fieldConfig.disabledProps?.includes('locationType')"
        />
      </div>
      <!-- 地址属性 End -->
      <div
        v-if="
          (!fieldConfig.options || fieldConfig.options.length === 0) &&
          ![
            FieldTypeEnum.DIVIDER,
            FieldTypeEnum.PICTURE,
            FieldTypeEnum.LOCATION,
            FieldTypeEnum.PHONE,
            FieldTypeEnum.MULTIPLE_INPUT,
          ].includes(fieldConfig.type)
        "
        class="crm-form-design-config-item"
      >
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.defaultValue') }}</div>
        <div
          v-if="[FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(fieldConfig.type)"
          class="flex items-center gap-[8px]"
        >
          <n-switch
            v-model:value="fieldConfig.hasCurrentUser"
            :disabled="fieldConfig.disabledProps?.includes('hasCurrentUser')"
            @update-value="
              ($event) => handleHasCurrentChange($event, fieldConfig.type === FieldTypeEnum.MEMBER_MULTIPLE)
            "
          />
          {{ t('crmFormDesign.loginUser') }}
        </div>
        <div
          v-else-if="[FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(fieldConfig.type)"
          class="flex items-center gap-[8px]"
        >
          <n-switch
            v-model:value="fieldConfig.hasCurrentUserDept"
            :disabled="fieldConfig.disabledProps?.includes('hasCurrentUserDept')"
            @update-value="
              ($event) => handleHasCurrentChange($event, fieldConfig.type === FieldTypeEnum.DEPARTMENT_MULTIPLE)
            "
          />
          {{ t('crmFormDesign.loginUserDept') }}
        </div>
        <n-input-number
          v-if="fieldConfig.type === FieldTypeEnum.INPUT_NUMBER"
          v-model:value="fieldConfig.defaultValue"
          :show-button="false"
          :min="0"
          :disabled="fieldConfig.disabledProps?.includes('defaultValue')"
        />
        <n-date-picker
          v-else-if="fieldConfig.type === FieldTypeEnum.DATE_TIME"
          v-model:value="fieldConfig.defaultValue"
          :type="fieldConfig.dateType"
          :disabled="fieldConfig.disabledProps?.includes('defaultValue')"
          class="w-full"
        ></n-date-picker>
        <CrmUserTagSelector
          v-else-if="[FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(fieldConfig.type)"
          v-show="fieldConfig.type === FieldTypeEnum.MEMBER_MULTIPLE || !fieldConfig.hasCurrentUser"
          v-model:selected-list="fieldConfig.initialOptions"
          v-model:value="fieldConfig.defaultValue"
          :multiple="fieldConfig.type === FieldTypeEnum.MEMBER_MULTIPLE"
          :drawer-title="t('crmFormDesign.selectMember')"
          :ok-text="t('common.confirm')"
          :member-types="[]"
          :disabled="fieldConfig.disabledProps?.includes('defaultValue')"
          :disabled-node-types="[DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE]"
        />
        <CrmUserTagSelector
          v-else-if="[FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(fieldConfig.type)"
          v-show="fieldConfig.type === FieldTypeEnum.DEPARTMENT_MULTIPLE || !fieldConfig.hasCurrentUserDept"
          v-model:selected-list="fieldConfig.initialOptions"
          v-model:value="fieldConfig.defaultValue"
          :multiple="fieldConfig.type === FieldTypeEnum.DEPARTMENT_MULTIPLE"
          :drawer-title="t('crmFormDesign.selectMember')"
          :ok-text="t('common.confirm')"
          :member-types="[]"
          :disabled="fieldConfig.disabledProps?.includes('defaultValue')"
          :disabled-node-types="[DeptNodeTypeEnum.USER, DeptNodeTypeEnum.ROLE]"
        />
        <CrmDataSource
          v-else-if="[FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(fieldConfig.type)"
          v-model:value="fieldConfig.defaultValue"
          v-model:rows="fieldConfig.initialOptions"
          :multiple="fieldConfig.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE"
          :data-source-type="fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER"
          :disabled="fieldConfig.disabledProps?.includes('defaultValue')"
        />
        <n-input
          v-else
          v-model:value="fieldConfig.defaultValue"
          :maxlength="255"
          :disabled="fieldConfig.disabledProps?.includes('defaultValue')"
          clearable
        />
      </div>
      <div v-if="showRules.length > 0" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.validator') }}</div>
        <n-checkbox-group
          v-model:value="checkedRules"
          :disabled="fieldConfig.disabledProps?.includes('rules')"
          @update-value="handleRuleChange"
        >
          <n-space item-class="w-full">
            <n-checkbox v-for="rule of showRules" :key="rule.key" :value="rule.key">
              {{ t(rule.label || '', { value: t(fieldConfig.name) }) }}
            </n-checkbox>
          </n-space>
        </n-checkbox-group>
      </div>
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.fieldPermission') }}
        </div>
        <n-checkbox v-model:checked="fieldConfig.readable" :disabled="fieldConfig.disabledProps?.includes('readable')">
          {{ t('crmFormDesign.readable') }}
        </n-checkbox>
        <n-checkbox
          v-if="![FieldTypeEnum.DIVIDER].includes(fieldConfig.type)"
          v-model:checked="fieldConfig.editable"
          :disabled="fieldConfig.disabledProps?.includes('editable')"
        >
          {{ t('crmFormDesign.editable') }}
        </n-checkbox>
      </div>
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.fieldWidth') }}
        </div>
        <n-radio-group
          v-model:value="fieldConfig.fieldWidth"
          name="radiogroup"
          class="flex"
          :disabled="fieldConfig.disabledProps?.includes('fieldWidth')"
        >
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
  <CrmModal
    v-model:show="showRuleConfigVisible"
    :title="t('crmFormDesign.showRuleSetting')"
    :positive-text="t('common.save')"
    @confirm="handleShowRuleConfigConfirm"
  >
    <div class="flex flex-col items-start gap-[12px]">
      <div v-for="rule in tempShowRules" :key="rule.value" class="flex w-full items-center gap-[8px]">
        <div>{{ t('crmFormDesign.choice') }}</div>
        <n-select
          v-model:value="rule.value"
          :options="getShowRuleOptions(rule)"
          :disabled="props.disabled"
          class="w-[150px]"
        />
        <div>{{ t('crmFormDesign.show') }}</div>
        <n-select
          v-model:value="rule.fieldIds"
          :options="showRuleFields"
          :disabled="props.disabled || !rule.value"
          class="w-[350px]"
          max-tag-count="responsive"
          multiple
          clearable
        />
        <n-button :disabled="props.disabled" @click="deleteShowRule(rule)">
          <CrmIcon type="iconicon_minus_circle1" />
        </n-button>
      </div>
      <n-button
        text
        type="primary"
        :disabled="props.disabled || tempShowRules.length === fieldConfig.options?.length"
        @click="addShowRule"
      >
        <div class="flex items-center gap-[8px]">
          <CrmIcon type="iconicon_add" />
          {{ t('crmFormDesign.addRule') }}
        </div>
      </n-button>
    </div>
  </CrmModal>
</template>

<script setup lang="ts">
  import {
    NButton,
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
  import { cloneDeep } from 'lodash-es';

  import { FieldDataSourceTypeEnum, FieldRuleEnum, FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmColorSelect from '@/components/pure/crm-color-select/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmDataSource from '@/components/business/crm-data-source-select/index.vue';
  import Divider from '@/components/business/crm-form-create/components/basic/divider.vue';
  import { rules, showRulesMap } from '@/components/business/crm-form-create/config';
  import {
    FormCreateField,
    FormCreateFieldRule,
    FormCreateFieldShowControlRule,
  } from '@/components/business/crm-form-create/types';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';
  import optionConfig from './optionConfig.vue';

  import { SelectOption } from 'naive-ui/es/select/src/interface';

  const props = defineProps<{
    list: FormCreateField[];
    disabled?: boolean;
  }>();

  const { t } = useI18n();

  const fieldConfig = defineModel<FormCreateField>('field', {
    default: null,
  });

  const showRules = computed(() => {
    if (!fieldConfig.value) {
      return [];
    }
    return rules.filter((rule) => {
      if (
        [
          FieldTypeEnum.SELECT_MULTIPLE,
          FieldTypeEnum.MEMBER_MULTIPLE,
          FieldTypeEnum.CHECKBOX,
          FieldTypeEnum.DEPARTMENT_MULTIPLE,
          FieldTypeEnum.DATA_SOURCE_MULTIPLE,
        ].includes(fieldConfig.value.type)
      ) {
        // 多选时不显示唯一性校验
        return rule.key && showRulesMap[fieldConfig.value.type].includes(rule.key) && rule.key !== FieldRuleEnum.UNIQUE;
      }
      return rule.key && showRulesMap[fieldConfig.value.type].includes(rule.key);
    });
  });

  const checkedRules = ref<(string | number)[]>([]);

  watch(
    () => fieldConfig.value?.rules,
    (arr) => {
      checkedRules.value = arr.map((e) => e.key);
    }
  );

  function handleRuleChange(val: (string | number)[]) {
    fieldConfig.value.rules = val
      .map((e) => {
        const rule = rules.find((item) => item.key === e);
        if (rule) {
          return { key: rule.key };
        }
        return null;
      })
      .filter((e) => e !== null) as FormCreateFieldRule[];
  }

  // function handleMultipleChange(val: boolean) {
  //   if (val || [FieldTypeEnum.MEMBER, FieldTypeEnum.DEPARTMENT].includes(fieldConfig.value.type)) {
  //     fieldConfig.value.defaultValue = [];
  //   } else {
  //     fieldConfig.value.defaultValue = null;
  //   }
  // }

  function handleHasCurrentChange(val: boolean, multiple: boolean) {
    if (val && !multiple) {
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
    {
      label: t('crmFormDesign.clue'),
      value: FieldDataSourceTypeEnum.CLUE,
    },
  ];

  const showRuleConfigVisible = ref(false);
  const tempShowRules = ref<FormCreateFieldShowControlRule[]>([]);
  const isShowRuleField = computed(() => {
    return fieldConfig.value.type === FieldTypeEnum.RADIO || fieldConfig.value.type === FieldTypeEnum.SELECT_MULTIPLE;
  });
  // 显隐规则可选字段
  const showRuleFields = computed(() => {
    if (isShowRuleField.value) {
      return props.list.filter((item) => item.id !== fieldConfig.value.id).map((e) => ({ label: e.name, value: e.id }));
    }
    return [];
  });

  // 显隐规则可选选项
  function getShowRuleOptions(rule: FormCreateFieldShowControlRule) {
    return (fieldConfig.value.options || []).filter(
      (e) => tempShowRules.value.every((item) => item.value !== e.value) || e.value === rule.value
    );
  }

  function showRuleConfig() {
    tempShowRules.value = fieldConfig.value.showControlRules || [];
    showRuleConfigVisible.value = true;
  }

  function deleteShowRule(rule: FormCreateFieldShowControlRule) {
    tempShowRules.value = tempShowRules.value.filter((item) => item !== rule);
  }

  function addShowRule() {
    tempShowRules.value.push({ value: undefined, fieldIds: [] });
  }

  function handleShowRuleConfigConfirm() {
    showRuleConfigVisible.value = false;
    fieldConfig.value.showControlRules = cloneDeep(tempShowRules.value);
  }
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
  .crm-form-design-color-select-wrapper {
    @apply cursor-pointer;

    padding: 4px;
    width: 130px;
    height: 32px;
    border: 1px solid var(--text-n7);
    border-radius: var(--border-radius-small);
    .crm-form-design-color-select {
      @apply h-full w-full;
    }
  }
</style>
