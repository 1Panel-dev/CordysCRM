import type { CompanyTypeEnum } from '../../enums/commonEnum';
import type { TableQueryParams } from '../common';
import { PersonalExportStatusEnum } from '@lib/shared/enums/systemEnum';

// 邮件设置
export interface ConfigEmailParams {
  host: string; // SMTP 主机
  port: string; // SMTP 端口
  account: string; // SMTP 账号
  password: string; // SMTP 密码
  from: string; // 指定发件人
  recipient: string; // 指定收件人
  ssl: string; // SSL 开关
  tsl: string; // TSL 开关
}

// 三方扫码登录配置基础类型
export interface ThirdPartyBaseLoginConfig { 
  agentId: string;
  appSecret: string;
  corpId: string;
  startEnable: boolean;
}
// 钉钉扫码登录配置类型
export interface ThirdPartyDingTalkLoginConfig extends ThirdPartyBaseLoginConfig { 
  appId: string;
}

// 飞书扫码登录配置类型
export interface ThirdPartyLarkLoginConfig extends ThirdPartyBaseLoginConfig { 
  redirectUrl: string;
}

// DE配置类型
export interface ThirdPartyDEConfig {
  agentId:string;
  appSecret:string;
  deAccessKey:string;
  deAutoSync: boolean;
  deBoardEnable: boolean;
  deOrgID: string;
  deSecretKey: string;
  redirectUrl: string;
}
// sql智能体配置类型
export interface ThirdPartySQLBotConfig {
  appSecret:string;
  sqlBotChatEnable: boolean;
  sqlBotBoardEnable: boolean;
}
// maxKB配置类型
export interface ThirdPartyMKConfig {
  appSecret:string;
  mkAddress: string;
  mkEnable: boolean;
} 
// 大单网配置类型
export interface ThirdPartyTenderConfig {
  tenderAddress:string;
  tenderEnable:boolean;
}

export interface ThirdPartyResourceConfig{
  type: CompanyTypeEnum;
  verify: boolean;
  config: Record<string, any>;
}

// 同步组织和扫码登录数据类型
export interface SyncAndScanItem extends ThirdPartyResourceConfig {
  title: string;
  description: string;
  logo: string;
  hasConfig: boolean;
}

export interface ThirdPartyResource {
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  type: string;
  organizationId: string;
  sync: boolean;
  syncResource: CompanyTypeEnum;
  enable: boolean;
}

export interface DEOrgItem {
  id: string;
  name: string;
}

// 同步组织和扫码卡片数据类型
export interface IntegrationItem extends ThirdPartyResourceConfig {
  type: CompanyTypeEnum; // 类型
  title: string;
  description: string;
  logo: string;
  hasConfig: boolean;
}

// 三方类型集合设置
export interface OptionDTO {
  id: string; // key
  name: string; // value
}

// 认证设置
export interface Auth {
  description: string; // 描述
  name: string; // 名称
  type: string; // 类型 OAUTH2, LDAP, OIDC, CAS
  enable: boolean; // 是否启用
}

export interface AuthForm extends Auth {
  id?: string; // 认证源ID
  configuration: Record<string, any>;
}

export interface AuthUpdateParams extends Auth {
  id?: string; // 认证源ID
  configuration: string; // 认证源配置
}

export interface AuthItem extends Auth {
  id: string; // ID
  createUser: string; // 创建人
  updateUser: string; // 修改人
  createTime: number; // 创建时间
  updateTime: number; // 更新时间
  configId: string; // 配置id
  content: string; // 配置内容
}

export interface AuthTableQueryParams extends TableQueryParams {
  configId: string; // 认证设置id
}

// 个人中心
export interface PersonalInfoRequest {
  email: string;
  phone: string;
}

export interface PersonalPassword {
  originPassword: string;
  password: string;
  confirmPassword: string;
}

export interface SendEmailDTO {
  email: string;
}

export interface RepeatClueParams extends TableQueryParams {
  name: string;
  id?: string;
}

export interface RepeatCustomerItem {
  id: string;
  name: string;
  owner: string;
  ownerName: string;
  createTime: number;
  repeatType: string;
  clueCount: number;
  clueModuleEnable: boolean;
  opportunityCount: number;
  opportunityModuleEnable: boolean;
}

export interface RepeatContactItem {
  id: string;
  name: string;
  customerName: string;
  ownerName: string;
  createTime: number;
  phone: string;
  enable: boolean;
  opportunityCount: number;
}

export interface RepeatClueItem {
  id: string;
  name: string;
  owner: string;
  stage: string;
  ownerName: string;
  contact: string;
  phone: string;
}

export interface RepeatOpportunityItem {
  id: string;
  name: string;
  customerId: string;
  customerName: string;
  stage: string;
  products: string[];
  productNames: string[];
  owner: string;
  ownerName: string;
  productNameList: string[];
}

export interface ExportCenterListParams {
  keyword: string;
  exportType: string;
  exportStatus: string;
}

export interface ExportCenterItem {
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  fileName: string;
  resourceType: string;
  fileId: string;
  status: PersonalExportStatusEnum;
  organizationId: string;
}

export interface ApiKey {
  id: string;
  createUser: string;
  accessKey: string;
  secretKey: string;
  createTime: number;
  enable: boolean;
  forever: boolean;
  expireTime: number;
  description: string;
}

export interface ApiKeyItem extends ApiKey {
  isExpire: boolean;
  desensitization: boolean;
  showDescInput: boolean;
}

export interface DefaultTimeForm {
  activeTimeType: string;
  time?: number;
  desc: string;
}

export interface UpdateApiKeyParams {
  id: string;
  forever?: boolean;
  expireTime?: number;
  description: string;
}

interface ParamItem {
  paramKey: string; // 参数的 key
  paramValue: string; // 参数的值
  type: string; // 参数类型，一般是 string
}

// 保存基础信息、邮箱信息接口入参
export type SaveInfoParams = ParamItem[];

// 界面设置
export interface SavePageConfigParams {
  fileList: (File | undefined)[];
  request: (Record<string, any> | undefined)[];
}

interface FileParamItem extends ParamItem {
  file: string;
  fileName: string;
}

// 页面配置返回参数
export type PageConfigReturns = FileParamItem[];




