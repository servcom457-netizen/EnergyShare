import test from 'node:test';
import assert from 'node:assert/strict';
import {
  DEFAULT_PROTECTION_CONFIG,
  evaluateProtection,
  inspectUsbCapability,
  inspectWirelessCapability,
  type TelemetrySnapshot,
} from './energyLogic';

const baseTelemetry: TelemetrySnapshot = {
  levelPct: 78,
  status: 'DISCHARGING',
  pluggedSource: 'NONE',
  temperatureCelsius: 31.4,
  voltageMv: 3980,
  currentNowMa: -280,
  health: 'GOOD',
  technology: 'Li-ion',
  sourceLabel: 'Capteur système',
};

test('evaluateProtection returns NOMINAL under safe conditions', () => {
  const verdict = evaluateProtection(baseTelemetry, DEFAULT_PROTECTION_CONFIG, 80, false);
  assert.equal(verdict.severity, 'NOMINAL');
  assert.equal(verdict.shouldStopSession, false);
});

test('evaluateProtection stops when donor battery reaches minimum threshold', () => {
  const verdict = evaluateProtection(
    { ...baseTelemetry, levelPct: 25 },
    DEFAULT_PROTECTION_CONFIG,
    60,
    false
  );
  assert.equal(verdict.severity, 'CRITICAL_STOP');
  assert.equal(verdict.shouldStopSession, true);
  assert.equal(verdict.reason, 'Seuil minimal donneur atteint');
});

test('evaluateProtection stops when share delta limit is reached', () => {
  const verdict = evaluateProtection(
    { ...baseTelemetry, levelPct: 60 },
    DEFAULT_PROTECTION_CONFIG,
    80,
    false
  );
  assert.equal(verdict.severity, 'CRITICAL_STOP');
  assert.equal(verdict.shouldStopSession, true);
  assert.equal(verdict.reason, 'Limite de partage atteinte');
});

test('evaluateProtection stops on overheat threshold', () => {
  const verdict = evaluateProtection(
    { ...baseTelemetry, temperatureCelsius: 42.5 },
    DEFAULT_PROTECTION_CONFIG,
    80,
    false
  );
  assert.equal(verdict.severity, 'CRITICAL_STOP');
  assert.equal(verdict.shouldStopSession, true);
  assert.equal(verdict.reason, 'Température excessive détectée');
});

test('evaluateProtection stops on connection anomaly', () => {
  const verdict = evaluateProtection(baseTelemetry, DEFAULT_PROTECTION_CONFIG, 79, true);
  assert.equal(verdict.shouldStopSession, true);
  assert.equal(verdict.reason, 'Anomalie de connexion détectée');
});

test('inspectUsbCapability never guarantees power transfer from cable connection alone', () => {
  const usb = inspectUsbCapability(true, 'NONE', false);
  assert.equal(usb.usbCableDetected, true);
  assert.equal(usb.hardwareTransferGuaranteed, false);
});

test('inspectWirelessCapability never claims public API control over reverse Qi', () => {
  const qi = inspectWirelessCapability('NONE', 'Pixel 8 Pro');
  assert.equal(qi.isReceivingWirelessNow, false);
  assert.equal(qi.hardwareControllableViaPublicApi, false);
});
