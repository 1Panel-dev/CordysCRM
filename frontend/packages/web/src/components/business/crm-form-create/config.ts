import {
  FieldDataSourceTypeEnum,
  FieldRuleEnum,
  FieldTypeEnum,
  FormDesignKeyEnum,
} from '@lib/shared/enums/formDesignEnum';
import type { CommonList } from '@lib/shared/models/common';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

import {
  addCustomer,
  addCustomerContact,
  addCustomerFollowPlan,
  addCustomerFollowRecord,
  getCustomer,
  getCustomerContact,
  getCustomerContactFormConfig,
  getCustomerContactList,
  getCustomerFollowPlanFormConfig,
  getCustomerFollowPlanList,
  getCustomerFollowRecord,
  getCustomerFollowRecordFormConfig,
  getCustomerFollowRecordList,
  getCustomerFormConfig,
  getCustomerList,
  updateCustomer,
  updateCustomerContact,
  updateCustomerFollowPlan,
  updateCustomerFollowRecord,
} from '@/api/modules/customer';

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
  defaultValue: [],
  description: '',
  readable: true,
  editable: true,
  rules: [],
  datetype: 'datetime',
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
  },
  {
    key: FieldRuleEnum.UNIQUE,
    message: 'common.valueExists',
    label: 'crmFormDesign.onlyOne',
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
  [FieldTypeEnum.MULTIPLE_INPUT]: [FieldRuleEnum.REQUIRED],
  [FieldTypeEnum.PICTURE]: [FieldRuleEnum.REQUIRED],
  [FieldTypeEnum.LOCATION]: [FieldRuleEnum.REQUIRED],
  [FieldTypeEnum.PHONE]: [FieldRuleEnum.REQUIRED, FieldRuleEnum.UNIQUE],
  [FieldTypeEnum.DATA_SOURCE]: [FieldRuleEnum.REQUIRED, FieldRuleEnum.UNIQUE],
};

export const getFormConfigApiMap: Record<FormDesignKeyEnum, () => Promise<FormDesignConfigDetailParams>> = {
  [FormDesignKeyEnum.CUSTOMER]: getCustomerFormConfig,
  [FormDesignKeyEnum.BUSINESS]: getCustomerFormConfig, // TODO:
  [FormDesignKeyEnum.CONTACT]: getCustomerContactFormConfig,
  [FormDesignKeyEnum.FOLLOW_PLAN]: getCustomerFollowPlanFormConfig,
  [FormDesignKeyEnum.FOLLOW_RECORD]: getCustomerFollowRecordFormConfig,
  [FormDesignKeyEnum.LEAD]: getCustomerFormConfig, // TODO:
  [FormDesignKeyEnum.PRODUCT]: getCustomerFormConfig, // TODO:
};

export const createFormApi: Record<FormDesignKeyEnum, (data: any) => Promise<any>> = {
  [FormDesignKeyEnum.CUSTOMER]: addCustomer,
  [FormDesignKeyEnum.BUSINESS]: addCustomer, // TODO:
  [FormDesignKeyEnum.CONTACT]: addCustomerContact,
  [FormDesignKeyEnum.FOLLOW_PLAN]: addCustomerFollowPlan,
  [FormDesignKeyEnum.FOLLOW_RECORD]: addCustomerFollowRecord,
  [FormDesignKeyEnum.LEAD]: addCustomer, // TODO:
  [FormDesignKeyEnum.PRODUCT]: addCustomer, // TODO:
};

export const updateFormApi: Record<FormDesignKeyEnum, (data: any) => Promise<any>> = {
  [FormDesignKeyEnum.CUSTOMER]: updateCustomer,
  [FormDesignKeyEnum.BUSINESS]: updateCustomer, // TODO:
  [FormDesignKeyEnum.CONTACT]: updateCustomerContact,
  [FormDesignKeyEnum.FOLLOW_PLAN]: updateCustomerFollowPlan,
  [FormDesignKeyEnum.FOLLOW_RECORD]: updateCustomerFollowRecord,
  [FormDesignKeyEnum.LEAD]: updateCustomer, // TODO:
  [FormDesignKeyEnum.PRODUCT]: updateCustomer, // TODO:
};

export const getFormDetailApiMap: Partial<Record<FormDesignKeyEnum, (id: string) => Promise<FormDetail>>> = {
  [FormDesignKeyEnum.CUSTOMER]: getCustomer,
  [FormDesignKeyEnum.BUSINESS]: getCustomer, // TODO:
  [FormDesignKeyEnum.CONTACT]: getCustomerContact,
  [FormDesignKeyEnum.FOLLOW_RECORD]: getCustomerFollowRecord,
  [FormDesignKeyEnum.LEAD]: getCustomer, // TODO:
  [FormDesignKeyEnum.PRODUCT]: getCustomer, // TODO:
};

export const getFormListApiMap: Partial<Record<FormDesignKeyEnum, (data: any) => Promise<CommonList<any>>>> = {
  [FormDesignKeyEnum.CUSTOMER]: getCustomerList,
  [FormDesignKeyEnum.BUSINESS]: getCustomerList, // TODO:
  [FormDesignKeyEnum.CONTACT]: getCustomerContactList,
  [FormDesignKeyEnum.FOLLOW_PLAN]: getCustomerFollowPlanList,
  [FormDesignKeyEnum.FOLLOW_RECORD]: getCustomerFollowRecordList,
  [FormDesignKeyEnum.LEAD]: getCustomerList, // TODO:
  [FormDesignKeyEnum.PRODUCT]: getCustomerList, // TODO:
};
