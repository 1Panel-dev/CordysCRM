import { FormItemRule } from 'naive-ui';

import { useI18n } from '@/hooks/useI18n';

const { t } = useI18n();

interface AuthField {
  label: string;
  key: string;
  placeholder?: string;
  rule?: FormItemRule[];
  subTip?: string;
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
        url: 'http://<keyclock>auth/realms/<metersphere>/protocol/openid-connect/auth',
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
        url: 'http://<keyclock>auth/realms/<metersphere>/protocol/openid-connect/token',
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
        url: 'http://<keyclock>auth/realms/<metersphere>/protocol/openid-connect/userinfo',
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
        url: 'http://<metersphere-endpoint>/sso/callback or http://<metersphere-endpoint>/sso/callback/authld',
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
      label: t('system.business.authenticationSettings.logoutSessionUrl'),
      key: 'logoutUrl',
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('system.business.authenticationSettings.logoutSessionUrl') }),
        },
      ],
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<keyclock>auth/realms/<metersphere>/protocol/openid- connect/token',
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
  OAUTH2: [
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
        url: 'http://<keyclock>auth/realms/<metersphere>/protocol/openid-connect/auth',
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
        url: 'http://<keyclock>auth/realms/<metersphere>/protocol/openid-connect/token',
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
        url: 'http://<keyclock>auth/realms/<metersphere>/protocol/openid-connect/userinfo',
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
        url: 'http://<metersphere-endpoint>/sso/callback or http://<metersphere-endpoint>/sso/callback/authld',
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
      label: t('system.business.authenticationSettings.logoutSessionUrl'),
      key: 'logoutUrl',
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<keyclock>auth/realms/<metersphere>/protocol/openid-connect/token',
      }),
    },
    {
      label: t('system.business.authenticationSettings.linkRange'),
      key: 'scope',
      placeholder: 'openid profile email',
    },
    {
      label: t('system.business.authenticationSettings.loginUrl'),
      key: 'loginUrl',
      placeholder: t('system.business.authenticationSettings.commonUrlPlaceholder', {
        url: 'http://<casurl>/login',
      }),
      subTip: t('system.business.authenticationSettings.loginUrlTip'),
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
  id: '',
  enable: true,
  description: '',
  name: '',
  type: 'CAS',
  configuration: {},
};
