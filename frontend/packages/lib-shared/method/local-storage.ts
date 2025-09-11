export const getLocalStorage = <T = string>(name: string, isJson?: boolean): T | null => {
  try {
    const value = localStorage.getItem(name);
    if (value && isJson) {
      return JSON.parse(value) as T;
    }
    return value as T;
  } catch {
    return null;
  }
};

export const setLocalStorage = (name: string, value: any): void => {
  try {
    if (typeof value !== 'string') {
      value = JSON.stringify(value);
    }
    localStorage.setItem(name, value);
  } catch {
    // ignore
  }
};

export const removeLocalStorage = (name: string) => {
  try {
    localStorage.removeItem(name);
  } catch {
    // ignore
  }
};

export const setSessionStorageTempState = (name: string, value: any) => {
  try {
    if (typeof value !== 'string') {
      value = JSON.stringify(value);
    }
    sessionStorage.setItem(name, value);
  } catch {
    // ignore
  }
};

export const getSessionStorageTempState = <T>(name: string, isJson?: boolean): T | null => {
  try {
    const value = sessionStorage.getItem(name);
    if (value && isJson) {
      return JSON.parse(value) as T;
    }
    return value as T;
  } catch {
    return null;
  }
};
