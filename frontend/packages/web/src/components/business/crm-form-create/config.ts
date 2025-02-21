import { FieldRuleEnum, FieldTypeEnum } from './enum';
import type { FormCreateField, FormCreateFieldRule } from './types';

export const inputDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.INPUT,
  name: 'crmFormDesign.input',
  icon: 'iconicon_single_line_text',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  showRules: [FieldRuleEnum.REQUIRED, FieldRuleEnum.UNIQUE],
};

export const textareaDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.TEXTAREA,
  icon: 'iconicon_multiline',
  name: 'crmFormDesign.textarea',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  showRules: [FieldRuleEnum.REQUIRED],
};

export const inputNumberDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.INPUT_NUMBER,
  icon: 'iconicon_hashtag_key',
  name: 'crmFormDesign.inputNumber',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  numberFormat: 'number',
  decimalPlaces: false,
  precision: 0,
  showThousandsSeparator: false,
  showRules: [FieldRuleEnum.REQUIRED],
};

export const dateTimeDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.DATE_TIME,
  icon: 'iconicon_calendar1',
  name: 'crmFormDesign.dateTime',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  datetype: 'datetime',
  showRules: [FieldRuleEnum.REQUIRED],
};

export const radioDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.RADIO,
  icon: 'iconicon_radio',
  name: 'crmFormDesign.radio',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  options: [],
  readable: true,
  editable: true,
  rules: [],
  showRules: [FieldRuleEnum.REQUIRED, FieldRuleEnum.UNIQUE],
  direction: 'vertical',
};

export const checkboxDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.CHECKBOX,
  icon: 'icona-icon_collectbeifen12',
  name: 'crmFormDesign.checkbox',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  options: [],
  readable: true,
  editable: true,
  rules: [],
  direction: 'vertical',
};

export const selectSingleDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.SELECT_SINGLE,
  icon: 'iconicon_pull_down_single_choice',
  name: 'crmFormDesign.selectSingle',
  fieldWidth: 1,
  showLabel: true,
  options: [],
  description: '',
  readable: true,
  editable: true,
  rules: [],
};

export const selectMultipleDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.SELECT_MULTIPLE,
  icon: 'iconicon_pull_down_multiple_selection',
  name: 'crmFormDesign.selectMultiple',
  fieldWidth: 1,
  showLabel: true,
  options: [],
  description: '',
  readable: true,
  editable: true,
  rules: [],
};

export const memberSingleDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.MEMBER_SINGLE,
  icon: 'iconicon_member_single_choice',
  name: 'crmFormDesign.memberSingle',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  defaultValue: [],
  showRules: [FieldRuleEnum.REQUIRED, FieldRuleEnum.UNIQUE],
};

export const memberMultipleDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.MEMBER_MULTIPLE,
  icon: 'iconicon_multiple_choice_of_members',
  name: 'crmFormDesign.memberMultiple',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  defaultValue: [],
  showRules: [FieldRuleEnum.REQUIRED],
};

export const departmentSingleDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.DEPARTMENT_SINGLE,
  icon: 'iconicon_department_single_choice',
  name: 'crmFormDesign.departmentSingle',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  defaultValue: [],
  showRules: [FieldRuleEnum.REQUIRED, FieldRuleEnum.UNIQUE],
};

export const departmentMultipleDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.DEPARTMENT_MULTIPLE,
  icon: 'icona-icon_multiple_selection_of_departments',
  name: 'crmFormDesign.departmentMultiple',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  defaultValue: [],
  showRules: [FieldRuleEnum.REQUIRED],
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
};

export const basicFields: FormCreateField[] = [
  inputDefaultFieldConfig,
  textareaDefaultFieldConfig,
  inputNumberDefaultFieldConfig,
  dateTimeDefaultFieldConfig,
  radioDefaultFieldConfig,
  checkboxDefaultFieldConfig,
  selectSingleDefaultFieldConfig,
  selectMultipleDefaultFieldConfig,
  memberSingleDefaultFieldConfig,
  memberMultipleDefaultFieldConfig,
  departmentSingleDefaultFieldConfig,
  departmentMultipleDefaultFieldConfig,
  dividerDefaultFieldConfig,
];

export const pictureDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.PICTURE,
  icon: 'iconicon_picture',
  name: 'crmFormDesign.picture',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
  uploadSizeLimit: 20,
};

export const locationDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.LOCATION,
  icon: 'iconicon_map',
  name: 'crmFormDesign.location',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
};

export const phoneDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.PHONE,
  icon: 'iconicon_phone',
  name: 'crmFormDesign.phone',
  fieldWidth: 1,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
};

export const advancedFields: FormCreateField[] = [
  pictureDefaultFieldConfig,
  locationDefaultFieldConfig,
  phoneDefaultFieldConfig,
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
