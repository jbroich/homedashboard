import { ChartPoint, Measurement } from '@/lib/api';
import { formatDateTime } from '@/lib/dates';

const STALE_READING_MS = 90 * 60 * 1000;

export type HealthTone = 'good' | 'warning' | 'danger' | 'muted';

export type MeasurementHealth = {
  tone: HealthTone;
  label: string;
  detail: string;
  color: string;
  backgroundColor: string;
};

const toneColors: Record<HealthTone, { color: string; backgroundColor: string }> = {
  good: { color: '#047857', backgroundColor: '#ECFDF3' },
  warning: { color: '#B54708', backgroundColor: '#FFFAEB' },
  danger: { color: '#B42318', backgroundColor: '#FEF3F2' },
  muted: { color: '#667085', backgroundColor: '#F2F4F7' },
};

function health(
  tone: HealthTone,
  label: string,
  detail: string,
): MeasurementHealth {
  return {
    tone,
    label,
    detail,
    ...toneColors[tone],
  };
}

export function getMeasurementHealth(
  measurement: Measurement | null | undefined,
  now = new Date(),
) {
  if (!measurement) {
    return health('muted', 'No data', 'No sensor reading has been received yet.');
  }

  const timestamp = measurement.ts ? new Date(measurement.ts) : null;

  if (!timestamp || Number.isNaN(timestamp.getTime())) {
    return health('warning', 'No timestamp', 'Latest reading is missing its update time.');
  }

  if (now.getTime() - timestamp.getTime() > STALE_READING_MS) {
    return health('danger', 'Offline', `Last update ${formatDateTime(measurement.ts)}.`);
  }

  if (
    typeof measurement.batteryPercentage === 'number' &&
    measurement.batteryPercentage <= 20
  ) {
    return health('warning', 'Low battery', `${measurement.batteryPercentage}% battery remaining.`);
  }

  if (typeof measurement.humidity === 'number') {
    if (measurement.humidity > 65) {
      return health('warning', 'Humid', 'Humidity is above the comfort range.');
    }

    if (measurement.humidity < 35) {
      return health('warning', 'Dry', 'Humidity is below the comfort range.');
    }
  }

  if (typeof measurement.temperature === 'number') {
    if (measurement.temperature > 27) {
      return health('warning', 'Warm', 'Temperature is above the comfort range.');
    }

    if (measurement.temperature < 18) {
      return health('warning', 'Cool', 'Temperature is below the comfort range.');
    }
  }

  return health('good', 'Comfortable', 'Temperature and humidity are in range.');
}

export function getMetricStats(
  points: ChartPoint[],
  key: 'temperature' | 'humidity',
) {
  const values = points
    .map((point) => point[key])
    .filter((value): value is number => typeof value === 'number' && Number.isFinite(value));

  if (!values.length) {
    return null;
  }

  const total = values.reduce((sum, value) => sum + value, 0);

  return {
    average: total / values.length,
    count: values.length,
    high: Math.max(...values),
    low: Math.min(...values),
  };
}
