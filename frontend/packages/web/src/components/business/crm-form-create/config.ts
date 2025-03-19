import {
  FieldDataSourceTypeEnum,
  FieldRuleEnum,
  FieldTypeEnum,
  FormDesignKeyEnum,
} from '@lib/shared/enums/formDesignEnum';
import type { CommonList } from '@lib/shared/models/common';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

import {
  addClue,
  addClueFollowPlan,
  addClueFollowRecord,
  getClue,
  getClueFollowPlan,
  getClueFollowRecord,
  getClueFormConfig,
  getClueList,
  updateClue,
  updateClueFollowPlan,
  updateClueFollowRecord,
} from '@/api/modules/clue';
import {
  addCustomer,
  addCustomerContact,
  addCustomerFollowPlan,
  addCustomerFollowRecord,
  getCustomer,
  getCustomerContact,
  getCustomerContactFormConfig,
  getCustomerContactList,
  getCustomerFollowPlan,
  getCustomerFollowPlanFormConfig,
  getCustomerFollowRecord,
  getCustomerFollowRecordFormConfig,
  getCustomerFormConfig,
  getCustomerList,
  getOpenSeaCustomerList,
  updateCustomer,
  updateCustomerContact,
  updateCustomerFollowPlan,
  updateCustomerFollowRecord,
} from '@/api/modules/customer';
import {
  addOpportunity,
  addOptFollowPlan,
  addOptFollowRecord,
  getOpportunityList,
  getOptFollowPlan,
  getOptFollowRecord,
  getOptFormConfig,
  updateOpportunity,
  updateOptFollowPlan,
  updateOptFollowRecord,
} from '@/api/modules/opportunity';
import { addProduct, getProduct, getProductFormConfig, getProductList, updateProduct } from '@/api/modules/product';

import type { FormCreateField, FormCreateFieldRule, FormDetail } from './types';

export const inputDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.INPUT,
  name: 'crmFormDesign.input',
  icon: 'iconicon_single_line_text',
  fieldWidth: 1,
  showLabel: true,
  defaultValue: '',
  description: '',
  readable: true,
  editable: true,
  rules: [],
};

export const textareaDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.TEXTAREA,
  icon: 'iconicon_multiline',
  name: 'crmFormDesign.textarea',
  fieldWidth: 1,
  showLabel: true,
  defaultValue: '',
  description: '',
  readable: true,
  editable: true,
  rules: [],
};

export const inputNumberDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.INPUT_NUMBER,
  icon: 'iconicon_hashtag_key',
  name: 'crmFormDesign.inputNumber',
  fieldWidth: 1,
  showLabel: true,
  defaultValue: null,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  numberFormat: 'number',
  decimalPlaces: false,
  precision: 0,
  showThousandsSeparator: false,
};

export const dateTimeDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.DATE_TIME,
  icon: 'iconicon_calendar1',
  name: 'crmFormDesign.dateTime',
  fieldWidth: 1,
  showLabel: true,
  defaultValue: null,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  dateType: 'datetime',
};

export const radioDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.RADIO,
  icon: 'iconicon_radio',
  name: 'crmFormDesign.radio',
  fieldWidth: 1,
  showLabel: true,
  defaultValue: '',
  description: '',
  options: [],
  readable: true,
  editable: true,
  rules: [],
  direction: 'vertical',
};

export const checkboxDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.CHECKBOX,
  icon: 'icona-icon_collectbeifen12',
  name: 'crmFormDesign.checkbox',
  fieldWidth: 1,
  showLabel: true,
  defaultValue: [],
  description: '',
  options: [],
  readable: true,
  editable: true,
  rules: [],
  direction: 'vertical',
};

export const selectDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.SELECT,
  icon: 'iconicon_pull_down_single_choice',
  name: 'crmFormDesign.select',
  fieldWidth: 1,
  showLabel: true,
  options: [],
  defaultValue: [],
  description: '',
  readable: true,
  editable: true,
  rules: [],
  multiple: false,
};

export const memberDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.MEMBER,
  icon: 'iconicon_member_single_choice',
  name: 'crmFormDesign.memberSelect',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  defaultValue: [],
  initialOptions: [],
  multiple: false,
};

export const departmentDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.DEPARTMENT,
  icon: 'iconicon_department_single_choice',
  name: 'crmFormDesign.departmentSelect',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  defaultValue: [],
  initialOptions: [],
  multiple: false,
};

export const dividerDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.DIVIDER,
  icon: 'iconicon_dividing_line',
  name: 'crmFormDesign.divider',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  dividerClass: 'divider--normal',
  dividerColor: '#edf0f1',
  titleColor: '#323535',
};

export const tagInputDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.MULTIPLE_INPUT,
  icon: 'iconicon_books',
  name: 'common.tag',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  defaultValue: [],
};

export const basicFields: FormCreateField[] = [
  inputDefaultFieldConfig,
  textareaDefaultFieldConfig,
  inputNumberDefaultFieldConfig,
  dateTimeDefaultFieldConfig,
  radioDefaultFieldConfig,
  checkboxDefaultFieldConfig,
  selectDefaultFieldConfig,
  memberDefaultFieldConfig,
  departmentDefaultFieldConfig,
  dividerDefaultFieldConfig,
  tagInputDefaultFieldConfig,
];

export const pictureDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.PICTURE,
  icon: 'iconicon_picture',
  name: 'crmFormDesign.picture',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  defaultValue: [],
  readable: true,
  editable: true,
  rules: [],
  uploadLimit: 10,
  uploadSizeLimit: 20,
  pictureShowType: 'card',
  uploadLimitEnable: false,
  uploadSizeLimitEnable: false,
};

export const locationDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.LOCATION,
  icon: 'iconicon_map',
  name: 'crmFormDesign.location',
  fieldWidth: 1,
  showLabel: true,
  defaultValue: '',
  description: '',
  readable: true,
  editable: true,
  rules: [],
  locationType: 'PCD',
};

export const phoneDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.PHONE,
  icon: 'iconicon_phone',
  name: 'crmFormDesign.phone',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  defaultValue: null,
  readable: true,
  editable: true,
  rules: [],
};

export const dataSourceDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.DATA_SOURCE,
  icon: 'iconicon_select_data',
  name: 'crmFormDesign.dataSource',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  multiple: false,
  defaultValue: [],
  initialOptions: [],
  dataSourceType: FieldDataSourceTypeEnum.CUSTOMER,
};

export const advancedFields: FormCreateField[] = [
  pictureDefaultFieldConfig,
  locationDefaultFieldConfig,
  phoneDefaultFieldConfig,
  dataSourceDefaultFieldConfig,
];

export const rules: FormCreateFieldRule[] = [
  {
    key: FieldRuleEnum.REQUIRED,
    required: true,
    message: 'common.notNull',
    label: 'crmFormDesign.required',
    trigger: 'input',
  },
  {
    key: FieldRuleEnum.UNIQUE,
    message: 'common.valueExists',
    label: 'crmFormDesign.onlyOne',
    validator: () => true, // 由后台验证
  },
];

export const showRulesMap: Record<FieldTypeEnum, FieldRuleEnum[]> = {
  [FieldTypeEnum.INPUT]: [FieldRuleEnum.REQUIRED, FieldRuleEnum.UNIQUE],
  [FieldTypeEnum.TEXTAREA]: [FieldRuleEnum.REQUIRED],
  [FieldTypeEnum.INPUT_NUMBER]: [FieldRuleEnum.REQUIRED],
  [FieldTypeEnum.DATE_TIME]: [FieldRuleEnum.REQUIRED],
  [FieldTypeEnum.RADIO]: [FieldRuleEnum.REQUIRED, FieldRuleEnum.UNIQUE],
  [FieldTypeEnum.CHECKBOX]: [FieldRuleEnum.REQUIRED],
  [FieldTypeEnum.SELECT]: [FieldRuleEnum.REQUIRED, FieldRuleEnum.UNIQUE],
  [FieldTypeEnum.MEMBER]: [FieldRuleEnum.REQUIRED, FieldRuleEnum.UNIQUE],
  [FieldTypeEnum.DEPARTMENT]: [FieldRuleEnum.REQUIRED, FieldRuleEnum.UNIQUE],
  [FieldTypeEnum.DIVIDER]: [],
  [FieldTypeEnum.USER_TAG_SELECTOR]: [],
  [FieldTypeEnum.USER_SELECT]: [],
  [FieldTypeEnum.TIME_RANGE_PICKER]: [],
  [FieldTypeEnum.TREE_SELECT]: [],
  [FieldTypeEnum.MULTIPLE_INPUT]: [FieldRuleEnum.REQUIRED],
  [FieldTypeEnum.PICTURE]: [FieldRuleEnum.REQUIRED],
  [FieldTypeEnum.LOCATION]: [FieldRuleEnum.REQUIRED],
  [FieldTypeEnum.PHONE]: [FieldRuleEnum.REQUIRED, FieldRuleEnum.UNIQUE],
  [FieldTypeEnum.DATA_SOURCE]: [FieldRuleEnum.REQUIRED, FieldRuleEnum.UNIQUE],
};

export const getFormConfigApiMap: Record<FormDesignKeyEnum, () => Promise<FormDesignConfigDetailParams>> = {
  [FormDesignKeyEnum.CUSTOMER]: getCustomerFormConfig,
  [FormDesignKeyEnum.BUSINESS]: getOptFormConfig,
  [FormDesignKeyEnum.CONTACT]: getCustomerContactFormConfig,
  [FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER]: getCustomerFollowPlanFormConfig,
  [FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER]: getCustomerFollowRecordFormConfig,
  [FormDesignKeyEnum.FOLLOW_PLAN_CLUE]: getCustomerFollowPlanFormConfig,
  [FormDesignKeyEnum.FOLLOW_RECORD_CLUE]: getCustomerFollowRecordFormConfig,
  [FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS]: getCustomerFollowPlanFormConfig,
  [FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS]: getCustomerFollowRecordFormConfig,
  [FormDesignKeyEnum.CLUE]: getClueFormConfig,
  [FormDesignKeyEnum.PRODUCT]: getProductFormConfig,
  [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: getCustomerFormConfig,
};

export const createFormApi: Record<FormDesignKeyEnum, (data: any) => Promise<any>> = {
  [FormDesignKeyEnum.CUSTOMER]: addCustomer,
  [FormDesignKeyEnum.BUSINESS]: addOpportunity,
  [FormDesignKeyEnum.CONTACT]: addCustomerContact,
  [FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER]: addCustomerFollowPlan,
  [FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER]: addCustomerFollowRecord,
  [FormDesignKeyEnum.FOLLOW_PLAN_CLUE]: addClueFollowPlan,
  [FormDesignKeyEnum.FOLLOW_RECORD_CLUE]: addClueFollowRecord,
  [FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS]: addOptFollowPlan,
  [FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS]: addOptFollowRecord,
  [FormDesignKeyEnum.CLUE]: addClue,
  [FormDesignKeyEnum.PRODUCT]: addProduct,
  [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: async () => ({}), // 公海无添加
};

export const updateFormApi: Record<FormDesignKeyEnum, (data: any) => Promise<any>> = {
  [FormDesignKeyEnum.CUSTOMER]: updateCustomer,
  [FormDesignKeyEnum.BUSINESS]: updateOpportunity,
  [FormDesignKeyEnum.CONTACT]: updateCustomerContact,
  [FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER]: updateCustomerFollowPlan,
  [FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER]: updateCustomerFollowRecord,
  [FormDesignKeyEnum.FOLLOW_PLAN_CLUE]: updateClueFollowPlan,
  [FormDesignKeyEnum.FOLLOW_RECORD_CLUE]: updateClueFollowRecord,
  [FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS]: updateOptFollowPlan,
  [FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS]: updateOptFollowRecord,
  [FormDesignKeyEnum.CLUE]: updateClue,
  [FormDesignKeyEnum.PRODUCT]: updateProduct,
  [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: async () => ({}), // 公海无更新
};

export const getFormDetailApiMap: Partial<Record<FormDesignKeyEnum, (id: string) => Promise<FormDetail>>> = {
  [FormDesignKeyEnum.CUSTOMER]: getCustomer,
  [FormDesignKeyEnum.BUSINESS]: getCustomer, // TODO:
  [FormDesignKeyEnum.CONTACT]: getCustomerContact,
  [FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER]: getCustomerFollowPlan,
  [FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER]: getCustomerFollowRecord,
  [FormDesignKeyEnum.FOLLOW_PLAN_CLUE]: getClueFollowPlan,
  [FormDesignKeyEnum.FOLLOW_RECORD_CLUE]: getClueFollowRecord,
  [FormDesignKeyEnum.FOLLOW_PLAN_BUSINESS]: getOptFollowPlan,
  [FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS]: getOptFollowRecord,
  [FormDesignKeyEnum.CLUE]: getClue,
  [FormDesignKeyEnum.PRODUCT]: getProduct,
};

export const getFormListApiMap: Partial<Record<FormDesignKeyEnum, (data: any) => Promise<CommonList<any>>>> = {
  [FormDesignKeyEnum.CUSTOMER]: getCustomerList,
  [FormDesignKeyEnum.BUSINESS]: getOpportunityList,
  [FormDesignKeyEnum.CONTACT]: getCustomerContactList,
  [FormDesignKeyEnum.CLUE]: getClueList,
  [FormDesignKeyEnum.PRODUCT]: getProductList,
  [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: getOpenSeaCustomerList,
};
