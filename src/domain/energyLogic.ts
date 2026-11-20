export type BatteryStatus = 'CHARGING' | 'DISCHARGING' | 'FULL' | 'NOT_CHARGING' | 'UNKNOWN';
export type PluggedSource = 'NONE' | 'AC' | 'USB' | 'WIRELESS';
export type BatteryHealth = 'GOOD' | 'OVERHEAT' | 'OVER_VOLTAGE' | 'DEAD' | 'COLD' | 'UNKNOWN';
export type TransferMode =
  | 'Partage USB-C / OTG'
  | 'Réception USB-C / OTG'
  | 'Partage sans fil Qi'
  | 'Réception sans fil Qi';

export interface TelemetrySnapshot {
  levelPct: number;
  status: BatteryStatus;
  pluggedSource: PluggedSource;
  temperatureCelsius: number | null;
  voltageMv: number;
  currentNowMa: number | null;
  health: BatteryHealth;
  technology: string;
  sourceLabel: string;
}

export interface ProtectionConfig {
  minDonorBatteryPct: number;
  maxShareDeltaPct: number;
  maxTempCelsius: number;
  autoStopOnCritical: boolean;
  thermalAlertEnabled: boolean;
  anomalyAlertEnabled: boolean;
}

export type ProtectionSeverity = 'NOMINAL' | 'WARNING' | 'CRITICAL_STOP';

export interface ProtectionVerdict {
  severity: ProtectionSeverity;
  shouldStopSession: boolean;
  reason: string;
  alertMessage: string | null;
}

export interface UsbPortInspection {
  hasUsbHostFeature: boolean;
  usbCableDetected: boolean;
  portRole: 'SOURCE' | 'SINK' | 'NONE' | 'UNAVAILABLE';
  hardwareTransferGuaranteed: boolean;
  diagnosticNote: string;
}

export interface WirelessInspection {
  isReceivingWirelessNow: boolean;
  deviceModel: string;
  oemReverseShareHint: boolean;
  oemSettingsPath: string;
  hardwareControllableViaPublicApi: false;
  diagnosticNote: string;
}

export interface EnergySessionRecord {
  id: string;
  mode: TransferMode;
  peerDeviceName: string;
  startBatteryPct: number;
  endBatteryPct: number;
  peakTempCelsius: number | null;
  startedAtIso: string;
  endedAtIso: string | null;
  terminationReason: string;
  hardwareTransferObserved: boolean;
}

export const DEFAULT_PROTECTION_CONFIG: ProtectionConfig = {
  minDonorBatteryPct: 25,
  maxShareDeltaPct: 20,
  maxTempCelsius: 42.0,
  autoStopOnCritical: true,
  thermalAlertEnabled: true,
  anomalyAlertEnabled: true,
};

export function evaluateProtection(
  telemetry: TelemetrySnapshot,
  config: ProtectionConfig,
  startDonorBatteryPct: number | null,
  connectionAnomalyDetected: boolean
): ProtectionVerdict {
  if (connectionAnomalyDetected && config.autoStopOnCritical) {
    return {
      severity: 'CRITICAL_STOP',
      shouldStopSession: true,
      reason: 'Anomalie de connexion détectée',
      alertMessage: 'Connexion électrique ou USB instable détectée : arrêt de sécurité.',
    };
  }

  if (
    telemetry.health === 'OVERHEAT' ||
    (telemetry.temperatureCelsius !== null && telemetry.temperatureCelsius >= config.maxTempCelsius)
  ) {
    return {
      severity: 'CRITICAL_STOP',
      shouldStopSession: config.autoStopOnCritical,
      reason: 'Température excessive détectée',
      alertMessage: `Température excessive (${telemetry.temperatureCelsius?.toFixed(1) ?? '--'} °C >= ${config.maxTempCelsius.toFixed(1)} °C).`,
    };
  }

  if (telemetry.health === 'OVER_VOLTAGE' || telemetry.health === 'DEAD') {
    return {
      severity: 'CRITICAL_STOP',
      shouldStopSession: config.autoStopOnCritical,
      reason: 'État batterie critique signalé',
      alertMessage: `Santé batterie critique (${telemetry.health}).`,
    };
  }

  if (telemetry.levelPct <= 10 || telemetry.levelPct <= config.minDonorBatteryPct) {
    return {
      severity: 'CRITICAL_STOP',
      shouldStopSession: config.autoStopOnCritical,
      reason: 'Seuil minimal donneur atteint',
      alertMessage: `Niveau batterie (${telemetry.levelPct}%) au seuil minimal (${config.minDonorBatteryPct}%).`,
    };
  }

  if (startDonorBatteryPct !== null) {
    const sharedDelta = Math.max(0, startDonorBatteryPct - telemetry.levelPct);
    if (sharedDelta >= config.maxShareDeltaPct) {
      return {
        severity: 'CRITICAL_STOP',
        shouldStopSession: config.autoStopOnCritical,
        reason: 'Limite de partage atteinte',
        alertMessage: `Limite de partage atteinte (${sharedDelta}% partagés / max ${config.maxShareDeltaPct}%).`,
      };
    }
  }

  if (telemetry.temperatureCelsius !== null && telemetry.temperatureCelsius >= config.maxTempCelsius - 3.0) {
    return {
      severity: 'WARNING',
      shouldStopSession: false,
      reason: 'Température élevée',
      alertMessage: `Température élevée (${telemetry.temperatureCelsius.toFixed(1)} °C) : surveillance rapprochée.`,
    };
  }

  return {
    severity: 'NOMINAL',
    shouldStopSession: false,
    reason: 'Fonctionnement nominal',
    alertMessage: null,
  };
}

export function inspectUsbCapability(
  usbCableDetected: boolean,
  portRole: 'SOURCE' | 'SINK' | 'NONE' | 'UNAVAILABLE',
  currentFlowConfirmed: boolean
): UsbPortInspection {
  const hardwareTransferGuaranteed =
    usbCableDetected && (portRole === 'SOURCE' || portRole === 'SINK') && currentFlowConfirmed;

  let diagnosticNote =
    'Aucun câble USB-C / OTG actif détecté. Une simple connexion USB ne garantit jamais un transfert électrique.';

  if (usbCableDetected && !hardwareTransferGuaranteed) {
    diagnosticNote =
      'Câble USB détecté (données/OTG), mais le transfert de puissance électrique n’est pas confirmé par le contrôleur USB-PD/PMIC.';
  } else if (hardwareTransferGuaranteed && portRole === 'SOURCE') {
    diagnosticNote =
      'Rôle USB-PD SOURCE actif confirmé : l’appareil agit comme source d’alimentation vers le périphérique connecté.';
  } else if (hardwareTransferGuaranteed && portRole === 'SINK') {
    diagnosticNote =
      'Rôle USB-PD SINK actif confirmé : l’appareil reçoit de l’énergie électrique.';
  }

  return {
    hasUsbHostFeature: true,
    usbCableDetected,
    portRole,
    hardwareTransferGuaranteed,
    diagnosticNote,
  };
}

export function inspectWirelessCapability(
  pluggedSource: PluggedSource,
  deviceModel: string
): WirelessInspection {
  const isReceivingWirelessNow = pluggedSource === 'WIRELESS';
  return {
    isReceivingWirelessNow,
    deviceModel,
    oemReverseShareHint: true,
    oemSettingsPath: 'Paramètres > Batterie > Partage de batterie / Recharge sans fil inversée',
    hardwareControllableViaPublicApi: false,
    diagnosticNote: isReceivingWirelessNow
      ? 'Réception sans fil Qi active détectée par le système.'
      : 'Aucune simulation effectuée : Android ne permet pas de forcer l’émission Qi inversée sans activation matérielle dans les paramètres constructeur.',
  };
}
