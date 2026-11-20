import fs from 'node:fs';
import path from 'node:path';

const androidRoot = path.resolve(process.cwd(), 'android');
const outDir = path.resolve(process.cwd(), 'src/generated');
const outFile = path.join(outDir, 'androidProjectBundle.ts');

function walk(dir) {
  const results = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      results.push(...walk(full));
    } else {
      results.push(full);
    }
  }
  return results;
}

function classify(relPath) {
  if (relPath.startsWith('.github/')) return 'CI / GitHub Actions';
  if (relPath.endsWith('.kts') || relPath.endsWith('.toml') || relPath.endsWith('.properties') || relPath.endsWith('.pro')) return 'Gradle & Configuration';
  if (relPath === 'BUILD_APK.md') return 'Documentation APK';
  if (relPath.endsWith('AndroidManifest.xml')) return 'Manifest';
  if (relPath.includes('/res/')) return 'Ressources XML & Graphiques';
  if (relPath.includes('/test/') || relPath.includes('/androidTest/')) return 'Tests Unitaires & Room';
  if (relPath.includes('/ui/')) return 'UI Jetpack Compose';
  if (relPath.includes('/hardware/')) return 'Contrôleurs Matériels & Services';
  if (relPath.includes('/data/')) return 'Base Room & Repository';
  if (relPath.includes('/domain/')) return 'Domaine & Protections';
  return 'Application Android';
}

function detectLang(relPath) {
  if (relPath.endsWith('.kt') || relPath.endsWith('.kts')) return 'kotlin';
  if (relPath.endsWith('.xml')) return 'xml';
  if (relPath.endsWith('.yml') || relPath.endsWith('.yaml')) return 'yaml';
  if (relPath.endsWith('.toml')) return 'toml';
  if (relPath.endsWith('.md')) return 'markdown';
  return 'text';
}

const allFiles = walk(androidRoot)
  .map((full) => {
    const relPath = path.relative(androidRoot, full).replace(/\\/g, '/');
    const content = fs.readFileSync(full, 'utf8');
    return {
      path: relPath,
      category: classify(relPath),
      language: detectLang(relPath),
      lines: content.split('\n').length,
      bytes: Buffer.byteLength(content, 'utf8'),
      content,
    };
  })
  .sort((a, b) => a.path.localeCompare(b.path));

// Structural verification checks
const checks = [];
const fileMap = new Map(allFiles.map((f) => [f.path, f]));

const requiredFiles = [
  'settings.gradle.kts',
  'build.gradle.kts',
  'app/build.gradle.kts',
  'gradle/libs.versions.toml',
  'app/src/main/AndroidManifest.xml',
  'app/src/main/java/com/batteryshare/ai/BatteryShareApplication.kt',
  'app/src/main/java/com/batteryshare/ai/MainActivity.kt',
  'app/src/main/java/com/batteryshare/ai/data/local/BatteryShareDatabase.kt',
  'app/src/main/java/com/batteryshare/ai/hardware/usb/UsbPowerController.kt',
  'app/src/main/java/com/batteryshare/ai/hardware/wireless/WirelessPowerDetector.kt',
  'app/src/main/java/com/batteryshare/ai/hardware/protection/ProtectionMonitor.kt',
  'app/src/main/java/com/batteryshare/ai/hardware/connectivity/PeerConnectionManager.kt',
  'app/src/test/java/com/batteryshare/ai/ProtectionEvaluatorTest.kt',
  '.github/workflows/android-ci.yml',
  'BUILD_APK.md',
];

for (const req of requiredFiles) {
  checks.push({
    id: `file:${req}`,
    label: `Présence de ${req}`,
    passed: fileMap.has(req),
  });
}

// Verify Kotlin package declarations match folder hierarchy
const ktFiles = allFiles.filter((f) => f.path.endsWith('.kt'));
let packageMismatches = 0;
let braceImbalances = 0;
for (const kt of ktFiles) {
  const pkgMatch = kt.content.match(/^package\s+([a-zA-Z0-9_.]+)/m);
  if (!pkgMatch) {
    packageMismatches++;
  } else {
    const expectedSubpath = pkgMatch[1].replace(/\./g, '/');
    if (!kt.path.includes(expectedSubpath)) {
      packageMismatches++;
    }
  }
  const openBraces = (kt.content.match(/\{/g) || []).length;
  const closeBraces = (kt.content.match(/\}/g) || []).length;
  if (openBraces !== closeBraces) {
    braceImbalances++;
  }
}

checks.push({
  id: 'kotlin:packages',
  label: `Cohérence des packages Kotlin (${ktFiles.length} fichiers)`,
  passed: packageMismatches === 0,
});

checks.push({
  id: 'kotlin:braces',
  label: `Équilibre syntaxique des accolades Kotlin (${ktFiles.length} fichiers)`,
  passed: braceImbalances === 0,
});

// Verify Manifest references
const manifest = fileMap.get('app/src/main/AndroidManifest.xml')?.content || '';
checks.push({
  id: 'manifest:refs',
  label: 'Déclarations Manifest (Application, MainActivity, EnergyMonitorService, usb_device_filter)',
  passed:
    manifest.includes('.BatteryShareApplication') &&
    manifest.includes('.MainActivity') &&
    manifest.includes('.hardware.service.EnergyMonitorService') &&
    manifest.includes('@xml/usb_device_filter'),
});

// Verify no fake simulation claims
const usbKt = fileMap.get('app/src/main/java/com/batteryshare/ai/hardware/usb/UsbPowerController.kt')?.content || '';
const wirelessKt = fileMap.get('app/src/main/java/com/batteryshare/ai/hardware/wireless/WirelessPowerDetector.kt')?.content || '';
checks.push({
  id: 'hardware:honesty',
  label: 'Séparation stricte matériel/logiciel (USB-PD & Qi non simulés)',
  passed:
    usbKt.includes('hardwareTransferGuaranteed') &&
    wirelessKt.includes('hardwareControllableViaPublicApi = false'),
});

fs.mkdirSync(outDir, { recursive: true });
const output = `// Auto-generated from /android/ by scripts/bundle-android-project.mjs
export interface AndroidProjectFile {
  path: string;
  category: string;
  language: 'kotlin' | 'xml' | 'yaml' | 'toml' | 'markdown' | 'text';
  lines: number;
  bytes: number;
  content: string;
}

export interface AndroidVerificationCheck {
  id: string;
  label: string;
  passed: boolean;
}

export const ANDROID_PROJECT_FILES: AndroidProjectFile[] = ${JSON.stringify(allFiles, null, 2)};

export const ANDROID_VERIFICATION_CHECKS: AndroidVerificationCheck[] = ${JSON.stringify(checks, null, 2)};
`;

fs.writeFileSync(outFile, output, 'utf8');
console.log(`Bundled ${allFiles.length} Android files (${checks.filter((c) => c.passed).length}/${checks.length} checks passed).`);
