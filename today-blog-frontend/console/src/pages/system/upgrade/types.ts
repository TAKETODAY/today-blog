export interface VersionInfo {
  version: string;
  build?: string | null;
}

export interface Options {
  backupEnabled: boolean;
  backupTimeoutSeconds: number;
  healthTimeoutSeconds: number;
  note: string;
}

export interface Environment {
  updaterReady: boolean;
  backupReady: boolean;
  installFreeBytes: number;
  backupFreeBytes: number;
}

export interface ReleaseMapping {
  name: string;
  mode: 'SYMLINK';
  source: string;
  target: string;
}

export interface MappingSettings {
  mappings: ReleaseMapping[];
  roots: string[];
  ready: boolean;
  message: string;
}

export interface UpgradePackage {
  id: number;
  originalName: string;
  displayName: string;
  note?: string;
  status: 'READY' | 'CONSUMED';
  version: string;
  build: number;
  fileSize: number;
  sha256: string;
  entryCount: number;
  location: string;
  createdAt: string;
  consumedAt?: string;
}

export interface UpgradeRecord {
  id: number;
  packageId: number;
  packageName: string;
  packageVersion: string;
  packageBuild: number;
  packageSha256: string;
  packagePath: string;
  oldVersion?: string;
  oldBuild?: number;
  status: string;
  backupEnabled: boolean;
  backupTimeoutSeconds: number;
  healthTimeoutSeconds: number;
  note?: string;
  deletePackageAfterUpgrade: boolean;
  releasePath?: string;
  backupPath?: string;
  backupBytes?: number;
  backupSha256?: string;
  backupCompletedAt?: string;
  message?: string;
  createdAt: string;
  startedAt?: string;
  finishedAt?: string;
}

export interface MaintenanceStatus {
  enabled: boolean;
  message?: string;
  retryAfterSeconds?: number;
  source?: 'upgrade' | 'manual';
}
