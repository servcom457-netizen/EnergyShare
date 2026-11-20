import React from 'react';
import { Cpu, Download, GitBranch, ShieldCheck, Terminal } from 'lucide-react';
import { ANDROID_PROJECT_FILES } from '../generated/androidProjectBundle';

interface ArchitectureSectionProps {
  mode: 'architecture' | 'build';
  onExportZip: () => void;
  isExporting: boolean;
}

export const ArchitectureAndBuildSection: React.FC<ArchitectureSectionProps> = ({
  mode,
  onExportZip,
  isExporting,
}) => {
  const buildGuide =
    ANDROID_PROJECT_FILES.find((f) => f.path === 'BUILD_APK.md')?.content ?? '';
  const ciYaml =
    ANDROID_PROJECT_FILES.find((f) => f.path.endsWith('android-ci.yml'))?.content ?? '';

  if (mode === 'architecture') {
    return (
      <section aria-labelledby="audit-heading" className="mx-auto max-w-7xl px-4 py-8 sm:px-6 space-y-6">
        <div className="border-b border-[#23355E] pb-5">
          <p className="text-xs font-medium text-[#3B82F6]">
            Architecture MVVM · Séparation opérationnel / matériel
          </p>
          <h2 id="audit-heading" className="mt-1 font-display text-2xl sm:text-3xl font-bold text-white">
            Audit de faisabilité Android 8.0+ et limites matérielles
          </h2>
        </div>

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <div className="rounded-2xl border border-[#10B981] bg-[#0E1730] p-6 space-y-4">
            <div className="flex items-center gap-2.5">
              <ShieldCheck className="h-5 w-5 text-[#10B981]" aria-hidden="true" />
              <h3 className="font-display text-lg font-semibold text-white">
                Fonctions logicielles 100 % opérationnelles
              </h3>
            </div>
            <ul className="space-y-2.5 text-sm text-[#E2E8F0]">
              <li>• Lecture en direct du niveau, état, tension (mV), santé et température via <code className="font-mono text-[#3B82F6]">BatteryManager</code> &amp; <code className="font-mono text-[#3B82F6]">ACTION_BATTERY_CHANGED</code>.</li>
              <li>• Persistance Room Database (<code className="font-mono text-[#3B82F6]">EnergySessionEntity</code>, <code className="font-mono text-[#3B82F6]">ProtectionConfigEntity</code>).</li>
              <li>• Moteur de protection donneur (<code className="font-mono text-[#3B82F6]">ProtectionEvaluator</code> + service premier plan <code className="font-mono text-[#3B82F6]">EnergyMonitorService</code>).</li>
              <li>• Découverte et échange de télémétrie via Bluetooth RFCOMM et Wi-Fi Direct (<code className="font-mono text-[#3B82F6]">PeerConnectionManager</code>) avec consentement mutuel.</li>
              <li>• Détection USB Host / OTG et écoute des événements système USB (<code className="font-mono text-[#3B82F6]">UsbPowerController</code>).</li>
            </ul>
          </div>

          <div className="rounded-2xl border border-[#F59E0B] bg-[#0E1730] p-6 space-y-4">
            <div className="flex items-center gap-2.5">
              <Cpu className="h-5 w-5 text-[#F59E0B]" aria-hidden="true" />
              <h3 className="font-display text-lg font-semibold text-white">
                Fonctions dépendantes du matériel et de l&apos;OEM
              </h3>
            </div>
            <ul className="space-y-2.5 text-sm text-[#E2E8F0]">
              <li>• <strong>Alimentation inversée USB-C / OTG</strong> : dépend du contrôleur PMIC, du câble et de la négociation USB-PD (<code className="font-mono text-[#F59E0B]">UsbPortStatus</code> sur API 28+). Une connexion USB seule ne garantit jamais le transfert électrique.</li>
              <li>• <strong>Recharge sans fil inversée (Qi)</strong> : aucune API publique Android ne permet d&apos;allumer matériellement la bobine d&apos;émission. L&apos;app vérifie les capacités et ouvre les paramètres constructeur sans jamais simuler une charge Qi.</li>
              <li>• <strong>Liaison Bluetooth / Wi-Fi Direct</strong> : strictement limitée à l&apos;appairage et à la synchronisation des données batterie (aucune énergie transmise par radio).</li>
            </ul>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section aria-labelledby="build-heading" className="mx-auto max-w-7xl px-4 py-8 sm:px-6 space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4 border-b border-[#23355E] pb-5">
        <div>
          <p className="text-xs font-medium text-[#3B82F6]">
            Gradle 8.9 · JDK 17 · AGP 8.7.2 · GitHub Actions CI
          </p>
          <h2 id="build-heading" className="mt-1 font-display text-2xl sm:text-3xl font-bold text-white">
            Générer l&apos;APK et exécuter la CI GitHub Actions
          </h2>
        </div>

        <button
          type="button"
          onClick={onExportZip}
          disabled={isExporting}
          className="inline-flex min-h-[44px] items-center gap-2 rounded-xl bg-[#10B981] px-4 py-2.5 text-xs font-bold text-[#070D1F] hover:bg-[#059669]"
        >
          <Download className="h-4 w-4" aria-hidden="true" />
          <span>{isExporting ? 'Création du ZIP...' : 'Télécharger le projet Android (.zip)'}</span>
        </button>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-[#23355E] bg-[#0E1730] p-5 space-y-3">
          <div className="flex items-center gap-2">
            <Terminal className="h-4 w-4 text-[#3B82F6]" aria-hidden="true" />
            <h3 className="font-display text-base font-semibold text-white">
              Instructions de compilation APK (BUILD_APK.md)
            </h3>
          </div>
          <pre className="overflow-auto rounded-xl border border-[#162242] bg-[#070D1F] p-4 font-mono text-xs leading-relaxed text-[#E2E8F0] max-h-[520px]">
            <code>{buildGuide}</code>
          </pre>
        </div>

        <div className="rounded-2xl border border-[#23355E] bg-[#0E1730] p-5 space-y-3">
          <div className="flex items-center gap-2">
            <GitBranch className="h-4 w-4 text-[#10B981]" aria-hidden="true" />
            <h3 className="font-display text-base font-semibold text-white">
              Workflow CI (.github/workflows/android-ci.yml)
            </h3>
          </div>
          <pre className="overflow-auto rounded-xl border border-[#162242] bg-[#070D1F] p-4 font-mono text-xs leading-relaxed text-[#E2E8F0] max-h-[520px]">
            <code>{ciYaml}</code>
          </pre>
        </div>
      </div>
    </section>
  );
};
