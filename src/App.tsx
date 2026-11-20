import { useState } from 'react';
import { TopBar, type WorkspaceSection } from './components/TopBar';
import { AndroidPhonePreview } from './components/AndroidPhonePreview';
import { AndroidProjectExplorer } from './components/AndroidProjectExplorer';
import { ArchitectureAndBuildSection } from './components/ArchitectureAndBuildSection';
import { downloadAndroidProjectZip } from './utils/exportAndroidZip';

export default function App() {
  const [activeSection, setActiveSection] = useState<WorkspaceSection>('preview');
  const [isExporting, setIsExporting] = useState(false);
  const [exportNotice, setExportNotice] = useState<string | null>(null);

  const handleExportZip = async () => {
    setIsExporting(true);
    setExportNotice(null);
    try {
      const result = await downloadAndroidProjectZip();
      setExportNotice(`Archive ${result.filename} générée (${result.fileCount} fichiers Android).`);
    } catch {
      setExportNotice('Erreur lors de la génération de l’archive ZIP.');
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#070D1F] text-white flex flex-col">
      <TopBar
        activeSection={activeSection}
        onNavigate={setActiveSection}
        onExportZip={handleExportZip}
        isExporting={isExporting}
      />

      {exportNotice && (
        <div
          role="status"
          className="border-b border-[#10B981]/40 bg-[#064E3B]/60 px-4 py-2 text-center text-xs text-[#D1FAE5]"
        >
          <span>{exportNotice}</span>
        </div>
      )}

      <main className="flex-1">
        {activeSection === 'preview' && <AndroidPhonePreview />}
        {activeSection === 'code' && <AndroidProjectExplorer />}
        {(activeSection === 'architecture' || activeSection === 'build') && (
          <ArchitectureAndBuildSection
            mode={activeSection}
            onExportZip={handleExportZip}
            isExporting={isExporting}
          />
        )}
      </main>

      <footer className="border-t border-[#162242] py-5 px-4 sm:px-6">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-3 text-xs text-[#94A3B8]">
          <span>BatteryShare AI · Version 1.0 · Kotlin · Jetpack Compose · Material 3</span>
          <span>Android 8.0+ (API 26–35) · Room Database · USB-C / OTG · Qi · Bluetooth · Wi-Fi Direct</span>
        </div>
      </footer>
    </div>
  );
}
