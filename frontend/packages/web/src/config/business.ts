import { FormItemRule, InputProps } from 'naive-ui';

import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { OrgUserInfo } from '@lib/shared/models/system/org';

const { t } = useI18n();

interface AuthField {
  label: string;
  key: string;
  placeholder?: string;
  rule?: FormItemRule[];
  subTip?: string;
  inputProps?: Partial<InputProps>;
}

export const authTypeFieldMap: Record<string, AuthField[]> = {
  CAS: [
    {
      label: t('system.business.authenticationSettings.serviceUrl'),
      key: 'casUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.serviceUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', { url: 'http://<casurl>' }),
    },
    {
      label: t('system.business.authenticationSettings.loginUrl'),
      key: 'loginUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.loginUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', { url: 'http://<casurl>/login' }),
      subTip: t('system.business.authenticationSettings.loginUrlTip'),
    },
    {
      label: t('system.business.authenticationSettings.callbackUrl'),
      key: 'redirectUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.callbackUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<meteresphere-endpoint>/sso/callback/cas/{authId}',
      }),
    },
    {
      label: t('system.business.authenticationSettings.verifyUrl'),
      key: 'validateUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.verifyUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<casurl>/serviceValidate',
      }),
      subTip: t('system.business.authenticationSettings.verifyUrlTip'),
    },
  ],
  OIDC: [
    {
      label: t('system.business.authenticationSettings.authUrl'),
      key: 'authUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.authUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<keyclock>auth/realms/<cordys>/protocol/openid-connect/auth',
      }),
    },
    {
      label: t('system.business.authenticationSettings.tokenUrl'),
      key: 'tokenUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.tokenUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<keyclock>auth/realms/<cordys>/protocol/openid-connect/token',
      }),
    },
    {
      label: t('system.business.authenticationSettings.userInfoUrl'),
      key: 'userInfoUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.userInfoUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<keyclock>auth/realms/<cordys>/protocol/openid-connect/userinfo',
      }),
    },
    {
      label: t('system.business.authenticationSettings.callbackUrl'),
      key: 'redirectUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.callbackUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<cordys-endpoint>/sso/callback or http://<cordys-endpoint>/sso/callback/authld',
      }),
    },
    {
      label: t('system.business.authenticationSettings.clientId'),
      key: 'clientId',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: `${t('system.business.authenticationSettings.clientId')} ` }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'Cordys',
      }),
    },
    {
      label: t('system.business.authenticationSettings.clientSecret'),
      key: 'secret',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.clientSecret') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'Cordys',
      }),
    },
    {
      label: t('system.business.authenticationSettings.loginUrl'),
      key: 'loginUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.loginUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<casurl>/login',
      }),
      subTip: t('system.business.authenticationSettings.loginUrlTip'),
    },
  ],
  GITHUB_OAUTH2: [
    {
      label: t('system.business.authenticationSettings.authUrl'),
      key: 'authUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.authUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<keyclock>auth/realms/<cordys>/protocol/openid-connect/auth',
      }),
    },
    {
      label: t('system.business.authenticationSettings.tokenUrl'),
      key: 'tokenUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.tokenUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<keyclock>auth/realms/<cordys>/protocol/openid-connect/token',
      }),
    },
    {
      label: t('system.business.authenticationSettings.userInfoUrl'),
      key: 'userInfoUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.userInfoUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<keyclock>auth/realms/<cordys>/protocol/openid-connect/userinfo',
      }),
    },
    {
      label: t('system.business.authenticationSettings.callbackUrl'),
      key: 'redirectUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.callbackUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<cordys-endpoint>/sso/callback or http://<cordys-endpoint>/sso/callback/authld',
      }),
    },
    {
      label: t('system.business.authenticationSettings.clientId'),
      key: 'clientId',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: `${t('system.business.authenticationSettings.clientId')} ` }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'Cordys',
      }),
    },
    {
      label: t('system.business.authenticationSettings.clientSecret'),
      key: 'secret',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.clientSecret') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'Cordys',
      }),
    },
    {
      label: t('system.business.authenticationSettings.propertyMap'),
      key: 'propertyMap',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.propertyMap') }),
        },
      ],
      placeholder: '{"userid":"login","username":"name","email":"email"}',
    },
    {
      label: t('system.business.authenticationSettings.linkRange'),
      key: 'scope',
      placeholder: 'openid profile email',
    },
  ],
  WECOM_OAUTH2: [
    {
      label: t('system.business.corpId'),
      key: 'corpId',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.corpId') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'Cordys',
      }),
    },
    {
      label: t('system.business.appSecret'),
      key: 'appSecret',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.appSecret') }),
        },
      ],
      inputProps: {
        type: 'password',
        showPasswordOn: 'click',
        inputProps: { autocomplete: 'new-password' },
      },
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'Cordys',
      }),
    },
    {
      label: t('system.business.agentId'),
      key: 'agentId',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.agentId') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'Cordys',
      }),
    },
    {
      label: t('system.business.authenticationSettings.callbackUrl'),
      key: 'redirectUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.callbackUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<cordys-endpoint>/sso/callback or http://<cordys-endpoint>/sso/callback/authld',
      }),
    },
  ],
  LDAP: [
    {
      label: t('system.business.authenticationSettings.LDAPUrl'),
      key: 'url',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.LDAPUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.LDAPUrlPlaceholder'),
    },
    {
      label: t('system.business.authenticationSettings.DN'),
      key: 'dn',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: `${t('system.business.authenticationSettings.DN')} ` }),
        },
      ],
    },
    {
      label: t('system.business.authenticationSettings.password'),
      key: 'password',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.password') }),
        },
      ],
    },
    {
      label: t('system.business.authenticationSettings.OU'),
      key: 'ou',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: `${t('system.business.authenticationSettings.OU')} ` }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.OUPlaceholder'),
    },
    {
      label: t('system.business.authenticationSettings.userFilter'),
      key: 'filter',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.userFilter') }),
        },
      ],
    },
    {
      label: t('system.business.authenticationSettings.LDAPPropertyMap'),
      key: 'mapping',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.LDAPPropertyMap') }),
        },
      ],
      subTip: t('system.business.authenticationSettings.LDAPPropertyMapTip'),
    },
  ],
};

export const defaultAuthForm = {
  enable: false,
  description: '',
  name: '',
  type: 'WECOM_OAUTH2',
  configuration: {},
};

export const defaultUserInfo: OrgUserInfo = {
  id: '',
  name: '',
  email: '',
  password: '',
  enable: true,
  createTime: 0,
  updateTime: 0,
  language: '',
  lastOrganizationId: '',
  phone: '',
  source: '',
  createUser: '',
  updateUser: '',
  platformInfo: '',
  avatar: '',
  permissionIds: [''],
  organizationIds: [''],
  csrfToken: '',
  sessionId: '',
  roles: [],
  userId: '',
  userName: '',
  departmentName: '',
  departmentId: '',
  defaultPwd: true,
};
// 默认登录图片
export const defaultLoginImage = `${import.meta.env.BASE_URL}images/login-banner.png`;
// 默认登录logo
export const defaultLoginLogo = `${import.meta.env.BASE_URL}images/login-logo.svg`;
// 默认左上角平台logo
export const defaultPlatformLogo = `${import.meta.env.BASE_URL}images/logo_CORDYS.svg`;

export const platformType = [CompanyTypeEnum.WECOM, CompanyTypeEnum.DINGTALK, CompanyTypeEnum.LARK];

export const platFormNameMap: Record<string, string> = {
  [CompanyTypeEnum.DINGTALK]: t('login.form.DINGTALK'),
  [CompanyTypeEnum.LARK]: t('login.form.LARK'),
  [CompanyTypeEnum.WECOM]: t('login.form.WECOM'),
};
