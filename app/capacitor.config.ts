import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.shorthand.app',
  appName: '神也背单词',
  webDir: 'dist',
  server: {
    androidScheme: 'http',
    cleartext: true
  }
}

export default config
