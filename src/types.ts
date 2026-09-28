export interface BookModule {
  id: string;
  fileId: string;
  title: string;
  category: 'Freshman' | 'Natural Science' | 'Social Science' | 'Engineering' | 'Grade 9-12';
  sizeBytes: number;
  description: string;
  downloadUrl: string;
  pagesCount: number;
  authorOrDept: string;
  sampleContent: {
    chapter: string;
    sections: { heading: string; body: string }[];
  }[];
}

export interface Flashcard {
  id: string;
  subject: string;
  question: string;
  answer: string;
  explanation: string;
  mastered?: boolean;
}

export interface CoursePackage {
  id: string;
  title: string;
  tagline: string;
  priceETB: number;
  originalPriceETB: number;
  badge?: string;
  category: string;
  features: string[];
  enrolledStudents: number;
  rating: number;
}

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  timestamp: string;
  type: 'order' | 'material' | 'exam' | 'system';
  read: boolean;
  link?: string;
}

export interface DownloadProgress {
  loaded: number;
  total: number;
  percent: number;
}

export type TabType = 'home' | 'learning' | 'packages' | 'account' | 'settings';
