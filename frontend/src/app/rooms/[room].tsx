import { useLocalSearchParams, useRouter } from 'expo-router';
import { AlertTriangle, BatteryMedium, CheckCircle2, ChevronLeft, Clock3, RefreshCw } from 'lucide-react-native';
import { useCallback, useEffect, useState } from 'react';
import { Pressable, ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { MetricLineChart } from '@/components/charts/metric-line-chart';
import { CalendarPicker } from '@/components/dashboard/calendar-picker';
import { ChartRange, RangeControl } from '@/components/dashboard/range-control';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { AppText } from '@/components/ui/text';
import { ChartDataResponse, fetchChartData, fetchLatestMeasurement, Measurement } from '@/lib/api';
import { formatDateTime, startOfMonth, toEndOfDayOffsetDateTime } from '@/lib/dates';
import { getMeasurementHealth, MeasurementHealth } from '@/lib/status';
import { isRoomId, palette, roomById } from '@/lib/theme';
import { formatMetric } from '@/lib/utils';

export default function RoomDetailScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ room?: string }>();
  const roomParam = Array.isArray(params.room) ? params.room[0] : params.room;
  const roomId = roomParam && isRoomId(roomParam) ? roomParam : null;
  const room = roomId ? roomById[roomId] : null;

  const [range, setRange] = useState<ChartRange>('day');
  const [selectedDate, setSelectedDate] = useState(() => new Date());
  const [visibleMonth, setVisibleMonth] = useState(() => startOfMonth(new Date()));
  const [latest, setLatest] = useState<Measurement | null>(null);
  const [chart, setChart] = useState<ChartDataResponse | null>(null);
  const [loadingLatest, setLoadingLatest] = useState(false);
  const [loadingChart, setLoadingChart] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const to = toEndOfDayOffsetDateTime(selectedDate);
  const health = getMeasurementHealth(latest);

  const loadLatest = useCallback(
    async (signal?: AbortSignal) => {
      if (!roomId) {
        return;
      }

      setLoadingLatest(true);
      try {
        const measurement = await fetchLatestMeasurement(roomId, signal);
        if (!signal?.aborted) {
          setLatest(measurement);
        }
      } finally {
        if (!signal?.aborted) {
          setLoadingLatest(false);
        }
      }
    },
    [roomId],
  );

  const loadChart = useCallback(
    async (signal?: AbortSignal) => {
      if (!roomId) {
        return;
      }

      setLoadingChart(true);
      setError(null);
      try {
        const response = await fetchChartData(roomId, range, to, signal);
        if (!signal?.aborted) {
          setChart(response);
        }
      } catch {
        if (!signal?.aborted) {
          setChart(null);
          setError('Unable to load chart data');
        }
      } finally {
        if (!signal?.aborted) {
          setLoadingChart(false);
        }
      }
    },
    [range, roomId, to],
  );

  useEffect(() => {
    const controller = new AbortController();
    const timeout = setTimeout(() => {
      loadLatest(controller.signal).catch(() => {});
    }, 0);

    return () => {
      clearTimeout(timeout);
      controller.abort();
    };
  }, [loadLatest]);

  useEffect(() => {
    const controller = new AbortController();
    const timeout = setTimeout(() => {
      loadChart(controller.signal);
    }, 0);

    return () => {
      clearTimeout(timeout);
      controller.abort();
    };
  }, [loadChart]);

  const refresh = useCallback(() => {
    loadLatest().catch(() => {});
    loadChart();
  }, [loadChart, loadLatest]);

  if (!roomId || !room) {
    return (
      <SafeAreaView className="flex-1 bg-dashboard-background px-4 pt-4">
        <AppText variant="title">Room not found</AppText>
        <Button className="mt-4" label="Back to rooms" onPress={() => router.replace('/')} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView className="flex-1 bg-dashboard-background" edges={['top', 'left', 'right']}>
      <ScrollView contentContainerClassName="gap-4 px-4 pb-8 pt-3">
        <View className="flex-row items-center justify-between">
          <Pressable
            accessibilityLabel="Back"
            accessibilityRole="button"
            className="h-11 w-11 items-center justify-center rounded-card bg-dashboard-surface"
            onPress={() => router.back()}>
            <ChevronLeft color={palette.text} size={22} />
          </Pressable>
          <Button
            accessibilityLabel="Refresh room"
            className="h-11 w-11 px-0"
            icon={<RefreshCw color="#FFFFFF" size={18} />}
            label=""
            onPress={refresh}
          />
        </View>

        <Card className="gap-4">
          <View>
            <AppText variant="title">{room.label}</AppText>
            <AppText variant="caption">
              {loadingLatest
                ? 'Loading latest reading'
                : latest?.ts
                  ? `Latest ${formatDateTime(latest.ts)}`
                  : 'No data yet'}
            </AppText>
          </View>

          <View className="gap-2">
            <HealthBanner health={health} />
            {typeof latest?.batteryPercentage === 'number' ? (
              <View className="flex-row items-center gap-2">
                <BatteryMedium color={palette.warning} size={18} />
                <AppText variant="caption">{latest.batteryPercentage}% battery</AppText>
              </View>
            ) : null}
          </View>

          <View className="flex-row gap-3">
            <View className="flex-1 rounded-card bg-dashboard-soft p-3">
              <AppText variant="label">Temp</AppText>
              <AppText variant="metric">
                {formatMetric(latest?.temperature)}
                <AppText className="text-xl text-dashboard-muted"> C</AppText>
              </AppText>
            </View>
            <View className="flex-1 rounded-card bg-dashboard-soft p-3">
              <AppText variant="label">Humidity</AppText>
              <AppText variant="metric">
                {formatMetric(latest?.humidity)}
                <AppText className="text-xl text-dashboard-muted"> %</AppText>
              </AppText>
            </View>
          </View>
        </Card>

        <RangeControl value={range} onChange={setRange} />

        <CalendarPicker
          selectedDate={selectedDate}
          visibleMonth={visibleMonth}
          onSelectDate={(date) => {
            setSelectedDate(date);
            setVisibleMonth(startOfMonth(date));
          }}
          onVisibleMonthChange={setVisibleMonth}
        />

        <View className="gap-1">
          <AppText variant="label">Range end</AppText>
          <AppText className="font-mono text-sm text-dashboard-muted">{to}</AppText>
        </View>

        {error ? (
          <Card>
            <AppText className="text-center text-red-700" variant="caption">
              {error}
            </AppText>
          </Card>
        ) : null}

        {loadingChart && !chart ? (
          <Card className="h-36 items-center justify-center">
            <AppText variant="caption">Loading chart data</AppText>
          </Card>
        ) : (
          <View className="gap-4">
            <MetricLineChart
              color={palette.temperature}
              points={chart?.points ?? []}
              title="Temperature"
              unit="C"
              valueKey="temperature"
            />
            <MetricLineChart
              color={palette.humidity}
              points={chart?.points ?? []}
              title="Humidity"
              unit="%"
              valueKey="humidity"
            />
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

function HealthBanner({ health }: { health: MeasurementHealth }) {
  const Icon =
    health.tone === 'good' ? CheckCircle2 : health.tone === 'muted' ? Clock3 : AlertTriangle;

  return (
    <View
      className="flex-row items-start gap-2 rounded-card px-3 py-2"
      style={{ backgroundColor: health.backgroundColor }}>
      <Icon color={health.color} size={18} strokeWidth={2.4} />
      <View className="min-w-0 flex-1">
        <AppText className="text-sm font-semibold" style={{ color: health.color }}>
          {health.label}
        </AppText>
        <AppText className="text-xs" style={{ color: health.color }} variant="caption">
          {health.detail}
        </AppText>
      </View>
    </View>
  );
}
