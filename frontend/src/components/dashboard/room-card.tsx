import {
  AlertTriangle,
  BatteryMedium,
  CheckCircle2,
  ChevronRight,
  Clock3,
  Droplets,
  Thermometer,
} from 'lucide-react-native';
import { Pressable, View } from 'react-native';

import { Card } from '@/components/ui/card';
import { AppText } from '@/components/ui/text';
import { Measurement } from '@/lib/api';
import { formatDateTime } from '@/lib/dates';
import { getMeasurementHealth, MeasurementHealth } from '@/lib/status';
import { palette } from '@/lib/theme';
import { formatMetric } from '@/lib/utils';

type RoomCardProps = {
  label: string;
  measurement: Measurement | null;
  onPress: () => void;
  status: 'loading' | 'ready' | 'empty' | 'error';
};

export function RoomCard({ label, measurement, onPress, status }: RoomCardProps) {
  const hasData = status === 'ready' && measurement;
  const health = hasData ? getMeasurementHealth(measurement) : getFallbackHealth(status);

  return (
    <Pressable accessibilityRole="button" onPress={onPress}>
      {({ pressed }) => (
        <Card className={pressed ? 'opacity-75' : undefined}>
          <View className="flex-row items-start justify-between gap-3">
            <View className="min-w-0 flex-1 gap-1">
              <AppText variant="subtitle">{label}</AppText>
              <AppText variant="caption">
                {status === 'loading'
                  ? 'Loading'
                  : hasData
                    ? formatDateTime(measurement.ts)
                    : status === 'error'
                      ? 'Unable to load'
                      : 'No data yet'}
              </AppText>
            </View>
            <View className="items-end gap-2">
              <HealthBadge health={health} />
              <ChevronRight color={palette.muted} size={20} />
            </View>
          </View>

          {hasData ? (
            <View className="mt-4 gap-3">
              <View className="flex-row gap-3">
                <MetricPill
                  icon={<Thermometer color={palette.temperature} size={18} />}
                  label="Temp"
                  value={`${formatMetric(measurement.temperature)} C`}
                />
                <MetricPill
                  icon={<Droplets color={palette.humidity} size={18} />}
                  label="Humidity"
                  value={`${formatMetric(measurement.humidity)}%`}
                />
              </View>

              <AppText variant="caption">{health.detail}</AppText>

              {typeof measurement.batteryPercentage === 'number' ? (
                <View className="flex-row items-center gap-2">
                  <BatteryMedium color={palette.warning} size={18} />
                  <AppText variant="caption">{measurement.batteryPercentage}% battery</AppText>
                </View>
              ) : null}
            </View>
          ) : (
            <View className="mt-4 h-20 justify-center rounded-card bg-dashboard-soft px-4">
              <AppText className="text-center" variant="caption">
                {status === 'loading' ? 'Checking latest readings' : 'No data yet'}
              </AppText>
            </View>
          )}
        </Card>
      )}
    </Pressable>
  );
}

function getFallbackHealth(status: RoomCardProps['status']): MeasurementHealth {
  if (status === 'error') {
    return {
      tone: 'danger',
      label: 'Error',
      detail: 'Unable to load the latest reading.',
      color: '#B42318',
      backgroundColor: '#FEF3F2',
    };
  }

  if (status === 'loading') {
    return {
      tone: 'muted',
      label: 'Loading',
      detail: 'Checking latest readings.',
      color: '#667085',
      backgroundColor: '#F2F4F7',
    };
  }

  return {
    tone: 'muted',
    label: 'No data',
    detail: 'No sensor reading has been received yet.',
    color: '#667085',
    backgroundColor: '#F2F4F7',
  };
}

function HealthBadge({ health }: { health: MeasurementHealth }) {
  const Icon =
    health.tone === 'good' ? CheckCircle2 : health.tone === 'muted' ? Clock3 : AlertTriangle;

  return (
    <View
      className="flex-row items-center gap-1 rounded-md px-2 py-1"
      style={{ backgroundColor: health.backgroundColor }}>
      <Icon color={health.color} size={14} strokeWidth={2.4} />
      <AppText className="text-xs font-semibold" style={{ color: health.color }} variant="caption">
        {health.label}
      </AppText>
    </View>
  );
}

type MetricPillProps = {
  icon: React.ReactNode;
  label: string;
  value: string;
};

function MetricPill({ icon, label, value }: MetricPillProps) {
  return (
    <View className="min-h-20 flex-1 justify-between rounded-card bg-dashboard-soft p-3">
      <View className="flex-row items-center gap-2">
        {icon}
        <AppText variant="label">{label}</AppText>
      </View>
      <AppText className="text-2xl" variant="metric">
        {value}
      </AppText>
    </View>
  );
}
