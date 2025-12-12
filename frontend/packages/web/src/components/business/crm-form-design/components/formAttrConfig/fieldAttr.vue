<template>
  <div class="p-[16px]">
    <div v-if="fieldConfig">
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.fieldTitle') }}</div>
        <n-input
          v-model:value="fieldConfig.name"
          :disabled="fieldConfig.disabledProps?.includes('name')"
          :maxlength="16"
          :placeholder="t('common.pleaseInput')"
          :status="isNameRepeat ? 'error' : undefined"
          clearable
        />
        <div v-if="isNameRepeat" class="text-[12px] text-[var(--error-red)]">
          {{ t('crmFormDesign.repeatFieldName') }}
        </div>
        <n-checkbox
          v-model:checked="fieldConfig.showLabel"
          :disabled="fieldConfig.disabledProps?.includes('showLabel') || !!fieldConfig.resourceFieldId"
        >
          {{ t('crmFormDesign.showTitle') }}
        </n-checkbox>
      </div>
      <div v-if="!isSubTableField" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.desc') }}</div>
        <n-input
          v-model:value="fieldConfig.description"
          :disabled="fieldConfig.disabledProps?.includes('description') || !!fieldConfig.resourceFieldId"
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
            FieldTypeEnum.PHONE,
            FieldTypeEnum.DATA_SOURCE,
            FieldTypeEnum.DATA_SOURCE_MULTIPLE,
            FieldTypeEnum.ATTACHMENT,
            FieldTypeEnum.FORMULA,
            FieldTypeEnum.SUB_PRICE,
            FieldTypeEnum.SUB_PRODUCT,
            FieldTypeEnum.RADIO,
            FieldTypeEnum.CHECKBOX,
          ].includes(fieldConfig.type)
        "
        class="crm-form-design-config-item"
      >
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.placeholder') }}
          <n-tooltip trigger="hover">
            <template #trigger>
              <CrmIcon
                type="iconicon_help_circle"
                class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
              />
            </template>
            {{ t('crmFormDesign.placeholderTip') }}
          </n-tooltip>
        </div>
        <n-input
          v-model:value="fieldConfig.placeholder"
          :disabled="fieldConfig.disabledProps?.includes('placeholder') || !!fieldConfig.resourceFieldId"
          :maxlength="56"
          clearable
        />
      </div>
      <!-- inputNumber数字输入属性 -->
      <div v-if="fieldConfig.type === FieldTypeEnum.INPUT_NUMBER" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.format') }}</div>
        <n-radio-group
          v-model:value="fieldConfig.numberFormat"
          :disabled="fieldConfig.disabledProps?.includes('numberFormat') || !!fieldConfig.resourceFieldId"
          name="radiogroup"
          class="flex"
        >
          <n-radio-button value="number" class="flex-1 text-center">{{ t('crmFormDesign.number') }}</n-radio-button>
          <n-radio-button value="percent" class="flex-1 text-center">{{ t('crmFormDesign.percent') }}</n-radio-button>
        </n-radio-group>
        <n-checkbox
          v-model:checked="fieldConfig.decimalPlaces"
          :disabled="fieldConfig.disabledProps?.includes('decimalPlaces') || !!fieldConfig.resourceFieldId"
          @update-checked="() => (fieldConfig.precision = 0)"
        >
          {{ t('crmFormDesign.saveFloat') }}
        </n-checkbox>
        <n-checkbox
          v-if="fieldConfig.numberFormat === 'number'"
          v-model:checked="fieldConfig.showThousandsSeparator"
          :disabled="fieldConfig.disabledProps?.includes('showThousandsSeparator') || !!fieldConfig.resourceFieldId"
        >
          {{ t('crmFormDesign.showThousandSeparator') }}
        </n-checkbox>
        <div v-if="fieldConfig.decimalPlaces || fieldConfig.showThousandsSeparator" class="flex items-center gap-[8px]">
          <CrmInputNumber
            v-if="fieldConfig.decimalPlaces"
            v-model:value="fieldConfig.precision"
            :disabled="fieldConfig.disabledProps?.includes('precision') || !!fieldConfig.resourceFieldId"
            :min="0"
            :max="4"
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
      <template v-if="[FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(fieldConfig.type)">
        <div class="crm-form-design-config-item">
          <div class="crm-form-design-config-item-title">
            {{ t('crmFormDesign.dataSource') }}
          </div>
          <n-select
            v-model:value="fieldConfig.dataSourceType"
            :options="dataSourceOptions"
            :disabled="fieldConfig.disabledProps?.includes('dataSourceType') || !!fieldConfig.resourceFieldId"
            @update-value="() => handleClearDataSourceDisplayField()"
          />
        </div>
        <div class="crm-form-design-config-item">
          <div class="crm-form-design-config-item-title">
            {{ t('crmFormDesign.dataSourceFilter') }}
          </div>
          <n-button
            :disabled="fieldConfig.disabledProps?.includes('dataSource') || !!fieldConfig.resourceFieldId"
            @click="handleDataSourceFilter"
          >
            {{ t('common.setting') }}
          </n-button>
        </div>
        <!-- 显示字段 -->
        <div v-if="fieldConfig.type === FieldTypeEnum.DATA_SOURCE" class="crm-form-design-config-item">
          <div class="crm-form-design-config-item-title">
            <span>{{ t('crmFormDesign.dataSourceDisplayField') }}</span>
            <n-button
              type="primary"
              text
              :disabled="!fieldConfig.showFields?.length || !!fieldConfig.resourceFieldId"
              @click="handleClearDataSourceDisplayField"
            >
              {{ t('common.clear') }}
            </n-button>
          </div>
          <n-button
            :disabled="fieldConfig.disabledProps?.includes('dataSource') || !!fieldConfig.resourceFieldId"
            @click="showDataSourceDisplayFieldModal = true"
          >
            {{
              fieldConfig.showFields?.length
                ? t('crmFormDesign.showFieldCount', { count: fieldConfig.showFields?.length })
                : t('common.setting')
            }}
          </n-button>
        </div>
      </template>
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
        <optionConfig
          v-model:field="fieldConfig"
          :disabled="fieldConfig.disabledProps?.includes('options') || !!fieldConfig.resourceFieldId"
        />
      </div>
      <!-- 选项属性 End -->
      <!-- 分割线属性 -->
      <!-- 显隐规则 -->
      <div v-if="isShowRuleField" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.showRule') }}
        </div>
        <n-button
          :disabled="fieldConfig.disabledProps?.includes('showControlRules') || !!fieldConfig.resourceFieldId"
          @click="showRuleConfig"
        >
          {{
            fieldConfig.showControlRules?.length
              ? t('crmFormDesign.showRuleCount', { count: fieldConfig.showControlRules?.length })
              : t('common.setting')
          }}
        </n-button>
      </div>
      <!-- 显隐规则 End -->
      <!-- 字段联动 -->
      <div v-if="isShowLinkField" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.fieldLink') }}
          <CrmPopConfirm
            v-model:show="linkClearPop"
            :title="t('crmFormDesign.linkFieldSettingClearTip')"
            icon-type="warning"
            :content="t('crmFormDesign.linkFieldSettingClearTipContent')"
            :positive-text="t('common.confirm')"
            trigger="click"
            :negative-text="t('common.cancel')"
            placement="right-end"
            @confirm="clearLink"
          >
            <n-button
              type="primary"
              text
              :disabled="!fieldConfig.linkProp?.linkOptions?.length || !!fieldConfig.resourceFieldId"
            >
              {{ t('common.clear') }}
            </n-button>
          </CrmPopConfirm>
        </div>
        <n-button
          :disabled="fieldConfig.disabledProps?.includes('linkProp') || !!fieldConfig.resourceFieldId"
          @click="showLinkConfig"
        >
          {{
            fieldConfig.linkProp?.linkOptions?.length
              ? t('crmFormDesign.linkSettingTip', { count: fieldConfig.linkProp.linkOptions.length })
              : t('common.setting')
          }}
        </n-button>
      </div>
      <!-- 字段联动 End -->
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
          :disabled="fieldConfig.disabledProps?.includes('dividerClass') || !!fieldConfig.resourceFieldId"
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
            :disabled="fieldConfig.disabledProps?.includes('dividerColor') || !!fieldConfig.resourceFieldId"
          >
            <template #trigger>
              <div class="crm-form-design-color-select-wrapper">
                <div class="crm-form-design-color-select" :style="{ backgroundColor: fieldConfig.dividerColor }"></div>
              </div>
            </template>
            <CrmColorSelect
              v-model:pure-color="fieldConfig.dividerColor"
              :disabled="fieldConfig.disabledProps?.includes('dividerColor') || !!fieldConfig.resourceFieldId"
            />
          </n-popover>
          <n-button
            class="px-[9px]"
            :disabled="fieldConfig.disabledProps?.includes('dividerColor') || !!fieldConfig.resourceFieldId"
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
            :disabled="fieldConfig.disabledProps?.includes('titleColor') || !!fieldConfig.resourceFieldId"
          >
            <template #trigger>
              <div class="crm-form-design-color-select-wrapper">
                <div class="crm-form-design-color-select" :style="{ backgroundColor: fieldConfig.titleColor }"></div>
              </div>
            </template>
            <CrmColorSelect
              v-model:pure-color="fieldConfig.titleColor"
              :disabled="fieldConfig.disabledProps?.includes('titleColor') || !!fieldConfig.resourceFieldId"
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
          :disabled="fieldConfig.disabledProps?.includes('direction') || !!fieldConfig.resourceFieldId"
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
            <CrmInputNumber
              v-if="fieldConfig.uploadLimitEnable"
              v-model:value="fieldConfig.uploadLimit"
              :step="1"
              :min="1"
              :max="10"
              class="w-[130px]"
            >
              <template #suffix>
                <div class="text-[var(--text-n4)]">{{ t('crmFormDesign.pictureNumUnit') }}</div>
              </template>
            </CrmInputNumber>
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
            <CrmInputNumber
              v-if="fieldConfig.uploadSizeLimitEnable"
              v-model:value="fieldConfig.uploadSizeLimit"
              :step="1"
              :min="0"
              :max="20"
              class="w-[130px]"
            >
              <template #suffix>
                <div class="text-[var(--text-n4)]">MB</div>
              </template>
            </CrmInputNumber>
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
              label: t('crmFormDesign.C'),
              value: 'C',
            },
            {
              label: t('crmFormDesign.P'),
              value: 'P',
            },
            {
              label: t('crmFormDesign.PC'),
              value: 'PC',
            },
            {
              label: t('crmFormDesign.PCD'),
              value: 'PCD',
            },
            {
              label: t('crmFormDesign.PCDDetail'),
              value: 'detail',
            },
          ]"
          :disabled="fieldConfig.disabledProps?.includes('locationType') || !!fieldConfig.resourceFieldId"
        />
      </div>
      <!-- 地址属性 End -->
      <!-- 流水号属性 -->
      <div v-if="fieldConfig.type === FieldTypeEnum.SERIAL_NUMBER" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.serialNumberRule') }}</div>
        <template v-if="fieldConfig.serialNumberRules">
          <n-input v-model:value="serialNumberRules1" maxlength="10">
            <template #prefix>{{ t('crmFormDesign.fixedChar') }}</template>
          </n-input>
          <n-input v-model:value="serialNumberRules2" maxlength="10">
            <template #prefix>{{ t('crmFormDesign.fixedChar') }}</template>
          </n-input>
          <n-input v-model:value="serialNumberRules3" disabled>
            <template #prefix>{{ t('crmFormDesign.submitDate') }}</template>
          </n-input>
          <n-input v-model:value="serialNumberRules4" maxlength="10">
            <template #prefix>{{ t('crmFormDesign.fixedChar') }}</template>
          </n-input>
          <n-input-number v-model:value="serialNumberRules5" :min="1" :max="9" :precision="0" :show-button="false">
            <template #prefix>{{ t('crmFormDesign.autoCount') }}</template>
          </n-input-number>
          <div
            class="flex flex-1 items-center gap-[8px] rounded-[var(--border-radius-small)] bg-[var(--text-n9)] px-[8px] py-[4px]"
          >
            <div class="text-[var(--text-n4)]">{{ t('common.preview') }}</div>
            {{ previewValue }}
          </div>
        </template>
      </div>
      <!-- 流水号属性 End -->
      <!-- 链接 -->
      <template v-else-if="fieldConfig.type === FieldTypeEnum.LINK">
        <div class="crm-form-design-config-item">
          <div class="crm-form-design-config-item-title">
            {{ t('crmFormDesign.linkSource') }}
          </div>
          <n-select
            v-model:value="fieldConfig.linkSource"
            :disabled="fieldConfig.disabledProps?.includes('linkSource') || !!fieldConfig.resourceFieldId"
            :options="linkSourceOptions"
          />
        </div>
        <div class="crm-form-design-config-item">
          <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.openMode') }}</div>
          <n-radio-group
            v-model:value="fieldConfig.openMode"
            :disabled="fieldConfig.disabledProps?.includes('openMode') || !!fieldConfig.resourceFieldId"
            name="radiogroup"
            class="flex"
          >
            <n-radio-button value="openInNew" class="flex-1 text-center">
              {{ t('crmFormDesign.openInNew') }}
            </n-radio-button>
            <n-radio-button value="openInCurrent" class="flex-1 text-center">
              {{ t('crmFormDesign.openInCurrent') }}
            </n-radio-button>
          </n-radio-group>
        </div>
      </template>
      <!-- 链接 End -->
      <!-- 公式 -->
      <template v-else-if="fieldConfig.type === FieldTypeEnum.FORMULA">
        <div class="crm-form-design-config-item">
          <div class="crm-form-design-config-item-title">
            <span> {{ t('crmFormDesign.formula') }}</span>
            <n-button
              type="primary"
              text
              :disabled="!fieldConfig.formula?.length || !!fieldConfig.resourceFieldId"
              @click="handleClearFormulaField"
            >
              {{ t('common.clear') }}
            </n-button>
          </div>
          <n-button
            type="default"
            class="outline--secondary"
            :disabled="!!fieldConfig.resourceFieldId"
            @click="handleCalculateFormula"
          >
            {{ fieldConfig.formula?.length ? t('crmFormDesign.formulaHasBeenSet') : t('common.setting') }}
          </n-button>
        </div>
      </template>
      <!-- 公式 End -->
      <!-- 电话 -->
      <template v-else-if="fieldConfig.type === FieldTypeEnum.PHONE">
        <div class="crm-form-design-config-item">
          <div class="crm-form-design-config-item-title">
            {{ t('crmFormDesign.format') }}
          </div>
          <n-select
            v-model:value="fieldConfig.format"
            :disabled="fieldConfig.disabledProps?.includes('format') || !!fieldConfig.resourceFieldId"
            :options="phoneFormatOptions"
          />
        </div>
      </template>
      <!-- 电话 End -->
      <!-- 默认值 -->
      <div
        v-if="
          (!fieldConfig.options || fieldConfig.options.length === 0) &&
          ![
            FieldTypeEnum.DIVIDER,
            FieldTypeEnum.PICTURE,
            FieldTypeEnum.LOCATION,
            FieldTypeEnum.PHONE,
            FieldTypeEnum.INPUT_MULTIPLE,
            FieldTypeEnum.DATA_SOURCE,
            FieldTypeEnum.DATA_SOURCE_MULTIPLE,
            FieldTypeEnum.SERIAL_NUMBER,
            FieldTypeEnum.LINK,
            FieldTypeEnum.ATTACHMENT,
            FieldTypeEnum.FORMULA,
            FieldTypeEnum.SUB_PRICE,
            FieldTypeEnum.SUB_PRODUCT,
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
            :rubber-band="false"
            :disabled="fieldConfig.disabledProps?.includes('hasCurrentUser') || !!fieldConfig.resourceFieldId"
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
            :rubber-band="false"
            :disabled="fieldConfig.disabledProps?.includes('hasCurrentUserDept') || !!fieldConfig.resourceFieldId"
            @update-value="
              ($event) => handleHasCurrentChange($event, fieldConfig.type === FieldTypeEnum.DEPARTMENT_MULTIPLE)
            "
          />
          {{ t('crmFormDesign.loginUserDept') }}
        </div>
        <CrmInputNumber
          v-if="fieldConfig.type === FieldTypeEnum.INPUT_NUMBER"
          v-model:value="fieldConfig.defaultValue"
          :show-button="false"
          :min="0"
          :disabled="fieldConfig.disabledProps?.includes('defaultValue') || !!fieldConfig.resourceFieldId"
        />
        <template v-else-if="fieldConfig.type === FieldTypeEnum.DATE_TIME">
          <n-select
            v-model:value="fieldConfig.dateDefaultType"
            :options="[
              {
                label: t('crmFormDesign.custom'),
                value: 'custom',
              },
              {
                label: t('crmFormDesign.currentTime'),
                value: 'current',
              },
            ]"
            :disabled="fieldConfig.disabledProps?.includes('defaultValue') || !!fieldConfig.resourceFieldId"
            @update-value="
              (val) => {
                if (val === 'custom') {
                  fieldConfig.defaultValue = null;
                }
              }
            "
          />
          <n-date-picker
            v-if="fieldConfig.dateDefaultType === 'custom'"
            v-model:value="fieldConfig.defaultValue"
            :type="fieldConfig.dateType"
            :disabled="fieldConfig.disabledProps?.includes('defaultValue') || !!fieldConfig.resourceFieldId"
            class="w-full"
          ></n-date-picker>
        </template>
        <CrmUserTagSelector
          v-else-if="[FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(fieldConfig.type)"
          v-show="fieldConfig.type === FieldTypeEnum.MEMBER_MULTIPLE || !fieldConfig.hasCurrentUser"
          v-model:selected-list="fieldConfig.initialOptions"
          v-model:value="fieldConfig.defaultValue"
          :api-type-key="MemberApiTypeEnum.FORM_FIELD"
          :multiple="fieldConfig.type === FieldTypeEnum.MEMBER_MULTIPLE"
          :drawer-title="t('crmFormDesign.selectMember')"
          :ok-text="t('common.confirm')"
          :member-types="memberTypes"
          :disabled="fieldConfig.disabledProps?.includes('defaultValue') || !!fieldConfig.resourceFieldId"
          :disabled-node-types="[DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE]"
        />
        <CrmUserTagSelector
          v-else-if="[FieldTypeEnum.DEPARTMENT, FieldTypeEnum.DEPARTMENT_MULTIPLE].includes(fieldConfig.type)"
          v-show="fieldConfig.type === FieldTypeEnum.DEPARTMENT_MULTIPLE || !fieldConfig.hasCurrentUserDept"
          v-model:selected-list="fieldConfig.initialOptions"
          v-model:value="fieldConfig.defaultValue"
          :api-type-key="MemberApiTypeEnum.FORM_FIELD"
          :multiple="fieldConfig.type === FieldTypeEnum.DEPARTMENT_MULTIPLE"
          :drawer-title="t('crmFormDesign.selectDepartment')"
          :ok-text="t('common.confirm')"
          :member-types="memberTypes"
          :disabled="fieldConfig.disabledProps?.includes('defaultValue') || !!fieldConfig.resourceFieldId"
          :disabled-node-types="[DeptNodeTypeEnum.USER, DeptNodeTypeEnum.ROLE]"
        />
        <CrmDataSource
          v-else-if="[FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(fieldConfig.type)"
          v-model:value="fieldConfig.defaultValue"
          v-model:rows="fieldConfig.initialOptions"
          :multiple="fieldConfig.type === FieldTypeEnum.DATA_SOURCE_MULTIPLE"
          :data-source-type="fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER"
          :disabled="fieldConfig.disabledProps?.includes('defaultValue') || !!fieldConfig.resourceFieldId"
        />
        <n-input
          v-else
          v-model:value="fieldConfig.defaultValue"
          :maxlength="255"
          :disabled="fieldConfig.disabledProps?.includes('defaultValue') || !!fieldConfig.resourceFieldId"
          clearable
        />
      </div>
      <!-- 默认值 End -->
      <!-- 附件 Start -->
      <div v-if="fieldConfig.type === FieldTypeEnum.ATTACHMENT" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.fileUploadConfig') }}
        </div>
        <div class="flex items-center">
          <n-checkbox
            v-model:checked="fieldConfig.onlyOne"
            :disabled="fieldConfig.disabledProps?.includes('onlyOne') || !!fieldConfig.resourceFieldId"
            @update-checked="
              ($event) => {
                if ($event === false) {
                  fieldConfig.onlyOne = false;
                }
              }
            "
          >
            {{ t('crmFormDesign.onlyOneFile') }}
          </n-checkbox>
          <n-tooltip trigger="hover">
            <template #trigger>
              <CrmIcon type="iconicon_help_circle" :size="16" class="cursor-pointer hover:text-[var(--primary-1)]" />
            </template>
            {{ t('crmFormDesign.onlyOneFileTip') }}
          </n-tooltip>
        </div>
        <n-checkbox
          v-model:checked="currentFieldHasAccept"
          :disabled="fieldConfig.disabledProps?.includes('accept') || !!fieldConfig.resourceFieldId"
          @update-checked="
            ($event) => {
              if (!$event) {
                fieldConfig.accept = '';
              }
            }
          "
        >
          {{ t('crmFormDesign.acceptFileType') }}
        </n-checkbox>
        <template v-if="currentFieldHasAccept">
          <n-input
            v-model:value="fieldConfig.accept"
            type="textarea"
            :placeholder="t('crmFormDesign.acceptFileTypePlaceholder')"
            :autosize="{
              minRows: 2,
            }"
            resizable
            clearable
          ></n-input>
          <div class="text-[12px] text-[var(--text-n4)]">{{ t('crmFormDesign.acceptFileTypeTip') }}</div>
        </template>
        <n-checkbox
          v-model:checked="currentFieldHasLimitSize"
          :disabled="fieldConfig.disabledProps?.includes('limitSize') || !!fieldConfig.resourceFieldId"
          @update-checked="
            ($event) => {
              if ($event === false) {
                fieldConfig.limitSize = '';
              }
            }
          "
        >
          {{ t('crmFormDesign.limitSize') }}
        </n-checkbox>
        <n-input-group v-if="currentFieldHasLimitSize">
          <n-input-number v-model:value="currentFieldLimitSize" :show-button="false" />
          <n-select
            v-model:value="currentFieldLimitSizeUnit"
            :options="[
              {
                label: 'KB',
                value: 'KB',
              },
              {
                label: 'MB',
                value: 'MB',
              },
            ]"
            class="w-[70px]"
          />
        </n-input-group>
      </div>
      <!-- 附件 End -->
      <!-- 校验规则 -->
      <div v-if="showRules.length > 0" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">{{ t('crmFormDesign.validator') }}</div>
        <n-checkbox-group v-model:value="checkedRules" @update-value="handleRuleChange">
          <n-space item-class="w-full">
            <n-checkbox
              v-for="rule of showRules"
              :key="rule.key"
              :value="rule.key"
              :disabled="fieldConfig.disabledProps?.includes(`rules.${rule.key}`) || !!fieldConfig.resourceFieldId"
            >
              {{ t(rule.label || '', { value: t(fieldConfig.name) }) }}
            </n-checkbox>
          </n-space>
        </n-checkbox-group>
      </div>
      <!-- 校验规则 End -->
      <!-- 字段权限 -->
      <div class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.fieldPermission') }}
        </div>
        <n-checkbox
          v-model:checked="fieldConfig.readable"
          :disabled="fieldConfig.disabledProps?.includes('readable') || !!fieldConfig.resourceFieldId"
          @update-checked="
            ($event) => {
              if ($event === false) {
                fieldConfig.editable = false;
              }
            }
          "
        >
          {{ t('crmFormDesign.readable') }}
        </n-checkbox>
        <n-checkbox
          v-if="
            ![
              FieldTypeEnum.DIVIDER,
              FieldTypeEnum.SERIAL_NUMBER,
              FieldTypeEnum.SUB_PRICE,
              FieldTypeEnum.SUB_PRODUCT,
              FieldTypeEnum.FORMULA,
            ].includes(fieldConfig.type)
          "
          v-model:checked="fieldConfig.editable"
          :disabled="fieldConfig.disabledProps?.includes('editable') || !!fieldConfig.resourceFieldId"
          @update-checked="
            ($event) => {
              if ($event === true) {
                fieldConfig.readable = true;
              }
            }
          "
        >
          {{ t('crmFormDesign.editable') }}
        </n-checkbox>
      </div>
      <!-- 字段权限 End -->
      <!-- 移动端 -->
      <div v-if="fieldConfig.type !== FieldTypeEnum.INPUT_MULTIPLE" class="crm-form-design-config-item">
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.mobile') }}
        </div>
        <n-checkbox
          v-model:checked="fieldConfig.mobile"
          :disabled="fieldConfig.disabledProps?.includes('mobile') || !!fieldConfig.resourceFieldId"
          @update-checked="
            ($event) => {
              if ($event === false) {
                fieldConfig.mobile = false;
              }
            }
          "
        >
          {{ t('crmFormDesign.readable') }}
        </n-checkbox>
      </div>
      <!-- 移动端 End -->
      <div
        v-if="![FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(fieldConfig.type) && !isSubTableField"
        class="crm-form-design-config-item"
      >
        <div class="crm-form-design-config-item-title">
          {{ t('crmFormDesign.fieldWidth') }}
        </div>
        <n-tooltip :disabled="fieldConfig.type !== FieldTypeEnum.ATTACHMENT">
          <template #trigger>
            <n-radio-group
              v-model:value="fieldConfig.fieldWidth"
              name="radiogroup"
              class="flex"
              :disabled="
                fieldConfig.disabledProps?.includes('fieldWidth') || fieldConfig.type === FieldTypeEnum.ATTACHMENT
              "
            >
              <n-radio-button :value="Number((1 / 4).toFixed(2))" class="!px-[8px]"> 1/4 </n-radio-button>
              <n-radio-button :value="Number((1 / 3).toFixed(2))" class="!px-[8px]"> 1/3 </n-radio-button>
              <n-radio-button :value="Number((1 / 2).toFixed(2))" class="!px-[8px]"> 1/2 </n-radio-button>
              <n-radio-button :value="Number((2 / 3).toFixed(2))" class="!px-[8px]"> 2/3 </n-radio-button>
              <n-radio-button :value="Number((3 / 4).toFixed(2))" class="!px-[8px]"> 3/4 </n-radio-button>
              <n-radio-button :value="1" class="!px-[8px]">
                {{ t('crmFormDesign.wholeLine') }}
              </n-radio-button>
            </n-radio-group>
          </template>
          {{ t('crmFormDesign.onlyFull') }}
        </n-tooltip>
      </div>
      <!-- 子表格 -->
      <template v-if="[FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(fieldConfig.type)">
        <div class="crm-form-design-config-item">
          <div class="crm-form-design-config-item-title">
            {{ t('crmFormDesign.subTableField') }}
          </div>
          <subTableFields v-model:field="fieldConfig" />
        </div>
        <div class="crm-form-design-config-item">
          <div class="crm-form-design-config-item-title">
            {{ t('crmFormDesign.sum') }}
          </div>
          <n-checkbox v-model:checked="showSumColumn">
            {{ t('crmFormDesign.show') }}
          </n-checkbox>
          <n-select v-if="showSumColumn" v-model:value="fieldConfig.sumColumns" multiple :options="sumOptions" />
        </div>
        <div class="crm-form-design-config-item">
          <div class="crm-form-design-config-item-title">
            {{ t('crmFormDesign.fixedCol') }}
          </div>
          <n-input-group>
            <n-input-group-label>{{ t('crmFormDesign.fixedColNum') }}</n-input-group-label>
            <n-select
              v-model:value="fieldConfig.fixedColumn"
              :options="[
                {
                  label: `1${t('crmFormDesign.fixedCols')}`,
                  value: 1,
                },
                {
                  label: `2${t('crmFormDesign.fixedCols')}`,
                  value: 2,
                },
                {
                  label: `3${t('crmFormDesign.fixedCols')}`,
                  value: 3,
                },
              ]"
            />
          </n-input-group>
        </div>
      </template>
      <!-- 子表格 End -->
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
        <div class="one-line-text">{{ t('crmFormDesign.choice') }}</div>
        <n-select
          v-model:value="rule.value"
          :options="getShowRuleOptions(rule)"
          :disabled="props.disabled"
          class="w-[150px]"
        />
        <div class="one-line-text">{{ t('crmFormDesign.show') }}</div>
        <n-select
          v-model:value="rule.fieldIds"
          :options="showRuleFields"
          :disabled="props.disabled || !rule.value"
          class="w-[320px]"
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
  <FilterModal
    v-if="fieldConfig"
    v-model:visible="showDataSourceFilterModal"
    :field-config="fieldConfig"
    :form-fields="list"
    @save="handleDataSourceFilterSave"
  />
  <DataSourceDisplayFieldModal
    v-if="fieldConfig"
    v-model:show="showDataSourceDisplayFieldModal"
    :field-config="fieldConfig"
    @save="handleDataSourceDisplayFieldSave"
  />
  <fieldLinkDrawer
    v-if="fieldConfig"
    v-model:visible="showLinkConfigVisible"
    :field-config="fieldConfig"
    :form-fields="list"
    @save="handleLinkConfigSave"
  />
  <formulaModal
    v-if="fieldConfig"
    v-model:visible="showCalculateFormulaModal"
    :field-config="fieldConfig"
    :form-fields="formulaScopedFields"
    :is-sub-table-field="isSubTableField"
    @save="handleCalculateFormulaConfigSave"
  />
</template>

<script setup lang="ts">
  import {
    NButton,
    NCheckbox,
    NCheckboxGroup,
    NDatePicker,
    NInput,
    NInputGroup,
    NInputGroupLabel,
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

  import {
    FieldDataSourceTypeEnum,
    FieldRuleEnum,
    FieldTypeEnum,
    FormDesignKeyEnum,
  } from '@lib/shared/enums/formDesignEnum';
  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmColorSelect from '@/components/pure/crm-color-select/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';
  import CrmDataSource from '@/components/business/crm-data-source-select/index.vue';
  import Divider from '@/components/business/crm-form-create/components/basic/divider.vue';
  import { rules, showRulesMap } from '@/components/business/crm-form-create/config';
  import {
    DataSourceFilterCombine,
    FieldLinkProp,
    FormCreateField,
    FormCreateFieldRule,
    FormCreateFieldShowControlRule,
  } from '@/components/business/crm-form-create/types';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';
  import DataSourceDisplayFieldModal from './dataSourceDisplayFieldModal.vue';
  import fieldLinkDrawer from './fieldLinkDrawer.vue';
  import FilterModal from './filterModal.vue';
  import formulaModal from './formulaModal.vue';
  import optionConfig from './optionConfig.vue';
  import subTableFields from './subTableFields.vue';

  // import useUserStore from '@/store/modules/user';
  import { SelectOption } from 'naive-ui/es/select/src/interface';

  const props = defineProps<{
    formKey: FormDesignKeyEnum;
    disabled?: boolean;
  }>();

  const { t } = useI18n();
  // const userStore = useUserStore();

  const fieldConfig = defineModel<FormCreateField>('field', {
    default: null,
  });

  const list = defineModel<FormCreateField[]>('fieldList', {
    required: true,
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
      checkedRules.value = arr?.map((e) => e.key);
    }
  );

  const isSubTableField = computed(() => {
    return list.value
      .filter((item) => item.type === FieldTypeEnum.SUB_PRICE || item.type === FieldTypeEnum.SUB_PRODUCT)
      .some((tableField) => {
        return tableField.subFields?.some((subField) => subField.id === fieldConfig.value?.id);
      });
  });
  const parentField = computed(() => {
    if (isSubTableField.value) {
      return list.value
        .filter((item) => item.type === FieldTypeEnum.SUB_PRICE || item.type === FieldTypeEnum.SUB_PRODUCT)
        .find((tableField) => {
          return tableField.subFields?.some((subField) => subField.id === fieldConfig.value?.id);
        });
    }
    return null;
  });
  const isNameRepeat = computed(() => {
    const fieldNameSet = new Set<string>();
    list.value.forEach((field) => {
      if (field.id !== fieldConfig.value?.id) {
        fieldNameSet.add(field.name);
      }
      if ([FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(field.type)) {
        field.subFields?.forEach((subField) => {
          if (subField.id !== fieldConfig.value?.id) {
            fieldNameSet.add(subField.name);
          }
        });
      }
    });
    return fieldConfig.value ? fieldNameSet.has(fieldConfig.value.name) : false;
  });

  const formulaScopedFields = computed(() => (isSubTableField.value ? parentField.value?.subFields ?? [] : list.value));

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

  const memberTypes = computed(() => {
    if ([FieldTypeEnum.MEMBER, FieldTypeEnum.MEMBER_MULTIPLE].includes(fieldConfig.value.type)) {
      return [
        {
          label: t('menu.settings.org'),
          value: MemberSelectTypeEnum.ORG,
        },
      ];
    }
    return [
      {
        label: t('menu.settings.org'),
        value: MemberSelectTypeEnum.ONLY_ORG,
      },
    ];
  });

  function handleHasCurrentChange(val: boolean, multiple: boolean) {
    if (val && !multiple) {
      fieldConfig.value.defaultValue = [];
      fieldConfig.value.initialOptions = [];
    }
  }

  const numberPreview = computed(() => {
    const tempVal = 9999;
    if (fieldConfig.value.numberFormat === 'percent') {
      if (fieldConfig.value.decimalPlaces && fieldConfig.value.precision) {
        return `99.${'1'.repeat(fieldConfig.value.precision)}%`;
      }
      return '99%';
    }
    if (fieldConfig.value.showThousandsSeparator && fieldConfig.value.decimalPlaces && fieldConfig.value.precision) {
      return `${tempVal.toLocaleString('en-US')}.${'1'.repeat(fieldConfig.value.precision)}`;
    }
    if (fieldConfig.value.showThousandsSeparator) {
      return tempVal.toLocaleString('en-US');
    }
    if (fieldConfig.value.decimalPlaces && fieldConfig.value.precision) {
      return `9999.${'1'.repeat(fieldConfig.value.precision)}`;
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

  const linkSourceOptions = [
    {
      label: t('crmFormDesign.userInput'),
      value: 'userInput',
    },
  ];

  const phoneFormatOptions = [
    {
      label: t('crmFormDesign.none'),
      value: '255',
    },
    {
      label: t('common.phoneNumber'),
      value: '11',
    },
  ];

  const dividerStyleShow = ref(false);
  function handleDividerStyleClick(value: string) {
    fieldConfig.value.dividerClass = value;
    dividerStyleShow.value = false;
  }

  const sumOptions = computed<SelectOption[]>(() => {
    return fieldConfig.value.subFields
      ?.filter((e) => [FieldTypeEnum.INPUT_NUMBER, FieldTypeEnum.FORMULA].includes(e.type))
      .map((e) => {
        const fieldId = e.resourceFieldId ? e.id : e.businessKey || e.id;
        return {
          label: e.name,
          value: fieldId,
        };
      }) as SelectOption[];
  });

  const dataSourceOptions = computed<SelectOption[]>(() => {
    const fullList = [
      {
        label: t('crmFormDesign.customer'),
        value: FieldDataSourceTypeEnum.CUSTOMER,
        formKey: FormDesignKeyEnum.CUSTOMER,
      },
      {
        label: t('crmFormDesign.contract'),
        value: FieldDataSourceTypeEnum.CONTACT,
        formKey: FormDesignKeyEnum.CONTACT,
      },
      {
        label: t('crmFormDesign.opportunity'),
        value: FieldDataSourceTypeEnum.BUSINESS,
        formKey: FormDesignKeyEnum.BUSINESS,
      },
      {
        label: t('crmFormDesign.product'),
        value: FieldDataSourceTypeEnum.PRODUCT,
        formKey: FormDesignKeyEnum.PRODUCT,
      },
      {
        label: t('crmFormDesign.clue'),
        value: FieldDataSourceTypeEnum.CLUE,
        formKey: FormDesignKeyEnum.CLUE,
      },
      {
        label: t('crmFormCreate.drawer.price'),
        value: FieldDataSourceTypeEnum.PRICE,
        formKey: FormDesignKeyEnum.PRICE,
      },
      {
        label: t('crmFormCreate.drawer.quotation'),
        value: FieldDataSourceTypeEnum.QUOTATION,
        formKey: FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
      },
      {
        label: t('module.contract'),
        value: FieldDataSourceTypeEnum.CONTRACT,
        formKey: FormDesignKeyEnum.CONTRACT,
      },
    ];
    if (isSubTableField.value) {
      return fullList.filter(
        (item) =>
          [FieldDataSourceTypeEnum.PRODUCT, FieldDataSourceTypeEnum.PRICE].includes(item.value) &&
          item.formKey !== props.formKey
      );
    }
    return fullList.filter((item) => item.formKey !== props.formKey);
  });

  watch(
    () => dataSourceOptions.value,
    (options) => {
      if (fieldConfig.value && !options.some((item) => item.value === fieldConfig.value.dataSourceType)) {
        fieldConfig.value.dataSourceType =
          (options[0]?.value as FieldDataSourceTypeEnum) || FieldDataSourceTypeEnum.CUSTOMER;
      }
    },
    { immediate: true }
  );

  const showRuleConfigVisible = ref(false);
  const tempShowRules = ref<FormCreateFieldShowControlRule[]>([]);
  const isShowRuleField = computed(() => {
    return (
      !isSubTableField.value &&
      (fieldConfig.value.type === FieldTypeEnum.RADIO || fieldConfig.value.type === FieldTypeEnum.SELECT)
    );
  });
  // 显隐规则可选字段
  const showRuleFields = computed(() => {
    if (isShowRuleField.value) {
      return list.value.filter((item) => item.id !== fieldConfig.value.id).map((e) => ({ label: e.name, value: e.id }));
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

  const isShowLinkField = computed(() => {
    return [FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(fieldConfig.value.type);
  });
  const showLinkConfigVisible = ref(false);
  const tempLinks = ref<FieldLinkProp>({
    targetField: '',
    linkOptions: [],
  });
  const linkClearPop = ref(false);

  function clearLink() {
    fieldConfig.value.linkProp = {
      targetField: '',
      linkOptions: [],
    };
    linkClearPop.value = false;
  }
  function showLinkConfig() {
    tempLinks.value = fieldConfig.value.linkProp || {
      targetField: '',
      linkOptions: [],
    };
    showLinkConfigVisible.value = true;
  }

  function handleLinkConfigSave(value: FieldLinkProp) {
    fieldConfig.value.linkProp = value;
  }

  const showDataSourceFilterModal = ref(false);

  function handleDataSourceFilter() {
    showDataSourceFilterModal.value = true;
  }

  const showDataSourceDisplayFieldModal = ref(false);
  function handleDataSourceDisplayFieldSave(value: string[], selectedList: any[]) {
    fieldConfig.value.showFields = value;
    if (isSubTableField.value) {
      const index = parentField.value?.subFields?.findIndex((item) => item.id === fieldConfig.value.id);
      if (index !== undefined && index >= 0 && parentField.value) {
        parentField.value.subFields = parentField.value?.subFields?.filter(
          (item) => item.resourceFieldId !== fieldConfig.value.id
        );
        parentField.value?.subFields?.splice(
          index + 1,
          0,
          ...selectedList.map((item) => ({
            ...item,
            resourceFieldId: fieldConfig.value.id,
            description: '',
            editable: false,
          }))
        );
      }
    } else {
      list.value = list.value.filter((item) => item.resourceFieldId !== fieldConfig.value.id);
      const fieldIndex = list.value.findIndex((item) => item.id === fieldConfig.value.id);
      if (fieldIndex >= 0) {
        nextTick(() => {
          list.value.splice(
            fieldIndex + 1,
            0,
            ...selectedList.map((item) => ({
              ...item,
              resourceFieldId: fieldConfig.value.id,
              description: '',
              editable: false,
            }))
          );
        });
      }
    }
  }
  function handleClearDataSourceDisplayField() {
    fieldConfig.value.showFields = [];
    if (isSubTableField.value && parentField.value) {
      parentField.value.subFields = parentField.value?.subFields?.filter(
        (item) => item.resourceFieldId !== fieldConfig.value.id
      );
    } else {
      list.value = list.value.filter((item) => item.resourceFieldId !== fieldConfig.value.id);
    }
  }

  function handleDataSourceFilterSave(result: DataSourceFilterCombine) {
    fieldConfig.value.combineSearch = result;
    showDataSourceFilterModal.value = false;
  }

  const showCalculateFormulaModal = ref(false);
  function handleCalculateFormula() {
    showCalculateFormulaModal.value = true;
  }

  function handleCalculateFormulaConfigSave(astValue: string) {
    fieldConfig.value.formula = astValue;
    showCalculateFormulaModal.value = false;
  }

  function handleClearFormulaField() {
    fieldConfig.value.formula = '';
  }

  const serialNumberRules1 = ref(fieldConfig.value?.serialNumberRules?.[0].toString() || '');
  const serialNumberRules2 = ref(fieldConfig.value?.serialNumberRules?.[1].toString() || '');
  const serialNumberRules3 = ref(fieldConfig.value?.serialNumberRules?.[2].toString() || 'yyyyMM');
  const serialNumberRules4 = ref(fieldConfig.value?.serialNumberRules?.[3].toString() || '');
  const serialNumberRules5 = ref(Number(fieldConfig.value?.serialNumberRules?.[4] || 6));
  const previewValue = computed(() => {
    const part1 = serialNumberRules1.value || '';
    const part2 = serialNumberRules2.value || '';
    const part4 = serialNumberRules4.value || '';
    const digits = Number(serialNumberRules5.value) ?? 0;
    const part5 = digits > 0 ? String(1).padStart(digits, '0') : '';

    return `${part1}${part2}202501${part4}${part5}`;
  });

  watch(
    () => [serialNumberRules1.value, serialNumberRules2.value, serialNumberRules4.value, serialNumberRules5.value],
    () => {
      fieldConfig.value.serialNumberRules = [
        serialNumberRules1.value,
        serialNumberRules2.value,
        serialNumberRules3.value,
        serialNumberRules4.value,
        String(serialNumberRules5.value),
      ];
    }
  );

  const currentFieldHasAccept = ref(!!fieldConfig.value?.accept);
  const currentFieldHasLimitSize = ref(!!fieldConfig.value?.limitSize);
  const currentFieldLimitSize = ref(parseInt(fieldConfig.value?.limitSize || '', 10) || null);
  const currentFieldLimitSizeUnit = ref(fieldConfig.value?.limitSize?.slice(-2) || 'KB');

  watch(
    () => [currentFieldLimitSize.value, currentFieldLimitSizeUnit.value],
    () => {
      if (currentFieldHasLimitSize.value && currentFieldLimitSize.value) {
        fieldConfig.value.limitSize = `${currentFieldLimitSize.value}${currentFieldLimitSizeUnit.value}`;
      } else {
        fieldConfig.value.limitSize = '';
      }
    }
  );

  const showSumColumn = ref(false);
  watch(
    () => fieldConfig.value?.id,
    () => {
      serialNumberRules1.value = fieldConfig.value?.serialNumberRules?.[0].toString() || '';
      serialNumberRules2.value = fieldConfig.value?.serialNumberRules?.[1].toString() || '';
      serialNumberRules4.value = fieldConfig.value?.serialNumberRules?.[3].toString() || '';
      serialNumberRules5.value = Number(fieldConfig.value?.serialNumberRules?.[4]);
      currentFieldHasAccept.value = !!fieldConfig.value?.accept;
      currentFieldHasLimitSize.value = !!fieldConfig.value?.limitSize;
      currentFieldLimitSize.value = parseInt(fieldConfig.value?.limitSize || '', 10) || null;
      currentFieldLimitSizeUnit.value = fieldConfig.value?.limitSize?.slice(-2) || 'KB';
      showSumColumn.value = !!(fieldConfig.value?.sumColumns && fieldConfig.value.sumColumns.length > 0);
    }
  );

  watch(
    () => showSumColumn.value,
    (val) => {
      if (!val) {
        fieldConfig.value.sumColumns = [];
      }
    }
  );
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
