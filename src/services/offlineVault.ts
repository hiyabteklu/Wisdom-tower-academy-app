import { PRESEEDED_BOOK_SIZES, ALL_BOOK_MODULES } from '../data/courses';
import { DownloadProgress } from '../types';

const VAULT_INDEX_KEY = 'wta_vault_index_v1';
const VAULT_STORAGE_PREFIX = 'wta_vault_file_';

export interface StoredVaultItem {
  id: string;
  fileId: string;
  title: string;
  url: string;
  sizeBytes: number;
  downloadedAt: number;
}

class OfflineVaultService {
  private activeProgress = new Map<string, DownloadProgress>();
  private listeners = new Set<(url: string, progress: DownloadProgress | null) => void>();

  public formatBytes(bytes: number): string {
    if (!bytes || bytes <= 0) return '0 B';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  public getPdfSize(fileIdOrUrl: string): number {
    if (!fileIdOrUrl) return 0;
    const clean = fileIdOrUrl.trim();

    for (const [key, size] of Object.entries(PRESEEDED_BOOK_SIZES)) {
      if (clean.includes(key)) {
        return size;
      }
    }

    const matched = ALL_BOOK_MODULES.find(m => 
      m.id === clean || m.fileId === clean || m.downloadUrl === clean
    );
    if (matched) return matched.sizeBytes;

    return 0;
  }

  public getPdfSizeByTitle(title: string): number {
    if (!title) return 0;
    const t = title.toLowerCase();
    const matched = ALL_BOOK_MODULES.find(m => m.title.toLowerCase().includes(t) || t.includes(m.title.toLowerCase()));
    if (matched) return matched.sizeBytes;
    return 0;
  }

  public isPdfCached(fileIdOrUrl: string): boolean {
    const items = this.getStoredItems();
    return items.some(item => 
      item.fileId === fileIdOrUrl || 
      item.url === fileIdOrUrl || 
      item.id === fileIdOrUrl ||
      fileIdOrUrl.includes(item.fileId)
    );
  }

  public getStoredItems(): StoredVaultItem[] {
    try {
      const raw = localStorage.getItem(VAULT_INDEX_KEY);
      if (!raw) return [];
      return JSON.parse(raw);
    } catch {
      return [];
    }
  }

  public getTotalVaultSize(): number {
    const items = this.getStoredItems();
    return items.reduce((acc, item) => acc + (item.sizeBytes || 0), 0);
  }

  public removeStoredItem(fileId: string): void {
    const items = this.getStoredItems();
    const filtered = items.filter(i => i.fileId !== fileId && i.id !== fileId);
    localStorage.setItem(VAULT_INDEX_KEY, JSON.stringify(filtered));
    try {
      localStorage.removeItem(VAULT_STORAGE_PREFIX + fileId);
    } catch {}
  }

  public clearAllVault(): void {
    const items = this.getStoredItems();
    for (const item of items) {
      try {
        localStorage.removeItem(VAULT_STORAGE_PREFIX + item.fileId);
      } catch {}
    }
    localStorage.removeItem(VAULT_INDEX_KEY);
  }

  public subscribeProgress(cb: (url: string, progress: DownloadProgress | null) => void) {
    this.listeners.add(cb);
    return () => {
      this.listeners.delete(cb);
    };
  }

  public getProgress(url: string): DownloadProgress | null {
    return this.activeProgress.get(url) || null;
  }

  /**
   * Simulates reliable byte streaming into offline vault with real-time HUD progress
   */
  public async downloadToVault(
    moduleItem: { id: string; fileId: string; title: string; downloadUrl: string; sizeBytes: number },
    onProgress?: (progress: DownloadProgress) => void
  ): Promise<boolean> {
    const targetUrl = moduleItem.downloadUrl;
    const totalSize = moduleItem.sizeBytes || this.getPdfSize(moduleItem.fileId) || 2_500_000;

    let loaded = 0;
    const stepSize = Math.max(64000, Math.floor(totalSize / 24));

    return new Promise<boolean>((resolve) => {
      const interval = setInterval(() => {
        loaded += stepSize;
        if (loaded > totalSize) loaded = totalSize;
        const percent = Math.min(100, Math.round((loaded / totalSize) * 100));

        const progress: DownloadProgress = { loaded, total: totalSize, percent };
        this.activeProgress.set(targetUrl, progress);
        this.activeProgress.set(moduleItem.fileId, progress);
        if (onProgress) onProgress(progress);
        this.listeners.forEach(fn => fn(targetUrl, progress));

        if (loaded >= totalSize) {
          clearInterval(interval);

          // Save to vault index
          const current = this.getStoredItems();
          if (!current.some(i => i.fileId === moduleItem.fileId)) {
            current.push({
              id: moduleItem.id,
              fileId: moduleItem.fileId,
              title: moduleItem.title,
              url: moduleItem.downloadUrl,
              sizeBytes: totalSize,
              downloadedAt: Date.now()
            });
            localStorage.setItem(VAULT_INDEX_KEY, JSON.stringify(current));
          }

          setTimeout(() => {
            this.activeProgress.delete(targetUrl);
            this.activeProgress.delete(moduleItem.fileId);
            this.listeners.forEach(fn => fn(targetUrl, null));
            resolve(true);
          }, 400);
        }
      }, 70);
    });
  }
}

export const offlineVault = new OfflineVaultService();
