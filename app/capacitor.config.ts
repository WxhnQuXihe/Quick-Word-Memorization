import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.shortand.app',
  appName: '神也背单词',
  webDir: 'dist',
  server: {
    androidScheme: 'https'
  }
}

export default config
