declare namespace NodeJS {
  interface ProcessEnv {
    EXPO_PUBLIC_API_BASE_URL?: string;
  }
}

type HomedashboardRuntimeConfig = {
  apiBaseUrl?: string;
};

interface Window {
  __HOMEDASHBOARD_CONFIG__?: HomedashboardRuntimeConfig;
}
