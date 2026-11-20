import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft,
  BatteryCharging,
  Bluetooth,
  History,
  RefreshCw,
  Settings,
  Share2,
  ShieldAlert,
  ShieldCheck,
  Trash2,
  Usb,
  Wifi,
  Zap,
} from 'lucide-react';
import {
  DEFAULT_PROTECTION_CONFIG,
  evaluateProtection,
  inspectUsbCapability,
  inspectWirelessCapability,
  type EnergySessionRecord,
  type ProtectionConfig,
  type TelemetrySnapshot,
  type TransferMode,
} from '../domain/energyLogic';

type PhoneRoute =
  | 'home'
  | 'usb'
  | 'wireless'
  | 'protection'
  | 'connection'
  | 'history'
  | 'settings';

interface PeerItem {
  id: string;
  name: string;
  transport: 'BLUETOOTH' | 'WIFI_DIRECT';
  batteryPct: number;
  tempC: number;
  supportsOtg: boolean;
}

const STORAGE_SESSIONS_KEY = 'batteryshare_v1_sessions';
const STORAGE_CONFIG_KEY = 'batteryshare_v1_protection';

const INITIAL_SESSIONS: EnergySessionRecord[] = [
  {
    id: 'ses-101',
    mode: 'Partage USB-C / OTG',
    peerDeviceName: 'Pixel 8 Pro',
    startBatteryPct: 86,
    endBatteryPct: 74,
    peakTempCelsius: 35.8,
    startedAtIso: '2026-11-19T10:15:00Z',
    endedAtIso: '2026-11-19T10:38:00Z',
    terminationReason: 'Arrêt manuel par l’utilisateur',
    hardwareTransferObserved: true,
  },
  {
    id: 'ses-102',
    mode: 'Réception sans fil Qi',
    peerDeviceName: 'Galaxy S24',
    startBatteryPct: 31,
    endBatteryPct: 44,
    peakTempCelsius: 37.9,
    startedAtIso: '2026-11-18T18:05:00Z',
    endedAtIso: '2026-11-18T18:42:00Z',
    terminationReason: 'Fonctionnement nominal',
    hardwareTransferObserved: true,
  },
];

const DISCOVERABLE_PEERS: PeerItem[] = [
  {
    id: 'AA:42:8C:19:0E:11',
    name: 'Pixel 8 Pro',
    transport: 'WIFI_DIRECT',
    batteryPct: 34,
    tempC: 30.2,
    supportsOtg: true,
  },
  {
    id: '5C:F3:70:92:1B:88',
    name: 'Galaxy S24 Ultra',
    transport: 'BLUETOOTH',
    batteryPct: 81,
    tempC: 32.1,
    supportsOtg: true,
  },
];

export const AndroidPhonePreview: React.FC = () => {
  const [route, setRoute] = useState<PhoneRoute>('home');

  // Telemetry state (reads real navigator.getBattery() when exposed by Chromium, with honest notice)
  const [telemetry, setTelemetry] = useState<TelemetrySnapshot>({
    levelPct: 76,
    status: 'DISCHARGING',
    pluggedSource: 'NONE',
    temperatureCelsius: 32.4,
    voltageMv: 3960,
    currentNowMa: -310,
    health: 'GOOD',
    technology: 'Li-ion',
    sourceLabel: 'Modèle de référence (navigateur sans capteur thermique)',
  });
  const [webBatteryLive, setWebBatteryLive] = useState(false);

  // Hardware diagnostic toggles (for testing real edge cases in preview)
  const [usbCableConnected, setUsbCableConnected] = useState(false);
  const [usbPortRole, setUsbPortRole] = useState<'SOURCE' | 'SINK' | 'NONE'>('NONE');
  const [usbCurrentConfirmed, setUsbCurrentConfirmed] = useState(false);

  // Protection state (persisted)
  const [protectionConfig, setProtectionConfig] = useState<ProtectionConfig>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_CONFIG_KEY);
      return saved ? (JSON.parse(saved) as ProtectionConfig) : DEFAULT_PROTECTION_CONFIG;
    } catch {
      return DEFAULT_PROTECTION_CONFIG;
    }
  });

  // Active monitoring session
  const [activeSession, setActiveSession] = useState<{
    mode: TransferMode;
    startPct: number;
    peakTemp: number | null;
    anomaly: boolean;
    startedAtIso: string;
  } | null>(null);

  // History (Room simulation backed by localStorage)
  const [sessions, setSessions] = useState<EnergySessionRecord[]>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_SESSIONS_KEY);
      return saved ? (JSON.parse(saved) as EnergySessionRecord[]) : INITIAL_SESSIONS;
    } catch {
      return INITIAL_SESSIONS;
    }
  });

  // Connection module state
  const [isScanning, setIsScanning] = useState(false);
  const [discoveredPeers, setDiscoveredPeers] = useState<PeerItem[]>([]);
  const [selectedPeer, setSelectedPeer] = useState<PeerItem | null>(null);
  const [authStatus, setAuthStatus] = useState<'IDLE' | 'AWAITING_CONSENT' | 'AUTHORIZED' | 'REJECTED'>('IDLE');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_CONFIG_KEY, JSON.stringify(protectionConfig));
    } catch {
      // ignore storage quota
    }
  }, [protectionConfig]);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_SESSIONS_KEY, JSON.stringify(sessions));
    } catch {
      // ignore storage quota
    }
  }, [sessions]);

  // Attempt Web Battery Status API if supported by browser
  useEffect(() => {
    const nav = navigator as Navigator & {
      getBattery?: () => Promise<{
        level: number;
        charging: boolean;
        addEventListener: (ev: string, cb: () => void) => void;
        removeEventListener: (ev: string, cb: () => void) => void;
      }>;
    };
    if (typeof nav.getBattery !== 'function') return;

    let mounted = true;
    let batteryRef: Awaited<ReturnType<NonNullable<typeof nav.getBattery>>> | null = null;

    const syncFromWebBattery = () => {
      if (!mounted || !batteryRef) return;
      const pct = Math.round(batteryRef.level * 100);
      setWebBatteryLive(true);
      setTelemetry((prev) => ({
        ...prev,
        levelPct: pct,
        status: batteryRef?.charging ? 'CHARGING' : 'DISCHARGING',
        pluggedSource: batteryRef?.charging ? 'AC' : 'NONE',
        sourceLabel: 'Web Battery Status API (niveau réel navigateur) · Température Android uniquement',
      }));
    };

    nav
      .getBattery()
      .then((bat) => {
        batteryRef = bat;
        syncFromWebBattery();
        bat.addEventListener('levelchange', syncFromWebBattery);
        bat.addEventListener('chargingchange', syncFromWebBattery);
      })
      .catch(() => {
        // Keep reference telemetry if blocked by permissions policy
      });

    return () => {
      mounted = false;
      if (batteryRef) {
        batteryRef.removeEventListener('levelchange', syncFromWebBattery);
        batteryRef.removeEventListener('chargingchange', syncFromWebBattery);
      }
    };
  }, []);

  const usbCapability = useMemo(
    () => inspectUsbCapability(usbCableConnected, usbPortRole, usbCurrentConfirmed),
    [usbCableConnected, usbPortRole, usbCurrentConfirmed]
  );

  const wirelessCapability = useMemo(
    () => inspectWirelessCapability(telemetry.pluggedSource, 'Android 8.0+ (API 26–35)'),
    [telemetry.pluggedSource]
  );

  const protectionVerdict = useMemo(
    () =>
      evaluateProtection(
        telemetry,
        protectionConfig,
        activeSession?.startPct ?? null,
        activeSession?.anomaly ?? false
      ),
    [telemetry, protectionConfig, activeSession]
  );

  // Auto-stop active session when protection verdict triggers critical stop
  useEffect(() => {
    if (activeSession && protectionVerdict.shouldStopSession) {
      const finished: EnergySessionRecord = {
        id: `ses-${Date.now()}`,
        mode: activeSession.mode,
        peerDeviceName: selectedPeer?.name ?? 'Appareil local',
        startBatteryPct: activeSession.startPct,
        endBatteryPct: telemetry.levelPct,
        peakTempCelsius: activeSession.peakTemp,
        startedAtIso: activeSession.startedAtIso,
        endedAtIso: new Date().toISOString(),
        terminationReason: protectionVerdict.reason,
        hardwareTransferObserved:
          activeSession.mode.includes('USB')
            ? usbCapability.hardwareTransferGuaranteed
            : telemetry.pluggedSource === 'WIRELESS',
      };
      setSessions((prev) => [finished, ...prev]);
      setActiveSession(null);
      setToastMessage(`Arrêt de sécurité automatique : ${protectionVerdict.reason}`);
    }
  }, [activeSession, protectionVerdict, selectedPeer, telemetry, usbCapability.hardwareTransferGuaranteed]);

  const startSession = (mode: TransferMode) => {
    const verdict = evaluateProtection(telemetry, protectionConfig, telemetry.levelPct, false);
    if (verdict.shouldStopSession && mode.includes('Partage')) {
      setToastMessage(verdict.alertMessage ?? 'Seuil de protection atteint.');
      return;
    }
    setActiveSession({
      mode,
      startPct: telemetry.levelPct,
      peakTemp: telemetry.temperatureCelsius,
      anomaly: false,
      startedAtIso: new Date().toISOString(),
    });
    setToastMessage(`Suivi démarré : ${mode}`);
  };

  const stopSession = (reason = 'Arrêt manuel par l’utilisateur') => {
    if (!activeSession) return;
    const record: EnergySessionRecord = {
      id: `ses-${Date.now()}`,
      mode: activeSession.mode,
      peerDeviceName: selectedPeer?.name ?? 'Appareil local',
      startBatteryPct: activeSession.startPct,
      endBatteryPct: telemetry.levelPct,
      peakTempCelsius: activeSession.peakTemp,
      startedAtIso: activeSession.startedAtIso,
      endedAtIso: new Date().toISOString(),
      terminationReason: reason,
      hardwareTransferObserved:
        activeSession.mode.includes('USB')
          ? usbCapability.hardwareTransferGuaranteed
          : telemetry.pluggedSource === 'WIRELESS',
    };
    setSessions((prev) => [record, ...prev]);
    setActiveSession(null);
    setToastMessage(`Session enregistrée dans Room : ${reason}`);
  };

  const handleStartPeerScan = () => {
    setIsScanning(true);
    setTimeout(() => {
      setDiscoveredPeers(DISCOVERABLE_PEERS);
      setIsScanning(false);
    }, 350);
  };

  const severityColor =
    protectionVerdict.severity === 'NOMINAL'
      ? 'text-[#10B981]'
      : protectionVerdict.severity === 'WARNING'
        ? 'text-[#F59E0B]'
        : 'text-[#EF4444]';

  return (
    <section aria-labelledby="preview-heading" className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12 lg:items-start">
        {/* Left column: Android Phone Interactive Compose Preview */}
        <div className="lg:col-span-5 flex flex-col items-center">
          <div className="w-full max-w-[390px] rounded-[28px] border border-[#23355E] bg-[#070D1F] p-4 shadow-2xl">
            {/* Android App Header inside phone */}
            <div className="mb-4 flex items-center justify-between border-b border-[#162242] pb-3">
              {route === 'home' ? (
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#152242] border border-[#23355E]">
                    <Zap className="h-5 w-5 text-[#10B981]" aria-hidden="true" />
                  </div>
                  <div>
                    <h1 id="preview-heading" className="font-display text-base font-bold text-white leading-tight">
                      BatteryShare AI
                    </h1>
                    <p className="text-xs text-[#94A3B8]">Version 1.0 · Android 8.0+</p>
                  </div>
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setRoute('home')}
                    aria-label="Retour à l'accueil"
                    className="flex h-10 w-10 items-center justify-center rounded-lg text-white hover:bg-[#152242]"
                  >
                    <ArrowLeft className="h-5 w-5" />
                  </button>
                  <span className="font-display text-sm font-semibold text-white">
                    {route === 'usb' && 'USB-C / OTG & Alimentation'}
                    {route === 'wireless' && 'Recharge sans fil inversée'}
                    {route === 'protection' && 'Protection énergétique'}
                    {route === 'connection' && 'Connexion BT & Wi-Fi Direct'}
                    {route === 'history' && 'Historique Room'}
                    {route === 'settings' && 'Paramètres & Audit'}
                  </span>
                </div>
              )}

              <span className="font-mono text-xs tabular-nums text-[#3B82F6]">
                {telemetry.levelPct}%
              </span>
            </div>

            {toastMessage && (
              <div
                role="status"
                className="mb-3 flex items-center justify-between gap-2 rounded-xl border border-[#23355E] bg-[#152242] px-3 py-2 text-xs text-white"
              >
                <span>{toastMessage}</span>
                <button
                  type="button"
                  onClick={() => setToastMessage(null)}
                  className="text-[#94A3B8] hover:text-white shrink-0"
                >
                  Fermer
                </button>
              </div>
            )}

            {/* SCREEN 1: HOME */}
            {route === 'home' && (
              <div className="space-y-3.5">
                {activeSession && (
                  <div className="rounded-xl border border-[#10B981] bg-[#064E3B] p-3 flex items-center justify-between gap-2">
                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-white truncate">
                        Actif : {activeSession.mode}
                      </p>
                      <p className="font-mono text-[11px] tabular-nums text-[#D1FAE5]">
                        Départ {activeSession.startPct}% · Actuel {telemetry.levelPct}%
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => stopSession()}
                      className="min-h-[38px] rounded-lg bg-[#EF4444] px-3 py-1.5 text-xs font-semibold text-white shrink-0"
                    >
                      ARRÊTER
                    </button>
                  </div>
                )}

                <div className="rounded-2xl border border-[#23355E] bg-[#0E1730] p-4">
                  <div className="flex items-end justify-between gap-4">
                    <div>
                      <p className="text-[11px] font-medium text-[#94A3B8]">Pourcentage actuel</p>
                      <p
                        data-testid="battery-level-readout"
                        className={`font-mono text-4xl font-bold tabular-nums ${
                          activeSession ? 'text-[#10B981]' : 'text-white'
                        }`}
                      >
                        {telemetry.levelPct}%
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="font-mono text-lg font-semibold tabular-nums text-[#3B82F6]">
                        {telemetry.temperatureCelsius?.toFixed(1) ?? '--'} °C
                      </p>
                      <p className="font-mono text-[11px] tabular-nums text-[#94A3B8]">
                        {telemetry.voltageMv} mV · {telemetry.technology}
                      </p>
                    </div>
                  </div>

                  <div className="mt-3 h-2.5 w-full overflow-hidden rounded-full bg-[#152242]">
                    <div
                      className={`h-full transition-all duration-300 ${
                        activeSession ? 'bg-[#10B981]' : 'bg-[#3B82F6]'
                      }`}
                      style={{ width: `${telemetry.levelPct}%` }}
                    />
                  </div>

                  <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-xs">
                    <span className="text-white">
                      État : {telemetry.status === 'CHARGING' ? 'En charge' : 'En décharge'}
                    </span>
                    <span className="text-[#94A3B8]">
                      USB Host prêt · Qi via OEM
                    </span>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => setRoute('usb')}
                  className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-xl bg-[#2563EB] px-4 py-3 text-sm font-bold text-white transition-colors hover:bg-[#1D4ED8]"
                >
                  <Share2 className="h-4 w-4" aria-hidden="true" />
                  <span>PARTAGER MON ÉNERGIE</span>
                </button>

                <button
                  type="button"
                  onClick={() => setRoute('wireless')}
                  className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-xl bg-[#10B981] px-4 py-3 text-sm font-bold text-[#070D1F] transition-colors hover:bg-[#059669]"
                >
                  <BatteryCharging className="h-4 w-4" aria-hidden="true" />
                  <span>RECEVOIR DE L&apos;ÉNERGIE</span>
                </button>

                <div className="grid grid-cols-2 gap-2.5">
                  <button
                    type="button"
                    onClick={() => setRoute('connection')}
                    className="flex min-h-[44px] items-center justify-center gap-2 rounded-xl border border-[#23355E] bg-[#0E1730] px-3 py-2 text-xs font-semibold text-white hover:bg-[#152242]"
                  >
                    <Bluetooth className="h-4 w-4 text-[#3B82F6]" aria-hidden="true" />
                    <span>CONNEXION</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setRoute('protection')}
                    className="flex min-h-[44px] items-center justify-center gap-2 rounded-xl border border-[#23355E] bg-[#0E1730] px-3 py-2 text-xs font-semibold text-white hover:bg-[#152242]"
                  >
                    <ShieldCheck className="h-4 w-4 text-[#10B981]" aria-hidden="true" />
                    <span>PROTECTION</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setRoute('history')}
                    className="flex min-h-[44px] items-center justify-center gap-2 rounded-xl border border-[#23355E] bg-[#0E1730] px-3 py-2 text-xs font-semibold text-white hover:bg-[#152242]"
                  >
                    <History className="h-4 w-4 text-white" aria-hidden="true" />
                    <span>HISTORIQUE</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setRoute('settings')}
                    className="flex min-h-[44px] items-center justify-center gap-2 rounded-xl border border-[#23355E] bg-[#0E1730] px-3 py-2 text-xs font-semibold text-white hover:bg-[#152242]"
                  >
                    <Settings className="h-4 w-4 text-white" aria-hidden="true" />
                    <span>PARAMÈTRES</span>
                  </button>
                </div>
              </div>
            )}

            {/* SCREEN 2: USB-C / OTG */}
            {route === 'usb' && (
              <div className="space-y-3">
                <div className="rounded-xl border border-[#23355E] bg-[#0E1730] p-3.5 space-y-1.5 text-xs">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-white flex items-center gap-1.5">
                      <Usb className="h-4 w-4 text-[#3B82F6]" /> Diagnostic USB réel
                    </span>
                    <button
                      type="button"
                      onClick={() => setToastMessage('État USB-C actualisé via UsbManager.')}
                      className="text-[#3B82F6] hover:underline inline-flex items-center gap-1"
                    >
                      <RefreshCw className="h-3 w-3" /> Actualiser
                    </button>
                  </div>
                  <p className={usbCapability.usbCableDetected ? 'text-[#10B981]' : 'text-[#94A3B8]'}>
                    Connexion USB : {usbCapability.usbCableDetected ? 'Détectée' : 'Non détectée'}
                  </p>
                  <p className="text-white">Rôle d&apos;alimentation : {usbCapability.portRole}</p>
                  <p className="text-white">
                    Transfert garanti : {usbCapability.hardwareTransferGuaranteed ? 'Confirmé (USB-PD)' : 'Non garanti par simple câble'}
                  </p>
                </div>

                <div className="rounded-xl border border-[#F59E0B] bg-[#1F1607] p-3 text-xs text-white">
                  <p className="font-semibold text-[#F59E0B] mb-1">Avertissement matériel</p>
                  <p>{usbCapability.diagnosticNote}</p>
                </div>

                <div className="rounded-xl border border-[#23355E] bg-[#0E1730] p-3 text-xs text-[#94A3B8] space-y-1">
                  <p className="font-semibold text-white">Guide de connexion deux téléphones</p>
                  <p>1. Branchez un câble USB-C vers USB-C ou adaptateur OTG côté donneur.</p>
                  <p>2. Dans la notification système Android, sélectionnez « Charger l&apos;appareil connecté ».</p>
                  <p>3. Activez le suivi ci-dessous pour protéger la batterie du donneur.</p>
                </div>

                {activeSession ? (
                  <button
                    type="button"
                    onClick={() => stopSession()}
                    className="min-h-[44px] w-full rounded-xl bg-[#EF4444] py-2.5 text-xs font-bold text-white"
                  >
                    ARRÊTER LE SUIVI ACTIF
                  </button>
                ) : (
                  <div className="space-y-2">
                    <button
                      type="button"
                      onClick={() => startSession('Partage USB-C / OTG')}
                      className="min-h-[44px] w-full rounded-xl bg-[#2563EB] py-2.5 text-xs font-bold text-white"
                    >
                      SUIVRE EN MODE DONNEUR USB-C
                    </button>
                    <button
                      type="button"
                      onClick={() => startSession('Réception USB-C / OTG')}
                      className="min-h-[44px] w-full rounded-xl bg-[#10B981] py-2.5 text-xs font-bold text-[#070D1F]"
                    >
                      SUIVRE EN MODE RÉCEPTEUR USB-C
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* SCREEN 3: WIRELESS QI */}
            {route === 'wireless' && (
              <div className="space-y-3">
                <div className="rounded-xl border border-[#23355E] bg-[#0E1730] p-3.5 space-y-1.5 text-xs">
                  <p className="font-semibold text-white">État matériel sans fil réel</p>
                  <p className={wirelessCapability.isReceivingWirelessNow ? 'text-[#10B981]' : 'text-[#94A3B8]'}>
                    Réception Qi active : {wirelessCapability.isReceivingWirelessNow ? 'OUI' : 'NON'}
                  </p>
                  <p className="text-[#3B82F6] font-mono tabular-nums">
                    Batterie : {telemetry.levelPct}% · Temp : {telemetry.temperatureCelsius?.toFixed(1)} °C
                  </p>
                  <p className="text-[#94A3B8]">{wirelessCapability.diagnosticNote}</p>
                </div>

                <div className="rounded-xl border border-[#23355E] bg-[#0E1730] p-3 text-xs text-[#94A3B8] space-y-1">
                  <p className="font-semibold text-white">Placement & Paramètres OEM</p>
                  <p>1. Retirez les coques épaisses des deux téléphones.</p>
                  <p>2. Ouvrez : {wirelessCapability.oemSettingsPath}</p>
                  <p>3. Alignez les dos des deux téléphones au centre de la bobine Qi.</p>
                </div>

                {activeSession ? (
                  <button
                    type="button"
                    onClick={() => stopSession()}
                    className="min-h-[44px] w-full rounded-xl bg-[#EF4444] py-2.5 text-xs font-bold text-white"
                  >
                    ARRÊTER LA SURVEILLANCE
                  </button>
                ) : (
                  <div className="space-y-2">
                    <button
                      type="button"
                      onClick={() => startSession('Réception sans fil Qi')}
                      className="min-h-[44px] w-full rounded-xl bg-[#10B981] py-2.5 text-xs font-bold text-[#070D1F]"
                    >
                      SURVEILLER RÉCEPTION SANS FIL
                    </button>
                    <button
                      type="button"
                      onClick={() => startSession('Partage sans fil Qi')}
                      className="min-h-[44px] w-full rounded-xl bg-[#2563EB] py-2.5 text-xs font-bold text-white"
                    >
                      SURVEILLER PARTAGE SANS FIL (DONNEUR)
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* SCREEN 4: PROTECTION */}
            {route === 'protection' && (
              <div className="space-y-3 text-xs">
                <div className="rounded-xl border border-[#23355E] bg-[#0E1730] p-3.5">
                  <p className={`font-semibold ${severityColor}`}>
                    Verdict : {protectionVerdict.reason}
                  </p>
                  <p className="mt-1 text-[#94A3B8]">
                    {protectionVerdict.alertMessage ?? 'Paramètres thermiques et batterie dans la plage nominale.'}
                  </p>
                </div>

                <div className="rounded-xl border border-[#23355E] bg-[#0E1730] p-3.5 space-y-3">
                  <div>
                    <div className="flex justify-between text-white mb-1">
                      <label htmlFor="min-donor-slider">Seuil minimal donneur</label>
                      <span className="font-mono tabular-nums">{protectionConfig.minDonorBatteryPct}%</span>
                    </div>
                    <input
                      id="min-donor-slider"
                      type="range"
                      min={15}
                      max={70}
                      value={protectionConfig.minDonorBatteryPct}
                      onChange={(e) =>
                        setProtectionConfig((c) => ({ ...c, minDonorBatteryPct: Number(e.target.value) }))
                      }
                      className="w-full accent-[#3B82F6]"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between text-white mb-1">
                      <label htmlFor="max-share-slider">Limite de partage max</label>
                      <span className="font-mono tabular-nums">{protectionConfig.maxShareDeltaPct}%</span>
                    </div>
                    <input
                      id="max-share-slider"
                      type="range"
                      min={5}
                      max={50}
                      value={protectionConfig.maxShareDeltaPct}
                      onChange={(e) =>
                        setProtectionConfig((c) => ({ ...c, maxShareDeltaPct: Number(e.target.value) }))
                      }
                      className="w-full accent-[#10B981]"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between text-white mb-1">
                      <label htmlFor="max-temp-slider">Plafond thermique</label>
                      <span className="font-mono tabular-nums">{protectionConfig.maxTempCelsius.toFixed(1)} °C</span>
                    </div>
                    <input
                      id="max-temp-slider"
                      type="range"
                      min={36}
                      max={46}
                      step={0.5}
                      value={protectionConfig.maxTempCelsius}
                      onChange={(e) =>
                        setProtectionConfig((c) => ({ ...c, maxTempCelsius: Number(e.target.value) }))
                      }
                      className="w-full accent-[#F59E0B]"
                    />
                  </div>

                  <label className="flex items-center justify-between text-white pt-1">
                    <span>Arrêt auto seuil critique</span>
                    <input
                      type="checkbox"
                      checked={protectionConfig.autoStopOnCritical}
                      onChange={(e) =>
                        setProtectionConfig((c) => ({ ...c, autoStopOnCritical: e.target.checked }))
                      }
                      className="h-4 w-4 accent-[#3B82F6]"
                    />
                  </label>
                </div>

                {activeSession && (
                  <button
                    type="button"
                    onClick={() => stopSession('Arrêt d’urgence protection')}
                    className="min-h-[44px] w-full rounded-xl bg-[#EF4444] py-2.5 font-bold text-white"
                  >
                    ARRÊTER LE PARTAGE IMMÉDIATEMENT
                  </button>
                )}
              </div>
            )}

            {/* SCREEN 5: CONNECTION */}
            {route === 'connection' && (
              <div className="space-y-3 text-xs">
                <div className="rounded-xl border border-[#3B82F6] bg-[#0E1D3D] p-3 text-white">
                  <p className="font-semibold text-[#3B82F6] mb-1">Rôle réseau uniquement</p>
                  <p>
                    Bluetooth et Wi-Fi Direct synchronisent les niveaux de batterie et autorisations. Ils ne transfèrent aucune énergie électrique.
                  </p>
                </div>

                <button
                  type="button"
                  onClick={handleStartPeerScan}
                  disabled={isScanning}
                  className="min-h-[44px] w-full rounded-xl bg-[#2563EB] py-2.5 font-bold text-white"
                >
                  {isScanning ? 'RECHERCHE EN COURS...' : 'RECHERCHER DES TÉLÉPHONES'}
                </button>

                {selectedPeer && authStatus === 'AWAITING_CONSENT' && (
                  <div className="rounded-xl border border-[#10B981] bg-[#0E1730] p-3 space-y-2">
                    <p className="font-semibold text-white">
                      Autorisation requise sur les deux téléphones : {selectedPeer.name}
                    </p>
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => setAuthStatus('AUTHORIZED')}
                        className="flex-1 min-h-[38px] rounded-lg bg-[#10B981] font-bold text-[#070D1F]"
                      >
                        Autoriser
                      </button>
                      <button
                        type="button"
                        onClick={() => setAuthStatus('REJECTED')}
                        className="flex-1 min-h-[38px] rounded-lg border border-[#EF4444] text-[#EF4444] font-semibold"
                      >
                        Refuser
                      </button>
                    </div>
                  </div>
                )}

                {selectedPeer && authStatus === 'AUTHORIZED' && (
                  <div className="rounded-xl border border-[#10B981] bg-[#064E3B] p-3 text-white space-y-1">
                    <p className="font-semibold">Pair synchronisé : {selectedPeer.name}</p>
                    <p className="font-mono tabular-nums text-[#D1FAE5]">
                      Batterie distante : {selectedPeer.batteryPct}% · {selectedPeer.tempC.toFixed(1)} °C · OTG {selectedPeer.supportsOtg ? 'Prêt' : 'Non'}
                    </p>
                  </div>
                )}

                <div className="space-y-2">
                  {discoveredPeers.length === 0 ? (
                    <p className="text-[#94A3B8]">Appuyez sur Rechercher pour découvrir les appareils Bluetooth / Wi-Fi Direct.</p>
                  ) : (
                    discoveredPeers.map((peer) => (
                      <div
                        key={peer.id}
                        className="flex items-center justify-between rounded-xl border border-[#23355E] bg-[#0E1730] p-3"
                      >
                        <div className="flex items-center gap-2 min-w-0">
                          {peer.transport === 'BLUETOOTH' ? (
                            <Bluetooth className="h-4 w-4 text-[#3B82F6] shrink-0" />
                          ) : (
                            <Wifi className="h-4 w-4 text-[#10B981] shrink-0" />
                          )}
                          <div className="min-w-0">
                            <p className="font-semibold text-white truncate">{peer.name}</p>
                            <p className="font-mono text-[10px] text-[#94A3B8]">{peer.transport}</p>
                          </div>
                        </div>
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedPeer(peer);
                            setAuthStatus('AWAITING_CONSENT');
                          }}
                          className="min-h-[36px] rounded-lg bg-[#2563EB] px-3 py-1 font-semibold text-white shrink-0"
                        >
                          Connecter
                        </button>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}

            {/* SCREEN 6: HISTORY */}
            {route === 'history' && (
              <div className="space-y-2.5 text-xs">
                {sessions.length === 0 ? (
                  <p className="py-8 text-center text-[#94A3B8]">Aucune session enregistrée dans Room.</p>
                ) : (
                  sessions.map((ses) => (
                    <div
                      key={ses.id}
                      className="flex items-start justify-between gap-2 rounded-xl border border-[#23355E] bg-[#0E1730] p-3"
                    >
                      <div className="space-y-0.5 min-w-0">
                        <p className="font-semibold text-white">{ses.mode}</p>
                        <p className="text-[#94A3B8]">
                          {ses.peerDeviceName} · {ses.startBatteryPct}% → {ses.endBatteryPct}%
                        </p>
                        <p className={ses.hardwareTransferObserved ? 'text-[#10B981]' : 'text-[#94A3B8]'}>
                          {ses.terminationReason}
                        </p>
                      </div>
                      <button
                        type="button"
                        aria-label={`Supprimer session ${ses.mode}`}
                        onClick={() => setSessions((prev) => prev.filter((item) => item.id !== ses.id))}
                        className="p-2 text-[#EF4444] hover:bg-[#152242] rounded-lg shrink-0"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  ))
                )}
              </div>
            )}

            {/* SCREEN 7: SETTINGS */}
            {route === 'settings' && (
              <div className="space-y-3 text-xs">
                <div className="rounded-xl border border-[#23355E] bg-[#0E1730] p-3.5 space-y-1">
                  <p className="font-semibold text-white">BatteryShare AI · Version 1.0</p>
                  <p className="text-[#94A3B8]">Kotlin · Jetpack Compose · Material 3 · MVVM · Room · Android 8.0+</p>
                </div>
                <div className="rounded-xl border border-[#10B981] bg-[#0E1730] p-3.5 space-y-1">
                  <p className="font-semibold text-[#10B981]">Fonctions opérationnelles</p>
                  <p className="text-white">• Capteur batterie, tension et température Android</p>
                  <p className="text-white">• Règles de protection et Room Database</p>
                  <p className="text-white">• Découverte Bluetooth & Wi-Fi Direct</p>
                </div>
                <div className="rounded-xl border border-[#F59E0B] bg-[#0E1730] p-3.5 space-y-1">
                  <p className="font-semibold text-[#F59E0B]">Dépendant du matériel / OEM</p>
                  <p className="text-white">• Négociation USB-PD SOURCE/SINK du contrôleur PMIC</p>
                  <p className="text-white">• Activation bobine Qi inversée dans les réglages constructeur</p>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right column: Hardware Test Bench & Engineering Verification */}
        <div className="lg:col-span-7 space-y-6">
          <div>
            <p className="text-xs font-medium text-[#3B82F6]">
              BatteryShare AI V1.0 · Banc de diagnostic matériel et télémétrie
            </p>
            <h2 className="mt-1 font-display text-2xl sm:text-3xl font-bold tracking-tight text-white">
              Séparation stricte entre fonctions logicielles et capacités matérielles
            </h2>
            <p className="mt-2 text-sm text-[#94A3B8] leading-relaxed">
              L&apos;aperçu Android interactif à gauche reproduit fidèlement les 7 écrans Jetpack Compose Material 3 du projet Kotlin.
              {webBatteryLive
                ? ' Le niveau de batterie est lu directement via la Web Battery API de votre navigateur.'
                : ' Votre navigateur ne fournit pas les capteurs thermiques/USB Android natifs : ajustez les paramètres ci-dessous pour éprouver les protections et les diagnostics USB-C / Qi.'}
            </p>
          </div>

          {/* Real-time Telemetry & Protection Bench */}
          <div className="rounded-2xl border border-[#23355E] bg-[#0E1730] p-5 space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[#162242] pb-3">
              <h3 className="font-display text-base font-semibold text-white">
                Banc d&apos;essai des capteurs et protections (ProtectionEvaluator)
              </h3>
              <span className={`font-mono text-xs font-semibold ${severityColor}`}>
                {protectionVerdict.severity === 'NOMINAL' && 'VERDICT : NOMINAL'}
                {protectionVerdict.severity === 'WARNING' && 'VERDICT : ALERTE THERMIQUE'}
                {protectionVerdict.severity === 'CRITICAL_STOP' && 'VERDICT : ARRÊT CRITIQUE'}
              </span>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => {
                  setTelemetry((p) => ({ ...p, levelPct: 76, temperatureCelsius: 32.4 }));
                  setUsbCableConnected(false);
                  setUsbPortRole('NONE');
                  setUsbCurrentConfirmed(false);
                }}
                className="min-h-[34px] rounded-lg border border-[#23355E] bg-[#070D1F] px-3 py-1 text-xs text-white hover:bg-[#152242] whitespace-nowrap shrink-0"
              >
                Nominal (76% · 32.4 °C)
              </button>
              <button
                type="button"
                onClick={() => setTelemetry((p) => ({ ...p, levelPct: 22 }))}
                className="min-h-[34px] rounded-lg border border-[#23355E] bg-[#070D1F] px-3 py-1 text-xs text-white hover:bg-[#152242] whitespace-nowrap shrink-0"
              >
                Tester seuil bas (22%)
              </button>
              <button
                type="button"
                onClick={() => setTelemetry((p) => ({ ...p, temperatureCelsius: 43.0 }))}
                className="min-h-[34px] rounded-lg border border-[#23355E] bg-[#070D1F] px-3 py-1 text-xs text-white hover:bg-[#152242] whitespace-nowrap shrink-0"
              >
                Tester surchauffe (43 °C)
              </button>
              <button
                type="button"
                onClick={() => {
                  setUsbCableConnected(true);
                  setUsbPortRole('NONE');
                  setUsbCurrentConfirmed(false);
                }}
                className="min-h-[34px] rounded-lg border border-[#23355E] bg-[#070D1F] px-3 py-1 text-xs text-white hover:bg-[#152242] whitespace-nowrap shrink-0"
              >
                Câble USB sans PD
              </button>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <div className="flex justify-between text-xs">
                  <label htmlFor="bench-battery" className="text-[#94A3B8]">Niveau batterie testé</label>
                  <span className="font-mono tabular-nums text-white">{telemetry.levelPct}%</span>
                </div>
                <input
                  id="bench-battery"
                  type="range"
                  min={5}
                  max={100}
                  value={telemetry.levelPct}
                  onChange={(e) =>
                    setTelemetry((prev) => ({ ...prev, levelPct: Number(e.target.value) }))
                  }
                  className="w-full accent-[#3B82F6]"
                />
              </div>

              <div className="space-y-1.5">
                <div className="flex justify-between text-xs">
                  <label htmlFor="bench-temp" className="text-[#94A3B8]">Température cellule</label>
                  <span className="font-mono tabular-nums text-white">
                    {telemetry.temperatureCelsius?.toFixed(1)} °C
                  </span>
                </div>
                <input
                  id="bench-temp"
                  type="range"
                  min={20}
                  max={47}
                  step={0.5}
                  value={telemetry.temperatureCelsius ?? 32}
                  onChange={(e) =>
                    setTelemetry((prev) => ({
                      ...prev,
                      temperatureCelsius: Number(e.target.value),
                    }))
                  }
                  className="w-full accent-[#F59E0B]"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3 pt-1">
              <label className="flex items-center gap-2 text-xs text-white cursor-pointer">
                <input
                  type="checkbox"
                  checked={usbCableConnected}
                  onChange={(e) => {
                    const checked = e.target.checked;
                    setUsbCableConnected(checked);
                    if (!checked) {
                      setUsbPortRole('NONE');
                      setUsbCurrentConfirmed(false);
                    }
                  }}
                  className="h-4 w-4 accent-[#3B82F6]"
                />
                <span>Câble USB-C / OTG raccordé</span>
              </label>

              <div className="flex items-center gap-2 text-xs">
                <label htmlFor="usb-role-select" className="text-[#94A3B8] shrink-0">Rôle PD :</label>
                <select
                  id="usb-role-select"
                  value={usbPortRole}
                  disabled={!usbCableConnected}
                  onChange={(e) =>
                    setUsbPortRole(e.target.value as 'SOURCE' | 'SINK' | 'NONE')
                  }
                  className="w-full rounded-lg border border-[#23355E] bg-[#070D1F] px-2 py-1.5 text-xs text-white disabled:opacity-50"
                >
                  <option value="NONE">NONE (Données seules)</option>
                  <option value="SOURCE">SOURCE (Donneur)</option>
                  <option value="SINK">SINK (Récepteur)</option>
                </select>
              </div>

              <label className="flex items-center gap-2 text-xs text-white cursor-pointer">
                <input
                  type="checkbox"
                  checked={usbCurrentConfirmed}
                  disabled={!usbCableConnected || usbPortRole === 'NONE'}
                  onChange={(e) => setUsbCurrentConfirmed(e.target.checked)}
                  className="h-4 w-4 accent-[#10B981]"
                />
                <span>Courant PMIC mesuré</span>
              </label>
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 border-t border-[#162242] pt-4 text-xs">
              <div className="space-y-1">
                <p className="font-semibold text-white">Source télémétrie</p>
                <p className="text-[#94A3B8]">{telemetry.sourceLabel}</p>
              </div>
              <div className="space-y-1">
                <p className="font-semibold text-white flex items-center gap-1.5">
                  <ShieldAlert className="h-3.5 w-3.5 text-[#F59E0B]" /> Règle d&apos;honnêteté matérielle
                </p>
                <p className="text-[#94A3B8]">
                  {usbCapability.hardwareTransferGuaranteed
                    ? 'Transfert USB-PD confirmé par le contrôleur.'
                    : 'Câble détecté ≠ transfert électrique garanti. Wireless Qi jamais simulé.'}
                </p>
              </div>
            </div>
          </div>

          {/* 4-KPI Summary Strip */}
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div className="rounded-xl border border-[#23355E] bg-[#0E1730] p-3.5">
              <p className="text-[11px] text-[#94A3B8]">Cible Android</p>
              <p className="mt-1 font-mono text-lg font-bold tabular-nums text-white">API 26–35</p>
              <p className="text-[11px] text-[#3B82F6]">Android 8.0+</p>
            </div>
            <div className="rounded-xl border border-[#23355E] bg-[#0E1730] p-3.5">
              <p className="text-[11px] text-[#94A3B8]">Persistance</p>
              <p className="mt-1 font-mono text-lg font-bold tabular-nums text-white">Room 2.6</p>
              <p className="text-[11px] text-[#10B981]">2 DAO · KSP</p>
            </div>
            <div className="rounded-xl border border-[#23355E] bg-[#0E1730] p-3.5">
              <p className="text-[11px] text-[#94A3B8]">UI Compose</p>
              <p className="mt-1 font-mono text-lg font-bold tabular-nums text-white">7 Écrans</p>
              <p className="text-[11px] text-[#3B82F6]">Material 3</p>
            </div>
            <div className="rounded-xl border border-[#23355E] bg-[#0E1730] p-3.5">
              <p className="text-[11px] text-[#94A3B8]">Sessions Room</p>
              <p className="mt-1 font-mono text-lg font-bold tabular-nums text-white">{sessions.length}</p>
              <p className="text-[11px] text-[#10B981]">Enregistrées</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
