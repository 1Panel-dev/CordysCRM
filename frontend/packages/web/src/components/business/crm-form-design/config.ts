import { FieldTypeEnum } from '../crm-form-create/enum';
import type { FormCreateField } from '../crm-form-create/types';

export const inputDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.INPUT,
  name: 'crmFormDesign.input',
  icon: 'iconicon_single_line_text',
  fieldWidth: 4,
  showLabel: true,
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
  fieldWidth: 4,
  showLabel: true,
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
  fieldWidth: 4,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
};

export const dateTimeDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.DATE_TIME,
  icon: 'iconicon_calendar1',
  name: 'crmFormDesign.dateTime',
  fieldWidth: 4,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
};

export const radioDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.RADIO,
  icon: 'iconicon_radio',
  name: 'crmFormDesign.radio',
  fieldWidth: 4,
  showLabel: true,
  description: '',
  options: [],
  readable: true,
  editable: true,
  rules: [],
};

export const checkboxDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.CHECKBOX,
  icon: 'icona-icon_collectbeifen12',
  name: 'crmFormDesign.checkbox',
  fieldWidth: 4,
  showLabel: true,
  description: '',
  options: [],
  readable: true,
  editable: true,
  rules: [],
};

export const selectSingleDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.SELECT_SINGLE,
  icon: 'iconicon_pull_down_single_choice',
  name: 'crmFormDesign.selectSingle',
  fieldWidth: 4,
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
  fieldWidth: 4,
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
  fieldWidth: 4,
  showLabel: true,
  options: [],
  description: '',
  readable: true,
  editable: true,
  rules: [],
};

export const memberMultipleDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.MEMBER_MULTIPLE,
  icon: 'iconicon_multiple_choice_of_members',
  name: 'crmFormDesign.memberMultiple',
  fieldWidth: 4,
  showLabel: true,
  options: [],
  description: '',
  readable: true,
  editable: true,
  rules: [],
};

export const departmentSingleDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.DEPARTMENT_SINGLE,
  icon: 'iconicon_department_single_choice',
  name: 'crmFormDesign.departmentSingle',
  fieldWidth: 4,
  showLabel: true,
  options: [],
  description: '',
  readable: true,
  editable: true,
  rules: [],
};

export const departmentMultipleDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.DEPARTMENT_MULTIPLE,
  icon: 'icona-icon_multiple_selection_of_departments',
  name: 'crmFormDesign.departmentMultiple',
  fieldWidth: 4,
  showLabel: true,
  options: [],
  description: '',
  readable: true,
  editable: true,
  rules: [],
};

export const dividerDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.DIVIDER,
  icon: 'iconicon_dividing_line',
  name: 'crmFormDesign.divider',
  fieldWidth: 4,
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
  fieldWidth: 4,
  showLabel: true,
  description: '',
  readable: true,
  editable: true,
  rules: [],
};

export const locationDefaultFieldConfig: FormCreateField = {
  id: '',
  type: FieldTypeEnum.LOCATION,
  icon: 'iconicon_map',
  name: 'crmFormDesign.location',
  fieldWidth: 4,
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
  fieldWidth: 4,
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
