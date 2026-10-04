export interface BottomNavItemConfig {
  id: string;
  title: string;
  url: string;
  iconName: 'Home' | 'BookOpen' | 'Layers' | 'User' | 'Settings';
}

export interface MenuLinkConfig {
  label: string;
  url: string;
  iconName: 'Info' | 'Mail' | 'HelpCircle' | 'Shield' | 'FileText' | 'LogOut';
  isDestructive?: boolean;
}

export interface OnboardPageConfig {
  title: string;
  body: string;
  iconName?: 'School' | 'CheckCircle' | 'Brain' | 'Bell';
  showGif?: boolean;
}

export interface StudyTimerState {
  isRunning: boolean;
  isPaused: boolean;
  remainingSeconds: number;
  totalSeconds: number;
  title: string;
  isDismissed: boolean;
}

export interface VaultBook {
  id: string;
  title: string;
  category: string;
  sizeBytes: number;
  formattedSize: string;
  fileId: string;
  url: string;
  isDownloaded: boolean;
  lastAccessed?: number;
}

export interface DownloadProgress {
  url: string;
  loaded: number;
  total: number;
  inProgress: boolean;
}
