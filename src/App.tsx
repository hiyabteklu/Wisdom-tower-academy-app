import React, { useState, useEffect, useCallback } from 'react';
import { TabType, BookModule, CoursePackage } from './types';
import { ALL_BOOK_MODULES } from './data/courses';
import { offlineVault } from './services/offlineVault';
import { TopBar } from './components/TopBar';
import { BottomNav } from './components/BottomNav';
import { MenuModal } from './components/MenuModal';
import { ExitGuiltDialog } from './components/ExitGuiltDialog';
import { FuturisticLoader } from './components/FuturisticLoader';
import { BookReaderModal } from './components/BookReaderModal';
import { CheckoutModal } from './components/CheckoutModal';
import { OnboardingScreen } from './screens/OnboardingScreen';
import { HomeScreen } from './screens/HomeScreen';
import { LearningScreen } from './screens/LearningScreen';
import { PackagesScreen } from './screens/PackagesScreen';
import { AccountScreen } from './screens/AccountScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { NotificationsScreen } from './screens/NotificationsScreen';
import { InfoPages } from './screens/InfoPages';
import { OfflineScreen } from './screens/OfflineScreen';

export const App: React.FC = () => {
  // Splash & Onboarding state
  const [isSplashLoading, setIsSplashLoading] = useState(true);
  const [showOnboarding, setShowOnboarding] = useState(false);

  // Navigation & Loading state
  const [currentTab, setCurrentTab] = useState<TabType>('home');
  const [activeRoute, setActiveRoute] = useState<string | null>(null);
  const [isNavigating, setIsNavigating] = useState(false);

  // Dialogs & Modals state
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [showExitDialog, setShowExitDialog] = useState(false);
  const [activeReadingBook, setActiveReadingBook] = useState<BookModule | null>(null);
  const [activeCheckoutPackage, setActiveCheckoutPackage] = useState<CoursePackage | null>(null);

  // Enrolled packages
  const [enrolledPackageIds, setEnrolledPackageIds] = useState<Set<string>>(
    new Set(['freshman-complete'])
  );

  // Download tracking map: fileId -> percent
  const [downloadingFileIds, setDownloadingFileIds] = useState<Map<string, number>>(new Map());

  // Network state
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [showOfflineScreen, setShowOfflineScreen] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Cinematic splash timing guaranteeing user sees the full custom brand animation
  useEffect(() => {
    const onboardingDone = localStorage.getItem('wta_onboarding_done') === 'true';
    const timer = setTimeout(() => {
      setIsSplashLoading(false);
      if (!onboardingDone) {
        setShowOnboarding(true);
      }
    }, 2200);

    return () => clearTimeout(timer);
  }, []);

  const handleTabChange = useCallback((tab: TabType) => {
    if (tab === currentTab && !activeRoute) return;
    setIsNavigating(true);
    setActiveRoute(null);
    setCurrentTab(tab);
    setTimeout(() => {
      setIsNavigating(false);
    }, 450);
  }, [currentTab, activeRoute]);

  const handleRouteChange = useCallback((route: string | null) => {
    setIsNavigating(true);
    setActiveRoute(route);
    setTimeout(() => {
      setIsNavigating(false);
    }, 400);
  }, []);

  // Online / Offline listener
  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      setShowOfflineScreen(false);
    };
    const handleOffline = () => {
      setIsOnline(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // Offline Vault download subscriber
  useEffect(() => {
    const unsubscribe = offlineVault.subscribeProgress((url, progress) => {
      setDownloadingFileIds((prev) => {
        const next = new Map(prev);
        if (progress && progress.percent < 100) {
          next.set(url, progress.percent);
        } else {
          next.delete(url);
        }
        return next;
      });
    });

    return () => unsubscribe();
  }, []);

  const handleFinishOnboarding = () => {
    localStorage.setItem('wta_onboarding_done', 'true');
    setShowOnboarding(false);
  };

  const handleResetOnboarding = () => {
    localStorage.removeItem('wta_onboarding_done');
    setShowOnboarding(true);
  };

  const handleRefresh = useCallback(() => {
    setIsRefreshing(true);
    setTimeout(() => {
      setIsRefreshing(false);
    }, 800);
  }, []);

  const handleDownloadToVault = async (module: BookModule) => {
    await offlineVault.downloadToVault(module, (prog) => {
      setDownloadingFileIds((prev) => {
        const next = new Map(prev);
        next.set(module.fileId, prog.percent);
        return next;
      });
    });
  };

  const handlePackageSuccess = (pkg: CoursePackage) => {
    setEnrolledPackageIds((prev) => new Set([...prev, pkg.id]));
    setActiveCheckoutPackage(null);
  };

  // Render content based on active route or current tab
  const renderMainContent = () => {
    if (activeRoute === '/notifications') {
      return (
        <NotificationsScreen
          onBack={() => setActiveRoute(null)}
          onNavigateToTab={(tab) => {
            setActiveRoute(null);
            setCurrentTab(tab);
          }}
        />
      );
    }

    if (activeRoute && ['/about', '/contact', '/faq', '/privacy', '/terms'].includes(activeRoute)) {
      return <InfoPages route={activeRoute} onBack={() => setActiveRoute(null)} />;
    }

    switch (currentTab) {
      case 'home':
        return (
          <HomeScreen
            onNavigateToTab={(tab) => {
              setActiveRoute(null);
              setCurrentTab(tab);
            }}
            onOpenBook={(m) => setActiveReadingBook(m)}
          />
        );
      case 'learning':
        return (
          <LearningScreen
            onOpenBook={(m) => setActiveReadingBook(m)}
            onDownloadToVault={handleDownloadToVault}
            downloadingFileIds={downloadingFileIds}
          />
        );
      case 'packages':
        return (
          <PackagesScreen
            onSelectPackage={(pkg) => setActiveCheckoutPackage(pkg)}
            enrolledPackageIds={enrolledPackageIds}
          />
        );
      case 'account':
        return (
          <AccountScreen
            enrolledPackageIds={enrolledPackageIds}
            onNavigateToTab={(tab) => {
              setActiveRoute(null);
              setCurrentTab(tab);
            }}
          />
        );
      case 'settings':
        return (
          <SettingsScreen
            onResetOnboarding={handleResetOnboarding}
            onClearVault={() => {}}
          />
        );
      default:
        return null;
    }
  };

  if (isSplashLoading) {
    return <FuturisticLoader isSplash={true} />;
  }

  if (showOnboarding) {
    return <OnboardingScreen onFinished={handleFinishOnboarding} />;
  }

  if (showOfflineScreen) {
    return (
      <OfflineScreen
        onRetry={() => {
          if (navigator.onLine) setShowOfflineScreen(false);
        }}
        onOpenVault={() => {
          setShowOfflineScreen(false);
          setCurrentTab('learning');
        }}
      />
    );
  }

  return (
    <div className="min-h-screen bg-[#0F172A] text-slate-100 flex flex-col font-sans select-none">
      {/* Fixed Native Top Bar */}
      <TopBar
        onOpenMenu={() => setIsMenuOpen(true)}
        onOpenNotifications={() => handleRouteChange('/notifications')}
        onRefresh={handleRefresh}
        isRefreshing={isRefreshing}
        unreadCount={2}
      />

      {/* Futuristic loader overlay on navigation / content fetch */}
      {isNavigating && (
        <FuturisticLoader
          isSplash={false}
          statusText="Wisdom Tower Academy"
          subText="Loading module…"
        />
      )}

      {/* Main Content Area */}
      <main className="flex-1 w-full max-w-7xl mx-auto">
        {renderMainContent()}
      </main>

      {/* Apple-Quality 5-Tab Bottom Navigation Bar */}
      <BottomNav
        currentTab={currentTab}
        onSelectTab={handleTabChange}
      />

      {/* Hamburger Menu Card Dialog */}
      <MenuModal
        isOpen={isMenuOpen}
        onClose={() => setIsMenuOpen(false)}
        onNavigate={(route) => {
          setIsMenuOpen(false);
          handleRouteChange(route);
        }}
      />

      {/* Exit App Confirmation Dialog */}
      <ExitGuiltDialog
        isOpen={showExitDialog}
        onStay={() => setShowExitDialog(false)}
        onExit={() => {
          setShowExitDialog(false);
        }}
      />

      {/* In-App Book Reader Modal */}
      {activeReadingBook && (
        <BookReaderModal
          module={activeReadingBook}
          onClose={() => setActiveReadingBook(null)}
          isCached={offlineVault.isPdfCached(activeReadingBook.fileId)}
          onDownloadToVault={handleDownloadToVault}
          isDownloading={downloadingFileIds.has(activeReadingBook.fileId)}
          downloadPercent={downloadingFileIds.get(activeReadingBook.fileId)}
        />
      )}

      {/* Course Package Checkout Modal */}
      {activeCheckoutPackage && (
        <CheckoutModal
          pkg={activeCheckoutPackage}
          onClose={() => setActiveCheckoutPackage(null)}
          onSuccess={handlePackageSuccess}
        />
      )}
    </div>
  );
};
