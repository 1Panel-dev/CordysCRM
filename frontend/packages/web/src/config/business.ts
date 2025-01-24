import { useI18n } from '@/hooks/useI18n';

const { t } = useI18n();

export const authTypeFieldMap: Record<string, { label: string; key: string }[]> = {
  CAS: [
    { label: t('system.business.authenticationSettings.serviceUrl'), key: 'casUrl' },
    { label: t('system.business.authenticationSettings.loginUrl'), key: 'loginUrl' },
    { label: t('system.business.authenticationSettings.callbackUrl'), key: 'redirectUrl' },
    { label: t('system.business.authenticationSettings.verifyUrl'), key: 'validateUrl' },
  ],
  OIDC: [
    { label: t('system.business.authenticationSettings.authUrl'), key: 'authUrl' },
    { label: t('system.business.authenticationSettings.tokenUrl'), key: 'tokenUrl' },
    { label: t('system.business.authenticationSettings.userInfoUrl'), key: 'userInfoUrl' },
    { label: t('system.business.authenticationSettings.callbackUrl'), key: 'redirectUrl' },
    { label: t('system.business.authenticationSettings.clientId'), key: 'clientId' },
    { label: t('system.business.authenticationSettings.clientSecret'), key: 'secret' },
    { label: t('system.business.authenticationSettings.logoutSessionUrl'), key: 'logoutUrl' },
    { label: t('system.business.authenticationSettings.loginUrl'), key: 'loginUrl' },
  ],
  OAUTH2: [
    { label: t('system.business.authenticationSettings.authUrl'), key: 'authUrl' },
    { label: t('system.business.authenticationSettings.tokenUrl'), key: 'tokenUrl' },
    { label: t('system.business.authenticationSettings.userInfoUrl'), key: 'userInfoUrl' },
    { label: t('system.business.authenticationSettings.callbackUrl'), key: 'redirectUrl' },
    { label: t('system.business.authenticationSettings.clientId'), key: 'clientId' },
    { label: t('system.business.authenticationSettings.clientSecret'), key: 'secret' },
    { label: t('system.business.authenticationSettings.logoutSessionUrl'), key: 'logoutUrl' },
    { label: t('system.business.authenticationSettings.linkRange'), key: 'scope' },
    { label: t('system.business.authenticationSettings.loginUrl'), key: 'loginUrl' },
  ],
  LDAP: [
    { label: t('system.business.authenticationSettings.LDAPUrl'), key: 'url' },
    { label: t('system.business.authenticationSettings.DN'), key: 'dn' },
    { label: t('system.business.authenticationSettings.password'), key: 'password' },
    { label: t('system.business.authenticationSettings.OU'), key: 'ou' },
    { label: t('system.business.authenticationSettings.userFilter'), key: 'filter' },
    { label: t('system.business.authenticationSettings.LDAPPropertyMap'), key: 'mapping' },
  ],
};

export default {};
